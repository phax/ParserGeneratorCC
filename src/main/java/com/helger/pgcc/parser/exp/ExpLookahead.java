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

import java.util.Set;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pgcc.parser.Token;

import jakarta.annotation.Nonnull;

/**
 * Describes lookahead rule for a particular expansion or expansion sequence
 * (See Sequence.java). In case this describes the lookahead rule for a single
 * expansion unit, then a sequence is created with this node as the first
 * element, and the expansion unit as the second and last element.
 */
public class ExpLookahead extends Expansion
{
  /**
   * Contains the list of tokens that make up the semantic lookahead if any. If
   * this node represents a different kind of lookahead (other than semantic
   * lookahead), then this list contains nothing. If this list contains
   * something, then it is the boolean expression that forms the semantic
   * lookahead. In this case, the following fields "amount" and "la_expansion"
   * are ignored.
   */
  private final ICommonsList <Token> m_action_tokens = new CommonsArrayList <> ();

  /**
   * The lookahead amount. Its default value essentially gives us infinite
   * lookahead.
   */
  private int m_amount = Integer.MAX_VALUE;

  /**
   * The expansion used to determine whether or not to choose the corresponding
   * parse option. This expansion is parsed upto "amount" tokens of lookahead or
   * until a complete match for it is found. Usually, this is the same as the
   * expansion to be parsed.
   */
  private Expansion m_la_expansion;

  /**
   * Is set to true if this is an explicit lookahead specification.
   */
  private boolean m_bIsExplicit;

  /**
   * @return the action_tokens
   */
  @Nonnull
  public final ICommonsList <Token> getActionTokens ()
  {
    return m_action_tokens;
  }

  /**
   * @return the amount
   */
  public final int getAmount ()
  {
    return m_amount;
  }

  /**
   * @param amount
   *        the amount to set
   */
  public final void setAmount (final int amount)
  {
    m_amount = amount;
  }

  /**
   * @return the la_expansion
   */
  public final Expansion getLaExpansion ()
  {
    return m_la_expansion;
  }

  /**
   * @param la_expansion
   *        the la_expansion to set
   */
  public final void setLaExpansion (final Expansion la_expansion)
  {
    m_la_expansion = la_expansion;
  }

  /**
   * @return the isExplicit
   */
  public final boolean isExplicit ()
  {
    return m_bIsExplicit;
  }

  /**
   * @param isExplicit
   *        the isExplicit to set
   */
  public final void setExplicit (final boolean isExplicit)
  {
    m_bIsExplicit = isExplicit;
  }

  @Override
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    final StringBuilder sb = super.dump (indent, alreadyDumped).append (m_bIsExplicit ? " explicit" : " implicit");
    if (alreadyDumped.add (this))
    {
      sb.append (EOL).append (m_la_expansion.dump (indent + 1, alreadyDumped));
    }
    return sb;
  }
}
