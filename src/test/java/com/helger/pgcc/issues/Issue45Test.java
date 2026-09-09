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
package com.helger.pgcc.issues;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.io.file.SimpleFileIO;
import com.helger.pgcc.jjtree.JJTree;

/**
 * Test for <a href="https://github.com/tulipcc/ParserGeneratorCC/issues/45">issue 45</a>: the
 * JJTree state must be reset in the <code>ReInit</code> methods of a parser that was generated from
 * a JJTree generated grammar file.
 *
 * @author Philip Helger
 */
public final class Issue45Test
{
  @Test
  public void testJJTreeStateIsResetOnReInit () throws IOException
  {
    final File aSrc = new File ("src/test/resources/issues/45/grammar.jjt");
    final File aOutDir = new File ("target/issue45");

    // Step 1: JJTree creates the annotated grammar
    new JJTree ().main (new String [] { "-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath (), aSrc.getAbsolutePath () });

    // Step 2: JavaCC creates the parser from the annotated grammar
    final File aGrammar = new File (aOutDir, "grammar.jj");
    com.helger.pgcc.parser.Main.mainProgram ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath (),
                                             aGrammar.getAbsolutePath ());

    // Step 3: all ReInit methods must reset the JJTree state
    final String sParser = SimpleFileIO.getFileAsString (new File (aOutDir, "Issue45Parser.java"),
                                                         StandardCharsets.UTF_8);
    assertTrue ("Parser was not generated", sParser != null);

    int nReInit = 0;
    int nReset = 0;
    for (final String sLine : sParser.split ("\n"))
    {
      if (sLine.contains ("void ReInit("))
        nReInit++;
      if (sLine.contains ("jjtree.reset()"))
        nReset++;
    }
    assertTrue ("Expected at least one ReInit method but found " + nReInit, nReInit > 0);
    assertTrue ("Expected " + nReInit + " calls to jjtree.reset() but found " + nReset, nReset == nReInit);
  }
}
