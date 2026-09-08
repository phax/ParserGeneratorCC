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

import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.Token;

/**
 * Describes one-or-more regular expressions (&lt;foo+&gt;).
 */
public final class ExpRRepetitionRange extends AbstractExpRegularExpression
{
  /**
   * The regular expression which is repeated one or more times.
   */
  private final AbstractExpRegularExpression m_aRegexpr;
  private int m_nMin = 0;
  private int m_nMax = -1;
  private final boolean m_bHasMax;

  public ExpRRepetitionRange (@NonNull final Token t,
                              final int nR1,
                              final int nR2,
                              final boolean bHasMax,
                              final AbstractExpRegularExpression r)
  {
    setLineNumber (t.beginLine);
    setColumnNumber (t.beginColumn);
    m_nMin = nR1;
    m_nMax = nR2;
    m_bHasMax = bHasMax;
    m_aRegexpr = r;
  }

  @NonNull
  public final AbstractExpRegularExpression getRegExpr ()
  {
    return m_aRegexpr;
  }

  public final int getMin ()
  {
    return m_nMin;
  }

  public final boolean hasMax ()
  {
    return m_bHasMax;
  }

  public final int getMax ()
  {
    return m_nMax;
  }

  @Override
  public Nfa generateNfa (final boolean bIgnoreCase)
  {
    final List <AbstractExpRegularExpression> aUnits = new ArrayList <> ();
    ExpRSequence aSeq;
    int i;

    for (i = 0; i < m_nMin; i++)
    {
      aUnits.add (m_aRegexpr);
    }

    // Unlimited
    if (m_bHasMax && m_nMax == -1)
    {
      aUnits.add (new ExpRZeroOrMore (m_aRegexpr));
    }

    while (i++ < m_nMax)
    {
      aUnits.add (new ExpRZeroOrOne (m_aRegexpr));
    }
    aSeq = new ExpRSequence (aUnits);
    return aSeq.generateNfa (bIgnoreCase);
  }
}
