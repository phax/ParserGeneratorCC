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
package com.helger.pgcc.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import com.helger.base.system.SystemHelper;
import com.helger.pgcc.EJDKVersion;

/**
 * Test cases to prod at the valitity of Options a little.
 *
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public final class OptionsTest
{
  @Before
  public void beforeEach ()
  {
    Options.init ();
    JavaCCErrors.reInit ();
  }

  @Test
  public void testDefaults ()
  {
    assertEquals (44, Options.s_optionValues.size ());

    assertTrue (Options.isBuildParser ());
    assertTrue (Options.isBuildTokenManager ());
    assertFalse (Options.isCacheTokens ());
    assertFalse (Options.isCommonTokenAction ());
    assertFalse (Options.isDebugLookahead ());
    assertFalse (Options.isDebugParser ());
    assertFalse (Options.isDebugTokenManager ());
    assertTrue (Options.isErrorReporting ());
    assertFalse (Options.isForceLaCheck ());
    assertFalse (Options.isIgnoreCase ());
    assertFalse (Options.isJavaUnicodeEscape ());
    assertTrue (Options.isKeepLineColumn ());
    assertTrue (Options.isSanityCheck ());
    assertFalse (Options.isUnicodeInput ());
    assertFalse (Options.isJavaUserCharStream ());
    assertFalse (Options.isUserTokenManager ());
    assertFalse (Options.isTokenManagerUsesParser ());

    assertEquals (2, Options.getChoiceAmbiguityCheck ());
    assertEquals (1, Options.getLookahead ());
    assertEquals (1, Options.getOtherAmbiguityCheck ());

    assertEquals (EJDKVersion.DEFAULT, Options.getJdkVersion ());
    assertEquals (new File ("."), Options.getOutputDirectory ());
    assertEquals ("", Options.getTokenExtends ());
    assertEquals ("", Options.getTokenFactory ());
    assertEquals (SystemHelper.getSystemCharsetName (), Options.getGrammarEncoding ().name ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void setJdkVersion ()
  {
    assertEquals (EJDKVersion.DEFAULT, Options.getJdkVersion ());
    assertEquals (EJDKVersion.JDK_1_5, Options.getJdkVersion ());

    beforeEach ();

    // Version too old
    Options.setCmdLineOption ("JDK_VERSION=1.1");
    assertEquals (EJDKVersion.DEFAULT, Options.getJdkVersion ());

    beforeEach ();

    // Version too old
    Options.setCmdLineOption ("JDK_VERSION=1.4");
    assertEquals (EJDKVersion.DEFAULT, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.5");
    assertEquals (EJDKVersion.JDK_1_5, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.7");
    assertEquals (EJDKVersion.JDK_1_7, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.8");
    assertEquals (EJDKVersion.JDK_1_8, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.9");
    assertEquals (EJDKVersion.JDK_9, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=9");
    assertEquals (EJDKVersion.JDK_9, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=10");
    assertEquals (EJDKVersion.JDK_10, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=11");
    assertEquals (EJDKVersion.JDK_11, Options.getJdkVersion ());

    beforeEach ();

    // Ignore invalid JDK version
    Options.setCmdLineOption ("JDK_VERSION=2.0");
    assertEquals (EJDKVersion.DEFAULT, Options.getJdkVersion ());
    assertEquals (0, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void testSetBooleanOption ()
  {
    assertFalse (Options.isJavaUnicodeEscape ());
    Options.setCmdLineOption ("-JAVA_UNICODE_ESCAPE:true");
    assertTrue (Options.isJavaUnicodeEscape ());

    assertTrue (Options.isSanityCheck ());
    Options.setCmdLineOption ("-SANITY_CHECK=false");
    assertFalse (Options.isSanityCheck ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testIntBooleanOption ()
  {
    assertEquals (1, Options.getLookahead ());

    Options.setCmdLineOption ("LOOKAHEAD=2");
    assertEquals (2, Options.getLookahead ());
    assertEquals (0, JavaCCErrors.getWarningCount ());

    Options.setCmdLineOption ("LOOKAHEAD=0");
    assertEquals (2, Options.getLookahead ());
    assertEquals (0, JavaCCErrors.getWarningCount ());

    Options.setInputFileOption (null, null, Options.USEROPTION__LOOKAHEAD, Integer.valueOf (0));
    assertEquals (2, Options.getLookahead ());
    assertEquals (1, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testSetStringOption ()
  {
    assertEquals ("", Options.getTokenExtends ());
    Options.setCmdLineOption ("-TOKEN_EXTENDS=java.lang.Object");
    assertEquals ("java.lang.Object", Options.getTokenExtends ());
    Options.setInputFileOption (null, null, Options.USEROPTION__TOKEN_EXTENDS, "Object");
    // File option does not override cmd line
    assertEquals ("java.lang.Object", Options.getTokenExtends ());

    Options.init ();
    JavaCCErrors.reInit ();

    Options.setInputFileOption (null, null, Options.USEROPTION__TOKEN_EXTENDS, "Object");
    assertEquals ("Object", Options.getTokenExtends ());
    Options.setCmdLineOption ("-TOKEN_EXTENDS=java.lang.Object");
    assertEquals ("java.lang.Object", Options.getTokenExtends ());
  }

  @Test
  public void testSetNonexistentOption ()
  {
    assertEquals (0, JavaCCErrors.getWarningCount ());
    Options.setInputFileOption (null, null, "NONEXISTENTOPTION", Boolean.TRUE);
    assertEquals (1, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testSetWrongTypeForOption ()
  {
    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    Options.setInputFileOption (null, null, Options.USEROPTION__CACHE_TOKENS, Integer.valueOf (8));
    assertEquals (1, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testNormalize ()
  {
    assertFalse (Options.isDebugLookahead ());
    assertFalse (Options.isDebugParser ());

    Options.setCmdLineOption ("-DEBUG_LOOKAHEAD=TRUE");
    Options.normalize ();

    assertTrue (Options.isDebugLookahead ());
    assertTrue (Options.isDebugParser ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testOptionsString ()
  {
    Options.setCmdLineOption ("-CACHE_TOKENS=False");
    Options.setCmdLineOption ("-IGNORE_CASE=True");
    final String [] options = { Options.USEROPTION__CACHE_TOKENS, Options.USEROPTION__IGNORE_CASE };
    final String optionString = Options.getOptionsString (options);
    assertEquals ("CACHE_TOKENS=false,IGNORE_CASE=true", optionString);
  }

  @Test
  public void testOutputEncoding ()
  {
    Options.setCmdLineOption ("-OUTPUT_ENCODING=bla");
    assertEquals (StandardCharsets.UTF_8, Options.getOutputEncoding ());
    beforeEach ();

    Options.setCmdLineOption ("-OUTPUT_ENCODING=iso-8859-1");
    assertEquals (StandardCharsets.ISO_8859_1, Options.getOutputEncoding ());
    beforeEach ();

    Options.setCmdLineOption ("-OUTPUT_ENCODING=ISO8859-1");
    assertEquals (StandardCharsets.ISO_8859_1, Options.getOutputEncoding ());
    beforeEach ();
  }
}
