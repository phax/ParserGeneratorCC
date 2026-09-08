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
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.helger.base.state.ESuccess;
import com.helger.pgcc.parser.Main;

/** Executes the generated CharSequenceCharStream (see issue 21). */
@RunWith (Parameterized.class)
public final class CharSequenceCharStreamTest
{
  @Parameters (name = "{0}, lineColumn={1}")
  public static Collection <Object []> parameters ()
  {
    final List <Object []> aRet = new ArrayList <> ();
    for (final String sTemplate : new String [] { "classic", "modern" })
      for (final boolean bLineColumn : new boolean [] { true, false })
        aRet.add (new Object [] { sTemplate, Boolean.valueOf (bLineColumn) });
    return aRet;
  }

  private final String m_sTemplate;
  private final boolean m_bLineColumn;

  public CharSequenceCharStreamTest (final String sTemplate, final boolean bLineColumn)
  {
    m_sTemplate = sTemplate;
    m_bLineColumn = bLineColumn;
  }

  private File _generate (final String sPrefix, final String sCharStreamType) throws Exception
  {
    final File aRoot = new File ("target/charsequence");
    Files.createDirectories (aRoot.toPath ());
    final File aDir = Files.createTempDirectory (aRoot.toPath (), sPrefix + "-").toFile ();
    final File aGrammar = new File ("src/test/resources/charstream/grammar.jj");
    // Always use this checkout's templates, not the bootstrap generator's resources.
    FilesJava.setReadFromClassPath (false);
    try
    {
      assertEquals (ESuccess.SUCCESS,
                    Main.mainProgram ("-JDK_VERSION=1.8",
                                      "-JAVA_TEMPLATE_TYPE=" + m_sTemplate,
                                      "-JAVA_CHAR_STREAM_TYPE=" + sCharStreamType,
                                      "-KEEP_LINE_COLUMN=" + m_bLineColumn,
                                      "-OUTPUT_DIRECTORY=" + aDir.getAbsolutePath (),
                                      aGrammar.getAbsolutePath ()));
    }
    finally
    {
      FilesJava.setReadFromClassPath (true);
    }
    return aDir;
  }

  private static void _compile (final File aDir, final File... aExtraSources) throws Exception
  {
    final List <File> aSources = new ArrayList <> (Arrays.asList (aDir.listFiles (f -> f.getName ()
                                                                                        .endsWith (".java"))));
    aSources.addAll (Arrays.asList (aExtraSources));
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
  }

  @Test
  public void testGeneratedStream () throws Exception
  {
    final File aDir = _generate (m_sTemplate, "charsequence");

    // No AbstractCharStream is needed - there is no buffer to manage
    assertTrue ("CharSequenceCharStream must be generated", new File (aDir, "CharSequenceCharStream.java").exists ());
    assertTrue ("SimpleCharStream must not be generated", !new File (aDir, "SimpleCharStream.java").exists ());
    assertTrue ("AbstractCharStream must not be generated", !new File (aDir, "AbstractCharStream.java").exists ());

    // Adapt only the Reader/Provider constructors; everything else is generated code.
    final String sInput = "modern".equals (m_sTemplate) ? "new StreamProvider(r)" : "r";
    final String sAdapter = "class BufferTestStream extends CharSequenceCharStream {\n" +
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
    _compile (aDir, new File ("src/test/resources/charstream/CharSequenceChecks.java"));

    try (final URLClassLoader aLoader = new URLClassLoader (new URL [] { aDir.toURI ().toURL () },
                                                            getClass ().getClassLoader ()))
    {
      final Result aResult = JUnitCore.runClasses (Class.forName ("CharSequenceChecks", true, aLoader));
      final StringBuilder aFailures = new StringBuilder ();
      for (final Failure aFailure : aResult.getFailures ())
        aFailures.append (aFailure.getTestHeader ()).append ('\n').append (aFailure.getTrace ());
      assertTrue (aFailures.toString (), aResult.wasSuccessful ());
      assertEquals (8, aResult.getRunCount ());
    }
  }

