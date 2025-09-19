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
// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2006, Sun Microsystems, Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.cpp.OtherFilesGenCPP;
import com.helger.pgcc.output.java.OtherFilesGenJava;
import com.helger.pgcc.utils.EOptionType;
import com.helger.pgcc.utils.OptionInfo;

import jakarta.annotation.Nonnull;

/**
 * Entry point.
 */
public class Main
{
  private Main ()
  {}

  private static void _showHelpMessage ()
  {
    PGPrinter.info ("Usage:");
    PGPrinter.info ("    " + CPG.CMDLINE_NAME + " option-settings inputfile");
    PGPrinter.info ();
    PGPrinter.info ("\"option-settings\" is a sequence of settings separated by spaces.");
    PGPrinter.info ("Each option setting must be of one of the following forms:");
    PGPrinter.info ();
    PGPrinter.info ("    -optionname=value (e.g., -STATIC=false)");
    PGPrinter.info ("    -optionname:value (e.g., -STATIC:false)");
    PGPrinter.info ("    -optionname       (equivalent to -optionname=true.  e.g., -STATIC)");
    PGPrinter.info ("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOSTATIC)");
    PGPrinter.info ();
    PGPrinter.info ("Option settings are not case-sensitive, so one can say \"-nOsTaTiC\" instead");
    PGPrinter.info ("of \"-NOSTATIC\".  Option values must be appropriate for the corresponding");
    PGPrinter.info ("option, and must be either an integer, a boolean, or a string value.");
    PGPrinter.info ();

    // 2013/07/23 -- Changed this to auto-generate from metadata in Options so
    // that help is always in-sync with codebase
    _printOptions ();

    PGPrinter.info ("EXAMPLE:");
    PGPrinter.info ("    " + CPG.CMDLINE_NAME + " -OUTPUT_DIRECTORY=target/code -LOOKAHEAD:2 -debug_parser mygrammar.jj");
    PGPrinter.info ();
  }

  private static void _printOptions ()
  {
    final Set <OptionInfo> options = Options.getUserOptions ();

    int maxLengthInt = 0;
    int maxLengthBool = 0;
    int maxLengthString = 0;

    for (final OptionInfo i : options)
    {
      final int length = i.getName ().length ();
      switch (i.getType ())
      {
        case INTEGER:
          maxLengthInt = length > maxLengthInt ? length : maxLengthInt;
          break;
        case BOOLEAN:
          maxLengthBool = length > maxLengthBool ? length : maxLengthBool;
          break;
        case STRING:
          maxLengthString = length > maxLengthString ? length : maxLengthString;
          break;
        case OTHER:
        default:
          // Not interested
          break;
      }
    }

    if (maxLengthInt > 0)
    {
      PGPrinter.info ("The integer valued options are:");
      PGPrinter.info ();
      for (final OptionInfo i : options)
      {
        _printOptionInfo (EOptionType.INTEGER, i, maxLengthInt);
      }
      PGPrinter.info ();
    }

    if (maxLengthBool > 0)
    {
      PGPrinter.info ("The boolean valued options are:");
      PGPrinter.info ();
      for (final OptionInfo i : options)
      {
        _printOptionInfo (EOptionType.BOOLEAN, i, maxLengthBool);
      }
      PGPrinter.info ();
    }

    if (maxLengthString > 0)
    {
      PGPrinter.info ("The string valued options are:");
      PGPrinter.info ();
      for (final OptionInfo i : options)
      {
        _printOptionInfo (EOptionType.STRING, i, maxLengthString);
      }
      PGPrinter.info ();
    }
  }

  private static void _printOptionInfo (final EOptionType filter, final OptionInfo optionInfo, final int padLength)
  {
    if (optionInfo.getType () == filter)
    {
      final Object default1 = optionInfo.getDefault ();
      PGPrinter.info ("    " +
                      _padRight (optionInfo.getName (), padLength + 1) +
                      (default1 == null ? "" : ("(default : " + (default1.toString ().length () == 0 ? "<<empty>>" : default1) + ")")));
    }
  }

