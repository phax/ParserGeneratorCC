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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.system.EJavaVersion;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.jjtree.JJTreeIO;
import com.helger.pgcc.jjtree.JJTreeOptions;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.parser.Options;

/**
 * Generate the State of a tree.
 */
@Immutable
public final class JJTreeStateJava
{
  private JJTreeStateJava ()
  {}

  public static void insertParserMembers (@NonNull final JJTreeIO aIo)
  {
    aIo.println ();
    aIo.println ("  protected " + _nameState () + " jjtree = new " + _nameState () + "();");
    aIo.println ();
  }

  @NonNull
  @Nonempty
  private static String _nameState ()
  {
    return "JJT" + PGCCContext.current ().jjtree ().getParserName () + "State";
  }

  public static void generateTreeState_java ()
  {
    final File aFile = new File (JJTreeOptions.getJJTreeOutputDirectory (), _nameState () + ".java");

    try (final OutputFile aOutputFile = new OutputFile (aFile); final PrintWriter aOstr = aOutputFile.getPrintWriter ())
    {
      NodeFilesJava.generatePrologue (aOstr);
      _insertState (aOstr);
    }
    catch (final IOException e)
    {
      throw new UncheckedIOException (e);
    }
  }

  private static void _insertState (@NonNull final PrintWriter aOstr)
  {
    final EJavaVersion eJavaVersion = Options.getJdkVersion ();
    final boolean bEmptyImplType = eJavaVersion.isNewerOrEqualsThan (EJavaVersion.JDK_1_7);

    aOstr.println ("public class " + _nameState () + " implements java.io.Serializable {");

    aOstr.println ("  private java.util.List<Node> nodes;");
    aOstr.println ("  private java.util.List<Integer> marks;");

    aOstr.println ();
    aOstr.println ("  /* number of nodes on stack */");
    aOstr.println ("  private int sp;");
    aOstr.println ("  /* current mark */");
    aOstr.println ("  private int mk;");
    aOstr.println ("  private boolean node_created;");
    aOstr.println ();
    aOstr.println ("  public " + _nameState () + "() {");

    aOstr.println ("    nodes = new java.util.ArrayList<" + (bEmptyImplType ? "" : "Node") + ">();");
    aOstr.println ("    marks = new java.util.ArrayList<" + (bEmptyImplType ? "" : "Integer") + ">();");

    aOstr.println ("    sp = 0;");
    aOstr.println ("    mk = 0;");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Determines whether the current node was actually closed and");
    aOstr.println ("     pushed.  This should only be called in the final user action of a");
    aOstr.println ("     node scope. */");
    aOstr.println ("  public boolean nodeCreated() {");
    aOstr.println ("    return node_created;");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Call this to reinitialize the node stack.  It is called");
    aOstr.println ("     automatically by the parser's ReInit() method. */");
    aOstr.println ("  public void reset() {");
    aOstr.println ("    nodes.clear();");
    aOstr.println ("    marks.clear();");
    aOstr.println ("    sp = 0;");
    aOstr.println ("    mk = 0;");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Returns the root node of the AST.  It only makes sense to call");
    aOstr.println ("     this after a successful parse. */");
    aOstr.println ("  public Node rootNode() {");
    aOstr.println ("    return nodes.get(0);");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Pushes a node on to the stack. */");
    aOstr.println ("  public void pushNode(Node n) {");
    aOstr.println ("    nodes.add(n);");
    aOstr.println ("    ++sp;");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Returns the node on the top of the stack, and remove it from the");
    aOstr.println ("     stack.  */");
    aOstr.println ("  public Node popNode() {");
    aOstr.println ("   --sp;");
    aOstr.println ("    if (sp < mk) {");
    aOstr.println ("      mk = marks.remove(marks.size()-1).intValue();");
    aOstr.println ("    }");
    aOstr.println ("    return nodes.remove(nodes.size()-1);");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Returns the node currently on the top of the stack. */");
    aOstr.println ("  public Node peekNode() {");
    aOstr.println ("    return nodes.get(nodes.size()-1);");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Returns the number of children on the stack in the current node");
    aOstr.println ("     scope. */");
    aOstr.println ("  public int nodeArity() {");
    aOstr.println ("    return sp - mk;");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* Parameter is currently unused. */");
    aOstr.println ("  public void clearNodeScope(@SuppressWarnings(\"unused\") final Node n) {");
    aOstr.println ("    while (sp > mk) {");
    aOstr.println ("      popNode();");
    aOstr.println ("    }");
    aOstr.println ("    mk = marks.remove(marks.size()-1).intValue();");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  public void openNodeScope(final Node n) {");
    aOstr.println ("    marks.add(Integer.valueOf(mk));");
    aOstr.println ("    mk = sp;");
    aOstr.println ("    n.jjtOpen();");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ("  /* A definite node is constructed from a specified number of");
    aOstr.println ("     children.  That number of nodes are popped from the stack and");
    aOstr.println ("     made the children of the definite node.  Then the definite node");
    aOstr.println ("     is pushed on to the stack. */");
    aOstr.println ("  public void closeNodeScope(final Node n, final int numIn) {");
    aOstr.println ("    mk = marks.remove(marks.size()-1).intValue();");
    aOstr.println ("    int num = numIn;");
    aOstr.println ("    while (num-- > 0) {");
    aOstr.println ("      Node c = popNode();");
    aOstr.println ("      c.jjtSetParent(n);");
    aOstr.println ("      n.jjtAddChild(c, num);");
    aOstr.println ("    }");
    aOstr.println ("    n.jjtClose();");
    aOstr.println ("    pushNode(n);");
    aOstr.println ("    node_created = true;");
    aOstr.println ("  }");
    aOstr.println ();
    aOstr.println ();
    aOstr.println ("  /* A conditional node is constructed if its condition is true.  All");
    aOstr.println ("     the nodes that have been pushed since the node was opened are");
    aOstr.println ("     made children of the conditional node, which is then pushed");
    aOstr.println ("     on to the stack.  If the condition is false the node is not");
    aOstr.println ("     constructed and they are left on the stack. */");
    aOstr.println ("  public void closeNodeScope(final Node n, final boolean condition) {");
    aOstr.println ("    if (condition) {");
    aOstr.println ("      int a = nodeArity();");
    aOstr.println ("      mk = marks.remove(marks.size()-1).intValue();");
    aOstr.println ("      while (a-- > 0) {");
    aOstr.println ("        final Node c = popNode();");
    aOstr.println ("        c.jjtSetParent(n);");
    aOstr.println ("        n.jjtAddChild(c, a);");
    aOstr.println ("      }");
    aOstr.println ("      n.jjtClose();");
    aOstr.println ("      pushNode(n);");
    aOstr.println ("      node_created = true;");
    aOstr.println ("    } else {");
    aOstr.println ("      mk = marks.remove(marks.size()-1).intValue();");
    aOstr.println ("      node_created = false;");
    aOstr.println ("    }");
    aOstr.println ("  }");
    aOstr.println ("}");
  }
}
