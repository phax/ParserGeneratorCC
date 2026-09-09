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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.NfaBuildState;
import com.helger.pgcc.context.TokenizerDataBuildState;
import com.helger.pgcc.context.TokenizerDataBuildState.CompositeStartState;
import com.helger.pgcc.output.AbstractLexGenJavaLike;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.TokenManagerDebug;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;

/**
 * The state of a Non-deterministic Finite Automaton.
 */
public class NfaState
{
  /**
   * {@return the build state of the lexical state that is currently being generated. Never
   * <code>null</code>.}
   */
  public static TokenizerDataBuildState tokenizerBuild ()
  {
    return AbstractLexGenJavaLike.lexer ().tokenizerDataBuild ();
  }

  /**
   * {@return the automaton being built for the lexical state that is currently being generated}
   */
  public static NfaBuildState nfa ()
  {
    return AbstractLexGenJavaLike.lexer ().nfa ();
  }

  /**
   * Reset the parts of the automaton that outlive one lexical state.
   */
  public static void reInitStatic ()
  {
    nfa ().resetForLexicalState ();
  }

  /**
   * The state's own data. Package private for the NFA construction, but read and written by the
   * lexer emitters in com.helger.pgcc.output.*, which is why they are public rather than package
   * private.
   */
  private long [] m_aAsciiMoves = new long [2];
  private char [] m_aCharMoves = null;
  private char [] m_aRangeMoves = null;
  private NfaState m_aNext = null;
  private NfaState m_aStateForCase;
  private final List <NfaState> m_aEpsilonMoves = new ArrayList <> ();
  private String m_sEpsilonMovesString;
  private final int m_nId;
  private int m_nStateName = -1;
  private int m_nKind = Integer.MAX_VALUE;
  private int m_nLookingFor;
  private int m_nUsefulEpsilonMoves = 0;
  private int m_nInNextOf;
  private int m_nLexState;
  private int m_nNonAsciiMethod = -1;
  private int m_nKindToPrint = Integer.MAX_VALUE;
  private boolean m_bDummy = false;
  private boolean m_bIsComposite = false;
  private int [] m_aCompositeStates = null;
  private boolean m_bIsFinal = false;
  private List <Integer> m_aLoByteVec;
  private int [] m_aNonAsciiMoveIndices;
  private int m_nRound = 0;
  private int m_nOnlyChar = 0;
  private char m_cMatchSingleChar;
  /**
   * The epsilon closure pass that last visited this state, 0 for none.
   * <p>
   * This one field answers both of the questions the closure walk asks, by being compared against
   * two different generations held by {@link NfaBuildState}: it equals
   * {@link NfaBuildState#getPassGeneration()} when the pass that is running has already been here,
   * and it equals {@link NfaBuildState#getClosureDoneGeneration()} when this state's closure is
   * finished. Passes are numbered from 1, so the initial 0 matches neither.
   */
  private int m_nClosureGeneration = 0;

  /**
   * Create a state and register it with the automaton of the lexical state that is currently being
   * built. The state's id is its position in {@link NfaBuildState#getAllStates()}.
   */
  public NfaState ()
  {
    final NfaBuildState aNfa = nfa ();
    m_nId = aNfa.getAndIncIdCnt ();
    aNfa.getAllStates ().add (this);
    m_nLexState = AbstractLexGenJavaLike.lexer ().getLexStateIndex ();
    m_nLookingFor = AbstractLexGenJavaLike.lexer ().getCurKind ();
  }

  @NonNull
  private NfaState _createClone ()
  {
    final NfaState aResult = new NfaState ();

    aResult.m_bIsFinal = m_bIsFinal;
    aResult.m_nKind = m_nKind;
    aResult.m_nLookingFor = m_nLookingFor;
    aResult.m_nLexState = m_nLexState;
    aResult.m_nInNextOf = m_nInNextOf;

    aResult._mergeMoves (this);

    return aResult;
  }

  private static void _insertInOrder (@NonNull final List <NfaState> v, @NonNull final NfaState s)
  {
    int nPos = 0;
    for (; nPos < v.size (); nPos++)
    {
      final NfaState aState = v.get (nPos);
      if (aState.m_nId > s.m_nId)
        break;
      if (aState.m_nId == s.m_nId)
        return;
    }

    v.add (nPos, s);
  }

  private static char [] _expandCharArr (@NonNull final char [] aOldArr, final int nIncr)
  {
    final char [] aRet = new char [aOldArr.length + nIncr];
    System.arraycopy (aOldArr, 0, aRet, 0, aOldArr.length);
    return aRet;
  }

  /**
   * Add an epsilon move from this state to another one.
   *
   * @param aNewState
   *        The target of the move. May not be <code>null</code>.
   */
  public void addMove (@NonNull final NfaState aNewState)
  {
    if (!m_aEpsilonMoves.contains (aNewState))
      _insertInOrder (m_aEpsilonMoves, aNewState);
  }

  private final void _addASCIIMove (final char c)
  {
    m_aAsciiMoves[c / 64] |= (1L << (c % 64));
  }

  /**
   * Make this state match one more character.
   *
   * @param c
   *        The character to add.
   */
  public void addChar (final char c)
  {
    final NfaBuildState aNfa = nfa ();
    m_nOnlyChar++;
    m_cMatchSingleChar = c;

    // ASCII char
    if (c < 128)
    {
      _addASCIIMove (c);
      return;
    }

    if (m_aCharMoves == null)
      m_aCharMoves = new char [10];

    int nLen = m_aCharMoves.length;

    if (m_aCharMoves[nLen - 1] != 0)
    {
      m_aCharMoves = _expandCharArr (m_aCharMoves, 10);
      nLen += 10;
    }

    int i = 0;
    for (; i < nLen; i++)
      if (m_aCharMoves[i] == 0 || m_aCharMoves[i] > c)
        break;

    if (!aNfa.isUnicodeWarningGiven () &&
        c > 0xff &&
        !Options.isJavaUnicodeEscape () &&
        !Options.isJavaUserCharStream ())
    {
      aNfa.setUnicodeWarningGiven (true);
      JavaCCErrors.warning (AbstractLexGenJavaLike.lexer ().getCurRE (),
                            "Non-ASCII characters used in regular expression.\n" +
                                                                         "Please make sure you use the correct Reader when you create the parser, " +
                                                                         "one that can handle your character set.");
    }

    char cTemp = m_aCharMoves[i];
    m_aCharMoves[i] = c;

    for (i++; i < nLen; i++)
    {
      if (cTemp == 0)
        break;

      final char cTemp1 = m_aCharMoves[i];
      m_aCharMoves[i] = cTemp;
      cTemp = cTemp1;
    }
  }

  /**
   * Make this state match one more range of characters, both ends included.
   *
   * @param cPleft
   *        The first character of the range.
   * @param cRight
   *        The last character of the range.
   */
  public void addRange (final char cPleft, final char cRight)
  {
    final NfaBuildState aNfa = nfa ();
    char cLeft = cPleft;
    m_nOnlyChar = 2;
    char cTempLeft1, cTempLeft2, cTempRight1, cTempRight2;

    if (cLeft < 128)
    {
      if (cRight < 128)
      {
        for (; cLeft <= cRight; cLeft++)
          _addASCIIMove (cLeft);

        return;
      }

      for (; cLeft < 128; cLeft++)
        _addASCIIMove (cLeft);
    }

    if (!aNfa.isUnicodeWarningGiven () &&
        (cLeft > 0xff || cRight > 0xff) &&
        !Options.isJavaUnicodeEscape () &&
        !Options.isJavaUserCharStream ())
    {
      aNfa.setUnicodeWarningGiven (true);
      JavaCCErrors.warning (AbstractLexGenJavaLike.lexer ().getCurRE (),
                            "Non-ASCII characters used in regular expression.\n" +
                                                                         "Please make sure you use the correct Reader when you create the parser, " +
                                                                         "one that can handle your character set.");
    }

    if (m_aRangeMoves == null)
      m_aRangeMoves = new char [20];

    int nLen = m_aRangeMoves.length;

    if (m_aRangeMoves[nLen - 1] != 0)
    {
      m_aRangeMoves = _expandCharArr (m_aRangeMoves, 20);
      nLen += 20;
    }

    int i = 0;
    for (; i < nLen; i += 2)
      if (m_aRangeMoves[i] == 0 ||
          (m_aRangeMoves[i] > cLeft) ||
          ((m_aRangeMoves[i] == cLeft) && (m_aRangeMoves[i + 1] > cRight)))
        break;

    cTempLeft1 = m_aRangeMoves[i];
    cTempRight1 = m_aRangeMoves[i + 1];
    m_aRangeMoves[i] = cLeft;
    m_aRangeMoves[i + 1] = cRight;

    for (i += 2; i < nLen; i += 2)
    {
      if (cTempLeft1 == 0)
        break;

      cTempLeft2 = m_aRangeMoves[i];
      cTempRight2 = m_aRangeMoves[i + 1];
      m_aRangeMoves[i] = cTempLeft1;
      m_aRangeMoves[i + 1] = cTempRight1;
      cTempLeft1 = cTempLeft2;
      cTempRight1 = cTempRight2;
    }
  }

  // From hereon down all the functions are used for code generation

  /**
   * The ascii moves.
   *
   * @return The value of m_aAsciiMoves.
   */
  public long [] getAsciiMoves ()
  {
    return m_aAsciiMoves;
  }

  /**
   * The ascii moves.
   *
   * @param aValue
   *        The new value of m_aAsciiMoves.
   */
  public void setAsciiMoves (final long [] aValue)
  {
    m_aAsciiMoves = aValue;
  }

  /**
   * The char moves.
   *
   * @return The value of m_aCharMoves.
   */
  public char [] getCharMoves ()
  {
    return m_aCharMoves;
  }

  /**
   * The char moves.
   *
   * @param aValue
   *        The new value of m_aCharMoves.
   */
  public void setCharMoves (final char [] aValue)
  {
    m_aCharMoves = aValue;
  }

  /**
   * The next.
   *
   * @return The value of m_aNext.
   */
  public NfaState getNext ()
  {
    return m_aNext;
  }

  /**
   * The next.
   *
   * @param aValue
   *        The new value of m_aNext.
   */
  public void setNext (final NfaState aValue)
  {
    m_aNext = aValue;
  }

  /**
   * The epsilon moves.
   *
   * @return The value of m_aEpsilonMoves.
   */
  public List <NfaState> getEpsilonMoves ()
  {
    return m_aEpsilonMoves;
  }

  /**
   * The state name.
   *
   * @return The value of m_nStateName.
   */
  public int getStateName ()
  {
    return m_nStateName;
  }

  /**
   * The state name.
   *
   * @param aValue
   *        The new value of m_nStateName.
   */
  public void setStateName (final int aValue)
  {
    m_nStateName = aValue;
  }

  /**
   * The kind.
   *
   * @return The value of m_nKind.
   */
  public int getKind ()
  {
    return m_nKind;
  }

  /**
   * The kind.
   *
   * @param aValue
   *        The new value of m_nKind.
   */
  public void setKind (final int aValue)
  {
    m_nKind = aValue;
  }

  /**
   * The in next of.
   *
   * @return The value of m_nInNextOf.
   */
  public int getInNextOf ()
  {
    return m_nInNextOf;
  }

  /**
   * The in next of.
   *
   * @param aValue
   *        The new value of m_nInNextOf.
   */
  public void setInNextOf (final int aValue)
  {
    m_nInNextOf = aValue;
  }

  /**
   * The dummy.
   *
   * @return The value of m_bDummy.
   */
  public boolean isDummy ()
  {
    return m_bDummy;
  }

  /**
   * The dummy.
   *
   * @param aValue
   *        The new value of m_bDummy.
   */
  public void setDummy (final boolean aValue)
  {
    m_bDummy = aValue;
  }

  /**
   * The is final.
   *
   * @return The value of m_bIsFinal.
   */
  public boolean isFinal ()
  {
    return m_bIsFinal;
  }

  /**
   * The is final.
   *
   * @param aValue
   *        The new value of m_bIsFinal.
   */
  public void setFinal (final boolean aValue)
  {
    m_bIsFinal = aValue;
  }

  /**
   * This function computes the closure and also updates the kind so that any time there is a move
   * to this state, it can go on epsilon to a new state in the epsilon moves that might have a lower
   * kind of token number for the same length.
   */

  /**
   * Add everything reachable from this state without consuming a character to its own set of
   * epsilon moves, and do the same for everything it reaches.
   * <p>
   * The walk stops at a state whose closure is already finished and at one this pass has already
   * been to; {@link #m_nClosureGeneration} answers both questions.
   */
  private void _recursiveEpsilonClosure ()
  {
    final NfaBuildState aNfa = nfa ();
    // Already finished, or already been here in this pass
    if (m_nClosureGeneration == aNfa.getClosureDoneGeneration () || m_nClosureGeneration == aNfa.getPassGeneration ())
      return;

    m_nClosureGeneration = aNfa.getPassGeneration ();

    // Recursively do closure
    for (final NfaState aReached : m_aEpsilonMoves)
      aReached._recursiveEpsilonClosure ();

    // Operate on copy!
    for (final NfaState aReached : new ArrayList <> (m_aEpsilonMoves))
    {
      for (final NfaState aReachedFromThere : aReached.m_aEpsilonMoves)
      {
        if (aReachedFromThere._isUsefulState () && !m_aEpsilonMoves.contains (aReachedFromThere))
        {
          _insertInOrder (m_aEpsilonMoves, aReachedFromThere);
          aNfa.setDone (false);
        }
      }

      if (m_nKind > aReached.m_nKind)
        m_nKind = aReached.m_nKind;
    }

    if (hasTransitions () && !m_aEpsilonMoves.contains (this))
      _insertInOrder (m_aEpsilonMoves, this);
  }

  private boolean _isUsefulState ()
  {
    return m_bIsFinal || hasTransitions ();
  }

  /**
   * {@return <code>true</code> if this state matches any character at all - a state with only
   * epsilon moves does not}
   */
  public boolean hasTransitions ()
  {
    return m_aAsciiMoves[0] != 0L ||
           m_aAsciiMoves[1] != 0L ||
           (m_aCharMoves != null && m_aCharMoves[0] != 0) ||
           (m_aRangeMoves != null && m_aRangeMoves[0] != 0);
  }

  private void _mergeMoves (@NonNull final NfaState aSource)
  {
    // Warning : This function does not merge epsilon moves
    if (m_aAsciiMoves == aSource.m_aAsciiMoves)
      JavaCCErrors.internalError ();

    m_aAsciiMoves[0] = m_aAsciiMoves[0] | aSource.m_aAsciiMoves[0];
    m_aAsciiMoves[1] = m_aAsciiMoves[1] | aSource.m_aAsciiMoves[1];

    if (aSource.m_aCharMoves != null)
    {
      if (m_aCharMoves == null)
        m_aCharMoves = aSource.m_aCharMoves;
      else
      {
        final char [] aTmpCharMoves = new char [m_aCharMoves.length + aSource.m_aCharMoves.length];
        System.arraycopy (m_aCharMoves, 0, aTmpCharMoves, 0, m_aCharMoves.length);
        m_aCharMoves = aTmpCharMoves;

        for (final char aCharMove : aSource.m_aCharMoves)
          addChar (aCharMove);
      }
    }

    if (aSource.m_aRangeMoves != null)
    {
      if (m_aRangeMoves == null)
        m_aRangeMoves = aSource.m_aRangeMoves;
      else
      {
        final char [] aTmpRangeMoves = new char [m_aRangeMoves.length + aSource.m_aRangeMoves.length];
        System.arraycopy (m_aRangeMoves, 0, aTmpRangeMoves, 0, m_aRangeMoves.length);
        m_aRangeMoves = aTmpRangeMoves;
        for (int i = 0; i < aSource.m_aRangeMoves.length; i += 2)
          addRange (aSource.m_aRangeMoves[i], aSource.m_aRangeMoves[i + 1]);
      }
    }

    if (aSource.m_nKind < m_nKind)
      m_nKind = aSource.m_nKind;

    if (aSource.m_nKindToPrint < m_nKindToPrint)
      m_nKindToPrint = aSource.m_nKindToPrint;

    m_bIsFinal |= aSource.m_bIsFinal;
  }

