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
package com.helger.pgcc;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public enum EJDKVersion
{
  JDK_1_1 (1),
  JDK_1_2 (2),
  JDK_1_3 (3),
  JDK_1_4 (4),
  JDK_1_5 (5),
  JDK_1_6 (6),
  JDK_1_7 (7),
  JDK_1_8 (8),
  JDK_9 (9),
  JDK_10 (10),
  JDK_11 (11),
  JDK_12 (12),
  JDK_13 (13),
  JDK_14 (14);

  public static final EJDKVersion DEFAULT = JDK_1_5;

  private int m_nMajor;

  private EJDKVersion (final int nMajor)
  {
    m_nMajor = nMajor;
  }

  public boolean isNewerOrEqualsThan (@Nonnull final EJDKVersion aOther)
  {
    return m_nMajor >= aOther.m_nMajor;
  }

  public boolean isOlderThan (@Nonnull final EJDKVersion aOther)
  {
    return m_nMajor < aOther.m_nMajor;
  }

  @Nullable
  public static EJDKVersion getFromStringOrNull (final String sVersion)
  {
    if ("1.1".equals (sVersion))
      return EJDKVersion.JDK_1_1;
    if ("1.2".equals (sVersion))
      return EJDKVersion.JDK_1_2;
    if ("1.3".equals (sVersion))
      return EJDKVersion.JDK_1_3;
    if ("1.4".equals (sVersion))
      return EJDKVersion.JDK_1_4;
    if ("1.5".equals (sVersion))
      return EJDKVersion.JDK_1_5;
    if ("1.6".equals (sVersion))
      return EJDKVersion.JDK_1_6;
    if ("1.7".equals (sVersion))
      return EJDKVersion.JDK_1_7;
    if ("1.8".equals (sVersion))
      return EJDKVersion.JDK_1_8;
    if ("1.9".equals (sVersion) || "9".equals (sVersion))
      return EJDKVersion.JDK_9;
    if ("1.10".equals (sVersion) || "10".equals (sVersion))
      return EJDKVersion.JDK_10;
    if ("1.11".equals (sVersion) || "11".equals (sVersion))
      return EJDKVersion.JDK_11;
    if ("1.12".equals (sVersion) || "12".equals (sVersion))
      return EJDKVersion.JDK_12;
    if ("1.13".equals (sVersion) || "13".equals (sVersion))
      return EJDKVersion.JDK_13;
    if ("1.14".equals (sVersion) || "14".equals (sVersion))
      return EJDKVersion.JDK_14;
    return null;
  }
}
