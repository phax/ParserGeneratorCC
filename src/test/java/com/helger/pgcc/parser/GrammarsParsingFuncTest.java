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
package com.helger.pgcc.parser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.helger.base.state.ESuccess;
import com.helger.io.file.FileSystemIterator;
import com.helger.io.file.FileSystemRecursiveIterator;
import com.helger.io.file.FilenameHelper;
import com.helger.io.file.IFileFilter;
import com.helger.pgcc.jjtree.JJTree;

import jakarta.annotation.Nonnull;

public final class GrammarsParsingFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (GrammarsParsingFuncTest.class);

  private static void _parseCreatedJavaFiles (@Nonnull final File fGrammarDest,
                                              @Nonnull final Charset aCharset) throws FileNotFoundException
  {
    // Parse all created Java files
    for (final File fJava : new FileSystemIterator (fGrammarDest).withFilter (IFileFilter.filenameEndsWith (".java")))
    {
      LOGGER.info ("  Java Parsing " + fJava.getName () + " in " + aCharset.name ());
      final ParserConfiguration aCfg = new ParserConfiguration ().setCharacterEncoding (aCharset);
      final CompilationUnit aCU = new JavaParser (aCfg).parse (fJava).getResult ().get ();
      assertNotNull (aCU);
    }
  }

  @Test
  public void testParseDemoGrammars () throws Exception
  {
    final File fDest = new File ("target/grammars");
    fDest.mkdirs ();

    for (final File f : new FileSystemIterator (new File ("grammars")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      LOGGER.info ("Parsing " + f.getName ());

      final File fGrammarDest = new File (fDest, FilenameHelper.getBaseName (f));
      fGrammarDest.mkdirs ();

      final ESuccess eSuccess = Main.mainProgram (new String [] { "-OUTPUT_DIRECTORY=" + fGrammarDest.getAbsolutePath (),
                                                                  "-JDK_VERSION=1.8",
                                                                  f.getAbsolutePath () });
      assertTrue ("Failed to parse " + f.getName (), eSuccess.isSuccess ());

      _parseCreatedJavaFiles (fGrammarDest, StandardCharsets.UTF_8);
    }
  }

  @Test
  public void testParseDemoGrammarsJDK15 () throws Exception
  {
    final File fDest = new File ("target/grammars");
    fDest.mkdirs ();

    for (final File f : new FileSystemIterator (new File ("grammars")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      LOGGER.info ("Parsing " + f.getName ());

      final File fGrammarDest = new File (fDest, FilenameHelper.getBaseName (f));
      fGrammarDest.mkdirs ();

      final ESuccess eSuccess = Main.mainProgram (new String [] { "-OUTPUT_DIRECTORY=" + fGrammarDest.getAbsolutePath (),
                                                                  "-JDK_VERSION=1.5",
                                                                  f.getAbsolutePath () });
      assertTrue ("Failed to parse " + f.getName (), eSuccess.isSuccess ());

      _parseCreatedJavaFiles (fGrammarDest, StandardCharsets.UTF_8);
    }
  }

  @Test
  public void testParseDemoGrammarsModern () throws Exception
  {
    final File fDest = new File ("target/grammars");
    fDest.mkdirs ();

    for (final File f : new FileSystemIterator (new File ("grammars")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      LOGGER.info ("Parsing " + f.getName ());

      final File fGrammarDest = new File (fDest, FilenameHelper.getBaseName (f));
      fGrammarDest.mkdirs ();

      final ESuccess eSuccess = Main.mainProgram (new String [] { "-OUTPUT_DIRECTORY=" + fGrammarDest.getAbsolutePath (),
                                                                  "-JDK_VERSION=1.8",
                                                                  "-JAVA_TEMPLATE_TYPE=modern",
                                                                  f.getAbsolutePath () });
      assertTrue ("Failed to parse " + f.getName (), eSuccess.isSuccess ());

      _parseCreatedJavaFiles (fGrammarDest, StandardCharsets.UTF_8);
    }
  }

  @Test
  public void testParseDemoGrammarsJJT () throws Exception
  {
    final File fDest = new File ("target/grammars");
    fDest.mkdirs ();

    for (final File f : new FileSystemIterator (new File ("grammars")).withFilter (IFileFilter.filenameEndsWith (".jjt")))
    {
      LOGGER.info ("Parsing " + f.getName ());

      final File fGrammarDest = new File (fDest, FilenameHelper.getBaseName (f));
      fGrammarDest.mkdirs ();

      final ESuccess eSuccess = new JJTree ().main (new String [] { "-OUTPUT_DIRECTORY=" + fGrammarDest.getAbsolutePath (),
                                                                    "-JDK_VERSION=1.8",
                                                                    f.getAbsolutePath () });
      assertTrue ("Failed to parse " + f.getName (), eSuccess.isSuccess ());

      _parseCreatedJavaFiles (fGrammarDest, StandardCharsets.UTF_8);
    }
  }

  @Test
  public void testParseExamples () throws Exception
  {
    final File fDest = new File ("target/examples");
    fDest.mkdirs ();

    for (final File f : new FileSystemRecursiveIterator (new File ("examples")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      final String sBaseName = FilenameHelper.getBaseName (f);
      final String sLogName = f.getParentFile ().getName () + "/" + f.getName ();
      LOGGER.info ("Parsing " + sLogName);

      final File fGrammarDest = new File (fDest, sBaseName);
      fGrammarDest.mkdirs ();

      final ESuccess eSuccess = Main.mainProgram ("-OUTPUT_DIRECTORY=" + fGrammarDest.getAbsolutePath (),
                                                  "-JDK_VERSION=1.8",
                                                  f.getAbsolutePath ());
      assertTrue ("Failed to parse " + sLogName, eSuccess.isSuccess ());

      _parseCreatedJavaFiles (fGrammarDest, StandardCharsets.UTF_8);
    }
  }
}
