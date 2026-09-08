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
package com.helger.pgcc.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.PGPrinter.IPrinter;
import com.helger.pgcc.PGPrinter.PSPrinter;

/**
 * Test class for {@link JavaCCInterpreter}, which tokenizes input against a grammar without
 * generating any code. It is the only consumer of the {@code TokenizerData} that
 * {@code LexGenJava.s_generateDataOnly} produces, and it had no test - which is why deleting the
 * table driven token manager needed care.
 *
 * @author Philip Helger
 */
public final class JavaCCInterpreterTest
{
  /** Collects what the interpreter prints. */
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

  /**
   * One character class plus string literals. Deliberately not two character classes - see
   * {@link #testTwoCharacterClassesAreBroken()}.
   */
  private static final String GRAMMAR = "PARSER_BEGIN(Sample)\n" +
                                        "public class Sample {}\n" +
                                        "PARSER_END(Sample)\n" +
                                        "SKIP : { \" \" | \"\\t\" | \"\\n\" | \"\\r\" }\n" +
                                        "TOKEN :\n" +
                                        "{ < WORD : ([\"a\"-\"z\"])+ >\n" +
                                        "| < PLUS : \"+\" >\n" +
                                        "| < SEMI : \";\" >\n" +
                                        "}\n" +
                                        "void start() : {} { ( <WORD> | <PLUS> | <SEMI> )* <EOF> }\n";

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

  private List <String> _tokenize (final String sInput)
  {
    // runTokenizer needs a started run, exactly like JavaCCInterpreter.main does it
    Main.reInitAll ();
    new JavaCCInterpreter ().runTokenizer (GRAMMAR, sInput);
    return m_aPrinter.m_aLines;
  }

  @Test
  public void testTokenizesAgainstTheGrammar ()
  {
    final List <String> aLines = _tokenize ("ab + cd");

    final List <String> aImages = new ArrayList <> ();
    for (final String sLine : aLines)
      if (sLine.startsWith ("Token: "))
        aImages.add (sLine.substring (sLine.indexOf ("image: ") + "image: ".length ()));

    assertEquals (aLines.toString (), List.of ("\"ab\"", "\"+\"", "\"cd\""), aImages);
    assertTrue (aLines.toString (), aLines.contains ("Matched EOF"));
  }

  @Test
  public void testSkippedTokensDoNotShowUp ()
  {
    // The SKIP production swallows the whitespace, so three tokens come out of nine characters
    int nTokens = 0;
    for (final String sLine : _tokenize ("  a   +   b  "))
      if (sLine.startsWith ("Token: "))
        nTokens++;
    assertEquals (3, nTokens);
  }

  @Test
  public void testReportsAnUnmatchableCharacter ()
  {
    boolean bFound = false;
    for (final String sLine : _tokenize ("a # b"))
      if (sLine.contains ("Encountered token error"))
        bFound = true;
    assertTrue ("Expected a token error for '#'", bFound);
  }

  /**
   * A characterization test for a defect, not an endorsement of it: the interpreter cannot handle a
   * grammar with two or more character class tokens. One character class works, and one character
   * class next to any number of string literals works, but the moment a second one appears every
   * input fails to tokenize.
   * <p>
   * The cause, for whoever picks this up. With one character class the initial epsilon move set has
   * a single member, so {@code NfaState.generateInitMoves} returns that state's own name and
   * {@code updateNfaData} finds it. With two, the set has two members and a <em>composite</em> state
   * name is returned, which belongs to no {@code NfaState} object - so {@code updateNfaData} stores
   * a <code>null</code> start state, {@code buildTokenizerData} writes -1 into
   * {@code TokenizerData.m_initialStates}, and the interpreter's {@code if (nfaStartState != -1)}
   * skips the NFA entirely.
   * <p>
   * A fix has to emit a synthetic NFA state for the composite start state. Note that
   * {@code buildTokenizerData} also adds composite member names without applying the lexical state
   * offset it applies to every other name, which only goes unnoticed because the offset is 0 for a
   * single lexical state. Both belong to the same unfinished upstream feature - the removed table
   * driven token manager had a stub that built exactly such a dummy state and then discarded it.
   * <p>
   * If somebody fixes this, this test will fail and should be turned into a positive one.
   */
  @Test
  public void testTwoCharacterClassesAreBroken ()
  {
    final String sTwoClasses = "PARSER_BEGIN(S)\npublic class S {}\nPARSER_END(S)\n" +
                               "SKIP : { \" \" }\n" +
                               "TOKEN : { < A : ([\"a\"-\"z\"])+ > | < B : ([\"0\"-\"9\"])+ > }\n" +
                               "void start() : {} { ( <A> | <B> )* <EOF> }\n";
    Main.reInitAll ();
    new JavaCCInterpreter ().runTokenizer (sTwoClasses, "ab");

    boolean bFailed = false;
    for (final String sLine : m_aPrinter.m_aLines)
      if (sLine.contains ("Encountered token error"))
        bFailed = true;
    assertTrue ("The two character class defect appears to be fixed - make this a positive test",
                bFailed);
  }

  @Test
  public void testEmptyInputMatchesEofImmediately ()
  {
    final List <String> aLines = _tokenize ("");
    assertTrue (aLines.toString (), aLines.contains ("Matched EOF"));
    for (final String sLine : aLines)
      assertTrue (sLine, !sLine.startsWith ("Token: "));
  }
}
