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

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the various regular expression productions.
 */
public class TokenProduction
{
  /**
   * The starting line and column of this token production.
   */
  private int m_nColumn;

  private int m_nLine;

  /**
   * The states in which this regular expression production exists. If this
   * array is null, then "&lt;*&gt;" has been specified and this regular
   * expression exists in all states. However, this null value is replaced by a
   * String array that includes all lexical state names during the
   * semanticization phase.
   */
  public String [] m_lexStates;

  /**
   * The kind of this token production - TOKEN, SKIP, MORE, or SPECIAL.
   */
  public ETokenKind m_kind;

  /**
   * The list of regular expression specifications that comprise this
   * production. Each entry is a "RegExprSpec".
   */
  public List <RegExprSpec> m_respecs = new ArrayList <> ();

  /**
   * This is true if this corresponds to a production that actually appears in
   * the input grammar. Otherwise (if this is created to describe a regular
   * expression that is part of the BNF) this is set to false.
   */
  public boolean m_isExplicit = true;

  /**
   * This is true if case is to be ignored within the regular expressions of
   * this token production.
   */
  public boolean m_ignoreCase = false;

  /**
   * The first and last tokens from the input stream that represent this
   * production.
   */
  public Token m_firstToken;
  public Token m_lastToken;

  /**
   * @return the column
   */
  public final int getColumn ()
  {
    return m_nColumn;
  }

  /**
   * @param nColumn
   *        the column to set
   */
  public final void setColumn (final int nColumn)
  {
    m_nColumn = nColumn;
  }

  /**
   * @return the line
   */
  public final int getLine ()
  {
    return m_nLine;
  }

  /**
   * @param nLine
   *        the line to set
   */
  public final void setLine (final int nLine)
  {
    m_nLine = nLine;
  }
}
