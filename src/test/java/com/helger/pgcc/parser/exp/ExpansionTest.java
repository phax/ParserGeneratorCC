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
package com.helger.pgcc.parser.exp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.parser.Token;

public final class ExpansionTest
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
  public void testZeroOrOneConstructor ()
  {
    final ExpZeroOrOne aZoo = new ExpZeroOrOne (m_aToken, m_aExp);
    assertEquals (m_aToken.beginColumn, aZoo.getColumn ());
    assertEquals (m_aToken.beginLine, aZoo.getLine ());
    assertEquals (m_aExp, aZoo.getExpansion ());
    assertSame (m_aExp.getParent (), aZoo);
  }

  @Test
  public void testZeroOrMoreConstructor ()
  {
    final ExpZeroOrMore aZom = new ExpZeroOrMore (m_aToken, m_aExp);
    assertEquals (m_aToken.beginColumn, aZom.getColumn ());
    assertEquals (m_aToken.beginLine, aZom.getLine ());
    assertEquals (m_aExp, aZom.getExpansion ());
    assertEquals (m_aExp.getParent (), aZom);
  }

  @Test
  public void testRZeroOrMoreConstructor ()
  {
    final AbstractExpRegularExpression r = new ExpRChoice ();
    final ExpRZeroOrMore aRzom = new ExpRZeroOrMore (m_aToken, r);
    assertEquals (m_aToken.beginColumn, aRzom.getColumn ());
    assertEquals (m_aToken.beginLine, aRzom.getLine ());
    assertEquals (r, aRzom.getRegExpr ());
  }

  @Test
  public void testROneOrMoreConstructor ()
  {
    final AbstractExpRegularExpression r = new ExpRChoice ();
    final ExpROneOrMore aRoom = new ExpROneOrMore (m_aToken, r);
    assertEquals (m_aToken.beginColumn, aRoom.getColumn ());
    assertEquals (m_aToken.beginLine, aRoom.getLine ());
    assertEquals (r, aRoom.getRegExpr ());
  }

  @Test
  public void testOneOrMoreConstructor ()
  {
    final Expansion aRce = new ExpRChoice ();
    final ExpOneOrMore aOom = new ExpOneOrMore (m_aToken, aRce);
    assertEquals (m_aToken.beginColumn, aOom.getColumn ());
    assertEquals (m_aToken.beginLine, aOom.getLine ());
    assertEquals (aRce, aOom.getExpansion ());
    assertEquals (aRce.getParent (), aOom);
  }

  @Test
  public void testRStringLiteralConstructor ()
  {
    final ExpRStringLiteral r = new ExpRStringLiteral (m_aToken, "hey");
    assertEquals (m_aToken.beginColumn, r.getColumn ());
    assertEquals (m_aToken.beginLine, r.getLine ());
    assertEquals ("hey", r.m_image);
  }

  @Test
  public void testRJustNameConstructor ()
  {
    final ExpRJustName r = new ExpRJustName (m_aToken, "hey");
    assertEquals (m_aToken.beginColumn, r.getColumn ());
    assertEquals (m_aToken.beginLine, r.getLine ());
    assertEquals ("hey", r.getLabel ());
  }

  @Test
  public void testSequenceConstructor ()
  {
    final ExpLookahead aLa = new ExpLookahead ();
    final ExpSequence s = new ExpSequence (m_aToken, aLa);
    assertEquals (m_aToken.beginColumn, s.getColumn ());
    assertEquals (m_aToken.beginLine, s.getLine ());
    assertSame (aLa, s.getUnitAt (0));
  }
}
