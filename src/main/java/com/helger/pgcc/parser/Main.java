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
package com.helger.pgcc.parser;

import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.base.state.ESuccess;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.cpp.LexGenCpp;
import com.helger.pgcc.output.cpp.OtherFilesGenCPP;
import com.helger.pgcc.output.cpp.ParseGenCPP;
import com.helger.pgcc.output.java.LexGenJava;
import com.helger.pgcc.output.java.OtherFilesGenJava;
import com.helger.pgcc.output.java.ParseGenJava;
import com.helger.pgcc.utils.EOptionType;
import com.helger.pgcc.utils.OptionInfo;

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
    PGPrinter.info ("    " +
                    CPG.CMDLINE_NAME +
                    " -OUTPUT_DIRECTORY=target/code -LOOKAHEAD:2 -debug_parser mygrammar.jj");
    PGPrinter.info ();
  }

  private static void _printOptions ()
  {
    final Set <OptionInfo> aOptions = Options.getUserOptions ();

    int nMaxLengthInt = 0;
    int nMaxLengthBool = 0;
    int nMaxLengthString = 0;

    for (final OptionInfo i : aOptions)
    {
      final int nLength = i.name ().length ();
      switch (i.type ())
      {
        case INTEGER -> nMaxLengthInt = Math.max (nLength, nMaxLengthInt);
        case BOOLEAN -> nMaxLengthBool = Math.max (nLength, nMaxLengthBool);
        case STRING -> nMaxLengthString = Math.max (nLength, nMaxLengthString);
        // OTHER is not printed
        default ->
            {
            }
      }
    }

    if (nMaxLengthInt > 0)
    {
      PGPrinter.info ("The integer valued options are:");
      PGPrinter.info ();
      for (final OptionInfo i : aOptions)
      {
        _printOptionInfo (EOptionType.INTEGER, i, nMaxLengthInt);
      }
      PGPrinter.info ();
    }

    if (nMaxLengthBool > 0)
    {
      PGPrinter.info ("The boolean valued options are:");
      PGPrinter.info ();
      for (final OptionInfo i : aOptions)
      {
        _printOptionInfo (EOptionType.BOOLEAN, i, nMaxLengthBool);
      }
      PGPrinter.info ();
    }

    if (nMaxLengthString > 0)
    {
      PGPrinter.info ("The string valued options are:");
      PGPrinter.info ();
      for (final OptionInfo i : aOptions)
      {
        _printOptionInfo (EOptionType.STRING, i, nMaxLengthString);
      }
      PGPrinter.info ();
    }
  }

  private static void _printOptionInfo (final EOptionType eFilter,
                                        @NonNull final OptionInfo aOptionInfo,
                                        final int nPadLength)
  {
    if (aOptionInfo.type () == eFilter)
    {
      final Object aDefault = aOptionInfo.defaultValue ();
      PGPrinter.info ("    " +
                      _padRight (aOptionInfo.name (), nPadLength + 1) +
                      (aDefault == null ? ""
                                        : ("(default : " +
                                           (aDefault.toString ().length () == 0 ? "<<empty>>" : aDefault) +
                                           ")")));
    }
  }

  private static String _padRight (@NonNull final String sName, final int nMaxLengthInt)
  {
    final int nCharsToPad = nMaxLengthInt - sName.length ();
    return nCharsToPad <= 0 ? sName : sName + " ".repeat (nCharsToPad);
  }

  /**
   * A main program that exercises the parser. Calls <code>System.exit</code> with return code 0 for
   * success and 1 for error!
   *
   * @param aArgs
   *        arguments to main
   * @throws IOException
   *         on IO error
   * @see #mainProgram(String...) for a version that does NOT call <code>System.exit</code>
   */
  public static void main (final String... aArgs) throws IOException
  {
    final ESuccess eSuccess = mainProgram (aArgs);
    System.exit (eSuccess.isSuccess () ? 0 : 1);
  }

  /**
   * The method to call to exercise the parser from other Java programs. It returns an error code.
   * See how the main program above uses this method.
   *
   * @param aArgs
   *        main arguments
   * @return {@link ESuccess}
   * @throws IOException
   *         on IO error
   */
  @NonNull
  public static ESuccess mainProgram (@NonNull final String... aArgs) throws IOException
  {
    // Initialize all static state
    reInitAll ();

    JavaCCGlobals.bannerLine (CPG.APP_NAME, "");

    if (aArgs.length == 0)
    {
      PGPrinter.info ();
      _showHelpMessage ();
      return ESuccess.FAILURE;
    }
    PGPrinter.info ("(type \"" + CPG.CMDLINE_NAME + "\" with no arguments for help)");

    if (Options.isOption (aArgs[aArgs.length - 1]))
    {
      PGPrinter.info ("Last argument \"" + aArgs[aArgs.length - 1] + "\" is not a filename.");
      return ESuccess.FAILURE;
    }
    for (int nArg = 0; nArg < aArgs.length - 1; nArg++)
    {
      if (!Options.isOption (aArgs[nArg]))
      {
        PGPrinter.info ("Argument \"" + aArgs[nArg] + "\" must be an option setting.");
        return ESuccess.FAILURE;
      }
      Options.setCmdLineOption (aArgs[nArg]);
    }

    JavaCCParser aParser = null;
    try
    {
      final File aFp = new File (aArgs[aArgs.length - 1]);
      if (!aFp.exists ())
      {
        PGPrinter.info ("File " + aArgs[aArgs.length - 1] + " not found.");
        return ESuccess.FAILURE;
      }
      if (aFp.isDirectory ())
      {
        PGPrinter.info (aArgs[aArgs.length - 1] + " is a directory. Please use a valid file name.");
        return ESuccess.FAILURE;
      }

      final Reader aReader = FileHelper.getBufferedReader (new File (aArgs[aArgs.length - 1]),
                                                           Options.getGrammarEncoding ());
      if (aReader == null)
      {
        PGPrinter.info ("File " + aArgs[aArgs.length - 1] + " not found.");
        return ESuccess.FAILURE;
      }
      aParser = new JavaCCParser (new StreamProvider (aReader));
    }
    catch (final SecurityException aSe)
    {
      PGPrinter.info ("Security violation while trying to open " + aArgs[aArgs.length - 1]);
      return ESuccess.FAILURE;
    }

    try
    {
      PGPrinter.info ("Reading from file " + aArgs[aArgs.length - 1] + " ...");
      grammar ().setFileName (aArgs[aArgs.length - 1]);
      grammar ().setOrigFileName (grammar ().getFileName ());
      grammar ().setJJTreeGenerated (JavaCCGlobals.isGeneratedBy ("JJTree", aArgs[aArgs.length - 1]));
      grammar ().setToolNameList (JavaCCGlobals.getToolNames (aArgs[aArgs.length - 1]));
      aParser.javacc_input ();

      // 2012/05/02 - Moved this here as cannot evaluate output language
      // until the cc file has been processed. Was previously setting the 'lg'
      // variable to a lexer before the configuration override in the cc file
      // had been read.
      final EOutputLanguage eOutputLanguage = Options.getOutputLanguage ();

      // 2013/07/22 Java Modern is a
      final boolean bIsJavaModern = eOutputLanguage.isJava () &&
                                    Options.getJavaTemplateType ().equals (Options.JAVA_TEMPLATE_TYPE_MODERN);

      JavaCCGlobals.createOutputDir (Options.getOutputDirectory ());

      if (Options.isUnicodeInput ())
      {
        NfaState.nfa ().setUnicodeWarningGiven (true);
        JavaCCErrors.note ("UNICODE_INPUT option is specified. " +
                           "Please make sure you create the parser/lexer using a Reader with the correct character encoding.");
      }

      Semanticize.start ();
      final boolean bIsBuildParser = Options.isBuildParser ();

      // 2012/05/02 -- This is not the best way to add-in GWT support, really
      // the code needs to turn supported languages into enumerations
      // and have the enumerations describe the deltas between the outputs. The
      // current approach means that per-langauge configuration is distributed
      // and small changes between targets does not benefit from inheritance.
      switch (eOutputLanguage)
      {
        case JAVA:
          if (bIsBuildParser)
          {
            new ParseGenJava ().start (bIsJavaModern);
          }

          // Must always create the lexer object even if not building a parser.
          new LexGenJava ().start ();

          Options.setStringOption (Options.NONUSER_OPTION__PARSER_NAME, grammar ().getParserName ());
          OtherFilesGenJava.start (bIsJavaModern);
          break;
        case CPP:
          // C++ for now
          if (bIsBuildParser)
          {
            new ParseGenCPP ().start ();
            new LexGenCpp ().start ();
          }
          Options.setStringOption (Options.NONUSER_OPTION__PARSER_NAME, grammar ().getParserName ());
          OtherFilesGenCPP.start ();
          break;
        default:
          throw new IllegalStateException ("Unhandled language!");
      }

      final int nErrors = JavaCCErrors.getErrorCount ();
      final int nWarnings = JavaCCErrors.getWarningCount ();
      if (nErrors == 0 && (bIsBuildParser || Options.isBuildTokenManager ()))
      {
        if (nWarnings == 0)
        {
          if (bIsBuildParser)
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
      PGPrinter.error ("Detected " +
                       JavaCCErrors.getErrorCount () +
                       " errors and " +
                       JavaCCErrors.getWarningCount () +
                       " warnings.");
    }
    catch (final ParseException e)
    {
      PGPrinter.error ("Detected " +
                       (JavaCCErrors.getErrorCount () + 1) +
                       " errors and " +
                       JavaCCErrors.getWarningCount () +
                       " warnings.",
                       e);
    }
    return ESuccess.FAILURE;
  }

  /**
   * Start a fresh generator run on the current thread. Everything the generator knows lives in
   * {@link com.helger.pgcc.context.PGCCContext} now, so this drops the whole context and refills
   * the option defaults.
   */
  public static void reInitAll ()
  {
    com.helger.pgcc.context.PGCCContext.reset ();
    com.helger.pgcc.parser.Options.init ();
  }
}
