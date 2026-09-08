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
 * This is the instance state behind the static fields of
 * {@link com.helger.pgcc.parser.LexGenJava}. Some of it describes the whole run - the lexical state
 * names, the per token flags and the ordinal counter - and some of it is overwritten for each
 * lexical state as the generator walks them, which is why the fields that look like scratch really
 * are scratch.
 *
 * @author Philip Helger
 */
public final class LexerState
{
  private final Map <String, List <TokenProduction>> m_aAllTpsForState = new LinkedHashMap <> ();
  private final Map <String, NfaState> m_aInitStates = new LinkedHashMap <> ();

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
  private boolean [] m_aStateHasActions;
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

  public String getTokenMgrClassName ()
  {
    return m_sTokenMgrClassName;
  }

  public void setTokenMgrClassName (final String sTokenMgrClassName)
  {
    m_sTokenMgrClassName = sTokenMgrClassName;
  }

  public int getLexStateIndex ()
  {
    return m_nLexStateIndex;
  }

  public void setLexStateIndex (final int nLexStateIndex)
  {
    m_nLexStateIndex = nLexStateIndex;
  }

  public ETokenKind [] getKinds ()
  {
    return m_aKinds;
  }

  public void setKinds (final ETokenKind [] aKinds)
  {
    m_aKinds = aKinds;
  }

  public int getMaxOrdinal ()
  {
    return m_nMaxOrdinal;
  }

  public void setMaxOrdinal (final int nMaxOrdinal)
  {
    m_nMaxOrdinal = nMaxOrdinal;
  }

  public String getLexStateSuffix ()
  {
    return m_sLexStateSuffix;
  }

  public void setLexStateSuffix (final String sLexStateSuffix)
  {
    m_sLexStateSuffix = sLexStateSuffix;
  }

  public String [] getNewLexState ()
  {
    return m_aNewLexState;
  }

  public void setNewLexState (final String [] aNewLexState)
  {
    m_aNewLexState = aNewLexState;
  }

  public int [] getLexStates ()
  {
    return m_aLexStates;
  }

  public void setLexStates (final int [] aLexStates)
  {
    m_aLexStates = aLexStates;
  }

  public boolean [] getIgnoreCase ()
  {
    return m_aIgnoreCase;
  }

  public void setIgnoreCase (final boolean [] aIgnoreCase)
  {
    m_aIgnoreCase = aIgnoreCase;
  }

  public ExpAction [] getActions ()
  {
    return m_aActions;
  }

  public void setActions (final ExpAction [] aActions)
  {
    m_aActions = aActions;
  }

  public int getStateSetSize ()
  {
    return m_nStateSetSize;
  }

  public void setStateSetSize (final int nStateSetSize)
  {
    m_nStateSetSize = nStateSetSize;
  }

  public int getTotalNumStates ()
  {
    return m_nTotalNumStates;
  }

  public void setTotalNumStates (final int nTotalNumStates)
  {
    m_nTotalNumStates = nTotalNumStates;
  }

  public int getMaxLexStates ()
  {
    return m_nMaxLexStates;
  }

  public void setMaxLexStates (final int nMaxLexStates)
  {
    m_nMaxLexStates = nMaxLexStates;
  }

  public String [] getLexStateName ()
  {
    return m_aLexStateName;
  }

  public void setLexStateName (final String [] aLexStateName)
  {
    m_aLexStateName = aLexStateName;
  }

  public NfaState [] getSinglesToSkip ()
  {
    return m_aSinglesToSkip;
  }

  public void setSinglesToSkip (final NfaState [] aSinglesToSkip)
  {
    m_aSinglesToSkip = aSinglesToSkip;
  }

  public long [] getToSkip ()
  {
    return m_aToSkip;
  }

  public void setToSkip (final long [] aToSkip)
  {
    m_aToSkip = aToSkip;
  }

  public long [] getToSpecial ()
  {
    return m_aToSpecial;
  }

  public void setToSpecial (final long [] aToSpecial)
  {
    m_aToSpecial = aToSpecial;
  }

  public long [] getToMore ()
  {
    return m_aToMore;
  }

  public void setToMore (final long [] aToMore)
  {
    m_aToMore = aToMore;
  }

  public long [] getToToken ()
  {
    return m_aToToken;
  }

  public void setToToken (final long [] aToToken)
  {
    m_aToToken = aToToken;
  }

  public int getDefaultLexState ()
  {
    return m_nDefaultLexState;
  }

  public void setDefaultLexState (final int nDefaultLexState)
  {
    m_nDefaultLexState = nDefaultLexState;
  }

  public AbstractExpRegularExpression [] getRexprs ()
  {
    return m_aRexprs;
  }

  public void setRexprs (final AbstractExpRegularExpression [] aRexprs)
  {
    m_aRexprs = aRexprs;
  }

  public int [] getMaxLongsReqd ()
  {
    return m_aMaxLongsReqd;
  }

  public void setMaxLongsReqd (final int [] aMaxLongsReqd)
  {
    m_aMaxLongsReqd = aMaxLongsReqd;
  }