  NfaState createEquivState (@NonNull final List <NfaState> aStates)
  {
    final NfaState aNewState = aStates.get (0)._createClone ();

    aNewState.m_aNext = new NfaState ();

    _insertInOrder (aNewState.m_aNext.m_aEpsilonMoves, aStates.get (0).m_aNext);

    for (int i = 1; i < aStates.size (); i++)
    {
      final NfaState aThirdState = (aStates.get (i));

      if (aThirdState.m_nKind < aNewState.m_nKind)
        aNewState.m_nKind = aThirdState.m_nKind;

      aNewState.m_bIsFinal |= aThirdState.m_bIsFinal;

      _insertInOrder (aNewState.m_aNext.m_aEpsilonMoves, aThirdState.m_aNext);
    }

    return aNewState;
  }

  private NfaState _getEquivalentRunTimeState ()
  {
    final List <NfaState> aAllStates = nfa ().getAllStates ();
    Outer: for (int i = aAllStates.size (); i-- > 0;)
    {
      final NfaState aCandidate = aAllStates.get (i);

      if (this != aCandidate &&
          aCandidate.m_nStateName != -1 &&
          m_nKindToPrint == aCandidate.m_nKindToPrint &&
          m_aAsciiMoves[0] == aCandidate.m_aAsciiMoves[0] &&
          m_aAsciiMoves[1] == aCandidate.m_aAsciiMoves[1] &&
          Arrays.equals (m_aCharMoves, aCandidate.m_aCharMoves) &&
          Arrays.equals (m_aRangeMoves, aCandidate.m_aRangeMoves))
      {
        if (m_aNext == aCandidate.m_aNext)
          return aCandidate;
        if (m_aNext != null && aCandidate.m_aNext != null)
        {
          if (m_aNext.m_aEpsilonMoves.size () == aCandidate.m_aNext.m_aEpsilonMoves.size ())
          {
            for (int j = 0; j < m_aNext.m_aEpsilonMoves.size (); j++)
              if (m_aNext.m_aEpsilonMoves.get (j) != aCandidate.m_aNext.m_aEpsilonMoves.get (j))
                continue Outer;

            return aCandidate;
          }
        }
      }
    }

    return null;
  }

  // generates code (without outputting it) and returns the name used.
  /**
   * Give this state and everything reachable from it a state number, which is what turns the NFA
   * into something that can be written out. A state that already has one is left alone.
   */
  public void generateCode ()
  {
    final NfaBuildState aNfa = nfa ();
    if (m_nStateName != -1)
      return;

    if (m_aNext != null)
    {
      m_aNext.generateCode ();
      if (m_aNext.m_nKind != Integer.MAX_VALUE)
        m_nKindToPrint = m_aNext.m_nKind;
    }

    if (m_nStateName == -1 && hasTransitions ())
    {
      final NfaState aState = _getEquivalentRunTimeState ();

      if (aState != null)
      {
        m_nStateName = aState.m_nStateName;
        // ????
        // tmp.inNextOf += inNextOf;
        // ????
        m_bDummy = true;
        return;
      }

      m_nStateName = aNfa.getAndIncGeneratedStates ();
      aNfa.indexedAllStates ().add (this);
      _generateNextStatesCode ();
    }
  }

  /**
   * Work out the epsilon closure of every state and merge the states that turn out to be
   * equivalent. This is the optimisation pass that decides how large the generated automaton ends
   * up.
   */
  public static void computeClosures ()
  {
    final NfaBuildState aNfa = nfa ();
    // Back to front
    for (int i = aNfa.getAllStates ().size () - 1; i >= 0; --i)
    {
      final NfaState aState = aNfa.getAllStates ().get (i);
      if (aState.m_nClosureGeneration != aNfa.getClosureDoneGeneration ())
        aState._optimizeEpsilonMoves (true);
    }

    // Operate on copy!
    for (final NfaState tmp : new ArrayList <> (aNfa.getAllStates ()))
      if (tmp.m_nClosureGeneration != aNfa.getClosureDoneGeneration ())
        tmp._optimizeEpsilonMoves (false);

    if (false)
    {
      for (int i = 0; i < aNfa.getAllStates ().size (); i++)
      {
        final NfaState aState = aNfa.getAllStates ().get (i);
        final NfaState [] aEpsilonMoveArray = new NfaState [aState.m_aEpsilonMoves.size ()];
        aState.m_aEpsilonMoves.toArray (aEpsilonMoveArray);
      }
    }
  }

  /**
   * Close this state's epsilon moves over the whole graph, then shrink the resulting set.
   * <p>
   * After the closure the set can hold states that are indistinguishable, and every one of them
   * costs a case in the generated token manager. Two passes remove them, repeated until neither
   * finds anything: states that consume the same characters collapse into one equivalent state, and
   * states that lead to the same place keep one of their number with the others' character moves
   * merged in.
   * <h2>Why the passes are numbered</h2> The closure used to track its progress with a
   * <code>boolean []</code> over every state plus a <code>closureDone</code> flag on each state.
   * That cost two walks of the whole state list per call - one <code>Arrays.fill</code> to clear
   * the array before each pass, and one loop afterwards to copy the array into the per state flags
   * - and {@code computeClosures} calls this method once per state. Two walks of n states, n times,
   * is quadratic in a grammar's token count for work that carries no information: for a synthetic
   * 1280 token lexer that copy alone ran 690 million times and was 70% of the whole generator run.
   * <p>
   * Numbering the passes says the same thing without the walks. A state stamps itself with the
   * number of the pass that visits it, so a stamp from an earlier pass is recognised by not
   * matching rather than by having been cleared, and the states the final pass reached are named
   * afterwards by recording that pass's number rather than by copying anything. The semantics are
   * unchanged, including the part that is easy to miss: a state the last pass did not reach stops
   * counting as finished, exactly as the old copy loop overwrote its flag with <code>false</code>.
   *
   * @param bOptReqd
   *        <code>false</code> to do the closure only and skip the shrinking.
   */
  private void _optimizeEpsilonMoves (final boolean bOptReqd)
  {
    // First do epsilon closure. nfa () walks a ThreadLocal, so take it once rather than per element
    final NfaBuildState aNfa = nfa ();
    aNfa.setDone (false);
    int nPass = 0;
    while (!aNfa.isDone ())
    {
      // A fresh number invalidates every stamp from the pass before, so nothing has to be cleared
      nPass = aNfa.nextPassGeneration ();
      aNfa.setDone (true);
      _recursiveEpsilonClosure ();
    }

    // The states the last pass reached are the ones whose closure is now finished. Naming that
    // pass is the whole of the bookkeeping the copy loop used to do.
    aNfa.setClosureDoneGeneration (nPass);

    // Warning : The following piece of code is just an optimization.
    // in case of trouble, just remove this piece.
    {
      boolean bSometingOptimized = true;

      NfaState aReplacement = null;
      NfaState aMove, aOtherMove;
      List <NfaState> aEquivStates = null;

      while (bSometingOptimized)
      {
        bSometingOptimized = false;

        // Pass one: states that consume exactly the same characters are interchangeable, so they
        // collapse into a single equivalent state that carries all their kinds
        for (int i = 0; bOptReqd && i < m_aEpsilonMoves.size (); i++)
        {
          aMove = m_aEpsilonMoves.get (i);
          if (aMove.hasTransitions ())
          {
            for (int j = i + 1; j < m_aEpsilonMoves.size (); j++)
            {
              aOtherMove = m_aEpsilonMoves.get (j);
              if (aOtherMove.hasTransitions () &&
                  (aMove.m_aAsciiMoves[0] == aOtherMove.m_aAsciiMoves[0] &&
                   aMove.m_aAsciiMoves[1] == aOtherMove.m_aAsciiMoves[1] &&
                   Arrays.equals (aMove.m_aCharMoves, aOtherMove.m_aCharMoves) &&
                   Arrays.equals (aMove.m_aRangeMoves, aOtherMove.m_aRangeMoves)))
              {
                if (aEquivStates == null)
                {
                  aEquivStates = new ArrayList <> ();
                  aEquivStates.add (aMove);
                }

                _insertInOrder (aEquivStates, aOtherMove);
                m_aEpsilonMoves.remove (j--);
              }
            }
          }

          if (aEquivStates != null)
          {
            bSometingOptimized = true;
            // The set of merged ids is the key, so that the same combination is only built once
            final StringBuilder aKeySB = new StringBuilder (aEquivStates.size () * 6);
            for (final NfaState aEquivState : aEquivStates)
              aKeySB.append (aEquivState.m_nId).append (", ");
            final String sEquivKey = aKeySB.toString ();

            aReplacement = aNfa.equivStatesTable ().get (sEquivKey);
            if (aReplacement == null)
            {
              aReplacement = createEquivState (aEquivStates);
              aNfa.equivStatesTable ().put (sEquivKey, aReplacement);
            }

            m_aEpsilonMoves.remove (i--);
            m_aEpsilonMoves.add (aReplacement);
            aEquivStates = null;
            aReplacement = null;
          }
        }

        // Pass two: states that lead to the same place can be replaced by one of them, with the
        // character moves of the others merged into it
        for (int i = 0; i < m_aEpsilonMoves.size (); i++)
        {
          aMove = m_aEpsilonMoves.get (i);

          for (int j = i + 1; j < m_aEpsilonMoves.size (); j++)
          {
            aOtherMove = m_aEpsilonMoves.get (j);

            if (aMove.m_aNext == aOtherMove.m_aNext)
            {
              if (aReplacement == null)
              {
                aReplacement = aMove._createClone ();
                aReplacement.m_aNext = aMove.m_aNext;
                bSometingOptimized = true;
              }

              aReplacement._mergeMoves (aOtherMove);
              m_aEpsilonMoves.remove (j--);
            }
          }

          if (aReplacement != null)
          {
            m_aEpsilonMoves.remove (i--);
            m_aEpsilonMoves.add (aReplacement);
            aReplacement = null;
          }
        }
      }
    }
    // End Warning

    // Generate an array of states for epsilon moves (not vector)
    if (!m_aEpsilonMoves.isEmpty ())
    {
      for (int i = 0; i < m_aEpsilonMoves.size (); i++)
        // Since we are doing a closure, just epsilon moves are unnecessary
        if (m_aEpsilonMoves.get (i).hasTransitions ())
          m_nUsefulEpsilonMoves++;
        else
          m_aEpsilonMoves.remove (i--);
    }
  }

  private void _generateNextStatesCode ()
  {
    if (m_aNext.m_nUsefulEpsilonMoves > 0)
      m_aNext._getEpsilonMovesString ();
  }

  private String _getEpsilonMovesString ()
  {
    final int [] aStateNames = new int [m_nUsefulEpsilonMoves];
    int nCnt = 0;

    if (m_sEpsilonMovesString != null)
      return m_sEpsilonMovesString;

    final NfaBuildState aNfa = nfa ();

    if (m_nUsefulEpsilonMoves > 0)
    {
      NfaState aTempState;
      m_sEpsilonMovesString = "{ ";
      for (final NfaState m_epsilonMove : m_aEpsilonMoves)
      {
        aTempState = m_epsilonMove;
        if (aTempState.hasTransitions ())
        {
          if (aTempState.m_nStateName == -1)
            aTempState.generateCode ();

          aNfa.indexedAllStates ().get (aTempState.m_nStateName).m_nInNextOf++;
          aStateNames[nCnt] = aTempState.m_nStateName;
          m_sEpsilonMovesString += aTempState.m_nStateName + ", ";
          if (nCnt++ > 0 && nCnt % 16 == 0)
            m_sEpsilonMovesString += "\n";
        }
      }

      m_sEpsilonMovesString += "};";
    }

    m_nUsefulEpsilonMoves = nCnt;
    if (m_sEpsilonMovesString != null && aNfa.allNextStates ().get (m_sEpsilonMovesString) == null)
    {
      final int [] aStatesToPut = new int [m_nUsefulEpsilonMoves];

      System.arraycopy (aStateNames, 0, aStatesToPut, 0, nCnt);
      aNfa.allNextStates ().put (m_sEpsilonMovesString, aStatesToPut);
    }

    return m_sEpsilonMovesString;
  }

  /**
   * {@return <code>true</code> if the given character can begin a match in the current lexical
   * state}
   *
   * @param c
   *        The character to test. Must be below 128.
   */
  public static boolean canStartNfaUsingAscii (final char c)
  {
    final NfaBuildState aNfa = nfa ();
    if (c >= 128)
      JavaCCErrors.internalError ();

    final String s = AbstractLexGenJavaLike.lexer ().getInitialState ()._getEpsilonMovesString ();

    if (s == null || s.equals ("null;"))
      return false;

    final int [] aStates = aNfa.allNextStates ().get (s);

    for (final int nStateName : aStates)
    {
      final NfaState aState = aNfa.indexedAllStates ().get (nStateName);

      if ((aState.m_aAsciiMoves[c / 64] & (1L << c % 64)) != 0L)
        return true;
    }

    return false;
  }

  private boolean _canMoveUsingChar (final char c)
  {
    if (m_nOnlyChar == 1)
      return c == m_cMatchSingleChar;

    if (c < 128)
      return (m_aAsciiMoves[c / 64] & (1L << c % 64)) != 0L;

    // Just check directly if there is a move for this char
    if (m_aCharMoves != null && m_aCharMoves[0] != 0)
    {
      for (final char aCharMove : m_aCharMoves)
      {
        if (c == aCharMove)
          return true;
        if (c < aCharMove || aCharMove == 0)
          break;
      }
    }

    // For ranges, iterate thru the table to see if the current char
    // is in some range
    if (m_aRangeMoves != null && m_aRangeMoves[0] != 0)
      for (int i = 0; i < m_aRangeMoves.length; i += 2)
      {
        if (c >= m_aRangeMoves[i] && c <= m_aRangeMoves[i + 1])
          return true;
        if (c < m_aRangeMoves[i] || m_aRangeMoves[i] == 0)
          break;
      }
    // return (nextForNegatedList != null);
    return false;
  }

  /**
   * Find the first position at or after nPos where this state matches, used by the interpreter
   * rather than by the code generator.
   *
   * @param s
   *        The input being scanned. May not be <code>null</code>.
   * @param nPos
   *        The position to start looking at.
   * @param nLen
   *        The position to stop before.
   * @return The position of the first match, or nLen if there is none.
   */
  public int getFirstValidPos (@NonNull final String s, final int nPos, final int nLen)
  {
    int i = nPos;
    if (m_nOnlyChar == 1)
    {
      final char c = m_cMatchSingleChar;
      while (c != s.charAt (i) && ++i < nLen)
      {}
      return i;
    }

    do
    {
      if (_canMoveUsingChar (s.charAt (i)))
        return i;
    } while (++i < nLen);

    return i;
  }

  /**
   * Follow this state's transition for one character, adding everything it leads to.
   *
   * @param c
   *        The character being consumed.
   * @param aNewStates
   *        Collects the states reached. May not be <code>null</code>.
   * @return The kind matched here, or {@link Integer#MAX_VALUE} if the character does not move.
   */
  public int moveFrom (final char c, final List <NfaState> aNewStates)
  {
    if (_canMoveUsingChar (c))
    {
      for (int i = m_aNext.m_aEpsilonMoves.size (); i-- > 0;)
        _insertInOrder (aNewStates, m_aNext.m_aEpsilonMoves.get (i));

      return m_nKindToPrint;
    }

    return Integer.MAX_VALUE;
  }

  /**
   * Follow the transition of every state in a set for one character.
   *
   * @param c
   *        The character being consumed.
   * @param states
   *        The states to move from. May not be <code>null</code>.
   * @param aNewStates
   *        Collects the states reached. May not be <code>null</code>.
   * @return The lowest kind matched, or {@link Integer#MAX_VALUE} if nothing matched.
   */
  public static int moveFromSet (final char c, @NonNull final List <NfaState> states, final List <NfaState> aNewStates)
  {
    int nRetVal = Integer.MAX_VALUE;

    for (int i = states.size (); i-- > 0;)
    {
      final int nTmp = states.get (i).moveFrom (c, aNewStates);
      if (nRetVal > nTmp)
        nRetVal = nTmp;
    }

    return nRetVal;
  }

