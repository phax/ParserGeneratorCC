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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.ETokenKind;
import com.helger.pgcc.parser.NfaState;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.TokenizerData;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;

/**
 * The working state of one token manager generation.
 * <p>
 * This is the instance state behind what used to be the static fields of
 * {@link com.helger.pgcc.output.java.LexGenJava}. Some of it describes the whole run - the lexical
 * state names, the per token flags and the ordinal counter - and some of it is overwritten for each
 * lexical state as the generator walks them, which is why the fields that look like scratch really
 * are scratch.
 *
 * @author Philip Helger
 */
public final class LexerState
{
  /** Default constructor. */
  public LexerState ()
  {}

  private final Map <String, List <TokenProduction>> m_aAllTpsForState = new LinkedHashMap <> ();
  private final Map <String, NfaState> m_aInitStates = new LinkedHashMap <> ();

  private final NfaBuildState m_aNfa = new NfaBuildState ();
  private final StringLiteralBuildState m_aStringLiterals = new StringLiteralBuildState ();
  private final TokenizerDataBuildState m_aTokenizerDataBuild = new TokenizerDataBuildState ();

  private String m_sTokenMgrClassName;
  private int m_nLexStateIndex = 0;
  private ETokenKind [] m_aKinds;
  private int m_nMaxOrdinal = 1;
  private String m_sLexStateSuffix;
  private String [] m_aNewLexState;
  private int [] m_aLexStates;
  private boolean [] m_aIgnoreCase;
  private ExpAction [] m_aActions;
  private int m_nStateSetSize;
  private int m_nTotalNumStates;
  private int m_nMaxLexStates;
  private String [] m_aLexStateName;
  private NfaState [] m_aSinglesToSkip;
  private long [] m_aToSkip;
  private long [] m_aToSpecial;
  private long [] m_aToMore;
  private long [] m_aToToken;
  private int m_nDefaultLexState;
  private AbstractExpRegularExpression [] m_aRexprs;
  private int [] m_aMaxLongsReqd;
  private int [] m_aInitMatch;
  private int [] m_aCanMatchAnyChar;
  private boolean m_bHasEmptyMatch;
  private boolean [] m_aCanLoop;
  private boolean m_bHasLoop = false;
  private boolean [] m_aCanReachOnMore;
  private boolean [] m_aHasNfa;
  private boolean [] m_aMixed;
  private NfaState m_aInitialState;
  private int m_nCurKind;
  private boolean m_bHasSkipActions = false;
  private boolean m_bHasMoreActions = false;
  private boolean m_bHasTokenActions = false;
  private boolean m_bHasSpecial = false;
  private boolean m_bHasSkip = false;
  private boolean m_bHasMore = false;
  private AbstractExpRegularExpression m_aCurRE;
  private boolean m_bKeepLineCol;
  private String m_sErrorHandlingClass;
  private final TokenizerData m_aTokenizerData = new TokenizerData ();
  private boolean m_bGenerateDataOnly;

  /**
   * The name of the token manager class being generated, the parser name plus "TokenManager".
   *
   * @return The class name. May be <code>null</code> before generation starts.
   */
  public String getTokenMgrClassName ()
  {
    return m_sTokenMgrClassName;
  }

  /**
   * The name of the token manager class being generated, the parser name plus "TokenManager".
   *
   * @param sTokenMgrClassName
   *        The class name to generate.
   */
  public void setTokenMgrClassName (final String sTokenMgrClassName)
  {
    m_sTokenMgrClassName = sTokenMgrClassName;
  }

  /**
   * The lexical state that is currently being generated. Most of the per state arrays here are
   * indexed by this.
   *
   * @return The index into {@link #getLexStateName()}.
   */
  public int getLexStateIndex ()
  {
    return m_nLexStateIndex;
  }

  /**
   * The lexical state that is currently being generated. Most of the per state arrays here are
   * indexed by this.
   *
   * @param nLexStateIndex
   *        The lexical state to generate next.
   */
  public void setLexStateIndex (final int nLexStateIndex)
  {
    m_nLexStateIndex = nLexStateIndex;
  }

