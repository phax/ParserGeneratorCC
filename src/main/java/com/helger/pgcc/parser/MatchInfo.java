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

import com.helger.pgcc.context.LookaheadState;

/**
 * Describes a match, within a given lookahead. The depth of that lookahead comes from
 * {@link LookaheadState}, which is per generator run.
 */
/**
 * One possible sequence of token kinds, of length up to {@link LookaheadState#getLimit()}. The
 * FIRST and FOLLOW computation in {@code LookaheadWalk} extends these, and two expansions are
 * ambiguous at length k exactly when their sets share one.
 * <p>
 * Not a record, although it looks like one. The fill level is assigned from outside in four places
 * - {@code LookaheadCalc} resets it to 0 to reuse an instance, {@code LookaheadWalk} advances it
 * after writing a kind - so this is mutable state rather than a value, and a record's components
 * are final. Making it one would mean allocating a new instance per token appended, which is the
 * opposite of cheaper. A record would not give direct field access either: components are exposed
 * as accessor methods, exactly like the getters here.
 * <p>
 * Performance does not argue for it in any case. Everything that touches this type accounts for
 * under one percent of a generator run.
 */
public class MatchInfo
{
  private int [] m_aMatch = new int [LookaheadState.current ().getLimit ()];
  private int m_nFirstFreeLoc;

  /**
   * The token kinds matched so far, in order. Only the first {@link #getFirstFreeLoc()} entries
   * are meaningful.
   *
   * @return The value of m_aMatch.
   */
  public int [] getMatch ()
  {
    return m_aMatch;
  }

  /**
   * The token kinds matched so far, in order. Only the first {@link #getFirstFreeLoc()} entries
   * are meaningful.
   *
   * @param aValue
   *        The new value of m_aMatch.
   */
  public void setMatch (final int [] aValue)
  {
    m_aMatch = aValue;
  }

  /**
   * How many entries of the match array are in use, which is also where the next kind goes.
   *
   * @return The value of m_nFirstFreeLoc.
   */
  public int getFirstFreeLoc ()
  {
    return m_nFirstFreeLoc;
  }

  /**
   * How many entries of the match array are in use, which is also where the next kind goes.
   *
   * @param aValue
   *        The new value of m_nFirstFreeLoc.
   */
  public void setFirstFreeLoc (final int aValue)
  {
    m_nFirstFreeLoc = aValue;
  }
}
