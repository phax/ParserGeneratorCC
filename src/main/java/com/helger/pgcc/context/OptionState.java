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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.output.EOutputLanguage;

/**
 * The option values of a single generator run: what the defaults are, what the grammar file set,
 * what the command line set, and the output language derived from them.
 * <p>
 * This is the instance state behind the static {@link com.helger.pgcc.parser.Options} facade. Every
 * caller goes through that facade's static methods, so moving the data here changed no call site.
 *
 * @author Philip Helger
 */
public final class OptionState
{
  /** Default constructor. */
  public OptionState ()
  {}

  private final Map <String, Object> m_aValues = new HashMap <> ();
  private final Set <String> m_aCmdLineSet = new HashSet <> ();
  private final Set <String> m_aInputFileSet = new HashSet <> ();
  private EOutputLanguage m_eLanguage = EOutputLanguage.JAVA;

  /**
   * {@return the mutable map of option name to value. Never <code>null</code>}
   */
  @NonNull
  public Map <String, Object> values ()
  {
    return m_aValues;
  }

  /**
   * {@return the names of the options that were set on the command line. Never <code>null</code>}
   */
  @NonNull
  public Set <String> cmdLineSet ()
  {
    return m_aCmdLineSet;
  }

  /**
   * {@return the names of the options that were set in the grammar file. Never <code>null</code>}
   */
  @NonNull
  public Set <String> inputFileSet ()
  {
    return m_aInputFileSet;
  }

  @NonNull
  public EOutputLanguage getLanguage ()
  {
    return m_eLanguage;
  }

  public void setLanguage (@NonNull final EOutputLanguage eLanguage)
  {
    m_eLanguage = eLanguage;
  }

  public void reset ()
  {
    m_aValues.clear ();
    m_aCmdLineSet.clear ();
    m_aInputFileSet.clear ();
    m_eLanguage = EOutputLanguage.JAVA;
  }
}
