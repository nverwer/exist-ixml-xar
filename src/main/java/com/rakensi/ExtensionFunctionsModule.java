package com.rakensi;

import static org.exist.xquery.FunctionDSL.functionDefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.exist.dom.QName;
import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.FunctionDSL;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.blitz.Option;
import io.lacuna.bifurcan.IEntry;

/**
 */
public class ExtensionFunctionsModule extends AbstractInternalModule {

    public static final String NAMESPACE_URI = "http://rakensi.com/exist-db/xquery/functions/ixml";
    public static final String PREFIX = "ixml";
    public static final String RELEASED_IN_VERSION = "eXist-6.2.0";

    // The location of the ixml grammar.
    private static final String IXML_GRAMMAR_RESOURCE = "ixml.ixml";

    // register the functions of the module
    public static final FunctionDef[] functions = functionDefs(
        functionDefs(FnInvisibleXml.class, FnInvisibleXml.FS_INVISIBLE_XML),
        functionDefs(FnTransparentInvisibleXml.class, FnTransparentInvisibleXml.FS_TRANSPARENT_INVISIBLE_XML)
    );

    public ExtensionFunctionsModule(final Map<String, List<? extends Object>> parameters) {
        super(functions, parameters);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "Invisible XML functions for eXist-db";
    }

    @Override
    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }

    public static FunctionSignature functionSignature(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType... paramTypes) {
        return FunctionDSL.functionSignature(new QName(name, NAMESPACE_URI), description, returnType, paramTypes);
    }

    public static FunctionSignature[] functionSignatures(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType[][] variableParamTypes) {
        return FunctionDSL.functionSignatures(new QName(name, NAMESPACE_URI), description, returnType, variableParamTypes);
    }

    /* Common functions for ixml and tixml */

    // Get Markup Blitz options from the `$options as map(*)` parameter.
    public static Map<Option, Object> getOptions(final MapType options) throws XPathException {
      HashMap<Option, Object> optionsMap = new HashMap<Option, Object>();
      if (options != null) {
        for (Iterator<IEntry<AtomicValue, Sequence>> it = options.iterator(); it.hasNext();) {
          IEntry<AtomicValue, Sequence> entry = it.next();
          String key = entry.key().getStringValue();
          String value = entry.value().getStringValue();
          if (!Option.addTo(optionsMap, key, value)) {
            throw new XPathException(ErrorCodes.ERROR, "Unsupported option \"" + key + "\": \"" + value + "\"");
          }
        }
      }
      return optionsMap;
    }

    // Read the ixml grammar from a resource.
    public static String getIxmlGrammar() throws XPathException {
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

//    static class ExpathBinModuleErrorCode extends ErrorCodes.ErrorCode {
//        private ExpathBinModuleErrorCode(final String code, final String description) {
//            super(new QName(code, NAMESPACE_URI, PREFIX), description);
//        }
//    }
}