  /**
   * The same as {@link #moveFromSet(char, List, List)} for the interpreter, which keeps its state
   * sets in arrays and marks visited states with a round number rather than clearing them.
   *
   * @param c
   *        The character being consumed.
   * @param aStates
   *        The states to move from. May not be <code>null</code>.
   * @param aNewStates
   *        Collects the states reached. May not be <code>null</code>.
   * @param nRound
   *        The current round, used to tell a stale entry from a fresh one.
   * @return The number of states written to aNewStates.
   */
  public static int moveFromSetForRegEx (final char c,
                                         @NonNull final NfaState [] aStates,
                                         @NonNull final NfaState [] aNewStates,
                                         final int nRound)
  {
    int nStart = 0;
    final int nSz = aStates.length;

    for (int i = 0; i < nSz; i++)
    {
      final NfaState aOtherState = aStates[i];
      if (aOtherState == null)
        break;

      if (aOtherState._canMoveUsingChar (c))
      {
        if (aOtherState.m_nKindToPrint != Integer.MAX_VALUE)
        {
          aNewStates[nStart] = null;
          return 1;
        }

        final List <NfaState> v = aOtherState.m_aNext.m_aEpsilonMoves;
        for (int j = v.size () - 1; j >= 0; j--)
        {
          final NfaState aThirdState = v.get (j);
          if (aThirdState.m_nRound != nRound)
          {
            aThirdState.m_nRound = nRound;
            aNewStates[nStart++] = aThirdState;
          }
        }
      }
    }

    aNewStates[nStart] = null;
    return Integer.MAX_VALUE;
  }

  /*
   * This function generates the bit vectors of low and hi bytes for common bit vectors and returns
   * those that are not common with anything (in loBytes) and returns an array of indices that can
   * be used to generate the function names for char matching using the common bit vectors. It also
   * generates code to match a char with the common bit vectors. (Need a better comment).
   */

  private void _generateNonAsciiMoves (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    int i = 0, j = 0;
    int nCnt = 0;
    final long [] [] aLoBytes = new long [256] [4];

    if ((m_aCharMoves == null || m_aCharMoves[0] == 0) && (m_aRangeMoves == null || m_aRangeMoves[0] == 0))
      return;

    if (m_aCharMoves != null)
    {
      for (i = 0; i < m_aCharMoves.length; i++)
      {
        if (m_aCharMoves[i] == 0)
          break;

        final char cHiByte = (char) (m_aCharMoves[i] >> 8);
        aLoBytes[cHiByte][(m_aCharMoves[i] & 0xff) / 64] |= (1L << ((m_aCharMoves[i] & 0xff) % 64));
      }
    }

    if (m_aRangeMoves != null)
    {
      for (i = 0; i < m_aRangeMoves.length; i += 2)
      {
        if (m_aRangeMoves[i] == 0)
          break;

        char c, r;

        r = (char) (m_aRangeMoves[i + 1] & 0xff);
        char cHiByte = (char) (m_aRangeMoves[i] >> 8);

        if (cHiByte == (char) (m_aRangeMoves[i + 1] >> 8))
        {
          for (c = (char) (m_aRangeMoves[i] & 0xff); c <= r; c++)
            aLoBytes[cHiByte][c / 64] |= (1L << (c % 64));

          continue;
        }

        for (c = (char) (m_aRangeMoves[i] & 0xff); c <= 0xff; c++)
          aLoBytes[cHiByte][c / 64] |= (1L << (c % 64));

        while (++cHiByte < (char) (m_aRangeMoves[i + 1] >> 8))
        {
          aLoBytes[cHiByte][0] |= 0xffffffffffffffffL;
          aLoBytes[cHiByte][1] |= 0xffffffffffffffffL;
          aLoBytes[cHiByte][2] |= 0xffffffffffffffffL;
          aLoBytes[cHiByte][3] |= 0xffffffffffffffffL;
        }

        for (c = 0; c <= r; c++)
          aLoBytes[cHiByte][c / 64] |= (1L << (c % 64));
      }
    }

    long [] aCommon = null;
    final boolean [] aDone = new boolean [256];

    for (i = 0; i <= 255; i++)
    {
      if (aDone[i] ||
          (aDone[i] = aLoBytes[i][0] == 0 && aLoBytes[i][1] == 0 && aLoBytes[i][2] == 0 && aLoBytes[i][3] == 0))
        continue;

      for (j = i + 1; j < 256; j++)
      {
        if (aDone[j])
          continue;

        if (aLoBytes[i][0] == aLoBytes[j][0] &&
            aLoBytes[i][1] == aLoBytes[j][1] &&
            aLoBytes[i][2] == aLoBytes[j][2] &&
            aLoBytes[i][3] == aLoBytes[j][3])
        {
          aDone[j] = true;
          if (aCommon == null)
          {
            aDone[i] = true;
            aCommon = new long [4];
            aCommon[i / 64] |= (1L << (i % 64));
          }

          aCommon[j / 64] |= (1L << (j % 64));
        }
      }

      if (aCommon != null)
      {
        String sTmp = "{\n   " +
                      eOutputLanguage.getLongHex (aCommon[0]) +
                      ", " +
                      eOutputLanguage.getLongHex (aCommon[1]) +
                      ", " +
                      eOutputLanguage.getLongHex (aCommon[2]) +
                      ", " +
                      eOutputLanguage.getLongHex (aCommon[3]) +
                      "\n};";
        Integer aInd = aNfa.loHiByteTab ().get (sTmp);
        if (aInd == null)
        {
          aNfa.getAllBitVectors ().add (sTmp);

          if (!allBitsSet (sTmp))
          {
            aCodeGenerator.genStaticArrayDeclaration (eOutputLanguage.getTypeLong (),
                                                      "jjbitVec" + aNfa.getLoHiByteCnt ());
            aCodeGenerator.genCodeLine (sTmp);
          }
          aInd = Integer.valueOf (aNfa.getAndIncLoHiByteCnt ());
          aNfa.loHiByteTab ().put (sTmp, aInd);
        }

        aNfa.getTmpIndices ()[nCnt++] = aInd.intValue ();

        sTmp = "{\n   " +
               eOutputLanguage.getLongHex (aLoBytes[i][0]) +
               ", " +
               eOutputLanguage.getLongHex (aLoBytes[i][1]) +
               ", " +
               eOutputLanguage.getLongHex (aLoBytes[i][2]) +
               ", " +
               eOutputLanguage.getLongHex (aLoBytes[i][3]) +
               "\n};";
        aInd = aNfa.loHiByteTab ().get (sTmp);
        if (aInd == null)
        {
          aNfa.getAllBitVectors ().add (sTmp);

          if (!allBitsSet (sTmp))
          {
            aCodeGenerator.genStaticArrayDeclaration (eOutputLanguage.getTypeLong (),
                                                      "jjbitVec" + aNfa.getLoHiByteCnt ());
            aCodeGenerator.genCodeLine (sTmp);
            // This one, unlike the other two, returns to the main file afterwards
            if (!eOutputLanguage.isJava ())
              aCodeGenerator.switchToMainFile ();
          }
          aInd = Integer.valueOf (aNfa.getAndIncLoHiByteCnt ());
          aNfa.loHiByteTab ().put (sTmp, aInd);
        }

        aNfa.getTmpIndices ()[nCnt++] = aInd.intValue ();

        aCommon = null;
      }
    }

    m_aNonAsciiMoveIndices = new int [nCnt];
    System.arraycopy (aNfa.getTmpIndices (), 0, m_aNonAsciiMoveIndices, 0, nCnt);

    /*
     * System.out.println("state : " + stateName + " cnt : " + cnt); while (cnt > 0) {
     * System.out.print(nonAsciiMoveIndices[cnt - 1] + ", " + nonAsciiMoveIndices[cnt - 2] + ", ");
     * cnt -= 2; } System.out.println("");
     */

    for (i = 0; i < 256; i++)
    {
      if (aDone[i])
        aLoBytes[i] = null;
      else
      {
        // System.out.print(i + ", ");
        final String sTmp = "{\n   " +
                            eOutputLanguage.getLongHex (aLoBytes[i][0]) +
                            ", " +
                            eOutputLanguage.getLongHex (aLoBytes[i][1]) +
                            ", " +
                            eOutputLanguage.getLongHex (aLoBytes[i][2]) +
                            ", " +
                            eOutputLanguage.getLongHex (aLoBytes[i][3]) +
                            "\n};";

        Integer aInd = aNfa.loHiByteTab ().get (sTmp);
        if (aInd == null)
        {
          aNfa.getAllBitVectors ().add (sTmp);

          if (!allBitsSet (sTmp))
            aCodeGenerator.genStaticArrayDeclaration (eOutputLanguage.getTypeLong (),
                                                      "jjbitVec" + aNfa.getLoHiByteCnt ());
          aCodeGenerator.genCodeLine (sTmp);
          aNfa.loHiByteTab ().put (sTmp, aInd = Integer.valueOf (aNfa.getAndIncLoHiByteCnt ()));
        }

        if (m_aLoByteVec == null)
          m_aLoByteVec = new ArrayList <> ();

        m_aLoByteVec.add (Integer.valueOf (i));
        m_aLoByteVec.add (aInd);
      }
    }
    // System.out.println("");
    _updateDuplicateNonAsciiMoves ();
  }

  private void _updateDuplicateNonAsciiMoves ()
  {
    final NfaBuildState aNfa = nfa ();
    for (int i = 0; i < aNfa.nonAsciiTableForMethod ().size (); i++)
    {
      final NfaState aState = aNfa.nonAsciiTableForMethod ().get (i);
      if (_equalLoByteVectors (m_aLoByteVec, aState.m_aLoByteVec) &&
          Arrays.equals (m_aNonAsciiMoveIndices, aState.m_aNonAsciiMoveIndices))
      {
        m_nNonAsciiMethod = i;
        return;
      }
    }

    m_nNonAsciiMethod = aNfa.nonAsciiTableForMethod ().size ();
    aNfa.nonAsciiTableForMethod ().add (this);
  }

  /**
   * Compare two low byte vectors.
   * <p>
   * This is {@link List#equals(Object)} with one deliberate difference: two <code>null</code>
   * vectors are <em>not</em> equal here. A state with no low byte vector is not interchangeable
   * with another one that has none, so {@code Objects.equals} would be wrong.
   *
   * @param aVec1
   *        The first vector. May be <code>null</code>.
   * @param aVec2
   *        The second vector. May be <code>null</code>.
   * @return <code>true</code> if both are present and hold the same values.
   */
  private static boolean _equalLoByteVectors (@Nullable final List <Integer> aVec1,
                                              @Nullable final List <Integer> aVec2)
  {
    return aVec1 != null && aVec2 != null && aVec1.equals (aVec2);
  }

  static boolean allBitsSet (@NonNull final String sBitVec)
  {
    return sBitVec.equals (nfa ().getAllBits ());
  }

  /**
   * Register a set of states as the start of a match and hand back the single state number that
   * stands for it.
   *
   * @param sStateSetString
   *        The set in its string form. May be <code>null</code>.
   * @return The composite state number.
   */
  public static int addStartStateSet (final String sStateSetString)
  {
    return _addCompositeStateSet (sStateSetString, true);
  }

  private static int _addCompositeStateSet (final String sStateSetString, final boolean bStarts)
  {
    final NfaBuildState aNfa = nfa ();
    Integer aStateNameToReturn = aNfa.stateNameForComposite ().get (sStateSetString);

    if (aStateNameToReturn != null)
      return aStateNameToReturn.intValue ();

    int nToRet = 0;
    final int [] aNameSet = aNfa.allNextStates ().get (sStateSetString);

    if (!bStarts)
      aNfa.stateBlockTable ().put (sStateSetString, sStateSetString);

    if (aNameSet == null)
      JavaCCErrors.internalError ();

    if (aNameSet.length == 1)
    {
      aStateNameToReturn = Integer.valueOf (aNameSet[0]);
      aNfa.stateNameForComposite ().put (sStateSetString, aStateNameToReturn);
      return aNameSet[0];
    }

    for (final int aStateName : aNameSet)
    {
      if (aStateName == -1)
        continue;

      final NfaState aMember = aNfa.indexedAllStates ().get (aStateName);
      aMember.m_bIsComposite = true;
      aMember.m_aCompositeStates = aNameSet;
    }

    while (nToRet < aNameSet.length && (bStarts && aNfa.indexedAllStates ().get (aNameSet[nToRet]).m_nInNextOf > 1))
      nToRet++;

    for (final String s : aNfa.compositeStateTable ().keySet ())
    {
      if (!s.equals (sStateSetString) && _intersect (sStateSetString, s))
      {
        final int [] aOverlappingSet = aNfa.compositeStateTable ().get (s);

        while (nToRet < aNameSet.length &&
               ((bStarts && aNfa.indexedAllStates ().get (aNameSet[nToRet]).m_nInNextOf > 1) ||
                _elemOccurs (aNameSet[nToRet], aOverlappingSet) >= 0))
          nToRet++;
      }
    }

    int nTmp;

    if (nToRet >= aNameSet.length)
    {
      if (aNfa.getDummyStateIndex () == -1)
        aNfa.setDummyStateIndex (aNfa.getGeneratedStates ());
      else
        aNfa.setDummyStateIndex (aNfa.getDummyStateIndex () + 1);
      nTmp = aNfa.getDummyStateIndex ();

    }
    else
      nTmp = aNameSet[nToRet];

    aStateNameToReturn = Integer.valueOf (nTmp);
    aNfa.stateNameForComposite ().put (sStateSetString, aStateNameToReturn);
    aNfa.compositeStateTable ().put (sStateSetString, aNameSet);

    return nTmp;
  }

  private static int _stateNameForComposite (final String sStateSetString)
  {
    return nfa ().stateNameForComposite ().get (sStateSetString).intValue ();
  }

  /**
   * {@return the state number the generated automaton starts a match in, or -1 if the lexical state
   * has no NFA at all}
   */
  public static int initStateName ()
  {
    final String s = AbstractLexGenJavaLike.lexer ().getInitialState ()._getEpsilonMovesString ();

    if (AbstractLexGenJavaLike.lexer ().getInitialState ().m_nUsefulEpsilonMoves != 0)
      return _stateNameForComposite (s);
    return -1;
  }

  /**
   * {@return the state number that stands for everything reachable from this state without
   * consuming a character}
   */
  public int generateInitMoves ()
  {
    _getEpsilonMovesString ();

    if (m_sEpsilonMovesString == null)
      m_sEpsilonMovesString = "null;";

    return addStartStateSet (m_sEpsilonMovesString);
  }

  private static int [] _getStateSetIndicesForUse (final String sArrayString)
  {
    final NfaBuildState aNfa = nfa ();
    int [] aRet;
    final int [] aSet = aNfa.allNextStates ().get (sArrayString);

    aRet = aNfa.tableToDump ().get (sArrayString);
    if (aRet == null)
    {
      aRet = new int [2];
      aRet[0] = aNfa.getLastIndex ();
      aRet[1] = aNfa.getLastIndex () + aSet.length - 1;
      aNfa.setLastIndex (aNfa.getLastIndex () + aSet.length);
      aNfa.tableToDump ().put (sArrayString, aRet);
      aNfa.orderedStateSet ().add (aSet);
    }

    return aRet;
  }

  /**
   * Write the jjnextStates array, which holds every state set the generated automaton moves into,
   * one after the other.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpStateSets (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();

    aCodeGenerator.genStaticArrayDeclaration ("int", "jjnextStates");
    aCodeGenerator.genCode ("{");
    if (!aNfa.orderedStateSet ().isEmpty ())
    {
      int nCnt = 0;
      for (final int [] set : aNfa.orderedStateSet ())
      {
        for (final int nStateName : set)
        {
          if (nCnt++ % 16 == 0)
            aCodeGenerator.genCode ("\n   ");

          aCodeGenerator.genCode (nStateName + ", ");
        }
      }
    }
    else
      aCodeGenerator.genCode ("0");

    aCodeGenerator.genCodeLine ("\n};");
    aCodeGenerator.switchToMainFile ();
  }

  private static String _getStateSetString (@NonNull final int [] aStates)
  {
    String sRetVal = "{ ";
    for (int i = 0; i < aStates.length;)
    {
      sRetVal += aStates[i] + ", ";

      if (i++ > 0 && i % 16 == 0)
        sRetVal += "\n";
    }

    sRetVal += "};";
    nfa ().allNextStates ().put (sRetVal, aStates);
    return sRetVal;
  }

  /**
   * Turn a set of states into the string that identifies it in the tables of
   * {@link com.helger.pgcc.context.NfaBuildState}, registering the set on the way.
   *
   * @param aStates
   *        The states. May be <code>null</code>.
   * @return The string form, "null;" for an empty set. Never <code>null</code>.
   */
  public static String getStateSetString (@Nullable final List <NfaState> aStates)
  {
    if (aStates == null || aStates.isEmpty ())
      return "null;";

    final int [] aSet = new int [aStates.size ()];
    String sRetVal = "{ ";
    for (int i = 0; i < aStates.size ();)
    {
      final int k = aStates.get (i).m_nStateName;
      sRetVal += k + ", ";
      aSet[i] = k;

      if (i++ > 0 && (i % 16) == 0)
        sRetVal += "\n";
    }

    sRetVal += "};";
    nfa ().allNextStates ().put (sRetVal, aSet);
    return sRetVal;
  }

