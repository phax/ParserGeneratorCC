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
import com.helger.pgcc.context.TokenizerDataBuildState.CompositeStartState;
import com.helger.pgcc.context.TokenizerDataBuildState;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.output.java.LexGenJava;

/**
 * The state of a Non-deterministic Finite Automaton.
 */
public class NfaState
{
  /**
   * @return The build state of the lexical state that is currently being generated. Never
   *         <code>null</code>.
   */
  public static TokenizerDataBuildState tokenizerBuild ()
  {
    return LexGenJava.lexer ().tokenizerDataBuild ();
  }

  public static NfaBuildState nfa ()
  {
    return LexGenJava.lexer ().nfa ();
  }

  public static void reInitStatic ()
  {
    nfa ().resetForLexicalState ();
  }

  /**
   * The state's own data. Package private for the NFA construction, but read and written by the
   * lexer emitters in com.helger.pgcc.output.*, which is why they are public rather than package
   * private.
   */
  public long [] m_asciiMoves = new long [2];
  public char [] m_charMoves = null;
  private char [] m_rangeMoves = null;
  public NfaState m_next = null;
  private NfaState m_stateForCase;
  public final List <NfaState> m_epsilonMoves = new ArrayList <> ();
  private String m_epsilonMovesString;

  private final int m_id;
  public int m_stateName = -1;
  public int m_kind = Integer.MAX_VALUE;
  private int m_lookingFor;
  private int m_usefulEpsilonMoves = 0;
  public int m_inNextOf;
  private int m_lexState;
  private int m_nonAsciiMethod = -1;
  private int m_kindToPrint = Integer.MAX_VALUE;
  public boolean m_dummy = false;
  private boolean m_isComposite = false;
  private int [] m_compositeStates = null;
  public boolean m_isFinal = false;
  private List <Integer> m_loByteVec;
  private int [] m_nonAsciiMoveIndices;
  private int m_round = 0;
  private int m_onlyChar = 0;
  private char m_matchSingleChar;

  public NfaState ()
  {
    m_id = nfa ().getAndIncIdCnt ();
    nfa ().getAllStates ().add (this);
    m_lexState = LexGenJava.lexer ().getLexStateIndex ();
    m_lookingFor = LexGenJava.lexer ().getCurKind ();
  }

  @NonNull
  private NfaState _createClone ()
  {
    final NfaState aRetVal = new NfaState ();

    aRetVal.m_isFinal = m_isFinal;
    aRetVal.m_kind = m_kind;
    aRetVal.m_lookingFor = m_lookingFor;
    aRetVal.m_lexState = m_lexState;
    aRetVal.m_inNextOf = m_inNextOf;

    aRetVal._mergeMoves (this);

    return aRetVal;
  }

  private static void _insertInOrder (final List <NfaState> v, final NfaState s)
  {
    int j = 0;
    for (; j < v.size (); j++)
    {
      final NfaState aTmp = v.get (j);
      if (aTmp.m_id > s.m_id)
        break;
      if (aTmp.m_id == s.m_id)
        return;
    }

    v.add (j, s);
  }

  private static char [] _expandCharArr (final char [] aOldArr, final int nIncr)
  {
    final char [] aRet = new char [aOldArr.length + nIncr];
    System.arraycopy (aOldArr, 0, aRet, 0, aOldArr.length);
    return aRet;
  }

  public void addMove (@NonNull final NfaState aNewState)
  {
    if (!m_epsilonMoves.contains (aNewState))
      _insertInOrder (m_epsilonMoves, aNewState);
  }

  private final void _addASCIIMove (final char c)
  {
    m_asciiMoves[c / 64] |= (1L << (c % 64));
  }

  public void addChar (final char c)
  {
    m_onlyChar++;
    m_matchSingleChar = c;

    if (c < 128) // ASCII char
    {
      _addASCIIMove (c);
      return;
    }

    if (m_charMoves == null)
      m_charMoves = new char [10];

    int nLen = m_charMoves.length;

    if (m_charMoves[nLen - 1] != 0)
    {
      m_charMoves = _expandCharArr (m_charMoves, 10);
      nLen += 10;
    }

    int i = 0;
    for (; i < nLen; i++)
      if (m_charMoves[i] == 0 || m_charMoves[i] > c)
        break;

    if (!nfa ().isUnicodeWarningGiven () &&
      c > 0xff &&
      !Options.isJavaUnicodeEscape () &&
      !Options.isJavaUserCharStream ())
    {
      nfa ().setUnicodeWarningGiven (true);
      JavaCCErrors.warning (LexGenJava.lexer ().getCurRE (),
                            "Non-ASCII characters used in regular expression.\n" +
                                                             "Please make sure you use the correct Reader when you create the parser, " +
                                                             "one that can handle your character set.");
    }

    char cTemp = m_charMoves[i];
    m_charMoves[i] = c;

    for (i++; i < nLen; i++)
    {
      if (cTemp == 0)
        break;

      final char cTemp1 = m_charMoves[i];
      m_charMoves[i] = cTemp;
      cTemp = cTemp1;
    }
  }

  public void addRange (final char cPleft, final char cRight)
  {
    char cLeft = cPleft;
    m_onlyChar = 2;
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

    if (!nfa ().isUnicodeWarningGiven () &&
      (cLeft > 0xff || cRight > 0xff) &&
      !Options.isJavaUnicodeEscape () &&
      !Options.isJavaUserCharStream ())
    {
      nfa ().setUnicodeWarningGiven (true);
      JavaCCErrors.warning (LexGenJava.lexer ().getCurRE (),
                            "Non-ASCII characters used in regular expression.\n" +
                                                             "Please make sure you use the correct Reader when you create the parser, " +
                                                             "one that can handle your character set.");
    }

    if (m_rangeMoves == null)
      m_rangeMoves = new char [20];

    int nLen = m_rangeMoves.length;

    if (m_rangeMoves[nLen - 1] != 0)
    {
      m_rangeMoves = _expandCharArr (m_rangeMoves, 20);
      nLen += 20;
    }

    int i = 0;
    for (; i < nLen; i += 2)
      if (m_rangeMoves[i] == 0 ||
        (m_rangeMoves[i] > cLeft) ||
        ((m_rangeMoves[i] == cLeft) && (m_rangeMoves[i + 1] > cRight)))
        break;

    cTempLeft1 = m_rangeMoves[i];
    cTempRight1 = m_rangeMoves[i + 1];
    m_rangeMoves[i] = cLeft;
    m_rangeMoves[i + 1] = cRight;

    for (i += 2; i < nLen; i += 2)
    {
      if (cTempLeft1 == 0)
        break;

      cTempLeft2 = m_rangeMoves[i];
      cTempRight2 = m_rangeMoves[i + 1];
      m_rangeMoves[i] = cTempLeft1;
      m_rangeMoves[i + 1] = cTempRight1;
      cTempLeft1 = cTempLeft2;
      cTempRight1 = cTempRight2;
    }
  }

  private static boolean _equalCharArr (final char [] aArr1, final char [] aArr2)
  {
    if (aArr1 == aArr2)
      return true;

    if (aArr1 != null && aArr2 != null && aArr1.length == aArr2.length)
    {
      for (int i = aArr1.length; i-- > 0;)
        if (aArr1[i] != aArr2[i])
          return false;

      return true;
    }

    return false;
  }

  // From hereon down all the functions are used for code generation

  private boolean m_closureDone = false;

  /**
   * This function computes the closure and also updates the kind so that any time there is a move
   * to this state, it can go on epsilon to a new state in the epsilon moves that might have a lower
   * kind of token number for the same length.
   */

  private void _recursiveEpsilonClosure ()
  {
    if (m_closureDone || nfa ().getMark ()[m_id])
      return;

    nfa ().getMark ()[m_id] = true;

    // Recursively do closure
    for (final NfaState tmp : m_epsilonMoves)
      tmp._recursiveEpsilonClosure ();

    // Operate on copy!
    for (final NfaState tmp : new ArrayList <> (m_epsilonMoves))
    {
      for (final NfaState tmp1 : tmp.m_epsilonMoves)
      {
        if (tmp1._isUsefulState () && !m_epsilonMoves.contains (tmp1))
        {
          _insertInOrder (m_epsilonMoves, tmp1);
          nfa ().setDone (false);
        }
      }

      if (m_kind > tmp.m_kind)
        m_kind = tmp.m_kind;
    }

    if (hasTransitions () && !m_epsilonMoves.contains (this))
      _insertInOrder (m_epsilonMoves, this);
  }

  private boolean _isUsefulState ()
  {
    return m_isFinal || hasTransitions ();
  }

  public boolean hasTransitions ()
  {
    return (m_asciiMoves[0] != 0L ||
      m_asciiMoves[1] != 0L ||
      (m_charMoves != null && m_charMoves[0] != 0) ||
      (m_rangeMoves != null && m_rangeMoves[0] != 0));
  }

  private void _mergeMoves (final NfaState aOther)
  {
    // Warning : This function does not merge epsilon moves
    if (m_asciiMoves == aOther.m_asciiMoves)
      JavaCCErrors.internalError ();

    m_asciiMoves[0] = m_asciiMoves[0] | aOther.m_asciiMoves[0];
    m_asciiMoves[1] = m_asciiMoves[1] | aOther.m_asciiMoves[1];

    if (aOther.m_charMoves != null)
    {
      if (m_charMoves == null)
        m_charMoves = aOther.m_charMoves;
      else
      {
        final char [] aTmpCharMoves = new char [m_charMoves.length + aOther.m_charMoves.length];
        System.arraycopy (m_charMoves, 0, aTmpCharMoves, 0, m_charMoves.length);
        m_charMoves = aTmpCharMoves;

        for (final char aCharMove : aOther.m_charMoves)
          addChar (aCharMove);
      }
    }

    if (aOther.m_rangeMoves != null)
    {
      if (m_rangeMoves == null)
        m_rangeMoves = aOther.m_rangeMoves;
      else
      {
        final char [] aTmpRangeMoves = new char [m_rangeMoves.length + aOther.m_rangeMoves.length];
        System.arraycopy (m_rangeMoves, 0, aTmpRangeMoves, 0, m_rangeMoves.length);
        m_rangeMoves = aTmpRangeMoves;
        for (int i = 0; i < aOther.m_rangeMoves.length; i += 2)
          addRange (aOther.m_rangeMoves[i], aOther.m_rangeMoves[i + 1]);
      }
    }

    if (aOther.m_kind < m_kind)
      m_kind = aOther.m_kind;

    if (aOther.m_kindToPrint < m_kindToPrint)
      m_kindToPrint = aOther.m_kindToPrint;

    m_isFinal |= aOther.m_isFinal;
  }

  NfaState createEquivState (final List <NfaState> aStates)
  {
    final NfaState aNewState = aStates.get (0)._createClone ();

    aNewState.m_next = new NfaState ();

    _insertInOrder (aNewState.m_next.m_epsilonMoves, aStates.get (0).m_next);

    for (int i = 1; i < aStates.size (); i++)
    {
      final NfaState aTmp2 = (aStates.get (i));

      if (aTmp2.m_kind < aNewState.m_kind)
        aNewState.m_kind = aTmp2.m_kind;

      aNewState.m_isFinal |= aTmp2.m_isFinal;

      _insertInOrder (aNewState.m_next.m_epsilonMoves, aTmp2.m_next);
    }

    return aNewState;
  }

