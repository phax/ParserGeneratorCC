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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.NfaState;

/**
 * The state of building the NFA and then the DFA of ONE lexical state.
 * <p>
 * This is the instance state behind the static fields of {@link com.helger.pgcc.parser.NfaState}.
 * Unlike the rest of the context it does not live for a whole run: {@code LexGenJava.start ()}
 * walks the lexical states and starts a fresh one for each, which is what
 * {@link LexerState#resetForLexicalState()} does.
 *
 * @author Philip Helger
 */
public final class NfaBuildState
{
  private final List <NfaState> m_aIndexedAllStates = new ArrayList <> ();
  private final List <NfaState> m_aNonAsciiTableForMethod = new ArrayList <> ();
  private final Map <String, NfaState> m_aEquivStatesTable = new HashMap <> ();
  private final Map <String, int []> m_aAllNextStates = new HashMap <> ();
  private final Map <String, Integer> m_aLoHiByteTab = new HashMap <> ();
  private final Map <String, Integer> m_aStateNameForComposite = new HashMap <> ();
  private final Map <String, int []> m_aCompositeStateTable = new HashMap <> ();
  private final Map <String, String> m_aStateBlockTable = new HashMap <> ();
  private final Map <String, int []> m_aStateSetsToFix = new HashMap <> ();
  private final Map <String, int []> m_aTableToDump = new HashMap <> ();
  private final List <int []> m_aOrderedStateSet = new ArrayList <> ();

  private boolean m_bUnicodeWarningGiven = false;
  private int m_nGeneratedStates = 0;
  private int m_nIdCnt = 0;
  private int m_nLoHiByteCnt;
  private int m_nDummyStateIndex = -1;
  private boolean m_bDone;
  /**
   * The number of the epsilon closure pass that is running. It is never rewound, not even between
   * lexical states, so that a state object which outlives the automaton it belonged to cannot match
   * a generation of the next one.
   */
  private int m_nPassGeneration = 0;
  /** The pass whose states count as having a finished closure, -1 for none. */
  private int m_nClosureDoneGeneration = -1;
  private boolean [] m_aStateDone;
  private List <NfaState> m_aAllStates = new ArrayList <> ();
  private boolean m_bJJCheckNAddStatesUnaryNeeded = false;
  private boolean m_bJJCheckNAddStatesDualNeeded = false;
  private List <String> m_aAllBitVectors = new ArrayList <> ();
  private int [] m_aTmpIndices = new int [512];
  private String m_sAllBits = "{\n   0xffffffffffffffffL, " +
                              "0xffffffffffffffffL, " +
                              "0xffffffffffffffffL, " +
                              "0xffffffffffffffffL\n};";
  private int m_nLastIndex = 0;
  private int [] [] m_aKinds;
  private int [] [] [] m_aStatesForState;

  /** Default constructor. */
  public NfaBuildState ()
  {}

  /**
   * Whether the "generated code will not compile without UNICODE_INPUT" warning has already been
   * given. It is issued once per run, not once per character above 0xff.
   *
   * @return <code>true</code> if the warning was already given.
   */
  public boolean isUnicodeWarningGiven ()
  {
    return m_bUnicodeWarningGiven;
  }

  /**
   * Whether the "generated code will not compile without UNICODE_INPUT" warning has already been
   * given. It is issued once per run, not once per character above 0xff.
   *
   * @param bUnicodeWarningGiven
   *        <code>true</code> once the warning has been given.
   */
  public void setUnicodeWarningGiven (final boolean bUnicodeWarningGiven)
  {
    m_bUnicodeWarningGiven = bUnicodeWarningGiven;
  }

  /**
   * How many NFA states of the current lexical state have been given a number. This is the size the
   * generated jjstateSet array needs.
   *
   * @return The number of numbered states.
   */
  public int getGeneratedStates ()
  {
    return m_nGeneratedStates;
  }

  /** {@return the current value, and increments it afterwards} */
  public int getAndIncGeneratedStates ()
  {
    return m_nGeneratedStates++;
  }

  /** {@return the current value, and increments it afterwards} */
  public int getAndIncIdCnt ()
  {
    return m_nIdCnt++;
  }

  /**
   * How many jjbitVec arrays have been emitted for non ASCII character tests. Each distinct 256 bit
   * vector gets one, and the counter names it.
   *
   * @return The number of vectors emitted so far.
   */
  public int getLoHiByteCnt ()
  {
    return m_nLoHiByteCnt;
  }

