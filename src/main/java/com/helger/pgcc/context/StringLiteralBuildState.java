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

  public int getMaxStrKind ()
  {
    return m_nMaxStrKind;
  }

  public void setMaxStrKind (final int nMaxStrKind)
  {
    m_nMaxStrKind = nMaxStrKind;
  }

  public int getMaxLen ()
  {
    return m_nMaxLen;
  }

  public void setMaxLen (final int nMaxLen)
  {
    m_nMaxLen = nMaxLen;
  }

  public int getCharCnt ()
  {
    return m_nCharCnt;
  }

  public void setCharCnt (final int nCharCnt)
  {
    m_nCharCnt = nCharCnt;
  }

  public List <Map <String, KindInfo>> getCharPosKind ()
  {
    return m_aCharPosKind;
  }

  public void setCharPosKind (final List <Map <String, KindInfo>> aCharPosKind)
  {
    m_aCharPosKind = aCharPosKind;
  }

  public int [] getMaxLenForActive ()
  {
    return m_aMaxLenForActive;
  }

  public void setMaxLenForActive (final int [] aMaxLenForActive)
  {
    m_aMaxLenForActive = aMaxLenForActive;
  }

  public String [] getAllImages ()
  {
    return m_aAllImages;
  }

  public void setAllImages (final String [] aAllImages)
  {
    m_aAllImages = aAllImages;
  }

  public int [] [] getIntermediateKinds ()
  {
    return m_aIntermediateKinds;
  }

  public void setIntermediateKinds (final int [] [] aIntermediateKinds)
  {
    m_aIntermediateKinds = aIntermediateKinds;
  }

  public int [] [] getIntermediateMatchedPos ()
  {
    return m_aIntermediateMatchedPos;
  }

  public void setIntermediateMatchedPos (final int [] [] aIntermediateMatchedPos)
  {
    m_aIntermediateMatchedPos = aIntermediateMatchedPos;
  }

  public boolean [] getSubString ()
  {
    return m_aSubString;
  }

  public void setSubString (final boolean [] aSubString)
  {
    m_aSubString = aSubString;
  }

  public boolean [] getSubStringAtPos ()
  {
    return m_aSubStringAtPos;
  }

  public void setSubStringAtPos (final boolean [] aSubStringAtPos)
  {
    m_aSubStringAtPos = aSubStringAtPos;
  }

  public Map <String, long []> [] getStatesForPos ()
  {
    return m_aStatesForPos;
  }

  public void setStatesForPos (final Map <String, long []> [] aStatesForPos)
  {
    m_aStatesForPos = aStatesForPos;
  }

  /** @return Whether the shared boiler plate methods were already emitted for this run */
  public boolean isBoilerPlateDumped ()
  {
    return m_bBoilerPlateDumped;
  }

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
