package com.rakensi;

import static com.rakensi.ExtensionFunctionsModule.functionSignature;
import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.FunctionDSL.param;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.exist.dom.QName;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.Function;
import org.exist.xquery.FunctionCall;
import org.exist.xquery.FunctionDSL;
import org.exist.xquery.FunctionFactory;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;
import org.greenmercury.smax.SmaxDocument;
import org.greenmercury.smax.SmaxElement;
import org.greenmercury.smax.SmaxException;
import org.greenmercury.smax.convert.DomElement;
import org.greenmercury.smax.convert.SAX;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.blitz.Option;
import de.bottlecaps.markup.blitz.Parser;

/**
 * Implementation of
 *   transparent-invisible-xml(
 *     $grammar  as item()?  := (),
 *     $options  as map(*)?  := {}
 *   )  as function($input as item()) as node()*
 * @see https://www.xmlprague.cz/day3-2024/#iXML
 */
public class FnTransparentInvisibleXml extends BasicFunction
{

  private static final String FS_TRANSPARENT_INVISIBLE_XML_NAME = "transparent-invisible-xml";

  static final FunctionSignature FS_TRANSPARENT_INVISIBLE_XML =
      functionSignature(
          FnTransparentInvisibleXml.FS_TRANSPARENT_INVISIBLE_XML_NAME,
          "Returns a transparent invisible xml parser from a grammar.",
          new FunctionReturnSequenceType(Type.FUNCTION_REFERENCE, Cardinality.EXACTLY_ONE, "A function that can be used to parse an input string or node."),
          optParam("grammar", Type.ITEM, "The ixml grammar used to generate the parser"),
          optParam("options", Type.MAP, "The options for the parser genarator and the parser itself. Supported options are 'fail-on-error', 'verbose', 'trace', 'timing'")
      );