  /** {@return the current value, and increments it afterwards} */
  public int getAndIncLoHiByteCnt ()
  {
    return m_nLoHiByteCnt++;
  }

  /**
   * The state number handed out for the composite state that stands for "no state at all", or -1
   * while none has been needed yet.
   *
   * @return The state number, or -1.
   */
  public int getDummyStateIndex ()
  {
    return m_nDummyStateIndex;
  }

  /**
   * The state number handed out for the composite state that stands for "no state at all", or -1
   * while none has been needed yet.
   *
   * @param nDummyStateIndex
   *        The state number.
   */
  public void setDummyStateIndex (final int nDummyStateIndex)
  {
    m_nDummyStateIndex = nDummyStateIndex;
  }

  /**
   * The termination flag of the epsilon move optimisation, which keeps merging states until a full
   * pass changes nothing.
   *
   * @return <code>true</code> when the last pass changed nothing.
   */
  public boolean isDone ()
  {
    return m_bDone;
  }

  /**
   * The termination flag of the epsilon move optimisation, which keeps merging states until a full
   * pass changes nothing.
   *
   * @param bDone
   *        <code>true</code> when the last pass changed nothing.
   */
  public void setDone (final boolean bDone)
  {
    m_bDone = bDone;
  }

  /**
   * The epsilon closure walks the states over and over until a pass changes nothing, and each pass
   * has to know which states it has already been to. Rather than clearing a flag on every state
   * between passes, each pass gets a number and a visited state is stamped with it, so that a stale
   * stamp is recognised by not matching rather than by having been cleared.
   *
   * @return The number of the pass that is running.
   */
  public int getPassGeneration ()
  {
    return m_nPassGeneration;
  }

  /**
   * Start a new epsilon closure pass, which invalidates every stamp from the pass before.
   *
   * @return The number of the new pass.
   */
  public int nextPassGeneration ()
  {
    return ++m_nPassGeneration;
  }

  /**
   * The states stamped with this generation are the ones whose closure is finished, which is what
   * lets the walk stop early and what lets {@code computeClosures} skip them. It is the generation
   * of the last pass of the most recent closure computation, so a state that computation did not
   * reach stops counting as finished - which is what the per state flag this replaced also did.
   *
   * @return The generation, -1 before the first closure computation.
   */
  public int getClosureDoneGeneration ()
  {
    return m_nClosureDoneGeneration;
  }

  /**
   * Declare the states stamped with one generation to be the ones whose closure is finished.
   *
   * @param nGeneration
   *        The generation of the pass that just finished.
   */
  public void setClosureDoneGeneration (final int nGeneration)
  {
    m_nClosureDoneGeneration = nGeneration;
  }

  /**
   * One flag per numbered state, telling {@code getStateSetString} which members of a state set are
   * still live.
   *
   * @return The flags, indexed by state number. May be <code>null</code> before generation starts.
   */
  public boolean [] getStateDone ()
  {
    return m_aStateDone;
  }

  /**
   * One flag per numbered state, telling {@code getStateSetString} which members of a state set are
   * still live.
   *
   * @param aStateDone
   *        The flags, indexed by state number.
   */
  public void setStateDone (final boolean [] aStateDone)
  {
    m_aStateDone = aStateDone;
  }

  /**
   * Every NFA state created for the current lexical state, in creation order. A state's id is its
   * position here.
   *
   * @return The states. Never <code>null</code>.
   */
  public List <NfaState> getAllStates ()
  {
    return m_aAllStates;
  }

  /**
   * Every NFA state created for the current lexical state, in creation order. A state's id is its
   * position here.
   *
   * @param aAllStates
   *        The states.
   */
  public void setAllStates (final List <NfaState> aAllStates)
  {
    m_aAllStates = aAllStates;
  }

  /**
   * Whether any generated transition calls jjCheckNAddStates with a single index, which decides
   * whether that overload is emitted at all.
   *
   * @return <code>true</code> if the generated token manager needs it.
   */
  public boolean isJJCheckNAddStatesUnaryNeeded ()
  {
    return m_bJJCheckNAddStatesUnaryNeeded;
  }

