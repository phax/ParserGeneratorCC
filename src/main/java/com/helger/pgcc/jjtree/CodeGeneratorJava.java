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
package com.helger.pgcc.jjtree;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.jjtree.output.JJTreeStateJava;
import com.helger.pgcc.jjtree.output.NodeFilesJava;
import com.helger.pgcc.parser.JavaCCGlobals;

public class CodeGeneratorJava extends DefaultJJTreeVisitor
{
  /** Default constructor. */
  public CodeGeneratorJava ()
  {}

  @Override
  public Object defaultVisit (final SimpleNode aNode, final Object aData)
  {
    visit ((JJTreeNode) aNode, aData);
    return null;
  }

  @Override
  public Object visit (@NonNull final ASTGrammar aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    aIo.println ("/*@bgen(jjtree) " +
                 JavaCCGlobals.getIdString (PGCCContext.current ().jjtree ().toolList (),
                                            new File (aIo.getOutputFilename ()).getName ()) +
                 " */");
    aIo.print ("/*@egen*/");

    return aNode.childrenAccept (this, aIo);
  }

  @Override
  public Object visit (@NonNull final ASTBNFAction aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    /*
     * Assume that this action requires an early node close, and then try to decide whether this
     * assumption is false. Do this by looking outwards through the enclosing expansion units. If we
     * ever find that we are enclosed in a unit which is not the final unit in a sequence we know
     * that an early close is not required.
     */

    final NodeScope aNs = NodeScope.getEnclosingNodeScope (aNode);
    if (aNs != null && !aNs.isVoid ())
    {
      boolean bNeedClose = true;
      final Node aSp = aNode.getScopingParent (aNs);

      JJTreeNode n = aNode;
      while (true)
      {
        final Node p = n.jjtGetParent ();
        if (p instanceof ASTBNFSequence || p instanceof ASTBNFTryBlock)
        {
          if (n.getOrdinal () != p.jjtGetNumChildren () - 1)
          {
            /* We're not the final unit in the sequence. */
            bNeedClose = false;
            break;
          }
        }
        else
          if (p instanceof ASTBNFZeroOrOne || p instanceof ASTBNFZeroOrMore || p instanceof ASTBNFOneOrMore)
          {
            bNeedClose = false;
            break;
          }
        if (p == aSp)
        {
          /* No more parents to look at. */
          break;
        }
        n = (JJTreeNode) p;
      }
      if (bNeedClose)
      {
        openJJTreeComment (aIo, null);
        aIo.println ();
        insertCloseNodeAction (aNs, aIo, getIndentation (aNode));
        closeJJTreeComment (aIo);
      }
    }

    return visit ((JJTreeNode) aNode, aIo);
  }

  @Override
  public Object visit (@NonNull final ASTBNFDeclaration aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    if (!aNode.getNodeScope ().isVoid ())
    {
      String sIndent = "";
      if (TokenUtils.hasTokens (aNode))
      {
        for (int i = 1; i < aNode.getFirstToken ().beginColumn; ++i)
        {
          sIndent += " ";
        }
      }
      else
      {
        sIndent = "  ";
      }

      openJJTreeComment (aIo, aNode.getNodeScope ().getNodeDescriptorText ());
      aIo.println ();
      insertOpenNodeCode (aNode.getNodeScope (), aIo, sIndent);
      closeJJTreeComment (aIo);
    }

    return visit ((JJTreeNode) aNode, aIo);
  }

  @Override
  public Object visit (@NonNull final ASTBNFNodeScope aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    if (aNode.getNodeScope ().isVoid ())
    {
      return visit ((JJTreeNode) aNode, aIo);
    }

    final String sIndent = getIndentation (aNode.getExpansionUnit ());

    openJJTreeComment (aIo, aNode.getNodeScope ().getNodeDescriptor ().getDescriptor ());
    aIo.println ();
    tryExpansionUnit (aNode.getNodeScope (), aIo, sIndent, aNode.getExpansionUnit ());
    return null;
  }

