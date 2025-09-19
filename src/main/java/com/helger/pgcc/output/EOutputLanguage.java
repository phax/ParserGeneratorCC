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
package com.helger.pgcc.output;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Various constants relating to possible values for certain options
 */

public enum EOutputLanguage implements IHasID <String>
{
  JAVA ("java")
  {
    @Override
    public String getTypeLong ()
    {
      return "long";
    }

    @Override
    public String getLongValueSuffix ()
    {
      return "L";
    }

    @Override
    public String getTypeBoolean ()
    {
      return "boolean";
    }
  },
  CPP ("c++")
  {
    @Override
    public String getTypeLong ()
    {
      return "unsigned long long";
    }

    @Override
    public String getLongValueSuffix ()
    {
      return "ULL";
    }

    @Override
    public String getTypeBoolean ()
    {
      return "bool";
    }
  };

  private final String m_sID;

  private EOutputLanguage (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The native data type for "long" values.
   */
  @Nonnull
  @Nonempty
  public abstract String getTypeLong ();

  /**
   * @return The value suffix to be used for long values.
   * @see #getTypeLong()
   */
  @Nonnull
  @Nonempty
  protected abstract String getLongValueSuffix ();

  @Nonnull
  public String getLongHex (final long n)
  {
    return "0x" + Long.toHexString (n) + getLongValueSuffix ();
  }

  @Nonnull
  public String getLongPlain (final long n)
  {
    return "0x" + Long.toString (n) + getLongValueSuffix ();
  }

  /**
   * @return The native data type for "boolean" values.
   */
  @Nonnull
  @Nonempty
  public abstract String getTypeBoolean ();

  public boolean isJava ()
  {
    return this == JAVA;
  }

  public boolean hasStaticsFile ()
  {
    return this == CPP;
  }

  public boolean hasIncludeFile ()
  {
    return this == CPP;
  }

  @Nullable
  public static EOutputLanguage getFromIDCaseInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EOutputLanguage.class, sID);
  }
}
