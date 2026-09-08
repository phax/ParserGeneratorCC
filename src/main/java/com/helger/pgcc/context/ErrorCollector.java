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

/**
 * Counts the errors and warnings of a single generator run.
 * <p>
 * This is the instance state behind the static
 * {@link com.helger.pgcc.parser.JavaCCErrors} facade. It is the first piece of the generator state
 * that was moved out of a static field, and the pattern the remaining ones follow: the mutable data
 * lives here, {@link PGCCContext} owns one instance per run, and the old static class stays as a
 * thin delegate so that grammar action code keeps compiling.
 *
 * @author Philip Helger
 */
public final class ErrorCollector
{
  private int m_nParseErrors;
  private int m_nSemanticErrors;
  private int m_nWarnings;

  public void onParseError ()
  {
    m_nParseErrors++;
  }

  public void onSemanticError ()
  {
    m_nSemanticErrors++;
  }

  public void onWarning ()
  {
    m_nWarnings++;
  }

  public int getParseErrorCount ()
  {
    return m_nParseErrors;
  }

  public int getSemanticErrorCount ()
  {
    return m_nSemanticErrors;
  }

  public int getWarningCount ()
  {
    return m_nWarnings;
  }

  /**
   * @return The number of errors, which is the sum of the parse errors and the semantic errors.
   *         Warnings are not errors.
   */
  public int getErrorCount ()
  {
    return m_nParseErrors + m_nSemanticErrors;
  }

  public void reset ()
  {
    m_nParseErrors = 0;
    m_nSemanticErrors = 0;
    m_nWarnings = 0;
  }
}
