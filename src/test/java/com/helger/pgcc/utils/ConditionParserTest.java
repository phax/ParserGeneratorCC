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
package com.helger.pgcc.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ConditionParser}.
 *
 * @author Philip Helger
 */
public final class ConditionParserTest
{
  private static void _test (final String input, final boolean expectedValue) throws ParseException
  {
    final Map <String, Object> values = new HashMap <> ();
    values.put ("F", Boolean.FALSE);
    values.put ("T", Boolean.TRUE);
    _test (input, values, expectedValue);
  }

  private static void _test (final String input, final Map <String, Object> values, final boolean expectedValue) throws ParseException
  {
    final ConditionParser cp = new ConditionParser (input);
    final boolean value = cp.CompilationUnit (values);
    assertEquals (Boolean.valueOf (expectedValue), Boolean.valueOf (value));
  }

  @Test
  public void testBasic () throws ParseException
  {
    _test ("F", false);
    _test ("T", true);
    _test ("F || T", true);
    _test ("T || F", true);
    _test ("T || will not be compiled )", true);
    _test ("F && T", false);
    _test ("T && T", true);
    _test ("unknown", false);
  }

  @Test
  @Ignore ("Works with code generated from PGCC 1.1.3 and onwards")
  public void testBufferExpansion () throws ParseException
  {
    // 4 = open + close of the comment
    final String a = StringHelper.getRepeated ('a', 2048 - 4);
    // force the buffer to expand and wrap around
    final String b = StringHelper.getRepeated ('b', 4096 - 4 + 1);
    _test ("/*" + a + "*/\n/*" + b + "*/\nT || F", true);
  }

  @Test
  public void testBufferExpansion2 () throws ParseException
  {
    // Large tokens
    final String a = StringHelper.getRepeated ('a', 1024 * 1024 * 4);
    final String b = StringHelper.getRepeated ('b', 1024 * 1024 * 4);
    _test ("/*" + a + "*/\n/*" + b + "*/\nT || F", true);
  }
}
