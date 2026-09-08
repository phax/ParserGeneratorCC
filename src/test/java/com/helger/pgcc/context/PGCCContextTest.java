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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;

/**
 * Test class for {@link PGCCContext}.
 * <p>
 * Note what this test does <em>not</em> claim: two full generator runs cannot share a JVM
 * concurrently yet, because most of the generator still keeps its state in static fields. What it
 * does prove is that the parts which have already moved into the context - the option values and
 * the error counters - are isolated per thread, which is the property the rest of the migration is
 * working towards.
 *
 * @author Philip Helger
 */
public final class PGCCContextTest
{
  @Test
  public void testResetStartsAFreshRun ()
  {
    Options.init ();
    JavaCCErrors.warning ("something");
    assertEquals (1, JavaCCErrors.getWarningCount ());

    PGCCContext.reset ();
    assertEquals (0, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void testOptionsAreIsolatedPerThread () throws Exception
  {
    Options.init ();
    Options.setCmdLineOption ("-OUTPUT_LANGUAGE=java");
    assertEquals (EOutputLanguage.JAVA, Options.getOutputLanguage ());

    final AtomicReference <EOutputLanguage> aOther = new AtomicReference <> ();
    final AtomicReference <Integer> aOtherWarnings = new AtomicReference <> ();
    final CountDownLatch aDone = new CountDownLatch (1);

    final Thread aThread = new Thread (() -> {
      Options.init ();
      Options.setCmdLineOption ("-OUTPUT_LANGUAGE=c++");
      JavaCCErrors.warning ("only in this thread");
      JavaCCErrors.warning ("and this one");
      aOther.set (Options.getOutputLanguage ());
      aOtherWarnings.set (Integer.valueOf (JavaCCErrors.getWarningCount ()));
      aDone.countDown ();
    });
    aThread.start ();
    assertTrue ("The other thread did not finish", aDone.await (30, TimeUnit.SECONDS));
    aThread.join ();

    // The other thread saw its own settings ...
    assertEquals (EOutputLanguage.CPP, aOther.get ());
    assertEquals (Integer.valueOf (2), aOtherWarnings.get ());

    // ... and did not disturb this one
    assertEquals (EOutputLanguage.JAVA, Options.getOutputLanguage ());
    assertEquals (0, JavaCCErrors.getWarningCount ());
  }
}
