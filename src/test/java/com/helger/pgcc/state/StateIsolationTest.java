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
package com.helger.pgcc.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileOperationManager;
import com.helger.pgcc.golden.GoldenManifest;
import com.helger.pgcc.jjtree.JJTree;
import com.helger.pgcc.output.java.FilesJava;
import com.helger.pgcc.parser.Main;

/**
 * The generator used to keep its whole state in static fields, so the result of a run could depend
 * on what ran before it in the same JVM. This test generates a grammar, generates something else in
 * between, then generates the first grammar again and requires both results to be identical byte
 * for byte.
 * <p>
 * It is the guard rail for replacing the static state with a proper context: as long as it passes,
 * a generator run does not depend on its history.
 *
 * @author Philip Helger
 */
public final class StateIsolationTest
{
  private static final File WORK_DIR = new File ("target/state-isolation");

  @BeforeClass
  public static void beforeClass ()
  {
    FilesJava.setReadFromClassPath (false);
  }

  @AfterClass
  public static void afterClass ()
  {
    FilesJava.setReadFromClassPath (true);
  }

  private static GoldenManifest _generate (final String sRun,
                                           final File aGrammar,
                                           final boolean bJJTree,
                                           final String... aOptions) throws Exception
  {
    final File aOutDir = new File (WORK_DIR, sRun);
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aOutDir);
    aOutDir.mkdirs ();

    final List <String> aArgs = new ArrayList <> ();
    aArgs.add ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath ());
    aArgs.addAll (List.of (aOptions));
    aArgs.add (aGrammar.getAbsolutePath ());
    final String [] aArgArray = aArgs.toArray (new String [0]);

    final ESuccess eSuccess = bJJTree ? new JJTree ().main (aArgArray) : Main.mainProgram (aArgArray);
    assertTrue ("Failed to generate " + sRun, eSuccess.isSuccess ());
    return GoldenManifest.ofDirectory (aOutDir);
  }

  @Test
  public void testJavaCCRunIsIndependentOfHistory () throws Exception
  {
    final File aFirst = new File ("grammars/CParser.jj");
    final GoldenManifest aBefore = _generate ("first", aFirst, false);

    // Something with a completely different shape in between
    _generate ("between1", new File ("grammars/PlSql.jj"), false, "-STATIC=false", "-JAVA_UNICODE_ESCAPE=true");
    _generate ("between2", new File ("src/test/resources/charstream/grammar.jj"), false, "-JAVA_TEMPLATE_TYPE=modern");

    final GoldenManifest aAfter = _generate ("again", aFirst, false);
    assertEquals ("Generating CParser.jj a second time produced different output:\n" + aAfter.getDifferences (aBefore),
                  "",
                  aAfter.getDifferences (aBefore));
  }

  @Test
  public void testJJTreeRunIsIndependentOfHistory () throws Exception
  {
    // EcmaScript.jjt sets NODE_PACKAGE, JSONParser.jjt does not - a classic place for state to leak
    final File aFirst = new File ("grammars/JSONParser.jjt");
    final GoldenManifest aBefore = _generate ("jjt-first", aFirst, true);

    _generate ("jjt-between", new File ("grammars/EcmaScript.jjt"), true);

    final GoldenManifest aAfter = _generate ("jjt-again", aFirst, true);
    assertEquals ("Generating JSONParser.jjt a second time produced different output:\n" +
                  aAfter.getDifferences (aBefore),
                  "",
                  aAfter.getDifferences (aBefore));
  }

  @Test
  public void testJJTreeDoesNotCarryNodesIntoTheNextRun () throws Exception
  {
    // Alpha's only node type is Alfa, Beta's only node type is Bravo. NodeFilesCpp collected the
    // node types to emit in a static set that nothing ever cleared, so running Alpha first made
    // Beta's output contain ASTAlfa.h, ASTAlfa.cc and a reference to Alfa in BetaTree.h.
    //
    // Note why the two tests above cannot see this: they generate the same grammar twice and
    // compare, and a leak that adds the same extra node to both runs cancels out.
    _generate ("leak-alpha", new File ("src/test/resources/state/alpha.jjt"), true);
    _generate ("leak-beta", new File ("src/test/resources/state/beta.jjt"), true);

    final File aBetaDir = new File (WORK_DIR, "leak-beta");
    for (final File aFile : aBetaDir.listFiles ())
      assertTrue ("Beta's output must not contain " + aFile.getName () + ", which belongs to Alpha",
                  !aFile.getName ().contains ("Alfa"));

    final File aTreeHeader = new File (aBetaDir, "BetaTree.h");
    assertTrue (aTreeHeader + " was not generated", aTreeHeader.isFile ());
    final String sTreeHeader = java.nio.file.Files.readString (aTreeHeader.toPath (),
                                                               java.nio.charset.StandardCharsets.UTF_8);
    assertTrue ("BetaTree.h must not mention Alpha's node:\n" + sTreeHeader, !sTreeHeader.contains ("Alfa"));
  }

  @Test
  public void testJavaAfterCppIsIndependent () throws Exception
  {
    final File aJava = new File ("src/test/resources/charstream/grammar.jj");
    final GoldenManifest aBefore = _generate ("java-first", aJava, false);

    _generate ("cpp-between", new File ("src/test/resources/cpp/calc.jj"), false);

    final GoldenManifest aAfter = _generate ("java-again", aJava, false);
    assertEquals ("Generating Java output after C++ output produced different results:\n" +
                  aAfter.getDifferences (aBefore),
                  "",
                  aAfter.getDifferences (aBefore));
  }
}
