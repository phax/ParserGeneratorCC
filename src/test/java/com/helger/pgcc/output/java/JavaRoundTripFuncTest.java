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
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import com.helger.pgcc.parser.Main;

/**
 * Generates a parser from a grammar, compiles it with the JDK compiler and runs it.
 * <p>
 * The C++ backend had such a round trip; Java, the language this generator is actually used with,
 * did not. What existed was {@link JavaTemplateValidityFuncTest}, which parses the templates in
 * isolation, and the golden files, which prove the emitted bytes did not change but say nothing
 * about whether they are correct. Neither would notice a token manager that compiles and then
 * tokenizes wrongly.
 * <p>
 * The grammar is built around the cases where the string literal trie and the NFA have to agree:
 * literals that are prefixes of each other, IGNORE_CASE literals competing with a general
 * identifier of the same length, and a MORE production that runs in a second lexical state. The
 * matrix runs it through every Java char stream the options can select.
 *
 * @author Philip Helger
 */
public final class JavaRoundTripFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (JavaRoundTripFuncTest.class);
  private static final File GRAMMAR = new File ("src/test/resources/roundtrip/lexer.jj");
  private static final File WORK_DIR = new File ("target/roundtrip");

  /** Three lines, so that the line and column of a token can be asserted as well */
  private static final String INPUT = "select selector selecting\n" +
                                      "12 + ++ -> - abc\n" +
                                      "\"he\\\"llo\" // trailing comment\n";

  /** What the lexer must produce for {@link #INPUT}: the token constant name and the image */
  private static final String [] [] EXPECTED = { { "SELECT", "select" },
                                                 { "SELECTOR", "selector" },
                                                 { "IDENT", "selecting" },
                                                 { "NUMBER", "12" },
                                                 { "PLUS", "+" },
                                                 { "PLUSPLUS", "++" },
                                                 { "ARROW", "->" },
                                                 { "MINUS", "-" },
                                                 { "IDENT", "abc" },
                                                 { "STRING", "\"he\\\"llo\"" } };

  @BeforeClass
  public static void beforeClass ()
  {
    // Use the templates from this checkout, not the ones in an older jar on the class path
    FilesJava.setReadFromClassPath (false);
  }

  @AfterClass
  public static void afterClass ()
  {
    FilesJava.setReadFromClassPath (true);
  }

  /**
   * Compile every generated Java file with the JDK compiler.
   *
   * @param aDir
   *        Directory holding the generated sources. May not be <code>null</code>.
   */
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

  /**
   * Instantiate the generated parser. Which constructors exist depends on the options, so take the
   * first one that fits: CharSequence for the CharSequence stream, String for the modern templates,
   * Reader for everything else.
   *
   * @param aParserClass
   *        The generated parser class. May not be <code>null</code>.
   * @return The parser instance. Never <code>null</code>.
   * @throws Exception
   *         On reflection error
   */
  private static Object _newParser (final Class <?> aParserClass) throws Exception
  {
    for (final Class <?> aArgType : new Class <?> [] { CharSequence.class, String.class })
    {
      Constructor <?> aCtor = null;
      try
      {
        aCtor = aParserClass.getConstructor (aArgType);
      }
      catch (final NoSuchMethodException aEx)
      {
        // Not this one
      }
      if (aCtor != null)
        return aCtor.newInstance (INPUT);
    }
    return aParserClass.getConstructor (java.io.Reader.class).newInstance (new java.io.StringReader (INPUT));
  }

  private static void _runOneConfiguration (final String sName, final String... aOptions) throws Exception
  {
    LOGGER.info ("Round trip: " + sName);

    final File aOutDir = new File (WORK_DIR, sName).getAbsoluteFile ();
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aOutDir);
    FileOperationManager.INSTANCE.createDirRecursive (aOutDir);

    final List <String> aArgs = new ArrayList <> ();
    aArgs.add ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath ());
    for (final String sOption : aOptions)
      aArgs.add (sOption);
    aArgs.add (GRAMMAR.getAbsolutePath ());

    final ESuccess eSuccess = Main.mainProgram (aArgs.toArray (new String [0]));
    assertTrue (sName + ": generation failed", eSuccess.isSuccess ());

    _compile (aOutDir);

    try (final URLClassLoader aCL = new URLClassLoader (new java.net.URL [] { aOutDir.toURI ().toURL () },
                                                        JavaRoundTripFuncTest.class.getClassLoader ()))
    {
      final Class <?> aParserClass = aCL.loadClass ("RoundTripParser");
      final Class <?> aConstantsClass = aCL.loadClass ("RoundTripParserConstants");
      final Class <?> aTokenClass = aCL.loadClass ("Token");

      final Field aKind = aTokenClass.getField ("kind");
      final Field aImage = aTokenClass.getField ("image");
      final Field aBeginLine = aTokenClass.getField ("beginLine");
      final Field aBeginColumn = aTokenClass.getField ("beginColumn");

      final Object aParser = _newParser (aParserClass);
      final Method aGetNextToken = aParserClass.getMethod ("getNextToken");
      final int nEOF = aConstantsClass.getField ("EOF").getInt (null);

      int nIndex = 0;
      Object aLastToken = null;
      while (true)
      {
        final Object aToken = aGetNextToken.invoke (aParser);
        if (aKind.getInt (aToken) == nEOF)
          break;
        assertTrue (sName + ": more tokens than expected, the " + (nIndex + 1) + ". is '" + aImage.get (aToken) + "'",
                    nIndex < EXPECTED.length);

        final String sExpectedName = EXPECTED[nIndex][0];
        assertEquals (sName + ": token " + nIndex + " should be " + sExpectedName,
                      aConstantsClass.getField (sExpectedName).getInt (null),
                      aKind.getInt (aToken));
        assertEquals (sName + ": image of token " + nIndex + " (" + sExpectedName + ")",
                      EXPECTED[nIndex][1],
                      aImage.get (aToken));
        aLastToken = aToken;
        nIndex++;
      }
      assertEquals (sName + ": number of tokens", EXPECTED.length, nIndex);

      // The comment is a SPECIAL_TOKEN, so it never shows up in the token chain, and the string
      // spans a lexical state switch - both of which the line and column have to survive
      assertEquals (sName + ": line of the last token", 3, aBeginLine.getInt (aLastToken));
      assertEquals (sName + ": column of the last token", 1, aBeginColumn.getInt (aLastToken));
    }
  }

  @Test
  public void testClassicSimpleCharStream () throws Exception
  {
    _runOneConfiguration ("classic-simple");
  }

  @Test
  public void testClassicCharSequenceCharStream () throws Exception
  {
    _runOneConfiguration ("classic-charsequence", "-JAVA_CHAR_STREAM_TYPE=charsequence");
  }

  @Test
  public void testClassicJavaCharStream () throws Exception
  {
    _runOneConfiguration ("classic-unicode", "-JAVA_UNICODE_ESCAPE=true");
  }

  @Test
  public void testModernSimpleCharStream () throws Exception
  {
    _runOneConfiguration ("modern-simple", "-JAVA_TEMPLATE_TYPE=modern");
  }

  @Test
  public void testModernCharSequenceCharStream () throws Exception
  {
    _runOneConfiguration ("modern-charsequence", "-JAVA_TEMPLATE_TYPE=modern", "-JAVA_CHAR_STREAM_TYPE=charsequence");
  }
}