  /**
   * Whether any generated transition calls jjCheckNAddStates with a single index, which decides
   * whether that overload is emitted at all.
   *
   * @param bJJCheckNAddStatesUnaryNeeded
   *        <code>true</code> if the generated token manager needs it.
   */
  public void setJJCheckNAddStatesUnaryNeeded (final boolean bJJCheckNAddStatesUnaryNeeded)
  {
    m_bJJCheckNAddStatesUnaryNeeded = bJJCheckNAddStatesUnaryNeeded;
  }

  /**
   * Whether any generated transition calls jjCheckNAddStates with a start and an end index, which
   * decides whether that overload is emitted at all.
   *
   * @return <code>true</code> if the generated token manager needs it.
   */
  public boolean isJJCheckNAddStatesDualNeeded ()
  {
    return m_bJJCheckNAddStatesDualNeeded;
  }

  /**
   * Whether any generated transition calls jjCheckNAddStates with a start and an end index, which
   * decides whether that overload is emitted at all.
   *
   * @param bJJCheckNAddStatesDualNeeded
   *        <code>true</code> if the generated token manager needs it.
   */
  public void setJJCheckNAddStatesDualNeeded (final boolean bJJCheckNAddStatesDualNeeded)
  {
    m_bJJCheckNAddStatesDualNeeded = bJJCheckNAddStatesDualNeeded;
  }

  /**
   * The text of every jjbitVec array emitted so far, in the order they were emitted, so that a
   * later state can ask whether a vector it needs is already there.
   *
   * @return The array initialisers. Never <code>null</code>.
   */
  public List <String> getAllBitVectors ()
  {
    return m_aAllBitVectors;
  }

  /**
   * Scratch space collecting the jjbitVec indices of one state's character ranges while its
   * transition is being written.
   *
   * @return The scratch array. Never <code>null</code>.
   */
  public int [] getTmpIndices ()
  {
    return m_aTmpIndices;
  }

  /**
   * The initialiser of a bit vector with every bit set, kept here so that a state whose vector
   * matches it can be recognised and generated as an unconditional move.
   *
   * @return The array initialiser text. Never <code>null</code>.
   */
  public String getAllBits ()
  {
    return m_sAllBits;
  }

  /**
   * How much of the generated jjnextStates array is in use. Every state set that goes in there
   * claims a contiguous run starting here.
   *
   * @return The next free index, and therefore the length the array needs.
   */
  public int getLastIndex ()
  {
    return m_nLastIndex;
  }

  /**
   * How much of the generated jjnextStates array is in use. Every state set that goes in there
   * claims a contiguous run starting here.
   *
   * @param nLastIndex
   *        The next free index.
   */
  public void setLastIndex (final int nLastIndex)
  {
    m_nLastIndex = nLastIndex;
  }

  /**
   * For each lexical state, the token kind each numbered state matches. Filled in when the lexical
   * state is generated and read again when the whole table is written out.
   *
   * @return The kinds, indexed by lexical state and then by state number. May be <code>null</code>
   *         before * generation starts.
   */
  public int [] [] getKinds ()
  {
    return m_aKinds;
  }

  /**
   * For each lexical state, the token kind each numbered state matches. Filled in when the lexical
   * state is generated and read again when the whole table is written out.
   *
   * @param aKinds
   *        The kinds, indexed by lexical state and then by state number.
   */
  public void setKinds (final int [] [] aKinds)
  {
    m_aKinds = aKinds;
  }

  /**
   * For each lexical state, the composite state sets each numbered state stands for. Filled in when
   * the lexical state is generated and read again when the whole table is written out.
   *
   * @return The state sets, indexed by lexical state and then by state number. May be
   *         <code>null</code> * before generation starts.
   */
  public int [] [] [] getStatesForState ()
  {
    return m_aStatesForState;
  }

  /**
   * For each lexical state, the composite state sets each numbered state stands for. Filled in when
   * the lexical state is generated and read again when the whole table is written out.
   *
   * @param aStatesForState
   *        The state sets, indexed by lexical state and then by state number.
   */
  public void setStatesForState (final int [] [] [] aStatesForState)
  {
    m_aStatesForState = aStatesForState;
  }

  /**
   * The NFA states that carry a state number, indexed by that number. A state only gets one once it
   * is reachable by a real character move, so this is a subset of {@link #getAllStates()}.
   *
   * @return The numbered states, indexed by state number. Never <code>null</code>.
   */
  @NonNull
  public List <NfaState> indexedAllStates ()
  {
    return m_aIndexedAllStates;
  }

