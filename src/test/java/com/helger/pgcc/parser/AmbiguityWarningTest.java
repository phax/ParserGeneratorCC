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
package com.helger.pgcc.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGPrinter.IPrinter;
import com.helger.pgcc.PGPrinter.PSPrinter;

/**
 * Test class for the ambiguity warnings that {@link LookaheadCalc} produces. They are the messages
 * a grammar author sees most often, and were not covered by anything.
 *
 * @author Philip Helger
 */
public final class AmbiguityWarningTest
{
  private static final File DIR = new File ("target/ambiguity-test");

  /** Collects what the generator prints, so the messages can be asserted on. */
  private static final class CollectingPrinter implements IPrinter
  {
    private final List <String> m_aLines = new ArrayList <> ();

    public void println (@NonNull final String s)
    {
      m_aLines.add (s);
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
    DIR.mkdirs ();
    m_aPrinter = new CollectingPrinter ();
    PGPrinter.init (m_aPrinter);
  }

  @After
  public void after ()
  {
    PGPrinter.init (new PSPrinter (System.out, false), new PSPrinter (System.err, false));
  }

  /**
   * Generate a parser from a grammar body and return everything that was printed.
   *
   * @param sName
   *        Name of the grammar, used for the file and the output directory. May not be
   *        <code>null</code>.
   * @param sBody
   *        The grammar after PARSER_END. May not be <code>null</code>.
   * @return All printed lines. Never <code>null</code>.
   * @throws Exception
   *         On error
   */
  @NonNull
  private List <String> _generate (final String sName, final String sBody) throws Exception
  {
    final File aGrammar = new File (DIR, sName + ".jj");
    Files.write (aGrammar.toPath (),
                 ("PARSER_BEGIN(" + sName + ")\npublic class " + sName + " {}\nPARSER_END(" + sName + ")\n" + sBody)
                                                                                                                    .getBytes (StandardCharsets.UTF_8));
    final File aOutDir = new File (DIR, sName);
    aOutDir.mkdirs ();
    Main.mainProgram ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath (), aGrammar.getAbsolutePath ());
    return m_aPrinter.m_aLines;
  }

  private static void _assertReported (final List <String> aLines, final String sExpectedFragment)
  {
    for (final String sLine : aLines)
      if (sLine.contains (sExpectedFragment))
        return;
    assertEquals ("Expected a message containing '" + sExpectedFragment + "' but got " + aLines, "", "not found");
  }

  private static void _assertNotReported (final List <String> aLines, final String sUnexpectedFragment)
  {
    for (final String sLine : aLines)
      assertTrue ("Did not expect '" + sUnexpectedFragment + "' in " + sLine, !sLine.contains (sUnexpectedFragment));
  }

  @Test
  public void testUnambiguousChoiceIsSilent () throws Exception
  {
    final List <String> aLines = _generate ("Clean",
                                            "TOKEN : { <A : \"a\"> | <B : \"b\"> }\n" +
                                                     "void start() : {} { <A> | <B> }\n");
    _assertNotReported (aLines, "Choice conflict");
    _assertNotReported (aLines, "Warning:");
  }

  @Test
  public void testChoiceConflictNeedsMoreLookahead () throws Exception
  {
    // Both alternatives start with <A>, so one token of lookahead cannot decide between them
    final List <String> aLines = _generate ("Conflict",
                                            "TOKEN : { <A : \"a\"> | <B : \"b\"> | <C : \"c\"> }\n" +
                                                        "void start() : {} { <A> <B> | <A> <C> }\n");
    _assertReported (aLines, "Choice conflict");
    _assertReported (aLines, "Consider using a lookahead of 2");
  }

  @Test
  public void testNestedExpansionConflict () throws Exception
  {
    // The (...)* can start with <A> and so can what follows it
    final List <String> aLines = _generate ("Nested",
                                            "TOKEN : { <A : \"a\"> }\n" + "void start() : {} { ( <A> )* <A> }\n");
    _assertReported (aLines, "Choice conflict");
  }

  @Test
  public void testAnExplicitLookaheadSilencesTheWarning () throws Exception
  {
    final List <String> aLines = _generate ("Silenced",
                                            "TOKEN : { <A : \"a\"> | <B : \"b\"> | <C : \"c\"> }\n" +
                                                        "void start() : {} { LOOKAHEAD(2) <A> <B> | <A> <C> }\n");
    _assertNotReported (aLines, "Choice conflict");
  }
}
