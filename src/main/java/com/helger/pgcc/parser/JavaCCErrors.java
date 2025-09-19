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
package com.helger.pgcc.parser;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.exp.CharacterRange;
import com.helger.pgcc.parser.exp.Expansion;
import com.helger.pgcc.parser.exp.SingleCharacter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Output error messages and keep track of totals.
 */
public final class JavaCCErrors
{
  private static int s_parse_error_count = 0;
  private static int s_semantic_error_count = 0;
  private static int s_warning_count = 0;

  private JavaCCErrors ()
  {}

  @Nonnull
  private static String _getLocationInfo (@Nullable final Object node)
  {
    if (node instanceof NormalProduction)
    {
      final NormalProduction n = (NormalProduction) node;
      return "Line " + n.getLine () + ", Column " + n.getColumn () + ": ";
    }
    if (node instanceof TokenProduction)
    {
      final TokenProduction n = (TokenProduction) node;
      return "Line " + n.getLine () + ", Column " + n.getColumn () + ": ";
    }
    if (node instanceof Expansion)
    {
      final Expansion n = (Expansion) node;
      return "Line " + n.getLine () + ", Column " + n.getColumn () + ": ";
    }
    if (node instanceof CharacterRange)
    {
      final CharacterRange n = (CharacterRange) node;
      return "Line " + n.getLine () + ", Column " + n.getColumn () + ": ";
    }
    if (node instanceof SingleCharacter)
    {
      final SingleCharacter n = (SingleCharacter) node;
      return "Line " + n.getLine () + ", Column " + n.getColumn () + ": ";
    }
    if (node instanceof Token)
    {
      final Token t = (Token) node;
      return "Line " + t.beginLine + ", Column " + t.beginColumn + ": ";
    }
    return "";
  }

  public static void parse_error (final Object node, final String mess)
  {
    PGPrinter.error ("Error: " + _getLocationInfo (node) + mess);
    s_parse_error_count++;
  }

  public static void parse_error (final String mess)
  {
    PGPrinter.error ("Error: " + mess);
    s_parse_error_count++;
  }

  public static int getParseErrorCount ()
  {
    return s_parse_error_count;
  }

  public static void semantic_error (final Object node, final String mess)
  {
    PGPrinter.error ("Error: " + _getLocationInfo (node) + mess);
    s_semantic_error_count++;
  }

  public static void semantic_error (final String mess)
  {
    PGPrinter.error ("Error: " + mess);
    s_semantic_error_count++;
  }

  public static void semantic_error (final String mess, final Throwable t)
  {
    PGPrinter.error ("Error: " + mess, t);
    s_semantic_error_count++;
  }

  public static int getSemanticErrorCount ()
  {
    return s_semantic_error_count;
  }

  public static void warning (final Object node, final String mess)
  {
    PGPrinter.warn ("Warning: " + _getLocationInfo (node) + mess);
    s_warning_count++;
  }

  public static void warning (final String mess)
  {
    PGPrinter.warn ("Warning: " + mess);
    s_warning_count++;
  }

  public static int getWarningCount ()
  {
    return s_warning_count;
  }

  public static int getErrorCount ()
  {
    return s_parse_error_count + s_semantic_error_count;
  }

  public static void fatal (final String message) throws IllegalStateException
  {
    PGPrinter.error ("Fatal Error: " + message);
    throw new IllegalStateException ("Fatal Error: " + message);
  }

  public static void internalError () throws IllegalStateException
  {
    fatal ("Internal error in JavaCC: Please file an issue at https://github.com/phax/ParserGeneratorCC/issues . Thank you.");
  }

  public static void note (final String mess)
  {
    PGPrinter.info ("Note: " + mess);
  }

  public static void reInit ()
  {
    s_parse_error_count = 0;
    s_semantic_error_count = 0;
    s_warning_count = 0;
  }
}