  @Override
  public Object visit (@NonNull final ASTCompilationUnit aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    Token t = aNode.getFirstToken ();

    while (true)
    {
      if (t == PGCCContext.current ().jjtree ().getParserImports ())
      {
        // If the parser and nodes are in separate packages (NODE_PACKAGE
        // specified in
        // OPTIONS), then generate an import for the node package.
        if (StringHelper.isNotEmpty (PGCCContext.current ().jjtree ().getNodePackageName ()) &&
            !PGCCContext.current ()
                        .jjtree ()
                        .getNodePackageName ()
                        .equals (PGCCContext.current ().jjtree ().getPackageName ()))
        {
          aIo.getOut ().println ();
          aIo.getOut ().println ("import " + PGCCContext.current ().jjtree ().getNodePackageName () + ".*;");
        }
      }

      if (t == PGCCContext.current ().jjtree ().getParserImplements ())
      {
        if (t.image.equals ("implements"))
        {
          aNode.print (t, aIo);
          openJJTreeComment (aIo, null);
          aIo.getOut ().print (" " + NodeFilesJava.nodeConstants () + ", ");
          closeJJTreeComment (aIo);
        }
        else
        {
          // t is pointing at the opening brace of the class body.
          openJJTreeComment (aIo, null);
          aIo.getOut ().print ("implements " + NodeFilesJava.nodeConstants ());
          closeJJTreeComment (aIo);
          aNode.print (t, aIo);
        }
      }
      else
      {
        aNode.print (t, aIo);
      }

      if (t == PGCCContext.current ().jjtree ().getParserClassBodyStart ())
      {
        openJJTreeComment (aIo, null);
        JJTreeStateJava.insertParserMembers (aIo);
        closeJJTreeComment (aIo);
      }

      if (t == aNode.getLastToken ())
      {
        return null;
      }
      t = t.next;
    }
  }

  @Override
  public Object visit (@NonNull final ASTExpansionNodeScope aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    final String sIndent = getIndentation (aNode.getExpansionUnit ());
    openJJTreeComment (aIo, aNode.getNodeScope ().getNodeDescriptor ().getDescriptor ());
    aIo.println ();
    insertOpenNodeAction (aNode.getNodeScope (), aIo, sIndent);
    tryExpansionUnit (aNode.getNodeScope (), aIo, sIndent, aNode.getExpansionUnit ());

    // Print the "whiteOut" equivalent of the Node descriptor to preserve
    // line numbers in the generated file.
    ((ASTNodeDescriptor) aNode.jjtGetChild (1)).jjtAccept (this, aIo);
    return null;
  }

  @Override
  public Object visit (@NonNull final ASTJavacodeBody aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    if (aNode.getNodeScope ().isVoid ())
    {
      return visit ((JJTreeNode) aNode, aIo);
    }

    final Token aFirst = aNode.getFirstToken ();

    String sIndent = "";
    for (int i = 4; i < aFirst.beginColumn; ++i)
    {
      sIndent += " ";
    }

    openJJTreeComment (aIo, aNode.getNodeScope ().getNodeDescriptorText ());
    aIo.println ();
    insertOpenNodeCode (aNode.getNodeScope (), aIo, sIndent);
    tryTokenSequence (aNode.getNodeScope (), aIo, sIndent, aFirst, aNode.getLastToken ());
    return null;
  }

  public Object visit (@NonNull final ASTLHS aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    final NodeScope aNs = NodeScope.getEnclosingNodeScope (aNode);

    /*
     * Print out all the tokens, converting all references to `jjtThis' into the current node
     * variable.
     */
    final Token aFirst = aNode.getFirstToken ();
    final Token aLast = aNode.getLastToken ();
    for (Token t = aFirst; t != aLast.next; t = t.next)
    {
      TokenUtils.print (t, aIo, "jjtThis", aNs.getNodeVariable ());
    }

    return null;
  }

  /*
   * This method prints the tokens corresponding to this node recursively calling the print methods
   * of its children. Overriding this print method in appropriate nodes gives the output the added
   * stuff not in the input.
   */

  public Object visit (@NonNull final JJTreeNode aNode, final Object aData)
  {
    final JJTreeIO aIo = (JJTreeIO) aData;
    /*
     * Some productions do not consume any tokens. In that case their first and last tokens are a
     * bit strange.
     */
    if (aNode.getLastToken ().next == aNode.getFirstToken ())
    {
      return null;
    }

    final Token aT1 = aNode.getFirstToken ();
    Token t = new Token ();
    t.next = aT1;
    JJTreeNode n;
    for (int nOrd = 0; nOrd < aNode.jjtGetNumChildren (); nOrd++)
    {
      n = (JJTreeNode) aNode.jjtGetChild (nOrd);
      while (true)
      {
        t = t.next;
        if (t == n.getFirstToken ())
          break;
        aNode.print (t, aIo);
      }
      n.jjtAccept (this, aIo);
      t = n.getLastToken ();
    }
    while (t != aNode.getLastToken ())
    {
      t = t.next;
      aNode.print (t, aIo);
    }

    return null;
  }

  static void openJJTreeComment (@NonNull final JJTreeIO aIo, @Nullable final String sArg)
  {
    if (sArg != null)
    {
      aIo.print ("/*@bgen(jjtree) " + sArg + " */");
    }
    else
    {
      aIo.print ("/*@bgen(jjtree)*/");
    }
  }

