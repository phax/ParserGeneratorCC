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
package com.helger.pgcc.context;

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.PGPrinter.IPrinter;

/**
 * The handful of settings that are deliberately process wide rather than per run.
 * <p>
 * {@link PGCCContext} holds what belongs to one generator run, on one thread. These two do not:
 * where the tool writes its console output, and whether the templates are read from the class path
 * or from the checkout. An embedder sets them once for the process, and a test sets them in
 * {@code @BeforeClass} and then generates on whatever thread it likes -
 * {@code ConcurrentGenerationTest} does exactly that. Putting them in the per run context would
 * mean the setting is lost the moment {@code Main.reInitAll ()} replaces the context, or invisible
 * to a worker thread.
 * <p>
 * They live here so that no <code>static</code> non final field exists anywhere outside this
 * package. The singleton reference is <code>final</code>; the state is instance state on it.
 *
 * @author Philip Helger
 */
public final class ProcessState
{
  private static final ProcessState INSTANCE = new ProcessState ();

  private IPrinter m_aOut;
  private IPrinter m_aErr;
  private boolean m_bReadTemplatesFromClassPath = true;

  private ProcessState ()
  {}

  /**
   * @return The single instance. Never <code>null</code>.
   */
  @NonNull
  public static ProcessState getInstance ()
  {
    return INSTANCE;
  }

  /**
   * @return Where informational output goes, or <code>null</code> if it has not been set yet.
   */
  public IPrinter getOut ()
  {
    return m_aOut;
  }

  /**
   * @return Where error output goes, or <code>null</code> if it has not been set yet.
   */
  public IPrinter getErr ()
  {
    return m_aErr;
  }

  /**
   * Set where the console output goes.
   *
   * @param aOut
   *        Informational output. May not be <code>null</code>.
   * @param aErr
   *        Error output. May not be <code>null</code>.
   */
  public void setPrinters (@NonNull final IPrinter aOut, @NonNull final IPrinter aErr)
  {
    ValueEnforcer.notNull (aOut, "Out");
    ValueEnforcer.notNull (aErr, "Err");
    m_aOut = aOut;
    m_aErr = aErr;
  }

  /**
   * @return <code>true</code> if the templates are read from the class path, <code>false</code> if
   *         they are read from the file system of this checkout.
   */
  public boolean isReadTemplatesFromClassPath ()
  {
    return m_bReadTemplatesFromClassPath;
  }

  /**
   * @param bReadFromClassPath
   *        <code>true</code> to read the templates from the class path.
   */
  public void setReadTemplatesFromClassPath (final boolean bReadFromClassPath)
  {
    m_bReadTemplatesFromClassPath = bReadFromClassPath;
  }
}