  /**
   * What each token kind does when it matches: produce a token, skip, continue with MORE or
   * produce a special token. Indexed by ordinal, <code>null</code> for a private regular
   * expression.
   *
   * @return The kinds, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public ETokenKind [] getKinds ()
  {
    return m_aKinds;
  }

  /**
   * What each token kind does when it matches: produce a token, skip, continue with MORE or
   * produce a special token. Indexed by ordinal, <code>null</code> for a private regular
   * expression.
   *
   * @param aKinds
   *        The kinds, indexed by ordinal.
   */
  public void setKinds (final ETokenKind [] aKinds)
  {
    m_aKinds = aKinds;
  }

  /**
   * One past the highest token ordinal in the grammar, and therefore the length of every array
   * indexed by ordinal.
   *
   * @return The number of token kinds.
   */
  public int getMaxOrdinal ()
  {
    return m_nMaxOrdinal;
  }

  /**
   * One past the highest token ordinal in the grammar, and therefore the length of every array
   * indexed by ordinal.
   *
   * @param nMaxOrdinal
   *        The number of token kinds.
   */
  public void setMaxOrdinal (final int nMaxOrdinal)
  {
    m_nMaxOrdinal = nMaxOrdinal;
  }

  /**
   * The suffix the generated method names of the current lexical state carry, "_" followed by
   * {@link #getLexStateIndex()} - the token manager has one jjMoveNfa, one jjStopAtPos and so on
   * per lexical state.
   *
   * @return The suffix. May be <code>null</code> before generation starts.
   */
  public String getLexStateSuffix ()
  {
    return m_sLexStateSuffix;
  }

  /**
   * The suffix the generated method names of the current lexical state carry, "_" followed by
   * {@link #getLexStateIndex()} - the token manager has one jjMoveNfa, one jjStopAtPos and so on
   * per lexical state.
   *
   * @param sLexStateSuffix
   *        The suffix to append.
   */
  public void setLexStateSuffix (final String sLexStateSuffix)
  {
    m_sLexStateSuffix = sLexStateSuffix;
  }

  /**
   * The lexical state each token kind switches to when it matches, <code>null</code> where it
   * stays in the current one. Indexed by ordinal.
   *
   * @return The target state names, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public String [] getNewLexState ()
  {
    return m_aNewLexState;
  }

  /**
   * The lexical state each token kind switches to when it matches, <code>null</code> where it
   * stays in the current one. Indexed by ordinal.
   *
   * @param aNewLexState
   *        The target state names, indexed by ordinal.
   */
  public void setNewLexState (final String [] aNewLexState)
  {
    m_aNewLexState = aNewLexState;
  }

  /**
   * The lexical state each token kind was declared in, indexed by ordinal.
   *
   * @return The state indices, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public int [] getLexStates ()
  {
    return m_aLexStates;
  }

  /**
   * The lexical state each token kind was declared in, indexed by ordinal.
   *
   * @param aLexStates
   *        The state indices, indexed by ordinal.
   */
  public void setLexStates (final int [] aLexStates)
  {
    m_aLexStates = aLexStates;
  }

  /**
   * Whether each token kind was declared inside an IGNORE_CASE token production, indexed by
   * ordinal.
   *
   * @return The flags, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public boolean [] getIgnoreCase ()
  {
    return m_aIgnoreCase;
  }

  /**
   * Whether each token kind was declared inside an IGNORE_CASE token production, indexed by
   * ordinal.
   *
   * @param aIgnoreCase
   *        The flags, indexed by ordinal.
   */
  public void setIgnoreCase (final boolean [] aIgnoreCase)
  {
    m_aIgnoreCase = aIgnoreCase;
  }

  /**
   * The lexical action attached to each token kind, <code>null</code> where there is none.
   * Indexed by ordinal.
   *
   * @return The actions, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public ExpAction [] getActions ()
  {
    return m_aActions;
  }

  /**
   * The lexical action attached to each token kind, <code>null</code> where there is none.
   * Indexed by ordinal.
   *
   * @param aActions
   *        The actions, indexed by ordinal.
   */
  public void setActions (final ExpAction [] aActions)
  {
    m_aActions = aActions;
  }

  /**
   * The number of NFA states of the lexical state that produced the largest automaton, which is
   * how wide the generated jjstateSet array has to be.
   *
   * @return The number of states.
   */
  public int getStateSetSize ()
  {
    return m_nStateSetSize;
  }

  /**
   * The number of NFA states of the lexical state that produced the largest automaton, which is
   * how wide the generated jjstateSet array has to be.
   *
   * @param nStateSetSize
   *        The number of states.
   */
  public void setStateSetSize (final int nStateSetSize)
  {
    m_nStateSetSize = nStateSetSize;
  }