  private NfaState _getEquivalentRunTimeState ()
  {
    Outer: for (int i = nfa ().getAllStates ().size (); i-- > 0;)
    {
      final NfaState aOther = nfa ().getAllStates ().get (i);

      if (this != aOther &&
        aOther.m_stateName != -1 &&
        m_kindToPrint == aOther.m_kindToPrint &&
        m_asciiMoves[0] == aOther.m_asciiMoves[0] &&
        m_asciiMoves[1] == aOther.m_asciiMoves[1] &&
        _equalCharArr (m_charMoves, aOther.m_charMoves) &&
        _equalCharArr (m_rangeMoves, aOther.m_rangeMoves))
      {
        if (m_next == aOther.m_next)
          return aOther;
        else
          if (m_next != null && aOther.m_next != null)
          {
            if (m_next.m_epsilonMoves.size () == aOther.m_next.m_epsilonMoves.size ())
            {
              for (int j = 0; j < m_next.m_epsilonMoves.size (); j++)
                if (m_next.m_epsilonMoves.get (j) != aOther.m_next.m_epsilonMoves.get (j))
                  continue Outer;

              return aOther;
            }
          }
      }
    }

    return null;
  }

  // generates code (without outputting it) and returns the name used.
  public void generateCode ()
  {
    if (m_stateName != -1)
      return;

    if (m_next != null)
    {
      m_next.generateCode ();
      if (m_next.m_kind != Integer.MAX_VALUE)
        m_kindToPrint = m_next.m_kind;
    }

    if (m_stateName == -1 && hasTransitions ())
    {
      final NfaState aTmp = _getEquivalentRunTimeState ();

      if (aTmp != null)
      {
        m_stateName = aTmp.m_stateName;
        // ????
        // tmp.inNextOf += inNextOf;
        // ????
        m_dummy = true;
        return;
      }

      m_stateName = nfa ().getAndIncGeneratedStates ();
      nfa ().indexedAllStates ().add (this);
      _generateNextStatesCode ();
    }
  }

  public static void computeClosures ()
  {
    // Back to front
    for (int i = nfa ().getAllStates ().size () - 1; i >= 0; --i)
    {
      final NfaState aTmp = nfa ().getAllStates ().get (i);
      if (!aTmp.m_closureDone)
        aTmp._optimizeEpsilonMoves (true);
    }

    // Operate on copy!
    for (final NfaState tmp : new ArrayList <> (nfa ().getAllStates ()))
      if (!tmp.m_closureDone)
        tmp._optimizeEpsilonMoves (false);

    if (false)
    {
      for (int i = 0; i < nfa ().getAllStates ().size (); i++)
      {
        final NfaState aTmp = nfa ().getAllStates ().get (i);
        final NfaState [] aEpsilonMoveArray = new NfaState [aTmp.m_epsilonMoves.size ()];
        aTmp.m_epsilonMoves.toArray (aEpsilonMoveArray);
      }
    }
  }

  private void _optimizeEpsilonMoves (final boolean bOptReqd)
  {
    // First do epsilon closure
    nfa ().setDone (false);
    while (!nfa ().isDone ())
    {
      if (nfa ().getMark () == null || nfa ().getMark ().length < nfa ().getAllStates ().size ())
        nfa ().setMark (new boolean [nfa ().getAllStates ().size ()]);

      for (int i = nfa ().getAllStates ().size (); i-- > 0;)
        nfa ().getMark ()[i] = false;

      nfa ().setDone (true);
      _recursiveEpsilonClosure ();
    }

    for (int i = nfa ().getAllStates ().size (); i-- > 0;)
    {
      final NfaState aTmp = nfa ().getAllStates ().get (i);
      aTmp.m_closureDone = nfa ().getMark ()[aTmp.m_id];
    }

    // Warning : The following piece of code is just an optimization.
    // in case of trouble, just remove this piece.

    boolean bSometingOptimized = true;

    NfaState aNewState = null;
    NfaState aTmp1, aTmp2;
    List <NfaState> aEquivStates = null;

    while (bSometingOptimized)
    {
      bSometingOptimized = false;
      for (int i = 0; bOptReqd && i < m_epsilonMoves.size (); i++)
      {
        aTmp1 = m_epsilonMoves.get (i);
        if (aTmp1.hasTransitions ())
        {
          for (int j = i + 1; j < m_epsilonMoves.size (); j++)
          {
            aTmp2 = m_epsilonMoves.get (j);
            if (aTmp2.hasTransitions () &&
              (aTmp1.m_asciiMoves[0] == aTmp2.m_asciiMoves[0] &&
                aTmp1.m_asciiMoves[1] == aTmp2.m_asciiMoves[1] &&
                _equalCharArr (aTmp1.m_charMoves, aTmp2.m_charMoves) &&
                _equalCharArr (aTmp1.m_rangeMoves, aTmp2.m_rangeMoves)))
            {
              if (aEquivStates == null)
              {
                aEquivStates = new ArrayList <> ();
                aEquivStates.add (aTmp1);
              }

              _insertInOrder (aEquivStates, aTmp2);
              m_epsilonMoves.remove (j--);
            }
          }
        }

        if (aEquivStates != null)
        {
          bSometingOptimized = true;
          String sTmp = "";
          for (final NfaState equivState : aEquivStates)
            sTmp += String.valueOf (equivState.m_id) + ", ";

          if ((aNewState = nfa ().equivStatesTable ().get (sTmp)) == null)
          {
            aNewState = createEquivState (aEquivStates);
            nfa ().equivStatesTable ().put (sTmp, aNewState);
          }

          m_epsilonMoves.remove (i--);
          m_epsilonMoves.add (aNewState);
          aEquivStates = null;
          aNewState = null;
        }
      }

      for (int i = 0; i < m_epsilonMoves.size (); i++)
      {
        // if ((tmp1 = (NfaState)epsilonMoves.elementAt(i)).next == null)
        // continue;
        aTmp1 = m_epsilonMoves.get (i);

        for (int j = i + 1; j < m_epsilonMoves.size (); j++)
        {
          aTmp2 = m_epsilonMoves.get (j);

          if (aTmp1.m_next == aTmp2.m_next)
          {
            if (aNewState == null)
            {
              aNewState = aTmp1._createClone ();
              aNewState.m_next = aTmp1.m_next;
              bSometingOptimized = true;
            }

            aNewState._mergeMoves (aTmp2);
            m_epsilonMoves.remove (j--);
          }
        }

        if (aNewState != null)
        {
          m_epsilonMoves.remove (i--);
          m_epsilonMoves.add (aNewState);
          aNewState = null;
        }
      }
    }

    // End Warning

    // Generate an array of states for epsilon moves (not vector)
    if (m_epsilonMoves.size () > 0)
    {
      for (int i = 0; i < m_epsilonMoves.size (); i++)
        // Since we are doing a closure, just epsilon moves are unncessary
        if (m_epsilonMoves.get (i).hasTransitions ())
          m_usefulEpsilonMoves++;
        else
          m_epsilonMoves.remove (i--);
    }
  }

  private void _generateNextStatesCode ()
  {
    if (m_next.m_usefulEpsilonMoves > 0)
      m_next._getEpsilonMovesString ();
  }

  private String _getEpsilonMovesString ()
  {
    final int [] aStateNames = new int [m_usefulEpsilonMoves];
    int nCnt = 0;

    if (m_epsilonMovesString != null)
      return m_epsilonMovesString;

    if (m_usefulEpsilonMoves > 0)
    {
      NfaState aTempState;
      m_epsilonMovesString = "{ ";
      for (final NfaState m_epsilonMove : m_epsilonMoves)
      {
        aTempState = m_epsilonMove;
        if (aTempState.hasTransitions ())
        {
          if (aTempState.m_stateName == -1)
            aTempState.generateCode ();

          nfa ().indexedAllStates ().get (aTempState.m_stateName).m_inNextOf++;
          aStateNames[nCnt] = aTempState.m_stateName;
          m_epsilonMovesString += aTempState.m_stateName + ", ";
          if (nCnt++ > 0 && nCnt % 16 == 0)
            m_epsilonMovesString += "\n";
        }
      }

      m_epsilonMovesString += "};";
    }

    m_usefulEpsilonMoves = nCnt;
    if (m_epsilonMovesString != null && nfa ().allNextStates ().get (m_epsilonMovesString) == null)
    {
      final int [] aStatesToPut = new int [m_usefulEpsilonMoves];

      System.arraycopy (aStateNames, 0, aStatesToPut, 0, nCnt);
      nfa ().allNextStates ().put (m_epsilonMovesString, aStatesToPut);
    }

    return m_epsilonMovesString;
  }

  public static boolean canStartNfaUsingAscii (final char c)
  {
    if (c >= 128)
      JavaCCErrors.internalError ();

    final String s = LexGenJava.lexer ().getInitialState ()._getEpsilonMovesString ();

    if (s == null || s.equals ("null;"))
      return false;

    final int [] aStates = nfa ().allNextStates ().get (s);

    for (final int aState : aStates)
    {
      final NfaState aTmp = nfa ().indexedAllStates ().get (aState);

      if ((aTmp.m_asciiMoves[c / 64] & (1L << c % 64)) != 0L)
        return true;
    }

    return false;
  }

  private boolean _canMoveUsingChar (final char c)
  {
    if (m_onlyChar == 1)
      return c == m_matchSingleChar;

    if (c < 128)
      return (m_asciiMoves[c / 64] & (1L << c % 64)) != 0L;

    // Just check directly if there is a move for this char
    if (m_charMoves != null && m_charMoves[0] != 0)
    {
      for (final char aCharMove : m_charMoves)
      {
        if (c == aCharMove)
          return true;
        if (c < aCharMove || aCharMove == 0)
          break;
      }
    }

    // For ranges, iterate thru the table to see if the current char
    // is in some range
    if (m_rangeMoves != null && m_rangeMoves[0] != 0)
      for (int i = 0; i < m_rangeMoves.length; i += 2)
      {
        if (c >= m_rangeMoves[i] && c <= m_rangeMoves[i + 1])
          return true;
        if (c < m_rangeMoves[i] || m_rangeMoves[i] == 0)
          break;
      }
    // return (nextForNegatedList != null);
    return false;
  }

