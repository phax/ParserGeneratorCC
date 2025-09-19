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
package com.helger.pgcc.utils;

import com.helger.base.compare.IComparable;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Chris Ainsley
 */
public class OptionInfo implements IComparable <OptionInfo>
{
  private final String m_name;
  private final EOptionType m_type;
  private final Comparable <?> m_default;

  public OptionInfo (@Nonnull final String name, @Nonnull final EOptionType type, @Nullable final Comparable <?> default1)
  {
    m_name = name;
    m_type = type;
    m_default = default1;
  }

  @Nonnull
  public String getName ()
  {
    return m_name;
  }

  @Nonnull
  public EOptionType getType ()
  {
    return m_type;
  }

  @Nullable
  public Comparable <?> getDefault ()
  {
    return m_default;
  }

  @Override
  public int compareTo (final OptionInfo o)
  {
    return m_name.compareTo (o.m_name);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final OptionInfo rhs = (OptionInfo) o;
    return m_name.equals (rhs.m_name) && m_type.equals (rhs.m_type) && EqualsHelper.equals (m_default, rhs.m_default);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_name).append (m_type).append (m_default).getHashCode ();
  }
}
