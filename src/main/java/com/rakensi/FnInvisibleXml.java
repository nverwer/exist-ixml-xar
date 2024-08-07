package com.rakensi;

import static com.rakensi.ExtensionFunctionsModule.functionSignature;
import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.FunctionDSL.param;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.exist.dom.QName;
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
import org.exist.xquery.functions.fn.ParsingFunctions;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.blitz.Parser;

/**
 * Implementation of
 *   fn:invisible-xml(
 *     $grammar  as item()?  := (),
 *     $options  as map(*)?  := {}
 *   )  as fn(xs:string) as item()
 * @see https://qt4cg.org/specifications/xpath-functions-40/Overview.html#ixml-functions
 *
 * To do:
 * - Use the Markup Blitz serializer instead of parsing serialized XML.
 */
public class FnInvisibleXml extends BasicFunction
{

  private static final String FS_INVISIBLE_XML_NAME = "invisible-xml";

  static final FunctionSignature FS_INVISIBLE_XML =
      functionSignature(
          FnInvisibleXml.FS_INVISIBLE_XML_NAME,
          "Returns an ixml parser from a grammar. The parser returns an XML representation of the input string as parsed by the provided grammar.",
          new FunctionReturnSequenceType(Type.FUNCTION_REFERENCE, Cardinality.EXACTLY_ONE, "A function that can be used to parse an input string."),
          optParam("grammar", Type.ITEM, "The ixml grammar used to generate the parser"),
          optParam("options", Type.MAP, "The options for the parser genarator and the parser itself. Supported options are 'fail-on-error', 'verbose', 'trace', 'timing'")
      );

  private static final String IXML_GRAMMAR_RESOURCE = "org/exist/xquery/lib/ixml.ixml";

  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(FnInvisibleXml.class);
  private static final Writer erorLogWriter = new Writer() {
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
        buffer.delete(0, buffer.length()-1);
      }
    }
    @Override
    public void close() throws IOException
    {
      flush();
    }
  };

  public FnInvisibleXml(final XQueryContext context, final FunctionSignature signature) {
      super(context, signature);
  }

  @Override
  public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
      // Handle $grammar and $options parameters.
      final String grammar;
      if (args[0].isEmpty()) {
        grammar = getIxmlGrammar();
      } else {
        grammar = ((StringValue)args[0].itemAt(0)).getStringValue();
      }
      final Blitz.Option[] options;
      if (args[1].isEmpty()) {
        options = new Blitz.Option[0];
      } else {
        options = getOptions((MapType) args[1].itemAt(0));
      }
      // Generate the Markup Blitz parser for the grammar.
      final Parser parser = Blitz.generate(grammar, options);
      parser.setTraceWriter(erorLogWriter);
      // Make an IxmlParser function from the Markup Blitz parser. The signature is function(xs:string) as item()
      FunctionSignature parserSignature = FunctionDSL.functionSignature(
          new QName("generated-ixml-parser", "https://invisiblexml.org/"),
          "Generated ixml parser, only used internally",
          new FunctionReturnSequenceType(Type.ITEM, Cardinality.EXACTLY_ONE, "The result of parsing the input string"),
          param("input", Type.STRING, "The input string")
      );
      final IxmlParser ixmlParser = new IxmlParser(context, parserSignature, parser);
      // Make a function reference that can be used as the result.
      FunctionCall functionCall = FunctionFactory.wrap(context, ixmlParser);
      return new FunctionReference(functionCall);
  }

  // Read the ixml grammar from a resource.
  private String getIxmlGrammar() throws XPathException {
    try (final InputStream ixmlGrammarStream = FnInvisibleXml.class.getClassLoader().getResourceAsStream(IXML_GRAMMAR_RESOURCE)) {
      if (ixmlGrammarStream == null) {
          throw new XPathException(ErrorCodes.FODC0002, "The ixml grammar resource cannot be found at "+IXML_GRAMMAR_RESOURCE);
      }
      try ( InputStreamReader ixmlGrammarStreamReader = new InputStreamReader(ixmlGrammarStream);
            BufferedReader reader = new BufferedReader(ixmlGrammarStreamReader)) {
          return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
    catch (IOException e)
    {
        throw new XPathException(ErrorCodes.FODC0002, "The ixml grammar resource cannot be opened at "+IXML_GRAMMAR_RESOURCE, e);
    }
  }

  // Get Markup Blitz options from the $options as map(*) parameter.
  private Blitz.Option[] getOptions(final MapType options) throws XPathException {
      List<Blitz.Option> optionsList = new ArrayList<Blitz.Option>();
      Sequence failOnErrorOption = options.get(new StringValue("fail-on-error"));
      if (!failOnErrorOption.isEmpty() && ((BooleanValue)failOnErrorOption.itemAt(0).convertTo(Type.BOOLEAN)).getValue()) {
        optionsList.add(Blitz.Option.FAIL_ON_ERROR);
      }
      Sequence traceOption = options.get(new StringValue("trace"));
      if (!traceOption.isEmpty() && ((BooleanValue)traceOption.itemAt(0).convertTo(Type.BOOLEAN)).getValue()) {
        optionsList.add(Blitz.Option.TRACE);
      }
      Sequence verboseOption = options.get(new StringValue("verbose"));
      if (!verboseOption.isEmpty() && ((BooleanValue)verboseOption.itemAt(0).convertTo(Type.BOOLEAN)).getValue()) {
        optionsList.add(Blitz.Option.VERBOSE);
      }
      Sequence timingOption = options.get(new StringValue("timing"));
      if (!timingOption.isEmpty() && ((BooleanValue)timingOption.itemAt(0).convertTo(Type.BOOLEAN)).getValue()) {
        optionsList.add(Blitz.Option.TIMING);
      }
      return optionsList.stream().toArray(Blitz.Option[]::new);
  }


  /**
   * A BasicFunction for the generated ixml parser.
   */
  private static final class IxmlParser extends BasicFunction {

    private Parser parser;

    public IxmlParser(XQueryContext context, FunctionSignature signature, Parser parser) throws XPathException
    {
        super(context, signature);
        this.parser = parser;
        // We must set the arguments, which is not done automatically from the signature.
        final List<Expression> ixmlParserArgs = new ArrayList<>(1);
        ixmlParserArgs.add(new Function.Placeholder(context));
        this.setArguments(ixmlParserArgs);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
    {
        final String input = ((StringValue)args[0].itemAt(0)).getStringValue();
        // Parse the input string.
        final String output = parser.parse(input);
        // The output is serialized XML, which we need to parse.
        ParsingFunctions xmlParser = new ParsingFunctions(context, ParsingFunctions.signatures[0]);
        final Sequence[] xmlParserArgs = new Sequence[1];
        xmlParserArgs[0] = new StringValue(output).toSequence();
        return xmlParser.eval(xmlParserArgs, contextSequence);
    }

  }

}
