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
package com.helger.pgcc.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.pgcc.jjtree.JJTree;
import com.helger.pgcc.output.java.FilesJava;
import com.helger.pgcc.parser.Main;

/**
 * The characterization test for the generated output.
 * <p>
 * The only real specification this project has for "the generated code is correct" is the bytes it
 * emits today. This test hashes every file of a generator run and compares it with a manifest that
 * is checked in below <code>src/test/resources/golden</code>, so that a refactoring which changes a
 * single byte of output fails until somebody looks at the change and blesses it.
 * <p>
 * After a deliberate change to the code generation, re-bless the manifests with
 *
 * <pre>
 * mvn test -Dtest=GeneratedOutputGoldenTest -Dpgcc.golden.update=true
 * </pre>
 *
 * and review the resulting diff of the manifest files before committing it.
 *
 * @author Philip Helger
 */
@RunWith (Parameterized.class)
public final class GeneratedOutputGoldenTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (GeneratedOutputGoldenTest.class);

  /** Set to <code>true</code> to overwrite the manifests instead of comparing against them */
  private static final boolean UPDATE = Boolean.getBoolean ("pgcc.golden.update");

  private static final File GOLDEN_DIR = new File ("src/test/resources/golden");
  private static final File WORK_DIR = new File ("target/golden");

  /**
   * One generator invocation.
   *
   * @param sName
   *        Name of the case, also the manifest file name.
   * @param aGrammar
   *        The grammar file to generate from.
   * @param bJJTree
   *        <code>true</code> to run JJTree instead of JavaCC.
   * @param aOptions
   *        Command line options, without the output directory.
   */
  public record GoldenCase (String sName, File aGrammar, boolean bJJTree, String... aOptions)
  {
    @Override
    public String toString ()
    {
      return sName;
    }
  }

  private static void _addTemplateMatrix (final List <GoldenCase> aCases)
  {
    // The template variants that drift apart most easily
    final File aGrammar = new File ("src/test/resources/charstream/grammar.jj");
    for (final String sTemplate : new String [] { "classic", "modern" })
      for (final String sUnicode : new String [] { "true", "false" })
        for (final String sLineColumn : new String [] { "true", "false" })
          aCases.add (new GoldenCase ("stream-" + sTemplate + "-unicode" + sUnicode + "-linecol" + sLineColumn,
                                      aGrammar,
                                      false,
                                      "-JAVA_TEMPLATE_TYPE=" + sTemplate,
                                      "-JAVA_UNICODE_ESCAPE=" + sUnicode,
                                      "-KEEP_LINE_COLUMN=" + sLineColumn));

    for (final String sTemplate : new String [] { "classic", "modern" })
      aCases.add (new GoldenCase ("stream-" + sTemplate + "-charsequence",
                                  aGrammar,
                                  false,
                                  "-JAVA_TEMPLATE_TYPE=" + sTemplate,
                                  "-JAVA_CHAR_STREAM_TYPE=charsequence"));

    // Options that visibly change the emitted parser
    aCases.add (new GoldenCase ("options-tokenmanagerusesparser", aGrammar, false, "-TOKEN_MANAGER_USES_PARSER=true"));
    aCases.add (new GoldenCase ("options-usercharstream", aGrammar, false, "-USER_CHAR_STREAM=true"));
    aCases.add (new GoldenCase ("options-cachetokens", aGrammar, false, "-CACHE_TOKENS=true"));
    aCases.add (new GoldenCase ("options-debug", aGrammar, false, "-DEBUG_PARSER=true", "-DEBUG_TOKEN_MANAGER=true"));
    aCases.add (new GoldenCase ("options-noerrorreporting", aGrammar, false, "-ERROR_REPORTING=false"));
    aCases.add (new GoldenCase ("options-jdk15", aGrammar, false, "-JDK_VERSION=1.5"));
    aCases.add (new GoldenCase ("options-jdk17", aGrammar, false, "-JDK_VERSION=17"));
  }

  private static void _addJJTreeMatrix (final List <GoldenCase> aCases)
  {
    // The JJTree option matrix decides what the node classes look like and was not covered at all
    final File aGrammar = new File ("src/test/resources/issues/45/grammar.jjt");
    aCases.add (new GoldenCase ("jjtree-opt-default", aGrammar, true));
    aCases.add (new GoldenCase ("jjtree-opt-multi", aGrammar, true, "-MULTI=true"));
    aCases.add (new GoldenCase ("jjtree-opt-visitor", aGrammar, true, "-VISITOR=true"));
    aCases.add (new GoldenCase ("jjtree-opt-multi-visitor",
                                aGrammar,
                                true,
                                "-MULTI=true",
                                "-VISITOR=true",
                                "-VISITOR_RETURN_TYPE=String",
                                "-VISITOR_DATA_TYPE=Object"));
    aCases.add (new GoldenCase ("jjtree-opt-tracktokens", aGrammar, true, "-TRACK_TOKENS=true"));
    aCases.add (new GoldenCase ("jjtree-opt-defaultvoid", aGrammar, true, "-NODE_DEFAULT_VOID=true"));
    aCases.add (new GoldenCase ("jjtree-opt-prefix", aGrammar, true, "-NODE_PREFIX=Nd", "-MULTI=true"));
    aCases.add (new GoldenCase ("jjtree-opt-package", aGrammar, true, "-NODE_PACKAGE=com.example.nodes"));
    aCases.add (new GoldenCase ("jjtree-opt-usesparser", aGrammar, true, "-NODE_USES_PARSER=true"));
    aCases.add (new GoldenCase ("jjtree-opt-scopehook", aGrammar, true, "-NODE_SCOPE_HOOK=true"));
    aCases.add (new GoldenCase ("jjtree-opt-nonodefiles", aGrammar, true, "-BUILD_NODE_FILES=false"));
  }

  private static void _addGrammarCorpus (final List <GoldenCase> aCases)
  {
    final File [] aFiles = new File ("grammars").listFiles ();
    if (aFiles == null)
      return;

    // Sorted, so that the case list is stable across file systems
    final List <File> aSorted = new ArrayList <> (List.of (aFiles));
    aSorted.sort ((a, b) -> a.getName ().compareTo (b.getName ()));
    for (final File f : aSorted)
    {
      final String sName = f.getName ();
      if (sName.endsWith (".jj"))
        aCases.add (new GoldenCase ("grammar-" + FilenameHelper.getBaseName (f), f, false));
      else
        if (sName.endsWith (".jjt"))
          aCases.add (new GoldenCase ("jjtree-" + FilenameHelper.getBaseName (f), f, true));
    }
  }

  @Parameters (name = "{0}")
  public static Collection <Object []> parameters ()
  {
    final List <GoldenCase> aCases = new ArrayList <> ();
    _addTemplateMatrix (aCases);
    _addJJTreeMatrix (aCases);
    _addGrammarCorpus (aCases);

    final List <Object []> ret = new ArrayList <> (aCases.size ());
    for (final GoldenCase aCase : aCases)
      ret.add (new Object [] { aCase });
    return ret;
  }

  private final GoldenCase m_aCase;

  public GeneratedOutputGoldenTest (final GoldenCase aCase)
  {
    m_aCase = aCase;
  }

  @BeforeClass
  public static void beforeClass ()
  {
    // Always use this checkout's templates, not the bootstrap generator's resources
    FilesJava.setReadFromClassPath (false);
  }

  @AfterClass
  public static void afterClass ()
  {
    FilesJava.setReadFromClassPath (true);
  }

  @Test
  public void testGeneratedOutputIsUnchanged () throws Exception
  {
    final File aOutDir = new File (WORK_DIR, m_aCase.sName ());
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aOutDir);
    aOutDir.mkdirs ();

    final List <String> aArgs = new ArrayList <> ();
    aArgs.add ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath ());
    aArgs.addAll (List.of (m_aCase.aOptions ()));
    aArgs.add (m_aCase.aGrammar ().getAbsolutePath ());
    final String [] aArgArray = aArgs.toArray (new String [0]);

    final ESuccess eSuccess = m_aCase.bJJTree () ? new JJTree ().main (aArgArray) : Main.mainProgram (aArgArray);
    assertTrue ("Failed to generate " + m_aCase.sName (), eSuccess.isSuccess ());

    final GoldenManifest aActual = GoldenManifest.ofDirectory (aOutDir);
    assertTrue ("Nothing was generated for " + m_aCase.sName (), aActual.getFileCount () > 0);

    final File aManifest = new File (GOLDEN_DIR, m_aCase.sName () + ".sha256");
    if (UPDATE)
    {
      aActual.write (aManifest);
      LOGGER.info ("Blessed " + aActual.getFileCount () + " files for " + m_aCase.sName ());
      return;
    }

    final GoldenManifest aExpected = GoldenManifest.read (aManifest);
    assertTrue ("No golden manifest for " + m_aCase.sName () + " - run with -Dpgcc.golden.update=true to create it",
                aExpected != null);

    final String sDifferences = aActual.getDifferences (aExpected);
    assertEquals ("The generated output changed for " +
                  m_aCase.sName () +
                  ". If that is intended, re-bless with -Dpgcc.golden.update=true and review the manifest diff.\n" +
                  sDifferences,
                  "",
                  sDifferences);
  }
}
