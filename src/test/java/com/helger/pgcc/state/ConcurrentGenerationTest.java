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
package com.helger.pgcc.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileOperationManager;
import com.helger.pgcc.golden.GoldenManifest;
import com.helger.pgcc.output.java.FilesJava;
import com.helger.pgcc.parser.Main;

/**
 * Two generator runs at the same time, in one JVM.
 * <p>
 * This is what moving the state out of static fields was for. Before that, running two generations
 * concurrently produced garbage, because they shared every table. The context is a
 * {@link ThreadLocal}, so each thread now has its own.
 *
 * @author Philip Helger
 */
public final class ConcurrentGenerationTest
{
  private static final File WORK_DIR = new File ("target/concurrent");

  @BeforeClass
  public static void beforeClass ()
  {
    FilesJava.setReadFromClassPath (false);
  }

  @AfterClass
  public static void afterClass ()
  {
    FilesJava.setReadFromClassPath (true);
  }

  private static GoldenManifest _generate (final String sRun, final File aGrammar, final String... aOptions)
                                                                                                            throws Exception
  {
    final File aOutDir = new File (WORK_DIR, sRun);
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aOutDir);
    aOutDir.mkdirs ();

    final List <String> aArgs = new ArrayList <> ();
    aArgs.add ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath ());
    aArgs.addAll (List.of (aOptions));
    aArgs.add (aGrammar.getAbsolutePath ());

    final ESuccess eSuccess = Main.mainProgram (aArgs.toArray (new String [0]));
    assertTrue ("Failed to generate " + sRun, eSuccess.isSuccess ());
    return GoldenManifest.ofDirectory (aOutDir);
  }

  @Test
  public void testTwoGrammarsAtOnce () throws Exception
  {
    final File aFirst = new File ("grammars/CParser.jj");
    final File aSecond = new File ("grammars/PlSql.jj");

    // What each of them looks like when nothing else is running
    final GoldenManifest aFirstAlone = _generate ("seq-first", aFirst);
    final GoldenManifest aSecondAlone = _generate ("seq-second", aSecond, "-JAVA_TEMPLATE_TYPE=modern");

    // The same two, at the same time, several times over so that they really interleave
    final ExecutorService aES = Executors.newFixedThreadPool (2);
    try
    {
      for (int nRound = 0; nRound < 4; ++nRound)
      {
        final int nCurRound = nRound;
        final Callable <GoldenManifest> aTaskA = () -> _generate ("par-first-" + nCurRound, aFirst);
        final Callable <GoldenManifest> aTaskB = () -> _generate ("par-second-" + nCurRound,
                                                                  aSecond,
                                                                  "-JAVA_TEMPLATE_TYPE=modern");
        final Future <GoldenManifest> aResA = aES.submit (aTaskA);
        final Future <GoldenManifest> aResB = aES.submit (aTaskB);

        assertEquals ("Round " + nCurRound + ", first grammar:\n" + aResA.get ().getDifferences (aFirstAlone),
                      "",
                      aResA.get ().getDifferences (aFirstAlone));
        assertEquals ("Round " + nCurRound + ", second grammar:\n" + aResB.get ().getDifferences (aSecondAlone),
                      "",
                      aResB.get ().getDifferences (aSecondAlone));
      }
    }
    finally
    {
      aES.shutdown ();
      aES.awaitTermination (5, TimeUnit.MINUTES);
    }
  }
}
