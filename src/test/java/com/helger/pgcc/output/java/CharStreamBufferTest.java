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
package com.helger.pgcc.output.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.helger.base.state.ESuccess;
import com.helger.pgcc.parser.Main;

/** Executes the generated streams, including the modern Provider variants. */
@RunWith (Parameterized.class)
public final class CharStreamBufferTest
{
  @Parameters (name = "{0}, unicode={1}, lineColumn={2}")
  public static Collection <Object []> parameters ()
  {
    final List <Object []> aRet = new ArrayList <> ();
    for (final String sTemplate : new String [] { "classic", "modern" })
      for (final boolean bUnicode : new boolean [] { true, false })
        for (final boolean bLineColumn : new boolean [] { true, false })
          aRet.add (new Object [] { sTemplate, Boolean.valueOf (bUnicode), Boolean.valueOf (bLineColumn) });
    return aRet;
  }

  private final String m_sTemplate;
  private final boolean m_bUnicode;
  private final boolean m_bLineColumn;

  public CharStreamBufferTest (final String sTemplate, final boolean bUnicode, final boolean bLineColumn)
  {
    m_sTemplate = sTemplate;
    m_bUnicode = bUnicode;
    m_bLineColumn = bLineColumn;
  }

  @Test
  public void testGeneratedStreams () throws Exception
  {
    final File aRoot = new File ("target/charstream");
    Files.createDirectories (aRoot.toPath ());
    final File aDir = Files.createTempDirectory (aRoot.toPath (), m_sTemplate + "-").toFile ();
    final File aGrammar = new File ("src/test/resources/charstream/grammar.jj");
    // Always use this checkout's templates, not the bootstrap generator's resources.
    FilesJava.setReadFromClassPath (false);
    try
    {
      assertEquals (ESuccess.SUCCESS,
                    Main.mainProgram ("-JDK_VERSION=1.8",
                                      "-JAVA_TEMPLATE_TYPE=" + m_sTemplate,
                                      "-JAVA_UNICODE_ESCAPE=" + m_bUnicode,
                                      "-KEEP_LINE_COLUMN=" + m_bLineColumn,
                                      "-OUTPUT_DIRECTORY=" + aDir.getAbsolutePath (),
                                      aGrammar.getAbsolutePath ()));
    }
    finally
    {
      FilesJava.setReadFromClassPath (true);
    }

    // Adapt only the Reader/Provider constructors; all buffering is generated code.
    final String sInput = "modern".equals (m_sTemplate) ? "new StreamProvider(r)" : "r";
    final String sAdapter = "class BufferTestStream extends " +
                            (m_bUnicode ? "Java" : "Simple") +
                            "CharStream {\n" +
                            "  static final boolean UNICODE = " +
                            m_bUnicode +
                            ";\n" +
                            "  static final boolean LINE_COLUMN = " +
                            m_bLineColumn +
                            ";\n" +
                            "  BufferTestStream(java.io.Reader r) { super(" +
                            sInput +
                            ", 1, 1, 4096); }\n" +
                            "  void reset(java.io.Reader r, int size) { reInit(" +
                            sInput +
                            ", 3, 7, size); }\n" +
                            "}\n";
    Files.write (new File (aDir, "BufferTestStream.java").toPath (), sAdapter.getBytes (StandardCharsets.UTF_8));
    final List <File> aSources = new ArrayList <> (Arrays.asList (aDir.listFiles (f -> f.getName ()
                                                                                        .endsWith (".java"))));
    aSources.add (new File ("src/test/resources/charstream/BufferChecks.java"));
    final JavaCompiler aCompiler = ToolProvider.getSystemJavaCompiler ();
    assertNotNull ("A JDK is required to compile the generated streams", aCompiler);
    try (final StandardJavaFileManager aManager = aCompiler.getStandardFileManager (null, null, StandardCharsets.UTF_8))
    {
      assertTrue ("Generated sources must compile",
                  aCompiler.getTask (null,
                                     aManager,
                                     null,
                                     Arrays.asList ("-d",
                                                    aDir.getAbsolutePath (),
                                                    "-classpath",
                                                    System.getProperty ("java.class.path")),
                                     null,
                                     aManager.getJavaFileObjectsFromFiles (aSources)).call ().booleanValue ());
    }
    try (final URLClassLoader aLoader = new URLClassLoader (new URL [] { aDir.toURI ().toURL () },
                                                            getClass ().getClassLoader ()))
    {
      final Result aResult = JUnitCore.runClasses (Class.forName ("BufferChecks", true, aLoader));
      final StringBuilder aFailures = new StringBuilder ();
      for (final Failure aFailure : aResult.getFailures ())
        aFailures.append (aFailure.getTestHeader ()).append ('\n').append (aFailure.getTrace ());
      assertTrue (aFailures.toString (), aResult.wasSuccessful ());
      assertEquals (6, aResult.getRunCount ());
    }
  }
}
