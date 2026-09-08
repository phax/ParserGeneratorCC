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

import org.jspecify.annotations.Nullable;

import com.helger.pgcc.jjdoc.IDocGenerator;

/**
 * The state of a single JJDoc run: what it reads, what it writes, and which generator produces the
 * output format.
 * <p>
 * This is the instance state behind the static {@link com.helger.pgcc.jjdoc.JJDocGlobals} facade.
 *
 * @author Philip Helger
 */
public final class JJDocState
{
  /** Default constructor. */
  public JJDocState ()
  {}

  private String m_sInputFile;
  private String m_sOutputFile;
  private IDocGenerator m_aGenerator;

  /**
   * {@return the grammar JJDoc is documenting}
   */
  @Nullable
  public String getInputFile ()
  {
    return m_sInputFile;
  }

  /**
   * Record which grammar JJDoc is documenting.
   *
   * @param sInputFile
   *        The file name, or {@link com.helger.pgcc.jjdoc.JJDocGlobals#STANDARD_INPUT}. May be
   *         <code>null</code>.
   */
  public void setInputFile (@Nullable final String sInputFile)
  {
    m_sInputFile = sInputFile;
  }

  /**
   * {@return the file JJDoc is writing}
   */
  @Nullable
  public String getOutputFile ()
  {
    return m_sOutputFile;
  }

  /**
   * Record which file JJDoc is writing.
   *
   * @param sOutputFile
   *        The file name, or {@link com.helger.pgcc.jjdoc.JJDocGlobals#STANDARD_OUTPUT}. May be
   *         <code>null</code>.
   */
  public void setOutputFile (@Nullable final String sOutputFile)
  {
    m_sOutputFile = sOutputFile;
  }

  /**
   * {@return the generator producing the output, which is what decides between HTML, plain text,
   * BNF and XText}
   */
  @Nullable
  public IDocGenerator getGenerator ()
  {
    return m_aGenerator;
  }

  /**
   * Choose what JJDoc produces.
   *
   * @param aGenerator
   *        The generator. May be <code>null</code>.
   */
  public void setGenerator (@Nullable final IDocGenerator aGenerator)
  {
    m_aGenerator = aGenerator;
  }
}
