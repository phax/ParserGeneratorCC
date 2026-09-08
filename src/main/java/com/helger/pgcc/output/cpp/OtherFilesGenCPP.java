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

import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.grammar;
import static com.helger.pgcc.parser.JavaCCGlobals.printToken;
import static com.helger.pgcc.parser.JavaCCGlobals.printTokenSetup;
import static com.helger.pgcc.parser.JavaCCGlobals.printTrailingComments;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.output.java.LexGenJava;
import com.helger.pgcc.parser.ETokenKind;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCParserConstants;
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
public class OtherFilesGenCPP
{
  // Used by the CPP code generatror
  public static void printCharArray (final PrintWriter aOstr, final String s)
  {
    aOstr.print ("{ ");
    for (int i = 0; i < s.length (); i++)
    {
      aOstr.print ("0x" + Integer.toHexString (s.charAt (i)) + ", ");
    }
    aOstr.print ("0 }");
  }

  static public void start () throws MetaParseException
  {
    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    FilesCpp.gen_JavaCCDefs ();
    FilesCpp.gen_CharStream ();
    // TODO(theov): issued twice??
    FilesCpp.gen_Token ();
    FilesCpp.gen_TokenManager ();
    FilesCpp.gen_TokenMgrError ();
    FilesCpp.gen_ParseException ();
    FilesCpp.gen_ErrorHandler ();

    final Writer w = FileHelper.getBufferedWriter (new File (Options.getOutputDirectory (),
                                                             grammar ().getParserName () + "Constants.h"),
                                                   Options.getOutputEncoding ());
    if (w == null)
    {
      JavaCCErrors.semantic_error ("Could not open file " + grammar ().getParserName () + "Constants.h for writing.");
      return;
    }

    try (PrintWriter s_ostr = new PrintWriter (w))
    {
      final List <String> aTn = new ArrayList <> (grammar ().getToolNameList ());
      aTn.add (CPG.APP_NAME);
      s_ostr.println ("/* " + getIdString (aTn, grammar ().getParserName () + "Constants.java") + " */");

      if (grammar ().cuToInsertionPoint1 ().size () != 0 &&
        grammar ().cuToInsertionPoint1 ().get (0).kind == JavaCCParserConstants.PACKAGE)
      {
        for (int i = 1; i < grammar ().cuToInsertionPoint1 ().size (); i++)
        {
          if (grammar ().cuToInsertionPoint1 ().get (i).kind == JavaCCParserConstants.SEMICOLON)
          {
            Token t = grammar ().cuToInsertionPoint1 ().get (0);
            printTokenSetup (t);
            for (int j = 0; j <= i; j++)
            {
              t = grammar ().cuToInsertionPoint1 ().get (j);
              printToken (t, s_ostr);
            }
            printTrailingComments (t);
            s_ostr.println ();
            s_ostr.println ();
            break;
          }
        }
      }
      s_ostr.println ("");
      s_ostr.println ("/**");
      s_ostr.println (" * Token literal values and constants.");
      s_ostr.println (" * Generated by " + OtherFilesGenCPP.class.getName () + "#start()");
      s_ostr.println (" */");

      final String sDefine = (grammar ().getParserName () + "Constants_h").toUpperCase (Locale.US);
      s_ostr.println ("#ifndef " + sDefine);
      s_ostr.println ("#define " + sDefine);
      s_ostr.println ("#include \"JavaCC.h\"");
      s_ostr.println ("");
      if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
      {
        s_ostr.println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }

      AbstractExpRegularExpression aRe;
      final String sConstPrefix = "const";
      s_ostr.println ("  /** End of File. */");
      s_ostr.println (sConstPrefix + "  int _EOF = 0;");
      for (final java.util.Iterator <AbstractExpRegularExpression> it = grammar ().orderedNameTokens ().iterator (); it
                                                                                                                       .hasNext ();)
      {
        aRe = it.next ();
        s_ostr.println ("  /** RegularExpression Id. */");
        s_ostr.println (sConstPrefix + "  int " + aRe.getLabel () + " = " + aRe.getOrdinal () + ";");
      }
      s_ostr.println ("");

      if (!Options.isUserTokenManager () && Options.isBuildTokenManager ())
      {
        for (int i = 0; i < LexGenJava.lexer ().getLexStateName ().length; i++)
        {
          s_ostr.println ("  /** Lexical state. */");
          s_ostr.println (sConstPrefix + "  int " + LexGenJava.lexer ().getLexStateName ()[i] + " = " + i + ";");
        }
        s_ostr.println ("");
      }
      s_ostr.println ("  /** Literal token values. */");
      int nCnt = 0;
      s_ostr.println ("  static const JJChar tokenImage_arr_" + nCnt + "[] = ");
      printCharArray (s_ostr, "<EOF>");
      s_ostr.println (";");

      for (final TokenProduction tp : grammar ().rexprList ())
      {
        final List <RegExprSpec> aRespecs = tp.m_aRespecs;
        for (final RegExprSpec res : aRespecs)
        {
          aRe = res.m_aRexp;
          s_ostr.println ("  static const JJChar tokenImage_arr_" + ++nCnt + "[] = ");
          if (aRe instanceof final ExpRStringLiteral aRStringLiteral)
          {
            printCharArray (s_ostr, "\"" + aRStringLiteral.m_sImage + "\"");
          }
          else
            if (aRe.hasLabel ())
            {
              printCharArray (s_ostr, "\"<" + aRe.getLabel () + ">\"");
            }
            else
            {
              if (aRe.m_aTpContext.m_eKind == ETokenKind.TOKEN)
              {
                JavaCCErrors.warning (aRe, "Consider giving this non-string token a label for better error reporting.");
              }
              printCharArray (s_ostr, "\"<token of kind " + aRe.getOrdinal () + ">\"");
            }
          s_ostr.println (";");
        }
      }

      s_ostr.println ("  static const JJChar* const tokenImage[] = {");
      for (int i = 0; i <= nCnt; i++)
      {
        s_ostr.println ("tokenImage_arr_" + i + ", ");
      }
      s_ostr.println ("  };");
      s_ostr.println ("");
      if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
      {
        s_ostr.println (Options.stringValue ("NAMESPACE_CLOSE"));
      }
      s_ostr.println ("#endif");
    }
  }

}
