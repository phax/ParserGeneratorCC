/*
 * Copyright 2017-2026 Philip Helger, pgcc@helger.com
 *
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Author: sreeni@google.com (Sreeni Viswanadha)
 *
 * Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.helger.pgcc.jjtree.output;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.jjtree.ASTNodeDescriptor;
import com.helger.pgcc.jjtree.JJTreeOptions;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.output.cpp.OtherFilesGenCPP;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.utils.OutputFileGenerator;

@Immutable
public final class NodeFilesCpp
{
  private NodeFilesCpp ()
  {}

  /**
   * ID of the latest version (of JJTree) in which one of the Node classes was modified.
   */
  private static final String NODE_VERSION = PGVersion.MAJOR_DOT_MINOR;

  public static void addType (@NonNull final String sType)
  {
    if (!sType.equals ("Node") && !sType.equals ("SimpleNode"))
    {
      PGCCContext.current ().jjtree ().nodesToGenerate ().add (sType);
    }
  }

  public static String nodeIncludeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), "Node.h").getAbsolutePath ();
  }

  public static String simpleNodeIncludeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), "SimpleNode.h").getAbsolutePath ();
  }

  public static String simpleNodeCodeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), "SimpleNode.cc").getAbsolutePath ();
  }

  public static String jjtreeIncludeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (),
                     PGCCContext.current ().jjtree ().getParserName () + "Tree.h").getAbsolutePath ();
  }

  public static String jjtreeImplFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (),
                     PGCCContext.current ().jjtree ().getParserName () + "Tree.cc").getAbsolutePath ();
  }

  public static String jjtreeIncludeFile (final String s)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), s + ".h").getAbsolutePath ();
  }

  public static String jjtreeImplFile (final String s)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), s + ".cc").getAbsolutePath ();
  }

  public static String jjtreeASTIncludeFile (final String ASTNode)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), ASTNode + ".h").getAbsolutePath ();
  }

  public static String jjtreeASTCodeFile (final String ASTNode)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), ASTNode + ".cc").getAbsolutePath ();
  }

  private static String _getVisitorIncludeFile ()
  {
    final String sName = getVisitorClass ();
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), sName + ".h").getAbsolutePath ();
  }

  public static void generateTreeClasses ()
  {
    _generateNodeHeader ();
    _generateSimpleNodeHeader ();
    _generateSimpleNodeCode ();
    _generateMultiTreeInterface ();
    _generateMultiTreeImpl ();
    _generateOneTreeInterface ();
    if (false)
      _generateOneTreeImpl ();
  }

  private static void _generateNodeHeader ()
  {
    final File aFile = new File (nodeIncludeFile ());

    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                 "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };

    try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
    {
      aOutputFile.setToolName ("JJTree");

      if (aFile.exists () && !aOutputFile.needToWrite ())
        return;

      final Map <String, Object> aOptionMap = Options.getAllOptions ();
      aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
      aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
      aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
      aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));

      generateFile (aOutputFile, "/templates/cpp/jjtree/Node.h.template", aOptionMap, false);
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  private static void _generateSimpleNodeHeader ()
  {
    final File aFile = new File (simpleNodeIncludeFile ());

    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                 "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };

    try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
    {
      aOutputFile.setToolName ("JJTree");

      if (aFile.exists () && !aOutputFile.needToWrite ())
        return;

      final Map <String, Object> aOptionMap = Options.getAllOptions ();
      aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
      aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
      aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
      aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));

      generateFile (aOutputFile, "/templates/cpp/jjtree/SimpleNode.h.template", aOptionMap, false);
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  private static void _generateSimpleNodeCode ()
  {
    final File aFile = new File (simpleNodeCodeFile ());

    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                 "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };

    try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
    {
      aOutputFile.setToolName ("JJTree");

      if (aFile.exists () && !aOutputFile.needToWrite ())
        return;

      final Map <String, Object> aOptionMap = Options.getAllOptions ();
      aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
      aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
      aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
      aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));

      generateFile (aOutputFile, "/templates/cpp/jjtree/SimpleNode.cc.template", aOptionMap, false);
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  private static void _generateMultiTreeInterface ()
  {
    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                 "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    try
    {
      for (final String sNode : PGCCContext.current ().jjtree ().nodesToGenerate ())
      {
        final File aFile = new File (jjtreeIncludeFile (sNode));
        try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
        {
          aOutputFile.setToolName ("JJTree");

          if (aFile.exists () && !aOutputFile.needToWrite ())
            return;

          final Map <String, Object> aOptionMap = Options.getAllOptions ();
          aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
          aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
          aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
          aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));
          aOptionMap.put ("NODE_TYPE", sNode);

          generateFile (aOutputFile, "/templates/cpp/jjtree/MultiNodeInterface.template", aOptionMap, false);
        }
      }
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  private static void _generateMultiTreeImpl ()
  {
    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                 "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };

    try
    {
      for (final String aString : PGCCContext.current ().jjtree ().nodesToGenerate ())
      {
        final String sNode = aString;
        final File aFile = new File (jjtreeImplFile (sNode));
        try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
        {
          aOutputFile.setToolName ("JJTree");

          if (aFile.exists () && !aOutputFile.needToWrite ())
            return;

          final Map <String, Object> aOptionMap = Options.getAllOptions ();
          aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
          aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
          aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
          aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));
          aOptionMap.put ("NODE_TYPE", sNode);

          generateFile (aOutputFile, "/templates/cpp/jjtree/MultiNodeImpl.template", aOptionMap, false);
        }
      }
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  private static void _generateOneTreeInterface ()
  {
    final File aFile = new File (jjtreeIncludeFile ());

    try
    {
      final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX",
                                   "NODE_EXTENDS", "NODE_FACTORY",
                                   Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
      try (OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
      {
        aOutputFile.setToolName ("JJTree");

        if (aFile.exists () && !aOutputFile.needToWrite ())
          return;

        final Map <String, Object> aOptionMap = Options.getAllOptions ();
        aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
        aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
        aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
        aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));

        try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
        {
          final String sIncludeName = aFile.getName ().replace ('.', '_').toUpperCase (Locale.US);
          aOstr.println ("#ifndef " + sIncludeName);
          aOstr.println ("#define " + sIncludeName);
          aOstr.println ("#include \"SimpleNode.h\"");
          for (final String aString : PGCCContext.current ().jjtree ().nodesToGenerate ())
          {
            final String s = aString;
            aOstr.println ("#include \"" + s + ".h\"");
          }
          aOstr.println ("#endif");
        }
      }
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  private static void _generateOneTreeImpl ()
  {
    final File aFile = new File (jjtreeImplFile ());

    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                 "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };

    try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
    {
      aOutputFile.setToolName ("JJTree");

      if (aFile.exists () && !aOutputFile.needToWrite ())
        return;

      final Map <String, Object> aOptionMap = Options.getAllOptions ();
      aOptionMap.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
      aOptionMap.put ("VISITOR_RETURN_TYPE", _getVisitorReturnType ());
      aOptionMap.put ("VISITOR_DATA_TYPE", _getVisitorArgumentType ());
      aOptionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (_getVisitorReturnType ().equals ("void")));
      generateFile (aOutputFile, "/templates/cpp/jjtree/_unused_TreeImplHeader.template", aOptionMap, false);

      final boolean bHasNamespace = Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0;
      if (bHasNamespace)
      {
        aOutputFile.getPrintWriter ().println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }

      for (final String aString : PGCCContext.current ().jjtree ().nodesToGenerate ())
      {
        final String s = aString;
        aOptionMap.put ("NODE_TYPE", s);
        generateFile (aOutputFile, "/templates/cpp/jjtree/MultiNodeImpl.template", aOptionMap, false);
      }

      if (bHasNamespace)
      {
        aOutputFile.getPrintWriter ().println (Options.stringValue ("NAMESPACE_CLOSE"));
      }
    }
    catch (

    final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  static void generatePrologue ()
  {
    // Output the node's namespace name?
  }

  static String nodeConstants ()
  {
    return PGCCContext.current ().jjtree ().getParserName () + "TreeConstants";
  }

  public static void generateTreeConstants ()
  {
    final String sName = nodeConstants ();
    final File aFile = new File (JJTreeOptions.getJJTreeOutputDirectory (), sName + ".h");
    PGCCContext.current ().jjtree ().headersForJJTreeH ().add (aFile.getName ());

    try (final OutputFile aOutputFile = new OutputFile (aFile))
    {
      final PrintWriter aOstr = aOutputFile.getPrintWriter ();

      final List <String> aNodeIds = ASTNodeDescriptor.getNodeIds ();
      final List <String> aNodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue ();
      aOstr.println ("#ifndef " + aFile.getName ().replace ('.', '_').toUpperCase (Locale.US));
      aOstr.println ("#define " + aFile.getName ().replace ('.', '_').toUpperCase (Locale.US));

      aOstr.println ("\n#include \"JavaCC.h\"");
      final boolean bHasNamespace = Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0;
      if (bHasNamespace)
      {
        aOstr.println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }
      aOstr.println ("enum {");
      for (int i = 0; i < aNodeIds.size (); ++i)
      {
        final String n = aNodeIds.get (i);
        aOstr.println ("  " + n + " = " + i + ",");
      }

      aOstr.println ("};");
      aOstr.println ();

      for (int i = 0; i < aNodeNames.size (); ++i)
      {
        aOstr.println ("  static JJChar jjtNodeName_arr_" + i + "[] = ");
        final String n = aNodeNames.get (i);
        // ostr.println(" (JJChar*)\"" + n + "\",");
        OtherFilesGenCPP.printCharArray (aOstr, n);
        aOstr.println (";");
      }
      aOstr.println ("  static JJString jjtNodeName[] = {");
      for (int i = 0; i < aNodeNames.size (); i++)
      {
        aOstr.println ("jjtNodeName_arr_" + i + ", ");
      }
      aOstr.println ("  };");

      if (bHasNamespace)
      {
        aOstr.println (Options.stringValue ("NAMESPACE_CLOSE"));
      }

      aOstr.println ("#endif");
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  static String getVisitorClass ()
  {
    return PGCCContext.current ().jjtree ().getParserName () + "Visitor";
  }

  private static String _getVisitMethodName (@NonNull final String sClassName)
  {
    final StringBuilder aSB = new StringBuilder ("visit");
    if (Options.booleanValue ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME"))
    {
      aSB.append (Character.toUpperCase (sClassName.charAt (0)));
      for (int i = 1; i < sClassName.length (); i++)
      {
        aSB.append (sClassName.charAt (i));
      }
    }

    return aSB.toString ();
  }

  private static String _getVisitorArgumentType ()
  {
    final String sRet = Options.stringValue ("VISITOR_DATA_TYPE");
    return sRet == null || sRet.length () == 0 || sRet.equals ("Object") ? "void *" : sRet;
  }

  private static String _getVisitorReturnType ()
  {
    final String sRet = Options.stringValue ("VISITOR_RETURN_TYPE");
    return sRet == null || sRet.length () == 0 || sRet.equals ("Object") ? "void " : sRet;
  }

  public static void generateVisitors ()
  {
    if (!JJTreeOptions.isVisitor ())
      return;

    final File aFile = new File (_getVisitorIncludeFile ());
    try (final OutputFile outputFile = new OutputFile (aFile); final PrintWriter aOstr = outputFile.getPrintWriter ())
    {
      generatePrologue ();
      aOstr.println ("#ifndef " + aFile.getName ().replace ('.', '_').toUpperCase (Locale.US));
      aOstr.println ("#define " + aFile.getName ().replace ('.', '_').toUpperCase (Locale.US));
      aOstr.println ("\n#include \"JavaCC.h\"");
      aOstr.println ("#include \"" + PGCCContext.current ().jjtree ().getParserName () + "Tree.h" + "\"");

      final boolean bHasNamespace = Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0;
      if (bHasNamespace)
      {
        aOstr.println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }

      _generateVisitorInterface (aOstr);
      _generateDefaultVisitor (aOstr);

      if (bHasNamespace)
      {
        aOstr.println (Options.stringValue ("NAMESPACE_CLOSE"));
      }

      aOstr.println ("#endif");
    }
    catch (final IOException aIoe)
    {
      throw new UncheckedIOException (aIoe);
    }
  }

  private static void _generateVisitorInterface (@NonNull final PrintWriter aOstr)
  {
    final String sName = getVisitorClass ();
    final List <String> aNodeNames = ASTNodeDescriptor.getNodeNames ();

    aOstr.println ("class " + sName);
    aOstr.println ("{");

    String sArgumentType = _getVisitorArgumentType ();
    final String sReturnType = _getVisitorReturnType ();
    if (StringHelper.isNotEmpty (JJTreeOptions.getVisitorDataType ()))
      sArgumentType = JJTreeOptions.getVisitorDataType ();

    aOstr.println ("  public:");

    aOstr.println ("  virtual " + sReturnType + " visit(const SimpleNode *node, " + sArgumentType + " data) = 0;");
    if (JJTreeOptions.isMulti ())
    {
      for (final String n : aNodeNames)
      {
        if (n.equals ("void"))
        {
          continue;
        }
        final String sNodeType = JJTreeOptions.getNodePrefix () + n;
        aOstr.println ("  virtual " +
                      sReturnType +
                      " " +
                      _getVisitMethodName (sNodeType) +
                      "(const " +
                      sNodeType +
                      " *node, " +
                      sArgumentType +
                      " data) = 0;");
      }
    }

    aOstr.println ("  virtual ~" + sName + "() { }");
    aOstr.println ("};");
  }

  static String defaultVisitorClass ()
  {
    return PGCCContext.current ().jjtree ().getParserName () + "DefaultVisitor";
  }

  private static void _generateDefaultVisitor (@NonNull final PrintWriter aOstr)
  {
    final String sClassName = defaultVisitorClass ();
    final List <String> aNodeNames = ASTNodeDescriptor.getNodeNames ();

    aOstr.println ("class " + sClassName + " : public " + getVisitorClass () + " {");

    final String sArgumentType = _getVisitorArgumentType ();
    final String sRet = _getVisitorReturnType ();

    aOstr.println ("public:");
    aOstr.println ("  virtual " + sRet + " defaultVisit(const SimpleNode *node, " + sArgumentType + " data) = 0;");
    // ostr.println(" node->childrenAccept(this, data);");
    // ostr.println(" return" + (ret.trim().equals("void") ? "" : " data") +
    // ";");
    // ostr.println(" }");

    aOstr.println ("  virtual " + sRet + " visit(const SimpleNode *node, " + sArgumentType + " data) {");
    aOstr.println ("    " + (sRet.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
    aOstr.println ("}");

    if (JJTreeOptions.isMulti ())
    {
      for (final String n : aNodeNames)
      {
        if (n.equals ("void"))
        {
          continue;
        }
        final String sNodeType = JJTreeOptions.getNodePrefix () + n;
        aOstr.println ("  virtual " +
                      sRet +
                      " " +
                      _getVisitMethodName (sNodeType) +
                      "(const " +
                      sNodeType +
                      " *node, " +
                      sArgumentType +
                      " data) {");
        aOstr.println ("    " + (sRet.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
        aOstr.println ("  }");
      }
    }
    aOstr.println ("  ~" + sClassName + "() { }");
    aOstr.println ("};");
  }

  public static void generateFile (@NonNull final OutputFile aOutputFile,
                                   final String sTemplate,
                                   final Map <String, Object> aOptions,
                                   final boolean bClose) throws IOException
  {
    final PrintWriter aOstr = aOutputFile.getPrintWriter ();
    generatePrologue ();

    final OutputFileGenerator aGenerator = new OutputFileGenerator (sTemplate, aOptions);
    aGenerator.generate (aOstr);
    if (bClose)
      aOstr.close ();
  }
}
