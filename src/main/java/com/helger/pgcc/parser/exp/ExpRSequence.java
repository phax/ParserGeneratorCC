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
package com.helger.pgcc.parser.exp;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;

/**
 * Describes regular expressions which are sequences of other regular expressions.
 */

public final class ExpRSequence extends AbstractExpRegularExpression
{

  /**
   * The list of units in this regular expression sequence. Each list component will narrow to
   * RegularExpression.
   */
  private final List <AbstractExpRegularExpression> m_aUnits;

  public ExpRSequence ()
  {
    m_aUnits = new ArrayList <> ();
  }

  ExpRSequence (final List <AbstractExpRegularExpression> aSeq)
  {
    setOrdinal (Integer.MAX_VALUE);
    m_aUnits = aSeq;
  }

  @NonNull
  public final List <AbstractExpRegularExpression> getUnits ()
  {
    return m_aUnits;
  }

  public final void addUnit (@NonNull final AbstractExpRegularExpression aEx)
  {
    ValueEnforcer.notNull (aEx, "RegEx");
    m_aUnits.add (aEx);
  }

  @Override
  public Nfa generateNfa (final boolean bIgnoreCase)
  {
    if (m_aUnits.size () == 1)
      return m_aUnits.get (0).generateNfa (bIgnoreCase);

    final Nfa aRetVal = new Nfa ();
    final NfaState aStartState = aRetVal.start ();
    final NfaState aFinalState = aRetVal.end ();
    Nfa aTemp1;
    Nfa aTemp2 = null;

    AbstractExpRegularExpression aCurRE;

    aCurRE = m_aUnits.get (0);
    aTemp1 = aCurRE.generateNfa (bIgnoreCase);
    aStartState.addMove (aTemp1.start ());

    for (int i = 1; i < m_aUnits.size (); i++)
    {
      aCurRE = m_aUnits.get (i);

      aTemp2 = aCurRE.generateNfa (bIgnoreCase);
      aTemp1.end ().addMove (aTemp2.start ());
      aTemp1 = aTemp2;
    }

    aTemp2.end ().addMove (aFinalState);

    return aRetVal;
  }
}
