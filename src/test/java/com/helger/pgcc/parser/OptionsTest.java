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
package com.helger.pgcc.parser;

import com.helger.pgcc.context.PGCCContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import com.helger.base.system.EJavaVersion;
import com.helger.base.system.SystemHelper;
import com.helger.pgcc.output.EOutputLanguage;

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
    PGCCContext.current ().errors ().reset ();
  }

  @Test
  public void testDefaults ()
  {
    assertEquals (43, Options.optionValues ().size ());

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

    assertEquals (Options.DEFAULT_JDK_VERSION, Options.getJdkVersion ());
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
  public void setOutputLanguageFromCommandLine ()
  {
    // Used to be ignored on the command line, because only the grammar file path assigned the
    // indirect language flag
    assertEquals (EOutputLanguage.JAVA, Options.getOutputLanguage ());

    Options.setCmdLineOption ("-OUTPUT_LANGUAGE=c++");
    assertEquals (EOutputLanguage.CPP, Options.getOutputLanguage ());

    beforeEach ();

    Options.setCmdLineOption ("-OUTPUT_LANGUAGE=java");
    assertEquals (EOutputLanguage.JAVA, Options.getOutputLanguage ());

    beforeEach ();

    // An unknown language is warned about and leaves the default in place
    Options.setCmdLineOption ("-OUTPUT_LANGUAGE=cobol");
    assertEquals (EOutputLanguage.JAVA, Options.getOutputLanguage ());
    assertEquals (1, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void setCppNamespaceFromCommandLine ()
  {
    Options.setCmdLineOption ("-NAMESPACE=foo::bar");
    assertTrue (Options.booleanValue (Options.NONUSER_OPTION__HAS_NAMESPACE));
    assertEquals ("foo {\nnamespace bar {", Options.stringValue (Options.NONUSER_OPTION__NAMESPACE_OPEN));
    assertEquals ("}\n}", Options.stringValue (Options.NONUSER_OPTION__NAMESPACE_CLOSE));
  }

  @Test
  public void setJdkVersion ()
  {
    assertEquals (Options.DEFAULT_JDK_VERSION, Options.getJdkVersion ());
    assertEquals (EJavaVersion.JDK_1_8, Options.getJdkVersion ());

    beforeEach ();

    // Version too old
    Options.setCmdLineOption ("JDK_VERSION=1.1");
    assertEquals (Options.DEFAULT_JDK_VERSION, Options.getJdkVersion ());

    beforeEach ();

    // Version too old
    Options.setCmdLineOption ("JDK_VERSION=1.4");
    assertEquals (Options.DEFAULT_JDK_VERSION, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.5");
    assertEquals (EJavaVersion.JDK_1_5, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.7");
    assertEquals (EJavaVersion.JDK_1_7, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.8");
    assertEquals (EJavaVersion.JDK_1_8, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=1.9");
    assertEquals (EJavaVersion.JDK_9, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=9");
    assertEquals (EJavaVersion.JDK_9, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=10");
    assertEquals (EJavaVersion.JDK_10, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=11");
    assertEquals (EJavaVersion.JDK_11, Options.getJdkVersion ());

    beforeEach ();

    // Used to silently fall back to the default, because the old EJDKVersion stopped at 14
    Options.setCmdLineOption ("JDK_VERSION=17");
    assertEquals (EJavaVersion.JDK_17, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=21");
    assertEquals (EJavaVersion.JDK_21, Options.getJdkVersion ());

    beforeEach ();

    Options.setCmdLineOption ("JDK_VERSION=25");
    assertEquals (EJavaVersion.JDK_25, Options.getJdkVersion ());

    beforeEach ();

    // The major version alone is accepted as well
    Options.setCmdLineOption ("JDK_VERSION=8");
    assertEquals (EJavaVersion.JDK_1_8, Options.getJdkVersion ());

    beforeEach ();

    // Ignore invalid JDK version
    Options.setCmdLineOption ("JDK_VERSION=2.0");
    assertEquals (Options.DEFAULT_JDK_VERSION, Options.getJdkVersion ());
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
    PGCCContext.current ().errors ().reset ();

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
  public void testAnOptionWhoseDefaultIsNullCanBeSetOnTheCommandLine ()
  {
    // PARSER_SUPER_CLASS and TOKEN_MANAGER_SUPER_CLASS are the only two options with a null
    // default, so setCmdLineOption had nothing to take the expected type from and threw a
    // NullPointerException instead of setting them. Setting them in the grammar file always
    // worked, because setInputFileOption checks for null first
    Options.setCmdLineOption ("-PARSER_SUPER_CLASS=MySuperParser");
    assertEquals ("MySuperParser", Options.stringValue (Options.USEROPTION__PARSER_SUPER_CLASS));

    Options.setCmdLineOption ("-TOKEN_MANAGER_SUPER_CLASS=MySuperTokenManager");
    assertEquals ("MySuperTokenManager", Options.stringValue (Options.USEROPTION__TOKEN_MANAGER_SUPER_CLASS));
  }

  @Test
  public void testTheCppOnlyOptionsWarnWhenSetForJava ()
  {
    // Both are read by the C++ backend only, and used to be ignored without a word
    PGCCContext.current ().errors ().reset ();
    Options.setCmdLineOption ("-PARSER_SUPER_CLASS=MySuperParser");
    Options.normalize ();
    assertEquals (1, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void testTheCppOnlyOptionsDoNotWarnForCpp ()
  {
    PGCCContext.current ().errors ().reset ();
    Options.setCmdLineOption ("-OUTPUT_LANGUAGE=c++");
    Options.setCmdLineOption ("-PARSER_SUPER_CLASS=MySuperParser");
    Options.normalize ();
    assertEquals (0, JavaCCErrors.getWarningCount ());
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
    final String [] aOptions = { Options.USEROPTION__CACHE_TOKENS, Options.USEROPTION__IGNORE_CASE };
    final String sOptionString = Options.getOptionsString (aOptions);
    assertEquals ("CACHE_TOKENS=false,IGNORE_CASE=true", sOptionString);
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
