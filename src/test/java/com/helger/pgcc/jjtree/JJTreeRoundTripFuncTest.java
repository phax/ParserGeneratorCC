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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileOperationManager;
import com.helger.pgcc.output.java.FilesJava;

/**
 * Runs a <code>.jjt</code> through JJTree, the resulting <code>.jj</code> through the parser
 * generator, compiles everything and builds an actual tree.
 * <p>
 * JJTree had no test that got as far as running its output. The golden files watch the emitted
 * bytes of the node classes and the option matrix behind them, but nothing checked that a generated
 * tree builder produces the tree the grammar describes.
 * <p>
 * The grammar is built around conditional node creation - <code>#Sum(&gt;1)</code> and
 * <code>#Product(&gt;1)</code> - so the shape of the tree depends on the input and not on the
 * grammar alone. That is the part of JJTree with actual logic in it: an unconditional node would
 * look the same however wrong the child counting was.
 *
 * @author Philip Helger
 */
public final class JJTreeRoundTripFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (JJTreeRoundTripFuncTest.class);
  private static final File GRAMMAR = new File ("src/test/resources/roundtrip/tree.jjt");
  private static final File OUT_DIR = new File ("target/roundtrip-jjtree").getAbsoluteFile ();

  private static URLClassLoader s_aCL;
  private static Class <?> s_aParserClass;
  private static Method s_aStart;
  private static Method s_aGetNumChildren;
  private static Method s_aGetChild;
  private static Method s_aGetValue;

  private static void _compile (final File aDir)
  {
    final JavaCompiler aCompiler = ToolProvider.getSystemJavaCompiler ();
    assertNotNull ("No JDK compiler available - this test needs a JDK, not a JRE", aCompiler);

    final List <String> aArgs = new ArrayList <> ();
    aArgs.add ("-nowarn");
    aArgs.add ("-d");
    aArgs.add (aDir.getAbsolutePath ());
    for (final File aFile : aDir.listFiles ())
      if (aFile.getName ().endsWith (".java"))
        aArgs.add (aFile.getAbsolutePath ());

    final ByteArrayOutputStream aErr = new ByteArrayOutputStream ();
    final int nRet = aCompiler.run (null,
                                    null,
                                    new PrintStream (aErr, true, StandardCharsets.UTF_8),
                                    aArgs.toArray (new String [0]));
    if (nRet != 0)
      fail ("Generated code does not compile:\n" + aErr.toString (StandardCharsets.UTF_8));
  }

  @BeforeClass
  public static void beforeClass () throws Exception
  {
    // Use the templates from this checkout, not the ones in an older jar on the class path
    FilesJava.setReadFromClassPath (false);

    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (OUT_DIR);
    FileOperationManager.INSTANCE.createDirRecursive (OUT_DIR);

    // 1. the .jjt becomes a .jj plus the node classes
    LOGGER.info ("Running JJTree on " + GRAMMAR.getName ());
    final ESuccess eTree = new JJTree ().main (new String [] { "-OUTPUT_DIRECTORY=" + OUT_DIR.getAbsolutePath (),
                                                               GRAMMAR.getAbsolutePath () });
    assertTrue ("JJTree failed", eTree.isSuccess ());

    final File aGeneratedGrammar = new File (OUT_DIR, "tree.jj");
    assertTrue ("JJTree did not write " + aGeneratedGrammar, aGeneratedGrammar.isFile ());

    // 2. the .jj becomes the parser
    LOGGER.info ("Running the parser generator on " + aGeneratedGrammar.getName ());
    final ESuccess eParser = com.helger.pgcc.parser.Main.mainProgram ("-OUTPUT_DIRECTORY=" + OUT_DIR.getAbsolutePath (),
                                                                      aGeneratedGrammar.getAbsolutePath ());
    assertTrue ("Parser generation failed", eParser.isSuccess ());

    // 3. all of it has to compile
    _compile (OUT_DIR);

    s_aCL = new URLClassLoader (new java.net.URL [] { OUT_DIR.toURI ().toURL () },
                                JJTreeRoundTripFuncTest.class.getClassLoader ());
    s_aParserClass = s_aCL.loadClass ("TreeParser");
    final Class <?> aNodeClass = s_aCL.loadClass ("Node");
    final Class <?> aSimpleNodeClass = s_aCL.loadClass ("SimpleNode");
    s_aStart = s_aParserClass.getMethod ("Start");
    s_aGetNumChildren = aNodeClass.getMethod ("jjtGetNumChildren");
    s_aGetChild = aNodeClass.getMethod ("jjtGetChild", int.class);
    s_aGetValue = aSimpleNodeClass.getMethod ("jjtGetValue");
  }

  @AfterClass
  public static void afterClass () throws Exception
  {
    FilesJava.setReadFromClassPath (true);
    if (s_aCL != null)
      s_aCL.close ();
  }

  /**
   * Render the tree as <code>Name(Child,Child)</code>, with the value of a leaf in brackets.
   *
   * @param aNode
   *        The node to render. May not be <code>null</code>.
   * @return The rendered tree. Never <code>null</code>.
   * @throws Exception
   *         On reflection error
   */
  private static String _renderRecursive (final Object aNode) throws Exception
  {
    final StringBuilder aSB = new StringBuilder (aNode.toString ());
    final Object aValue = s_aGetValue.invoke (aNode);
    if (aValue != null)
      aSB.append ('[').append (aValue).append (']');

    final int nChildren = ((Integer) s_aGetNumChildren.invoke (aNode)).intValue ();
    if (nChildren > 0)
    {
      aSB.append ('(');
      for (int i = 0; i < nChildren; ++i)
      {
        if (i > 0)
          aSB.append (',');
        aSB.append (_renderRecursive (s_aGetChild.invoke (aNode, Integer.valueOf (i))));
      }
      aSB.append (')');
    }
    return aSB.toString ();
  }

  private static String _parse (final String sInput) throws Exception
  {
    final Object aParser = s_aParserClass.getConstructor (java.io.Reader.class)
                                         .newInstance (new java.io.StringReader (sInput));
    return _renderRecursive (s_aStart.invoke (aParser));
  }

  @Test
  public void testConditionalNodesAreCreatedWhenTheyHaveMoreThanOneChild () throws Exception
  {
    // Sum has two children so it becomes a node; the left Product has only one and does not
    assertEquals ("Start(Sum(Num[1],Product(Num[2],Num[3])))", _parse ("1 + 2 * 3"));
  }

  @Test
  public void testConditionalNodesAreSkippedWhenTheyHaveOneChild () throws Exception
  {
    // Neither Sum nor Product has more than one child, so the number is the only child of Start
    assertEquals ("Start(Num[1])", _parse ("1"));
  }

  @Test
  public void testParenthesesChangeTheShapeOfTheTree () throws Exception
  {
    assertEquals ("Start(Product(Sum(Num[1],Num[2]),Num[3]))", _parse ("(1 + 2) * 3"));
  }

  @Test
  public void testTheVisitorInterfaceIsGeneratedAndAccepted () throws Exception
  {
    // VISITOR=true adds jjtAccept to every node class; that it compiles is checked above, that it
    // dispatches is checked here
    final Class <?> aVisitorClass = s_aParserClass.getClassLoader ().loadClass ("TreeParserVisitor");
    final Object aTree = s_aStart.invoke (s_aParserClass.getConstructor (java.io.Reader.class)
                                                        .newInstance (new java.io.StringReader ("1 + 2")));

    final List <String> aVisited = new ArrayList <> ();
    final Object aVisitor = java.lang.reflect.Proxy.newProxyInstance (aVisitorClass.getClassLoader (),
                                                                      new Class <?> [] { aVisitorClass },
                                                                      (proxy, method, args) -> {
                                                                        if (!"visit".equals (method.getName ()))
                                                                          return null;
                                                                        final Object aNode = args[0];
                                                                        aVisited.add (method.getParameterTypes ()[0].getSimpleName ());
                                                                        final int n = ((Integer) s_aGetNumChildren.invoke (aNode)).intValue ();
                                                                        for (int i = 0; i < n; ++i)
                                                                        {
                                                                          final Object aChild = s_aGetChild.invoke (aNode,
                                                                                                                    Integer.valueOf (i));
                                                                          aChild.getClass ()
                                                                                .getMethod ("jjtAccept",
                                                                                            aVisitorClass,
                                                                                            Object.class)
                                                                                .invoke (aChild, proxy, null);
                                                                        }
                                                                        return null;
                                                                      });

    aTree.getClass ().getMethod ("jjtAccept", aVisitorClass, Object.class).invoke (aTree, aVisitor, null);

    // One overload per node type, so the dispatch has to pick the right one at every level
    assertEquals (List.of ("ASTStart", "ASTSum", "ASTNum", "ASTNum"), aVisited);
  }
}