  /**
   * The number of NFA states over all lexical states together, used to decide whether the
   * generated token manager needs the two or the four argument jjCheckNAddStates.
   *
   * @return The number of states.
   */
  public int getTotalNumStates ()
  {
    return m_nTotalNumStates;
  }

  /**
   * The number of NFA states over all lexical states together, used to decide whether the
   * generated token manager needs the two or the four argument jjCheckNAddStates.
   *
   * @param nTotalNumStates
   *        The number of states.
   */
  public void setTotalNumStates (final int nTotalNumStates)
  {
    m_nTotalNumStates = nTotalNumStates;
  }

  /**
   * The number of lexical states in the grammar, and therefore the length of every array indexed
   * by lexical state.
   *
   * @return The number of lexical states.
   */
  public int getMaxLexStates ()
  {
    return m_nMaxLexStates;
  }

  /**
   * The number of lexical states in the grammar, and therefore the length of every array indexed
   * by lexical state.
   *
   * @param nMaxLexStates
   *        The number of lexical states.
   */
  public void setMaxLexStates (final int nMaxLexStates)
  {
    m_nMaxLexStates = nMaxLexStates;
  }

  /**
   * The declared name of each lexical state, indexed by lexical state.
   *
   * @return The names, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public String [] getLexStateName ()
  {
    return m_aLexStateName;
  }

  /**
   * The declared name of each lexical state, indexed by lexical state.
   *
   * @param aLexStateName
   *        The names, indexed by lexical state.
   */
  public void setLexStateName (final String [] aLexStateName)
  {
    m_aLexStateName = aLexStateName;
  }

  /**
   * For each lexical state, the single characters that are skipped outright - a SKIP of exactly
   * one character needs no automaton, the generated code just loops over them. The NfaState is a
   * dummy that is used as a character set, not as part of the automaton.
   *
   * @return The character sets, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public NfaState [] getSinglesToSkip ()
  {
    return m_aSinglesToSkip;
  }

  /**
   * For each lexical state, the single characters that are skipped outright - a SKIP of exactly
   * one character needs no automaton, the generated code just loops over them. The NfaState is a
   * dummy that is used as a character set, not as part of the automaton.
   *
   * @param aSinglesToSkip
   *        The character sets, indexed by lexical state.
   */
  public void setSinglesToSkip (final NfaState [] aSinglesToSkip)
  {
    m_aSinglesToSkip = aSinglesToSkip;
  }

  /**
   * The token kinds that are skipped, as a bit set: bit <code>n % 64</code> of element
   * <code>n / 64</code>. Both SKIP and SPECIAL_TOKEN are in here.
   *
   * @return The bit set over the ordinals. May be <code>null</code> before generation starts.
   */
  public long [] getToSkip ()
  {
    return m_aToSkip;
  }

  /**
   * The token kinds that are skipped, as a bit set: bit <code>n % 64</code> of element
   * <code>n / 64</code>. Both SKIP and SPECIAL_TOKEN are in here.
   *
   * @param aToSkip
   *        The bit set over the ordinals.
   */
  public void setToSkip (final long [] aToSkip)
  {
    m_aToSkip = aToSkip;
  }

  /**
   * The token kinds that are special tokens, as a bit set: bit <code>n % 64</code> of element
   * <code>n / 64</code>.
   *
   * @return The bit set over the ordinals. May be <code>null</code> before generation starts.
   */
  public long [] getToSpecial ()
  {
    return m_aToSpecial;
  }

  /**
   * The token kinds that are special tokens, as a bit set: bit <code>n % 64</code> of element
   * <code>n / 64</code>.
   *
   * @param aToSpecial
   *        The bit set over the ordinals.
   */
  public void setToSpecial (final long [] aToSpecial)
  {
    m_aToSpecial = aToSpecial;
  }

  /**
   * The token kinds that continue the match with MORE, as a bit set: bit <code>n % 64</code> of
   * element <code>n / 64</code>.
   *
   * @return The bit set over the ordinals. May be <code>null</code> before generation starts.
   */
  public long [] getToMore ()
  {
    return m_aToMore;
  }

