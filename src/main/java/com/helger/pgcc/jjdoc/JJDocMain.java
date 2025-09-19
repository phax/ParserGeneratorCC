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
package com.helger.pgcc.jjdoc;

import java.io.DataInputStream;
import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.ParseException;
import com.helger.pgcc.parser.StreamProvider;

import jakarta.annotation.Nonnull;

/**
 * Main class.
 */
public final class JJDocMain
{
  private JJDocMain ()
  {}

  static void help_message ()
  {
    PGPrinter.info ();
    PGPrinter.info ("    jjdoc option-settings - (to read from standard input)");
    PGPrinter.info ("OR");
    PGPrinter.info ("    jjdoc option-settings inputfile (to read from a file)");
    PGPrinter.info ();
    PGPrinter.info ("WHERE");
    PGPrinter.info ("    \"option-settings\" is a sequence of settings separated by spaces.");
    PGPrinter.info ();

    PGPrinter.info ("Each option setting must be of one of the following forms:");
    PGPrinter.info ();
    PGPrinter.info ("    -optionname=value (e.g., -TEXT=false)");
    PGPrinter.info ("    -optionname:value (e.g., -TEXT:false)");
    PGPrinter.info ("    -optionname       (equivalent to -optionname=true.  e.g., -TEXT)");
    PGPrinter.info ("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOTEXT)");
    PGPrinter.info ();
    PGPrinter.info ("Option settings are not case-sensitive, so one can say \"-nOtExT\" instead");
    PGPrinter.info ("of \"-NOTEXT\".  Option values must be appropriate for the corresponding");
    PGPrinter.info ("option, and must be either an integer, boolean or string value.");
    PGPrinter.info ();
    PGPrinter.info ("The string valued options are:");
    PGPrinter.info ();
    PGPrinter.info ("    OUTPUT_FILE");
    PGPrinter.info ("    CSS");
    PGPrinter.info ();
    PGPrinter.info ("The boolean valued options are:");
    PGPrinter.info ();
    PGPrinter.info ("    ONE_TABLE              (default true)");
    PGPrinter.info ("    TEXT                   (default false)");
    PGPrinter.info ("    BNF                    (default false)");
    PGPrinter.info ();

    PGPrinter.info ();
    PGPrinter.info ("EXAMPLES:");
    PGPrinter.info ("    jjdoc -ONE_TABLE=false mygrammar.jj");
    PGPrinter.info ("    jjdoc - < mygrammar.jj");
    PGPrinter.info ();
    PGPrinter.info ("ABOUT JJDoc:");
    PGPrinter.info ("    JJDoc generates JavaDoc documentation from JavaCC grammar files.");
    PGPrinter.info ();
    PGPrinter.info ("    For more information, see the online JJDoc documentation at");
    PGPrinter.info ("    https://javacc.dev.java.net/doc/JJDoc.html");
  }

  /**
   * A main program that exercises the parser.
   *
   * @param args
   *        Cmdline args
   * @throws Exception
   *         in case of error
   */
  public static void main (final String [] args) throws Exception
  {
    final ESuccess errorcode = mainProgram (args);
    System.exit (errorcode.isFailure () ? 1 : 0);
  }

  /**
   * The method to call to exercise the parser from other Java programs. It
   * returns an error code. See how the main program above uses this method.
   *
   * @param args
   *        Cmdline args
   * @return {@link ESuccess}
   * @throws Exception
   *         in case of error
   */
  @SuppressWarnings ("resource")
  @Nonnull
  public static ESuccess mainProgram (final String [] args) throws Exception
  {
    Main.reInitAll ();
    JJDocOptions.init ();

    JavaCCGlobals.bannerLine ("Documentation Generator", "0.1.4");

    JavaCCParser parser = null;
    if (args.length == 0)
    {
      help_message ();
      return ESuccess.FAILURE;
    }
    PGPrinter.info ("(type \"jjdoc\" with no arguments for help)");

    if (Options.isOption (args[args.length - 1]))
    {
      PGPrinter.error ("Last argument \"" + args[args.length - 1] + "\" is not a filename or \"-\".  ");
      return ESuccess.FAILURE;
    }
    for (int arg = 0; arg < args.length - 1; arg++)
    {
      if (!Options.isOption (args[arg]))
      {
        PGPrinter.error ("Argument \"" + args[arg] + "\" must be an option setting.");
        return ESuccess.FAILURE;
      }
      Options.setCmdLineOption (args[arg]);
    }

    if (args[args.length - 1].equals ("-"))
    {
      PGPrinter.info ("Reading from standard input . . .");
      parser = new JavaCCParser (new StreamProvider (new DataInputStream (System.in), Charset.defaultCharset ()));
      JJDocGlobals.s_input_file = JJDocGlobals.STANDARD_INPUT;
      JJDocGlobals.s_output_file = JJDocGlobals.STANDARD_OUTPUT;
    }
    else
    {
      PGPrinter.info ("Reading from file " + args[args.length - 1] + " . . .");
      try
      {
        final File fp = new File (args[args.length - 1]);
        if (!fp.exists ())
        {
          PGPrinter.error ("File " + args[args.length - 1] + " not found.");
          return ESuccess.FAILURE;
        }
        if (fp.isDirectory ())
        {
          PGPrinter.error (args[args.length - 1] + " is a directory. Please use a valid file name.");
          return ESuccess.FAILURE;
        }
        JJDocGlobals.s_input_file = fp.getName ();
        final Reader aReader = FileHelper.getBufferedReader (new File (args[args.length - 1]), Options.getGrammarEncoding ());
        if (aReader == null)
        {
          PGPrinter.error ("File " + args[args.length - 1] + " not found.");
          return ESuccess.FAILURE;
        }
        parser = new JavaCCParser (new StreamProvider (aReader));
      }
      catch (final SecurityException se)
      {
        PGPrinter.error ("Security violation while trying to open " + args[args.length - 1]);
        return ESuccess.FAILURE;
      }
    }
    try
    {
      parser.javacc_input ();
      JJDoc.start ();

      if (JavaCCErrors.getErrorCount () == 0)
      {
        if (JavaCCErrors.getWarningCount () == 0)
        {
          PGPrinter.info ("Grammar documentation generated successfully in " + JJDocGlobals.s_output_file);
        }
        else
        {
          PGPrinter.info ("Grammar documentation generated with 0 errors and " + JavaCCErrors.getWarningCount () + " warnings.");
        }
        return ESuccess.SUCCESS;
      }

      PGPrinter.error ("Detected " + JavaCCErrors.getErrorCount () + " errors and " + JavaCCErrors.getWarningCount () + " warnings.");
      return ESuccess.valueOf (JavaCCErrors.getErrorCount () == 0);
    }
    catch (final MetaParseException e)
    {
      PGPrinter.error ("Detected " + JavaCCErrors.getErrorCount () + " errors and " + JavaCCErrors.getWarningCount () + " warnings.", e);
    }
    catch (final ParseException e)
    {
      PGPrinter.error ("Detected " + (JavaCCErrors.getErrorCount () + 1) + " errors and " + JavaCCErrors.getWarningCount () + " warnings.",
                       e);
    }
    return ESuccess.FAILURE;
  }
}
