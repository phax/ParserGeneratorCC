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
package com.helger.pgcc.output.cpp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.utils.OutputFileGenerator;

/**
 * Generate CharStream, TokenManager and Exceptions.
 */
public class FilesCpp
{
  private FilesCpp ()
  {}

  private static void genFile (final String dir, final String name, final String version, final String [] parameters)
  {
    final File file = new File (Options.getOutputDirectory (), name);
    try (final OutputFile outputFile = new OutputFile (file, version, parameters))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        final OutputFileGenerator generator = new OutputFileGenerator ("/templates/" + dir + "/" + name + ".template",
                                                                       Options.getAllOptions ());
        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      PGPrinter.error ("Failed to create file: " + file, e);
      JavaCCErrors.semantic_error ("Could not open file: " + file + " for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_CharStream ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("stream/cpp", "CharStream.h", PGVersion.MAJOR_DOT_MINOR, parameters);
    genFile ("stream/cpp", "CharStream.cc", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void gen_ParseException ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("cpp", "ParseException.h", PGVersion.MAJOR_DOT_MINOR, parameters);
    genFile ("cpp", "ParseException.cc", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void gen_TokenMgrError ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("cpp", "TokenMgrError.h", PGVersion.MAJOR_DOT_MINOR, parameters);
    genFile ("cpp", "TokenMgrError.cc", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void gen_Token ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC,
                                                 Options.USEROPTION__CPP_TOKEN_INCLUDES,
                                                 Options.USEROPTION__TOKEN_EXTENDS };
    genFile ("cpp", "Token.h", PGVersion.MAJOR_DOT_MINOR, parameters);
    genFile ("cpp", "Token.cc", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void gen_TokenManager ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("cpp", "TokenManager.h", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void gen_JavaCCDefs ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("cpp", "JavaCC.h", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void gen_ErrorHandler ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC,
                                                 Options.USEROPTION__BUILD_PARSER,
                                                 Options.USEROPTION__BUILD_TOKEN_MANAGER };
    genFile ("cpp", "ErrorHandler.h", PGVersion.MAJOR_DOT_MINOR, parameters);
  }

  public static void reInit ()
  {
    // empty
  }
}
