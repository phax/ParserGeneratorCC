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
package com.helger.pgcc.jjtree;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileOperationManager;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGPrinter.IPrinter;
import com.helger.pgcc.PGPrinter.PSPrinter;

/**
 * Everything JJTree prints during a normal run has to be something a user would want to read.
 * <p>
 * {@code ASTGrammar.generate} printed <code>opt:java</code> on every single run - a leftover debug
 * line that had been shipped for as long as this fork exists. Nothing noticed, because the golden
 * files watch the generated files and say nothing about the console. This checks the console.
 *
 * @author Philip Helger
 */
public final class JJTreeConsoleOutputTest
{
  private static final File WORK_DIR = new File ("target/jjtree-console");

  /** What a normal run is allowed to say */
  private static final String [] EXPECTED_PREFIXES = { "ParserGeneratorCC Version", "(type \"jjtree\"",
                                                       "Reading from file ", "Warning: Output directory ", "File \"",
                                                       "Annotated grammar generated successfully" };

  private static final class CollectingPrinter implements IPrinter
  {
    private final List <String> m_aLines = new ArrayList <> ();

    public void println (final String s)
    {
      m_aLines.add (s == null ? "" : s);
    }

    public void flush ()
    {}

    public void close ()
    {}
  }

  private CollectingPrinter m_aPrinter;

  @Before
  public void before ()
  {
    m_aPrinter = new CollectingPrinter ();
    PGPrinter.init (m_aPrinter);
  }

  @After
  public void after ()
  {
    PGPrinter.init (new PSPrinter (System.out, false), new PSPrinter (System.err, false));
  }

  @Test
  public void testANormalRunPrintsNothingUnexpected ()
  {
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (WORK_DIR);

    final ESuccess eSuccess = new JJTree ().main (new String [] { "-OUTPUT_DIRECTORY=" + WORK_DIR.getAbsolutePath (),
                                                                  "-MULTI=true", "-VISITOR=true", new File (
                                                                                                            "src/test/resources/roundtrip/tree.jjt").getAbsolutePath () });
    assertTrue ("JJTree failed", eSuccess.isSuccess ());

    final List <String> aUnexpected = new ArrayList <> ();
    for (final String sLine : m_aPrinter.m_aLines)
    {
      if (sLine.isBlank ())
        continue;
      boolean bKnown = false;
      for (final String sPrefix : EXPECTED_PREFIXES)
        if (sLine.startsWith (sPrefix))
          bKnown = true;
      if (!bKnown)
        aUnexpected.add (sLine);
    }

    assertTrue ("JJTree printed lines that look like leftover debugging. Either the line should go, " +
                "or EXPECTED_PREFIXES should learn about it:\n" +
                String.join ("\n", aUnexpected),
                aUnexpected.isEmpty ());
    assertTrue ("JJTree printed nothing at all", m_aPrinter.m_aLines.size () > 3);
  }
}
