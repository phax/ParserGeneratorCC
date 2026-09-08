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
import java.util.List;
import java.util.Map;

import com.helger.pgcc.parser.exp.ExpRStringLiteral.KindInfo;

/**
 * The state of building the string literal matcher of ONE lexical state - the trie over the
 * literals that becomes the {@code jjMoveStringLiteralDfa*} methods.
 * <p>
 * This is the instance state behind the static fields of
 * {@link com.helger.pgcc.parser.exp.ExpRStringLiteral}. Like {@link NfaBuildState} it is per
 * lexical state, not per run.
 *
 * @author Philip Helger
 */
public final class StringLiteralBuildState
{
  /** Default constructor. */
  public StringLiteralBuildState ()
  {}

  private boolean m_bBoilerPlateDumped = false;

  private int m_nMaxStrKind = 0;
  private int m_nMaxLen = 0;
  private int m_nCharCnt = 0;
  private List <Map <String, KindInfo>> m_aCharPosKind = new ArrayList <> ();
  private int [] m_aMaxLenForActive = new int [100];
  private String [] m_aAllImages;
  private int [] [] m_aIntermediateKinds;
  private int [] [] m_aIntermediateMatchedPos;
  private boolean [] m_aSubString;
  private boolean [] m_aSubStringAtPos;
  private Map <String, long []> [] m_aStatesForPos;

  /**
   * One past the highest ordinal of a string literal in the current lexical state, and therefore
   * the length of the per literal arrays here.
   *
   * @return The number of string literal kinds.
   */
  public int getMaxStrKind ()
  {
    return m_nMaxStrKind;
  }

  /**
   * One past the highest ordinal of a string literal in the current lexical state, and therefore
   * the length of the per literal arrays here.
   *
   * @param nMaxStrKind
   *        The number of string literal kinds.
   */
  public void setMaxStrKind (final int nMaxStrKind)
  {
    m_nMaxStrKind = nMaxStrKind;
  }

  /**
   * The length of the longest string literal in the current lexical state, which is how deep the
   * generated jjMoveStringLiteralDfa chain goes.
   *
   * @return The length in characters.
   */
  public int getMaxLen ()
  {
    return m_nMaxLen;
  }

  /**
   * The length of the longest string literal in the current lexical state, which is how deep the
   * generated jjMoveStringLiteralDfa chain goes.
   *
   * @param nMaxLen
   *        The length in characters.
   */
  public void setMaxLen (final int nMaxLen)
  {
    m_nMaxLen = nMaxLen;
  }

  /**
   * How many characters have been written to the current output line. It only exists so that the
   * generated literal tables get a line break every 80 characters or so.
   *
   * @return The number of characters on the line so far.
   */
  public int getCharCnt ()
  {
    return m_nCharCnt;
  }

  /**
   * How many characters have been written to the current output line. It only exists so that the
   * generated literal tables get a line break every 80 characters or so.
   *
   * @param nCharCnt
   *        The number of characters on the line so far.
   */
  public void setCharCnt (final int nCharCnt)
  {
    m_nCharCnt = nCharCnt;
  }

  /**
   * The trie of the string literals of the current lexical state: one entry per character
   * position, each mapping a character to the kinds that can still match after it. This is what
   * the generated jjMoveStringLiteralDfa chain is built from.
   *
   * @return The trie, indexed by character position. Never <code>null</code>.
   */
  public List <Map <String, KindInfo>> getCharPosKind ()
  {
    return m_aCharPosKind;
  }


  /**
   * The length of the longest string literal in each 64 kind block, indexed by
   * <code>ordinal / 64</code>. A generated jjMoveStringLiteralDfa method only takes an active
   * parameter for a block that can still be alive at its position.
   *
   * @return The lengths, indexed by kind block. Never <code>null</code>.
   */
  public int [] getMaxLenForActive ()
  {
    return m_aMaxLenForActive;
  }


  /**
   * The image of every string literal, indexed by ordinal, or <code>null</code> where the kind is
   * not a string literal of this lexical state.
   *
   * @return The images, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public String [] getAllImages ()
  {
    return m_aAllImages;
  }

  /**
   * The image of every string literal, indexed by ordinal, or <code>null</code> where the kind is
   * not a string literal of this lexical state.
   *
   * @param aAllImages
   *        The images, indexed by ordinal.
   */
  public void setAllImages (final String [] aAllImages)
  {
    m_aAllImages = aAllImages;
  }

  /**
   * For each string literal and each position in it, the lowest kind that a shorter literal would
   * already have matched there. It is what lets the generated DFA remember a shorter match while
   * it keeps looking for a longer one.
   *
   * @return The kinds, indexed by ordinal and then by position. May be <code>null</code> before generation
   *   *         starts.
   */
  public int [] [] getIntermediateKinds ()
  {
    return m_aIntermediateKinds;
  }