  static void closeJJTreeComment (@NonNull final JJTreeIO aIo)
  {
    aIo.print ("/*@egen*/");
  }

  String getIndentation (final JJTreeNode n)
  {
    return getIndentation (n, 0);
  }

  String getIndentation (@NonNull final JJTreeNode n, final int nOffset)
  {
    String s = "";
    for (int i = nOffset + 1; i < n.getFirstToken ().beginColumn; ++i)
    {
      s += " ";
    }
    return s;
  }

  void insertOpenNodeDeclaration (final NodeScope aNs, final JJTreeIO aIo, final String sIndent)
  {
    insertOpenNodeCode (aNs, aIo, sIndent);
  }

  void insertOpenNodeCode (@NonNull final NodeScope aNs, @NonNull final JJTreeIO aIo, final String sIndent)
  {
    final String sType = aNs.getNodeDescriptor ().getNodeType ();
    final String sNodeClass;
    if (JJTreeOptions.getNodeClass ().length () > 0 && !JJTreeOptions.isMulti ())
    {
      sNodeClass = JJTreeOptions.getNodeClass ();
    }
    else
    {
      sNodeClass = sType;
    }

    /*
     * Ensure that there is a template definition file for the node type.
     */
    NodeFilesJava.ensure (aIo, sType);

    aIo.print (sIndent + sNodeClass + " " + aNs.getNodeVar () + " = ");
    final String sParserArg = JJTreeOptions.isNodeUsesParser () ? ("this, ") : "";

    if (JJTreeOptions.getNodeFactory ().equals ("*"))
    {
      // Old-style multiple-implementations.
      aIo.println ("(" +
                   sNodeClass +
                   ")" +
                   sNodeClass +
                   ".jjtCreate(" +
                   sParserArg +
                   aNs.getNodeDescriptor ().getNodeId () +
                   ");");
    }
    else
      if (JJTreeOptions.getNodeFactory ().length () > 0)
      {
        aIo.println ("(" +
                     sNodeClass +
                     ")" +
                     JJTreeOptions.getNodeFactory () +
                     ".jjtCreate(" +
                     sParserArg +
                     aNs.getNodeDescriptor ().getNodeId () +
                     ");");
      }
      else
      {
        aIo.println ("new " + sNodeClass + "(" + sParserArg + aNs.getNodeDescriptor ().getNodeId () + ");");
      }

    if (aNs.usesCloseNodeVar ())
    {
      aIo.println (sIndent + "boolean " + aNs.getClosedVar () + " = true;");
    }
    aIo.println (sIndent + aNs.getNodeDescriptor ().openNode (aNs.getNodeVar ()));
    if (JJTreeOptions.isNodeScopeHook ())
    {
      aIo.println (sIndent + "jjtreeOpenNodeScope(" + aNs.getNodeVar () + ");");
    }

    if (JJTreeOptions.isTrackTokens ())
    {
      aIo.println (sIndent + aNs.getNodeVar () + ".jjtSetFirstToken(getToken(1));");
    }
  }

  void insertCloseNodeCode (@NonNull final NodeScope aNs,
                            @NonNull final JJTreeIO aIo,
                            final String sIndent,
                            final boolean bIsFinal)
  {
    final String sCloseNode = aNs.getNodeDescriptor ().closeNode (aNs.getNodeVar ());
    aIo.println (sIndent + sCloseNode);
    if (aNs.usesCloseNodeVar () && !bIsFinal)
    {
      aIo.println (sIndent + aNs.getClosedVar () + " = false;");
    }
    if (JJTreeOptions.isNodeScopeHook ())
    {
      aIo.println (sIndent + "if (jjtree.nodeCreated()) {");
      aIo.println (sIndent + " jjtreeCloseNodeScope(" + aNs.getNodeVar () + ");");
      aIo.println (sIndent + "}");
    }

    if (JJTreeOptions.isTrackTokens ())
    {
      aIo.println (sIndent + aNs.getNodeVar () + ".jjtSetLastToken(getToken(0));");
    }
  }

  void insertOpenNodeAction (final NodeScope aNs, @NonNull final JJTreeIO aIo, final String sIndent)
  {
    aIo.println (sIndent + "{");
    insertOpenNodeCode (aNs, aIo, sIndent + "  ");
    aIo.println (sIndent + "}");
  }

  void insertCloseNodeAction (final NodeScope aNs, @NonNull final JJTreeIO aIo, final String sIndent)
  {
    aIo.println (sIndent + "{");
    insertCloseNodeCode (aNs, aIo, sIndent + "  ", false);
    aIo.println (sIndent + "}");
  }

