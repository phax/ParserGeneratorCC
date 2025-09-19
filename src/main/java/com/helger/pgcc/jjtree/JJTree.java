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

package com.helger.pgcc.jjtree;

import java.io.IOException;

import com.helger.base.state.ESuccess;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.jjtree.output.JJTreeStateCpp;
import com.helger.pgcc.jjtree.output.JJTreeStateJava;
import com.helger.pgcc.jjtree.output.NodeFilesCpp;
import com.helger.pgcc.jjtree.output.NodeFilesJava;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.Options;

import jakarta.annotation.Nonnull;

public class JJTree
{
  private JJTreeIO io;

  private void help_message ()
  {
    PGPrinter.info ("Usage:");
    PGPrinter.info ("    jjtree option-settings inputfile");
    PGPrinter.info ("");
    PGPrinter.info ("\"option-settings\" is a sequence of settings separated by spaces.");
    PGPrinter.info ("Each option setting must be of one of the following forms:");
    PGPrinter.info ("");
    PGPrinter.info ("    -optionname=value (e.g., -STATIC=false)");
    PGPrinter.info ("    -optionname:value (e.g., -STATIC:false)");
    PGPrinter.info ("    -optionname       (equivalent to -optionname=true.  e.g., -STATIC)");
    PGPrinter.info ("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOSTATIC)");
    PGPrinter.info ("");
    PGPrinter.info ("Option settings are not case-sensitive, so one can say \"-nOsTaTiC\" instead");
    PGPrinter.info ("of \"-NOSTATIC\".  Option values must be appropriate for the corresponding");
    PGPrinter.info ("option, and must be either an integer or a string value.");
    PGPrinter.info ("");

    PGPrinter.info ("The boolean valued options are:");
    PGPrinter.info ("");
    PGPrinter.info ("    MULTI                    (default false)");
    PGPrinter.info ("    NODE_DEFAULT_VOID        (default false)");
    PGPrinter.info ("    NODE_SCOPE_HOOK          (default false)");
    PGPrinter.info ("    NODE_USES_PARSER         (default false)");
    PGPrinter.info ("    BUILD_NODE_FILES         (default true)");
    PGPrinter.info ("    TRACK_TOKENS             (default false)");
    PGPrinter.info ("    VISITOR                  (default false)");
    PGPrinter.info ("");
    PGPrinter.info ("The string valued options are:");
    PGPrinter.info ("");
    PGPrinter.info ("    JDK_VERSION              (default \"1.5\")");
    PGPrinter.info ("    NODE_CLASS               (default \"\")");
    PGPrinter.info ("    NODE_PREFIX              (default \"AST\")");
    PGPrinter.info ("    NODE_PACKAGE             (default \"\")");
    PGPrinter.info ("    NODE_EXTENDS             (default \"\")");
    PGPrinter.info ("    NODE_FACTORY             (default \"\")");
    PGPrinter.info ("    OUTPUT_FILE              (default remove input file suffix, add .jj)");
    PGPrinter.info ("    OUTPUT_DIRECTORY         (default \"\")");
    PGPrinter.info ("    JJTREE_OUTPUT_DIRECTORY  (default value of OUTPUT_DIRECTORY option)");
    PGPrinter.info ("    VISITOR_DATA_TYPE        (default \"\")");
    PGPrinter.info ("    VISITOR_RETURN_TYPE      (default \"Object\")");
    PGPrinter.info ("    VISITOR_EXCEPTION        (default \"\")");
    PGPrinter.info ("");
    PGPrinter.info ("JJTree also accepts JavaCC options, which it inserts into the generated file.");
    PGPrinter.info ("");

    PGPrinter.info ("EXAMPLES:");
    PGPrinter.info ("    jjtree -STATIC=false mygrammar.jjt");
    PGPrinter.info ("");
    PGPrinter.info ("ABOUT JJTree:");
    PGPrinter.info ("    JJTree is a preprocessor for JavaCC that inserts actions into a");
    PGPrinter.info ("    JavaCC grammar to build parse trees for the input.");
    PGPrinter.info ("");
    PGPrinter.info ("    For more information, see the online JJTree documentation at ");
    PGPrinter.info ("    https://javacc.dev.java.net/doc/JJTree.html ");
    PGPrinter.info ("");
  }

  /**
   * A main program that exercises the parser.
   *
   * @param args
   *        commandline arguments
   * @return {@link ESuccess}
   */
  @Nonnull
  public ESuccess main (final String [] args)
  {
    // initialize static state for allowing repeat runs without exiting
    ASTNodeDescriptor.reInit ();
    com.helger.pgcc.parser.Main.reInitAll ();

    JavaCCGlobals.bannerLine ("Tree Builder", "");

    io = new JJTreeIO ();
    try
    {

      initializeOptions ();
      if (args.length == 0)
      {
        PGPrinter.info ("");
        help_message ();
        return ESuccess.FAILURE;
      }
      PGPrinter.info ("(type \"jjtree\" with no arguments for help)");

      final String fn = args[args.length - 1];
      if (Options.isOption (fn))
      {
        PGPrinter.info ("Last argument \"" + fn + "\" is not a filename");
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

      JJTreeOptions.validate ();

      try
      {
        io.setInput (fn);
      }
      catch (final IOException ioe)
      {
        PGPrinter.info ("Error setting input: " + ioe.getMessage ());
        return ESuccess.FAILURE;
      }
      PGPrinter.info ("Reading from file " + io.getInputFilename () + " . . .");

      JJTreeGlobals.toolList.clear ();
      JJTreeGlobals.toolList.addAll (JavaCCGlobals.getToolNames (fn));
      JJTreeGlobals.toolList.add ("JJTree");

      try
      {
        final JJTreeParser parser = new JJTreeParser (new StreamProvider (io.getIn ()));
        parser.javacc_input ();

        final ASTGrammar root = (ASTGrammar) parser.jjtree.rootNode ();
        if (Boolean.getBoolean ("jjtree-dump"))
        {
          root.dump (" ");
        }
        try
        {
          io.setOutput ();
        }
        catch (final IOException ioe)
        {
          PGPrinter.info ("Error setting output: " + ioe.getMessage ());
          return ESuccess.FAILURE;
        }
        root.generate (io);
        io.getOut ().close ();

        // TODO :: Not yet tested this in GWT/Modern mode (disabled by default
        // in 6.1)
        switch (Options.getOutputLanguage ())
        {
          case JAVA:
            NodeFilesJava.generateTreeConstants_java ();
            NodeFilesJava.generateVisitor_java ();
            NodeFilesJava.generateDefaultVisitor_java ();
            JJTreeStateJava.generateTreeState_java ();
            break;
          case CPP:
            NodeFilesCpp.generateTreeConstants ();
            NodeFilesCpp.generateVisitors ();
            JJTreeStateCpp.generateTreeState ();
            break;
          default:
            PGPrinter.info ("Unsupported JJTree output language : " + Options.getOutputLanguage ());
            return ESuccess.FAILURE;
        }

        PGPrinter.info ("Annotated grammar generated successfully in " + io.getOutputFilename ());
        return ESuccess.SUCCESS;
      }
      catch (final ParseException pe)
      {
        PGPrinter.error ("Error parsing input: " + pe.toString ());
      }
      catch (final Exception e)
      {
        PGPrinter.error ("Error parsing input", e);
      }
      return ESuccess.FAILURE;
    }
    finally
    {
      io.closeAll ();
    }
  }

  /**
   * Initialize for JJTree
   */
  private void initializeOptions ()
  {
    JJTreeOptions.init ();
    JJTreeGlobals.initialize ();
  }
}
