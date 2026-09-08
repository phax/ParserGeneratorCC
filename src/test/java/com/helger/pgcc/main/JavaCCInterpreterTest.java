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

  /**
   * Pick the token images out of what the interpreter printed.
   *
   * @param aLines
   *        The collected output. May not be <code>null</code>.
   * @return One entry per token, in order. Never <code>null</code>.
   */
  private static List <String> _imagesOf (final List <String> aLines)
  {
    final List <String> aImages = new ArrayList <> ();
    for (final String sLine : aLines)
      if (sLine.startsWith ("Token: "))
        aImages.add (sLine.substring (sLine.indexOf ("image: ") + "image: ".length ()));
    return aImages;
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
    assertEquals (aLines.toString (), List.of ("\"ab\"", "\"+\"", "\"cd\""), _imagesOf (aLines));
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
   * Several character class tokens in one lexical state.
   * <p>
   * This used to fail outright, and it is worth recording why. The initial epsilon move set has one
   * member per character class. With one member {@code _addCompositeStateSet} returns that state's
   * own name, which an {@code NfaState} object carries. With more it returns the name of one of the
   * members - unless every member is already used elsewhere, in which case it allocates a name one
   * past the end that belongs to no object at all. {@code updateNfaData} then stored a
   * <code>null</code> start state, {@code buildTokenizerData} wrote -1 into
   * {@code TokenizerData.m_initialStates}, and the interpreter's {@code if (nfaStartState != -1)}
   * skipped the NFA entirely - so every input failed on its first character.
   * <p>
   * {@code buildTokenizerData} now emits a state for that name which matches nothing itself and
   * only carries the composite members, which is what the interpreter expands on entry.
   */
  @Test
  public void testSeveralCharacterClassesInOneLexicalState ()
  {
    final String sFour = "PARSER_BEGIN(S)\npublic class S {}\nPARSER_END(S)\n" +
                         "SKIP : { \" \" }\n" +
                         "TOKEN : { < A : ([\"a\"-\"c\"])+ > | < B : ([\"0\"-\"9\"])+ >" +
                         " | < C : ([\"x\"-\"z\"])+ > | < D : ([\"A\"-\"Z\"])+ > }\n" +
                         "void start() : {} { ( <A> | <B> | <C> | <D> )* <EOF> }\n";
    Main.reInitAll ();
    new JavaCCInterpreter ().runTokenizer (sFour, "abc 123 xyz ABC");

    assertEquals (m_aPrinter.m_aLines.toString (),
                  List.of ("\"abc\"", "\"123\"", "\"xyz\"", "\"ABC\""),
                  _imagesOf (m_aPrinter.m_aLines));
    assertTrue (m_aPrinter.m_aLines.toString (), m_aPrinter.m_aLines.contains ("Matched EOF"));
  }

  /**
   * The same, across two lexical states, which is what catches the naming of the synthesized state.
   * <p>
   * State names are shifted per lexical state so that all of them fit into one array, and the name
   * the NFA construction hands out for a composite state is one past the last state of its own
   * lexical state - which is exactly where the next lexical state's names begin after the shift.
   * Reusing it points the first lexical state at the second one's states. The synthesized state
   * therefore gets a fresh name past every real one.
   * <p>
   * The STRING token also checks that a MORE production's characters end up in the image. The
   * interpreter used to take the image from the start of the last match rather than the start of
   * the token, so everything MORE had consumed was lost and a string came out as its closing quote.
   */
  @Test
  public void testTwoLexicalStates ()
  {
    final String sTwoStates = "PARSER_BEGIN(M)\npublic class M {}\nPARSER_END(M)\n" +
                              "SKIP : { \" \" }\n" +
                              "TOKEN : { < NUM : ([\"0\"-\"9\"])+ > | < ID : ([\"a\"-\"z\"])+ > | < PLUS : \"+\" > }\n" +
                              "MORE : { \"\\\"\" : IN_STR }\n" +
                              "<IN_STR> MORE : { < ~[\"\\\"\"] > }\n" +
                              "<IN_STR> TOKEN : { < STR : \"\\\"\" > : DEFAULT }\n" +
                              "void start() : {} { ( <NUM> | <ID> | <PLUS> | <STR> )* <EOF> }\n";
    Main.reInitAll ();
    new JavaCCInterpreter ().runTokenizer (sTwoStates, "12 + ab \"hello\" xy");

    assertEquals (m_aPrinter.m_aLines.toString (),
                  List.of ("\"12\"", "\"+\"", "\"ab\"", "\"\"hello\"\"", "\"xy\""),
                  _imagesOf (m_aPrinter.m_aLines));
    assertTrue (m_aPrinter.m_aLines.toString (), m_aPrinter.m_aLines.contains ("Matched EOF"));
  }

  /**
   * A characterization test for a gap, not an endorsement of it.
   * <p>
   * In a <em>mixed</em> lexical state - JavaCC's name for one whose string literals and NFA cannot
   * be kept apart - the interpreter cannot carry a literal match on into the NFA. It matches the
   * literal "select", stops, and starts again, so "selecting" comes out as SELECT followed by the
   * identifier "ing" instead of one identifier.
   * <p>
   * The reason is {@code ExpRStringLiteral._getStateSetForKind}, which returns -1 for a mixed state
   * before looking at anything else, so {@code TokenizerData.m_kindToNfaStartState} holds -1 for
   * every literal and the interpreter has nowhere to continue. The generated token manager does not
   * use that table at all; it emits {@code jjStartNfaWithStates} calls instead. Closing this means
   * giving the interpreter the equivalent, which is more than it looks.
   * <p>
   * {@code IGNORE_CASE} is what makes the state mixed here - drop it from the grammar below and the
   * same input tokenizes correctly as one identifier, because the literal is then folded into the
   * NFA and never goes through the literal table.
   * <p>
   * This is the one token where the interpreter disagrees with the compiled parser on the grammar
   * in {@code JavaRoundTripFuncTest}, which has an IGNORE_CASE keyword for exactly this reason.
   */
  @Test
  public void testALiteralDoesNotContinueIntoTheNfaInAMixedState ()
  {
    final String sMixed = "PARSER_BEGIN(X)\npublic class X {}\nPARSER_END(X)\n" +
                          "SKIP : { \" \" }\n" +
                          "TOKEN [IGNORE_CASE] : { < SELECT : \"select\" > }\n" +
                          "TOKEN : { < IDENT : ([\"a\"-\"z\"])+ > }\n" +
                          "void start() : {} { ( <SELECT> | <IDENT> )* <EOF> }\n";
    Main.reInitAll ();
    new JavaCCInterpreter ().runTokenizer (sMixed, "selecting");

    assertEquals ("The mixed state literal gap appears to be closed - make this a positive test",
                  List.of ("\"select\"", "\"ing\""),
                  _imagesOf (m_aPrinter.m_aLines));
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
