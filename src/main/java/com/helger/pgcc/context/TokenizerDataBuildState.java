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
package com.helger.pgcc.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.NfaState;
import com.helger.pgcc.parser.TokenizerData;

/**
 * The scratch space for building the {@link TokenizerData} that
 * {@code com.helger.pgcc.main.JavaCCInterpreter} consumes.
 * <p>
 * It is filled per lexical state, by {@code NfaState.updateNfaData} and
 * {@code ExpRStringLiteral.updateStringLiteralData}, and read once at the end of the run. So its
 * lifetime is the run, not the lexical state, which is why it hangs off {@link LexerState} directly
 * rather than off {@link NfaBuildState} or {@link StringLiteralBuildState}.
 * <p>
 * These eight maps used to be <code>static final</code> fields on {@code NfaState} and
 * {@code ExpRStringLiteral}. Being <code>final</code> they read like constants, but the content is
 * mutable and nothing ever cleared them, so every run in a JVM added to what the previous run had
 * left behind. There is no reset method here on purpose: every entry point calls
 * {@code Main.reInitAll ()}, which drops the whole {@link PGCCContext} and with it this object.
 *
 * @author Philip Helger
 */
public final class TokenizerDataBuildState
{
  /** Default constructor. */
  public TokenizerDataBuildState ()
  {}

  /**
   * A start state that is composite and got a name no {@code NfaState} object carries.
   *
   * @param nStateName
   *        The name the composite set was given.
   * @param aMemberStates
   *        The states the composite set stands for.
   */
  public record CompositeStartState (int nStateName, int [] aMemberStates)
  {}

  private final Map <Integer, CompositeStartState> m_aCompositeStartStates = new HashMap <> ();
  private final Map <Integer, NfaState> m_aInitialStates = new HashMap <> ();
  private final Map <Integer, List <NfaState>> m_aStatesForLexicalState = new HashMap <> ();
  private final Map <Integer, Integer> m_aNfaStateOffset = new HashMap <> ();
  private final Map <Integer, Integer> m_aMatchAnyChar = new HashMap <> ();

  private final Map <Integer, List <String>> m_aLiteralsByLength = new HashMap <> ();
  private final Map <Integer, List <Integer>> m_aLiteralKinds = new HashMap <> ();
  private final Map <Integer, Integer> m_aKindToLexicalState = new HashMap <> ();
  private final Map <Integer, NfaState> m_aNfaStateMap = new HashMap <> ();

  /**
   * {@return lexical state index to its composite start state, for the lexical states whose start
   * state has no {@code NfaState} of its own. Never <code>null</code>.}
   */
  @NonNull
  public Map <Integer, CompositeStartState> compositeStartStates ()
  {
    return m_aCompositeStartStates;
  }

  /**
   * {@return lexical state index to the NFA state the tokenizer starts in. Never <code>null</code>}
   */
  @NonNull
  public Map <Integer, NfaState> initialStates ()
  {
    return m_aInitialStates;
  }

  /**
   * {@return lexical state index to all NFA states of that lexical state. Never <code>null</code>}
   */
  @NonNull
  public Map <Integer, List <NfaState>> statesForLexicalState ()
  {
    return m_aStatesForLexicalState;
  }

  /**
   * {@return lexical state index to the amount its state names have to be shifted by, so that the
   * states of all lexical states fit into one array. Never <code>null</code>.}
   */
  @NonNull
  public Map <Integer, Integer> nfaStateOffset ()
  {
    return m_aNfaStateOffset;
  }

  /**
   * {@return lexical state index to the kind of the token that matches any character, or
   * {@link Integer#MAX_VALUE} if there is none. Never <code>null</code>.}
   */
  @NonNull
  public Map <Integer, Integer> matchAnyChar ()
  {
    return m_aMatchAnyChar;
  }

  /**
   * {@return literal length to the string literals of that length. Never <code>null</code>}
   */
  @NonNull
  public Map <Integer, List <String>> literalsByLength ()
  {
    return m_aLiteralsByLength;
  }

  /**
   * {@return literal length to the token kinds of the literals of that length, in the same order as
   * {@link #literalsByLength()}. Never <code>null</code>.}
   */
  @NonNull
  public Map <Integer, List <Integer>> literalKinds ()
  {
    return m_aLiteralKinds;
  }

  /**
   * {@return token kind to the lexical state it belongs to. Never <code>null</code>}
   */
  @NonNull
  public Map <Integer, Integer> kindToLexicalState ()
  {
    return m_aKindToLexicalState;
  }

  /**
   * {@return token kind to the NFA state a literal match continues in, if any. Never
   * <code>null</code>.}
   */
  @NonNull
  public Map <Integer, NfaState> nfaStateMap ()
  {
    return m_aNfaStateMap;
  }
}