  /**
   * For each string literal and each position in it, the lowest kind that a shorter literal would
   * already have matched there. It is what lets the generated DFA remember a shorter match while
   * it keeps looking for a longer one.
   *
   * @param aIntermediateKinds
   *        The kinds, indexed by ordinal and then by position.
   */
  public void setIntermediateKinds (final int [] [] aIntermediateKinds)
  {
    m_aIntermediateKinds = aIntermediateKinds;
  }

  /**
   * The position at which the kind in {@link #getIntermediateKinds()} was matched, with the same
   * indexing.
   *
   * @return The positions, indexed by ordinal and then by position. May be <code>null</code> before
   *   *         generation starts.
   */
  public int [] [] getIntermediateMatchedPos ()
  {
    return m_aIntermediateMatchedPos;
  }

  /**
   * The position at which the kind in {@link #getIntermediateKinds()} was matched, with the same
   * indexing.
   *
   * @param aIntermediateMatchedPos
   *        The positions, indexed by ordinal and then by position.
   */
  public void setIntermediateMatchedPos (final int [] [] aIntermediateMatchedPos)
  {
    m_aIntermediateMatchedPos = aIntermediateMatchedPos;
  }

  /**
   * Whether each string literal is a prefix of some other literal in the same lexical state,
   * indexed by ordinal. One that is not can be accepted the moment it matches. A mixed case
   * lexical state marks all of them, because the optimisation does not hold there.
   *
   * @return The flags, indexed by ordinal. May be <code>null</code> before generation starts.
   */
  public boolean [] getSubString ()
  {
    return m_aSubString;
  }

  /**
   * Whether each string literal is a prefix of some other literal in the same lexical state,
   * indexed by ordinal. One that is not can be accepted the moment it matches. A mixed case
   * lexical state marks all of them, because the optimisation does not hold there.
   *
   * @param aSubString
   *        The flags, indexed by ordinal.
   */
  public void setSubString (final boolean [] aSubString)
  {
    m_aSubString = aSubString;
  }

  /**
   * Whether any string literal ending at each position is a prefix of a longer one, indexed by
   * position - the per position summary of {@link #getSubString()}.
   *
   * @return The flags, indexed by position. May be <code>null</code> before generation starts.
   */
  public boolean [] getSubStringAtPos ()
  {
    return m_aSubStringAtPos;
  }

  /**
   * Whether any string literal ending at each position is a prefix of a longer one, indexed by
   * position - the per position summary of {@link #getSubString()}.
   *
   * @param aSubStringAtPos
   *        The flags, indexed by position.
   */
  public void setSubStringAtPos (final boolean [] aSubStringAtPos)
  {
    m_aSubStringAtPos = aSubStringAtPos;
  }

  /**
   * For each position in the literals, the NFA state sets that a match can continue into, keyed
   * by the string form of the set. This is what the generated jjStopStringLiteralDfa methods
   * hand back to the NFA.
   *
   * @return The state sets, indexed by position. May be <code>null</code> before generation starts.
   */
  public Map <String, long []> [] getStatesForPos ()
  {
    return m_aStatesForPos;
  }

  /**
   * For each position in the literals, the NFA state sets that a match can continue into, keyed
   * by the string form of the set. This is what the generated jjStopStringLiteralDfa methods
   * hand back to the NFA.
   *
   * @param aStatesForPos
   *        The state sets, indexed by position.
   */
  public void setStatesForPos (final Map <String, long []> [] aStatesForPos)
  {
    m_aStatesForPos = aStatesForPos;
  }

  /** {@return whether the shared boiler plate methods were already emitted for this run} */
  public boolean isBoilerPlateDumped ()
  {
    return m_bBoilerPlateDumped;
  }

  /**
   * Whether the helper methods that every lexical state shares - jjStopAtPos and friends - have
   * already been written. They are emitted once per run, not once per lexical state.
   *
   * @param bBoilerPlateDumped
   *        <code>true</code> once they have been written.
   */
  public void setBoilerPlateDumped (final boolean bBoilerPlateDumped)
  {
    m_bBoilerPlateDumped = bBoilerPlateDumped;
  }

  /**
   * Start over for the next lexical state. Mirrors what {@code ExpRStringLiteral.reInitStatic ()}
   * used to do - note that the images, the character counter and the boiler plate flag are per run
   * and deliberately survive.
   */
  public void resetForLexicalState ()
  {
    m_nMaxStrKind = 0;
    m_nMaxLen = 0;
    m_aCharPosKind = new ArrayList <> ();
    m_aMaxLenForActive = new int [100];
    m_aIntermediateKinds = null;
    m_aIntermediateMatchedPos = null;
    m_aSubString = null;
    m_aSubStringAtPos = null;
    m_aStatesForPos = null;
  }
}
