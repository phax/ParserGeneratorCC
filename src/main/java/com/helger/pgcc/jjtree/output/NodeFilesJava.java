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

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.jjtree.ASTNodeDescriptor;
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
   * ID of the latest version (of JJTree) in which one of the Node classes was modified.
   */
  private static final String NODE_VERSION = PGVersion.MAJOR_DOT_MINOR;

  public static void ensure (final JJTreeIO aIo, @NonNull final String sNodeType)
  {
    final File aFile = new File (JJTreeOptions.getJJTreeOutputDirectory (), sNodeType + ".java");

    if (sNodeType.equals ("Node"))
    {
      // Nothing
    }
    else
      if (sNodeType.equals ("SimpleNode"))
      {
        // Check super interface
        ensure (aIo, "Node");
      }
      else
      {
        // Whatever - SimpleNode is as deep as we can handle
        ensure (aIo, "SimpleNode");
      }

    /*
     * Only build the node file if we're dealing with Node.java, or the NODE_BUILD_FILES option is
     * set.
     */
    if (!(sNodeType.equals ("Node") || JJTreeOptions.isBuildNodeFiles ()))
    {
      return;
    }

    if (aFile.exists () && PGCCContext.current ().jjtree ().nodesGenerated ().contains (aFile.getName ()))
    {
      return;
    }

    final String [] aOptions = { "MULTI", "NODE_USES_PARSER", "VISITOR", "TRACK_TOKENS", "NODE_PREFIX", "NODE_EXTENDS",
                                "NODE_FACTORY", Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    try (final OutputFile aOutputFile = new OutputFile (aFile, NODE_VERSION, aOptions))
    {
      aOutputFile.setToolName ("JJTree");

      PGCCContext.current ().jjtree ().nodesGenerated ().add (aFile.getName ());

      if (!aOutputFile.needToWrite ())
        return;

      if (sNodeType.equals ("Node"))
      {
        _generateNode_java (aOutputFile);
      }
      else
        if (sNodeType.equals ("SimpleNode"))
        {
          _generateSimpleNode_java (aOutputFile);
        }
        else
        {
          _generateMultiNode_java (aOutputFile, sNodeType);
        }
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  static void generatePrologue (@NonNull final PrintWriter aOstr)
  {
    // Output the node's package name. JJTreeGlobals.nodePackageName
    // will be the value of NODE_PACKAGE in OPTIONS; if that wasn't set it
    // will default to the parser's package name.
    // If the package names are different we will need to import classes
    // from the parser's package.
    if (StringHelper.isNotEmpty (PGCCContext.current ().jjtree ().getNodePackageName ()))
    {
      aOstr.println ("package " + PGCCContext.current ().jjtree ().getNodePackageName () + ";");
      aOstr.println ();
      if (!PGCCContext.current ()
                      .jjtree ()
                      .getNodePackageName ()
                      .equals (PGCCContext.current ().jjtree ().getPackageName ()))
      {
        aOstr.println ("import " + PGCCContext.current ().jjtree ().getPackageName () + ".*;");
        aOstr.println ();
      }
    }
  }

  public static String nodeConstants ()
  {
    return PGCCContext.current ().jjtree ().getParserName () + "TreeConstants";
  }

  public static void generateTreeConstants_java ()
  {
    final String sName = nodeConstants ();
    final File aFile = new File (JJTreeOptions.getJJTreeOutputDirectory (), sName + ".java");

    try (final OutputFile outputFile = new OutputFile (aFile); final PrintWriter aOstr = outputFile.getPrintWriter ())
    {
      final List <String> aNodeIds = ASTNodeDescriptor.getNodeIds ();
      final List <String> aNodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (aOstr);
      aOstr.println ("public interface " + sName);
      aOstr.println ("{");

      for (int i = 0; i < aNodeIds.size (); ++i)
      {
        final String n = aNodeIds.get (i);
        aOstr.println ("  public int " + n + " = " + i + ";");
      }

      aOstr.println ();
      aOstr.println ();

      aOstr.println ("  public String[] jjtNodeName = {");
      for (final String n : aNodeNames)
      {
        aOstr.println ("    \"" + n + "\",");
      }
      aOstr.println ("  };");

      aOstr.println ("}");
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  static String visitorClass ()
  {
    return PGCCContext.current ().jjtree ().getParserName () + "Visitor";
  }

  public static void generateVisitor_java ()
  {
    if (!JJTreeOptions.isVisitor ())
    {
      return;
    }

    final String sName = visitorClass ();
    final File aFile = new File (JJTreeOptions.getJJTreeOutputDirectory (), sName + ".java");

    try (final OutputFile outputFile = new OutputFile (aFile); final PrintWriter aOstr = outputFile.getPrintWriter ())
    {
      final List <String> aNodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (aOstr);
      aOstr.println ("public interface " + sName);
      aOstr.println ("{");

      final String sVe = _mergeVisitorException ();

      String sArgumentType;
      if (StringHelper.isNotEmpty (JJTreeOptions.getVisitorDataType ()))
        sArgumentType = JJTreeOptions.getVisitorDataType ();
      else
        sArgumentType = "Object";

      aOstr.println ("  public " +
                    JJTreeOptions.getVisitorReturnType () +
                    " visit(SimpleNode node, " +
                    sArgumentType +
                    " data)" +
                    sVe +
                    ";");
      if (JJTreeOptions.isMulti ())
      {
        for (final String n : aNodeNames)
        {
          if (n.equals ("void"))
          {
            continue;
          }
          final String sNodeType = JJTreeOptions.getNodePrefix () + n;
          aOstr.println ("  public " +
                        JJTreeOptions.getVisitorReturnType () +
                        " " +
                        _getVisitMethodName (sNodeType) +
                        "(" +
                        sNodeType +
                        " node, " +
                        sArgumentType +
                        " data)" +
                        sVe +
                        ";");
        }
      }
      aOstr.println ("}");
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  static String defaultVisitorClass ()
  {
    return PGCCContext.current ().jjtree ().getParserName () + "DefaultVisitor";
  }

  private static String _getVisitMethodName (@NonNull final String sClassName)
  {
    final StringBuilder aSB = new StringBuilder ("visit");
    if (Options.booleanValue ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME"))
    {
      aSB.append (Character.toUpperCase (sClassName.charAt (0)));
      aSB.append (sClassName.substring (1));
    }
    return aSB.toString ();
  }

  public static void generateDefaultVisitor_java ()
  {
    if (!JJTreeOptions.isVisitor ())
    {
      return;
    }

    final String sClassName = defaultVisitorClass ();
    final File aFile = new File (JJTreeOptions.getJJTreeOutputDirectory (), sClassName + ".java");

    try (final OutputFile outputFile = new OutputFile (aFile); final PrintWriter aOstr = outputFile.getPrintWriter ())
    {
      final List <String> aNodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (aOstr);
      aOstr.println ("public class " + sClassName + " implements " + visitorClass () + "{");

      final String sVe = _mergeVisitorException ();

      String sArgumentType;
      if (StringHelper.isNotEmpty (JJTreeOptions.getVisitorDataType ()))
        sArgumentType = JJTreeOptions.getVisitorDataType ();
      else
        sArgumentType = "Object";

      final String sRet = JJTreeOptions.getVisitorReturnType ();
      aOstr.println ("  public " +
                    sRet +
                    " defaultVisit(final SimpleNode node, final " +
                    sArgumentType +
                    " data)" +
                    sVe +
                    "{");
      aOstr.println ("    node.childrenAccept(this, data);");
      aOstr.println ("    return" + (sRet.trim ().equals ("void") ? "" : " data") + ";");
      aOstr.println ("  }");

      aOstr.println ("  public " + sRet + " visit(final SimpleNode node, final " + sArgumentType + " data)" + sVe + "{");
      aOstr.println ("    " + (sRet.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
      aOstr.println ("  }");

      if (JJTreeOptions.isMulti ())
      {
        for (final String n : aNodeNames)
        {
          if (n.equals ("void"))
          {
            continue;
          }
          final String sNodeType = JJTreeOptions.getNodePrefix () + n;
          aOstr.println ("  public " +
                        sRet +
                        " " +
                        _getVisitMethodName (sNodeType) +
                        "(" +
                        sNodeType +
                        " node, " +
                        sArgumentType +
                        " data)" +
                        sVe +
                        "{");
          aOstr.println ("    " + (sRet.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
          aOstr.println ("  }");
        }
      }
      aOstr.println ("}");
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  private static String _mergeVisitorException ()
  {
    String sVe = JJTreeOptions.getVisitorException ();
    if (StringHelper.isNotEmpty (sVe))
      sVe = " throws " + sVe;
    return sVe;
  }

  private static void _generateNode_java (@NonNull final OutputFile aOutputFile) throws IOException
  {
    try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
    {
      generatePrologue (aOstr);

      final Map <String, Object> aOptions = Options.getAllOptions ();
      aOptions.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());

      final OutputFileGenerator aGenerator = new OutputFileGenerator ("/templates/java/jjtree/Node.template", aOptions);

      aGenerator.generate (aOstr);
    }
  }

  private static void _generateSimpleNode_java (@NonNull final OutputFile aOutputFile) throws IOException
  {
    try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
    {
      generatePrologue (aOstr);

      final Map <String, Object> aOptions = Options.getAllOptions ();
      aOptions.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
      aOptions.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (JJTreeOptions.getVisitorReturnType ().equals ("void")));

      final OutputFileGenerator aGenerator = new OutputFileGenerator ("/templates/java/jjtree/SimpleNode.template",
                                                                     aOptions);

      aGenerator.generate (aOstr);
    }
  }

  private static void _generateMultiNode_java (@NonNull final OutputFile aOutputFile, final String sNodeType) throws IOException
  {
    try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
    {
      generatePrologue (aOstr);

      final Map <String, Object> aOptions = Options.getAllOptions ();
      aOptions.put (Options.NONUSER_OPTION__PARSER_NAME, PGCCContext.current ().jjtree ().getParserName ());
      aOptions.put ("NODE_TYPE", sNodeType);
      aOptions.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (JJTreeOptions.getVisitorReturnType ().equals ("void")));

      final OutputFileGenerator aGenerator = new OutputFileGenerator ("/templates/java/jjtree/MultiNode.template",
                                                                     aOptions);

      aGenerator.generate (aOstr);
    }
  }

}