  private static String _padRight (final String name, final int maxLengthInt)
  {
    final int nameLength = name.length ();
    if (nameLength == maxLengthInt)
      return name;

    final int charsToPad = maxLengthInt - nameLength;
    final StringBuilder sb = new StringBuilder (charsToPad);
    sb.append (name);
    for (int i = 0; i < charsToPad; i++)
      sb.append (" ");

    return sb.toString ();
  }

  /**
   * A main program that exercises the parser. Calls <code>System.exit</code>
   * with return code 0 for success and 1 for error!
   *
   * @param args
   *        arguments to main
   * @throws IOException
   *         on IO error
   * @see #mainProgram(String...) for a version that does NOT call
   *      <code>System.exit</code>
   */
  public static void main (final String... args) throws IOException
  {
    final ESuccess eSuccess = mainProgram (args);
    System.exit (eSuccess.isSuccess () ? 0 : 1);
  }

  /**
   * The method to call to exercise the parser from other Java programs. It
   * returns an error code. See how the main program above uses this method.
   *
   * @param args
   *        main arguments
   * @return {@link ESuccess}
   * @throws IOException
   *         on IO error
   */
  @Nonnull
  public static ESuccess mainProgram (final String... args) throws IOException
  {
    // Initialize all static state
    reInitAll ();

    JavaCCGlobals.bannerLine (CPG.APP_NAME, "");

    if (args.length == 0)
    {
      PGPrinter.info ();
      _showHelpMessage ();
      return ESuccess.FAILURE;
    }
    PGPrinter.info ("(type \"" + CPG.CMDLINE_NAME + "\" with no arguments for help)");

    if (Options.isOption (args[args.length - 1]))
    {
      PGPrinter.info ("Last argument \"" + args[args.length - 1] + "\" is not a filename.");
      return ESuccess.FAILURE;
    }
    for (int arg = 0; arg < args.length - 1; arg++)
    {
      if (!Options.isOption (args[arg]))
      {
        PGPrinter.info ("Argument \"" + args[arg] + "\" must be an option setting.");
        return ESuccess.FAILURE;
      }
      Options.setCmdLineOption (args[arg]);
    }

    JavaCCParser parser = null;
    try
    {
      final File fp = new File (args[args.length - 1]);
      if (!fp.exists ())
      {
        PGPrinter.info ("File " + args[args.length - 1] + " not found.");
        return ESuccess.FAILURE;
      }
      if (fp.isDirectory ())
      {
        PGPrinter.info (args[args.length - 1] + " is a directory. Please use a valid file name.");
        return ESuccess.FAILURE;
      }

      final Reader aReader = FileHelper.getBufferedReader (new File (args[args.length - 1]), Options.getGrammarEncoding ());
      if (aReader == null)
      {
        PGPrinter.info ("File " + args[args.length - 1] + " not found.");
        return ESuccess.FAILURE;
      }
      parser = new JavaCCParser (new StreamProvider (aReader));
    }
    catch (final SecurityException se)
    {
      PGPrinter.info ("Security violation while trying to open " + args[args.length - 1]);
      return ESuccess.FAILURE;
    }

    try
    {
      PGPrinter.info ("Reading from file " + args[args.length - 1] + " ...");
      JavaCCGlobals.s_fileName = args[args.length - 1];
      JavaCCGlobals.s_origFileName = JavaCCGlobals.s_fileName;
      JavaCCGlobals.s_jjtreeGenerated = JavaCCGlobals.isGeneratedBy ("JJTree", args[args.length - 1]);
      JavaCCGlobals.s_toolNames = JavaCCGlobals.getToolNames (args[args.length - 1]);
      parser.javacc_input ();

      // 2012/05/02 - Moved this here as cannot evaluate output language
      // until the cc file has been processed. Was previously setting the 'lg'
      // variable to a lexer before the configuration override in the cc file
      // had been read.
      final EOutputLanguage eOutputLanguage = Options.getOutputLanguage ();

      // 2013/07/22 Java Modern is a
      final boolean isJavaModern = eOutputLanguage.isJava () && Options.getJavaTemplateType ().equals (Options.JAVA_TEMPLATE_TYPE_MODERN);

      JavaCCGlobals.createOutputDir (Options.getOutputDirectory ());

      if (Options.isUnicodeInput ())
      {
        NfaState.s_unicodeWarningGiven = true;
        JavaCCErrors.note ("UNICODE_INPUT option is specified. " +
                           "Please make sure you create the parser/lexer using a Reader with the correct character encoding.");
      }

      Semanticize.start ();
      final boolean isBuildParser = Options.isBuildParser ();

      // 2012/05/02 -- This is not the best way to add-in GWT support, really
      // the code needs to turn supported languages into enumerations
      // and have the enumerations describe the deltas between the outputs. The
      // current approach means that per-langauge configuration is distributed
      // and small changes between targets does not benefit from inheritance.
      switch (eOutputLanguage)
      {
        case JAVA:
          if (isBuildParser)
          {
            new ParseGenJava ().start (isJavaModern);
          }

          // Must always create the lexer object even if not building a parser.
          new LexGenJava ().start ();

          Options.setStringOption (Options.NONUSER_OPTION__PARSER_NAME, JavaCCGlobals.s_cu_name);
          OtherFilesGenJava.start (isJavaModern);
          break;
        case CPP:
          // C++ for now
          if (isBuildParser)
          {
            new ParseGenCPP ().start ();
          }
          if (isBuildParser)
          {
            new LexGenCpp ().start ();
          }
          Options.setStringOption (Options.NONUSER_OPTION__PARSER_NAME, JavaCCGlobals.s_cu_name);
          OtherFilesGenCPP.start ();
          break;
        default:
          throw new IllegalStateException ("Unhandled language!");
      }

      final int nErrors = JavaCCErrors.getErrorCount ();
      final int nWarnings = JavaCCErrors.getWarningCount ();
      if (nErrors == 0 && (isBuildParser || Options.isBuildTokenManager ()))
      {
        if (nWarnings == 0)
        {
          if (isBuildParser)
            PGPrinter.info ("Parser generated successfully.");
        }
        else
        {
          PGPrinter.info ("Parser generated with 0 errors and " + nWarnings + " warnings.");
        }
      }
      else
      {
        PGPrinter.info ("Detected " + nErrors + " error(s) and " + nWarnings + " warning(s).");
      }
      return ESuccess.valueOf (nErrors == 0);
    }
    catch (final MetaParseException e)
    {
      PGPrinter.error ("Detected " + JavaCCErrors.getErrorCount () + " errors and " + JavaCCErrors.getWarningCount () + " warnings.");
    }
    catch (final ParseException e)
    {
      PGPrinter.error ("Detected " + (JavaCCErrors.getErrorCount () + 1) + " errors and " + JavaCCErrors.getWarningCount () + " warnings.",
                       e);
    }
    return ESuccess.FAILURE;
  }

  public static void reInitAll ()
  {
    com.helger.pgcc.parser.exp.Expansion.reInit ();
    com.helger.pgcc.parser.JavaCCErrors.reInit ();
    com.helger.pgcc.parser.JavaCCGlobals.reInitStatic ();
    com.helger.pgcc.parser.Options.init ();
    com.helger.pgcc.parser.JavaCCParserInternals.reInit ();
    com.helger.pgcc.parser.exp.ExpRStringLiteral.reInit ();
    com.helger.pgcc.output.java.FilesJava.reInit ();
    com.helger.pgcc.parser.NfaState.reInit ();
    com.helger.pgcc.parser.MatchInfo.reInitStatic ();
    com.helger.pgcc.parser.LookaheadWalk.reInit ();
    com.helger.pgcc.parser.Semanticize.reInit ();
    com.helger.pgcc.output.java.OtherFilesGenJava.reInit ();
    com.helger.pgcc.parser.LexGenJava.reInit ();
  }
}