  // Make a logger, and a trace-writer for Markup Blitz.
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(FnTransparentInvisibleXml.class);
  private static final Writer logWriter = new Writer() {
    StringBuffer buffer = new StringBuffer();
    public int indexOf(char[] cbuf, char c, int off, int len) {
      int maxPos = off + len;
      for (int i = off; i < maxPos; i++) if (cbuf[i] == c) return i;
      return -1;
    }
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
      int eolPos;
      while ((eolPos = indexOf(cbuf, '\n', off, len)) >= 0) {
        buffer.append(String.valueOf(cbuf, off, eolPos - off));
        flush();
        off = eolPos + 1;
        len = len - (eolPos - off) - 1;
      }
      if (len > 0) {
        buffer.append(String.valueOf(cbuf, off, len));
      }
    }
    @Override
    public void flush() throws IOException
    {
      if (buffer.length() > 0) {
        logger.info(buffer);
        buffer.delete(0, buffer.length());
      }
    }
    @Override
    public void close() throws IOException
    {
      flush();
    }
  };

  public FnTransparentInvisibleXml(final XQueryContext context, final FunctionSignature signature) {
      super(context, signature);
  }

  /**
   * Implementation of the transparent-invisible-xml function.
   */
  @Override
  public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
      // Handle $grammar and $options parameters.
      final String grammar;
      if (args[0].isEmpty()) {
        grammar = ExtensionFunctionsModule.getIxmlGrammar();
      } else {
        grammar = ((StringValue)args[0].itemAt(0)).getStringValue();
      }
      final Map<Option, Object> options;
      if (args[1].isEmpty()) {
        options = ExtensionFunctionsModule.getOptions(null);
      } else {
        options = ExtensionFunctionsModule.getOptions((MapType) args[1].itemAt(0));
      }
      // Generate the Markup Blitz parser for the grammar.
      final Parser parser = Blitz.generate(grammar, options);
      parser.setTraceWriter(logWriter);
      // Make a TixmlParser function from the Markup Blitz parser. The signature is function(xs:item) as item()+
      FunctionSignature parserSignature = FunctionDSL.functionSignature(
          new QName("generated-tixml-parser", "https://invisiblexml.org/"),
          "Generated tixml parser, only used internally",
          new FunctionReturnSequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE, "The result of parsing the input"),
          param("input", Type.ITEM, "The input string or node")
      );
      final TixmlParser tixmlParser = new TixmlParser(context, parserSignature, parser);
      // Make a function reference that can be used as the result.
      FunctionCall functionCall = FunctionFactory.wrap(context, tixmlParser);
      return new FunctionReference(functionCall);
  }


  /**
   * A BasicFunction for the generated tixml parser.
   */
  private static final class TixmlParser extends BasicFunction {

    private Parser parser;

    public TixmlParser(XQueryContext context, FunctionSignature signature, Parser parser) throws XPathException
    {
        super(context, signature);
        this.parser = parser;
        // We must set the arguments, which is not done automatically from the signature.
        final List<Expression> ixmlParserArgs = new ArrayList<>(1);
        ixmlParserArgs.add(new Function.Placeholder(context));
        this.setArguments(ixmlParserArgs);
    }

    /**
     * Implementation of the generated parser function.
     */
    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
    {
      Item inputParameter = args[0].itemAt(0);
      // Create a SMAX document with a <wrapper> root element that will be removed later.
      SmaxDocument smaxDocument = null;
      if (Type.subTypeOf(inputParameter.getType(), Type.STRING)) {
        // Wrap the string in an element.
        final String inputString = inputParameter.getStringValue();
        final SmaxElement wrapper = new SmaxElement("wrapper").setStartPos(0).setEndPos(inputString.length());
        smaxDocument = new SmaxDocument(wrapper, inputString);
      } else if (Type.subTypeOf(inputParameter.getType(), Type.NODE)) {
        Node inputNode = ((NodeValue) inputParameter).getNode();
        if (inputNode instanceof Document || inputNode instanceof DocumentFragment) {
          inputNode = inputNode.getFirstChild();
        }
        Element inputElement = wrap(inputNode);
        try{
          smaxDocument = DomElement.toSmax(inputElement);
        } catch (SmaxException e) {
          throw new XPathException(this, ErrorCodes.ERROR, e);
        }
      } else {
        throw new XPathException(this, ErrorCodes.ERROR, "The generated NER function accepts a string or node, but not a "+Type.getTypeName(inputParameter.getType()));
      }
      // Do Named Entity Recognition on the SMAX document.
      SmaxSerializer serializer = new SmaxSerializer(smaxDocument);
      this.parser.parse(smaxDocument.getContentBuffer().toString(), serializer);
      // Convert the SMAX document to something that eXist-db can use.
      SAXAdapter saxAdapter = new SAXAdapter();
      try {
        SAX.fromSMAX(smaxDocument, saxAdapter);
      } catch (SAXException e) {
        throw new XPathException(this, ErrorCodes.ERROR, e);
      }
      DocumentImpl outputDocument = saxAdapter.getDocument();
      final NodeList children = outputDocument.getDocumentElement().getChildNodes();
      if(children.getLength() == 0) {
          return Sequence.EMPTY_SEQUENCE;
      }
      final ValueSequence result = new ValueSequence(children.getLength());
      for(int i = 0; i < children.getLength(); i++) {
        NodeValue child = (NodeValue)children.item(i);
        result.add(child);
      }
      return result;
    }

    /**
     * The org.exist.dom.memtree.Element does not implement appendChild(), and org.exist.dom.persistent.ElementImpl does not want the owner element of `node`.
     * Therefore, we have to make our own wrapper element, which needs to work for org.greenmercury.smax.convert.DomElement.toSmax(Element).
     * @param node A node that must be wrapped in a "wrapper" element.
     * @return The wrapper element.
     */
    private Element wrap(Node node)
    {
      Element wrapper = new VerySimpleElementImpl("wrapper");
      wrapper.appendChild(node);
      return wrapper;
    }

  }

}
