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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.rt.StackTraceHelper;
import com.helger.base.string.StringHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class PGPrinter
{
  public static interface IPrinter extends AutoCloseable
  {
    void println (@Nonnull String s);

    void flush ();
  }

  public static final class PSPrinter implements IPrinter
  {
    private final PrintStream m_aPS;
    private final boolean m_bCanClose;

    public PSPrinter (@Nonnull final PrintStream aPS, final boolean bCanClose)
    {
      m_aPS = aPS;
      m_bCanClose = bCanClose;
    }

    public void close () throws IOException
    {
      if (m_bCanClose)
        m_aPS.close ();
    }

    public void println (@Nonnull final String s)
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

  private static IPrinter s_aOut = new PSPrinter (System.out, false);
  private static IPrinter s_aErr = new PSPrinter (System.err, false);

  private PGPrinter ()
  {}

  public static void init (@Nonnull final IPrinter aPrinter)
  {
    init (aPrinter, aPrinter);
  }

  public static void init (@Nonnull final IPrinter aPrinterInfo, @Nonnull final IPrinter aPrinterError)
  {
    ValueEnforcer.notNull (aPrinterInfo, "PrinterInfo");
    ValueEnforcer.notNull (aPrinterError, "PrinterError");
    s_aOut = aPrinterInfo;
    s_aErr = aPrinterError;
  }

  public static void debug (@Nonnull final String sMsg)
  {
    s_aOut.println (sMsg);
  }

  public static void info ()
  {
    s_aOut.println (null);
  }

  public static void info (@Nonnull final String sMsg)
  {
    s_aOut.println (sMsg);
  }

  public static void warn (@Nonnull final String sMsg)
  {
    warn (sMsg, null);
  }

  public static void warn (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    s_aErr.println (sMsg);
    if (t != null)
      s_aErr.println (StackTraceHelper.getStackAsString (t));
  }

  public static void error (@Nonnull final String sMsg)
  {
    error (sMsg, null);
  }

  public static void error (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    s_aErr.println (sMsg);
    if (t != null)
      s_aErr.println (StackTraceHelper.getStackAsString (t));
  }

  public static void flush ()
  {
    s_aOut.flush ();
    s_aErr.flush ();
  }

  public static void close () throws Exception
  {
    s_aOut.close ();
    s_aErr.close ();
  }

  @Nonnull
  public static PrintWriter getOutWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.out, Charset.defaultCharset ()));
  }

  @Nonnull
  public static PrintWriter getErrWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.err, Charset.defaultCharset ()));
  }
}
