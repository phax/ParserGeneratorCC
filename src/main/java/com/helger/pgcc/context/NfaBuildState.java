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
 * This is the instance state behind the static fields of
 * {@link com.helger.pgcc.parser.NfaState}. Unlike the rest of the context it does not live for a
 * whole run: {@code LexGenJava.start ()} walks the lexical states and starts a fresh one for each,
 * which is what {@link LexerState#resetForLexicalState()} does.
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
  private boolean [] m_aMark;
  private boolean [] m_aStateDone;
  private List <NfaState> m_aAllStates = new ArrayList <> ();
  private boolean m_bJJCheckNAddStatesUnaryNeeded = false;
  private boolean m_bJJCheckNAddStatesDualNeeded = false;
  private List <String> m_aAllBitVectors = new ArrayList <> ();
  private int [] m_aTmpIndices = new int [512];
  private String m_sAllBits = "{\n   0xffffffffffffffffL, " + "0xffffffffffffffffL, " + "0xffffffffffffffffL, " + "0xffffffffffffffffL\n};";
  private int m_nLastIndex = 0;
  private int [] [] m_aKinds;
  private int [] [] [] m_aStatesForState;

  public boolean isUnicodeWarningGiven ()
  {
    return m_bUnicodeWarningGiven;
  }

  public void setUnicodeWarningGiven (final boolean bUnicodeWarningGiven)
  {
    m_bUnicodeWarningGiven = bUnicodeWarningGiven;
  }

  public int getGeneratedStates ()
  {
    return m_nGeneratedStates;
  }

  public void setGeneratedStates (final int nGeneratedStates)
  {
    m_nGeneratedStates = nGeneratedStates;
  }

  /** @return The current value, and increments it afterwards. */
  public int getAndIncGeneratedStates ()
  {
    return m_nGeneratedStates++;
  }

  public int getIdCnt ()
  {
    return m_nIdCnt;
  }

  public void setIdCnt (final int nIdCnt)
  {
    m_nIdCnt = nIdCnt;
  }

  /** @return The current value, and increments it afterwards. */
  public int getAndIncIdCnt ()
  {
    return m_nIdCnt++;
  }

  public int getLoHiByteCnt ()
  {
    return m_nLoHiByteCnt;
  }

  public void setLoHiByteCnt (final int nLoHiByteCnt)
  {
    m_nLoHiByteCnt = nLoHiByteCnt;
  }

  /** @return The current value, and increments it afterwards. */
  public int getAndIncLoHiByteCnt ()
  {
    return m_nLoHiByteCnt++;
  }

  public int getDummyStateIndex ()
  {
    return m_nDummyStateIndex;
  }

  public void setDummyStateIndex (final int nDummyStateIndex)
  {
    m_nDummyStateIndex = nDummyStateIndex;
  }

  public boolean isDone ()
  {
    return m_bDone;
  }

  public void setDone (final boolean bDone)
  {
    m_bDone = bDone;
  }

  public boolean [] getMark ()
  {
    return m_aMark;
  }

  public void setMark (final boolean [] aMark)
  {
    m_aMark = aMark;
  }

  public boolean [] getStateDone ()
  {
    return m_aStateDone;
  }

  public void setStateDone (final boolean [] aStateDone)
  {
    m_aStateDone = aStateDone;
  }

  public List <NfaState> getAllStates ()
  {
    return m_aAllStates;
  }

  public void setAllStates (final List <NfaState> aAllStates)
  {
    m_aAllStates = aAllStates;
  }

  public boolean isJJCheckNAddStatesUnaryNeeded ()
  {
    return m_bJJCheckNAddStatesUnaryNeeded;
  }

  public void setJJCheckNAddStatesUnaryNeeded (final boolean bJJCheckNAddStatesUnaryNeeded)
  {
    m_bJJCheckNAddStatesUnaryNeeded = bJJCheckNAddStatesUnaryNeeded;
  }

  public boolean isJJCheckNAddStatesDualNeeded ()
  {
    return m_bJJCheckNAddStatesDualNeeded;
  }

  public void setJJCheckNAddStatesDualNeeded (final boolean bJJCheckNAddStatesDualNeeded)
  {
    m_bJJCheckNAddStatesDualNeeded = bJJCheckNAddStatesDualNeeded;
  }

  public List <String> getAllBitVectors ()
  {
    return m_aAllBitVectors;
  }

  public void setAllBitVectors (final List <String> aAllBitVectors)
  {
    m_aAllBitVectors = aAllBitVectors;
  }

  public int [] getTmpIndices ()
  {
    return m_aTmpIndices;
  }

  public void setTmpIndices (final int [] aTmpIndices)
  {
    m_aTmpIndices = aTmpIndices;
  }

  public String getAllBits ()
  {
    return m_sAllBits;
  }

  public void setAllBits (final String sAllBits)
  {
    m_sAllBits = sAllBits;
  }

  public int getLastIndex ()
  {
    return m_nLastIndex;
  }

  public void setLastIndex (final int nLastIndex)
  {
    m_nLastIndex = nLastIndex;
  }

  public int [] [] getKinds ()
  {
    return m_aKinds;
  }

  public void setKinds (final int [] [] aKinds)
  {
    m_aKinds = aKinds;
  }

  public int [] [] [] getStatesForState ()
  {
    return m_aStatesForState;
  }

  public void setStatesForState (final int [] [] [] aStatesForState)
  {
    m_aStatesForState = aStatesForState;
  }
  /** @return indexedAllStates */
  @NonNull
  public List <NfaState> indexedAllStates ()
  {
    return m_aIndexedAllStates;
  }

  /** @return nonAsciiTableForMethod */
  @NonNull
  public List <NfaState> nonAsciiTableForMethod ()
  {
    return m_aNonAsciiTableForMethod;
  }

  /** @return equivStatesTable */
  @NonNull
  public Map <String, NfaState> equivStatesTable ()
  {
    return m_aEquivStatesTable;
  }

  /** @return allNextStates */
  @NonNull
  public Map <String, int []> allNextStates ()
  {
    return m_aAllNextStates;
  }

  /** @return loHiByteTab */
  @NonNull
  public Map <String, Integer> loHiByteTab ()
  {
    return m_aLoHiByteTab;
  }

  /** @return stateNameForComposite */
  @NonNull
  public Map <String, Integer> stateNameForComposite ()
  {
    return m_aStateNameForComposite;
  }

  /** @return compositeStateTable */
  @NonNull
  public Map <String, int []> compositeStateTable ()
  {
    return m_aCompositeStateTable;
  }

  /** @return stateBlockTable */
  @NonNull
  public Map <String, String> stateBlockTable ()
  {
    return m_aStateBlockTable;
  }

  /** @return stateSetsToFix */
  @NonNull
  public Map <String, int []> stateSetsToFix ()
  {
    return m_aStateSetsToFix;
  }

  /** @return tableToDump */
  @NonNull
  public Map <String, int []> tableToDump ()
  {
    return m_aTableToDump;
  }

  /** @return orderedStateSet */
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
    m_aMark = null;
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