  /**
   * The token kinds that continue the match with MORE, as a bit set: bit <code>n % 64</code> of
   * element <code>n / 64</code>.
   *
   * @param aToMore
   *        The bit set over the ordinals.
   */
  public void setToMore (final long [] aToMore)
  {
    m_aToMore = aToMore;
  }

  /**
   * The token kinds that produce a token, as a bit set: bit <code>n % 64</code> of element
   * <code>n / 64</code>.
   *
   * @return The bit set over the ordinals. May be <code>null</code> before generation starts.
   */
  public long [] getToToken ()
  {
    return m_aToToken;
  }

  /**
   * The token kinds that produce a token, as a bit set: bit <code>n % 64</code> of element
   * <code>n / 64</code>.
   *
   * @param aToToken
   *        The bit set over the ordinals.
   */
  public void setToToken (final long [] aToToken)
  {
    m_aToToken = aToToken;
  }

  /**
   * The lexical state the generated token manager starts in, which is DEFAULT unless the grammar
   * declares otherwise.
   *
   * @return The index into {@link #getLexStateName()}.
   */
  public int getDefaultLexState ()
  {
    return m_nDefaultLexState;
  }

  /**
   * The lexical state the generated token manager starts in, which is DEFAULT unless the grammar
   * declares otherwise.
   *
   * @param nDefaultLexState
   *        The index into {@link #getLexStateName()}.
   */
  public void setDefaultLexState (final int nDefaultLexState)
  {
    m_nDefaultLexState = nDefaultLexState;
  }

  /**
   * The regular expression behind each token kind, indexed by ordinal. This is what an error
   * message needs to point back at the grammar.
   *
   * @return The regular expressions, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public AbstractExpRegularExpression [] getRexprs ()
  {
    return m_aRexprs;
  }

  /**
   * The regular expression behind each token kind, indexed by ordinal. This is what an error
   * message needs to point back at the grammar.
   *
   * @param aRexprs
   *        The regular expressions, indexed by ordinal.
   */
  public void setRexprs (final AbstractExpRegularExpression [] aRexprs)
  {
    m_aRexprs = aRexprs;
  }

  /**
   * How many 64 bit words the "which literals are still possible" bit vector needs in each
   * lexical state, indexed by lexical state. It decides how many active0, active1 ... parameters
   * the generated jjMoveStringLiteralDfa methods take.
   *
   * @return The word counts, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public int [] getMaxLongsReqd ()
  {
    return m_aMaxLongsReqd;
  }

  /**
   * How many 64 bit words the "which literals are still possible" bit vector needs in each
   * lexical state, indexed by lexical state. It decides how many active0, active1 ... parameters
   * the generated jjMoveStringLiteralDfa methods take.
   *
   * @param aMaxLongsReqd
   *        The word counts, indexed by lexical state.
   */
  public void setMaxLongsReqd (final int [] aMaxLongsReqd)
  {
    m_aMaxLongsReqd = aMaxLongsReqd;
  }

  /**
   * The token kind that matches the empty string in each lexical state, indexed by lexical state.
   * Both 0 and {@link Integer#MAX_VALUE} mean there is none.
   *
   * @return The ordinals, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public int [] getInitMatch ()
  {
    return m_aInitMatch;
  }

  /**
   * The token kind that matches the empty string in each lexical state, indexed by lexical state.
   * Both 0 and {@link Integer#MAX_VALUE} mean there is none.
   *
   * @param aInitMatch
   *        The ordinals, indexed by lexical state.
   */
  public void setInitMatch (final int [] aInitMatch)
  {
    m_aInitMatch = aInitMatch;
  }

  /**
   * The lowest ordinal of a token kind that matches any character at all in each lexical state,
   * indexed by lexical state, or -1 where there is none. Such a kind is the fallback the
   * automaton falls through to.
   *
   * @return The ordinals, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public int [] getCanMatchAnyChar ()
  {
    return m_aCanMatchAnyChar;
  }

  /**
   * The lowest ordinal of a token kind that matches any character at all in each lexical state,
   * indexed by lexical state, or -1 where there is none. Such a kind is the fallback the
   * automaton falls through to.
   *
   * @param aCanMatchAnyChar
   *        The ordinals, indexed by lexical state.
   */
  public void setCanMatchAnyChar (final int [] aCanMatchAnyChar)
  {
    m_aCanMatchAnyChar = aCanMatchAnyChar;
  }

