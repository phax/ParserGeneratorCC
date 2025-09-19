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
    final ExpZeroOrOne zoo = new ExpZeroOrOne (m_aToken, m_aExp);
    assertEquals (m_aToken.beginColumn, zoo.getColumn ());
    assertEquals (m_aToken.beginLine, zoo.getLine ());
    assertEquals (m_aExp, zoo.getExpansion ());
    assertSame (m_aExp.getParent (), zoo);
  }

  @Test
  public void testZeroOrMoreConstructor ()
  {
    final ExpZeroOrMore zom = new ExpZeroOrMore (m_aToken, m_aExp);
    assertEquals (m_aToken.beginColumn, zom.getColumn ());
    assertEquals (m_aToken.beginLine, zom.getLine ());
    assertEquals (m_aExp, zom.getExpansion ());
    assertEquals (m_aExp.getParent (), zom);
  }

  @Test
  public void testRZeroOrMoreConstructor ()
  {
    final AbstractExpRegularExpression r = new ExpRChoice ();
    final ExpRZeroOrMore rzom = new ExpRZeroOrMore (m_aToken, r);
    assertEquals (m_aToken.beginColumn, rzom.getColumn ());
    assertEquals (m_aToken.beginLine, rzom.getLine ());
    assertEquals (r, rzom.getRegExpr ());
  }

  @Test
  public void testROneOrMoreConstructor ()
  {
    final AbstractExpRegularExpression r = new ExpRChoice ();
    final ExpROneOrMore room = new ExpROneOrMore (m_aToken, r);
    assertEquals (m_aToken.beginColumn, room.getColumn ());
    assertEquals (m_aToken.beginLine, room.getLine ());
    assertEquals (r, room.getRegExpr ());
  }

  @Test
  public void testOneOrMoreConstructor ()
  {
    final Expansion rce = new ExpRChoice ();
    final ExpOneOrMore oom = new ExpOneOrMore (m_aToken, rce);
    assertEquals (m_aToken.beginColumn, oom.getColumn ());
    assertEquals (m_aToken.beginLine, oom.getLine ());
    assertEquals (rce, oom.getExpansion ());
    assertEquals (rce.getParent (), oom);
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
    final ExpLookahead la = new ExpLookahead ();
    final ExpSequence s = new ExpSequence (m_aToken, la);
    assertEquals (m_aToken.beginColumn, s.getColumn ());
    assertEquals (m_aToken.beginLine, s.getLine ());
    assertSame (la, s.getUnitAt (0));
  }
}
