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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGPrinter.IPrinter;
import com.helger.pgcc.PGPrinter.PSPrinter;

/**
 * Test class for the grammar checks in {@link Semanticize}. They produce the error messages a user
 * sees for a broken grammar, and had no test at all - which meant an accidental change to a message
 * or to the condition behind it went unnoticed.
 *
 * @author Philip Helger
 */
public final class SemanticizeTest
{
  /** Collects what the generator prints, so the messages can be asserted on. */
  private static final class CollectingPrinter implements IPrinter
  {
    private final List <String> m_aLines = new ArrayList <> ();

    public void println (@NonNull final String s)
    {
      m_aLines.add (s);
    }

    public void flush ()
    {}

    public void close ()
    {}
  }

  private CollectingPrinter m_aPrinter;

  @Before
  public void before ()
  {
    m_aPrinter = new CollectingPrinter ();
    PGPrinter.init (m_aPrinter);
  }

  @After
  public void after ()
  {
    PGPrinter.init (new PSPrinter (System.out, false), new PSPrinter (System.err, false));
  }

  /**
   * Run the parser and the semantic checks over a grammar.
   *
   * @param sGrammar
   *        The grammar source. May not be <code>null</code>.
   * @return All lines that were printed while doing so. Never <code>null</code>.
   */
  @NonNull
  private List <String> _check (final String sGrammar)
  {
    Main.reInitAll ();
    try
    {
      final JavaCCParser aParser = new JavaCCParser (sGrammar);
      aParser.javacc_input ();
      Semanticize.start ();
    }
    catch (final Exception aEx)
    {
      m_aPrinter.println ("EXCEPTION: " + aEx.getMessage ());
    }
    return m_aPrinter.m_aLines;
  }

  private static String _grammar (final String sBody)
  {
    return "PARSER_BEGIN(T)\n" + "public class T {}\n" + "PARSER_END(T)\n" + sBody;
  }

  private static void _assertReported (final List <String> aLines, final String sExpectedFragment)
  {
    for (final String sLine : aLines)
      if (sLine.contains (sExpectedFragment))
        return;
    assertEquals ("Expected a message containing '" + sExpectedFragment + "' but got " + aLines, "", "not found");
  }

  @Test
  public void testValidGrammarHasNoErrors ()
  {
    final List <String> aLines = _check (_grammar ("TOKEN : { <A : \"a\"> }\n" + "void start() : {} { <A> }\n"));
    assertEquals (aLines.toString (), 0, JavaCCErrors.getErrorCount ());
    assertEquals (aLines.toString (), 0, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void testUndefinedNonTerminal ()
  {
    final List <String> aLines = _check (_grammar ("TOKEN : { <A : \"a\"> }\n" + "void start() : {} { missing() }\n"));
    assertTrue (JavaCCErrors.getErrorCount () > 0);
    _assertReported (aLines, "Non-terminal missing has not been defined");
  }

  @Test
  public void testLeftRecursion ()
  {
    final List <String> aLines = _check (_grammar ("TOKEN : { <A : \"a\"> }\n" +
                                                   "void start() : {} { loop() }\n" +
                                                   "void loop() : {} { loop() <A> }\n"));
    assertTrue (JavaCCErrors.getErrorCount () > 0);
    _assertReported (aLines, "Left recursion detected");
  }

  @Test
  public void testDuplicateTokenLabel ()
  {
    final List <String> aLines = _check (_grammar ("TOKEN : { <A : \"a\"> | <A : \"b\"> }\n" +
                                                   "void start() : {} { <A> }\n"));
    assertTrue (JavaCCErrors.getErrorCount () > 0);
    _assertReported (aLines, "Multiply defined lexical token name");
  }

  @Test
  public void testUnknownLexicalState ()
  {
    final List <String> aLines = _check (_grammar ("TOKEN : { <A : \"a\"> : NOWHERE }\n" +
                                                   "void start() : {} { <A> }\n"));
    assertTrue (JavaCCErrors.getErrorCount () > 0);
    _assertReported (aLines, "Lexical state \"NOWHERE\" has not been defined");
  }

  @Test
  public void testExpansionThatCanMatchNothingInsideAStar ()
  {
    final List <String> aLines = _check (_grammar ("TOKEN : { <A : \"a\"> }\n" +
                                                   "void start() : {} { (empty())* }\n" +
                                                   "void empty() : {} { {} }\n"));
    assertTrue (JavaCCErrors.getErrorCount () > 0);
    _assertReported (aLines, "can be matched by empty string");
  }

  @Test
  public void testStringThatCanNeverBeMatchedBecauseOfIgnoreCase ()
  {
    // The IGNORE_CASE variant swallows the later exact literal - this is the branch that used to
    // report through the static Semanticize.other field
    final List <String> aLines = _check (_grammar ("TOKEN [IGNORE_CASE] : { <A : \"abc\"> }\n" +
                                                   "TOKEN : { <B : \"abc\"> }\n" +
                                                   "void start() : {} { <A> }\n"));
    assertTrue (JavaCCErrors.getErrorCount () > 0);
    _assertReported (aLines, "can never be matched");
  }
}