  public int [] getInitMatch ()
  {
    return m_aInitMatch;
  }

  public void setInitMatch (final int [] aInitMatch)
  {
    m_aInitMatch = aInitMatch;
  }

  public int [] getCanMatchAnyChar ()
  {
    return m_aCanMatchAnyChar;
  }

  public void setCanMatchAnyChar (final int [] aCanMatchAnyChar)
  {
    m_aCanMatchAnyChar = aCanMatchAnyChar;
  }

  public boolean isHasEmptyMatch ()
  {
    return m_bHasEmptyMatch;
  }

  public void setHasEmptyMatch (final boolean bHasEmptyMatch)
  {
    m_bHasEmptyMatch = bHasEmptyMatch;
  }

  public boolean [] getCanLoop ()
  {
    return m_aCanLoop;
  }

  public void setCanLoop (final boolean [] aCanLoop)
  {
    m_aCanLoop = aCanLoop;
  }

  public boolean [] getStateHasActions ()
  {
    return m_aStateHasActions;
  }

  public void setStateHasActions (final boolean [] aStateHasActions)
  {
    m_aStateHasActions = aStateHasActions;
  }

  public boolean isHasLoop ()
  {
    return m_bHasLoop;
  }

  public void setHasLoop (final boolean bHasLoop)
  {
    m_bHasLoop = bHasLoop;
  }

  public boolean [] getCanReachOnMore ()
  {
    return m_aCanReachOnMore;
  }

  public void setCanReachOnMore (final boolean [] aCanReachOnMore)
  {
    m_aCanReachOnMore = aCanReachOnMore;
  }

  public boolean [] getHasNfa ()
  {
    return m_aHasNfa;
  }

  public void setHasNfa (final boolean [] aHasNfa)
  {
    m_aHasNfa = aHasNfa;
  }

  public boolean [] getMixed ()
  {
    return m_aMixed;
  }

  public void setMixed (final boolean [] aMixed)
  {
    m_aMixed = aMixed;
  }

  public NfaState getInitialState ()
  {
    return m_aInitialState;
  }

  public void setInitialState (final NfaState aInitialState)
  {
    m_aInitialState = aInitialState;
  }

  public int getCurKind ()
  {
    return m_nCurKind;
  }

  public void setCurKind (final int nCurKind)
  {
    m_nCurKind = nCurKind;
  }

  public boolean isHasSkipActions ()
  {
    return m_bHasSkipActions;
  }

  public void setHasSkipActions (final boolean bHasSkipActions)
  {
    m_bHasSkipActions = bHasSkipActions;
  }

  public boolean isHasMoreActions ()
  {
    return m_bHasMoreActions;
  }

  public void setHasMoreActions (final boolean bHasMoreActions)
  {
    m_bHasMoreActions = bHasMoreActions;
  }

  public boolean isHasTokenActions ()
  {
    return m_bHasTokenActions;
  }

  public void setHasTokenActions (final boolean bHasTokenActions)
  {
    m_bHasTokenActions = bHasTokenActions;
  }

  public boolean isHasSpecial ()
  {
    return m_bHasSpecial;
  }

  public void setHasSpecial (final boolean bHasSpecial)
  {
    m_bHasSpecial = bHasSpecial;
  }

  public boolean isHasSkip ()
  {
    return m_bHasSkip;
  }

  public void setHasSkip (final boolean bHasSkip)
  {
    m_bHasSkip = bHasSkip;
  }

  public boolean isHasMore ()
  {
    return m_bHasMore;
  }

  public void setHasMore (final boolean bHasMore)
  {
    m_bHasMore = bHasMore;
  }

  public AbstractExpRegularExpression getCurRE ()
  {
    return m_aCurRE;
  }

  public void setCurRE (final AbstractExpRegularExpression aCurRE)
  {
    m_aCurRE = aCurRE;
  }

  public boolean isKeepLineCol ()
  {
    return m_bKeepLineCol;
  }

  public void setKeepLineCol (final boolean bKeepLineCol)
  {
    m_bKeepLineCol = bKeepLineCol;
  }

  public String getErrorHandlingClass ()
  {
    return m_sErrorHandlingClass;
  }

  public void setErrorHandlingClass (final String sErrorHandlingClass)
  {
    m_sErrorHandlingClass = sErrorHandlingClass;
  }

  public TokenizerData getTokenizerData ()
  {
    return m_aTokenizerData;
  }

  /** @return All token productions, grouped by the lexical state they belong to */
  @NonNull
  public Map <String, List <TokenProduction>> allTpsForState ()
  {
    return m_aAllTpsForState;
  }

  /** @return The initial NFA state of each lexical state */
  @NonNull
  public Map <String, NfaState> initStates ()
  {
    return m_aInitStates;
  }

  public boolean isGenerateDataOnly ()
  {
    return m_bGenerateDataOnly;
  }

  public void setGenerateDataOnly (final boolean bGenerateDataOnly)
  {
    m_bGenerateDataOnly = bGenerateDataOnly;
  }
}
