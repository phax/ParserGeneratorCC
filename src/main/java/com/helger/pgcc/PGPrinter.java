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

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.rt.StackTraceHelper;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.ProcessState;

  /**
   * Everything the generator prints for a human to read. Routing it through here rather than
   * through System.out is what lets a caller that embeds the generator - the Maven plugin, a test
   * - collect the output instead of having it land on the console.
   */
public final class PGPrinter
{
  /**
   * Where the messages go. Two of these are installed at a time, one for the ordinary output and
   * one for the errors, and they may well be the same object.
   */
  public interface IPrinter extends AutoCloseable, Flushable
  {
  /**
   * Print one line.
   *
   * @param s
   *        The line, without a line break. May not be <code>null</code>.
   */
    void println (@NonNull String s);

    void flush ();
  }

  /**
   * An {@link IPrinter} writing to a {@link PrintStream}, which is what the command line
   * installs.
   */
  public static final class PSPrinter implements IPrinter
  {
    private final PrintStream m_aPS;
    private final boolean m_bCanClose;

  /**
   * Create a printer for one stream.
   *
   * @param aPS
   *        The stream to write to. May not be <code>null</code>.
   * @param bCanClose
   *        <code>false</code> for a stream this printer does not own, System.out above all.
   */
    public PSPrinter (@NonNull final PrintStream aPS, final boolean bCanClose)
    {
      m_aPS = aPS;
      m_bCanClose = bCanClose;
    }

    public void close () throws IOException
    {
      if (m_bCanClose)
        m_aPS.close ();
    }

    public void println (@NonNull final String s)
    {
      if (StringHelper.isEmpty (s))
        m_aPS.println ();
      else
        m_aPS.println (s);
    }

    public void flush ()
    {
      m_aPS.flush ();
    }
  }

  static
  {
    // The default is the console. It lives on ProcessState so that no static non final field
    // exists outside the context package
    ProcessState.getInstance ().setPrinters (new PSPrinter (System.out, false), new PSPrinter (System.err, false));
  }

  private PGPrinter ()
  {}

  @NonNull
  private static IPrinter _out ()
  {
    return ProcessState.getInstance ().getOut ();
  }

  @NonNull
  private static IPrinter _err ()
  {
    return ProcessState.getInstance ().getErr ();
  }

  /**
   * Send both the ordinary output and the errors to the same printer.
   *
   * @param aPrinter
   *        Where everything goes. May not be <code>null</code>.
   */
  public static void init (@NonNull final IPrinter aPrinter)
  {
    init (aPrinter, aPrinter);
  }

  /**
   * Send the ordinary output and the errors to different printers.
   *
   * @param aPrinterInfo
   *        Where the ordinary output goes. May not be <code>null</code>.
   * @param aPrinterError
   *        Where the errors go. May not be <code>null</code>.
   */
  public static void init (@NonNull final IPrinter aPrinterInfo, @NonNull final IPrinter aPrinterError)
  {
    ProcessState.getInstance ().setPrinters (aPrinterInfo, aPrinterError);
  }

  /**
   * Print a message that only matters while chasing a problem.
   *
   * @param sMsg
   *        The message. May not be <code>null</code>.
   */
  public static void debug (@NonNull final String sMsg)
  {
    _out ().println (sMsg);
  }

  /**
   * Print an empty line, to separate what comes next from what came before.
   */
  public static void info ()
  {
    _out ().println (null);
  }

  /**
   * Print a message that the user is meant to read.
   *
   * @param sMsg
   *        The message. May not be <code>null</code>.
   */
  public static void info (@NonNull final String sMsg)
  {
    _out ().println (sMsg);
  }

  /**
   * Print a warning.
   *
   * @param sMsg
   *        The message. May not be <code>null</code>.
   */
  public static void warn (@NonNull final String sMsg)
  {
    warn (sMsg, null);
  }

  /**
   * Print a warning together with what went wrong.
   *
   * @param sMsg
   *        The message. May not be <code>null</code>.
   * @param t
   *        The exception behind it. May be <code>null</code>.
   */
  public static void warn (@NonNull final String sMsg, @Nullable final Throwable t)
  {
    _err ().println (sMsg);
    if (t != null)
      _err ().println (StackTraceHelper.getStackAsString (t));
  }

  /**
   * Print an error.
   *
   * @param sMsg
   *        The message. May not be <code>null</code>.
   */
  public static void error (@NonNull final String sMsg)
  {
    error (sMsg, null);
  }

  /**
   * Print an error together with what went wrong.
   *
   * @param sMsg
   *        The message. May not be <code>null</code>.
   * @param t
   *        The exception behind it. May be <code>null</code>.
   */
  public static void error (@NonNull final String sMsg, @Nullable final Throwable t)
  {
    _err ().println (sMsg);
    if (t != null)
      _err ().println (StackTraceHelper.getStackAsString (t));
  }

  /**
   * Push everything written so far out to both printers.
   */
  public static void flush ()
  {
    _out ().flush ();
    _err ().flush ();
  }

  /**
   * Close both printers, which is a no-op for the ones that do not own their stream. @throws
   * Exception if a printer refuses to close
   */
  public static void close () throws Exception
  {
    _out ().close ();
    _err ().close ();
  }

  /**
   * {@return a writer onto the process's standard output, for the code that needs a PrintWriter
   * rather than an IPrinter}
   */
  @NonNull
  public static PrintWriter getOutWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.out, Charset.defaultCharset ()));
  }

  /**
   * {@return a writer onto the process's standard error, for the code that needs a PrintWriter
   * rather than an IPrinter}
   */
  @NonNull
  public static PrintWriter getErrWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.err, Charset.defaultCharset ()));
  }
}
