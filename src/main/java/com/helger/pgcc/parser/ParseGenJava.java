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

import static com.helger.pgcc.parser.JavaCCGlobals.CU_FROM_INSERTION_POINT_2;
import static com.helger.pgcc.parser.JavaCCGlobals.CU_TO_INSERTION_POINT_1;
import static com.helger.pgcc.parser.JavaCCGlobals.CU_TO_INSERTION_POINT_2;
import static com.helger.pgcc.parser.JavaCCGlobals.MASK_VALS;
import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jj2index;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jjtreeGenerated;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskindex;
import static com.helger.pgcc.parser.JavaCCGlobals.s_tokenCount;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolNames;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.helger.pgcc.CPG;
import com.helger.pgcc.EJDKVersion;
import com.helger.pgcc.output.EOutputLanguage;

/**
 * Generate the parser.
 */
public class ParseGenJava extends CodeGenerator
{
  public void start (final boolean bIsJavaModernMode) throws MetaParseException
  {
    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    if (!Options.isBuildParser ())
      return;

    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    final EJDKVersion eJavaVersion = Options.getJdkVersion ();
    final boolean bHasCharset = eJavaVersion.isNewerOrEqualsThan (EJDKVersion.JDK_1_6);
    final boolean bEmptyTypeVar = eJavaVersion.isNewerOrEqualsThan (EJDKVersion.JDK_1_7);

    final List <String> tn = new ArrayList <> (s_toolNames);
    tn.add (CPG.APP_NAME);

    // This is the first line generated -- the the comment line at the top of
    // the generated parser
    genCodeLine ("/* " + getIdString (tn, s_cu_name + ".java") + " */");

    boolean bImplementsExists = false;

    if (CU_TO_INSERTION_POINT_1.size () != 0)
    {
      final Token firstToken = CU_TO_INSERTION_POINT_1.get (0);
      printTokenSetup (firstToken);
      setColToStart ();
      Token t;
      for (final Token aToken : CU_TO_INSERTION_POINT_1)
      {
        t = aToken;
        if (t.kind == JavaCCParserConstants.IMPLEMENTS)
        {
          bImplementsExists = true;
        }
        else
          if (t.kind == JavaCCParserConstants.CLASS)
          {
            bImplementsExists = false;
          }

        printToken (t);
      }
    }

    if (bImplementsExists)
    {
      genCode (", ");
    }
    else
    {
      genCode (" implements ");
    }
    genCode (s_cu_name + "Constants ");
    if (CU_TO_INSERTION_POINT_2.isNotEmpty ())
    {
      printTokenSetup (CU_TO_INSERTION_POINT_2.get (0));
      for (final Token aToken : CU_TO_INSERTION_POINT_2)
      {
        printToken (aToken);
      }
    }

    genCodeNewLine ();
    genCodeNewLine ();

    new ParseEngine ().build (this);

    if (Options.isUserTokenManager ())
    {
      genCodeLine ("  /** User defined Token Manager. */");
      genCodeLine ("  public TokenManager token_source;");
    }
    else
    {
      genCodeLine ("  /** Generated Token Manager. */");
      genCodeLine ("  public " + s_cu_name + "TokenManager token_source;");
      if (!Options.isJavaUserCharStream ())
      {
        if (Options.isJavaUnicodeEscape ())
        {
          genCodeLine ("  JavaCharStream jj_input_stream;");
        }
        else
        {
          genCodeLine ("  SimpleCharStream jj_input_stream;");
        }
      }
    }
    genCodeLine ("  /** Current token. */");
    genCodeLine ("  public Token token;");
    genCodeLine ("  /** Next token. */");
    genCodeLine ("  public Token jj_nt;");
    if (!Options.isCacheTokens ())
    {
      genCodeLine ("  private int jj_ntk;");
    }
    if (Options.hasDepthLimit ())
    {
      genCodeLine ("  /** current depth */");
      genCodeLine ("  private int jj_depth;");
    }
    if (s_jj2index != 0)
    {
      genCodeLine ("  private Token jj_scanpos, jj_lastpos;");
      genCodeLine ("  private int jj_la;");
      if (JavaCCGlobals.isLookAheadNeeded ())
      {
        genCodeLine ("  /** Whether we are looking ahead. */");
        genCodeLine ("  private " + eOutputLanguage.getTypeBoolean () + " jj_lookingAhead = false;");
        genCodeLine ("  private " + eOutputLanguage.getTypeBoolean () + " jj_semLA;");
      }
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("  private int jj_gen;");
      genCodeLine ("  final private int[] jj_la1 = new int[" + s_maskindex + "];");
      final int tokenMaskSize = (s_tokenCount - 1) / 32 + 1;
      for (int i = 0; i < tokenMaskSize; i++)
      {
        genCodeLine ("  static private int[] jj_la1_" + i + ";");
      }
      genCodeLine ("  static {");
      for (int i = 0; i < tokenMaskSize; i++)
      {
        genCodeLine ("	   jj_la1_init_" + i + "();");
      }
      genCodeLine ("	}");
      for (int i = 0; i < tokenMaskSize; i++)
      {
        genCodeLine ("	private static void jj_la1_init_" + i + "() {");
        genCode ("	   jj_la1_" + i + " = new int[] {");
        for (final int [] tokenMask : MASK_VALS)
          genCode ("0x" + Integer.toHexString (tokenMask[i]) + ",");
        genCodeLine ("};");
        genCodeLine ("	}");
      }
    }
    if (s_jj2index != 0 && Options.isErrorReporting ())
    {
      genCodeLine ("  private final JJCalls[] jj_2_rtns = new JJCalls[" + s_jj2index + "];");
      genCodeLine ("  private " + eOutputLanguage.getTypeBoolean () + " jj_rescan = false;");
      genCodeLine ("  private int jj_gc = 0;");
    }
    genCodeNewLine ();

    if (!Options.isUserTokenManager ())
    {
      if (Options.isJavaUserCharStream ())
      {
        genCodeLine ("  /**");
        genCodeLine ("   * Constructor with user supplied CharStream.");
        genCodeLine ("   * @param stream stream to init with");
        genCodeLine ("   */");
        genCodeLine ("  public " + s_cu_name + "(final CharStream stream) {");
        if (Options.isTokenManagerUsesParser ())
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(this, stream);");
        else
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(stream);");
        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("   jj_nt = token_source.getNextToken();");
          genCodeLine ("   token.next = jj_nt;");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");
        genCodeNewLine ();
        genCodeLine ("  /**");
        genCodeLine ("   * Reinitialise.");
        genCodeLine ("   * @param stream stream to init with");
        genCodeLine ("   */");
        genCodeLine ("  public void ReInit(final CharStream stream) {");

        if (Options.isTokenManagerRequiresParserAccess ())
        {
          genCodeLine ("	 token_source.ReInit(this,stream);");
        }
        else
        {
          genCodeLine ("	 token_source.ReInit(stream);");
        }

        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("   jj_nt = token_source.getNextToken();");
          genCodeLine ("   token.next = jj_nt;");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (JavaCCGlobals.isLookAheadNeeded ())
        {
          genCodeLine ("	 jj_lookingAhead = false;");
        }
        if (s_jjtreeGenerated)
        {
          genCodeLine ("	 jjtree.reset();");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("   for (int i = 0; i < " + s_maskindex + "; i++)");
            genCodeLine ("     jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("   for (int i = 0; i < jj_2_rtns.length; i++)");
            genCodeLine ("     jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");
      }
      else
      {
        if (!bIsJavaModernMode)
        {
          // See #108
          if (false)
          {
            genCodeLine ("  /**");
            genCodeLine ("   * Constructor with InputStream.");
            genCodeLine ("   * @param stream input stream");
            genCodeLine ("   */");
            genCodeLine ("  public " + s_cu_name + "(final java.io.InputStream stream) {");
            genCodeLine ("	  this(stream, null);");
            genCodeLine ("  }");
            genCodeNewLine ();
          }
          genCodeLine ("  /**");
          genCodeLine ("   * Constructor with InputStream and supplied encoding");
          genCodeLine ("   * @param stream input stream");
          genCodeLine ("   * @param encoding charset to be used. May not be <code>null</code>.");
          genCodeLine ("   */");
          if (bHasCharset)
          {
            genCodeLine ("  public " +
                         s_cu_name +
                         "(final java.io.InputStream stream, final java.nio.charset.Charset encoding) {");
            genCodeLine ("   jj_input_stream = new " +
                         (Options.isJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream") +
                         "(stream, encoding, 1, 1);");
          }
          else
          {
            genCodeLine ("  public " + s_cu_name + "(final java.io.InputStream stream, final String encoding) {");
            genCodeLine ("   try {");
            genCodeLine ("     jj_input_stream = new " +
                         (Options.isJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream") +
                         "(stream, encoding, 1, 1);");
            genCodeLine ("   } catch(final java.io.UnsupportedEncodingException e) {");
            genCodeLine ("     throw new IllegalStateException(e);");
            genCodeLine ("   }");
          }

          if (Options.isTokenManagerUsesParser ())
            genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(this, jj_input_stream);");
          else
            genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(jj_input_stream);");
          genCodeLine ("	 token = new Token();");
          if (Options.isCacheTokens ())
          {
            genCodeLine ("   jj_nt = token_source.getNextToken();");
            genCodeLine ("   token.next = jj_nt;");
          }
          else
          {
            genCodeLine ("	 jj_ntk = -1;");
          }
          if (Options.hasDepthLimit ())
          {
            genCodeLine ("    jj_depth = -1;");
          }
          if (Options.isErrorReporting ())
          {
            genCodeLine ("	 jj_gen = 0;");
            if (s_maskindex > 0)
            {
              genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (s_jj2index != 0)
            {
              genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine ("  }");
          genCodeNewLine ();

          // See #108
          if (false)
          {
            genCodeLine ("  /**");
            genCodeLine ("   * Reinitialise");
            genCodeLine ("   * @param stream input stream");
            genCodeLine ("   */");
            genCodeLine ("  public void ReInit(final java.io.InputStream stream) {");
            genCodeLine ("	  ReInit(stream, null);");
            genCodeLine ("  }");
          }

          genCodeLine ("  /**");
          genCodeLine ("   * Reinitialise");
          genCodeLine ("   * @param stream input stream");
          genCodeLine ("   * @param encoding charset to be used. May not be <code>null</code>.");
          genCodeLine ("   */");
          if (bHasCharset)
          {
            genCodeLine ("  public void ReInit(final java.io.InputStream stream, final java.nio.charset.Charset encoding) {");
            genCodeLine ("    jj_input_stream.reInit(stream, encoding, 1, 1);");
          }
          else
          {
            genCodeLine ("  public void ReInit(final java.io.InputStream stream, final String encoding) {");
            genCodeLine ("	  try {");
            genCodeLine ("      jj_input_stream.reInit(stream, encoding, 1, 1);");
            genCodeLine ("    } catch(final java.io.UnsupportedEncodingException e) { ");
            genCodeLine ("      throw new IllegalStateException(e);");
            genCodeLine ("    }");
          }

          if (Options.isTokenManagerRequiresParserAccess ())
          {
            genCodeLine ("	 token_source.ReInit(this,jj_input_stream);");
          }
          else
          {
            genCodeLine ("	 token_source.ReInit(jj_input_stream);");
          }

          genCodeLine ("	 token = new Token();");
          if (Options.isCacheTokens ())
          {
            genCodeLine ("   jj_nt = token_source.getNextToken();");
            genCodeLine ("   token.next = jj_nt;");
          }
          else
          {
            genCodeLine ("	 jj_ntk = -1;");
          }
          if (Options.hasDepthLimit ())
          {
            genCodeLine ("    jj_depth = -1;");
          }
          if (s_jjtreeGenerated)
          {
            genCodeLine ("	 jjtree.reset();");
          }
          if (Options.isErrorReporting ())
          {
            genCodeLine ("	 jj_gen = 0;");
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
            if (s_jj2index != 0)
            {
              genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine ("  }");
          genCodeNewLine ();

        }

        final String readerInterfaceName = bIsJavaModernMode ? "Provider" : "java.io.Reader";
        final String stringReaderClass = bIsJavaModernMode ? "StringProvider" : "java.io.StringReader";

        genCodeLine ("  /**");
        genCodeLine ("   * Constructor with InputStream.");
        genCodeLine ("   * @param stream char stream");
        genCodeLine ("   */");
        genCodeLine ("  public " + s_cu_name + "(final " + readerInterfaceName + " stream) {");
        if (Options.isJavaUnicodeEscape ())
        {
          genCodeLine ("	 jj_input_stream = new JavaCharStream(stream, 1, 1);");
        }
        else
        {
          genCodeLine ("	 jj_input_stream = new SimpleCharStream(stream, 1, 1);");
        }
        if (Options.isTokenManagerUsesParser ())
        {
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(this, jj_input_stream);");
        }
        else
        {
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(jj_input_stream);");
        }
        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("   for (int i = 0; i < " + s_maskindex + "; i++)");
            genCodeLine ("     jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("   for (int i = 0; i < jj_2_rtns.length; i++)");
            genCodeLine ("     jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");
        genCodeNewLine ();

        // Add-in a string based constructor because its convenient (modern
        // only to prevent regressions)
        if (bIsJavaModernMode)
        {
          genCodeLine ("  /**");
          genCodeLine ("   * Constructor with InputStream.");
          genCodeLine ("   * @param sDSL String representation to be parsed");
          genCodeLine ("   */");
          genCodeLine ("  public " + s_cu_name + "(final String sDSL) {");
          genCodeLine ("	   this(new " + stringReaderClass + "(sDSL));");
          genCodeLine ("  }");
          genCodeNewLine ();

          genCodeLine ("  /**");
          genCodeLine ("   * Reinitialise.");
          genCodeLine ("   * @param sDSL String representation to be parsed");
          genCodeLine ("   */");
          genCodeLine ("  public void ReInit(final String sDSL) {");
          genCodeLine ("	  ReInit(new " + stringReaderClass + "(sDSL));");
          genCodeLine ("  }");

        }

        genCodeLine ("  /**");
        genCodeLine ("   * Reinitialise");
        genCodeLine ("   * @param stream char stream");
        genCodeLine ("   */");
        genCodeLine ("  public void ReInit(final " + readerInterfaceName + " stream) {");
        if (Options.isJavaUnicodeEscape ())
        {
          genCodeLine ("	if (jj_input_stream == null) {");
          genCodeLine ("	  jj_input_stream = new JavaCharStream(stream, 1, 1);");
          genCodeLine ("	} else {");
          genCodeLine ("	  jj_input_stream.reInit(stream, 1, 1);");
          genCodeLine ("  }");
        }
        else
        {
          genCodeLine ("	if (jj_input_stream == null) {");
          genCodeLine ("	  jj_input_stream = new SimpleCharStream(stream, 1, 1);");
          genCodeLine ("	} else {");
          genCodeLine ("	  jj_input_stream.reInit(stream, 1, 1);");
          genCodeLine ("  }");
        }

        genCodeLine ("	if (token_source == null) {");
        if (Options.isTokenManagerUsesParser ())
          genCodeLine (" token_source = new " + s_cu_name + "TokenManager(this, jj_input_stream);");
        else
          genCodeLine (" token_source = new " + s_cu_name + "TokenManager(jj_input_stream);");
        genCodeLine ("	}");
        genCodeNewLine ();

        if (Options.isTokenManagerRequiresParserAccess ())
        {
          genCodeLine ("	 token_source.ReInit(this,jj_input_stream);");
        }
        else
        {
          genCodeLine ("	 token_source.ReInit(jj_input_stream);");
        }

        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (s_jjtreeGenerated)
        {
          genCodeLine ("	 jjtree.reset();");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("   for (int i = 0; i < " + s_maskindex + "; i++)");
            genCodeLine ("     jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("   for (int i = 0; i < jj_2_rtns.length; i++)");
            genCodeLine ("     jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");

      }
    }
    genCodeNewLine ();
    if (Options.isUserTokenManager ())
    {
      genCodeLine ("  /**");
      genCodeLine ("   * Constructor with user supplied Token Manager.");
      genCodeLine ("   * @param tm Token manager to use");
      genCodeLine ("   */");
      genCodeLine ("  public " + s_cu_name + "(final TokenManager tm) {");
    }
    else
    {
      genCodeLine ("  /**");
      genCodeLine ("   * Constructor with generated Token Manager.");
      genCodeLine ("   * @param tm Token manager to use");
      genCodeLine ("   */");
      genCodeLine ("  public " + s_cu_name + "(final " + s_cu_name + "TokenManager tm) {");
    }
    genCodeLine ("	 token_source = tm;");
    genCodeLine ("	 token = new Token();");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
    }
    else
    {
      genCodeLine ("	 jj_ntk = -1;");
    }
    if (Options.hasDepthLimit ())
    {
      genCodeLine ("    jj_depth = -1;");
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("	 jj_gen = 0;");
      if (s_maskindex > 0)
      {
        genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
      }
      if (s_jj2index != 0)
      {
        genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine ("  }");
    genCodeNewLine ();
    if (Options.isUserTokenManager ())
    {
      genCodeLine ("  /**");
      genCodeLine ("   * Reinitialise");
      genCodeLine ("   * @param tm Token manager to use");
      genCodeLine ("   */");
      genCodeLine ("  public void ReInit(final TokenManager tm) {");
    }
    else
    {
      genCodeLine ("  /**");
      genCodeLine ("   * Reinitialise");
      genCodeLine ("   * @param tm Token manager to use");
      genCodeLine ("   */");
      genCodeLine ("  public void ReInit(final " + s_cu_name + "TokenManager tm) {");
    }
    genCodeLine ("	 token_source = tm;");
    genCodeLine ("	 token = new Token();");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
    }
    else
    {
      genCodeLine ("	 jj_ntk = -1;");
    }
    if (Options.hasDepthLimit ())
    {
      genCodeLine ("    jj_depth = -1;");
    }
    if (s_jjtreeGenerated)
    {
      genCodeLine ("	 jjtree.reset();");
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("	 jj_gen = 0;");
      if (s_maskindex > 0)
      {
        genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
      }
      if (s_jj2index != 0)
      {
        genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine ("  }");
    genCodeNewLine ();
    genCodeLine ("  private Token jj_consume_token(final int kind) throws ParseException {");
    genCodeLine ("    final Token oldToken = token;");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("    token = jj_nt;");
      genCodeLine ("    if (token.next != null)");
      genCodeLine ("      jj_nt = jj_nt.next;");
      genCodeLine ("    else {");
      genCodeLine ("      jj_nt.next = token_source.getNextToken();");
      genCodeLine ("      jj_nt = jj_nt.next;");
      genCodeLine ("    }");
    }
    else
    {
      genCodeLine ("    if (token.next != null)");
      genCodeLine ("      token = token.next;");
      genCodeLine ("    else {");
      genCodeLine ("      token.next = token_source.getNextToken();");
      genCodeLine ("      token = token.next;");
      genCodeLine ("    }");
      genCodeLine ("    jj_ntk = -1;");
    }
    genCodeLine ("    if (token.kind == kind) {");
    if (Options.isErrorReporting ())
    {
      genCodeLine ("      jj_gen++;");
      if (s_jj2index != 0)
      {
        genCodeLine ("      if (++jj_gc > 100) {");
        genCodeLine ("        jj_gc = 0;");
        genCodeLine ("        for (int i = 0; i < jj_2_rtns.length; i++) {");
        genCodeLine ("          JJCalls c = jj_2_rtns[i];");
        genCodeLine ("          while (c != null) {");
        genCodeLine ("            if (c.gen < jj_gen)");
        genCodeLine ("              c.first = null;");
        genCodeLine ("            c = c.next;");
        genCodeLine ("          }");
        genCodeLine ("        }");
        genCodeLine ("      }");
      }
    }
    if (Options.isDebugParser ())
    {
      genCodeLine ("      trace_token(token, \"\");");
    }
    genCodeLine ("      return token;");
    genCodeLine ("    }");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("    jj_nt = token;");
    }
    genCodeLine ("    token = oldToken;");
    if (Options.isErrorReporting ())
    {
      genCodeLine ("    jj_kind = kind;");
    }
    genCodeLine ("    throw generateParseException();");
    genCodeLine ("  }");
    genCodeNewLine ();
    if (s_jj2index != 0)
    {
      if (false)
        genCodeLine ("  @SuppressWarnings(\"serial\")");
      genCodeLine ("  private static final class LookaheadSuccess extends IllegalStateException {}");
      genCodeLine ("  private final LookaheadSuccess jj_ls = new LookaheadSuccess();");
      genCodeLine ("  private " + eOutputLanguage.getTypeBoolean () + " jj_scan_token(int kind) {");
      genCodeLine ("	 if (jj_scanpos == jj_lastpos) {");
      genCodeLine ("	   jj_la--;");
      genCodeLine ("	   if (jj_scanpos.next == null) {");
      genCodeLine ("		   jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();");
      genCodeLine ("	   } else {");
      genCodeLine ("		   jj_lastpos = jj_scanpos = jj_scanpos.next;");
      genCodeLine ("	   }");
      genCodeLine ("	 } else {");
      genCodeLine ("	   jj_scanpos = jj_scanpos.next;");
      genCodeLine ("	 }");
      if (Options.isErrorReporting ())
      {
        genCodeLine ("	 if (jj_rescan) {");
        genCodeLine ("	   int i = 0; Token tok = token;");
        genCodeLine ("	   while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }");
        genCodeLine ("	   if (tok != null) jj_add_error_token(kind, i);");
        if (Options.isDebugLookahead ())
        {
          genCodeLine ("	 } else {");
          genCodeLine ("	   trace_scan(jj_scanpos, kind);");
        }
        genCodeLine ("	 }");
      }
      else
        if (Options.isDebugLookahead ())
        {
          genCodeLine ("	 trace_scan(jj_scanpos, kind);");
        }
      genCodeLine ("	 if (jj_scanpos.kind != kind) return true;");
      genCodeLine ("	 if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;");
      genCodeLine ("	 return false;");
      genCodeLine ("  }");
      genCodeNewLine ();
    }
    genCodeNewLine ();
    genCodeLine ("  /**");
    genCodeLine ("   * @return the next Token.");
    genCodeLine ("   */");
    genCodeLine ("  public final Token getNextToken() {");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("   token = jj_nt;");
      genCodeLine ("   if (token.next != null)");
      genCodeLine ("     jj_nt = jj_nt.next;");
      genCodeLine ("   else");
      genCodeLine ("     jj_nt = jj_nt.next = token_source.getNextToken();");
    }
    else
    {
      genCodeLine ("   if (token.next != null)");
      genCodeLine ("     token = token.next;");
      genCodeLine ("   else");
      genCodeLine ("     token = token.next = token_source.getNextToken();");
      genCodeLine ("	 jj_ntk = -1;");
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("	 jj_gen++;");
    }
    if (Options.isDebugParser ())
    {
      genCodeLine ("	   trace_token(token, \" (in getNextToken)\");");
    }
    genCodeLine ("	 return token;");
    genCodeLine ("  }");
    genCodeNewLine ();
    genCodeLine ("  /**");
    genCodeLine ("   * @param index index to be retrieved");
    genCodeLine ("   * @return the specific Token.");
    genCodeLine ("   */");
    genCodeLine ("  public final Token getToken(final int index) {");
    if (JavaCCGlobals.isLookAheadNeeded ())
      genCodeLine ("    Token t = jj_lookingAhead ? jj_scanpos : token;");
    else
      genCodeLine ("    Token t = token;");
    genCodeLine ("    for (int i = 0; i < index; i++) {");
    genCodeLine ("      if (t.next == null)");
    genCodeLine ("        t.next = token_source.getNextToken();");
    genCodeLine ("      t = t.next;");
    genCodeLine ("    }");
    genCodeLine ("    return t;");
    genCodeLine ("  }");
    genCodeNewLine ();
    if (!Options.isCacheTokens ())
    {
      if (true)
      {
        // This is a version with optimized runtime performance
        // It uses way less member access
        genCodeLine ("  private int jj_ntk_f() {");
        genCodeLine ("    final Token nt = jj_nt = token.next;");
        genCodeLine ("    final int ret;");
        genCodeLine ("    if (nt == null) {");
        genCodeLine ("      token.next = token_source.getNextToken();");
        genCodeLine ("      ret = jj_ntk = token.next.kind;");
        genCodeLine ("    }");
        genCodeLine ("    else");
        genCodeLine ("      ret = jj_ntk = nt.kind;");
        genCodeLine ("    return ret;");
        genCodeLine ("  }");
      }
      else
      {
        // Original version
        genCodeLine ("  private int jj_ntk_f() {");
        genCodeLine ("    jj_nt = token.next;");
        genCodeLine ("    if (jj_nt == null) {");
        genCodeLine ("      token.next = token_source.getNextToken();");
        genCodeLine ("      jj_ntk = token.next.kind;");
        genCodeLine ("      return jj_ntk;");
        genCodeLine ("    }");
        genCodeLine ("    jj_ntk = jj_nt.kind;");
        genCodeLine ("    return jj_ntk;");
        genCodeLine ("  }");
      }
      genCodeNewLine ();
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<" +
                   (bEmptyTypeVar ? "" : "int[]") +
                   ">();");
      genCodeLine ("  private int[] jj_expentry;");
      genCodeLine ("  private int jj_kind = -1;");
      if (s_jj2index != 0)
      {
        genCodeLine ("  private int[] jj_lasttokens = new int[100];");
        genCodeLine ("  private int jj_endpos;");
        genCodeNewLine ();
        genCodeLine ("  private void jj_add_error_token(int kind, int pos) {");
        genCodeLine ("  if (pos >= 100) {");
        genCodeLine ("    return;");
        genCodeLine ("  }");
        genCodeNewLine ();
        genCodeLine ("  if (pos == jj_endpos + 1) {");
        genCodeLine ("    jj_lasttokens[jj_endpos++] = kind;");
        genCodeLine ("  } else if (jj_endpos != 0) {");
        genCodeLine ("    jj_expentry = new int[jj_endpos];");
        genCodeNewLine ();
        genCodeLine ("    for (int i = 0; i < jj_endpos; i++) {");
        genCodeLine ("      jj_expentry[i] = jj_lasttokens[i];");
        genCodeLine ("    }");
        genCodeNewLine ();
        genCodeLine ("    for (final int[] oldentry : jj_expentries) {");
        genCodeLine ("      if (oldentry.length == jj_expentry.length) {");
        genCodeLine ("        boolean isMatched = true;");
        genCodeLine ("        for (int i = 0; i < jj_expentry.length; i++) {");
        genCodeLine ("          if (oldentry[i] != jj_expentry[i]) {");
        genCodeLine ("            isMatched = false;");
        genCodeLine ("            break;");
        genCodeLine ("          }");
        genCodeLine ("        }");
        genCodeLine ("        if (isMatched) {");
        genCodeLine ("          jj_expentries.add(jj_expentry);");
        genCodeLine ("          break;");
        genCodeLine ("        }");
        genCodeLine ("      }");
        genCodeLine ("    }");
        genCodeNewLine ();
        genCodeLine ("    if (pos != 0) {");
        genCodeLine ("      jj_endpos = pos;");
        genCodeLine ("      jj_lasttokens[jj_endpos - 1] = kind;");
        genCodeLine ("    }");
        genCodeLine ("  }");
        genCodeLine ("}");
      }
      genCodeNewLine ();
      genCodeLine ("  /**");
      genCodeLine ("   * Generate ParseException.");
      genCodeLine ("   * @return new Exception object. Never <code>null</code>");
      genCodeLine ("   */");
      genCodeLine ("  public ParseException generateParseException() {");
      genCodeLine ("    jj_expentries.clear();");
      genCodeLine ("    " +
                   eOutputLanguage.getTypeBoolean () +
                   "[] la1tokens = new " +
                   eOutputLanguage.getTypeBoolean () +
                   "[" +
                   s_tokenCount +
                   "];");
      genCodeLine ("    if (jj_kind >= 0) {");
      genCodeLine ("      la1tokens[jj_kind] = true;");
      genCodeLine ("      jj_kind = -1;");
      genCodeLine ("    }");
      genCodeLine ("    for (int i = 0; i < " + s_maskindex + "; i++) {");
      genCodeLine ("      if (jj_la1[i] == jj_gen) {");
      genCodeLine ("        for (int j = 0; j < 32; j++) {");
      for (int i = 0; i < (s_tokenCount - 1) / 32 + 1; i++)
      {
        genCodeLine ("          if ((jj_la1_" + i + "[i] & (1<<j)) != 0) {");
        genCode ("            la1tokens[");
        if (i != 0)
          genCode ((32 * i) + "+");
        genCodeLine ("j] = true;");
        genCodeLine ("          }");
      }
      genCodeLine ("        }");
      genCodeLine ("      }");
      genCodeLine ("    }");
      genCodeLine ("    for (int i = 0; i < " + s_tokenCount + "; i++) {");
      genCodeLine ("      if (la1tokens[i]) {");
      genCodeLine ("        jj_expentry = new int[1];");
      genCodeLine ("        jj_expentry[0] = i;");
      genCodeLine ("        jj_expentries.add(jj_expentry);");
      genCodeLine ("      }");
      genCodeLine ("    }");
      if (s_jj2index != 0)
      {
        genCodeLine ("    jj_endpos = 0;");
        genCodeLine ("    jj_rescan_token();");
        genCodeLine ("    jj_add_error_token(0, 0);");
      }
      genCodeLine ("    int[][] exptokseq = new int[jj_expentries.size()][];");
      genCodeLine ("    for (int i = 0; i < jj_expentries.size(); i++) {");
      genCodeLine ("      exptokseq[i] = jj_expentries.get(i);");
      genCodeLine ("    }");

      if (bIsJavaModernMode && !Options.isUserTokenManager ())
      {
        // Add the lexical state onto the exception message
        genCodeLine ("    return new ParseException(token, exptokseq, tokenImage, token_source == null ? null : " +
                     s_cu_name +
                     "TokenManager.lexStateNames[token_source.curLexState]);");
      }
      else
      {
        genCodeLine ("    return new ParseException(token, exptokseq, tokenImage);");
      }

      genCodeLine ("  }");
    }
    else
    {
      genCodeLine ("  /** Generate ParseException. */");
      genCodeLine ("  public ParseException generateParseException() {");
      genCodeLine ("  final Token errortok = token.next;");
      if (Options.isKeepLineColumn ())
      {
        genCodeLine ("  final int line = errortok.beginLine;");
        genCodeLine ("  final int column = errortok.beginColumn;");
      }
      genCodeLine ("  final String mess = errortok.kind == 0 ? tokenImage[0] : errortok.image;");
      if (Options.isKeepLineColumn ())
      {
        genCodeLine ("  return new ParseException(" +
                     "\"Parse error at line \" + line + \", column \" + column + \".  " +
                     "Encountered: \" + mess);");
      }
      else
      {
        genCodeLine ("  return new ParseException(\"Parse error at <unknown location>.  " + "Encountered: \" + mess);");
      }
      genCodeLine ("  }");
    }
    genCodeNewLine ();

    if (Options.isDebugParser ())
    {
      genCodeLine ("  private int trace_indent = 0;");
      genCodeLine ("  private " +
                   eOutputLanguage.getTypeBoolean () +
                   " trace_enabled = " +
                   (Options.isDebugParser () ? "true" : "false") +
                   ";");
      genCodeNewLine ();
      genCodeLine ("  /**");
      genCodeLine ("   * @return Trace enabled or not?");
      genCodeLine ("   */");
      genCodeLine ("  public final boolean trace_enabled() {");
      genCodeLine ("    return trace_enabled;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  /** Enable tracing. */");
      genCodeLine ("  public final void enable_tracing() {");
      genCodeLine ("    trace_enabled = true;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  /** Disable tracing. */");
      genCodeLine ("  public final void disable_tracing() {");
      genCodeLine ("    trace_enabled = false;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  protected java.io.PrintStream tracePS = System.out;");
      genCodeNewLine ();
      genCodeLine ("  protected void trace_call(final String s) {");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) {");
      genCodeLine ("        tracePS.print(\" \");");
      genCodeLine ("      }");
      genCodeLine ("      tracePS.println(\"Call:	\" + s);");
      genCodeLine ("    }");
      genCodeLine ("    trace_indent += 2;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  protected void trace_return(String s) {");
      genCodeLine ("    trace_indent -= 2;");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) { tracePS.print(\" \"); }");
      genCodeLine ("      tracePS.println(\"Return: \" + s);");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  protected void trace_token(Token t, String where) {");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) {");
      genCodeLine ("        tracePS.print(\" \");");
      genCodeLine ("      }");
      genCodeLine ("      tracePS.print(\"Consumed token: <\" + tokenImage[t.kind]);");
      genCodeLine ("      if (t.kind != 0 && !tokenImage[t.kind].equals(\"\\\"\" + t.image + \"\\\"\")) {");
      genCodeLine ("        tracePS.print(\": \\\"\" + " +
                   Options.getTokenMgrErrorClass () +
                   ".addEscapes(" +
                   "t.image) + \"\\\"\");");
      genCodeLine ("      }");
      genCodeLine ("      tracePS.println(\" at line \" + t.beginLine + " +
                   "\" column \" + t.beginColumn + \">\" + where);");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  protected void trace_scan(Token t1, int t2) {");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) { tracePS.print(\" \"); }");
      genCodeLine ("      tracePS.print(\"Visited token: <\" + tokenImage[t1.kind]);");
      genCodeLine ("      if (t1.kind != 0 && !tokenImage[t1.kind].equals(\"\\\"\" + t1.image + \"\\\"\")) {");
      genCodeLine ("        tracePS.print(\": \\\"\" + " +
                   Options.getTokenMgrErrorClass () +
                   ".addEscapes(" +
                   "t1.image) + \"\\\"\");");
      genCodeLine ("      }");
      genCodeLine ("      tracePS.println(\" at line \" + t1.beginLine + \"" +
                   " column \" + t1.beginColumn + \">; Expected token: <\" + tokenImage[t2] + \">\");");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeNewLine ();
    }
    else
    {
      genCodeLine ("  /**");
      genCodeLine ("   * @return Always <code>false</code>.");
      genCodeLine ("   */");
      genCodeLine ("  public final boolean trace_enabled() {");
      genCodeLine ("    return false;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  /** Enable tracing. */");
      genCodeLine ("  public final void enable_tracing() {}");
      genCodeNewLine ();
      genCodeLine ("  /** Disable tracing. */");
      genCodeLine ("  public final void disable_tracing() {}");
      genCodeNewLine ();
    }

    if (s_jj2index != 0 && Options.isErrorReporting ())
    {
      genCodeLine ("  private void jj_rescan_token() {");
      genCodeLine ("    jj_rescan = true;");
      genCodeLine ("    for (int i = 0; i < " + s_jj2index + "; i++) {");
      genCodeLine ("      try {");
      genCodeLine ("        JJCalls p = jj_2_rtns[i];");
      genCodeLine ("        do {");
      genCodeLine ("          if (p.gen > jj_gen) {");
      genCodeLine ("            jj_la = p.arg;");
      genCodeLine ("            jj_scanpos = p.first;");
      genCodeLine ("            jj_lastpos = p.first;");
      genCodeLine ("            switch (i) {");
      for (int i = 0; i < s_jj2index; i++)
      {
        genCodeLine ("              case " + i + ": jj_3_" + (i + 1) + "(); break;");
      }
      genCodeLine ("            }");
      genCodeLine ("          }");
      genCodeLine ("          p = p.next;");
      genCodeLine ("        } while (p != null);");
      genCodeLine ("      } catch(LookaheadSuccess ls) { /* ignore */ }");
      genCodeLine ("    }");
      genCodeLine ("    jj_rescan = false;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  private void jj_save(int index, int xla) {");
      genCodeLine ("    JJCalls p = jj_2_rtns[index];");
      genCodeLine ("    while (p.gen > jj_gen) {");
      genCodeLine ("      if (p.next == null) {");
      genCodeLine ("        p.next = new JJCalls();");
      genCodeLine ("        p = p.next;");
      genCodeLine ("        break;");
      genCodeLine ("      }");
      genCodeLine ("      p = p.next;");
      genCodeLine ("    }");
      genCodeLine ("    p.gen = jj_gen + xla - jj_la; ");
      genCodeLine ("    p.first = token;");
      genCodeLine ("    p.arg = xla;");
      genCodeLine ("  }");
      genCodeNewLine ();
    }

    if (s_jj2index != 0 && Options.isErrorReporting ())
    {
      genCodeLine ("  static final class JJCalls {");
      genCodeLine ("	 int gen;");
      genCodeLine ("	 Token first;");
      genCodeLine ("	 int arg;");
      genCodeLine ("	 JJCalls next;");
      genCodeLine ("  }");
      genCodeNewLine ();
    }

    if (CU_FROM_INSERTION_POINT_2.isNotEmpty ())
    {
      printTokenSetup (CU_FROM_INSERTION_POINT_2.getFirstOrNull ());
      setColToStart ();
      Token t = null;
      for (final Token aElement : CU_FROM_INSERTION_POINT_2)
      {
        t = aElement;
        printToken (t);
      }
      printTrailingComments (t);
    }
    genCodeNewLine ();

    saveOutput (Options.getOutputDirectory () + File.separator + s_cu_name + getFileExtension ());
  }

  public static void reInit ()
  {
    JavaCCGlobals.setLookAheadNeeded (false);
  }
}
