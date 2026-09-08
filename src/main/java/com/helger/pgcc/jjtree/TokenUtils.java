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

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.JavaCCErrors;

/**
 * Utilities for manipulating Tokens.
 */
public final class TokenUtils
{
  private TokenUtils ()
  {}

  static void print (final Token t, final JJTreeIO aIo, final String sIn, final String sOut)
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

  static String addUnicodeEscapes (final String sStr)
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

  static String remove_escapes_and_quotes (final Token t, final String sStr)
  {
    String sRetval = "";
    int nIndex = 1;
    while (nIndex < sStr.length () - 1)
    {
      if (sStr.charAt (nIndex) != '\\')
      {
        sRetval += sStr.charAt (nIndex);
        nIndex++;
        continue;
      }
      nIndex++;
      char cCh = sStr.charAt (nIndex);
      if (cCh == 'b')
      {
        sRetval += '\b';
        nIndex++;
        continue;
      }
      if (cCh == 't')
      {
        sRetval += '\t';
        nIndex++;
        continue;
      }
      if (cCh == 'n')
      {
        sRetval += '\n';
        nIndex++;
        continue;
      }
      if (cCh == 'f')
      {
        sRetval += '\f';
        nIndex++;
        continue;
      }
      if (cCh == 'r')
      {
        sRetval += '\r';
        nIndex++;
        continue;
      }
      if (cCh == '"')
      {
        sRetval += '\"';
        nIndex++;
        continue;
      }
      if (cCh == '\'')
      {
        sRetval += '\'';
        nIndex++;
        continue;
      }
      if (cCh == '\\')
      {
        sRetval += '\\';
        nIndex++;
        continue;
      }
      if (cCh >= '0' && cCh <= '7')
      {
        int nOrdinal = (cCh) - ('0');
        nIndex++;
        char cCh1 = sStr.charAt (nIndex);
        if (cCh1 >= '0' && cCh1 <= '7')
        {
          nOrdinal = nOrdinal * 8 + (cCh1) - ('0');
          nIndex++;
          cCh1 = sStr.charAt (nIndex);
          if (cCh <= '3' && cCh1 >= '0' && cCh1 <= '7')
          {
            nOrdinal = nOrdinal * 8 + (cCh1) - ('0');
            nIndex++;
          }
        }
        sRetval += (char) nOrdinal;
        continue;
      }
      if (cCh == 'u')
      {
        nIndex++;
        cCh = sStr.charAt (nIndex);
        if (_isHexchar (cCh))
        {
          int nOrdinal = _getHexVal (cCh);
          nIndex++;
          cCh = sStr.charAt (nIndex);
          if (_isHexchar (cCh))
          {
            nOrdinal = nOrdinal * 16 + _getHexVal (cCh);
            nIndex++;
            cCh = sStr.charAt (nIndex);
            if (_isHexchar (cCh))
            {
              nOrdinal = nOrdinal * 16 + _getHexVal (cCh);
              nIndex++;
              cCh = sStr.charAt (nIndex);
              if (_isHexchar (cCh))
              {
                nOrdinal = nOrdinal * 16 + _getHexVal (cCh);
                nIndex++;
                continue;
              }
            }
          }
        }
        JavaCCErrors.parse_error (t,
                                  "Encountered non-hex character '" +
                                     cCh +
                                     "' at position " +
                                     nIndex +
                                     " of string - Unicode escape must have 4 hex digits after it.");
        return sRetval;
      }
      JavaCCErrors.parse_error (t, "Illegal escape sequence '\\" + cCh + "' at position " + nIndex + " of string.");
      return sRetval;
    }
    return sRetval;
  }

  private static boolean _isHexchar (final char cCh)
  {
    if (cCh >= '0' && cCh <= '9')
      return true;
    if (cCh >= 'A' && cCh <= 'F')
      return true;
    if (cCh >= 'a' && cCh <= 'f')
      return true;
    return false;
  }

  private static int _getHexVal (final char cCh)
  {
    if (cCh >= '0' && cCh <= '9')
      return cCh - '0';
    if (cCh >= 'A' && cCh <= 'F')
      return cCh - 'A' + 10;
    return cCh - 'a' + 10;
  }
}
