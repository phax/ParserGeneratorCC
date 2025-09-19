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
 * Describes expansions where one of many choices is taken (c1|c2|...).
 */
public class ExpChoice extends Expansion
{
  /**
   * The list of choices of this expansion unit. Each List component will narrow
   * to ExpansionUnit.
   */
  private final List <Expansion> m_choices = new ArrayList <> ();

  public ExpChoice ()
  {}

  public ExpChoice (final Token token)
  {
    setLine (token.beginLine);
    setColumn (token.beginColumn);
  }

  public ExpChoice (final Expansion expansion)
  {
    setLine (expansion.getLine ());
    setColumn (expansion.getColumn ());
    m_choices.add (expansion);
  }

  /**
   * @return the choices
   */
  @Nonnull
  public final Iterable <Expansion> getChoices ()
  {
    return m_choices;
  }

  @Nonnegative
  public final int getChoiceCount ()
  {
    return m_choices.size ();
  }

  @Nonnull
  public final Expansion getChoiceAt (final int nIndex)
  {
    return m_choices.get (nIndex);
  }

  public final void addChoice (@Nonnull final Expansion a)
  {
    ValueEnforcer.notNull (a, "Expansion");
    m_choices.add (a);
  }

  @Override
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    final StringBuilder sb = super.dump (indent, alreadyDumped);
    if (alreadyDumped.add (this))
      for (final Expansion next : getChoices ())
        sb.append (EOL).append (next.dump (indent + 1, alreadyDumped));
    return sb;
  }
}
