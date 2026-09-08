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
package com.helger.pgcc.output.java;

import static com.helger.pgcc.parser.JavaCCGlobals.addEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.grammar;
import static com.helger.pgcc.parser.JavaCCGlobals.printToken;
import static com.helger.pgcc.parser.JavaCCGlobals.printTokenSetup;
import static com.helger.pgcc.parser.JavaCCGlobals.printTrailingComments;
import static com.helger.pgcc.parser.JavaCCParserConstants.PACKAGE;
import static com.helger.pgcc.parser.JavaCCParserConstants.SEMICOLON;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.parser.ETokenKind;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.RegExprSpec;
import com.helger.pgcc.parser.Token;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpRStringLiteral;

/**
 * Generates the Constants file.
 */
public class OtherFilesGenJava
{
  private static final String CONSTANTS_FILENAME_SUFFIX = "Constants.java";

  private static final IJavaResourceTemplateLocations RESOURCES_JAVA_CLASSIC = new JavaResourceTemplateLocationImpl ();
  private static final IJavaResourceTemplateLocations RESOURCES_JAVA_MODERN = new JavaModernResourceTemplateLocationImpl ();

  public static void start (final boolean bIsJavaModern) throws MetaParseException
  {
    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    final IJavaResourceTemplateLocations aTemplateLoc = bIsJavaModern ? RESOURCES_JAVA_MODERN : RESOURCES_JAVA_CLASSIC;

    Token t = null;

    // Added this if condition -- 2012/10/17 -- cba
    if (Options.isGenerateJavaBoilerplateCode ())
    {
      if (bIsJavaModern)
      {
        FilesJava.gen_JavaModernFiles ();
      }

      FilesJava.gen_TokenMgrError (aTemplateLoc);
      FilesJava.gen_ParseException (aTemplateLoc);
      FilesJava.gen_Token (aTemplateLoc);
    }

    if (Options.isUserTokenManager ())
    {
      // CBA -- I think that Token managers are unique so will always be
      // generated
      FilesJava.gen_TokenManager (aTemplateLoc);
    }
    else
      if (Options.isGenerateJavaBoilerplateCode ())
      {
        FilesJava.gen_CharStream (aTemplateLoc);

        if (!Options.isJavaUserCharStream ())
        {
          if (Options.isCharSequenceCharStream ())
          {
            // Does not use the buffering of AbstractCharStream
            FilesJava.gen_CharSequenceCharStream (aTemplateLoc);
          }
          else
          {
            FilesJava.gen_AbstractCharStream (aTemplateLoc);
            if (Options.isJavaUnicodeEscape ())
              FilesJava.gen_JavaCharStream (aTemplateLoc);
            else
              FilesJava.gen_SimpleCharStream (aTemplateLoc);
          }
        }
      }

    final Writer w = FileHelper.getBufferedWriter (new File (Options.getOutputDirectory (),
                                                             grammar ().getParserName () + CONSTANTS_FILENAME_SUFFIX),
                                                   Options.getOutputEncoding ());
    if (w == null)
    {
      JavaCCErrors.semantic_error ("Could not open file " +
                                   grammar ().getParserName () +
                                   CONSTANTS_FILENAME_SUFFIX +
                                   " for writing.");
      return;
    }

    try (final PrintWriter aOstr = new PrintWriter (w))
    {
      final List <String> aTn = new ArrayList <> (grammar ().getToolNameList ());
      aTn.add (CPG.APP_NAME);

      aOstr.println ("/* " + getIdString (aTn, grammar ().getParserName () + CONSTANTS_FILENAME_SUFFIX) + " */");

      if (grammar ().cuToInsertionPoint1 ().isNotEmpty () && grammar ().cuToInsertionPoint1 ().get (0).kind == PACKAGE)
      {
        for (int i = 1; i < grammar ().cuToInsertionPoint1 ().size (); i++)
        {
          if (grammar ().cuToInsertionPoint1 ().get (i).kind == SEMICOLON)
          {
            t = grammar ().cuToInsertionPoint1 ().get (0);
            printTokenSetup (t);
            for (int j = 0; j <= i; j++)
            {
              t = grammar ().cuToInsertionPoint1 ().get (j);
              printToken (t, aOstr);
            }
            printTrailingComments (t);
            aOstr.println ();
            aOstr.println ();
            break;
          }
        }
      }
      aOstr.println ();
      aOstr.println ("/**");
      aOstr.println (" * Token literal values and constants.");
      aOstr.println (" * Generated by " + OtherFilesGenJava.class.getName () + "#start()");
      aOstr.println (" */");

      if (Options.isJavaSupportClassVisibilityPublic ())
      {
        aOstr.print ("public ");
      }
      aOstr.println ("interface " + grammar ().getParserName () + "Constants {");
      aOstr.println ();

      aOstr.println ("  /** End of File. */");
      aOstr.println ("  int EOF = 0;");
      for (final AbstractExpRegularExpression re : grammar ().orderedNameTokens ())
      {
        aOstr.println ("  /** RegularExpression Id. */");
        aOstr.println ("  int " + re.getLabel () + " = " + re.getOrdinal () + ";");
      }
      aOstr.println ();
      if (!Options.isUserTokenManager () && Options.isBuildTokenManager ())
      {
        for (int i = 0; i < LexGenJava.lexer ().getLexStateName ().length; i++)
        {
          aOstr.println ("  /** Lexical state. */");
          aOstr.println ("  int " + LexGenJava.lexer ().getLexStateName ()[i] + " = " + i + ";");
        }
        aOstr.println ();
      }
      aOstr.println ("  /** Literal token values. */");
      aOstr.println ("  String[] tokenImage = {");
      aOstr.println ("    \"<EOF>\",");

      for (final TokenProduction aTokenProduction : grammar ().rexprList ())
      {
        final TokenProduction aTp = (aTokenProduction);
        final List <RegExprSpec> aRespecs = aTp.getRespecs ();
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          final AbstractExpRegularExpression aRe = aRes.getRexp ();
          aOstr.print ("    ");
          if (aRe instanceof final ExpRStringLiteral aRStringLiteral)
          {
            aOstr.println ("\"\\\"" + addEscapes (addEscapes (aRStringLiteral.m_sImage)) + "\\\"\",");
          }
          else
            if (aRe.hasLabel ())
            {
              aOstr.println ("\"<" + aRe.getLabel () + ">\",");
            }
            else
            {
              if (aRe.m_aTpContext.getKind () == ETokenKind.TOKEN)
              {
                JavaCCErrors.warning (aRe, "Consider giving this non-string token a label for better error reporting.");
              }
              aOstr.println ("\"<token of kind " + aRe.getOrdinal () + ">\",");
            }

        }
      }
      aOstr.println ("  };");
      aOstr.println ();
      aOstr.println ("}");
    }
  }

}
