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

import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;
import com.helger.pgcc.parser.Token;

import jakarta.annotation.Nonnull;

/**
 * Describes one-or-more regular expressions (&lt;foo+&gt;).
 */
public class ExpROneOrMore extends AbstractExpRegularExpression
{
  /**
   * The regular expression which is repeated one or more times.
   */
  private final AbstractExpRegularExpression m_regexpr;

  public ExpROneOrMore (final AbstractExpRegularExpression re)
  {
    m_regexpr = re;
  }

  public ExpROneOrMore (final Token t, final AbstractExpRegularExpression re)
  {
    this (re);
    setLine (t.beginLine);
    setColumn (t.beginColumn);
  }

  @Nonnull
  public final AbstractExpRegularExpression getRegExpr ()
  {
    return m_regexpr;
  }

  @Override
  public Nfa generateNfa (final boolean ignoreCase)
  {
    final Nfa retVal = new Nfa ();
    final NfaState startState = retVal.start ();
    final NfaState finalState = retVal.end ();

    final Nfa temp = m_regexpr.generateNfa (ignoreCase);

    startState.addMove (temp.start ());
    temp.end ().addMove (temp.start ());
    temp.end ().addMove (finalState);

    return retVal;
  }
}
