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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.jspecify.annotations.NonNull;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.base.state.ESuccess;
import com.helger.pgcc.jjdoc.test.JJDocMainTest;

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
                  "{ <NUMBER> ( <PLUS> <NUMBER> )* <EOF> }\n" +
                  "\n" +
                  // A second production, so that a reference to a non terminal is covered too
                  "void expr() : {}\n" +
                  "{ sum() ( <PLUS> sum() )* }\n").getBytes (StandardCharsets.UTF_8));
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

    // HTML5, not the HTML 3.2 this used to emit
    assertTrue (s, s.startsWith ("<!DOCTYPE html>"));
    assertTrue (s, s.contains ("<html lang=\"en\">"));
    assertTrue (s, s.contains ("<meta charset="));

    // The productions and the tokens have to show up
    assertTrue (s, s.contains ("sum"));
    assertTrue (s, s.contains ("NUMBER"));
    assertTrue (s, s.contains ("PLUS"));

    // Anchors are ids, and the layout is a stylesheet rather than an attribute on every cell
    assertTrue (s, s.contains ("id=\"prod1\""));
    assertFalse (s, s.contains ("<A NAME"));
    assertFalse (s, s.contains ("ALIGN="));
    assertFalse (s, s.contains ("VALIGN="));

    // A token production with nothing to show used to leave an empty row behind
    assertFalse (s, s.contains ("<pre>\n   </pre>"));
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
    assertTrue (s, !s.contains ("<HTML>"));

    // The whole production, terminals included. Up to and including 2.0.3 the terminals were
    // dropped and this read "sum ::= ( )* <EOF>"
    assertTrue (s, s.contains ("sum ::= <NUMBER> ( <PLUS> <NUMBER> )* <EOF>"));

    // The .bnf format lists the productions only, so a token definition does not appear - the
    // character class of NUMBER is nowhere in the file
    assertTrue (s, !s.contains ("\"0\"-\"9\""));
  }

  @Test
  public void testXText () throws Exception
  {
    final String s = _run ("sample.xtext", "-XTEXT:true");
    assertTrue (s, s.contains ("grammar "));
    assertTrue (s, s.contains ("org.eclipse.xtext.common.Terminals"));
    assertTrue (s, s.contains ("<NUMBER>"));

    // Pinned as the current behaviour, and it is not valid Xtext. Three things are wrong and none
    // of them is a small fix:
    // - XTextGenerator.productionStart is empty, so a rule is emitted as a bare body with no name
    // in front of it
    // - nonTerminalStart prints "terminal " and nonTerminalEnd prints ";" around a name that JJDoc
    // has already emitted through text (), so a reference to sum comes out as "terminal sum;"
    // - token productions produce no terminal rules at all, and token references keep the JavaCC
    // angle brackets
    // Making this correct means designing a JavaCC to Xtext mapping and validating it against
    // Xtext, which is a different job from fixing a bug. Whoever picks it up should start here
    assertTrue (s, !s.contains ("sum:"));
    assertTrue (s, s.contains ("terminal sum;"));
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
