/*
 * Copyright 2017-2025 Philip Helger, pgcc@helger.com
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
// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2006, Sun Microsystems, Inc.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.jjtree.ASTNodeDescriptor;
import com.helger.pgcc.jjtree.JJTreeGlobals;
import com.helger.pgcc.jjtree.JJTreeIO;
import com.helger.pgcc.jjtree.JJTreeOptions;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.utils.OutputFileGenerator;

@Immutable
public final class NodeFilesJava
{
  private NodeFilesJava ()
  {}

  /**
   * ID of the latest version (of JJTree) in which one of the Node classes was
   * modified.
   */
  private static final String s_nodeVersion = PGVersion.MAJOR_DOT_MINOR;

  private static final Set <String> s_nodesGenerated = new HashSet <> ();

  public static void ensure (final JJTreeIO io, final String nodeType)
  {
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), nodeType + ".java");

    if (nodeType.equals ("Node"))
    {
      // Nothing
    }
    else
      if (nodeType.equals ("SimpleNode"))
      {
        // Check super interface
        ensure (io, "Node");
      }
      else
      {
        // Whatever - SimpleNode is as deep as we can handle
        ensure (io, "SimpleNode");
      }

    /*
     * Only build the node file if we're dealing with Node.java, or the
     * NODE_BUILD_FILES option is set.
     */
    if (!(nodeType.equals ("Node") || JJTreeOptions.isBuildNodeFiles ()))
    {
      return;
    }

    if (file.exists () && s_nodesGenerated.contains (file.getName ()))
    {
      return;
    }

    final String [] options = new String [] { "MULTI",
                                              "NODE_USES_PARSER",
                                              "VISITOR",
                                              "TRACK_TOKENS",
                                              "NODE_PREFIX",
                                              "NODE_EXTENDS",
                                              "NODE_FACTORY",
                                              Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    try (final OutputFile outputFile = new OutputFile (file, s_nodeVersion, options))
    {
      outputFile.setToolName ("JJTree");

      s_nodesGenerated.add (file.getName ());

      if (!outputFile.needToWrite ())
        return;

      if (nodeType.equals ("Node"))
      {
        _generateNode_java (outputFile);
      }
      else
        if (nodeType.equals ("SimpleNode"))
        {
          _generateSimpleNode_java (outputFile);
        }
        else
        {
          _generateMultiNode_java (outputFile, nodeType);
        }
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  static void generatePrologue (final PrintWriter ostr)
  {
    // Output the node's package name. JJTreeGlobals.nodePackageName
    // will be the value of NODE_PACKAGE in OPTIONS; if that wasn't set it
    // will default to the parser's package name.
    // If the package names are different we will need to import classes
    // from the parser's package.
    if (StringHelper.isNotEmpty (JJTreeGlobals.s_nodePackageName))
    {
      ostr.println ("package " + JJTreeGlobals.s_nodePackageName + ";");
      ostr.println ();
      if (!JJTreeGlobals.s_nodePackageName.equals (JJTreeGlobals.s_packageName))
      {
        ostr.println ("import " + JJTreeGlobals.s_packageName + ".*;");
        ostr.println ();
      }
    }
  }

  public static String nodeConstants ()
  {
    return JJTreeGlobals.s_parserName + "TreeConstants";
  }

  public static void generateTreeConstants_java ()
  {
    final String name = nodeConstants ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), name + ".java");

    try (final OutputFile outputFile = new OutputFile (file); final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      final List <String> nodeIds = ASTNodeDescriptor.getNodeIds ();
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("public interface " + name);
      ostr.println ("{");

      for (int i = 0; i < nodeIds.size (); ++i)
      {
        final String n = nodeIds.get (i);
        ostr.println ("  public int " + n + " = " + i + ";");
      }

      ostr.println ();
      ostr.println ();

      ostr.println ("  public String[] jjtNodeName = {");
      for (int i = 0; i < nodeNames.size (); ++i)
      {
        final String n = nodeNames.get (i);
        ostr.println ("    \"" + n + "\",");
      }
      ostr.println ("  };");

      ostr.println ("}");
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  static String visitorClass ()
  {
    return JJTreeGlobals.s_parserName + "Visitor";
  }

  public static void generateVisitor_java ()
  {
    if (!JJTreeOptions.isVisitor ())
    {
      return;
    }

    final String name = visitorClass ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), name + ".java");

    try (final OutputFile outputFile = new OutputFile (file); final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("public interface " + name);
      ostr.println ("{");

      final String ve = _mergeVisitorException ();

      String argumentType;
      if (StringHelper.isNotEmpty (JJTreeOptions.getVisitorDataType ()))
        argumentType = JJTreeOptions.getVisitorDataType ();
      else
        argumentType = "Object";

      ostr.println ("  public " + JJTreeOptions.getVisitorReturnType () + " visit(SimpleNode node, " + argumentType + " data)" + ve + ";");
      if (JJTreeOptions.isMulti ())
      {
        for (int i = 0; i < nodeNames.size (); ++i)
        {
          final String n = nodeNames.get (i);
          if (n.equals ("void"))
          {
            continue;
          }
          final String nodeType = JJTreeOptions.getNodePrefix () + n;
          ostr.println ("  public " +
                        JJTreeOptions.getVisitorReturnType () +
                        " " +
                        _getVisitMethodName (nodeType) +
                        "(" +
                        nodeType +
                        " node, " +
                        argumentType +
                        " data)" +
                        ve +
                        ";");
        }
      }
      ostr.println ("}");
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  static String defaultVisitorClass ()
  {
    return JJTreeGlobals.s_parserName + "DefaultVisitor";
  }

  private static String _getVisitMethodName (final String className)
  {
    final StringBuilder sb = new StringBuilder ("visit");
    if (Options.booleanValue ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME"))
    {
      sb.append (Character.toUpperCase (className.charAt (0)));
      sb.append (className.substring (1));
    }
    return sb.toString ();
  }

  public static void generateDefaultVisitor_java ()
  {
    if (!JJTreeOptions.isVisitor ())
    {
      return;
    }

    final String className = defaultVisitorClass ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), className + ".java");

    try (final OutputFile outputFile = new OutputFile (file); final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("public class " + className + " implements " + visitorClass () + "{");

      final String ve = _mergeVisitorException ();

      String argumentType;
      if (StringHelper.isNotEmpty (JJTreeOptions.getVisitorDataType ()))
        argumentType = JJTreeOptions.getVisitorDataType ();
      else
        argumentType = "Object";

      final String ret = JJTreeOptions.getVisitorReturnType ();
      ostr.println ("  public " + ret + " defaultVisit(final SimpleNode node, final " + argumentType + " data)" + ve + "{");
      ostr.println ("    node.childrenAccept(this, data);");
      ostr.println ("    return" + (ret.trim ().equals ("void") ? "" : " data") + ";");
      ostr.println ("  }");

      ostr.println ("  public " + ret + " visit(final SimpleNode node, final " + argumentType + " data)" + ve + "{");
      ostr.println ("    " + (ret.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
      ostr.println ("  }");

      if (JJTreeOptions.isMulti ())
      {
        for (int i = 0; i < nodeNames.size (); ++i)
        {
          final String n = nodeNames.get (i);
          if (n.equals ("void"))
          {
            continue;
          }
          final String nodeType = JJTreeOptions.getNodePrefix () + n;
          ostr.println ("  public " +
                        ret +
                        " " +
                        _getVisitMethodName (nodeType) +
                        "(" +
                        nodeType +
                        " node, " +
                        argumentType +
                        " data)" +
                        ve +
                        "{");
          ostr.println ("    " + (ret.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
          ostr.println ("  }");
        }
      }
      ostr.println ("}");
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  private static String _mergeVisitorException ()
  {
    String ve = JJTreeOptions.getVisitorException ();
    if (StringHelper.isNotEmpty (ve))
      ve = " throws " + ve;
    return ve;
  }

  private static void _generateNode_java (final OutputFile outputFile) throws IOException
  {
    try (final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      generatePrologue (ostr);

      final Map <String, Object> options = Options.getAllOptions ();
      options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);

      final OutputFileGenerator generator = new OutputFileGenerator ("/templates/jjtree/java/Node.template", options);

      generator.generate (ostr);
    }
  }

  private static void _generateSimpleNode_java (final OutputFile outputFile) throws IOException
  {
    try (final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      generatePrologue (ostr);

      final Map <String, Object> options = Options.getAllOptions ();
      options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);
      options.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (JJTreeOptions.getVisitorReturnType ().equals ("void")));

      final OutputFileGenerator generator = new OutputFileGenerator ("/templates/jjtree/java/SimpleNode.template", options);

      generator.generate (ostr);
    }
  }

  private static void _generateMultiNode_java (final OutputFile outputFile, final String nodeType) throws IOException
  {
    try (final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      generatePrologue (ostr);

      final Map <String, Object> options = Options.getAllOptions ();
      options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);
      options.put ("NODE_TYPE", nodeType);
      options.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (JJTreeOptions.getVisitorReturnType ().equals ("void")));

      final OutputFileGenerator generator = new OutputFileGenerator ("/templates/jjtree/java/MultiNode.template", options);

      generator.generate (ostr);
    }
  }

}
