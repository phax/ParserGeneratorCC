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

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pgcc.parser.AbstractNormalProduction;
import com.helger.pgcc.parser.Token;

/**
 * Describes non terminals.
 */

public final class ExpNonTerminal extends Expansion
{
  /** Default constructor. */
  public ExpNonTerminal ()
  {}

  /**
   * The LHS to which the return value of the non-terminal is assigned. In case there is no LHS,
   * then the vector remains empty.
   */
  private final ICommonsList <Token> m_aLhsTokens = new CommonsArrayList <> ();

  /**
   * The name of the non-terminal.
   */
  private String m_sName;

  /**
   * The list of all tokens in the argument list.
   */
  private final ICommonsList <Token> m_aArgumentTokens = new CommonsArrayList <> ();

  private final ICommonsList <Token> m_aParametrizedTypeTokens = new CommonsArrayList <> ();

  /**
   * The production this non-terminal corresponds to.
   */
  private AbstractNormalProduction m_aProd;

  @Override
  public StringBuilder dump (final int nIndent, final Set <? super Expansion> aAlreadyDumped)
  {
    return super.dump (nIndent, aAlreadyDumped).append (' ').append (m_sName);
  }

  /**
   * {@return the lhsTokens}
   */
  @NonNull
  public final Iterable <Token> getLhsTokens ()
  {
    return m_aLhsTokens;
  }

  /**
   * {@return how many tokens the left hand side of the assignment in front of this non-terminal
   * has}
   */
  @Nonnegative
  public final int getLhsTokenCount ()
  {
    return m_aLhsTokens.size ();
  }

  /**
   * One token of the left hand side of the assignment in front of this non-terminal.
   *
   * @param nIndex
   *        The position, from 0.
   * @return The token. Never <code>null</code>.
   */
  @NonNull
  public final Token getLhsTokenAt (final int nIndex)
  {
    return m_aLhsTokens.get (nIndex);
  }

  /**
   * Record the left hand side of the assignment in front of this non-terminal.
   *
   * @param aLhsTokens
   *        The tokens of the left hand side. May not be <code>null</code>.
   */
  public final void setLhsTokens (@NonNull final List <Token> aLhsTokens)
  {
    m_aLhsTokens.setAll (aLhsTokens);
  }

  /**
   * {@return the name of the production this non-terminal refers to}
   */
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * Record which production this non-terminal refers to.
   *
   * @param sName
   *        The production name. May not be <code>null</code>.
   */
  public final void setName (final String sName)
  {
    m_sName = sName;
  }

  /**
   * {@return the tokens of the argument list, as a list the caller may add to}
   */
  @NonNull
  public final List <Token> getMutableArgumentTokens ()
  {
    return m_aArgumentTokens;
  }

  /**
   * {@return the tokens of the argument list of this non-terminal}
   */
  @NonNull
  public final Iterable <Token> getArgumentTokens ()
  {
    return m_aArgumentTokens;
  }

  /**
   * {@return how many tokens the argument list of this non-terminal has}
   */
  @Nonnegative
  public final int getArgumentTokenCount ()
  {
    return m_aArgumentTokens.size ();
  }

  /**
   * One token of the argument list of this non-terminal.
   *
   * @param n
   *        The position, from 0.
   * @return The token. Never <code>null</code>.
   */
  @NonNull
  public final Token getArgumentTokenAt (final int n)
  {
    return m_aArgumentTokens.get (n);
  }

  /**
   * {@return the tokens of the type arguments, as a list the caller may add to}
   */
  @NonNull
  public final List <Token> getMutableParametrizedTypeTokens ()
  {
    return m_aParametrizedTypeTokens;
  }

  /**
   * {@return the tokens of the type arguments of this non-terminal}
   */
  @NonNull
  public final Iterable <Token> getParametrizedTypeTokens ()
  {
    return m_aParametrizedTypeTokens;
  }

  /**
   * {@return the production this non-terminal refers to, or <code>null</code> if the name does not
   * resolve}
   */
  public final AbstractNormalProduction getProd ()
  {
    return m_aProd;
  }

  /**
   * Attach the production this non-terminal refers to, once the grammar has been read and the name
   * could be resolved.
   *
   * @param aProd
   *        The production. May be <code>null</code> if the name does not resolve.
   */
  public final void setProd (final AbstractNormalProduction aProd)
  {
    m_aProd = aProd;
  }
}
