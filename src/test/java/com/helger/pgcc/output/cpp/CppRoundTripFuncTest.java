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
package com.helger.pgcc.output.cpp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileOperationManager;
import com.helger.pgcc.parser.Main;

/**
 * The C++ backend is frozen but supported, so it needs exactly one test that proves it still
 * produces something that compiles and runs. Generate a small calculator, build it with the C++
 * compiler that is on the PATH, and check the numbers it prints.
 * <p>
 * The test skips itself when no C++ compiler is available, so that it does not turn a Java only
 * environment red.
 *
 * @author Philip Helger
 */
public final class CppRoundTripFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CppRoundTripFuncTest.class);

  private static String _findCompiler ()
  {
    for (final String sCompiler : new String [] { "g++", "clang++" })
      try
      {
        if (new ProcessBuilder (sCompiler, "--version").redirectErrorStream (true).start ().waitFor () == 0)
          return sCompiler;
      }
      catch (final IOException | InterruptedException aEx)
      {
        // Try the next one
      }
    return null;
  }

  private static String _run (final File aWorkDir, final String... aCommand) throws Exception
  {
    final Process aProcess = new ProcessBuilder (aCommand).directory (aWorkDir).redirectErrorStream (true).start ();
    final String sOutput;
    try (final InputStream aIS = aProcess.getInputStream ())
    {
      sOutput = new String (aIS.readAllBytes (), StandardCharsets.UTF_8);
    }
    final int nExitCode = aProcess.waitFor ();
    assertEquals ("Command " + String.join (" ", aCommand) + " failed:\n" + sOutput, 0, nExitCode);
    return sOutput;
  }

  @Test
  public void testGenerateCompileAndRun () throws Exception
  {
    final String sCompiler = _findCompiler ();
    Assume.assumeTrue ("No C++ compiler on the PATH", sCompiler != null);
    LOGGER.info ("Using the C++ compiler '" + sCompiler + "'");

    final File aOutDir = new File ("target/cpp-roundtrip");
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aOutDir);
    aOutDir.mkdirs ();

    final ESuccess eSuccess = Main.mainProgram ("-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath (),
                                                new File ("src/test/resources/cpp/calc.jj").getAbsolutePath ());
    assertTrue ("Failed to generate the C++ parser", eSuccess.isSuccess ());
    assertTrue ("Expected C++ output, not Java", new File (aOutDir, "Calc.cc").exists ());

    FileOperationManager.INSTANCE.copyFile (new File ("src/test/resources/cpp/main.cc"), new File (aOutDir, "main.cc"));

    final List <String> aCompile = new ArrayList <> ();
    aCompile.add (sCompiler);
    aCompile.add ("-std=c++17");
    // The generated code triggers a pile of upstream warnings that are not this test's business
    aCompile.add ("-w");
    aCompile.add ("-o");
    aCompile.add ("calc");
    for (final File f : aOutDir.listFiles ())
      if (f.getName ().endsWith (".cc"))
        aCompile.add (f.getName ());
    _run (aOutDir, aCompile.toArray (new String [0]));

    final String sOutput = _run (aOutDir, "./calc");
    // 1+2+39, 2*3 + 4*10, 7
    assertEquals ("42\n46\n7\n", sOutput.replace ("\r\n", "\n"));
  }
}
