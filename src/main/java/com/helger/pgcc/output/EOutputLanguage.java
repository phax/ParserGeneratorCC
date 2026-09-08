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
package com.helger.pgcc.output;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

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

    @Override
    public String getFileExtension ()
    {
      return ".java";
    }

    @Override
    public String addUnicodeEscapes (@NonNull final String sStr)
    {
      final StringBuilder aRetVal = new StringBuilder (sStr.length () * 2);
      for (final char ch : sStr.toCharArray ())
        if (ch < 0x20 || ch > 0x7e)
        {
          final String s = "0000" + Integer.toString (ch, 16);
          aRetVal.append ("\\u").append (s.substring (s.length () - 4));
        }
        else
          aRetVal.append (ch);
      return aRetVal.toString ();
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

    @Override
    public String getFileExtension ()
    {
      return ".cc";
    }

    @Override
    public String addUnicodeEscapes (final String sStr)
    {
      // C++ source is written as it is
      return sStr;
    }
  };

  private final String m_sID;

  private EOutputLanguage (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The native data type for "long" values.
   */
  @NonNull
  @Nonempty
  public abstract String getTypeLong ();

  /**
   * @return The value suffix to be used for long values.
   * @see #getTypeLong()
   */
  @NonNull
  @Nonempty
  protected abstract String getLongValueSuffix ();

  @NonNull
  public String getLongHex (final long n)
  {
    return "0x" + Long.toHexString (n) + getLongValueSuffix ();
  }

  @NonNull
  public String getLongPlain (final long n)
  {
    return "0x" + Long.toString (n) + getLongValueSuffix ();
  }

  /**
   * @return The native data type for "boolean" values.
   */
  @NonNull
  @Nonempty
  public abstract String getTypeBoolean ();

  /**
   * @return The file extension for a generated source file in this language, dot included. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public abstract String getFileExtension ();

  /**
   * Escape everything that is not printable ASCII, the way a string literal in this language wants
   * it.
   *
   * @param sStr
   *        The string to escape. May not be <code>null</code>.
   * @return The escaped string. Never <code>null</code>.
   */
  @NonNull
  public abstract String addUnicodeEscapes (@NonNull String sStr);

  public boolean isJava ()
  {
    return this == JAVA;
  }

  /**
   * @param sA
   *        First expression. May not be <code>null</code>.
   * @param sB
   *        Second expression. May not be <code>null</code>.
   * @return An expression for the larger of the two, as this language spells it. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public String getMax (@NonNull final String sA, @NonNull final String sB)
  {
    return (this == JAVA ? "Math.max(" : "MAX(") + sA + ", " + sB + ")";
  }

  /**
   * @param sA
   *        First expression. May not be <code>null</code>.
   * @param sB
   *        Second expression. May not be <code>null</code>.
   * @return An expression for the smaller of the two, as this language spells it. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public String getMin (@NonNull final String sA, @NonNull final String sB)
  {
    return (this == JAVA ? "Math.min(" : "MIN(") + sA + ", " + sB + ")";
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
