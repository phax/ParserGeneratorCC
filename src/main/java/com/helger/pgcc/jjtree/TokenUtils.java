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
package com.helger.pgcc.jjtree;

import org.jspecify.annotations.Nullable;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCGlobals;

/**
 * Utilities for manipulating Tokens.
 */
public final class TokenUtils
{
  private TokenUtils ()
  {}

  static void print (@NonNull final Token t, @NonNull final JJTreeIO aIo, @Nullable final String sIn, final String sOut)
  {
    Token aTt = t.specialToken;
    if (aTt != null)
    {
      while (aTt.specialToken != null)
        aTt = aTt.specialToken;
      while (aTt != null)
      {
        aIo.print (addUnicodeEscapes (aTt.image));
        aTt = aTt.next;
      }
    }
    String i = t.image;
    if (sIn != null && i.equals (sIn))
    {
      i = sOut;
    }
    aIo.print (addUnicodeEscapes (i));
  }

  static void print (final Token t, final JJTreeIO aIo)
  {
    print (t, aIo, null, null);
  }

  /**
   * Escape what a text file cannot carry, and nothing else.
   * <p>
   * This is not the same function as {@code EOutputLanguage.JAVA.addUnicodeEscapes}, although it
   * looks like it. That one escapes into a Java <em>string literal</em>, where a raw newline is
   * illegal, so it escapes everything outside printable ASCII. This one escapes into grammar
   * <em>source text</em>: JJTree copies the input grammar through to the generated .jj verbatim,
   * and there the newlines and tabs are the layout. Escaping them turns the whole file into one
   * line of \\u000a - which is exactly what happens if the two are merged, as 17 of the golden
   * cases will tell you.
   *
   * @param sStr
   *        The text to escape. May not be <code>null</code>.
   * @return The text with non-printable characters escaped, whitespace left alone. Never
   *         <code>null</code>.
   */
  static String addUnicodeEscapes (@NonNull final String sStr)
  {
    final StringBuilder aRet = new StringBuilder (sStr.length ());
    for (final char ch : sStr.toCharArray ())
    {
      if ((ch < 0x20 || ch > 0x7e) && ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f')
      {
        final String s = "0000" + Integer.toString (ch, 16);
        aRet.append ("\\u").append (s.substring (s.length () - 4, s.length ()));
      }
      else
      {
        aRet.append (ch);
      }
    }
    return aRet.toString ();
  }

  static boolean hasTokens (@NonNull final JJTreeNode n)
  {
    if (n.getLastToken ().next == n.getFirstToken ())
      return false;
    return true;
  }

  /**
   * Turn a string literal as written in the grammar into the characters it stands for. The same
   * function JavaCC uses - both Token classes are an {@link IGrammarLocation}, which is what lets
   * them share it.
   *
   * @param t
   *        Where to report a bad escape.
   * @param sStr
   *        The literal including its quotes. May not be <code>null</code>.
   * @return The characters the literal denotes. Never <code>null</code>.
   */
  static String removeEscapesAndQuotes (final Token t, @NonNull final String sStr)
  {
    return JavaCCGlobals.removeEscapesAndQuotes (t, sStr);
  }


}