  private static int _numberOfBitsSet (final long l)
  {
    int nRet = 0;
    for (int i = 0; i < 63; i++)
      if (((l >> i) & 1L) != 0L)
        nRet++;

    return nRet;
  }

  private static int _isOnlyOneBitSet (final long l)
  {
    int nOneSeen = -1;
    for (int i = 0; i < 64; i++)
      if (((l >> i) & 1L) != 0L)
      {
        if (nOneSeen >= 0)
          return -1;
        nOneSeen = i;
      }

    return nOneSeen;
  }

  private static int _elemOccurs (final int nElem, @NonNull final int [] aArr)
  {
    for (int i = aArr.length; i-- > 0;)
      if (aArr[i] == nElem)
        return i;

    return -1;
  }

  @SuppressWarnings ("unused")
  private boolean _findCommonBlocks ()
  {
    final NfaBuildState aNfa = nfa ();
    if (m_aNext == null || m_aNext.m_nUsefulEpsilonMoves <= 1)
      return false;

    if (aNfa.getStateDone () == null)
      aNfa.setStateDone (new boolean [aNfa.getGeneratedStates ()]);

    final String sSet = m_aNext.m_sEpsilonMovesString;

    final int [] aNameSet = aNfa.allNextStates ().get (sSet);

    if (aNameSet.length <= 2 || aNfa.compositeStateTable ().get (sSet) != null)
      return false;

    final int freq[] = new int [aNameSet.length];
    final boolean live[] = new boolean [aNameSet.length];
    final int [] aCount = new int [aNfa.allNextStates ().size ()];

    for (int i = 0; i < aNameSet.length; i++)
    {
      if (aNameSet[i] != -1)
      {
        live[i] = !aNfa.getStateDone ()[aNameSet[i]];
        if (live[i])
          aCount[0]++;
      }
    }

    int nBlockLen = 0, commonFreq = 0;
    boolean bNeedUpdate;

    for (final Map.Entry <String, int []> aEntry : aNfa.allNextStates ().entrySet ())
    {
      final int [] aTmpSet = aEntry.getValue ();
      if (aTmpSet == aNameSet)
        continue;

      bNeedUpdate = false;
      for (int j = 0; j < aNameSet.length; j++)
      {
        if (aNameSet[j] == -1)
          continue;

        if (live[j] && _elemOccurs (aNameSet[j], aTmpSet) >= 0)
        {
          if (!bNeedUpdate)
          {
            bNeedUpdate = true;
            commonFreq++;
          }

          aCount[freq[j]]--;
          aCount[commonFreq]++;
          freq[j] = commonFreq;
        }
      }

      if (bNeedUpdate)
      {
        int nFoundFreq = -1;
        nBlockLen = 0;

        for (int j = 0; j <= commonFreq; j++)
          if (aCount[j] > nBlockLen)
          {
            nFoundFreq = j;
            nBlockLen = aCount[j];
          }

        if (nBlockLen <= 1)
          return false;

        for (int j = 0; j < aNameSet.length; j++)
          if (aNameSet[j] != -1 && freq[j] != nFoundFreq)
          {
            live[j] = false;
            aCount[freq[j]]--;
          }
      }
    }

    if (nBlockLen <= 1)
      return false;

    final int [] aCommonBlock = new int [nBlockLen];
    int nCnt = 0;
    // System.out.println("Common Block for " + set + " :");
    for (int i = 0; i < aNameSet.length; i++)
    {
      if (live[i])
      {
        if (aNfa.indexedAllStates ().get (aNameSet[i]).m_bIsComposite)
          return false;

        aNfa.getStateDone ()[aNameSet[i]] = true;
        aCommonBlock[nCnt++] = aNameSet[i];
        // System.out.print(nameSet[i] + ", ");
      }
    }

    // System.out.println("");

    final String s = _getStateSetString (aCommonBlock);

    Outer: for (final Map.Entry <String, int []> aEntry : aNfa.allNextStates ().entrySet ())
    {
      boolean bFirstOne = true;
      final String sStringToFix = aEntry.getKey ();
      final int [] aSetToFix = aEntry.getValue ();

      if (aSetToFix == aCommonBlock)
        continue;

      for (int k = 0; k < nCnt; k++)
      {
        final int nAt = _elemOccurs (aCommonBlock[k], aSetToFix);
        if (nAt < 0)
          continue Outer;
        if (!bFirstOne)
          aSetToFix[nAt] = -1;
        bFirstOne = false;
      }

      if (aNfa.stateSetsToFix ().get (sStringToFix) == null)
        aNfa.stateSetsToFix ().put (sStringToFix, aSetToFix);
    }

    m_aNext.m_nUsefulEpsilonMoves -= nBlockLen - 1;
    _addCompositeStateSet (s, false);
    return true;
  }

  @SuppressWarnings ("unused")
  private boolean _checkNextOccursTogether ()
  {
    final NfaBuildState aNfa = nfa ();
    if (m_aNext == null || m_aNext.m_nUsefulEpsilonMoves <= 1)
      return true;

    final String sSet = m_aNext.m_sEpsilonMovesString;

    final int [] aNameSet = aNfa.allNextStates ().get (sSet);

    if (aNameSet.length == 1 ||
        aNfa.compositeStateTable ().get (sSet) != null ||
        aNfa.stateSetsToFix ().get (sSet) != null)
      return false;

    final Map <String, int []> aOccursIn = new HashMap <> ();
    final NfaState aState = aNfa.getAllStates ().get (aNameSet[0]);

    for (int i = 1; i < aNameSet.length; i++)
    {
      final NfaState aOtherState = aNfa.getAllStates ().get (aNameSet[i]);

      if (aState.m_nInNextOf != aOtherState.m_nInNextOf)
        return false;
    }

    for (final Map.Entry <String, int []> aEntry : aNfa.allNextStates ().entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] aTmpSet = aEntry.getValue ();

      if (aTmpSet == aNameSet)
        continue;

      int nIsPresent = 0;
      int j = 0;
      for (final int nStateName : aNameSet)
      {
        if (_elemOccurs (nStateName, aTmpSet) >= 0)
          nIsPresent++;
        else
          if (nIsPresent > 0)
            return false;
        j++;
      }

      if (nIsPresent == j)
      {
        if (aTmpSet.length > aNameSet.length)
          aOccursIn.put (s, aTmpSet);

        // May not need. But safe.
        if (aNfa.compositeStateTable ().get (s) != null || aNfa.stateSetsToFix ().get (s) != null)
          return false;
      }
      else
        if (nIsPresent != 0)
          return false;
    }

    for (final Map.Entry <String, int []> aEntry : aOccursIn.entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] aSetToFix = aEntry.getValue ();

      if (!aNfa.stateSetsToFix ().containsKey (s))
        aNfa.stateSetsToFix ().put (s, aSetToFix);