  /**
   * Whether any lexical state has a token kind that matches the empty string. The generated token
   * manager needs an extra guard against looping forever if it does.
   *
   * @return <code>true</code> if at least one lexical state matches the empty string.
   */
  public boolean isHasEmptyMatch ()
  {
    return m_bHasEmptyMatch;
  }

  /**
   * Whether any lexical state has a token kind that matches the empty string. The generated token
   * manager needs an extra guard against looping forever if it does.
   *
   * @param bHasEmptyMatch
   *        <code>true</code> if at least one lexical state matches the empty string.
   */
  public void setHasEmptyMatch (final boolean bHasEmptyMatch)
  {
    m_bHasEmptyMatch = bHasEmptyMatch;
  }

  /**
   * Whether each lexical state takes part in a cycle of empty string matches, indexed by lexical
   * state - state A matches the empty string and switches to B, B does the same and switches back.
   *
   * @return The flags, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public boolean [] getCanLoop ()
  {
    return m_aCanLoop;
  }

  /**
   * Whether each lexical state takes part in a cycle of empty string matches, indexed by lexical
   * state - state A matches the empty string and switches to B, B does the same and switches back.
   *
   * @param aCanLoop
   *        The flags, indexed by lexical state.
   */
  public void setCanLoop (final boolean [] aCanLoop)
  {
    m_aCanLoop = aCanLoop;
  }



  /**
   * Whether {@link #getCanLoop()} is set for any lexical state.
   *
   * @return <code>true</code> if there is at least one empty string match cycle.
   */
  public boolean isHasLoop ()
  {
    return m_bHasLoop;
  }

  /**
   * Whether {@link #getCanLoop()} is set for any lexical state.
   *
   * @param bHasLoop
   *        <code>true</code> if there is at least one empty string match cycle.
   */
  public void setHasLoop (final boolean bHasLoop)
  {
    m_bHasLoop = bHasLoop;
  }

  /**
   * Whether each lexical state can be entered while a MORE is still being collected, indexed by
   * lexical state. If it can, the generated code must not throw the image collected so far away.
   *
   * @return The flags, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public boolean [] getCanReachOnMore ()
  {
    return m_aCanReachOnMore;
  }

  /**
   * Whether each lexical state can be entered while a MORE is still being collected, indexed by
   * lexical state. If it can, the generated code must not throw the image collected so far away.
   *
   * @param aCanReachOnMore
   *        The flags, indexed by lexical state.
   */
  public void setCanReachOnMore (final boolean [] aCanReachOnMore)
  {
    m_aCanReachOnMore = aCanReachOnMore;
  }

  /**
   * Whether each lexical state produced any NFA states at all, indexed by lexical state - a state
   * whose tokens are all plain string literals needs no automaton.
   *
   * @return The flags, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public boolean [] getHasNfa ()
  {
    return m_aHasNfa;
  }

  /**
   * Whether each lexical state produced any NFA states at all, indexed by lexical state - a state
   * whose tokens are all plain string literals needs no automaton.
   *
   * @param aHasNfa
   *        The flags, indexed by lexical state.
   */
  public void setHasNfa (final boolean [] aHasNfa)
  {
    m_aHasNfa = aHasNfa;
  }

  /**
   * Whether each lexical state mixes case sensitive and IGNORE_CASE string literals, indexed by
   * lexical state. The string literal DFA cannot be shared between the two, so a mixed state
   * falls back to the NFA.
   *
   * @return The flags, indexed by lexical state. May be <code>null</code> before generation starts.
   */
  public boolean [] getMixed ()
  {
    return m_aMixed;
  }

  /**
   * Whether each lexical state mixes case sensitive and IGNORE_CASE string literals, indexed by
   * lexical state. The string literal DFA cannot be shared between the two, so a mixed state
   * falls back to the NFA.
   *
   * @param aMixed
   *        The flags, indexed by lexical state.
   */
  public void setMixed (final boolean [] aMixed)
  {
    m_aMixed = aMixed;
  }

  /**
   * The NFA state every match of the lexical state currently being generated starts from. All
   * regular expressions of the state are attached to it as epsilon moves.
   *
   * @return The start state. May be <code>null</code> before generation starts.
   */
  public NfaState getInitialState ()
  {
    return m_aInitialState;
  }

  /**
   * The NFA state every match of the lexical state currently being generated starts from. All
   * regular expressions of the state are attached to it as epsilon moves.
   *
   * @param aInitialState
   *        The start state.
   */
  public void setInitialState (final NfaState aInitialState)
  {
    m_aInitialState = aInitialState;
  }

