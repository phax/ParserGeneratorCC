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
package com.helger.pgcc.jjdoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.jspecify.annotations.NonNull;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.base.state.ESuccess;

/**
 * Test class for the four JJDoc output formats. {@link JJDocMainTest} only checked that a run
 * succeeds, so nothing noticed if a generator produced an empty or malformed document.
 *
 * @author Philip Helger
 */
public final class JJDocOutputTest
{
  private static final File DIR = new File ("target/jjdoc-output");
  private static final File GRAMMAR = new File (DIR, "sample.jj");

  @BeforeClass
  public static void beforeClass () throws Exception
  {
    DIR.mkdirs ();
    Files.write (GRAMMAR.toPath (),
                 ("PARSER_BEGIN(Sample)\n" +
                  "public class Sample {}\n" +
                  "PARSER_END(Sample)\n" +
                  "\n" +
                  "SKIP : { \" \" }\n" +
                  "\n" +
                  "TOKEN :\n" +
                  "{ < PLUS : \"+\" >\n" +
                  "| < NUMBER : ([\"0\"-\"9\"])+ >\n" +
                  "}\n" +
                  "\n" +
                  "void sum() : {}\n" +
                  "{ <NUMBER> ( <PLUS> <NUMBER> )* <EOF> }\n").getBytes (StandardCharsets.UTF_8));
  }

  /**
   * Run JJDoc over the sample grammar.
   *
   * @param sOutputName
   *        Name of the file to write. May not be <code>null</code>.
   * @param aOptions
   *        Extra options. May not be <code>null</code>.
   * @return The generated document. Never <code>null</code>.
   * @throws Exception
   *         On error
   */
  @NonNull
  private static String _run (final String sOutputName, final String... aOptions) throws Exception
  {
    final File aOut = new File (DIR, sOutputName);
    Files.deleteIfExists (aOut.toPath ());

    final String [] aArgs = new String [aOptions.length + 2];
    aArgs[0] = "-OUTPUT_FILE:" + aOut.getAbsolutePath ();
    System.arraycopy (aOptions, 0, aArgs, 1, aOptions.length);
    aArgs[aArgs.length - 1] = GRAMMAR.getAbsolutePath ();

    assertEquals (ESuccess.SUCCESS, JJDocMain.mainProgram (aArgs));
    assertTrue ("JJDoc did not write " + aOut, aOut.exists ());
    return Files.readString (aOut.toPath (), StandardCharsets.UTF_8);
  }

  @Test
  public void testHtmlIsTheDefault () throws Exception
  {
    final String s = _run ("sample.html");
    assertTrue (s, s.contains ("<HTML>") || s.contains ("<html>"));
    // The productions and the tokens have to show up
    assertTrue (s, s.contains ("sum"));
    assertTrue (s, s.contains ("NUMBER"));
    assertTrue (s, s.contains ("PLUS"));
  }

  @Test
  public void testText () throws Exception
  {
    final String s = _run ("sample.txt", "-TEXT:true");
    assertTrue (s, s.contains ("DOCUMENT START"));
    assertTrue (s, s.contains ("DOCUMENT END"));
    assertTrue (s, s.contains ("sum"));
    assertTrue (s, s.contains ("NUMBER"));
    // Plain text, so no markup
    assertTrue (s, !s.contains ("<HTML>"));
  }

  @Test
  public void testBnf () throws Exception
  {
    final String s = _run ("sample.bnf", "-BNF:true");
    assertTrue (s, s.contains ("sum ::="));
    assertTrue (s, !s.contains ("<HTML>"));

    // Note what is NOT in there: BNFGenerator.reStart switches printing off for ExpRJustName and
    // ExpRCharacterList, so every reference to a named token disappears from the production and
    // "sum ::= ( )* <EOF>" is what comes out. That is inherited from upstream JavaCC and is pinned
    // here as the current behaviour, not endorsed as correct.
    assertTrue (s, !s.contains ("NUMBER"));
    assertTrue (s, !s.contains ("PLUS"));
  }

  @Test
  public void testXText () throws Exception
  {
    final String s = _run ("sample.xtext", "-XTEXT:true");
    assertTrue (s, s.contains ("grammar "));
    assertTrue (s, s.contains ("org.eclipse.xtext.common.Terminals"));
    // The rule body is emitted, but without the rule name in front of it - pinned as the current
    // behaviour
    assertTrue (s, s.contains ("<NUMBER>"));
  }

  @Test
  public void testTheFourFormatsDifferFromEachOther () throws Exception
  {
    final String sHtml = _run ("cmp.html");
    final String sText = _run ("cmp.txt", "-TEXT:true");
    final String sBnf = _run ("cmp.bnf", "-BNF:true");
    final String sXText = _run ("cmp.xtext", "-XTEXT:true");

    assertTrue (!sHtml.equals (sText));
    assertTrue (!sText.equals (sBnf));
    assertTrue (!sBnf.equals (sXText));
    assertTrue (!sXText.equals (sHtml));
  }
}
