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
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.Token;
import com.helger.pgcc.parser.TokenProduction;

/**
 * Describes regular expressions.
 */
public abstract sealed class AbstractExpRegularExpression extends Expansion permits
                                                          ExpRCharacterList,
                                                          ExpRChoice,
                                                          ExpREndOfFile,
                                                          ExpRJustName,
                                                          ExpROneOrMore,
                                                          ExpRRepetitionRange,
                                                          ExpRSequence,
                                                          ExpRStringLiteral,
                                                          ExpRZeroOrMore,
                                                          ExpRZeroOrOne
{
  /**
   * The label of the regular expression (if any). If no label is present, this is set to "".
   */
  private String m_sLabel = "";

  /**
   * The ordinal value assigned to the regular expression. It is used for internal processing and
   * passing information between the parser and the lexical analyzer.
   */
  private int m_nOrdinal;

  /**
   * The LHS to which the token value of the regular expression is assigned. In case there is no
   * LHS, then the list remains empty.
   */
  private final List <Token> m_aLhsTokens = new ArrayList <> ();

  /**
   * We now allow qualified access to token members. Store it here.
   */
  private Token m_aRhsToken;

  /**
   * This flag is set if the regular expression has a label prefixed with the # symbol - this
   * indicates that the purpose of the regular expression is solely for defining other regular
   * expressions.
   */
  public boolean m_bPrivateRexp = false;

  /**
   * If this is a top-level regular expression (nested directly within a TokenProduction), then this
   * field point to that TokenProduction object.
   */
  public TokenProduction m_aTpContext;
  /**
   * The following variable is used to maintain state information for the loop determination
   * algorithm: It is initialized to 0, and set to -1 if this node has been visited in a pre-order
   * walk, and then it is set to 1 if the pre-order walk of the whole graph from this node has been
   * traversed. i.e., -1 indicates partially processed, and 1 indicates fully processed.
   */
  private int m_nWalkStatus = 0;

  /** Default constructor. */
  protected AbstractExpRegularExpression ()
  {}

  /**
   * The name in angle brackets, if the grammar gave this one.
   *
   * @return The name this regular expression was given in the grammar, empty if it is anonymous.
   *         Never <code>null</code>.
   */
  public final String getLabel ()
  {
    return m_sLabel;
  }

  /**
   * Whether this regular expression has a name at all.
   *
   * @return <code>true</code> if this regular expression was named in the grammar.
   */
  public final boolean hasLabel ()
  {
    return StringHelper.isNotEmpty (m_sLabel);
  }

  /**
   * Name this regular expression.
   *
   * @param s
   *        The name to give this regular expression.
   */
  public final void setLabel (final String s)
  {
    m_sLabel = s;
  }

  /**
   * The token kind, which is the number the token manager reports on a match.
   *
   * @return The token kind this regular expression matches, as assigned by Semanticize.
   */
  public final int getOrdinal ()
  {
    return m_nOrdinal;
  }

  /**
   * Assign the token kind. Semanticize does this once every regular expression is known.
   *
   * @param n
   *        The token kind to assign.
   */
  public final void setOrdinal (final int n)
  {
    m_nOrdinal = n;
  }

  /**
   * The left hand side of an assignment in a token production.
   *
   * @return The variables this regular expression assigns to when it matches. Never
   *         <code>null</code>.
   */
  @NonNull
  public final List <Token> getLhsTokens ()
  {
    return m_aLhsTokens;
  }

  /**
   * The variables this regular expression assigns to when it matches.
   *
   * @param aLhsTokens
   *        the lhsTokens to set
   */
  public final void setLhsTokens (@NonNull final List <Token> aLhsTokens)
  {
    m_aLhsTokens.clear ();
    m_aLhsTokens.addAll (aLhsTokens);
  }

  /**
   * The right hand side of an assignment in a token production.
   *
   * @return The token that names what is assigned, or <code>null</code> if there is none.
   */
  @Nullable
  public final Token getRhsToken ()
  {
    return m_aRhsToken;
  }

  /**
   * The right hand side of an assignment in a token production.
   *
   * @param aRhsToken
   *        The token that names what is assigned. May be <code>null</code>.
   */
  public final void setRhsToken (@Nullable final Token aRhsToken)
  {
    m_aRhsToken = aRhsToken;
  }

  /**
   * Where the loop detection walk has got to with this node.
   *
   * @return How far the loop detection walk has got with this node: 0 untouched, -1 partially
   *         processed, 1 fully processed.
   */
  public final int getWalkStatus ()
  {
    return m_nWalkStatus;
  }

  /**
   * Record where the loop detection walk has got to.
   *
   * @param n
   *        The walk state to record. See {@link #getWalkStatus()}.
   */
  public final void setWalkStatus (final int n)
  {
    m_nWalkStatus = n;
  }

  /**
   * Build the NFA fragment that matches this regular expression.
   *
   * @param ignoreCase
   *        <code>true</code> if the enclosing token production is IGNORE_CASE.
   * @return the start and end state of the fragment. Never <code>null</code>.
   */
  public abstract Nfa generateNfa (boolean ignoreCase);

  /**
   * Whether this is the "match anything" expression.
   *
   * @return <code>true</code> if this matches every single character, which the token manager can
   *         special case.
   */
  public boolean canMatchAnyChar ()
  {
    return false;
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public StringBuilder dump (final int nIndent, @NonNull final Set <? super Expansion> aAlreadyDumped)
  {
    aAlreadyDumped.add (this);
    final StringBuilder aSB = super.dump (nIndent, aAlreadyDumped);
    aSB.append (' ').append (m_sLabel);
    return aSB;
  }
}