  /**
   * The ordinal of the regular expression currently being turned into an automaton.
   *
   * @return The ordinal.
   */
  public int getCurKind ()
  {
    return m_nCurKind;
  }

  /**
   * The ordinal of the regular expression currently being turned into an automaton.
   *
   * @param nCurKind
   *        The ordinal.
   */
  public void setCurKind (final int nCurKind)
  {
    m_nCurKind = nCurKind;
  }

  /**
   * Whether any skipped token kind carries a lexical action or switches the lexical state, which
   * is what decides whether the generated token manager needs a SkipLexicalActions method.
   *
   * @return <code>true</code> if such a kind exists.
   */
  public boolean isHasSkipActions ()
  {
    return m_bHasSkipActions;
  }

  /**
   * Whether any skipped token kind carries a lexical action or switches the lexical state, which
   * is what decides whether the generated token manager needs a SkipLexicalActions method.
   *
   * @param bHasSkipActions
   *        <code>true</code> if such a kind exists.
   */
  public void setHasSkipActions (final boolean bHasSkipActions)
  {
    m_bHasSkipActions = bHasSkipActions;
  }

  /**
   * Whether any MORE token kind carries a lexical action, which is what decides whether the
   * generated token manager needs a MoreLexicalActions method.
   *
   * @return <code>true</code> if such a kind exists.
   */
  public boolean isHasMoreActions ()
  {
    return m_bHasMoreActions;
  }

  /**
   * Whether any MORE token kind carries a lexical action, which is what decides whether the
   * generated token manager needs a MoreLexicalActions method.
   *
   * @param bHasMoreActions
   *        <code>true</code> if such a kind exists.
   */
  public void setHasMoreActions (final boolean bHasMoreActions)
  {
    m_bHasMoreActions = bHasMoreActions;
  }

  /**
   * Whether any token producing kind carries a lexical action, which is what decides whether the
   * generated token manager needs a TokenLexicalActions method.
   *
   * @return <code>true</code> if such a kind exists.
   */
  public boolean isHasTokenActions ()
  {
    return m_bHasTokenActions;
  }

  /**
   * Whether any token producing kind carries a lexical action, which is what decides whether the
   * generated token manager needs a TokenLexicalActions method.
   *
   * @param bHasTokenActions
   *        <code>true</code> if such a kind exists.
   */
  public void setHasTokenActions (final boolean bHasTokenActions)
  {
    m_bHasTokenActions = bHasTokenActions;
  }

  /**
   * Whether the grammar declares any SPECIAL_TOKEN.
   *
   * @return <code>true</code> if it does.
   */
  public boolean isHasSpecial ()
  {
    return m_bHasSpecial;
  }

  /**
   * Whether the grammar declares any SPECIAL_TOKEN.
   *
   * @param bHasSpecial
   *        <code>true</code> if it does.
   */
  public void setHasSpecial (final boolean bHasSpecial)
  {
    m_bHasSpecial = bHasSpecial;
  }

  /**
   * Whether the grammar declares any SKIP.
   *
   * @return <code>true</code> if it does.
   */
  public boolean isHasSkip ()
  {
    return m_bHasSkip;
  }

  /**
   * Whether the grammar declares any SKIP.
   *
   * @param bHasSkip
   *        <code>true</code> if it does.
   */
  public void setHasSkip (final boolean bHasSkip)
  {
    m_bHasSkip = bHasSkip;
  }

  /**
   * Whether the grammar declares any MORE.
   *
   * @return <code>true</code> if it does.
   */
  public boolean isHasMore ()
  {
    return m_bHasMore;
  }

  /**
   * Whether the grammar declares any MORE.
   *
   * @param bHasMore
   *        <code>true</code> if it does.
   */
  public void setHasMore (final boolean bHasMore)
  {
    m_bHasMore = bHasMore;
  }

  /**
   * The regular expression currently being turned into an automaton.
   *
   * @return The regular expression. May be <code>null</code> before generation starts.
   */
  public AbstractExpRegularExpression getCurRE ()
  {
    return m_aCurRE;
  }

  /**
   * The regular expression currently being turned into an automaton.
   *
   * @param aCurRE
   *        The regular expression.
   */
  public void setCurRE (final AbstractExpRegularExpression aCurRE)
  {
    m_aCurRE = aCurRE;
  }

