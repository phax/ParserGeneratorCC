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
package com.helger.pgcc.parser.exp;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.parser.Token;

public final class ExpChoiceTest
{
  private Token m_aToken;
  private Expansion m_aExp;

  @Before
  public void setUp ()
  {
    m_aToken = new Token ();
    m_aToken.beginColumn = 2;
    m_aToken.beginLine = 3;
    m_aExp = new Expansion ();
    m_aExp.setColumn (5);
    m_aExp.setLine (6);
  }

  @Test
  public void testChoiceConstructor ()
  {
    ExpChoice c = new ExpChoice (m_aToken);
    assertEquals (m_aToken.beginColumn, c.getColumn ());
    assertEquals (m_aToken.beginLine, c.getLine ());
    assertEquals (0, c.getChoiceCount ());
    c = new ExpChoice (m_aExp);
    assertEquals (m_aExp.getColumn (), c.getColumn ());
    assertEquals (m_aExp.getLine (), c.getLine ());
    assertEquals (m_aExp, c.getChoiceAt (0));
  }
}
