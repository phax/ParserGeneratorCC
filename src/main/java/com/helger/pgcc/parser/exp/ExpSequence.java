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

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.parser.Token;

/**
 * Describes expansions that are sequences of expansion units. (c1 c2 ...)
 */
public final class ExpSequence extends Expansion
{
  /**
   * The list of units in this expansion sequence. Each List component will narrow to Expansion.
   */
  private final List <Expansion> m_aUnits = new ArrayList <> ();

  /**
   * Create an empty sequence.
   */
  public ExpSequence ()
  {}

  /**
   * Create a sequence that starts with a lookahead specification.
   *
   * @param aToken
   *        The token the sequence starts at, for error messages. May not be <code>null</code>.
   * @param aLookahead
   *        The lookahead. May be <code>null</code>.
   */
  public ExpSequence (@NonNull final Token aToken, final ExpLookahead aLookahead)
  {
    setLineNumber (aToken.beginLine);
    setColumnNumber (aToken.beginColumn);
    m_aUnits.add (aLookahead);
  }

  /**
   * {@return the expansions this sequence is made of, in order}
   */
  @NonNull
  public final Iterable <Expansion> getUnits ()
  {
    return m_aUnits;
  }

  /**
   * {@return how many expansions this sequence is made of}
   */
  @Nonnegative
  public final int getUnitCount ()
  {
    return m_aUnits.size ();
  }

  /**
   * One expansion of this sequence.
   *
   * @param nIndex
   *        The position, from 0.
   * @return The expansion. Never <code>null</code>.
   */
  @NonNull
  public final Expansion getUnitAt (final int nIndex)
  {
    return m_aUnits.get (nIndex);
  }

  /**
   * Append an expansion to this sequence.
   *
   * @param aObj
   *        The expansion to append. May not be <code>null</code>.
   */
  public final void addUnit (@NonNull final Expansion aObj)
  {
    ValueEnforcer.notNull (aObj, "Obj");
    m_aUnits.add (aObj);
  }

  /**
   * Insert an expansion into this sequence.
   *
   * @param n
   *        The position to insert at, from 0.
   * @param aObj
   *        The expansion to insert. May not be <code>null</code>.
   */
  public final void addUnit (final int n, @NonNull final Expansion aObj)
  {
    ValueEnforcer.notNull (aObj, "Obj");
    m_aUnits.add (n, aObj);
  }

  /**
   * Replace one expansion of this sequence.
   *
   * @param n
   *        The position to replace, from 0.
   * @param aObj
   *        The new expansion. May not be <code>null</code>.
   */
  public final void setUnit (final int n, @NonNull final Expansion aObj)
  {
    ValueEnforcer.notNull (aObj, "Obj");
    m_aUnits.set (n, aObj);
  }

  @Override
  public StringBuilder dump (final int nIndent, @NonNull final Set <? super Expansion> aAlreadyDumped)
  {
    if (!aAlreadyDumped.add (this))
    {
      return super.dump (0, aAlreadyDumped).insert (0, '[').append (']').insert (0, dumpPrefix (nIndent));
    }

    final StringBuilder aSB = super.dump (nIndent, aAlreadyDumped);
    for (final Expansion next : m_aUnits)
    {
      aSB.append (EOL).append (next.dump (nIndent + 1, aAlreadyDumped));
    }
    return aSB;
  }
}
