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

  /**
   * Report something the grammar parser could not make sense of, pointing at where it is.
   *
   * @param aNode
   *        Where in the grammar the problem is. May be <code>null</code>.
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void parseError (@Nullable final IGrammarLocation aNode, final String sMess)
  {
    PGPrinter.error ("Error: " + _getLocationInfo (aNode) + sMess);
    PGCCContext.current ().errors ().onParseError ();
  }

  /**
   * Report something the grammar parser could not make sense of, without a position.
   *
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void parseError (final String sMess)
  {
    PGPrinter.error ("Error: " + sMess);
    PGCCContext.current ().errors ().onParseError ();
  }

  /**
   * {@return how many parse errors this run has reported}
   */
  public static int getParseErrorCount ()
  {
    return PGCCContext.current ().errors ().getParseErrorCount ();
  }

  /**
   * Report a grammar that parses but does not make sense - an undefined production, a token that
   * can never match - pointing at where it is.
   *
   * @param aNode
   *        Where in the grammar the problem is. May be <code>null</code>.
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void semanticError (@Nullable final IGrammarLocation aNode, final String sMess)
  {
    PGPrinter.error ("Error: " + _getLocationInfo (aNode) + sMess);
    PGCCContext.current ().errors ().onSemanticError ();
  }

  /**
   * Report a grammar that parses but does not make sense, without a position.
   *
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void semanticError (final String sMess)
  {
    PGPrinter.error ("Error: " + sMess);
    PGCCContext.current ().errors ().onSemanticError ();
  }

  /**
   * Report a grammar that parses but does not make sense, together with what went wrong.
   *
   * @param sMess
   *        The message. May not be <code>null</code>.
   * @param t
   *        The exception behind it. May be <code>null</code>.
   */
  public static void semanticError (final String sMess, final Throwable t)
  {
    PGPrinter.error ("Error: " + sMess, t);
    PGCCContext.current ().errors ().onSemanticError ();
  }

  /**
   * {@return how many semantic errors this run has reported}
   */
  public static int getSemanticErrorCount ()
  {
    return PGCCContext.current ().errors ().getSemanticErrorCount ();
  }

  /**
   * Report something questionable that does not stop generation, pointing at where it is.
   *
   * @param aNode
   *        Where in the grammar the problem is. May be <code>null</code>.
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void warning (@Nullable final IGrammarLocation aNode, final String sMess)
  {
    PGPrinter.warn ("Warning: " + _getLocationInfo (aNode) + sMess);
    PGCCContext.current ().errors ().onWarning ();
  }

  /**
   * Report something questionable that does not stop generation, without a position.
   *
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void warning (final String sMess)
  {
    PGPrinter.warn ("Warning: " + sMess);
    PGCCContext.current ().errors ().onWarning ();
  }

  /**
   * {@return how many warnings this run has reported}
   */
  public static int getWarningCount ()
  {
    return PGCCContext.current ().errors ().getWarningCount ();
  }

  /**
   * {@return how many errors this run has reported, parse and semantic together}
   */
  public static int getErrorCount ()
  {
    return PGCCContext.current ().errors ().getErrorCount ();
  }

  /**
   * Report something that makes going on pointless, and stop.
   *
   * @param sMessage
   *        The message. May not be <code>null</code>.
   *
   * @throws IllegalStateException
   *         always
   */
  public static void fatal (final String sMessage) throws IllegalStateException
  {
    PGPrinter.error ("Fatal Error: " + sMessage);
    throw new IllegalStateException ("Fatal Error: " + sMessage);
  }

  /**
   * Report a bug in the generator itself, and stop.
   *
   * @throws IllegalStateException
   *         always
   */
  public static void internalError () throws IllegalStateException
  {
    fatal ("Internal error in JavaCC: Please file an issue at https://github.com/phax/ParserGeneratorCC/issues . Thank you.");
  }

  /**
   * Tell the user something they may want to know, which is neither a problem nor a warning.
   *
   * @param sMess
   *        The message. May not be <code>null</code>.
   */
  public static void note (final String sMess)
  {
    PGPrinter.info ("Note: " + sMess);
  }
}