  /**
   * A copy of the KEEP_LINE_COLUMN option, taken when generation starts.
   *
   * @return <code>true</code> if the generated char stream tracks line and column.
   */
  public boolean isKeepLineCol ()
  {
    return m_bKeepLineCol;
  }

  /**
   * A copy of the KEEP_LINE_COLUMN option, taken when generation starts.
   *
   * @param bKeepLineCol
   *        <code>true</code> if the generated char stream tracks line and column.
   */
  public void setKeepLineCol (final boolean bKeepLineCol)
  {
    m_bKeepLineCol = bKeepLineCol;
  }

  /**
   * A copy of the TOKEN_MGR_ERROR_CLASS option, taken when generation starts.
   *
   * @return The class name the generated token manager throws. May be <code>null</code> before generation
   *         starts.
   */
  public String getErrorHandlingClass ()
  {
    return m_sErrorHandlingClass;
  }

  /**
   * A copy of the TOKEN_MGR_ERROR_CLASS option, taken when generation starts.
   *
   * @param sErrorHandlingClass
   *        The class name the generated token manager throws.
   */
  public void setErrorHandlingClass (final String sErrorHandlingClass)
  {
    m_sErrorHandlingClass = sErrorHandlingClass;
  }

  /**
   * The tokenizer description that JavaCCInterpreter runs directly, filled in alongside the
   * generated code.
   *
   * @return The tokenizer data. Never <code>null</code>.
   */
  public TokenizerData getTokenizerData ()
  {
    return m_aTokenizerData;
  }

  /**
   * Every token production of the grammar, in declaration order within its lexical state.
   *
   * @return The productions, keyed by lexical state name. Never <code>null</code>.
   */
  @NonNull
  public Map <String, List <TokenProduction>> allTpsForState ()
  {
    return m_aAllTpsForState;
  }

  /**
   * The NFA state every match of a lexical state starts from.
   *
   * @return The start states, keyed by lexical state name. Never <code>null</code>.
   */
  @NonNull
  public Map <String, NfaState> initStates ()
  {
    return m_aInitStates;
  }

  /**
   * The automaton being built for the lexical state that is currently being generated. It is reset
   * between lexical states, so nothing in it outlives the state it belongs to.
   *
   * @return The NFA build state. Never <code>null</code>.
   */
  @NonNull
  public NfaBuildState nfa ()
  {
    return m_aNfa;
  }

  /**
   * The string literal trie being built for the lexical state that is currently being generated. It
   * is reset between lexical states, just like {@link #nfa()}.
   *
   * @return The string literal build state. Never <code>null</code>.
   */
  @NonNull
  public StringLiteralBuildState stringLiterals ()
  {
    return m_aStringLiterals;
  }

  /**
   * The scratch space for the TokenizerData that JavaCCInterpreter consumes. Unlike {@link #nfa()}
   * and {@link #stringLiterals()} this lives for the whole run, because it collects one entry per
   * lexical state.
   *
   * @return The tokenizer data build state. Never <code>null</code>.
   */
  @NonNull
  public TokenizerDataBuildState tokenizerDataBuild ()
  {
    return m_aTokenizerDataBuild;
  }

  /**
   * Start over for the next lexical state. Everything the NFA and the string literal construction
   * accumulated is per lexical state, not per run - {@code LexGenJava.start ()} calls this at the
   * top of its loop.
   */
  public void resetForLexicalState ()
  {
    m_aNfa.resetForLexicalState ();
    m_aStringLiterals.resetForLexicalState ();
  }

  /**
   * Whether this run only collects the {@link com.helger.pgcc.parser.TokenizerData} that
   * JavaCCInterpreter consumes instead of writing a token manager.
   *
   * @return <code>true</code> to skip the code generation.
   */
  public boolean isGenerateDataOnly ()
  {
    return m_bGenerateDataOnly;
  }

  /**
   * Whether this run only collects the {@link com.helger.pgcc.parser.TokenizerData} that
   * JavaCCInterpreter consumes instead of writing a token manager.
   *
   * @param bGenerateDataOnly
   *        <code>true</code> to skip the code generation.
   */
  public void setGenerateDataOnly (final boolean bGenerateDataOnly)
  {
    m_bGenerateDataOnly = bGenerateDataOnly;
  }
}
