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
package com.helger.pgcc.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;

/**
 * Test class for {@link OutputFile}, which decides whether a generated file may be overwritten.
 * <p>
 * This is the safety net for anybody who edits a generated file: the generator stamps a checksum of
 * the content into it, and refuses to overwrite a file whose content no longer matches. It had no
 * test, so nothing noticed if that protection stopped working.
 *
 * @author Philip Helger
 */
public final class OutputFileTest
{
  private static final File DIR = new File ("target/outputfile-test");

  @Before
  public void before ()
  {
    DIR.mkdirs ();
    PGCCContext.reset ();
    Options.init ();
  }

  /**
   * Write a file through OutputFile, exactly like the generator does.
   *
   * @param aFile
   *        The file to write. May not be <code>null</code>.
   * @param sContent
   *        The body. May not be <code>null</code>.
   * @return <code>true</code> if it was actually written.
   * @throws IOException
   *         On IO error
   */
  private static boolean _write (final File aFile, final String sContent) throws IOException
  {
    try (final OutputFile aOF = new OutputFile (aFile))
    {
      if (!aOF.needToWrite ())
        return false;
      try (final PrintWriter aPW = aOF.getPrintWriter ())
      {
        aPW.println (sContent);
      }
      return true;
    }
  }

  @Test
  public void testWritesAFileThatDoesNotExist () throws Exception
  {
    final File aFile = new File (DIR, "fresh.java");
    Files.deleteIfExists (aFile.toPath ());

    assertTrue (_write (aFile, "// hello"));
    assertTrue (aFile.exists ());

    final String s = Files.readString (aFile.toPath (), StandardCharsets.UTF_8);
    assertTrue (s, s.contains ("// hello"));
    // The checksum of the content is stamped in, so that the next run knows whether it may
    // overwrite the file
    assertTrue (s, s.contains ("OriginalChecksum="));
  }

  @Test
  public void testRebuildsItsOwnUntouchedOutput () throws Exception
  {
    final File aFile = new File (DIR, "untouched.java");
    Files.deleteIfExists (aFile.toPath ());

    assertTrue (_write (aFile, "// first"));
    // Nothing changed the file, so the generator owns it and rewrites it
    assertTrue (_write (aFile, "// second"));
    assertTrue (Files.readString (aFile.toPath (), StandardCharsets.UTF_8).contains ("// second"));
  }

  @Test
  public void testRefusesToOverwriteAnEditedFile () throws Exception
  {
    final File aFile = new File (DIR, "edited.java");
    Files.deleteIfExists (aFile.toPath ());
    assertTrue (_write (aFile, "// generated"));

    // Somebody edits the generated file
    final String sEdited = Files.readString (aFile.toPath (), StandardCharsets.UTF_8)
                                .replace ("// generated", "// generated, then edited by hand");
    Files.writeString (aFile.toPath (), sEdited, StandardCharsets.UTF_8);

    assertFalse ("An edited file must not be overwritten", _write (aFile, "// regenerated"));
    final String s = Files.readString (aFile.toPath (), StandardCharsets.UTF_8);
    assertTrue (s, s.contains ("edited by hand"));
    assertFalse (s, s.contains ("// regenerated"));
  }

  @Test
  public void testRefusesToOverwriteAFileWithoutAChecksum () throws Exception
  {
    final File aFile = new File (DIR, "foreign.java");
    Files.writeString (aFile.toPath (), "// not written by this generator\n", StandardCharsets.UTF_8);

    assertFalse ("A file the generator did not write must not be overwritten", _write (aFile, "// mine now"));
    assertEquals ("// not written by this generator\n", Files.readString (aFile.toPath (), StandardCharsets.UTF_8));
  }

  @Test
  public void testWarnsAboutAnObsoleteVersion () throws Exception
  {
    final File aFile = new File (DIR, "obsolete.java");
    // A file that claims to have been generated by an older version, with no valid checksum
    Files.writeString (aFile.toPath (),
                       "/* Generated by: ParserGeneratorCC: Do not edit this line. obsolete.java Version 0.1 */\n" +
                                        "// body\n",
                       StandardCharsets.UTF_8);

    JavaCCErrors.reInit ();
    try (final OutputFile aOF = new OutputFile (aFile, "9.9", null))
    {
      assertFalse (aOF.needToWrite ());
    }
    assertTrue ("Expected a warning about the obsolete file", JavaCCErrors.getWarningCount () > 0);
  }

  @Test
  public void testWarnsAboutIncompatibleOptions () throws Exception
  {
    final File aFile = new File (DIR, "options.java");
    // The options line records what the file was generated with; a file generated with a different
    // value for one of the options the caller cares about must not be silently reused
    Files.writeString (aFile.toPath (),
                       "/* Generated by: ParserGeneratorCC: Do not edit this line. options.java Version 9.9 */\n" +
                                        "/* ParserGeneratorCCOptions:KEEP_LINE_COLUMN=false */\n" +
                                        "// body\n",
                       StandardCharsets.UTF_8);

    JavaCCErrors.reInit ();
    try (final OutputFile aOF = new OutputFile (aFile, "9.9", new String [] { Options.USEROPTION__KEEP_LINE_COLUMN }))
    {
      assertFalse (aOF.needToWrite ());
    }
    // KEEP_LINE_COLUMN defaults to true, so the recorded "false" is incompatible
    assertTrue ("Expected a warning about incompatible options", JavaCCErrors.getWarningCount () > 0);
  }

  @Test
  public void testDoesNotWarnWhenTheOptionsMatch () throws Exception
  {
    final File aFile = new File (DIR, "options-ok.java");
    Files.writeString (aFile.toPath (),
                       "/* Generated by: ParserGeneratorCC: Do not edit this line. options-ok.java Version 9.9 */\n" +
                                        "/* ParserGeneratorCCOptions:KEEP_LINE_COLUMN=true */\n" +
                                        "// body\n",
                       StandardCharsets.UTF_8);

    JavaCCErrors.reInit ();
    try (final OutputFile aOF = new OutputFile (aFile, "9.9", new String [] { Options.USEROPTION__KEEP_LINE_COLUMN }))
    {
      assertFalse (aOF.needToWrite ());
    }
    assertEquals (0, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void testDoesNotWarnWhenTheVersionMatches () throws Exception
  {
    final File aFile = new File (DIR, "current.java");
    Files.writeString (aFile.toPath (),
                       "/* Generated by: ParserGeneratorCC: Do not edit this line. current.java Version 9.9 */\n" +
                                        "// body\n",
                       StandardCharsets.UTF_8);

    JavaCCErrors.reInit ();
    try (final OutputFile aOF = new OutputFile (aFile, "9.9", null))
    {
      assertFalse (aOF.needToWrite ());
    }
    assertEquals (0, JavaCCErrors.getWarningCount ());
  }
}