  /**
   * The states whose non ASCII character test was large enough to be worth a jjCanMove_ method of
   * its own. The position here is the number in the method name.
   *
   * @return The states that got their own method. Never <code>null</code>.
   */
  @NonNull
  public List <NfaState> nonAsciiTableForMethod ()
  {
    return m_aNonAsciiTableForMethod;
  }

  /**
   * States that turned out to behave identically, keyed by a signature built from what they match
   * and where they go. The first state with a given signature stands in for all the others.
   *
   * @return The representative state per signature. Never <code>null</code>.
   */
  @NonNull
  public Map <String, NfaState> equivStatesTable ()
  {
    return m_aEquivStatesTable;
  }

  /**
   * The state sets that appear as a transition target, keyed by the string form of the set.
   *
   * @return The state numbers per state set string. Never <code>null</code>.
   */
  @NonNull
  public Map <String, int []> allNextStates ()
  {
    return m_aAllNextStates;
  }

  /**
   * The jjbitVec index of every distinct 256 bit vector emitted so far, keyed by the array
   * initialiser itself so that an identical vector is written only once.
   *
   * @return The vector index per initialiser text. Never <code>null</code>.
   */
  @NonNull
  public Map <String, Integer> loHiByteTab ()
  {
    return m_aLoHiByteTab;
  }

  /**
   * The synthesised state number that stands for a set of states, keyed by the string form of the
   * set. Turning a set into one number is what makes the generated automaton a DFA.
   *
   * @return The composite state number per state set string. Never <code>null</code>.
   */
  @NonNull
  public Map <String, Integer> stateNameForComposite ()
  {
    return m_aStateNameForComposite;
  }

  /**
   * The reverse of {@link #stateNameForComposite()}: the set of states each composite number stands
   * for, keyed by the same string form.
   *
   * @return The state numbers per state set string. Never <code>null</code>.
   */
  @NonNull
  public Map <String, int []> compositeStateTable ()
  {
    return m_aCompositeStateTable;
  }

  /**
   * The state sets that are entered as a block rather than as a start state, used as a set - key
   * and value are the same string. A block can be generated as one contiguous run of jjnextStates
   * entries.
   *
   * @return The state set strings that are blocks. Never <code>null</code>.
   */
  @NonNull
  public Map <String, String> stateBlockTable ()
  {
    return m_aStateBlockTable;
  }

  /**
   * State sets that had members removed because those members always occur together with another
   * state, keyed by the string form of the original set. The removed positions are -1.
   *
   * @return The patched state sets per original state set string. Never <code>null</code>.
   */
  @NonNull
  public Map <String, int []> stateSetsToFix ()
  {
    return m_aStateSetsToFix;
  }

  /**
   * Where each state set landed in the generated jjnextStates array, keyed by the string form of
   * the set: a two element array of the first and the last index.
   *
   * @return The index range per state set string. Never <code>null</code>.
   */
  @NonNull
  public Map <String, int []> tableToDump ()
  {
    return m_aTableToDump;
  }

  /**
   * The state sets in the order they go into the generated jjnextStates array, which is the order
   * the ranges in {@link #tableToDump()} refer to.
   *
   * @return The state sets in emission order. Never <code>null</code>.
   */
  @NonNull
  public List <int []> orderedStateSet ()
  {
    return m_aOrderedStateSet;
  }

  /**
   * Start over for the next lexical state. Mirrors what {@code NfaState.reInitStatic ()} used to
   * do: the NFA is rebuilt from scratch for every lexical state.
   */
  public void resetForLexicalState ()
  {
    m_nGeneratedStates = 0;
    m_nIdCnt = 0;
    m_nDummyStateIndex = -1;
    m_bDone = false;
    // Do not rewind the pass counter: a state object that outlives its lexical state must not
    // be able to match a generation of the next one
    m_nClosureDoneGeneration = -1;
    m_aStateDone = null;

    m_aAllStates.clear ();
    m_aIndexedAllStates.clear ();
    m_aEquivStatesTable.clear ();
    m_aAllNextStates.clear ();
    m_aCompositeStateTable.clear ();
    m_aStateBlockTable.clear ();
    m_aStateNameForComposite.clear ();
    m_aStateSetsToFix.clear ();
  }
}
