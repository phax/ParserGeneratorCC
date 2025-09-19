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
package com.helger.pgcc.output.java;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FileSystemIterator;
import com.helger.io.file.IFileFilter;
import com.helger.pgcc.parser.Options;

/**
 * Test class that creates the Java template files to disk and tries to parse
 * them with the JavaParser
 *
 * @author Philip Helger
 */
public final class JavaTemplateValidityFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (JavaTemplateValidityFuncTest.class);

  @BeforeClass
  public static void before ()
  {
    Options.init ();
    // Read from file - to avoid reading template from old PGCC version
    FilesJava.setReadFromClassPath (false);
  }

  @AfterClass
  public static void afterClass ()
  {
    // Restore default!
    FilesJava.setReadFromClassPath (true);
  }

  @Test
  public void testGenerateAndCompile ()
  {
    final File aTargetDir = new File ("target/templates/java").getAbsoluteFile ();
    Options.setInputFileOption (null, null, Options.USEROPTION__OUTPUT_DIRECTORY, aTargetDir.getAbsolutePath ());

    for (int nClassicOrModern = 0; nClassicOrModern <= 1; ++nClassicOrModern)
    {
      final boolean bIsModern = nClassicOrModern == 0;
      final IJavaResourceTemplateLocations aLocation = bIsModern ? new JavaModernResourceTemplateLocationImpl ()
                                                                 : new JavaResourceTemplateLocationImpl ();
      final String sMode = bIsModern ? "modern " : "classic ";

      for (int nKeepLineColumn = 0; nKeepLineColumn <= 1; ++nKeepLineColumn)
      {
        final boolean bKeepLineColumn = nKeepLineColumn != 0;
        Options.setStringOption (Options.USEROPTION__KEEP_LINE_COLUMN, Boolean.toString (bKeepLineColumn));
        final String sKeepLineColumn = bKeepLineColumn ? "[keep line/col] " : "";

        // Create files
        FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aTargetDir);
        FileOperationManager.INSTANCE.createDirRecursive (aTargetDir);

        if (bIsModern)
          FilesJava.gen_JavaModernFiles ();

        FilesJava.gen_ParseException (aLocation);
        FilesJava.gen_Token (aLocation);
        FilesJava.gen_TokenManager (aLocation);
        FilesJava.gen_TokenMgrError (aLocation);

        FilesJava.gen_CharStream (aLocation);
        FilesJava.gen_AbstractCharStream (aLocation);
        FilesJava.gen_JavaCharStream (aLocation);
        FilesJava.gen_SimpleCharStream (aLocation);

        // Now try compiling the files
        for (final File aFile : new FileSystemIterator (aTargetDir).withFilter (IFileFilter.fileOnly ()
                                                                                           .and (IFileFilter.filenameEndsWith (".java"))))
        {
          LOGGER.info ("Parsing " + sMode + sKeepLineColumn + aFile.getName ());
          final ParseResult <CompilationUnit> aResult = new JavaParser ().parse (FileHelper.getInputStream (aFile),
                                                                                 Options.getOutputEncoding ());
          assertTrue (aResult.getProblems ().toString (), aResult.isSuccessful ());
        }
      }
    }

  }
}
