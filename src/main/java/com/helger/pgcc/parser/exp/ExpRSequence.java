/*
 * Copyright 2017-2025 Philip Helger, pgcc@helger.com
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
package com.helger.pgcc.parser.exp;

import java.util.ArrayList;
import java.util.List;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;

import jakarta.annotation.Nonnull;

/**
 * Describes regular expressions which are sequences of other regular
 * expressions.
 */

public class ExpRSequence extends AbstractExpRegularExpression
{

  /**
   * The list of units in this regular expression sequence. Each list component
   * will narrow to RegularExpression.
   */
  private final List <AbstractExpRegularExpression> m_units;

  public ExpRSequence ()
  {
    m_units = new ArrayList <> ();
  }

  ExpRSequence (final List <AbstractExpRegularExpression> seq)
  {
    setOrdinal (Integer.MAX_VALUE);
    m_units = seq;
  }

  @Nonnull
  public final List <AbstractExpRegularExpression> getUnits ()
  {
    return m_units;
  }

  public final void addUnit (final AbstractExpRegularExpression ex)
  {
    ValueEnforcer.notNull (ex, "RegEx");
    m_units.add (ex);
  }

  @Override
  public Nfa generateNfa (final boolean ignoreCase)
  {
    if (m_units.size () == 1)
      return m_units.get (0).generateNfa (ignoreCase);

    final Nfa retVal = new Nfa ();
    final NfaState startState = retVal.start ();
    final NfaState finalState = retVal.end ();
    Nfa temp1;
    Nfa temp2 = null;

    AbstractExpRegularExpression curRE;

    curRE = m_units.get (0);
    temp1 = curRE.generateNfa (ignoreCase);
    startState.addMove (temp1.start ());

    for (int i = 1; i < m_units.size (); i++)
    {
      curRE = m_units.get (i);

      temp2 = curRE.generateNfa (ignoreCase);
      temp1.end ().addMove (temp2.start ());
      temp1 = temp2;
    }

    temp2.end ().addMove (finalState);

    return retVal;
  }
}