  private List <String> _dumpTokens (final File aDir, final String sStreamName, final String sInput) throws Exception
  {
    try (final URLClassLoader aLoader = new URLClassLoader (new URL [] { aDir.toURI ().toURL () },
                                                            CharSequenceCharStreamTest.class.getClassLoader ()))
    {
      final Class <?> aStreamClass = Class.forName (sStreamName, true, aLoader);
      final Object aStream = aStreamClass.getConstructor (java.io.Reader.class, int.class, int.class)
                                         .newInstance (new StringReader (sInput),
                                                       Integer.valueOf (1),
                                                       Integer.valueOf (1));

      final Class <?> aTMClass = Class.forName ("BufferParserTokenManager", true, aLoader);
      Constructor <?> aTMCtor = null;
      for (final Constructor <?> aCtor : aTMClass.getConstructors ())
        if (aCtor.getParameterCount () == 1)
          aTMCtor = aCtor;
      assertNotNull (aTMCtor);
      final Object aTM = aTMCtor.newInstance (aStream);
      final Method aNext = aTMClass.getMethod ("getNextToken");
      final Class <?> aTokenClass = Class.forName ("Token", true, aLoader);

      final List <String> aRet = new ArrayList <> ();
      while (true)
      {
        final Object aToken = aNext.invoke (aTM);
        final int nKind = aTokenClass.getField ("kind").getInt (aToken);
        final StringBuilder aLine = new StringBuilder ().append (nKind)
                                                        .append ('|')
                                                        .append (aTokenClass.getField ("image").get (aToken));
        if (m_bLineColumn)
          aLine.append ('|')
               .append (aTokenClass.getField ("beginLine").getInt (aToken))
               .append (':')
               .append (aTokenClass.getField ("beginColumn").getInt (aToken))
               .append ('-')
               .append (aTokenClass.getField ("endLine").getInt (aToken))
               .append (':')
               .append (aTokenClass.getField ("endColumn").getInt (aToken));
        aRet.add (aLine.toString ());
        if (nKind == 0)
          break;
      }
      return aRet;
    }
  }

  @Test
  public void testTokenParityWithSimpleCharStream () throws Exception
  {
    // The Reader based reflection below only fits the classic templates
    Assume.assumeTrue ("classic".equals (m_sTemplate));

    final File aSimple = _generate ("simple", "simple");
    _compile (aSimple);
    final File aCharSeq = _generate ("charsequence", "charsequence");
    _compile (aCharSeq);

    final StringBuilder aLong = new StringBuilder ();
    for (int i = 0; i < 500; ++i)
      aLong.append ("word").append ((char) ('a' + i % 10)).append (i % 7 == 0 ? "\r\n" : " ");

    // Note: an empty input is not part of this - SimpleCharStream reports the EOF token at 0:0
    // because it reads back an untouched buffer slot
    for (final String sInput : new String [] { "abc", "abc;\r\ndef;\rghi;\njkl", "\t\tabc\t;\tdef",
                                               "\"a string\";\r\n\"another\"", aLong.toString (), _letters (4093) +
                                                                                                  ";" +
                                                                                                  _letters (8193),
                                               "\"" + _letters (12345) + "\";" + _letters (4096) })
    {
      assertEquals ("Token stream differs for an input of length " + sInput.length (),
                    _dumpTokens (aSimple, "SimpleCharStream", sInput),
                    _dumpTokens (aCharSeq, "CharSequenceCharStream", sInput));
    }
  }

  private static String _letters (final int nLength)
  {
    final StringBuilder aRet = new StringBuilder (nLength);
    for (int i = 0; i < nLength; ++i)
      aRet.append ((char) ('a' + i % 26));
    return aRet.toString ();
  }
}