  private void insertCatchBlocks (@NonNull final NodeScope aNs,
                                  @NonNull final JJTreeIO aIo,
                                  @NonNull final Collection <String> aThrown_names,
                                  final String sIndent)
  {
    if (!aThrown_names.isEmpty ())
    {
      aIo.println (sIndent + "} catch (Throwable " + aNs.getExceptionVar () + ") {");

      if (aNs.usesCloseNodeVar ())
      {
        aIo.println (sIndent + "  if (" + aNs.getClosedVar () + ") {");
        aIo.println (sIndent + "    jjtree.clearNodeScope(" + aNs.getNodeVar () + ");");
        aIo.println (sIndent + "    " + aNs.getClosedVar () + " = false;");
        aIo.println (sIndent + "  } else {");
        aIo.println (sIndent + "    jjtree.popNode();");
        aIo.println (sIndent + "  }");
      }

      for (final String thrown : aThrown_names)
      {
        aIo.println (sIndent + "  if (" + aNs.getExceptionVar () + " instanceof " + thrown + ") {");
        aIo.println (sIndent + "    throw (" + thrown + ")" + aNs.getExceptionVar () + ";");
        aIo.println (sIndent + "  }");
      }
      /*
       * This is either an Error or an undeclared Exception. If it's an Error then the cast is good,
       * otherwise we want to force the user to declare it by crashing on the bad cast.
       */
      aIo.println (sIndent + "  throw (Error)" + aNs.getExceptionVar () + ";");
    }
  }

  void tryTokenSequence (@NonNull final NodeScope aNs,
                         @NonNull final JJTreeIO aIo,
                         final String sIndent,
                         final Token aFirst,
                         @NonNull final Token aLast)
  {
    aIo.println (sIndent + "try {");
    closeJJTreeComment (aIo);

    /*
     * Print out all the tokens, converting all references to `jjtThis' into the current node
     * variable.
     */
    for (Token t = aFirst; t != aLast.next; t = t.next)
    {
      TokenUtils.print (t, aIo, "jjtThis", aNs.getNodeVar ());
    }

    openJJTreeComment (aIo, null);
    aIo.println ();

    insertCatchBlocks (aNs, aIo, aNs.getProduction ().getThrowsList (), sIndent);

    aIo.println (sIndent + "} finally {");
    if (aNs.usesCloseNodeVar ())
    {
      aIo.println (sIndent + "  if (" + aNs.getClosedVar () + ") {");
      insertCloseNodeCode (aNs, aIo, sIndent + "    ", true);
      aIo.println (sIndent + "  }");
    }
    aIo.println (sIndent + "}");
    closeJJTreeComment (aIo);
  }

  private static void findThrown (final NodeScope aNs,
                                  @NonNull final Map <String, String> aThrown_set,
                                  @NonNull final JJTreeNode aExpansion_unit)
  {
    if (aExpansion_unit instanceof ASTBNFNonTerminal)
    {
      /*
       * Should really make the nonterminal explicitly maintain its name.
       */
      final String sNt = aExpansion_unit.getFirstToken ().image;
      final ASTProduction aProd = PGCCContext.current ().jjtree ().productions ().get (sNt);
      if (aProd != null)
      {
        for (final String t : aProd.getThrowsList ())
          aThrown_set.put (t, t);
      }
    }
    for (int i = 0; i < aExpansion_unit.jjtGetNumChildren (); ++i)
    {
      final JJTreeNode n = (JJTreeNode) aExpansion_unit.jjtGetChild (i);
      findThrown (aNs, aThrown_set, n);
    }
  }

  void tryExpansionUnit (@NonNull final NodeScope aNs,
                         @NonNull final JJTreeIO aIo,
                         final String sIndent,
                         @NonNull final JJTreeNode aExpansion_unit)
  {
    aIo.println (sIndent + "try {");
    closeJJTreeComment (aIo);

    aExpansion_unit.jjtAccept (this, aIo);

    openJJTreeComment (aIo, null);
    aIo.println ();

    // Order for consistent output
    final Map <String, String> aThrown_set = new LinkedHashMap <> ();
    findThrown (aNs, aThrown_set, aExpansion_unit);
    insertCatchBlocks (aNs, aIo, aThrown_set.keySet (), sIndent);

    aIo.println (sIndent + "} finally {");
    if (aNs.usesCloseNodeVar ())
    {
      aIo.println (sIndent + "  if (" + aNs.getClosedVar () + ") {");
      insertCloseNodeCode (aNs, aIo, sIndent + "    ", true);
      aIo.println (sIndent + "  }");
    }
    aIo.println (sIndent + "}");
    closeJJTreeComment (aIo);
  }

}
