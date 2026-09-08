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

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;

import com.helger.pgcc.parser.Token;

/**
 * Describes expansions of the form "try {...} ...".
 */

public final class ExpTryBlock extends Expansion
{
  /**
   * The expansion contained within the try block.
   */
  private Expansion m_aExp;
  /**
   * The types of each catch block. Each list entry is itself a list which in turn contains tokens
   * as entries.
   */
  private List <List <Token>> m_aTypes;
  /**
   * The exception identifiers of each catch block. Each list entry is a token.
   */
  private List <Token> m_aIds;
  /**
   * The block part of each catch block. Each list entry is itself a list which in turn contains
   * tokens as entries.
   */
  private List <List <Token>> m_aCatchblks;
  /**
   * The block part of the finally block. Each list entry is a token. If there is no finally block,
   * this is null.
   */
  private List <Token> m_aFinallyblk;

  /**
   * The exp.
   *
   * @return The value of m_aExp.
   */
  public Expansion getExp ()
  {
    return m_aExp;
  }

  /**
   * The exp.
   *
   * @param aValue
   *        The new value of m_aExp.
   */
  public void setExp (final Expansion aValue)
  {
    m_aExp = aValue;
  }

  @Override
  public StringBuilder dump (final int nIndent, @NonNull final Set <? super Expansion> aAlreadyDumped)
  {
    final StringBuilder aSB = super.dump (nIndent, aAlreadyDumped);
    if (aAlreadyDumped.add (this))
    {
      aSB.append (EOL).append (m_aExp.dump (nIndent + 1, aAlreadyDumped));
    }
    return aSB;
  }

  /**
   * The types.
   *
   * @return The value of m_aTypes.
   */
  public List <List <Token>> getTypes ()
  {
    return m_aTypes;
  }

  /**
   * The types.
   *
   * @param aValue
   *        The new value of m_aTypes.
   */
  public void setTypes (final List <List <Token>> aValue)
  {
    m_aTypes = aValue;
  }

  /**
   * The ids.
   *
   * @return The value of m_aIds.
   */
  public List <Token> getIds ()
  {
    return m_aIds;
  }

  /**
   * The ids.
   *
   * @param aValue
   *        The new value of m_aIds.
   */
  public void setIds (final List <Token> aValue)
  {
    m_aIds = aValue;
  }

  /**
   * The catchblks.
   *
   * @return The value of m_aCatchblks.
   */
  public List <List <Token>> getCatchblks ()
  {
    return m_aCatchblks;
  }

  /**
   * The catchblks.
   *
   * @param aValue
   *        The new value of m_aCatchblks.
   */
  public void setCatchblks (final List <List <Token>> aValue)
  {
    m_aCatchblks = aValue;
  }

  /**
   * The finallyblk.
   *
   * @return The value of m_aFinallyblk.
   */
  public List <Token> getFinallyblk ()
  {
    return m_aFinallyblk;
  }

  /**
   * The finallyblk.
   *
   * @param aValue
   *        The new value of m_aFinallyblk.
   */
  public void setFinallyblk (final List <Token> aValue)
  {
    m_aFinallyblk = aValue;
  }
}
