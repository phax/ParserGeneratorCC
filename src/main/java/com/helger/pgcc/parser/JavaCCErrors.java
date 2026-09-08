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
package com.helger.pgcc.parser;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.context.PGCCContext;

/**
 * Output error messages and keep track of totals. The totals live in
 * {@link com.helger.pgcc.context.ErrorCollector}, one per generator run.
 */
public final class JavaCCErrors
{
  private JavaCCErrors ()
  {}

  @NonNull
  private static String _getLocationInfo (@Nullable final IGrammarLocation aNode)
  {
    return aNode == null ? "" : "Line " + aNode.getLineNumber () + ", Column " + aNode.getColumnNumber () + ": ";
  }

  public static void parse_error (@Nullable final IGrammarLocation aNode, final String sMess)
  {
    PGPrinter.error ("Error: " + _getLocationInfo (aNode) + sMess);
    PGCCContext.current ().errors ().onParseError ();
  }

  public static void parse_error (final String sMess)
  {
    PGPrinter.error ("Error: " + sMess);
    PGCCContext.current ().errors ().onParseError ();
  }

  public static int getParseErrorCount ()
  {
    return PGCCContext.current ().errors ().getParseErrorCount ();
  }

  public static void semantic_error (@Nullable final IGrammarLocation aNode, final String sMess)
  {
    PGPrinter.error ("Error: " + _getLocationInfo (aNode) + sMess);
    PGCCContext.current ().errors ().onSemanticError ();
  }

  public static void semantic_error (final String sMess)
  {
    PGPrinter.error ("Error: " + sMess);
    PGCCContext.current ().errors ().onSemanticError ();
  }

  public static void semantic_error (final String sMess, final Throwable t)
  {
    PGPrinter.error ("Error: " + sMess, t);
    PGCCContext.current ().errors ().onSemanticError ();
  }

  public static int getSemanticErrorCount ()
  {
    return PGCCContext.current ().errors ().getSemanticErrorCount ();
  }

  public static void warning (@Nullable final IGrammarLocation aNode, final String sMess)
  {
    PGPrinter.warn ("Warning: " + _getLocationInfo (aNode) + sMess);
    PGCCContext.current ().errors ().onWarning ();
  }

  public static void warning (final String sMess)
  {
    PGPrinter.warn ("Warning: " + sMess);
    PGCCContext.current ().errors ().onWarning ();
  }

  public static int getWarningCount ()
  {
    return PGCCContext.current ().errors ().getWarningCount ();
  }

  public static int getErrorCount ()
  {
    return PGCCContext.current ().errors ().getErrorCount ();
  }

  public static void fatal (final String sMessage) throws IllegalStateException
  {
    PGPrinter.error ("Fatal Error: " + sMessage);
    throw new IllegalStateException ("Fatal Error: " + sMessage);
  }

  public static void internalError () throws IllegalStateException
  {
    fatal ("Internal error in JavaCC: Please file an issue at https://github.com/phax/ParserGeneratorCC/issues . Thank you.");
  }

  public static void note (final String sMess)
  {
    PGPrinter.info ("Note: " + sMess);
  }
}
