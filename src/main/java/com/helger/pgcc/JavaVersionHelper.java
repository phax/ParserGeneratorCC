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
package com.helger.pgcc;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringParser;
import com.helger.base.system.EJavaVersion;

/**
 * Helper to translate the textual <code>JDK_VERSION</code> option value into an
 * {@link EJavaVersion}.
 * <p>
 * {@link EJavaVersion} identifies its constants by the class file version (52.0 for Java 8), so
 * neither <code>getFromVersionNumber</code> nor <code>getFromMajorAndMinor</code> can be used to
 * resolve a language version like <code>"1.8"</code> or <code>"17"</code>.
 *
 * @author Philip Helger
 */
public final class JavaVersionHelper
{
  /**
   * Major language version to enum value. Derived from the enum constant names, so that a new JDK
   * added to ph-base is picked up without a change here.
   */
  private static final Map <Integer, EJavaVersion> MAJOR_TO_VERSION;

  static
  {
    final Map <Integer, EJavaVersion> aMap = new HashMap <> ();
    for (final EJavaVersion eVersion : EJavaVersion.values ())
    {
      // "JDK_1_8" -> 8, "JDK_17" -> 17, "UNKNOWN" -> not a version
      final String sName = eVersion.name ();
      if (!sName.startsWith ("JDK_"))
        continue;

      String sMajor = sName.substring ("JDK_".length ());
      if (sMajor.startsWith ("1_"))
        sMajor = sMajor.substring ("1_".length ());

      final int nMajor = StringParser.parseInt (sMajor, -1);
      if (nMajor > 0)
        aMap.put (Integer.valueOf (nMajor), eVersion);
    }
    MAJOR_TO_VERSION = Map.copyOf (aMap);
  }

  private JavaVersionHelper ()
  {}

  /**
   * Parse a <code>JDK_VERSION</code> option value. Accepted are the old style <code>"1.x"</code>
   * spellings as well as plain major versions, so <code>"1.8"</code> and <code>"8"</code> both
   * denote Java 8, and <code>"1.9"</code> and <code>"9"</code> both denote Java 9.
   *
   * @param sVersion
   *        The version string to parse. May be <code>null</code>.
   * @return <code>null</code> if the version is unknown.
   */
  @Nullable
  public static EJavaVersion getFromStringOrNull (@Nullable final String sVersion)
  {
    if (sVersion == null)
      return null;

    final String sTrimmed = sVersion.trim ();
    if (sTrimmed.length () == 0)
      return null;

    // "1.8" is Java 8 and "1.9" is Java 9 - in both cases the part after the dot is the major
    // language version
    final int nDot = sTrimmed.indexOf ('.');
    final String sMajor = nDot < 0 ? sTrimmed
                                   : sTrimmed.substring (0, nDot).equals ("1") ? sTrimmed.substring (nDot + 1)
                                                                               : sTrimmed.substring (0, nDot);

    final int nMajor = StringParser.parseInt (sMajor, -1);
    if (nMajor <= 0)
      return null;

    return MAJOR_TO_VERSION.get (Integer.valueOf (nMajor));
  }

  /**
   * @param eVersion
   *        The version to check. May not be <code>null</code>.
   * @param eOther
   *        The version to compare with. May not be <code>null</code>.
   * @return <code>true</code> if <code>eVersion</code> is strictly older than <code>eOther</code>.
   *         {@link EJavaVersion} only offers <code>isOlderOrEqualsThan</code>.
   */
  public static boolean isOlderThan (@NonNull final EJavaVersion eVersion, @NonNull final EJavaVersion eOther)
  {
    return eVersion.ordinal () < eOther.ordinal ();
  }
}
