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

import java.util.List;
import java.util.Set;

import com.helger.annotation.Nonnegative;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.Token;

import jakarta.annotation.Nonnull;

/**
 * Describes non terminals.
 */

public class ExpNonTerminal extends Expansion
{
  /**
   * The LHS to which the return value of the non-terminal is assigned. In case
   * there is no LHS, then the vector remains empty.
   */
  private final ICommonsList <Token> m_lhsTokens = new CommonsArrayList <> ();

  /**
   * The name of the non-terminal.
   */
  private String m_name;

  /**
   * The list of all tokens in the argument list.
   */
  private final ICommonsList <Token> m_argumentTokens = new CommonsArrayList <> ();

  private final ICommonsList <Token> m_parametrizedTypeTokens = new CommonsArrayList <> ();

  /**
   * The production this non-terminal corresponds to.
   */
  private NormalProduction m_prod;

  @Override
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    return super.dump (indent, alreadyDumped).append (' ').append (m_name);
  }

  /**
   * @return the lhsTokens
   */
  @Nonnull
  public final Iterable <Token> getLhsTokens ()
  {
    return m_lhsTokens;
  }

  @Nonnegative
  public final int getLhsTokenCount ()
  {
    return m_lhsTokens.size ();
  }

  @Nonnull
  public final Token getLhsTokenAt (final int nIndex)
  {
    return m_lhsTokens.get (nIndex);
  }

  /**
   * @param lhsTokens
   *        the lhsTokens to set
   */
  public final void setLhsTokens (@Nonnull final List <Token> lhsTokens)
  {
    m_lhsTokens.setAll (lhsTokens);
  }

  /**
   * @return the name
   */
  public final String getName ()
  {
    return m_name;
  }

  /**
   * @param name
   *        the name to set
   */
  public final void setName (final String name)
  {
    m_name = name;
  }

  @Nonnull
  public final List <Token> getMutableArgumentTokens ()
  {
    return m_argumentTokens;
  }

  /**
   * @return the argument_tokens
   */
  @Nonnull
  public final Iterable <Token> getArgumentTokens ()
  {
    return m_argumentTokens;
  }

  @Nonnegative
  public final int getArgumentTokenCount ()
  {
    return m_argumentTokens.size ();
  }

  @Nonnull
  public final Token getArgumentTokenAt (final int n)
  {
    return m_argumentTokens.get (n);
  }

  @Nonnull
  public final List <Token> getMutableParametrizedTypeTokens ()
  {
    return m_parametrizedTypeTokens;
  }

  /**
   * @return the argument_tokens
   */
  @Nonnull
  public final Iterable <Token> getParametrizedTypeTokens ()
  {
    return m_parametrizedTypeTokens;
  }

  /**
   * @return the prod
   */
  public final NormalProduction getProd ()
  {
    return m_prod;
  }

  /**
   * @param prod
   *        the prod to set
   */
  public final void setProd (final NormalProduction prod)
  {
    m_prod = prod;
  }
}