  public int getFirstValidPos (final String s, final int nPos, final int nLen)
  {
    int i = nPos;
    if (m_onlyChar == 1)
    {
      final char c = m_matchSingleChar;
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

  public int moveFrom (final char c, final List <NfaState> aNewStates)
  {
    if (_canMoveUsingChar (c))
    {
      for (int i = m_next.m_epsilonMoves.size (); i-- > 0;)
        _insertInOrder (aNewStates, m_next.m_epsilonMoves.get (i));

      return m_kindToPrint;
    }

    return Integer.MAX_VALUE;
  }

  public static int moveFromSet (final char c, final List <NfaState> states, final List <NfaState> aNewStates)
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

  public static int moveFromSetForRegEx (final char c,
                                         final NfaState [] aStates,
                                         final NfaState [] aNewStates,
                                         final int nRound)
  {
    int nStart = 0;
    final int nSz = aStates.length;

    for (int i = 0; i < nSz; i++)
    {
      final NfaState aTmp1 = aStates[i];
      if (aTmp1 == null)
        break;

      if (aTmp1._canMoveUsingChar (c))
      {
        if (aTmp1.m_kindToPrint != Integer.MAX_VALUE)
        {
          aNewStates[nStart] = null;
          return 1;
        }

        final List <NfaState> v = aTmp1.m_next.m_epsilonMoves;
        for (int j = v.size () - 1; j >= 0; j--)
        {
          final NfaState aTmp2 = v.get (j);
          if (aTmp2.m_round != nRound)
          {
            aTmp2.m_round = nRound;
            aNewStates[nStart++] = aTmp2;
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

  private void _generateNonAsciiMoves (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    int i = 0, j = 0;
    int nCnt = 0;
    final long [] [] aLoBytes = new long [256] [4];

    if ((m_charMoves == null || m_charMoves[0] == 0) && (m_rangeMoves == null || m_rangeMoves[0] == 0))
      return;

    if (m_charMoves != null)
    {
      for (i = 0; i < m_charMoves.length; i++)
      {
        if (m_charMoves[i] == 0)
          break;

        final char cHiByte = (char) (m_charMoves[i] >> 8);
        aLoBytes[cHiByte][(m_charMoves[i] & 0xff) / 64] |= (1L << ((m_charMoves[i] & 0xff) % 64));
      }
    }

    if (m_rangeMoves != null)
    {
      for (i = 0; i < m_rangeMoves.length; i += 2)
      {
        if (m_rangeMoves[i] == 0)
          break;

        char c, r;

        r = (char) (m_rangeMoves[i + 1] & 0xff);
        char cHiByte = (char) (m_rangeMoves[i] >> 8);

        if (cHiByte == (char) (m_rangeMoves[i + 1] >> 8))
        {
          for (c = (char) (m_rangeMoves[i] & 0xff); c <= r; c++)
            aLoBytes[cHiByte][c / 64] |= (1L << (c % 64));

          continue;
        }

        for (c = (char) (m_rangeMoves[i] & 0xff); c <= 0xff; c++)
          aLoBytes[cHiByte][c / 64] |= (1L << (c % 64));

        while (++cHiByte < (char) (m_rangeMoves[i + 1] >> 8))
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
      if (aDone[i] || (aDone[i] = aLoBytes[i][0] == 0 && aLoBytes[i][1] == 0 && aLoBytes[i][2] == 0 && aLoBytes[i][3] == 0))
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
        Integer aInd = nfa ().loHiByteTab ().get (sTmp);
        if (aInd == null)
        {
          nfa ().getAllBitVectors ().add (sTmp);

          if (!allBitsSet (sTmp))
          {
            aCodeGenerator.genStaticArrayDeclaration (eOutputLanguage.getTypeLong (),
                                                     "jjbitVec" + nfa ().getLoHiByteCnt ());
            aCodeGenerator.genCodeLine (sTmp);
          }
          aInd = Integer.valueOf (nfa ().getAndIncLoHiByteCnt ());
          nfa ().loHiByteTab ().put (sTmp, aInd);
        }

        nfa ().getTmpIndices ()[nCnt++] = aInd.intValue ();

        sTmp = "{\n   " +
              eOutputLanguage.getLongHex (aLoBytes[i][0]) +
              ", " +
              eOutputLanguage.getLongHex (aLoBytes[i][1]) +
              ", " +
              eOutputLanguage.getLongHex (aLoBytes[i][2]) +
              ", " +
              eOutputLanguage.getLongHex (aLoBytes[i][3]) +
              "\n};";
        aInd = nfa ().loHiByteTab ().get (sTmp);
        if (aInd == null)
        {
          nfa ().getAllBitVectors ().add (sTmp);

          if (!allBitsSet (sTmp))
          {
            aCodeGenerator.genStaticArrayDeclaration (eOutputLanguage.getTypeLong (),
                                                     "jjbitVec" + nfa ().getLoHiByteCnt ());
            aCodeGenerator.genCodeLine (sTmp);
            // This one, unlike the other two, returns to the main file afterwards
            if (!eOutputLanguage.isJava ())
              aCodeGenerator.switchToMainFile ();
          }
          aInd = Integer.valueOf (nfa ().getAndIncLoHiByteCnt ());
          nfa ().loHiByteTab ().put (sTmp, aInd);
        }

        nfa ().getTmpIndices ()[nCnt++] = aInd.intValue ();

        aCommon = null;
      }
    }

    m_nonAsciiMoveIndices = new int [nCnt];
    System.arraycopy (nfa ().getTmpIndices (), 0, m_nonAsciiMoveIndices, 0, nCnt);

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

        Integer aInd = nfa ().loHiByteTab ().get (sTmp);
        if (aInd == null)
        {
          nfa ().getAllBitVectors ().add (sTmp);

          if (!allBitsSet (sTmp))
            aCodeGenerator.genStaticArrayDeclaration (eOutputLanguage.getTypeLong (),
                                                     "jjbitVec" + nfa ().getLoHiByteCnt ());
          aCodeGenerator.genCodeLine (sTmp);
          nfa ().loHiByteTab ().put (sTmp, aInd = Integer.valueOf (nfa ().getAndIncLoHiByteCnt ()));
        }

        if (m_loByteVec == null)
          m_loByteVec = new ArrayList <> ();

        m_loByteVec.add (Integer.valueOf (i));
        m_loByteVec.add (aInd);
      }
    }
    // System.out.println("");
    _updateDuplicateNonAsciiMoves ();
  }

  private void _updateDuplicateNonAsciiMoves ()
  {
    for (int i = 0; i < nfa ().nonAsciiTableForMethod ().size (); i++)
    {
      final NfaState aTmp = nfa ().nonAsciiTableForMethod ().get (i);
      if (_equalLoByteVectors (m_loByteVec, aTmp.m_loByteVec) &&
        _equalNonAsciiMoveIndices (m_nonAsciiMoveIndices, aTmp.m_nonAsciiMoveIndices))
      {
        m_nonAsciiMethod = i;
        return;
      }
    }

    m_nonAsciiMethod = nfa ().nonAsciiTableForMethod ().size ();
    nfa ().nonAsciiTableForMethod ().add (this);
  }

  private static boolean _equalLoByteVectors (final List <Integer> vec1, final List <Integer> aVec2)
  {
    if (vec1 == null || aVec2 == null)
      return false;

    if (vec1 == aVec2)
      return true;

    if (vec1.size () != aVec2.size ())
      return false;

    for (int i = 0; i < vec1.size (); i++)
    {
      if (vec1.get (i).intValue () != aVec2.get (i).intValue ())
        return false;
    }

    return true;
  }

  private static boolean _equalNonAsciiMoveIndices (final int [] aMoves1, final int [] aMoves2)
  {
    if (aMoves1 == aMoves2)
      return true;

    if (aMoves1 == null || aMoves2 == null)
      return false;

    if (aMoves1.length != aMoves2.length)
      return false;

    for (int i = 0; i < aMoves1.length; i++)
    {
      if (aMoves1[i] != aMoves2[i])
        return false;
    }

    return true;
  }

  static boolean allBitsSet (final String sBitVec)
  {
    return sBitVec.equals (nfa ().getAllBits ());
  }

  public static int addStartStateSet (final String sStateSetString)
  {
    return _addCompositeStateSet (sStateSetString, true);
  }

  private static int _addCompositeStateSet (final String sStateSetString, final boolean bStarts)
  {
    Integer aStateNameToReturn;

    if ((aStateNameToReturn = nfa ().stateNameForComposite ().get (sStateSetString)) != null)
      return aStateNameToReturn.intValue ();

    int nToRet = 0;
    final int [] aNameSet = nfa ().allNextStates ().get (sStateSetString);

    if (!bStarts)
      nfa ().stateBlockTable ().put (sStateSetString, sStateSetString);

    if (aNameSet == null)
      JavaCCErrors.internalError ();

    if (aNameSet.length == 1)
    {
      aStateNameToReturn = Integer.valueOf (aNameSet[0]);
      nfa ().stateNameForComposite ().put (sStateSetString, aStateNameToReturn);
      return aNameSet[0];
    }

    for (final int aElement : aNameSet)
    {
      if (aElement == -1)
        continue;

      final NfaState aSt = nfa ().indexedAllStates ().get (aElement);
      aSt.m_isComposite = true;
      aSt.m_compositeStates = aNameSet;
    }

    while (nToRet < aNameSet.length && (bStarts && nfa ().indexedAllStates ().get (aNameSet[nToRet]).m_inNextOf > 1))
      nToRet++;

    for (final String s : nfa ().compositeStateTable ().keySet ())
    {
      if (!s.equals (sStateSetString) && _intersect (sStateSetString, s))
      {
        final int [] aOther = nfa ().compositeStateTable ().get (s);

        while (nToRet < aNameSet.length &&
          ((bStarts && nfa ().indexedAllStates ().get (aNameSet[nToRet]).m_inNextOf > 1) ||
            _elemOccurs (aNameSet[nToRet], aOther) >= 0))
          nToRet++;
      }
    }

    int nTmp;

    if (nToRet >= aNameSet.length)
    {
      if (nfa ().getDummyStateIndex () == -1)
        nfa ().setDummyStateIndex (nfa ().getGeneratedStates ());
      else
        nfa ().setDummyStateIndex (nfa ().getDummyStateIndex () + 1);
      nTmp = nfa ().getDummyStateIndex ();

    }
    else
      nTmp = aNameSet[nToRet];

    aStateNameToReturn = Integer.valueOf (nTmp);
    nfa ().stateNameForComposite ().put (sStateSetString, aStateNameToReturn);
    nfa ().compositeStateTable ().put (sStateSetString, aNameSet);

    return nTmp;
  }

  private static int _stateNameForComposite (final String sStateSetString)
  {
    return nfa ().stateNameForComposite ().get (sStateSetString).intValue ();
  }

  public static int initStateName ()
  {
    final String s = LexGenJava.lexer ().getInitialState ()._getEpsilonMovesString ();

    if (LexGenJava.lexer ().getInitialState ().m_usefulEpsilonMoves != 0)
      return _stateNameForComposite (s);
    return -1;
  }

  public int generateInitMoves ()
  {
    _getEpsilonMovesString ();

    if (m_epsilonMovesString == null)
      m_epsilonMovesString = "null;";

    return addStartStateSet (m_epsilonMovesString);
  }

  private static int [] _getStateSetIndicesForUse (final String sArrayString)
  {
    int [] aRet;
    final int [] aSet = nfa ().allNextStates ().get (sArrayString);

    if ((aRet = nfa ().tableToDump ().get (sArrayString)) == null)
    {
      aRet = new int [2];
      aRet[0] = nfa ().getLastIndex ();
      aRet[1] = nfa ().getLastIndex () + aSet.length - 1;
      nfa ().setLastIndex (nfa ().getLastIndex () + aSet.length);
      nfa ().tableToDump ().put (sArrayString, aRet);
      nfa ().orderedStateSet ().add (aSet);
    }

    return aRet;
  }

  public static void dumpStateSets (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();

    aCodeGenerator.genStaticArrayDeclaration ("int", "jjnextStates");
    aCodeGenerator.genCode ("{");
    if (nfa ().orderedStateSet ().size () > 0)
    {
      int nCnt = 0;
      for (final int [] set : nfa ().orderedStateSet ())
      {
        for (final int aElement : set)
        {
          if (nCnt++ % 16 == 0)
            aCodeGenerator.genCode ("\n   ");

          aCodeGenerator.genCode (aElement + ", ");
        }
      }
    }
    else
      aCodeGenerator.genCode ("0");

    aCodeGenerator.genCodeLine ("\n};");
    aCodeGenerator.switchToMainFile ();
  }

  private static String _getStateSetString (final int [] aStates)
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

  public static String getStateSetString (@Nullable final List <NfaState> aStates)
  {
    if (aStates == null || aStates.size () == 0)
      return "null;";

    final int [] aSet = new int [aStates.size ()];
    String sRetVal = "{ ";
    for (int i = 0; i < aStates.size ();)
    {
      final int k = aStates.get (i).m_stateName;
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

  private static int _elemOccurs (final int nElem, final int [] aArr)
  {
    for (int i = aArr.length; i-- > 0;)
      if (aArr[i] == nElem)
        return i;

    return -1;
  }

  @SuppressWarnings ("unused")
  private boolean _findCommonBlocks ()
  {
    if (m_next == null || m_next.m_usefulEpsilonMoves <= 1)
      return false;

    if (nfa ().getStateDone () == null)
      nfa ().setStateDone (new boolean [nfa ().getGeneratedStates ()]);

    final String sSet = m_next.m_epsilonMovesString;

    final int [] aNameSet = nfa ().allNextStates ().get (sSet);

    if (aNameSet.length <= 2 || nfa ().compositeStateTable ().get (sSet) != null)
      return false;

    final int freq[] = new int [aNameSet.length];
    final boolean live[] = new boolean [aNameSet.length];
    final int [] aCount = new int [nfa ().allNextStates ().size ()];

    for (int i = 0; i < aNameSet.length; i++)
    {
      if (aNameSet[i] != -1)
      {
        live[i] = !nfa ().getStateDone ()[aNameSet[i]];
        if (live[i])
          aCount[0]++;
      }
    }

    int nBlockLen = 0, commonFreq = 0;
    boolean bNeedUpdate;

    for (final Map.Entry <String, int []> aEntry : nfa ().allNextStates ().entrySet ())
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
        if (nfa ().indexedAllStates ().get (aNameSet[i]).m_isComposite)
          return false;

        nfa ().getStateDone ()[aNameSet[i]] = true;
        aCommonBlock[nCnt++] = aNameSet[i];
        // System.out.print(nameSet[i] + ", ");
      }
    }

    // System.out.println("");

    final String s = _getStateSetString (aCommonBlock);

    Outer: for (final Map.Entry <String, int []> aEntry : nfa ().allNextStates ().entrySet ())
    {
      boolean bFirstOne = true;
      final String sStringToFix = aEntry.getKey ();
      final int [] aSetToFix = aEntry.getValue ();

      if (aSetToFix == aCommonBlock)
        continue;

      for (int k = 0; k < nCnt; k++)
      {
        final int nAt = _elemOccurs (aCommonBlock[k], aSetToFix);
        if (nAt >= 0)
        {
          if (!bFirstOne)
            aSetToFix[nAt] = -1;
          bFirstOne = false;
        }
        else
          continue Outer;
      }

      if (nfa ().stateSetsToFix ().get (sStringToFix) == null)
        nfa ().stateSetsToFix ().put (sStringToFix, aSetToFix);
    }

    m_next.m_usefulEpsilonMoves -= nBlockLen - 1;
    _addCompositeStateSet (s, false);
    return true;
  }

  @SuppressWarnings ("unused")
  private boolean _checkNextOccursTogether ()
  {
    if (m_next == null || m_next.m_usefulEpsilonMoves <= 1)
      return true;

    final String sSet = m_next.m_epsilonMovesString;

    final int [] aNameSet = nfa ().allNextStates ().get (sSet);

    if (aNameSet.length == 1 ||
      nfa ().compositeStateTable ().get (sSet) != null ||
      nfa ().stateSetsToFix ().get (sSet) != null)
      return false;

    final Map <String, int []> aOccursIn = new HashMap <> ();
    final NfaState aTmp = nfa ().getAllStates ().get (aNameSet[0]);

    for (int i = 1; i < aNameSet.length; i++)
    {
      final NfaState aTmp1 = nfa ().getAllStates ().get (aNameSet[i]);

      if (aTmp.m_inNextOf != aTmp1.m_inNextOf)
        return false;
    }

    for (final Map.Entry <String, int []> aEntry : nfa ().allNextStates ().entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] aTmpSet = aEntry.getValue ();

      if (aTmpSet == aNameSet)
        continue;

      int nIsPresent = 0;
      int j = 0;
      for (final int aElement : aNameSet)
      {
        if (_elemOccurs (aElement, aTmpSet) >= 0)
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
        if (nfa ().compositeStateTable ().get (s) != null || nfa ().stateSetsToFix ().get (s) != null)
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

      if (!nfa ().stateSetsToFix ().containsKey (s))
        nfa ().stateSetsToFix ().put (s, aSetToFix);

      for (int k = 0; k < aSetToFix.length; k++)
      {
        // Not >= since need the first one (0)
        if (_elemOccurs (aSetToFix[k], aNameSet) > 0)
          aSetToFix[k] = -1;
      }
    }

    m_next.m_usefulEpsilonMoves = 1;
    _addCompositeStateSet (m_next.m_epsilonMovesString, false);
    return true;
  }

  private static void _fixStateSets ()
  {
    final Map <String, int []> aFixedSets = new HashMap <> ();
    final int [] aTmp = new int [nfa ().getGeneratedStates ()];

    for (final Map.Entry <String, int []> aEntry : nfa ().stateSetsToFix ().entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] aToFix = aEntry.getValue ();
      int nCnt = 0;

      // System.out.print("Fixing : ");
      for (final int aElement : aToFix)
      {
        // System.out.print(toFix[i] + ", ");
        if (aElement != -1)
          aTmp[nCnt++] = aElement;
      }

      final int [] aFixed = new int [nCnt];
      System.arraycopy (aTmp, 0, aFixed, 0, nCnt);
      aFixedSets.put (s, aFixed);
      nfa ().allNextStates ().put (s, aFixed);
      // System.out.println(" as " + GetStateSetString(fixed));
    }

    for (final NfaState tmpState : nfa ().getAllStates ())
    {
      if (tmpState.m_next == null || tmpState.m_next.m_usefulEpsilonMoves == 0)
        continue;

      /*
       * if (compositeStateTable.get(tmpState.next.epsilonMovesString) != null)
       * tmpState.next.usefulEpsilonMoves = 1; else
       */
      final int [] aNewSet = aFixedSets.get (tmpState.m_next.m_epsilonMovesString);
      if (aNewSet != null)
        tmpState._fixNextStates (aNewSet);
    }
  }

  private final void _fixNextStates (final int [] aNewSet)
  {
    m_next.m_usefulEpsilonMoves = aNewSet.length;
    // next.epsilonMovesString = GetStateSetString(newSet);
  }

  private static boolean _intersect (final String sSet1, final String sSet2)
  {
    if (sSet1 == null || sSet2 == null)
      return false;

    final int [] aNameSet1 = nfa ().allNextStates ().get (sSet1);
    final int [] aNameSet2 = nfa ().allNextStates ().get (sSet2);

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

  private static void _dumpHeadForCase (final CodeGenerator aCodeGenerator, final int nByteNum)
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

  private static List <List <NfaState>> _partitionStatesSetForAscii (final int [] aStates, final int nByteNum)
  {
    final int [] aCardinalities = new int [aStates.length];
    List <NfaState> aOriginal = new ArrayList <> ();
    final List <List <NfaState>> aPartition = new ArrayList <> ();
    NfaState aTmp;

    for (@SuppressWarnings ("unused")
    final int x : aStates)
      aOriginal.add (null);

    int nCnt = 0;
    for (int i = 0; i < aStates.length; i++)
    {
      aTmp = nfa ().getAllStates ().get (aStates[i]);

      if (aTmp.m_asciiMoves[nByteNum] != 0L)
      {
        int j;
        final int p = _numberOfBitsSet (aTmp.m_asciiMoves[nByteNum]);

        for (j = 0; j < i; j++)
          if (aCardinalities[j] <= p)
            break;

        for (int k = i; k > j; k--)
          aCardinalities[k] = aCardinalities[k - 1];

        aCardinalities[j] = p;

        aOriginal.add (j, aTmp);
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
      aTmp = aOriginal.remove (0);

      long nBitVec = aTmp.m_asciiMoves[nByteNum];
      final List <NfaState> aSubSet = new ArrayList <> ();
      aSubSet.add (aTmp);

      for (int j = 0; j < aOriginal.size (); j++)
      {
        final NfaState aTmp1 = aOriginal.get (j);

        if ((aTmp1.m_asciiMoves[nByteNum] & nBitVec) == 0L)
        {
          nBitVec |= aTmp1.m_asciiMoves[nByteNum];
          aSubSet.add (aTmp1);
          aOriginal.remove (j--);
        }
      }

      aPartition.add (aSubSet);
    }

    return aPartition;
  }

  private String _printNoBreak (final CodeGenerator aCodeGenerator, final int nByteNum, final boolean [] aDumped)
  {
    if (m_inNextOf != 1)
      JavaCCErrors.internalError ();

    aDumped[m_stateName] = true;

    if (nByteNum >= 0)
    {
      if (m_asciiMoves[nByteNum] != 0L)
      {
        aCodeGenerator.genCodeLine ("               case " + m_stateName + ":");
        _dumpAsciiMoveForCompositeState (aCodeGenerator, nByteNum, false);
        return "";
      }
    }
    else
      if (m_nonAsciiMethod != -1)
      {
        aCodeGenerator.genCodeLine ("               case " + m_stateName + ":");
        _dumpNonAsciiMoveForCompositeState (aCodeGenerator);
        return "";
      }

    return ("               case " + m_stateName + ":\n");
  }

  private static void _dumpCompositeStatesAsciiMoves (final CodeGenerator aCodeGenerator,
                                                      final String sKey,
                                                      final int nByteNum,
                                                      final boolean [] aDumped)
  {
    final int [] aNameSet = nfa ().allNextStates ().get (sKey);

    if (aNameSet.length == 1 || aDumped[_stateNameForComposite (sKey)])
      return;

    NfaState aToBePrinted = null;
    int nNeededStates = 0;
    NfaState aStateForCase = null;
    String sToPrint = "";
    final boolean bStateBlock = (nfa ().stateBlockTable ().get (sKey) != null);

    for (final int aElement : aNameSet)
    {
      final NfaState aTmp = nfa ().getAllStates ().get (aElement);

      if (aTmp.m_asciiMoves[nByteNum] != 0L)
      {
        if (nNeededStates++ == 1)
          break;
        aToBePrinted = aTmp;
      }
      else
        aDumped[aTmp.m_stateName] = true;

      if (aTmp.m_stateForCase != null)
      {
        if (aStateForCase != null)
          JavaCCErrors.internalError ();

        aStateForCase = aTmp.m_stateForCase;
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

      if (!aDumped[aToBePrinted.m_stateName] && !bStateBlock && aToBePrinted.m_inNextOf > 1)
        aCodeGenerator.genCodeLine ("               case " + aToBePrinted.m_stateName + ":");

      aDumped[aToBePrinted.m_stateName] = true;
      aToBePrinted._dumpAsciiMove (aCodeGenerator, nByteNum, aDumped);
      return;
    }

    final List <List <NfaState>> aPartition = _partitionStatesSetForAscii (aNameSet, nByteNum);

    if (StringHelper.isNotEmpty (sToPrint))
      aCodeGenerator.genCode (sToPrint);

    final int nKeyState = _stateNameForComposite (sKey);
    aCodeGenerator.genCodeLine ("               case " + nKeyState + ":");
    if (nKeyState < nfa ().getGeneratedStates ())
      aDumped[nKeyState] = true;

    for (final List <NfaState> subSet : aPartition)
    {
      int nIndex = 0;
      for (final NfaState tmp : subSet)
      {
        if (bStateBlock)
          aDumped[tmp.m_stateName] = true;
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
    if (m_next == null || m_next.m_epsilonMovesString == null)
      return false;

    final int [] aSet = nfa ().allNextStates ().get (m_next.m_epsilonMovesString);
    return _elemOccurs (m_stateName, aSet) >= 0;
  }

  private void _dumpAsciiMoveForCompositeState (final CodeGenerator aCodeGenerator,
                                                final int nByteNum,
                                                final boolean bElseNeeded)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    boolean bNextIntersects = _selfLoop ();

    for (final NfaState temp1 : nfa ().getAllStates ())
    {
      if (this == temp1 ||
        temp1.m_stateName == -1 ||
        temp1.m_dummy ||
        m_stateName == temp1.m_stateName ||
        temp1.m_asciiMoves[nByteNum] == 0L)
        continue;

      if (!bNextIntersects && _intersect (temp1.m_next.m_epsilonMovesString, m_next.m_epsilonMovesString))
      {
        bNextIntersects = true;
        break;
      }
    }

    // System.out.println(stateName + " \'s nextIntersects : " +
    // nextIntersects);
    String sPrefix = "";
    if (m_asciiMoves[nByteNum] != 0xffffffffffffffffL)
    {
      final int nOneBit = _isOnlyOneBitSet (m_asciiMoves[nByteNum]);

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
                                   eOutputLanguage.getLongHex (m_asciiMoves[nByteNum]) +
                                   " & l) != " +
                                   eOutputLanguage.getLongPlain (0) +
                                   ")");
      sPrefix = "   ";
    }

    if (m_kindToPrint != Integer.MAX_VALUE)
    {
      if (m_asciiMoves[nByteNum] != 0xffffffffffffffffL)
      {
        aCodeGenerator.genCodeLine ("                  {");
      }

      aCodeGenerator.genCodeLine (sPrefix + "                  if (kind > " + m_kindToPrint + ")");
      aCodeGenerator.genCodeLine (sPrefix + "                     kind = " + m_kindToPrint + ";");
    }

    if (m_next != null && m_next.m_usefulEpsilonMoves > 0)
    {
      final int [] aStateNames = nfa ().allNextStates ().get (m_next.m_epsilonMovesString);
      if (m_next.m_usefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];

        if (bNextIntersects)
          aCodeGenerator.genCodeLine (sPrefix + "                  { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine (sPrefix + "                  jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_next.m_usefulEpsilonMoves == 2 && bNextIntersects)
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
          final int [] aIndices = _getStateSetIndicesForUse (m_next.m_epsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode (sPrefix + "                  { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              nfa ().setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              nfa ().setJJCheckNAddStatesUnaryNeeded (true);
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

    if (m_asciiMoves[nByteNum] != 0xffffffffffffffffL && m_kindToPrint != Integer.MAX_VALUE)
      aCodeGenerator.genCodeLine ("                  }");
  }

  private void _dumpAsciiMove (final CodeGenerator aCodeGenerator, final int nByteNum, final boolean dumped[])
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    boolean bNextIntersects = _selfLoop () && m_isComposite;
    boolean bOnlyState = true;

    for (final NfaState s_allState : nfa ().getAllStates ())
    {
      final NfaState aTemp1 = s_allState;

      if (this == aTemp1 ||
        aTemp1.m_stateName == -1 ||
        aTemp1.m_dummy ||
        m_stateName == aTemp1.m_stateName ||
        aTemp1.m_asciiMoves[nByteNum] == 0L)
        continue;

      if (bOnlyState && (m_asciiMoves[nByteNum] & aTemp1.m_asciiMoves[nByteNum]) != 0L)
        bOnlyState = false;

      if (!bNextIntersects && _intersect (aTemp1.m_next.m_epsilonMovesString, m_next.m_epsilonMovesString))
        bNextIntersects = true;

      if (!dumped[aTemp1.m_stateName] &&
        !aTemp1.m_isComposite &&
        m_asciiMoves[nByteNum] == aTemp1.m_asciiMoves[nByteNum] &&
        m_kindToPrint == aTemp1.m_kindToPrint &&
        (m_next.m_epsilonMovesString == aTemp1.m_next.m_epsilonMovesString ||
          (m_next.m_epsilonMovesString != null &&
            aTemp1.m_next.m_epsilonMovesString != null &&
            m_next.m_epsilonMovesString.equals (aTemp1.m_next.m_epsilonMovesString))))
      {
        dumped[aTemp1.m_stateName] = true;
        aCodeGenerator.genCodeLine ("               case " + aTemp1.m_stateName + ":");
      }
    }

    // if (onlyState)
    // nextIntersects = false;

    final int nOneBit = _isOnlyOneBitSet (m_asciiMoves[nByteNum]);
    if (m_asciiMoves[nByteNum] != 0xffffffffffffffffL)
    {
      if ((m_next == null || m_next.m_usefulEpsilonMoves == 0) && m_kindToPrint != Integer.MAX_VALUE)
      {
        String sKindCheck = "";

        if (!bOnlyState)
          sKindCheck = " && kind > " + m_kindToPrint;

        if (nOneBit != -1)
          aCodeGenerator.genCodeLine ("                  if (curChar == " + (64 * nByteNum + nOneBit) + sKindCheck + ")");
        else
          aCodeGenerator.genCodeLine ("                  if ((" +
                                     eOutputLanguage.getLongHex (m_asciiMoves[nByteNum]) +
                                     " & l) != " +
                                     eOutputLanguage.getLongPlain (0) +
                                     sKindCheck +
                                     ")");

        aCodeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");

        if (bOnlyState)
          aCodeGenerator.genCodeLine ("                  break;");
        else
          aCodeGenerator.genCodeLine ("                  break;");

        return;
      }
    }

    String sPrefix = "";
    if (m_kindToPrint != Integer.MAX_VALUE)
    {

      if (nOneBit != -1)
      {
        aCodeGenerator.genCodeLine ("                  if (curChar != " + (64 * nByteNum + nOneBit) + ")");
        aCodeGenerator.genCodeLine ("                     break;");
      }
      else
        if (m_asciiMoves[nByteNum] != 0xffffffffffffffffL)
        {
          aCodeGenerator.genCodeLine ("                  if ((" +
                                     eOutputLanguage.getLongHex (m_asciiMoves[nByteNum]) +
                                     " & l) == " +
                                     eOutputLanguage.getLongPlain (0) +
                                     ")");
          aCodeGenerator.genCodeLine ("                     break;");
        }

      if (bOnlyState)
      {
        aCodeGenerator.genCodeLine ("                  kind = " + m_kindToPrint + ";");
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (kind > " + m_kindToPrint + ")");
        aCodeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");
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
        if (m_asciiMoves[nByteNum] != 0xffffffffffffffffL)
        {
          aCodeGenerator.genCodeLine ("                  if ((" +
                                     eOutputLanguage.getLongHex (m_asciiMoves[nByteNum]) +
                                     " & l) != " +
                                     eOutputLanguage.getLongPlain (0) +
                                     ")");
          sPrefix = "   ";
        }
    }

    if (m_next != null && m_next.m_usefulEpsilonMoves > 0)
    {
      final int [] aStateNames = nfa ().allNextStates ().get (m_next.m_epsilonMovesString);
      if (m_next.m_usefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];
        if (bNextIntersects)
          aCodeGenerator.genCodeLine (sPrefix + "                  { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine (sPrefix + "                  jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_next.m_usefulEpsilonMoves == 2 && bNextIntersects)
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
          final int [] aIndices = _getStateSetIndicesForUse (m_next.m_epsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode (sPrefix + "                  { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              nfa ().setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              nfa ().setJJCheckNAddStatesUnaryNeeded (true);
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

  private static void _dumpAsciiMoves (final CodeGenerator aCodeGenerator, final int nByteNum)
  {
    final boolean [] aDumped = new boolean [Math.max (nfa ().getGeneratedStates (), nfa ().getDummyStateIndex () + 1)];

    _dumpHeadForCase (aCodeGenerator, nByteNum);

    for (final String s : nfa ().compositeStateTable ().keySet ())
      _dumpCompositeStatesAsciiMoves (aCodeGenerator, s, nByteNum, aDumped);

    for (final NfaState s_allState : nfa ().getAllStates ())
    {
      final NfaState aTemp = s_allState;

      if (aDumped[aTemp.m_stateName] ||
        aTemp.m_lexState != LexGenJava.lexer ().getLexStateIndex () ||
        !aTemp.hasTransitions () ||
        aTemp.m_dummy ||
        aTemp.m_stateName == -1)
        continue;

      String sToPrint = "";

      if (aTemp.m_stateForCase != null)
      {
        if (aTemp.m_inNextOf == 1)
          continue;

        if (aDumped[aTemp.m_stateForCase.m_stateName])
          continue;

        sToPrint = (aTemp.m_stateForCase._printNoBreak (aCodeGenerator, nByteNum, aDumped));

        if (aTemp.m_asciiMoves[nByteNum] == 0L)
        {
          if (StringHelper.isEmpty (sToPrint))
            aCodeGenerator.genCodeLine ("                  break;");

          continue;
        }
      }

      if (aTemp.m_asciiMoves[nByteNum] == 0L)
        continue;

      if (StringHelper.isNotEmpty (sToPrint))
        aCodeGenerator.genCode (sToPrint);

      aDumped[aTemp.m_stateName] = true;
      aCodeGenerator.genCodeLine ("               case " + aTemp.m_stateName + ":");
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

  private static void _dumpCompositeStatesNonAsciiMoves (final CodeGenerator aCodeGenerator,
                                                         final String sKey,
                                                         final boolean [] aDumped)
  {
    final int [] aNameSet = nfa ().allNextStates ().get (sKey);

    if (aNameSet.length == 1 || aDumped[_stateNameForComposite (sKey)])
      return;

    NfaState aToBePrinted = null;
    int nNeededStates = 0;
    NfaState aTmp;
    NfaState aStateForCase = null;
    String sToPrint = "";
    final boolean bStateBlock = (nfa ().stateBlockTable ().get (sKey) != null);

    for (final int aElement : aNameSet)
    {
      aTmp = nfa ().getAllStates ().get (aElement);

      if (aTmp.m_nonAsciiMethod != -1)
      {
        if (nNeededStates++ == 1)
          break;
        aToBePrinted = aTmp;
      }
      else
        aDumped[aTmp.m_stateName] = true;

      if (aTmp.m_stateForCase != null)
      {
        if (aStateForCase != null)
          JavaCCErrors.internalError ();

        aStateForCase = aTmp.m_stateForCase;
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

      if (!aDumped[aToBePrinted.m_stateName] && !bStateBlock && aToBePrinted.m_inNextOf > 1)
        aCodeGenerator.genCodeLine ("               case " + aToBePrinted.m_stateName + ":");

      aDumped[aToBePrinted.m_stateName] = true;
      aToBePrinted._dumpNonAsciiMove (aCodeGenerator, aDumped);
      return;
    }

    if (StringHelper.isNotEmpty (sToPrint))
      aCodeGenerator.genCode (sToPrint);

    final int nKeyState = _stateNameForComposite (sKey);
    aCodeGenerator.genCodeLine ("               case " + nKeyState + ":");
    if (nKeyState < nfa ().getGeneratedStates ())
      aDumped[nKeyState] = true;

    for (final int aElement : aNameSet)
    {
      aTmp = nfa ().getAllStates ().get (aElement);

      if (aTmp.m_nonAsciiMethod != -1)
      {
        if (bStateBlock)
          aDumped[aTmp.m_stateName] = true;
        aTmp._dumpNonAsciiMoveForCompositeState (aCodeGenerator);
      }
    }

    if (bStateBlock)
      aCodeGenerator.genCodeLine ("                  break;");
    else
      aCodeGenerator.genCodeLine ("                  break;");
  }

  private final void _dumpNonAsciiMoveForCompositeState (final CodeGenerator aCodeGenerator)
  {
    boolean bNextIntersects = _selfLoop ();
    for (final NfaState temp1 : nfa ().getAllStates ())
    {
      if (this == temp1 ||
        temp1.m_stateName == -1 ||
        temp1.m_dummy ||
        m_stateName == temp1.m_stateName ||
        (temp1.m_nonAsciiMethod == -1))
        continue;

      if (!bNextIntersects && _intersect (temp1.m_next.m_epsilonMovesString, m_next.m_epsilonMovesString))
      {
        bNextIntersects = true;
        break;
      }
    }

    if (!Options.isJavaUnicodeEscape () && !nfa ().isUnicodeWarningGiven ())
    {
      if (m_loByteVec != null && m_loByteVec.size () > 1)
        aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                   m_loByteVec.get (1).intValue () +
                                   "[i2" +
                                   "] & l2) != 0L)");
    }
    else
    {
      aCodeGenerator.genCodeLine ("                  if (jjCanMove_" + m_nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
    }

    if (m_kindToPrint != Integer.MAX_VALUE)
    {
      aCodeGenerator.genCodeLine ("                  {");
      aCodeGenerator.genCodeLine ("                     if (kind > " + m_kindToPrint + ")");
      aCodeGenerator.genCodeLine ("                        kind = " + m_kindToPrint + ";");
    }

    if (m_next != null && m_next.m_usefulEpsilonMoves > 0)
    {
      final int [] aStateNames = nfa ().allNextStates ().get (m_next.m_epsilonMovesString);
      if (m_next.m_usefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];
        if (bNextIntersects)
          aCodeGenerator.genCodeLine ("                     { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine ("                     jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_next.m_usefulEpsilonMoves == 2 && bNextIntersects)
        {
          aCodeGenerator.genCodeLine ("                     { jjCheckNAddTwoStates(" +
                                     aStateNames[0] +
                                     ", " +
                                     aStateNames[1] +
                                     "); }");
        }
        else
        {
          final int [] aIndices = _getStateSetIndicesForUse (m_next.m_epsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode ("                     { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              nfa ().setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              nfa ().setJJCheckNAddStatesUnaryNeeded (true);
            }
            aCodeGenerator.genCodeLine ("); }");
          }
          else
            aCodeGenerator.genCodeLine ("                     { jjAddStates(" + aIndices[0] + ", " + aIndices[1] + "); }");
        }
    }

    if (m_kindToPrint != Integer.MAX_VALUE)
      aCodeGenerator.genCodeLine ("                  }");
  }

  private final void _dumpNonAsciiMove (final CodeGenerator aCodeGenerator, final boolean dumped[])
  {
    boolean bNextIntersects = _selfLoop () && m_isComposite;

    for (final NfaState s_allState : nfa ().getAllStates ())
    {
      final NfaState aTemp1 = s_allState;

      if (this == aTemp1 ||
        aTemp1.m_stateName == -1 ||
        aTemp1.m_dummy ||
        m_stateName == aTemp1.m_stateName ||
        (aTemp1.m_nonAsciiMethod == -1))
        continue;

      if (!bNextIntersects && _intersect (aTemp1.m_next.m_epsilonMovesString, m_next.m_epsilonMovesString))
        bNextIntersects = true;

      if (!dumped[aTemp1.m_stateName] &&
        !aTemp1.m_isComposite &&
        m_nonAsciiMethod == aTemp1.m_nonAsciiMethod &&
        m_kindToPrint == aTemp1.m_kindToPrint &&
        (m_next.m_epsilonMovesString == aTemp1.m_next.m_epsilonMovesString ||
          (m_next.m_epsilonMovesString != null &&
            aTemp1.m_next.m_epsilonMovesString != null &&
            m_next.m_epsilonMovesString.equals (aTemp1.m_next.m_epsilonMovesString))))
      {
        dumped[aTemp1.m_stateName] = true;
        aCodeGenerator.genCodeLine ("               case " + aTemp1.m_stateName + ":");
      }
    }

    if (m_next == null || m_next.m_usefulEpsilonMoves <= 0)
    {
      final String sKindCheck = " && kind > " + m_kindToPrint;

      if (!Options.isJavaUnicodeEscape () && !nfa ().isUnicodeWarningGiven ())
      {
        if (m_loByteVec != null && m_loByteVec.size () > 1)
          aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                     m_loByteVec.get (1).intValue () +
                                     "[i2" +
                                     "] & l2) != 0L" +
                                     sKindCheck +
                                     ")");
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (jjCanMove_" +
                                   m_nonAsciiMethod +
                                   "(hiByte, i1, i2, l1, l2)" +
                                   sKindCheck +
                                   ")");
      }
      aCodeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");
      aCodeGenerator.genCodeLine ("                  break;");
      return;
    }

    String sPrefix = "   ";
    if (m_kindToPrint != Integer.MAX_VALUE)
    {
      if (!Options.isJavaUnicodeEscape () && !nfa ().isUnicodeWarningGiven ())
      {
        if (m_loByteVec != null && m_loByteVec.size () > 1)
        {
          aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                     m_loByteVec.get (1).intValue () +
                                     "[i2" +
                                     "] & l2) == 0L)");
          aCodeGenerator.genCodeLine ("                     break;");
        }
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (!jjCanMove_" +
                                   m_nonAsciiMethod +
                                   "(hiByte, i1, i2, l1, l2))");
        aCodeGenerator.genCodeLine ("                     break;");
      }

      aCodeGenerator.genCodeLine ("                  if (kind > " + m_kindToPrint + ")");
      aCodeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");
      sPrefix = "";
    }
    else
      if (!Options.isJavaUnicodeEscape () && !nfa ().isUnicodeWarningGiven ())
      {
        if (m_loByteVec != null && m_loByteVec.size () > 1)
          aCodeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                     m_loByteVec.get (1).intValue () +
                                     "[i2" +
                                     "] & l2) != 0L)");
      }
      else
      {
        aCodeGenerator.genCodeLine ("                  if (jjCanMove_" + m_nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
      }

    if (m_next != null && m_next.m_usefulEpsilonMoves > 0)
    {
      final int [] aStateNames = nfa ().allNextStates ().get (m_next.m_epsilonMovesString);
      if (m_next.m_usefulEpsilonMoves == 1)
      {
        final int nName = aStateNames[0];
        if (bNextIntersects)
          aCodeGenerator.genCodeLine (sPrefix + "                  { jjCheckNAdd(" + nName + "); }");
        else
          aCodeGenerator.genCodeLine (sPrefix + "                  jjstateSet[jjnewStateCnt++] = " + nName + ";");
      }
      else
        if (m_next.m_usefulEpsilonMoves == 2 && bNextIntersects)
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
          final int [] aIndices = _getStateSetIndicesForUse (m_next.m_epsilonMovesString);
          final boolean bNotTwo = (aIndices[0] + 1 != aIndices[1]);

          if (bNextIntersects)
          {
            aCodeGenerator.genCode (sPrefix + "                  { jjCheckNAddStates(" + aIndices[0]);
            if (bNotTwo)
            {
              nfa ().setJJCheckNAddStatesDualNeeded (true);
              aCodeGenerator.genCode (", " + aIndices[1]);
            }
            else
            {
              nfa ().setJJCheckNAddStatesUnaryNeeded (true);
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

  public static void dumpCharAndRangeMoves (final CodeGenerator aCodeGenerator)
  {
    final boolean [] aDumped = new boolean [Math.max (nfa ().getGeneratedStates (), nfa ().getDummyStateIndex () + 1)];

    _dumpHeadForCase (aCodeGenerator, -1);

    for (final String s : nfa ().compositeStateTable ().keySet ())
      _dumpCompositeStatesNonAsciiMoves (aCodeGenerator, s, aDumped);

    for (final NfaState temp : nfa ().getAllStates ())
    {
      if (temp.m_stateName == -1 ||
        aDumped[temp.m_stateName] ||
        temp.m_lexState != LexGenJava.lexer ().getLexStateIndex () ||
        !temp.hasTransitions () ||
        temp.m_dummy)
        continue;

      String sToPrint = "";

      if (temp.m_stateForCase != null)
      {
        if (temp.m_inNextOf == 1)
          continue;

        if (aDumped[temp.m_stateForCase.m_stateName])
          continue;

        sToPrint = temp.m_stateForCase._printNoBreak (aCodeGenerator, -1, aDumped);

        if (temp.m_nonAsciiMethod == -1)
        {
          if (StringHelper.isEmpty (sToPrint))
            aCodeGenerator.genCodeLine ("                  break;");

          continue;
        }
      }

      if (temp.m_nonAsciiMethod == -1)
        continue;

      if (StringHelper.isNotEmpty (sToPrint))
        aCodeGenerator.genCode (sToPrint);

      aDumped[temp.m_stateName] = true;
      // System.out.println("case : " + temp.stateName);
      aCodeGenerator.genCodeLine ("               case " + temp.m_stateName + ":");
      temp._dumpNonAsciiMove (aCodeGenerator, aDumped);
    }

    if (Options.isJavaUnicodeEscape () || nfa ().isUnicodeWarningGiven ())
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

  public static void dumpNonAsciiMoveMethods (final CodeGenerator aCodeGenerator)
  {
    if (!Options.isJavaUnicodeEscape () && !nfa ().isUnicodeWarningGiven ())
      return;

    if (nfa ().nonAsciiTableForMethod ().size () <= 0)
      return;

    for (final NfaState tmp : nfa ().nonAsciiTableForMethod ())
    {
      tmp._dumpNonAsciiMoveMethod (aCodeGenerator);
    }
  }

  private void _dumpNonAsciiMoveMethod (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("private static final " +
                                   eOutputLanguage.getTypeBoolean () +
                                   " jjCanMove_" +
                                   m_nonAsciiMethod +
                                   "(int hiByte, int i1, int i2, " +
                                   eOutputLanguage.getTypeLong () +
                                   " l1, " +
                                   eOutputLanguage.getTypeLong () +
                                   " l2)");
        break;
      case CPP:
        aCodeGenerator.generateMethodDefHeader (eOutputLanguage.getTypeBoolean (),
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjCanMove_" +
                                                                                            m_nonAsciiMethod +
                                                                                            "(int hiByte, int i1, int i2, " +
                                                                                            eOutputLanguage.getTypeLong () +
                                                                                            " l1, " +
                                                                                            eOutputLanguage.getTypeLong () +
                                                                                            " l2)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   switch(hiByte)");
    aCodeGenerator.genCodeLine ("   {");

    if (m_loByteVec != null && m_loByteVec.size () > 0)
    {
      for (int j = 0; j < m_loByteVec.size (); j += 2)
      {
        aCodeGenerator.genCodeLine ("      case " + m_loByteVec.get (j).intValue () + ":");
        if (!allBitsSet (nfa ().getAllBitVectors ().get (m_loByteVec.get (j + 1).intValue ())))
        {
          aCodeGenerator.genCodeLine ("         return ((jjbitVec" +
                                     m_loByteVec.get (j + 1).intValue () +
                                     "[i2" +
                                     "] & l2) != 0L);");
        }
        else
          aCodeGenerator.genCodeLine ("            return true;");
      }
    }

    aCodeGenerator.genCodeLine ("      default :");

    if (m_nonAsciiMoveIndices != null)
    {
      int j = m_nonAsciiMoveIndices.length;
      if (j > 0)
        do
        {
          if (!allBitsSet (nfa ().getAllBitVectors ().get (m_nonAsciiMoveIndices[j - 2])))
            aCodeGenerator.genCodeLine ("         if ((jjbitVec" + m_nonAsciiMoveIndices[j - 2] + "[i1] & l1) != 0L)");
          if (!allBitsSet (nfa ().getAllBitVectors ().get (m_nonAsciiMoveIndices[j - 1])))
          {
            aCodeGenerator.genCodeLine ("            if ((jjbitVec" +
                                       m_nonAsciiMoveIndices[j - 1] +
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
    final List <NfaState> v = nfa ().getAllStates ();
    nfa ().setAllStates (new ArrayList <> (Collections.nCopies (nfa ().getGeneratedStates (), null)));

    if (nfa ().getAllStates ().size () != nfa ().getGeneratedStates ())
      JavaCCErrors.internalError ();

    for (int j = 0; j < v.size (); j++)
    {
      final NfaState aTmp = v.get (j);
      if (aTmp.m_stateName != -1 && !aTmp.m_dummy)
        nfa ().getAllStates ().set (aTmp.m_stateName, aTmp);
    }
  }

  // private static boolean boilerPlateDumped = false;
  public static void printBoilerPlateJava (final CodeGenerator aCodeGenerator)
  {
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

    if (nfa ().isJJCheckNAddStatesDualNeeded ())
    {
      aCodeGenerator.genCodeLine ("private void jjCheckNAddStates(int start, int end)");
      aCodeGenerator.genCodeLine ("{");
      aCodeGenerator.genCodeLine ("   do {");
      aCodeGenerator.genCodeLine ("      jjCheckNAdd(jjnextStates[start]);");
      aCodeGenerator.genCodeLine ("   } while (start++ != end);");
      aCodeGenerator.genCodeLine ("}");
      aCodeGenerator.genCodeNewLine ();
    }

    if (nfa ().isJJCheckNAddStatesUnaryNeeded ())
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
  public static void printBoilerPlateCPP (final CodeGenerator aCodeGenerator)
  {
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

    if (nfa ().isJJCheckNAddStatesDualNeeded ())
    {
      aCodeGenerator.genCodeLine ("#define jjCheckNAddStates(start, end)\\");
      aCodeGenerator.genCodeLine ("{\\");
      aCodeGenerator.genCodeLine ("   for (int x = start; x <= end; x++) {\\");
      aCodeGenerator.genCodeLine ("      jjCheckNAdd(jjnextStates[x]);\\");
      aCodeGenerator.genCodeLine ("   } /*while (start++ != end);*/\\");
      aCodeGenerator.genCodeLine ("}");
      aCodeGenerator.genCodeNewLine ();
    }

    if (nfa ().isJJCheckNAddStatesUnaryNeeded ())
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
    final Map <String, String> aPrinted = new HashMap <> ();
    final boolean [] aPut = new boolean [nfa ().getGeneratedStates ()];
    int nCnt = 0;
    int nFoundAt = 0;

    Outer: for (final NfaState tmpState : nfa ().getAllStates ())
    {
      NfaState aStateForCase = null;
      if (tmpState.m_stateName == -1 ||
        tmpState.m_dummy ||
        !tmpState._isUsefulState () ||
        tmpState.m_next == null ||
        tmpState.m_next.m_usefulEpsilonMoves < 1)
        continue;

      final String s = tmpState.m_next.m_epsilonMovesString;

      if (nfa ().compositeStateTable ().get (s) != null || aPrinted.get (s) != null)
        continue;

      aPrinted.put (s, s);
      final int [] aNexts = nfa ().allNextStates ().get (s);

      if (aNexts.length == 1)
        continue;

      int nState = nCnt;
      // System.out.println("State " + tmpState.stateName + " : " + s);
      for (int i = 0; i < aNexts.length; i++)
      {
        if ((nState = aNexts[i]) == -1)
          continue;

        final NfaState aTmp = nfa ().getAllStates ().get (nState);

        if (!aTmp.m_isComposite && aTmp.m_inNextOf == 1)
        {
          if (aPut[nState])
            JavaCCErrors.internalError ();

          nFoundAt = i;
          nCnt++;
          aStateForCase = aTmp;
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
        if ((nState = aNexts[i]) == -1)
          continue;

        final NfaState aTmp = nfa ().getAllStates ().get (nState);

        if (!aPut[nState] && aTmp.m_inNextOf > 1 && !aTmp.m_isComposite && aTmp.m_stateForCase == null)
        {
          nCnt++;
          aNexts[i] = -1;
          aPut[nState] = true;

          final int nToSwap = aNexts[0];
          aNexts[0] = aNexts[nFoundAt];
          aNexts[nFoundAt] = nToSwap;

          aTmp.m_stateForCase = aStateForCase;
          aStateForCase.m_stateForCase = aTmp;
          nfa ().stateSetsToFix ().put (s, aNexts);

          // System.out.println("For : " + s + "; " + stateForCase.stateName +
          // " and " + tmp.stateName);

          continue Outer;
        }
      }

      for (final int aNext : aNexts)
      {
        if ((nState = aNext) == -1)
          continue;

        final NfaState aTmp = nfa ().getAllStates ().get (nState);
        if (aTmp.m_inNextOf <= 1)
          aPut[nState] = false;
      }
    }
  }

  public static void dumpMoveNfa (final CodeGenerator aCodeGenerator)
  {
    // if (!boilerPlateDumped)
    // PrintBoilerPlate(codeGenerator);

    // boilerPlateDumped = true;
    int [] aKindsForStates = null;
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();

    if (nfa ().getKinds () == null)
    {
      nfa ().setKinds (new int [LexGenJava.lexer ().getMaxLexStates ()] []);
      nfa ().setStatesForState (new int [LexGenJava.lexer ().getMaxLexStates ()] [] []);
    }

    _reArrange ();

    for (final NfaState s_allState : nfa ().getAllStates ())
    {
      final NfaState aTemp = s_allState;

      if (aTemp.m_lexState != LexGenJava.lexer ().getLexStateIndex () ||
        !aTemp.hasTransitions () ||
        aTemp.m_dummy ||
        aTemp.m_stateName == -1)
        continue;

      if (aKindsForStates == null)
      {
        aKindsForStates = new int [nfa ().getGeneratedStates ()];
        nfa ().getStatesForState ()[LexGenJava.lexer ().getLexStateIndex ()] = new int [Math.max (nfa ()
                                                                                                        .getGeneratedStates (),
                                                                                                  nfa ().getDummyStateIndex () +
                                                                                                                                1)] [];
      }

      aKindsForStates[aTemp.m_stateName] = aTemp.m_lookingFor;
      nfa ().getStatesForState ()[LexGenJava.lexer ().getLexStateIndex ()][aTemp.m_stateName] = aTemp.m_compositeStates;

      aTemp._generateNonAsciiMoves (aCodeGenerator);
    }

    for (final Map.Entry <String, Integer> aEntry : nfa ().stateNameForComposite ().entrySet ())
    {
      final String s = aEntry.getKey ();
      final int nState = aEntry.getValue ().intValue ();

      if (nState >= nfa ().getGeneratedStates ())
        nfa ().getStatesForState ()[LexGenJava.lexer ().getLexStateIndex ()][nState] = nfa ().allNextStates ().get (s);
    }

    if (nfa ().stateSetsToFix ().size () != 0)
      _fixStateSets ();

    nfa ().getKinds ()[LexGenJava.lexer ().getLexStateIndex ()] = aKindsForStates;

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("private int jjMoveNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(int startState, int curPos)");
        break;
      case CPP:
        aCodeGenerator.generateMethodDefHeader ("int",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjMoveNfa" +
                                                                                            LexGenJava.lexer ()
                                                                                                      .getLexStateSuffix () +
                                                                                            "(int startState, int curPos)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    aCodeGenerator.genCodeLine ("{");
    if (nfa ().getGeneratedStates () == 0)
    {
      aCodeGenerator.genCodeLine ("   return curPos;");
      aCodeGenerator.genCodeLine ("}");
      return;
    }

    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
    {
      aCodeGenerator.genCodeLine ("   int strKind = jjmatchedKind;");
      aCodeGenerator.genCodeLine ("   int strPos = jjmatchedPos;");
      aCodeGenerator.genCodeLine ("   int seenUpto;");
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("   input_stream.backup(seenUpto = curPos + 1);");
          aCodeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
          // TODO do not throw error
          aCodeGenerator.genCodeLine ("   catch(final java.io.IOException e) { throw new Error(\"Internal Error\"); }");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("   input_stream->backup(seenUpto = curPos + 1);");
          aCodeGenerator.genCodeLine ("   assert(!input_stream->endOfInput());");
          aCodeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      aCodeGenerator.genCodeLine ("   curPos = 0;");
    }

    aCodeGenerator.genCodeLine ("   int startsAt = 0;");
    aCodeGenerator.genCodeLine ("   jjnewStateCnt = " + nfa ().getGeneratedStates () + ";");
    aCodeGenerator.genCodeLine ("   int i = 1;");
    aCodeGenerator.genCodeLine ("   jjstateSet[0] = startState;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("      debugStream.println(\"   Starting NFA to match one of : \" + " +
                                     "jjKindsForStateVector(curLexState, jjstateSet, 0, 1));");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   Starting NFA to match one of : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, 0, 1).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("      debugStream.println(" +
                                     (LexGenJava.lexer ().getMaxLexStates () > 1
                                                                                 ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                 : "") +
                                     "\"Current character : \" + " +
                                     Options.getTokenMgrErrorClass () +
                                     ".addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                                     "input_stream->getEndLine(), input_stream->getEndColumn());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
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
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                     Integer.toHexString (Integer.MAX_VALUE) +
                                     ")");
          aCodeGenerator.genCodeLine ("         debugStream.println(" +
                                     "\"   Currently matched the first \" + (jjmatchedPos + 1) + \" characters as" +
                                     " a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                     Integer.toHexString (Integer.MAX_VALUE) +
                                     ")");
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        if (false)
        {
          // Old
          aCodeGenerator.genCodeLine ("      if ((i = jjnewStateCnt) == (startsAt = " +
                                     nfa ().getGeneratedStates () +
                                     " - (jjnewStateCnt = startsAt)))");
        }
        else
        {
          // New
          aCodeGenerator.genCodeLine ("      i = jjnewStateCnt;");
          aCodeGenerator.genCodeLine ("      jjnewStateCnt = startsAt;");
          aCodeGenerator.genCodeLine ("      startsAt = " + nfa ().getGeneratedStates () + " - jjnewStateCnt;");
          aCodeGenerator.genCodeLine ("      if (i == startsAt)");
        }
        break;
      case CPP:
        aCodeGenerator.genCodeLine ("      if ((i = jjnewStateCnt), (jjnewStateCnt = startsAt), (i == (startsAt = " +
                                   nfa ().getGeneratedStates () +
                                   " - startsAt)))");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
      aCodeGenerator.genCodeLine ("         break;");
    else
      aCodeGenerator.genCodeLine ("         return curPos;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("      debugStream.println(\"   Possible kinds of longer matches : \" + " +
                                     "jjKindsForStateVector(curLexState, jjstateSet, startsAt, i));");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   Possible kinds of longer matches : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, startsAt, i).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("      try { curChar = input_stream.readChar(); }");
        if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
          aCodeGenerator.genCodeLine ("      catch(final java.io.IOException e) { break; }");
        else
          aCodeGenerator.genCodeLine ("      catch(final java.io.IOException e) { return curPos; }");
        break;
      case CPP:
        if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
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
        case JAVA:
          aCodeGenerator.genCodeLine ("      debugStream.println(" +
                                     (LexGenJava.lexer ().getMaxLexStates () > 1
                                                                                 ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                 : "") +
                                     "\"Current character : \" + " +
                                     Options.getTokenMgrErrorClass () +
                                     ".addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                                     "input_stream->getEndLine(), input_stream->getEndColumn());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    aCodeGenerator.genCodeLine ("   }");

    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
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
        case JAVA:
          aCodeGenerator.genCodeLine ("         try { curChar = input_stream.readChar(); }");
          // TODO do not throw error
          aCodeGenerator.genCodeLine ("         catch(final java.io.IOException e) { throw new Error(\"Internal Error : Please send a bug report.\"); }");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("        {  assert(!input_stream->endOfInput());");
          aCodeGenerator.genCodeLine ("           curChar = input_stream->readChar(); }");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
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
    nfa ().getAllStates ().clear ();
  }

  public static void dumpStatesForStateCPP (final CodeGenerator aCodeGenerator)
  {
    if (nfa ().getStatesForState () == null)
    {
      assert (false) : "This should never be null.";
      aCodeGenerator.genCodeLine ("null;");
      return;
    }

    aCodeGenerator.switchToStaticsFile ();
    for (int i = 0; i < LexGenJava.lexer ().getMaxLexStates (); i++)
    {
      if (nfa ().getStatesForState ()[i] == null)
      {
        continue;
      }

      for (int j = 0; j < nfa ().getStatesForState ()[i].length; j++)
      {
        final int [] aStateSet = nfa ().getStatesForState ()[i][j];

        aCodeGenerator.genCode ("const int stateSet_" +
                               i +
                               "_" +
                               j +
                               "[" +
                               LexGenJava.lexer ().getStateSetSize () +
                               "] = ");
        if (aStateSet == null)
        {
          aCodeGenerator.genCodeLine ("   { " + j + " };");
          continue;
        }

        aCodeGenerator.genCode ("   { ");

        for (final int aElement : aStateSet)
          aCodeGenerator.genCode (aElement + ", ");

        aCodeGenerator.genCodeLine ("};");
      }

    }

    for (int i = 0; i < LexGenJava.lexer ().getMaxLexStates (); i++)
    {
      aCodeGenerator.genCodeLine ("const int *stateSet_" + i + "[] = {");
      if (nfa ().getStatesForState ()[i] == null)
      {
        aCodeGenerator.genCodeLine (" NULL, ");
        aCodeGenerator.genCodeLine ("};");
        continue;
      }

      for (int j = 0; j < nfa ().getStatesForState ()[i].length; j++)
      {
        aCodeGenerator.genCode ("stateSet_" + i + "_" + j + ",");
      }
      aCodeGenerator.genCodeLine ("};");
    }

    aCodeGenerator.genCode ("const int** statesForState[] = { ");
    for (int i = 0; i < LexGenJava.lexer ().getMaxLexStates (); i++)
    {
      aCodeGenerator.genCodeLine ("stateSet_" + i + ", ");
    }

    aCodeGenerator.genCodeLine ("\n};");
    aCodeGenerator.switchToMainFile ();
  }

  public static void dumpStatesForStateJava (final CodeGenerator aCodeGenerator)
  {
    aCodeGenerator.genCodeLine ("protected static final class States {");
    aCodeGenerator.genCode ("  protected static final int[][][] statesForState = ");

    if (nfa ().getStatesForState () == null)
    {
      aCodeGenerator.genCodeLine ("null;");
    }
    else
    {
      aCodeGenerator.genCodeLine ("{");
      for (int i = 0; i < LexGenJava.lexer ().getMaxLexStates (); i++)
      {
        if (nfa ().getStatesForState ()[i] == null)
        {
          aCodeGenerator.genCodeLine (" {},");
          continue;
        }

        aCodeGenerator.genCodeLine (" {");
        for (int j = 0; j < nfa ().getStatesForState ()[i].length; j++)
        {
          final int [] aStateSet = nfa ().getStatesForState ()[i][j];

          if (aStateSet == null)
          {
            aCodeGenerator.genCodeLine ("   { " + j + " },");
          }
          else
          {
            aCodeGenerator.genCode ("   { ");
            for (final int aElement : aStateSet)
              aCodeGenerator.genCode (aElement + ", ");
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

  public static void dumpStatesForKind (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        dumpStatesForStateJava (aCodeGenerator);
        break;
      case CPP:
        dumpStatesForStateCPP (aCodeGenerator);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    boolean bMoreThanOne = false;
    int nCnt = 0;

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("protected static final class Kinds {");
        aCodeGenerator.genCode ("  protected static final int[][] kindForState = ");
        break;
      case CPP:
        aCodeGenerator.switchToStaticsFile ();
        aCodeGenerator.genCode ("static const int kindForState[" +
                               LexGenJava.lexer ().getStateSetSize () +
                               "][" +
                               LexGenJava.lexer ().getStateSetSize () +
                               "] = ");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    if (nfa ().getKinds () == null)
    {
      aCodeGenerator.genCodeLine ("null;");
    }
    else
    {
      aCodeGenerator.genCodeLine ("{");

      for (final int [] aKind : nfa ().getKinds ())
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
          for (final int aElement : aKind)
          {
            if (nCnt % 15 == 0)
              aCodeGenerator.genCode ("\n  ");
            else
              if (nCnt > 1)
                aCodeGenerator.genCode (" ");

            aCodeGenerator.genCode (aElement + ", ");
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
    for (final Map.Entry <String, int []> aEntry : nfa ().compositeStateTable ().entrySet ())
    {
      final Integer aName = nfa ().stateNameForComposite ().get (aEntry.getKey ());
      if (aName != null && aName.intValue () == nStateName)
        return aEntry.getValue ();
    }
    return null;
  }

  public static void updateNfaData (final int nMaxState,
                                    final int nStartStateName,
                                    final int nLexicalStateIndex,
                                    final int nMatchAnyCharKind)
  {
    // Cleanup the state set.
    final Set <Integer> aDone = new HashSet <> ();
    final List <NfaState> aCleanStates = new ArrayList <> ();
    NfaState aStartState = null;
    for (int i = 0; i < nfa ().getAllStates ().size (); i++)
    {
      final NfaState aTmp = nfa ().getAllStates ().get (i);
      if (aTmp.m_stateName == -1)
        continue;
      if (!aDone.add (Integer.valueOf (aTmp.m_stateName)))
        continue;
      aCleanStates.add (aTmp);
      if (aTmp.m_stateName == nStartStateName)
      {
        aStartState = aTmp;
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
                         .put (Integer.valueOf (nLexicalStateIndex), new CompositeStartState (nStartStateName, aMembers));
    }

    tokenizerBuild ().initialStates ().put (Integer.valueOf (nLexicalStateIndex), aStartState);
    tokenizerBuild ().statesForLexicalState ().put (Integer.valueOf (nLexicalStateIndex), aCleanStates);
    tokenizerBuild ().nfaStateOffset ().put (Integer.valueOf (nLexicalStateIndex), Integer.valueOf (nMaxState));
    tokenizerBuild ().matchAnyChar ()
                     .put (Integer.valueOf (nLexicalStateIndex),
                           Integer.valueOf (nMatchAnyCharKind > 0 ? nMatchAnyCharKind : Integer.MAX_VALUE));
  }

  /**
   * Add a state that stands for a composite set and matches nothing itself, unless the state is
   * already there.
   *
   * @param tokenizerData
   *        Where to add it. May not be <code>null</code>.
   * @param nName
   *        The name of the composite state, already shifted by the lexical state offset.
   * @param aMemberStates
   *        The member state names, not yet shifted.
   * @param nOffset
   *        The lexical state offset to shift the member names by.
   */
  public static void buildTokenizerData (final TokenizerData aTokenizerData)
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
        if (state.m_stateName == -1)
          continue;
        state.m_stateName += nOffset;
        aOffsetOfState.put (state, Integer.valueOf (nOffset));
      }
      aCleanStateList.addAll (aStates);
    }
    aCleanStates = new NfaState [aCleanStateList.size ()];
    for (final NfaState s : aCleanStateList)
    {
      assert (aCleanStates[s.m_stateName] == null);
      aCleanStates[s.m_stateName] = s;
      final Set <Character> aChars = new TreeSet <> ();
      for (int c = 0; c <= Character.MAX_VALUE; c++)
      {
        if (s._canMoveUsingChar ((char) c))
        {
          aChars.add (Character.valueOf ((char) c));
        }
      }
      final Set <Integer> aNextStates = new TreeSet <> ();
      if (s.m_next != null)
      {
        for (final NfaState next : s.m_next.m_epsilonMoves)
        {
          aNextStates.add (Integer.valueOf (next.m_stateName));
        }
      }
      final SortedSet <Integer> aComposite = new TreeSet <> ();
      if (s.m_isComposite)
      {
        // The member names are the ones from before the shift above, so they need the same offset
        final int nOffset = aOffsetOfState.getOrDefault (s, Integer.valueOf (0)).intValue ();
        for (final int c : s.m_compositeStates)
          aComposite.add (Integer.valueOf (c + nOffset));
      }
      aTokenizerData.addNfaState (s.m_stateName, aChars, aNextStates, aComposite, s.m_kindToPrint);
    }
    final Map <Integer, Integer> aInitStates = new HashMap <> ();
    int nNextFreeStateName = aCleanStateList.size ();
    for (final int l : tokenizerBuild ().initialStates ().keySet ())
    {
      final NfaState x = tokenizerBuild ().initialStates ().get (Integer.valueOf (l));
      if (x != null)
      {
        aInitStates.put (Integer.valueOf (l), Integer.valueOf (x.m_stateName));
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

  @Nullable
  public static NfaState getNfaState (final int nIndex)
  {
    if (nIndex == -1)
      return null;

    for (final NfaState s : nfa ().getAllStates ())
      if (s.m_stateName == nIndex)
        return s;

    assert false;
    return null;
  }
}