      for (int k = 0; k < aSetToFix.length; k++)
      {
        // Not >= since need the first one (0)
        if (_elemOccurs (aSetToFix[k], aNameSet) > 0)
          aSetToFix[k] = -1;
      }
    }

    m_aNext.m_nUsefulEpsilonMoves = 1;
    _addCompositeStateSet (m_aNext.m_sEpsilonMovesString, false);
    return true;
  }

  private static void _fixStateSets ()
  {
    final NfaBuildState aNfa = nfa ();
    final Map <String, int []> aFixedSets = new HashMap <> ();
    final int [] aFixedSet = new int [aNfa.getGeneratedStates ()];

    for (final Map.Entry <String, int []> aEntry : aNfa.stateSetsToFix ().entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] aToFix = aEntry.getValue ();
      int nCnt = 0;

      // System.out.print("Fixing : ");
      for (final int nStateName : aToFix)
      {
        // System.out.print(toFix[i] + ", ");
        if (nStateName != -1)
          aFixedSet[nCnt++] = nStateName;
      }

      final int [] aFixed = new int [nCnt];
      System.arraycopy (aFixedSet, 0, aFixed, 0, nCnt);
      aFixedSets.put (s, aFixed);
      aNfa.allNextStates ().put (s, aFixed);
      // System.out.println(" as " + GetStateSetString(fixed));
    }

    for (final NfaState tmpState : aNfa.getAllStates ())
    {
      if (tmpState.m_aNext == null || tmpState.m_aNext.m_nUsefulEpsilonMoves == 0)
        continue;

      /*
       * if (compositeStateTable.get(tmpState.next.epsilonMovesString) != null)
       * tmpState.next.usefulEpsilonMoves = 1; else
       */
      final int [] aNewSet = aFixedSets.get (tmpState.m_aNext.m_sEpsilonMovesString);
      if (aNewSet != null)
        tmpState._fixNextStates (aNewSet);
    }
  }

  private final void _fixNextStates (@NonNull final int [] aNewSet)
  {
    m_aNext.m_nUsefulEpsilonMoves = aNewSet.length;
    // next.epsilonMovesString = GetStateSetString(newSet);
  }

  private static boolean _intersect (@Nullable final String sSet1, @Nullable final String sSet2)
  {
    if (sSet1 == null || sSet2 == null)
      return false;

    final Map <String, int []> aAllNextStates = nfa ().allNextStates ();
    final int [] aNameSet1 = aAllNextStates.get (sSet1);
    final int [] aNameSet2 = aAllNextStates.get (sSet2);

    if (aNameSet1 == null || aNameSet2 == null)
      return false;

    if (aNameSet1 == aNameSet2)
      return true;

    for (int i = aNameSet1.length; i-- > 0;)
      for (int j = aNameSet2.length; j-- > 0;)
        if (aNameSet1[i] == aNameSet2[j])
          return true;

    return false;
  }

  private static void _dumpHeadForCase (@NonNull final AbstractCodeGenerator aCodeGenerator, final int nByteNum)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    if (nByteNum == 0)
    {
      aCodeGenerator.genCodeLine ("         " + eOutputLanguage.getTypeLong () + " l = 1L << curChar;");
      switch (eOutputLanguage)
      {
        case JAVA:
          // Nothing
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("         (void)l;");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    else
      if (nByteNum == 1)
      {
        aCodeGenerator.genCodeLine ("         " + eOutputLanguage.getTypeLong () + " l = 1L << (curChar & 077);");
        switch (eOutputLanguage)
        {
          case JAVA:
            // Nothing
            break;
          case CPP:
            aCodeGenerator.genCodeLine ("         (void)l;");
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }
      }
      else
      {
        if (Options.isJavaUnicodeEscape () || nfa ().isUnicodeWarningGiven ())
        {
          aCodeGenerator.genCodeLine ("         int hiByte = (curChar >> 8);");
          aCodeGenerator.genCodeLine ("         int i1 = hiByte >> 6;");
          aCodeGenerator.genCodeLine ("         " + eOutputLanguage.getTypeLong () + " l1 = 1L << (hiByte & 077);");
        }

        aCodeGenerator.genCodeLine ("         int i2 = (curChar & 0xff) >> 6;");
        aCodeGenerator.genCodeLine ("         " + eOutputLanguage.getTypeLong () + " l2 = 1L << (curChar & 077);");
      }

    // codeGenerator.genCodeLine(" MatchLoop: do");
    aCodeGenerator.genCodeLine ("         do");
    aCodeGenerator.genCodeLine ("         {");

    aCodeGenerator.genCodeLine ("            switch(jjstateSet[--i])");
    aCodeGenerator.genCodeLine ("            {");
  }

  private static List <List <NfaState>> _partitionStatesSetForAscii (@NonNull final int [] aStates, final int nByteNum)
  {
    final int [] aCardinalities = new int [aStates.length];
    List <NfaState> aOriginal = new ArrayList <> ();
    final List <List <NfaState>> aPartition = new ArrayList <> ();
    NfaState aState;

    for (@SuppressWarnings ("unused")
    final int x : aStates)
      aOriginal.add (null);

    int nCnt = 0;
    for (int i = 0; i < aStates.length; i++)
    {
      aState = nfa ().getAllStates ().get (aStates[i]);

      if (aState.m_aAsciiMoves[nByteNum] != 0L)
      {
        int j;
        final int p = _numberOfBitsSet (aState.m_aAsciiMoves[nByteNum]);

        for (j = 0; j < i; j++)
          if (aCardinalities[j] <= p)
            break;

        for (int k = i; k > j; k--)
          aCardinalities[k] = aCardinalities[k - 1];

        aCardinalities[j] = p;

        aOriginal.add (j, aState);
        nCnt++;
      }
    }

    // original.setSize (cnt);
    while (aOriginal.size () < nCnt)
      aOriginal.add (null);
    if (aOriginal.size () > nCnt)
      aOriginal = aOriginal.subList (0, nCnt);

    while (!aOriginal.isEmpty ())
    {
      aState = aOriginal.remove (0);

      long nBitVec = aState.m_aAsciiMoves[nByteNum];
      final List <NfaState> aSubSet = new ArrayList <> ();
      aSubSet.add (aState);

      for (int j = 0; j < aOriginal.size (); j++)
      {
        final NfaState aOtherState = aOriginal.get (j);

        if ((aOtherState.m_aAsciiMoves[nByteNum] & nBitVec) == 0L)
        {
          nBitVec |= aOtherState.m_aAsciiMoves[nByteNum];
          aSubSet.add (aOtherState);
          aOriginal.remove (j--);
        }
      }

      aPartition.add (aSubSet);
    }

    return aPartition;
  }

  private String _printNoBreak (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                final int nByteNum,
                                @NonNull final boolean [] aDumped)
  {
    if (m_nInNextOf != 1)
      JavaCCErrors.internalError ();

    aDumped[m_nStateName] = true;

    if (nByteNum >= 0)
    {
      if (m_aAsciiMoves[nByteNum] != 0L)
      {
        aCodeGenerator.genCodeLine ("               case " + m_nStateName + ":");
        _dumpAsciiMoveForCompositeState (aCodeGenerator, nByteNum, false);
        return "";
      }
    }
    else
      if (m_nNonAsciiMethod != -1)
      {
        aCodeGenerator.genCodeLine ("               case " + m_nStateName + ":");
        _dumpNonAsciiMoveForCompositeState (aCodeGenerator);
        return "";
      }

    return ("               case " + m_nStateName + ":\n");
  }

  private static void _dumpCompositeStatesAsciiMoves (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                                      final String sKey,
                                                      final int nByteNum,
                                                      @NonNull final boolean [] aDumped)
  {
    final NfaBuildState aNfa = nfa ();
    final int [] aNameSet = aNfa.allNextStates ().get (sKey);

    if (aNameSet.length == 1 || aDumped[_stateNameForComposite (sKey)])
      return;

    NfaState aToBePrinted = null;
    int nNeededStates = 0;
    NfaState aStateForCase = null;
    String sToPrint = "";
    final boolean bStateBlock = (aNfa.stateBlockTable ().get (sKey) != null);

    for (final int nStateName : aNameSet)
    {
      final NfaState aState = aNfa.getAllStates ().get (nStateName);

      if (aState.m_aAsciiMoves[nByteNum] != 0L)
      {
        if (nNeededStates++ == 1)
          break;
        aToBePrinted = aState;
      }
      else
        aDumped[aState.m_nStateName] = true;

      if (aState.m_aStateForCase != null)
      {
        if (aStateForCase != null)
          JavaCCErrors.internalError ();

        aStateForCase = aState.m_aStateForCase;
      }
    }

    if (aStateForCase != null)
      sToPrint = aStateForCase._printNoBreak (aCodeGenerator, nByteNum, aDumped);

    if (nNeededStates == 0)
    {
      if (aStateForCase != null && sToPrint.length () == 0)
        aCodeGenerator.genCodeLine ("                  break;");
      return;
    }

    if (nNeededStates == 1)
    {
      // if (byteNum == 1)
      // System.out.println(toBePrinted.stateName + " is the only state for "
      // + key + " ; and key is : " + StateNameForComposite(key));

      if (StringHelper.isNotEmpty (sToPrint))
        aCodeGenerator.genCode (sToPrint);

      aCodeGenerator.genCodeLine ("               case " + _stateNameForComposite (sKey) + ":");

      if (!aDumped[aToBePrinted.m_nStateName] && !bStateBlock && aToBePrinted.m_nInNextOf > 1)
        aCodeGenerator.genCodeLine ("               case " + aToBePrinted.m_nStateName + ":");

      aDumped[aToBePrinted.m_nStateName] = true;
      aToBePrinted._dumpAsciiMove (aCodeGenerator, nByteNum, aDumped);
      return;
    }

    final List <List <NfaState>> aPartition = _partitionStatesSetForAscii (aNameSet, nByteNum);

    if (StringHelper.isNotEmpty (sToPrint))
      aCodeGenerator.genCode (sToPrint);

    final int nKeyState = _stateNameForComposite (sKey);
    aCodeGenerator.genCodeLine ("               case " + nKeyState + ":");
    if (nKeyState < aNfa.getGeneratedStates ())
      aDumped[nKeyState] = true;

    for (final List <NfaState> subSet : aPartition)
    {
      int nIndex = 0;
      for (final NfaState tmp : subSet)
      {
        if (bStateBlock)
          aDumped[tmp.m_nStateName] = true;
        tmp._dumpAsciiMoveForCompositeState (aCodeGenerator, nByteNum, nIndex != 0);
        ++nIndex;
      }
    }

    if (bStateBlock)
      aCodeGenerator.genCodeLine ("                  break;");
    else
      aCodeGenerator.genCodeLine ("                  break;");
  }

  private boolean _selfLoop ()
  {
    if (m_aNext == null || m_aNext.m_sEpsilonMovesString == null)
      return false;

    final int [] aSet = nfa ().allNextStates ().get (m_aNext.m_sEpsilonMovesString);
    return _elemOccurs (m_nStateName, aSet) >= 0;
  }

  private void _dumpAsciiMoveForCompositeState (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                                final int nByteNum,
                                                final boolean bElseNeeded)
  {
    final NfaBuildState aNfa = nfa ();
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    boolean bNextIntersects = _selfLoop ();

    for (final NfaState temp1 : aNfa.getAllStates ())
    {
      if (this == temp1 ||
          temp1.m_nStateName == -1 ||
          temp1.m_bDummy ||
          m_nStateName == temp1.m_nStateName ||
          temp1.m_aAsciiMoves[nByteNum] == 0L)
        continue;

      if (!bNextIntersects && _intersect (temp1.m_aNext.m_sEpsilonMovesString, m_aNext.m_sEpsilonMovesString))
      {
        bNextIntersects = true;
        break;
      }
    }

    // System.out.println(stateName + " \'s nextIntersects : " +
    // nextIntersects);
    String sPrefix = "";
    if (m_aAsciiMoves[nByteNum] != 0xffffffffffffffffL)
    {
      final int nOneBit = _isOnlyOneBitSet (m_aAsciiMoves[nByteNum]);

      if (nOneBit != -1)
        aCodeGenerator.genCodeLine ("                  " +
                                    (bElseNeeded ? "else " : "") +
                                    "if (curChar == " +
                                    (64 * nByteNum + nOneBit) +
                                    ")");
      else
        aCodeGenerator.genCodeLine ("                  " +
                                    (bElseNeeded ? "else " : "") +
                                    "if ((" +
                                    eOutputLanguage.getLongHex (m_aAsciiMoves[nByteNum]) +
                                    " & l) != " +
                                    eOutputLanguage.getLongPlain (0) +
                                    ")");
      sPrefix = "   ";
    }

    if (m_nKindToPrint != Integer.MAX_VALUE)
    {
      if (m_aAsciiMoves[nByteNum] != 0xffffffffffffffffL)
      {
        aCodeGenerator.genCodeLine ("                  {");
      }

      aCodeGenerator.genCodeLine (sPrefix + "                  if (kind > " + m_nKindToPrint + ")");
      aCodeGenerator.genCodeLine (sPrefix + "                     kind = " + m_nKindToPrint + ";");
    }

    if (m_aNext != null && m_aNext.m_nUsefulEpsilonMoves > 0)
    {
      final int [] aStateNames = aNfa.allNextStates ().get (m_aNext.m_sEpsilonMovesString);
      if (m_aNext.m_nUsefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];

        if (bNextIntersects)
          aCodeGenerator.genCodeLine (sPrefix + "                  { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine (sPrefix + "                  jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_aNext.m_nUsefulEpsilonMoves == 2 && bNextIntersects)
        {
          aCodeGenerator.genCodeLine (sPrefix +
                                      "                  { jjCheckNAddTwoStates(" +
                                      aStateNames[0] +
                                      ", " +
                                      aStateNames[1] +
                                      "); }");
        }
        else
        {
          final int [] aIndices = _getStateSetIndicesForUse (m_aNext.m_sEpsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode (sPrefix + "                  { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              aNfa.setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              aNfa.setJJCheckNAddStatesUnaryNeeded (true);
            }
            aCodeGenerator.genCodeLine ("); }");
          }
          else
            aCodeGenerator.genCodeLine (sPrefix +
                                        "                  { jjAddStates(" +
                                        aIndices[0] +
                                        ", " +
                                        aIndices[1] +
                                        "); }");
        }
    }

    if (m_aAsciiMoves[nByteNum] != 0xffffffffffffffffL && m_nKindToPrint != Integer.MAX_VALUE)
      aCodeGenerator.genCodeLine ("                  }");
  }

  private void _dumpAsciiMove (@NonNull final AbstractCodeGenerator aCodeGenerator,
                               final int nByteNum,
                               final boolean dumped[])
  {
    final NfaBuildState aNfa = nfa ();
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    boolean bNextIntersects = _selfLoop () && m_bIsComposite;
    boolean bOnlyState = true;

    for (final NfaState aCurState : aNfa.getAllStates ())
    {
      final NfaState aTemp1 = aCurState;

      if (this == aTemp1 ||
          aTemp1.m_nStateName == -1 ||
          aTemp1.m_bDummy ||
          m_nStateName == aTemp1.m_nStateName ||
          aTemp1.m_aAsciiMoves[nByteNum] == 0L)
        continue;

      if (bOnlyState && (m_aAsciiMoves[nByteNum] & aTemp1.m_aAsciiMoves[nByteNum]) != 0L)
        bOnlyState = false;

      if (!bNextIntersects && _intersect (aTemp1.m_aNext.m_sEpsilonMovesString, m_aNext.m_sEpsilonMovesString))
        bNextIntersects = true;

      if (!dumped[aTemp1.m_nStateName] &&
          !aTemp1.m_bIsComposite &&
          m_aAsciiMoves[nByteNum] == aTemp1.m_aAsciiMoves[nByteNum] &&
          m_nKindToPrint == aTemp1.m_nKindToPrint &&
          (m_aNext.m_sEpsilonMovesString == aTemp1.m_aNext.m_sEpsilonMovesString ||
           (m_aNext.m_sEpsilonMovesString != null &&
            aTemp1.m_aNext.m_sEpsilonMovesString != null &&
            m_aNext.m_sEpsilonMovesString.equals (aTemp1.m_aNext.m_sEpsilonMovesString))))
      {
        dumped[aTemp1.m_nStateName] = true;
        aCodeGenerator.genCodeLine ("               case " + aTemp1.m_nStateName + ":");
      }
    }

    // if (onlyState)
    // nextIntersects = false;

    final int nOneBit = _isOnlyOneBitSet (m_aAsciiMoves[nByteNum]);
    if (m_aAsciiMoves[nByteNum] != 0xffffffffffffffffL)
    {
      if ((m_aNext == null || m_aNext.m_nUsefulEpsilonMoves == 0) && m_nKindToPrint != Integer.MAX_VALUE)
      {
        String sKindCheck = "";

        if (!bOnlyState)
          sKindCheck = " && kind > " + m_nKindToPrint;

        if (nOneBit != -1)
          aCodeGenerator.genCodeLine ("                  if (curChar == " +
                                      (64 * nByteNum + nOneBit) +
                                      sKindCheck +
                                      ")");
        else
          aCodeGenerator.genCodeLine ("                  if ((" +
                                      eOutputLanguage.getLongHex (m_aAsciiMoves[nByteNum]) +
                                      " & l) != " +
                                      eOutputLanguage.getLongPlain (0) +
                                      sKindCheck +
                                      ")");

        aCodeGenerator.genCodeLine ("                     kind = " + m_nKindToPrint + ";");

        if (bOnlyState)
          aCodeGenerator.genCodeLine ("                  break;");
        else
          aCodeGenerator.genCodeLine ("                  break;");

        return;
      }
    }

    String sPrefix = "";
    if (m_nKindToPrint != Integer.MAX_VALUE)
    {

      if (nOneBit != -1)
      {
        aCodeGenerator.genCodeLine ("                  if (curChar != " + (64 * nByteNum + nOneBit) + ")");
        aCodeGenerator.genCodeLine ("                     break;");
      }
      else
        if (m_aAsciiMoves[nByteNum] != 0xffffffffffffffffL)
        {
          aCodeGenerator.genCodeLine ("                  if ((" +
                                      eOutputLanguage.getLongHex (m_aAsciiMoves[nByteNum]) +
                                      " & l) == " +
                                      eOutputLanguage.getLongPlain (0) +
                                      ")");
          aCodeGenerator.genCodeLine ("                     break;");
        }

      if (bOnlyState)
      {
        aCodeGenerator.genCodeLine ("                  kind = " + m_nKindToPrint + ";");
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (kind > " + m_nKindToPrint + ")");
        aCodeGenerator.genCodeLine ("                     kind = " + m_nKindToPrint + ";");
      }
    }
    else
    {
      if (nOneBit != -1)
      {
        aCodeGenerator.genCodeLine ("                  if (curChar == " + (64 * nByteNum + nOneBit) + ")");
        sPrefix = "   ";
      }
      else
        if (m_aAsciiMoves[nByteNum] != 0xffffffffffffffffL)
        {
          aCodeGenerator.genCodeLine ("                  if ((" +
                                      eOutputLanguage.getLongHex (m_aAsciiMoves[nByteNum]) +
                                      " & l) != " +
                                      eOutputLanguage.getLongPlain (0) +
                                      ")");
          sPrefix = "   ";
        }
    }

    if (m_aNext != null && m_aNext.m_nUsefulEpsilonMoves > 0)
    {
      final int [] aStateNames = aNfa.allNextStates ().get (m_aNext.m_sEpsilonMovesString);
      if (m_aNext.m_nUsefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];
        if (bNextIntersects)
          aCodeGenerator.genCodeLine (sPrefix + "                  { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine (sPrefix + "                  jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_aNext.m_nUsefulEpsilonMoves == 2 && bNextIntersects)
        {
          aCodeGenerator.genCodeLine (sPrefix +
                                      "                  { jjCheckNAddTwoStates(" +
                                      aStateNames[0] +
                                      ", " +
                                      aStateNames[1] +
                                      "); }");
        }
        else
        {
          final int [] aIndices = _getStateSetIndicesForUse (m_aNext.m_sEpsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode (sPrefix + "                  { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              aNfa.setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              aNfa.setJJCheckNAddStatesUnaryNeeded (true);
            }
            aCodeGenerator.genCodeLine ("); }");
          }
          else
            aCodeGenerator.genCodeLine (sPrefix +
                                        "                  { jjAddStates(" +
                                        aIndices[0] +
                                        ", " +
                                        aIndices[1] +
                                        "); }");
        }
    }

    if (bOnlyState)
      aCodeGenerator.genCodeLine ("                  break;");
    else
      aCodeGenerator.genCodeLine ("                  break;");
  }

  private static void _dumpAsciiMoves (@NonNull final AbstractCodeGenerator aCodeGenerator, final int nByteNum)
  {
    final NfaBuildState aNfa = nfa ();
    final boolean [] aDumped = new boolean [Math.max (aNfa.getGeneratedStates (), aNfa.getDummyStateIndex () + 1)];

    _dumpHeadForCase (aCodeGenerator, nByteNum);

    for (final String s : aNfa.compositeStateTable ().keySet ())
      _dumpCompositeStatesAsciiMoves (aCodeGenerator, s, nByteNum, aDumped);

    for (final NfaState aCurState : aNfa.getAllStates ())
    {
      final NfaState aTemp = aCurState;

      if (aDumped[aTemp.m_nStateName] ||
          aTemp.m_nLexState != AbstractLexGenJavaLike.lexer ().getLexStateIndex () ||
          !aTemp.hasTransitions () ||
          aTemp.m_bDummy ||
          aTemp.m_nStateName == -1)
        continue;

      String sToPrint = "";

      if (aTemp.m_aStateForCase != null)
      {
        if (aTemp.m_nInNextOf == 1)
          continue;

        if (aDumped[aTemp.m_aStateForCase.m_nStateName])
          continue;

        sToPrint = (aTemp.m_aStateForCase._printNoBreak (aCodeGenerator, nByteNum, aDumped));

        if (aTemp.m_aAsciiMoves[nByteNum] == 0L)
        {
          if (StringHelper.isEmpty (sToPrint))
            aCodeGenerator.genCodeLine ("                  break;");

          continue;
        }
      }

      if (aTemp.m_aAsciiMoves[nByteNum] == 0L)
        continue;

      if (StringHelper.isNotEmpty (sToPrint))
        aCodeGenerator.genCode (sToPrint);

      aDumped[aTemp.m_nStateName] = true;
      aCodeGenerator.genCodeLine ("               case " + aTemp.m_nStateName + ":");
      aTemp._dumpAsciiMove (aCodeGenerator, nByteNum, aDumped);
    }

    if (nByteNum != 0 && nByteNum != 1)
    {
      aCodeGenerator.genCodeLine ("               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;");
    }
    else
    {
      aCodeGenerator.genCodeLine ("               default : break;");
    }

    aCodeGenerator.genCodeLine ("            }");
    aCodeGenerator.genCodeLine ("         } while(i != startsAt);");
  }

  private static void _dumpCompositeStatesNonAsciiMoves (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                                         final String sKey,
                                                         @NonNull final boolean [] aDumped)
  {
    final NfaBuildState aNfa = nfa ();
    final int [] aNameSet = aNfa.allNextStates ().get (sKey);

    if (aNameSet.length == 1 || aDumped[_stateNameForComposite (sKey)])
      return;

    NfaState aToBePrinted = null;
    int nNeededStates = 0;
    NfaState aState;
    NfaState aStateForCase = null;
    String sToPrint = "";
    final boolean bStateBlock = (aNfa.stateBlockTable ().get (sKey) != null);

    for (final int nStateName : aNameSet)
    {
      aState = aNfa.getAllStates ().get (nStateName);

      if (aState.m_nNonAsciiMethod != -1)
      {
        if (nNeededStates++ == 1)
          break;
        aToBePrinted = aState;
      }
      else
        aDumped[aState.m_nStateName] = true;

      if (aState.m_aStateForCase != null)
      {
        if (aStateForCase != null)
          JavaCCErrors.internalError ();

        aStateForCase = aState.m_aStateForCase;
      }
    }

    if (aStateForCase != null)
      sToPrint = aStateForCase._printNoBreak (aCodeGenerator, -1, aDumped);

    if (nNeededStates == 0)
    {
      if (aStateForCase != null)
        if (StringHelper.isEmpty (sToPrint))
          aCodeGenerator.genCodeLine ("                  break;");

      return;
    }

    if (nNeededStates == 1)
    {
      if (StringHelper.isNotEmpty (sToPrint))
        aCodeGenerator.genCode (sToPrint);

      aCodeGenerator.genCodeLine ("               case " + _stateNameForComposite (sKey) + ":");

      if (!aDumped[aToBePrinted.m_nStateName] && !bStateBlock && aToBePrinted.m_nInNextOf > 1)
        aCodeGenerator.genCodeLine ("               case " + aToBePrinted.m_nStateName + ":");

      aDumped[aToBePrinted.m_nStateName] = true;
      aToBePrinted._dumpNonAsciiMove (aCodeGenerator, aDumped);
      return;
    }

    if (StringHelper.isNotEmpty (sToPrint))
      aCodeGenerator.genCode (sToPrint);

    final int nKeyState = _stateNameForComposite (sKey);
    aCodeGenerator.genCodeLine ("               case " + nKeyState + ":");
    if (nKeyState < aNfa.getGeneratedStates ())
      aDumped[nKeyState] = true;

    for (final int nStateName : aNameSet)
    {
      aState = aNfa.getAllStates ().get (nStateName);

      if (aState.m_nNonAsciiMethod != -1)
      {
        if (bStateBlock)
          aDumped[aState.m_nStateName] = true;
        aState._dumpNonAsciiMoveForCompositeState (aCodeGenerator);
      }
    }

    if (bStateBlock)
      aCodeGenerator.genCodeLine ("                  break;");
    else
      aCodeGenerator.genCodeLine ("                  break;");
  }

  private final void _dumpNonAsciiMoveForCompositeState (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    boolean bNextIntersects = _selfLoop ();
    for (final NfaState temp1 : aNfa.getAllStates ())
    {
      if (this == temp1 ||
          temp1.m_nStateName == -1 ||
          temp1.m_bDummy ||
          m_nStateName == temp1.m_nStateName ||
          (temp1.m_nNonAsciiMethod == -1))
        continue;

      if (!bNextIntersects && _intersect (temp1.m_aNext.m_sEpsilonMovesString, m_aNext.m_sEpsilonMovesString))
      {
        bNextIntersects = true;
        break;
      }
    }

    if (!Options.isJavaUnicodeEscape () && !aNfa.isUnicodeWarningGiven ())
    {
      if (m_aLoByteVec != null && m_aLoByteVec.size () > 1)
        aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                    m_aLoByteVec.get (1).intValue () +
                                    "[i2" +
                                    "] & l2) != 0L)");
    }
    else
    {
      aCodeGenerator.genCodeLine ("                  if (jjCanMove_" + m_nNonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
    }

    if (m_nKindToPrint != Integer.MAX_VALUE)
    {
      aCodeGenerator.genCodeLine ("                  {");
      aCodeGenerator.genCodeLine ("                     if (kind > " + m_nKindToPrint + ")");
      aCodeGenerator.genCodeLine ("                        kind = " + m_nKindToPrint + ";");
    }

    if (m_aNext != null && m_aNext.m_nUsefulEpsilonMoves > 0)
    {
      final int [] aStateNames = aNfa.allNextStates ().get (m_aNext.m_sEpsilonMovesString);
      if (m_aNext.m_nUsefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];
        if (bNextIntersects)
          aCodeGenerator.genCodeLine ("                     { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine ("                     jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_aNext.m_nUsefulEpsilonMoves == 2 && bNextIntersects)
        {
          aCodeGenerator.genCodeLine ("                     { jjCheckNAddTwoStates(" +
                                      aStateNames[0] +
                                      ", " +
                                      aStateNames[1] +
                                      "); }");
        }
        else
        {
          final int [] aIndices = _getStateSetIndicesForUse (m_aNext.m_sEpsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode ("                     { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              aNfa.setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              aNfa.setJJCheckNAddStatesUnaryNeeded (true);
            }
            aCodeGenerator.genCodeLine ("); }");
          }
          else
            aCodeGenerator.genCodeLine ("                     { jjAddStates(" +
                                        aIndices[0] +
                                        ", " +
                                        aIndices[1] +
                                        "); }");
        }
    }

    if (m_nKindToPrint != Integer.MAX_VALUE)
      aCodeGenerator.genCodeLine ("                  }");
  }

  private final void _dumpNonAsciiMove (@NonNull final AbstractCodeGenerator aCodeGenerator, final boolean dumped[])
  {
    final NfaBuildState aNfa = nfa ();
    boolean bNextIntersects = _selfLoop () && m_bIsComposite;

    for (final NfaState aCurState : aNfa.getAllStates ())
    {
      final NfaState aTemp1 = aCurState;

      if (this == aTemp1 ||
          aTemp1.m_nStateName == -1 ||
          aTemp1.m_bDummy ||
          m_nStateName == aTemp1.m_nStateName ||
          (aTemp1.m_nNonAsciiMethod == -1))
        continue;

      if (!bNextIntersects && _intersect (aTemp1.m_aNext.m_sEpsilonMovesString, m_aNext.m_sEpsilonMovesString))
        bNextIntersects = true;

      if (!dumped[aTemp1.m_nStateName] &&
          !aTemp1.m_bIsComposite &&
          m_nNonAsciiMethod == aTemp1.m_nNonAsciiMethod &&
          m_nKindToPrint == aTemp1.m_nKindToPrint &&
          (m_aNext.m_sEpsilonMovesString == aTemp1.m_aNext.m_sEpsilonMovesString ||
           (m_aNext.m_sEpsilonMovesString != null &&
            aTemp1.m_aNext.m_sEpsilonMovesString != null &&
            m_aNext.m_sEpsilonMovesString.equals (aTemp1.m_aNext.m_sEpsilonMovesString))))
      {
        dumped[aTemp1.m_nStateName] = true;
        aCodeGenerator.genCodeLine ("               case " + aTemp1.m_nStateName + ":");
      }
    }

    if (m_aNext == null || m_aNext.m_nUsefulEpsilonMoves <= 0)
    {
      final String sKindCheck = " && kind > " + m_nKindToPrint;

      if (!Options.isJavaUnicodeEscape () && !aNfa.isUnicodeWarningGiven ())
      {
        if (m_aLoByteVec != null && m_aLoByteVec.size () > 1)
          aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                      m_aLoByteVec.get (1).intValue () +
                                      "[i2" +
                                      "] & l2) != 0L" +
                                      sKindCheck +
                                      ")");
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (jjCanMove_" +
                                    m_nNonAsciiMethod +
                                    "(hiByte, i1, i2, l1, l2)" +
                                    sKindCheck +
                                    ")");
      }
      aCodeGenerator.genCodeLine ("                     kind = " + m_nKindToPrint + ";");
      aCodeGenerator.genCodeLine ("                  break;");
      return;
    }

    String sPrefix = "   ";
    if (m_nKindToPrint != Integer.MAX_VALUE)
    {
      if (!Options.isJavaUnicodeEscape () && !aNfa.isUnicodeWarningGiven ())
      {
        if (m_aLoByteVec != null && m_aLoByteVec.size () > 1)
        {
          aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                      m_aLoByteVec.get (1).intValue () +
                                      "[i2" +
                                      "] & l2) == 0L)");
          aCodeGenerator.genCodeLine ("                     break;");
        }
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (!jjCanMove_" +
                                    m_nNonAsciiMethod +
                                    "(hiByte, i1, i2, l1, l2))");
        aCodeGenerator.genCodeLine ("                     break;");
      }

      aCodeGenerator.genCodeLine ("                  if (kind > " + m_nKindToPrint + ")");
      aCodeGenerator.genCodeLine ("                     kind = " + m_nKindToPrint + ";");
      sPrefix = "";
    }
    else
      if (!Options.isJavaUnicodeEscape () && !aNfa.isUnicodeWarningGiven ())
      {
        if (m_aLoByteVec != null && m_aLoByteVec.size () > 1)
          aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                      m_aLoByteVec.get (1).intValue () +
                                      "[i2" +
                                      "] & l2) != 0L)");
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (jjCanMove_" +
                                    m_nNonAsciiMethod +
                                    "(hiByte, i1, i2, l1, l2))");
      }

    if (m_aNext != null && m_aNext.m_nUsefulEpsilonMoves > 0)
    {
      final int [] aStateNames = aNfa.allNextStates ().get (m_aNext.m_sEpsilonMovesString);
      if (m_aNext.m_nUsefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];
        if (bNextIntersects)
          aCodeGenerator.genCodeLine (sPrefix + "                  { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine (sPrefix + "                  jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_aNext.m_nUsefulEpsilonMoves == 2 && bNextIntersects)
        {
          aCodeGenerator.genCodeLine (sPrefix +
                                      "                  { jjCheckNAddTwoStates(" +
                                      aStateNames[0] +
                                      ", " +
                                      aStateNames[1] +
                                      "); }");
        }
        else
        {
          final int [] aIndices = _getStateSetIndicesForUse (m_aNext.m_sEpsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode (sPrefix + "                  { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              aNfa.setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              aNfa.setJJCheckNAddStatesUnaryNeeded (true);
            }
            aCodeGenerator.genCodeLine ("); }");
          }
          else
            aCodeGenerator.genCodeLine (sPrefix +
                                        "                  { jjAddStates(" +
                                        aIndices[0] +
                                        ", " +
                                        aIndices[1] +
                                        "); }");
        }
    }

    aCodeGenerator.genCodeLine ("                  break;");
  }

  /**
   * Write the character tests of every state of the current lexical state that matches above ASCII.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpCharAndRangeMoves (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    final boolean [] aDumped = new boolean [Math.max (aNfa.getGeneratedStates (), aNfa.getDummyStateIndex () + 1)];

    _dumpHeadForCase (aCodeGenerator, -1);

    for (final String s : aNfa.compositeStateTable ().keySet ())
      _dumpCompositeStatesNonAsciiMoves (aCodeGenerator, s, aDumped);

    for (final NfaState temp : aNfa.getAllStates ())
    {
      if (temp.m_nStateName == -1 ||
          aDumped[temp.m_nStateName] ||
          temp.m_nLexState != AbstractLexGenJavaLike.lexer ().getLexStateIndex () ||
          !temp.hasTransitions () ||
          temp.m_bDummy)
        continue;

      String sToPrint = "";

      if (temp.m_aStateForCase != null)
      {
        if (temp.m_nInNextOf == 1)
          continue;

        if (aDumped[temp.m_aStateForCase.m_nStateName])
          continue;

        sToPrint = temp.m_aStateForCase._printNoBreak (aCodeGenerator, -1, aDumped);

        if (temp.m_nNonAsciiMethod == -1)
        {
          if (StringHelper.isEmpty (sToPrint))
            aCodeGenerator.genCodeLine ("                  break;");

          continue;
        }
      }

      if (temp.m_nNonAsciiMethod == -1)
        continue;

      if (StringHelper.isNotEmpty (sToPrint))
        aCodeGenerator.genCode (sToPrint);

      aDumped[temp.m_nStateName] = true;
      // System.out.println("case : " + temp.stateName);
      aCodeGenerator.genCodeLine ("               case " + temp.m_nStateName + ":");
      temp._dumpNonAsciiMove (aCodeGenerator, aDumped);
    }

    if (Options.isJavaUnicodeEscape () || aNfa.isUnicodeWarningGiven ())
    {
      aCodeGenerator.genCodeLine ("               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;");
    }
    else
    {
      aCodeGenerator.genCodeLine ("               default : break;");
    }
    aCodeGenerator.genCodeLine ("            }");
    aCodeGenerator.genCodeLine ("         } while(i != startsAt);");
  }

  /**
   * Write the jjCanMove_ methods, one per state whose non ASCII character test was too large to
   * inline.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpNonAsciiMoveMethods (final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    if (!Options.isJavaUnicodeEscape () && !aNfa.isUnicodeWarningGiven ())
      return;

    if (aNfa.nonAsciiTableForMethod ().isEmpty ())
      return;

    for (final NfaState tmp : aNfa.nonAsciiTableForMethod ())
    {
      tmp._dumpNonAsciiMoveMethod (aCodeGenerator);
    }
  }

  private void _dumpNonAsciiMoveMethod (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    aCodeGenerator.generateMethodDefHeader (eOutputLanguage.getMethodModifiers ("private static final") +
                                            eOutputLanguage.getTypeBoolean (),
                                            AbstractLexGenJavaLike.lexer ().getTokenMgrClassName (),
                                            "jjCanMove_" +
                                                                                                     m_nNonAsciiMethod +
                                                                                                     "(int hiByte, int i1, int i2, " +
                                                                                                     eOutputLanguage.getTypeLong () +
                                                                                                     " l1, " +
                                                                                                     eOutputLanguage.getTypeLong () +
                                                                                                     " l2)");
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   switch(hiByte)");
    aCodeGenerator.genCodeLine ("   {");

    if (m_aLoByteVec != null && !m_aLoByteVec.isEmpty ())
    {
      for (int j = 0; j < m_aLoByteVec.size (); j += 2)
      {
        aCodeGenerator.genCodeLine ("      case " + m_aLoByteVec.get (j).intValue () + ":");
        if (!allBitsSet (aNfa.getAllBitVectors ().get (m_aLoByteVec.get (j + 1).intValue ())))
        {
          aCodeGenerator.genCodeLine ("         return ((jjbitVec" +
                                      m_aLoByteVec.get (j + 1).intValue () +
                                      "[i2" +
                                      "] & l2) != 0L);");
        }
        else
          aCodeGenerator.genCodeLine ("            return true;");
      }
    }

    aCodeGenerator.genCodeLine ("      default :");

    if (m_aNonAsciiMoveIndices != null)
    {
      int j = m_aNonAsciiMoveIndices.length;
      if (j > 0)
        do
        {
          if (!allBitsSet (aNfa.getAllBitVectors ().get (m_aNonAsciiMoveIndices[j - 2])))
            aCodeGenerator.genCodeLine ("         if ((jjbitVec" + m_aNonAsciiMoveIndices[j - 2] + "[i1] & l1) != 0L)");
          if (!allBitsSet (aNfa.getAllBitVectors ().get (m_aNonAsciiMoveIndices[j - 1])))
          {
            aCodeGenerator.genCodeLine ("            if ((jjbitVec" +
                                        m_aNonAsciiMoveIndices[j - 1] +
                                        "[i2] & l2) == 0L)");
            aCodeGenerator.genCodeLine ("               return false;");
            aCodeGenerator.genCodeLine ("            else");
          }
          aCodeGenerator.genCodeLine ("            return true;");
        } while ((j -= 2) > 0);
    }

    aCodeGenerator.genCodeLine ("         return false;");
    aCodeGenerator.genCodeLine ("   }");
    aCodeGenerator.genCodeLine ("}");
  }

  private static void _reArrange ()
  {
    final NfaBuildState aNfa = nfa ();
    final List <NfaState> v = aNfa.getAllStates ();
    aNfa.setAllStates (new ArrayList <> (Collections.nCopies (aNfa.getGeneratedStates (), null)));

    if (aNfa.getAllStates ().size () != aNfa.getGeneratedStates ())
      JavaCCErrors.internalError ();

    for (int j = 0; j < v.size (); j++)
    {
      final NfaState aState = v.get (j);
      if (aState.m_nStateName != -1 && !aState.m_bDummy)
        aNfa.getAllStates ().set (aState.m_nStateName, aState);
    }
  }

  // private static boolean boilerPlateDumped = false;
  /**
   * Write the Java helper methods every generated token manager shares - jjCheckNAdd and friends.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void printBoilerPlateJava (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    aCodeGenerator.genCodeLine ("private void jjCheckNAdd(int state)");
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   if (jjrounds[state] != jjround)");
    aCodeGenerator.genCodeLine ("   {");
    aCodeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = state;");
    aCodeGenerator.genCodeLine ("      jjrounds[state] = jjround;");
    aCodeGenerator.genCodeLine ("   }");
    aCodeGenerator.genCodeLine ("}");

    aCodeGenerator.genCodeLine ("private void jjAddStates(int start, int end)");
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   do {");
    aCodeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = jjnextStates[start];");
    aCodeGenerator.genCodeLine ("   } while (start++ != end);");
    aCodeGenerator.genCodeLine ("}");

    aCodeGenerator.genCodeLine ("private void jjCheckNAddTwoStates(int state1, int state2)");
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   jjCheckNAdd(state1);");
    aCodeGenerator.genCodeLine ("   jjCheckNAdd(state2);");
    aCodeGenerator.genCodeLine ("}");
    aCodeGenerator.genCodeNewLine ();

    if (aNfa.isJJCheckNAddStatesDualNeeded ())
    {
      aCodeGenerator.genCodeLine ("private void jjCheckNAddStates(int start, int end)");
      aCodeGenerator.genCodeLine ("{");
      aCodeGenerator.genCodeLine ("   do {");
      aCodeGenerator.genCodeLine ("      jjCheckNAdd(jjnextStates[start]);");
      aCodeGenerator.genCodeLine ("   } while (start++ != end);");
      aCodeGenerator.genCodeLine ("}");
      aCodeGenerator.genCodeNewLine ();
    }

    if (aNfa.isJJCheckNAddStatesUnaryNeeded ())
    {
      aCodeGenerator.genCodeLine ("private void jjCheckNAddStates(int start)");
      aCodeGenerator.genCodeLine ("{");
      aCodeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start]);");
      aCodeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start + 1]);");
      aCodeGenerator.genCodeLine ("}");
      aCodeGenerator.genCodeNewLine ();
    }
  }

  // private static boolean boilerPlateDumped = false;
  /**
   * Write the C++ helper methods every generated token manager shares - jjCheckNAdd and friends.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void printBoilerPlateCpp (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    aCodeGenerator.switchToIncludeFile ();
    aCodeGenerator.genCodeLine ("#define jjCheckNAdd(state)\\");
    aCodeGenerator.genCodeLine ("{\\");
    aCodeGenerator.genCodeLine ("   if (jjrounds[state] != jjround)\\");
    aCodeGenerator.genCodeLine ("   {\\");
    aCodeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = state;\\");
    aCodeGenerator.genCodeLine ("      jjrounds[state] = jjround;\\");
    aCodeGenerator.genCodeLine ("   }\\");
    aCodeGenerator.genCodeLine ("}");

    aCodeGenerator.genCodeLine ("#define jjAddStates(start, end)\\");
    aCodeGenerator.genCodeLine ("{\\");
    aCodeGenerator.genCodeLine ("   for (int x = start; x <= end; x++) {\\");
    aCodeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = jjnextStates[x];\\");
    aCodeGenerator.genCodeLine ("   } /*while (start++ != end);*/\\");
    aCodeGenerator.genCodeLine ("}");

    aCodeGenerator.genCodeLine ("#define jjCheckNAddTwoStates(state1, state2)\\");
    aCodeGenerator.genCodeLine ("{\\");
    aCodeGenerator.genCodeLine ("   jjCheckNAdd(state1);\\");
    aCodeGenerator.genCodeLine ("   jjCheckNAdd(state2);\\");
    aCodeGenerator.genCodeLine ("}");
    aCodeGenerator.genCodeNewLine ();

    if (aNfa.isJJCheckNAddStatesDualNeeded ())
    {
      aCodeGenerator.genCodeLine ("#define jjCheckNAddStates(start, end)\\");
      aCodeGenerator.genCodeLine ("{\\");
      aCodeGenerator.genCodeLine ("   for (int x = start; x <= end; x++) {\\");
      aCodeGenerator.genCodeLine ("      jjCheckNAdd(jjnextStates[x]);\\");
      aCodeGenerator.genCodeLine ("   } /*while (start++ != end);*/\\");
      aCodeGenerator.genCodeLine ("}");
      aCodeGenerator.genCodeNewLine ();
    }

    if (aNfa.isJJCheckNAddStatesUnaryNeeded ())
    {
      aCodeGenerator.genCodeLine ("#define jjCheckNAddStates(start)\\");
      aCodeGenerator.genCodeLine ("{\\");
      aCodeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start]);\\");
      aCodeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start + 1]);\\");
      aCodeGenerator.genCodeLine ("}");
      aCodeGenerator.genCodeNewLine ();
    }
    aCodeGenerator.switchToMainFile ();
  }

  @SuppressWarnings ("unused")
  private static void _findStatesWithNoBreak ()
  {
    final NfaBuildState aNfa = nfa ();
    final Map <String, String> aPrinted = new HashMap <> ();
    final boolean [] aPut = new boolean [aNfa.getGeneratedStates ()];
    int nCnt = 0;
    int nFoundAt = 0;

    Outer: for (final NfaState tmpState : aNfa.getAllStates ())
    {
      NfaState aStateForCase = null;
      if (tmpState.m_nStateName == -1 ||
          tmpState.m_bDummy ||
          !tmpState._isUsefulState () ||
          tmpState.m_aNext == null ||
          tmpState.m_aNext.m_nUsefulEpsilonMoves < 1)
        continue;

      final String s = tmpState.m_aNext.m_sEpsilonMovesString;

      if (aNfa.compositeStateTable ().get (s) != null || aPrinted.get (s) != null)
        continue;

      aPrinted.put (s, s);
      final int [] aNexts = aNfa.allNextStates ().get (s);

      if (aNexts.length == 1)
        continue;

      int nState = nCnt;
      // System.out.println("State " + tmpState.stateName + " : " + s);
      for (int i = 0; i < aNexts.length; i++)
      {
        nState = aNexts[i];
        if (nState == -1)
          continue;

        final NfaState aState = aNfa.getAllStates ().get (nState);

        if (!aState.m_bIsComposite && aState.m_nInNextOf == 1)
        {
          if (aPut[nState])
            JavaCCErrors.internalError ();

          nFoundAt = i;
          nCnt++;
          aStateForCase = aState;
          aPut[nState] = true;

          // System.out.print(state + " : " + tmp.inNextOf + ", ");
          break;
        }
      }
      // System.out.println("");

      if (aStateForCase == null)
        continue;

      for (int i = 0; i < aNexts.length; i++)
      {
        nState = aNexts[i];
        if (nState == -1)
          continue;

        final NfaState aState = aNfa.getAllStates ().get (nState);

        if (!aPut[nState] && aState.m_nInNextOf > 1 && !aState.m_bIsComposite && aState.m_aStateForCase == null)
        {
          nCnt++;
          aNexts[i] = -1;
          aPut[nState] = true;

          final int nToSwap = aNexts[0];
          aNexts[0] = aNexts[nFoundAt];
          aNexts[nFoundAt] = nToSwap;

          aState.m_aStateForCase = aStateForCase;
          aStateForCase.m_aStateForCase = aState;
          aNfa.stateSetsToFix ().put (s, aNexts);

          // System.out.println("For : " + s + "; " + stateForCase.stateName +
          // " and " + tmp.stateName);

          continue Outer;
        }
      }

      for (final int aNext : aNexts)
      {
        nState = aNext;
        if (nState == -1)
          continue;

        final NfaState aState = aNfa.getAllStates ().get (nState);
        if (aState.m_nInNextOf <= 1)
          aPut[nState] = false;
      }
    }
  }

  /**
   * Write the jjMoveNfa method of the current lexical state, the loop that runs the automaton over
   * the input.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpMoveNfa (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    // if (!boilerPlateDumped)
    // PrintBoilerPlate(codeGenerator);

    // boilerPlateDumped = true;
    int [] aKindsForStates = null;
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();

    if (aNfa.getKinds () == null)
    {
      aNfa.setKinds (new int [AbstractLexGenJavaLike.lexer ().getMaxLexStates ()] []);
      aNfa.setStatesForState (new int [AbstractLexGenJavaLike.lexer ().getMaxLexStates ()] [] []);
    }

    _reArrange ();

    for (final NfaState aCurState : aNfa.getAllStates ())
    {
      final NfaState aTemp = aCurState;

      if (aTemp.m_nLexState != AbstractLexGenJavaLike.lexer ().getLexStateIndex () ||
          !aTemp.hasTransitions () ||
          aTemp.m_bDummy ||
          aTemp.m_nStateName == -1)
        continue;

      if (aKindsForStates == null)
      {
        aKindsForStates = new int [aNfa.getGeneratedStates ()];
        aNfa.getStatesForState ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()] = new int [Math.max (aNfa
                                                                                                                .getGeneratedStates (),
                                                                                                            aNfa.getDummyStateIndex () +
                                                                                                                                        1)] [];
      }

      aKindsForStates[aTemp.m_nStateName] = aTemp.m_nLookingFor;
      aNfa.getStatesForState ()[AbstractLexGenJavaLike.lexer ()
                                                      .getLexStateIndex ()][aTemp.m_nStateName] = aTemp.m_aCompositeStates;

      aTemp._generateNonAsciiMoves (aCodeGenerator);
    }

    for (final Map.Entry <String, Integer> aEntry : aNfa.stateNameForComposite ().entrySet ())
    {
      final String s = aEntry.getKey ();
      final int nState = aEntry.getValue ().intValue ();

      if (nState >= aNfa.getGeneratedStates ())
        aNfa.getStatesForState ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()][nState] = aNfa.allNextStates ()
                                                                                                     .get (s);
    }

    if (!aNfa.stateSetsToFix ().isEmpty ())
      _fixStateSets ();

    aNfa.getKinds ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()] = aKindsForStates;

    aCodeGenerator.generateMethodDefHeader (eOutputLanguage.getMethodModifiers ("private") + "int",
                                            AbstractLexGenJavaLike.lexer ().getTokenMgrClassName (),
                                            "jjMoveNfa" +
                                                                                                     AbstractLexGenJavaLike.lexer ()
                                                                                                                           .getLexStateSuffix () +
                                                                                                     "(int startState, int curPos)");
    aCodeGenerator.genCodeLine ("{");
    if (aNfa.getGeneratedStates () == 0)
    {
      aCodeGenerator.genCodeLine ("   return curPos;");
      aCodeGenerator.genCodeLine ("}");
      return;
    }

    if (AbstractLexGenJavaLike.lexer ().getMixed ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()])
    {
      aCodeGenerator.genCodeLine ("   int strKind = jjmatchedKind;");
      aCodeGenerator.genCodeLine ("   int strPos = jjmatchedPos;");
      aCodeGenerator.genCodeLine ("   int seenUpto;");
      switch (eOutputLanguage)
      {
        case JAVA ->
        {
          aCodeGenerator.genCodeLine ("   input_stream.backup(seenUpto = curPos + 1);");
          aCodeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
          // TODO do not throw error
          aCodeGenerator.genCodeLine ("   catch(final java.io.IOException e) { throw new Error(\"Internal Error\"); }");
        }
        case CPP ->
        {
          aCodeGenerator.genCodeLine ("   input_stream->backup(seenUpto = curPos + 1);");
          aCodeGenerator.genCodeLine ("   assert(!input_stream->endOfInput());");
          aCodeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
        }
        default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      aCodeGenerator.genCodeLine ("   curPos = 0;");
    }

    aCodeGenerator.genCodeLine ("   int startsAt = 0;");
    aCodeGenerator.genCodeLine ("   jjnewStateCnt = " + aNfa.getGeneratedStates () + ";");
    aCodeGenerator.genCodeLine ("   int i = 1;");
    aCodeGenerator.genCodeLine ("   jjstateSet[0] = startState;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA -> aCodeGenerator.genCodeLine ("      debugStream.println(\"   Starting NFA to match one of : \" + " +
                                                 "jjKindsForStateVector(curLexState, jjstateSet, 0, 1));");
        case CPP -> aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   Starting NFA to match one of : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, 0, 1).c_str());");
        default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA -> aCodeGenerator.genCodeLine ("      debugStream.println(" +
                                                 (AbstractLexGenJavaLike.lexer ().getMaxLexStates () > 1
                                                                                                         ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                                         : "") +
                                                 "\"Current character : \" + " +
                                                 Options.getTokenMgrErrorClass () +
                                                 ".addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                                 "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
        case CPP -> aCodeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                                "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                                "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                                                "input_stream->getEndLine(), input_stream->getEndColumn());");
        default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    aCodeGenerator.genCodeLine ("   int kind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");
    aCodeGenerator.genCodeLine ("   for (;;)");
    aCodeGenerator.genCodeLine ("   {");
    aCodeGenerator.genCodeLine ("      if (++jjround == 0x" + Integer.toHexString (Integer.MAX_VALUE) + ")");
    aCodeGenerator.genCodeLine ("         ReInitRounds();");
    aCodeGenerator.genCodeLine ("      if (curChar < 64)");
    aCodeGenerator.genCodeLine ("      {");

    _dumpAsciiMoves (aCodeGenerator, 0);

    aCodeGenerator.genCodeLine ("      }");

    aCodeGenerator.genCodeLine ("      else if (curChar < 128)");

    aCodeGenerator.genCodeLine ("      {");

    _dumpAsciiMoves (aCodeGenerator, 1);

    aCodeGenerator.genCodeLine ("      }");

    aCodeGenerator.genCodeLine ("      else");
    aCodeGenerator.genCodeLine ("      {");

    dumpCharAndRangeMoves (aCodeGenerator);

    aCodeGenerator.genCodeLine ("      }");

    aCodeGenerator.genCodeLine ("      if (kind != 0x" + Integer.toHexString (Integer.MAX_VALUE) + ")");
    aCodeGenerator.genCodeLine ("      {");
    aCodeGenerator.genCodeLine ("         jjmatchedKind = kind;");
    aCodeGenerator.genCodeLine ("         jjmatchedPos = curPos;");
    aCodeGenerator.genCodeLine ("         kind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");
    aCodeGenerator.genCodeLine ("      }");
    aCodeGenerator.genCodeLine ("      ++curPos;");

    if (Options.isDebugTokenManager ())
    {
      TokenManagerDebug.genCurrentlyMatchedIfAny (aCodeGenerator, "      ");
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        if (false)
        {
          // Old
          aCodeGenerator.genCodeLine ("      if ((i = jjnewStateCnt) == (startsAt = " +
                                      aNfa.getGeneratedStates () +
                                      " - (jjnewStateCnt = startsAt)))");
        }
        else
        {
          // New
          aCodeGenerator.genCodeLine ("      i = jjnewStateCnt;");
          aCodeGenerator.genCodeLine ("      jjnewStateCnt = startsAt;");
          aCodeGenerator.genCodeLine ("      startsAt = " + aNfa.getGeneratedStates () + " - jjnewStateCnt;");
          aCodeGenerator.genCodeLine ("      if (i == startsAt)");
        }
        break;
      case CPP:
        aCodeGenerator.genCodeLine ("      if ((i = jjnewStateCnt), (jjnewStateCnt = startsAt), (i == (startsAt = " +
                                    aNfa.getGeneratedStates () +
                                    " - startsAt)))");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    if (AbstractLexGenJavaLike.lexer ().getMixed ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()])
      aCodeGenerator.genCodeLine ("         break;");
    else
      aCodeGenerator.genCodeLine ("         return curPos;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA -> aCodeGenerator.genCodeLine ("      debugStream.println(\"   Possible kinds of longer matches : \" + " +
                                                 "jjKindsForStateVector(curLexState, jjstateSet, startsAt, i));");
        case CPP -> aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   Possible kinds of longer matches : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, startsAt, i).c_str());");
        default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("      try { curChar = input_stream.readChar(); }");
        if (AbstractLexGenJavaLike.lexer ().getMixed ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()])
          aCodeGenerator.genCodeLine ("      catch(final java.io.IOException e) { break; }");
        else
          aCodeGenerator.genCodeLine ("      catch(final java.io.IOException e) { return curPos; }");
        break;
      case CPP:
        if (AbstractLexGenJavaLike.lexer ().getMixed ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()])
          aCodeGenerator.genCodeLine ("      if (input_stream->endOfInput()) { break; }");
        else
          aCodeGenerator.genCodeLine ("      if (input_stream->endOfInput()) { return curPos; }");
        aCodeGenerator.genCodeLine ("      curChar = input_stream->readChar();");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA -> aCodeGenerator.genCodeLine ("      debugStream.println(" +
                                                 (AbstractLexGenJavaLike.lexer ().getMaxLexStates () > 1
                                                                                                         ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                                         : "") +
                                                 "\"Current character : \" + " +
                                                 Options.getTokenMgrErrorClass () +
                                                 ".addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                                 "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
        case CPP -> aCodeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                                "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                                "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                                                "input_stream->getEndLine(), input_stream->getEndColumn());");
        default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    aCodeGenerator.genCodeLine ("   }");

    if (AbstractLexGenJavaLike.lexer ().getMixed ()[AbstractLexGenJavaLike.lexer ().getLexStateIndex ()])
    {
      aCodeGenerator.genCodeLine ("   if (jjmatchedPos > strPos)");
      aCodeGenerator.genCodeLine ("      return curPos;");
      aCodeGenerator.genCodeNewLine ();
      aCodeGenerator.genCodeLine ("   int toRet = " + eOutputLanguage.getMax ("curPos", "seenUpto") + ";");
      aCodeGenerator.genCodeNewLine ();
      aCodeGenerator.genCodeLine ("   if (curPos < toRet)");
      aCodeGenerator.genCodeLine ("      for (i = toRet - " +
                                  eOutputLanguage.getMin ("curPos", "seenUpto") +
                                  "; i-- > 0; )");
      // Reading the next char: Java catches the end of input, C++ asserts against it
      switch (eOutputLanguage)
      {
        case JAVA ->
        {
          aCodeGenerator.genCodeLine ("         try { curChar = input_stream.readChar(); }");
          // TODO do not throw error
          aCodeGenerator.genCodeLine ("         catch(final java.io.IOException e) { throw new Error(\"Internal Error : Please send a bug report.\"); }");
        }
        case CPP ->
        {
          aCodeGenerator.genCodeLine ("        {  assert(!input_stream->endOfInput());");
          aCodeGenerator.genCodeLine ("           curChar = input_stream->readChar(); }");
        }
        default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      aCodeGenerator.genCodeNewLine ();
      aCodeGenerator.genCodeLine ("   if (jjmatchedPos < strPos)");
      aCodeGenerator.genCodeLine ("   {");
      aCodeGenerator.genCodeLine ("      jjmatchedKind = strKind;");
      aCodeGenerator.genCodeLine ("      jjmatchedPos = strPos;");
      aCodeGenerator.genCodeLine ("   }");
      aCodeGenerator.genCodeLine ("   else");
      aCodeGenerator.genCodeLine ("   if (jjmatchedPos == strPos && jjmatchedKind > strKind)");
      aCodeGenerator.genCodeLine ("      jjmatchedKind = strKind;");
      aCodeGenerator.genCodeNewLine ();
      aCodeGenerator.genCodeLine ("   return toRet;");
    }

    aCodeGenerator.genCodeLine ("}");
    aNfa.getAllStates ().clear ();
  }

  /**
   * Write the C++ form of the table that maps a state number to the composite state set it stands
   * for.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpStatesForStateCpp (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    if (aNfa.getStatesForState () == null)
    {
      assert (false) : "This should never be null.";
      aCodeGenerator.genCodeLine ("null;");
      return;
    }

    aCodeGenerator.switchToStaticsFile ();
    for (int i = 0; i < AbstractLexGenJavaLike.lexer ().getMaxLexStates (); i++)
    {
      if (aNfa.getStatesForState ()[i] == null)
      {
        continue;
      }

      for (int j = 0; j < aNfa.getStatesForState ()[i].length; j++)
      {
        final int [] aStateSet = aNfa.getStatesForState ()[i][j];

        aCodeGenerator.genCode ("const int stateSet_" +
                                i +
                                "_" +
                                j +
                                "[" +
                                AbstractLexGenJavaLike.lexer ().getStateSetSize () +
                                "] = ");
        if (aStateSet == null)
        {
          aCodeGenerator.genCodeLine ("   { " + j + " };");
          continue;
        }

        aCodeGenerator.genCode ("   { ");

        for (final int nStateName : aStateSet)
          aCodeGenerator.genCode (nStateName + ", ");

        aCodeGenerator.genCodeLine ("};");
      }

    }

    for (int i = 0; i < AbstractLexGenJavaLike.lexer ().getMaxLexStates (); i++)
    {
      aCodeGenerator.genCodeLine ("const int *stateSet_" + i + "[] = {");
      if (aNfa.getStatesForState ()[i] == null)
      {
        aCodeGenerator.genCodeLine (" NULL, ");
        aCodeGenerator.genCodeLine ("};");
        continue;
      }

      for (int j = 0; j < aNfa.getStatesForState ()[i].length; j++)
      {
        aCodeGenerator.genCode ("stateSet_" + i + "_" + j + ",");
      }
      aCodeGenerator.genCodeLine ("};");
    }

    aCodeGenerator.genCode ("const int** statesForState[] = { ");
    for (int i = 0; i < AbstractLexGenJavaLike.lexer ().getMaxLexStates (); i++)
    {
      aCodeGenerator.genCodeLine ("stateSet_" + i + ", ");
    }

    aCodeGenerator.genCodeLine ("\n};");
    aCodeGenerator.switchToMainFile ();
  }

  /**
   * Write the Java form of the table that maps a state number to the composite state set it stands
   * for.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpStatesForStateJava (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    aCodeGenerator.genCodeLine ("protected static final class States {");
    aCodeGenerator.genCode ("  protected static final int[][][] statesForState = ");

    if (aNfa.getStatesForState () == null)
    {
      aCodeGenerator.genCodeLine ("null;");
    }
    else
    {
      aCodeGenerator.genCodeLine ("{");
      for (int i = 0; i < AbstractLexGenJavaLike.lexer ().getMaxLexStates (); i++)
      {
        if (aNfa.getStatesForState ()[i] == null)
        {
          aCodeGenerator.genCodeLine (" {},");
          continue;
        }

        aCodeGenerator.genCodeLine (" {");
        for (int j = 0; j < aNfa.getStatesForState ()[i].length; j++)
        {
          final int [] aStateSet = aNfa.getStatesForState ()[i][j];

          if (aStateSet == null)
          {
            aCodeGenerator.genCodeLine ("   { " + j + " },");
          }
          else
          {
            aCodeGenerator.genCode ("   { ");
            for (final int nStateName : aStateSet)
              aCodeGenerator.genCode (nStateName + ", ");
            aCodeGenerator.genCodeLine ("},");
          }
        }
        aCodeGenerator.genCodeLine ("},");
      }

      aCodeGenerator.genCodeLine ("\n};");
    }

    // Close class
    aCodeGenerator.genCodeLine ("}");
  }

  /**
   * Write the table that maps a state number to the token kind it matches, for every lexical state.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public static void dumpStatesForKind (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    final NfaBuildState aNfa = nfa ();
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA -> dumpStatesForStateJava (aCodeGenerator);
      case CPP -> dumpStatesForStateCpp (aCodeGenerator);
      default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    boolean bMoreThanOne = false;
    int nCnt = 0;

    switch (eOutputLanguage)
    {
      case JAVA ->
      {
        aCodeGenerator.genCodeLine ("protected static final class Kinds {");
        aCodeGenerator.genCode ("  protected static final int[][] kindForState = ");
      }
      case CPP ->
      {
        aCodeGenerator.switchToStaticsFile ();
        aCodeGenerator.genCode ("static const int kindForState[" +
                                AbstractLexGenJavaLike.lexer ().getStateSetSize () +
                                "][" +
                                AbstractLexGenJavaLike.lexer ().getStateSetSize () +
                                "] = ");
      }
      default -> throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    if (aNfa.getKinds () == null)
    {
      aCodeGenerator.genCodeLine ("null;");
    }
    else
    {
      aCodeGenerator.genCodeLine ("{");

      for (final int [] aKind : aNfa.getKinds ())
      {
        if (bMoreThanOne)
          aCodeGenerator.genCodeLine (",");
        bMoreThanOne = true;

        if (aKind == null)
          aCodeGenerator.genCodeLine ("{}");
        else
        {
          nCnt = 0;
          aCodeGenerator.genCode ("{ ");
          for (final int nStateName : aKind)
          {
            if (nCnt % 15 == 0)
              aCodeGenerator.genCode ("\n  ");
            else
              if (nCnt > 1)
                aCodeGenerator.genCode (" ");

            aCodeGenerator.genCode (nStateName + ", ");
          }

          aCodeGenerator.genCode ("}");
        }
      }
      aCodeGenerator.genCodeLine ("\n};");
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        // Close class
        aCodeGenerator.genCode ("}");
        break;
      case CPP:
        // empty
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    aCodeGenerator.switchToMainFile ();
  }

  /**
   * Find what a composite state name stands for.
   *
   * @param nStateName
   *        The name to look for.
   * @return The member states of that composite set, or <code>null</code> if the name is not a
   *         composite one.
   */
  @Nullable
  public static int [] memberStatesOfComposite (final int nStateName)
  {
    final NfaBuildState aNfa = nfa ();
    for (final Map.Entry <String, int []> aEntry : aNfa.compositeStateTable ().entrySet ())
    {
      final Integer aName = aNfa.stateNameForComposite ().get (aEntry.getKey ());
      if (aName != null && aName.intValue () == nStateName)
        return aEntry.getValue ();
    }
    return null;
  }

  /**
   * Hand the finished automaton of one lexical state over to the TokenizerData that
   * JavaCCInterpreter runs, dropping the states that the optimisation passes left behind.
   *
   * @param nMaxState
   *        One past the highest state number in use.
   * @param nStartStateName
   *        The state a match starts in.
   * @param nLexicalStateIndex
   *        The lexical state this automaton belongs to.
   * @param nMatchAnyCharKind
   *        The kind that matches any character, or -1 if there is none.
   */
  public static void updateNfaData (final int nMaxState,
                                    final int nStartStateName,
                                    final int nLexicalStateIndex,
                                    final int nMatchAnyCharKind)
  {
    final NfaBuildState aNfa = nfa ();
    // Cleanup the state set.
    final Set <Integer> aDone = new HashSet <> ();
    final List <NfaState> aCleanStates = new ArrayList <> ();
    NfaState aStartState = null;
    for (int i = 0; i < aNfa.getAllStates ().size (); i++)
    {
      final NfaState aState = aNfa.getAllStates ().get (i);
      if (aState.m_nStateName == -1)
        continue;
      if (!aDone.add (Integer.valueOf (aState.m_nStateName)))
        continue;
      aCleanStates.add (aState);
      if (aState.m_nStateName == nStartStateName)
      {
        aStartState = aState;
      }
    }

    if (aStartState == null && nStartStateName != -1)
    {
      // The start state is composite and carries a name that no NfaState object has:
      // _addCompositeStateSet ran out of member states whose name it could reuse and allocated a
      // dummy one past the end instead. The generated token manager copes, because it only ever
      // switches on the name, but the TokenizerData the interpreter reads needs a state to start
      // in. Remember what the name stands for so that buildTokenizerData can synthesize one.
      final int [] aMembers = memberStatesOfComposite (nStartStateName);
      if (aMembers != null)
        tokenizerBuild ().compositeStartStates ()
                         .put (Integer.valueOf (nLexicalStateIndex),
                               new CompositeStartState (nStartStateName, aMembers));
    }

    tokenizerBuild ().initialStates ().put (Integer.valueOf (nLexicalStateIndex), aStartState);
    tokenizerBuild ().statesForLexicalState ().put (Integer.valueOf (nLexicalStateIndex), aCleanStates);
    tokenizerBuild ().nfaStateOffset ().put (Integer.valueOf (nLexicalStateIndex), Integer.valueOf (nMaxState));
    tokenizerBuild ().matchAnyChar ()
                     .put (Integer.valueOf (nLexicalStateIndex),
                           Integer.valueOf (nMatchAnyCharKind > 0 ? nMatchAnyCharKind : Integer.MAX_VALUE));
  }

  /**
   * Turn everything {@link #updateNfaData(int, int, int, int)} collected per lexical state into the
   * {@link TokenizerData} that {@code JavaCCInterpreter} reads.
   * <p>
   * The state names of each lexical state are shifted so that all of them fit into one array, and a
   * lexical state whose start state is composite gets a synthesized state for it - the name the NFA
   * construction handed out belongs to no {@code NfaState} object.
   *
   * @param aTokenizerData
   *        Where to put the result. May not be <code>null</code>.
   */
  public static void buildTokenizerData (@NonNull final TokenizerData aTokenizerData)
  {
    NfaState [] aCleanStates;
    final List <NfaState> aCleanStateList = new ArrayList <> ();
    // Which lexical state a state came from is needed further down, to shift its composite members
    // by the same offset as its own name
    final Map <NfaState, Integer> aOffsetOfState = new IdentityHashMap <> ();
    for (final int l : tokenizerBuild ().statesForLexicalState ().keySet ())
    {
      final int nOffset = tokenizerBuild ().nfaStateOffset ().get (Integer.valueOf (l)).intValue ();
      final List <NfaState> aStates = tokenizerBuild ().statesForLexicalState ().get (Integer.valueOf (l));
      for (final NfaState state : aStates)
      {
        if (state.m_nStateName == -1)
          continue;
        state.m_nStateName += nOffset;
        aOffsetOfState.put (state, Integer.valueOf (nOffset));
      }
      aCleanStateList.addAll (aStates);
    }
    aCleanStates = new NfaState [aCleanStateList.size ()];
    for (final NfaState s : aCleanStateList)
    {
      assert (aCleanStates[s.m_nStateName] == null);
      aCleanStates[s.m_nStateName] = s;
      final Set <Character> aChars = new TreeSet <> ();
      for (int c = 0; c <= Character.MAX_VALUE; c++)
      {
        if (s._canMoveUsingChar ((char) c))
        {
          aChars.add (Character.valueOf ((char) c));
        }
      }
      final Set <Integer> aNextStates = new TreeSet <> ();
      if (s.m_aNext != null)
      {
        for (final NfaState next : s.m_aNext.m_aEpsilonMoves)
        {
          aNextStates.add (Integer.valueOf (next.m_nStateName));
        }
      }
      final SortedSet <Integer> aComposite = new TreeSet <> ();
      if (s.m_bIsComposite)
      {
        // The member names are the ones from before the shift above, so they need the same offset
        final int nOffset = aOffsetOfState.getOrDefault (s, Integer.valueOf (0)).intValue ();
        for (final int c : s.m_aCompositeStates)
          aComposite.add (Integer.valueOf (c + nOffset));
      }
      aTokenizerData.addNfaState (s.m_nStateName, aChars, aNextStates, aComposite, s.m_nKindToPrint);
    }
    final Map <Integer, Integer> aInitStates = new HashMap <> ();
    int nNextFreeStateName = aCleanStateList.size ();
    for (final int l : tokenizerBuild ().initialStates ().keySet ())
    {
      final NfaState x = tokenizerBuild ().initialStates ().get (Integer.valueOf (l));
      if (x != null)
      {
        aInitStates.put (Integer.valueOf (l), Integer.valueOf (x.m_nStateName));
        continue;
      }

      // No NfaState object for the start state, so emit one that only stands for its members. It
      // matches nothing itself - the interpreter adds the composite members to the current set and
      // moves on from those.
      //
      // The name has to be a fresh one past every real state. The dummy index the NFA construction
      // handed out is one past the last state of *its* lexical state, which is exactly where the
      // next lexical state's names start after the shift above, so reusing it would land on that
      // state instead.
      final CompositeStartState aComposite = tokenizerBuild ().compositeStartStates ().get (Integer.valueOf (l));
      if (aComposite == null)
      {
        aInitStates.put (Integer.valueOf (l), Integer.valueOf (-1));
        continue;
      }

      final int nOffset = tokenizerBuild ().nfaStateOffset ().get (Integer.valueOf (l)).intValue ();
      final int nName = nNextFreeStateName++;
      final SortedSet <Integer> aMembers = new TreeSet <> ();
      for (final int c : aComposite.aMemberStates ())
        aMembers.add (Integer.valueOf (c + nOffset));
      aTokenizerData.addNfaState (nName, new TreeSet <> (), new TreeSet <> (), aMembers, Integer.MAX_VALUE);
      aInitStates.put (Integer.valueOf (l), Integer.valueOf (nName));
    }
    aTokenizerData.setInitialStates (aInitStates);
    aTokenizerData.setWildcardKind (tokenizerBuild ().matchAnyChar ());
  }

  /**
   * Look a state up by its state number.
   *
   * @param nIndex
   *        The state number, or -1.
   * @return The state, or <code>null</code> if nIndex is -1.
   */
  @Nullable
  public static NfaState getNfaState (final int nIndex)
  {
    if (nIndex == -1)
      return null;

    for (final NfaState s : nfa ().getAllStates ())
      if (s.m_nStateName == nIndex)
        return s;

    assert false;
    return null;
  }
}
