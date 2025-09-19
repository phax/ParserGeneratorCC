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
import java.util.Set;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.parser.Token;

import jakarta.annotation.Nonnull;

/**
 * Describes expansions that are sequences of expansion units. (c1 c2 ...)
 */
public class ExpSequence extends Expansion
{
  /**
   * The list of units in this expansion sequence. Each List component will
   * narrow to Expansion.
   */
  private final List <Expansion> m_units = new ArrayList <> ();

  public ExpSequence ()
  {}

  public ExpSequence (final Token token, final ExpLookahead lookahead)
  {
    setLine (token.beginLine);
    setColumn (token.beginColumn);
    m_units.add (lookahead);
  }

  @Nonnull
  public final Iterable <Expansion> getUnits ()
  {
    return m_units;
  }

  @Nonnegative
  public final int getUnitCount ()
  {
    return m_units.size ();
  }

  @Nonnull
  public final Expansion getUnitAt (final int nIndex)
  {
    return m_units.get (nIndex);
  }

  public final void addUnit (final Expansion aObj)
  {
    ValueEnforcer.notNull (aObj, "Obj");
    m_units.add (aObj);
  }

  public final void addUnit (final int n, final Expansion aObj)
  {
    ValueEnforcer.notNull (aObj, "Obj");
    m_units.add (n, aObj);
  }

  public final void setUnit (final int n, final Expansion aObj)
  {
    ValueEnforcer.notNull (aObj, "Obj");
    m_units.set (n, aObj);
  }

  @Override
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    if (!alreadyDumped.add (this))
    {
      return super.dump (0, alreadyDumped).insert (0, '[').append (']').insert (0, dumpPrefix (indent));
    }

    final StringBuilder sb = super.dump (indent, alreadyDumped);
    for (final Expansion next : m_units)
    {
      sb.append (EOL).append (next.dump (indent + 1, alreadyDumped));
    }
    return sb;
  }
}
