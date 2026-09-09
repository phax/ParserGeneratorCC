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
package com.helger.pgcc.output;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.AbstractCodeGenerator;
import com.helger.pgcc.parser.Options;

/**
 * The lines a generated token manager writes to its debug stream when
 * <code>DEBUG_TOKEN_MANAGER</code> is on.
 * <p>
 * They used to be spelled out at every place that needed them - the "Currently matched the first
 * ..." line alone existed in five copies, each with its own idea of how many spaces go after a
 * comma. Java concatenates the values into one expression while C++ passes them to
 * <code>fprintf</code>, so the two spellings cannot be derived from one another and each method
 * here keeps its own pair. What can be derived lives on {@link EOutputLanguage} instead.
 *
 * @author Philip Helger
 */
public final class TokenManagerDebug
{
  /**
   * The largest kind, the value the generated code uses for "nothing matched yet".
   */
  private static final String NO_KIND = "0x" + Integer.toHexString (Integer.MAX_VALUE);

  private TokenManagerDebug ()
  {}

  /**
   * Write one fixed line to the token manager debug stream.
   *
   * @param aCodeGenerator
   *        The code generator to write to. May not be <code>null</code>.
   * @param sIndent
   *        The indentation of the generated line. May not be <code>null</code>.
   * @param sMessage
   *        A fixed message, without a trailing line break. May not be <code>null</code>.
   */
  public static void genMessage (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                 @NonNull final String sIndent,
                                 @NonNull final String sMessage)
  {
    aCodeGenerator.genCodeLine (sIndent + aCodeGenerator.getOutputLanguage ().getDebugStreamPrintLine (sMessage));
  }

  /**
   * The line that reports how much of the input the current match covers.
   *
   * @param aCodeGenerator
   *        The code generator to write to. May not be <code>null</code>.
   * @param sIndent
   *        The indentation of the generated line. May not be <code>null</code>.
   */
  public static void genCurrentlyMatched (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                          @NonNull final String sIndent)
  {
    switch (aCodeGenerator.getOutputLanguage ())
    {
      case JAVA -> aCodeGenerator.genCodeLine (sIndent +
                                               "debugStream.println(\"   Currently matched the first \" + " +
                                               "(jjmatchedPos + 1) + \" characters as a \" + " +
                                               "tokenImage[jjmatchedKind] + \" token.\");");
      case CPP -> aCodeGenerator.genCodeLine (sIndent +
                                              "fprintf(debugStream, \"   Currently matched the first %d " +
                                              "characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1), " +
                                              "addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
      default -> throw new UnsupportedOutputLanguageException (aCodeGenerator.getOutputLanguage ());
    }
  }

  /**
   * {@link #genCurrentlyMatched(AbstractCodeGenerator, String)} guarded by a test for "something
   * matched at all".
   *
   * @param aCodeGenerator
   *        The code generator to write to. May not be <code>null</code>.
   * @param sIndent
   *        The indentation of the generated <code>if</code>. May not be <code>null</code>.
   */
  public static void genCurrentlyMatchedIfAny (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                               @NonNull final String sIndent)
  {
    aCodeGenerator.genCodeLine (sIndent + "if (jjmatchedKind != 0 && jjmatchedKind != " + NO_KIND + ")");
    genCurrentlyMatched (aCodeGenerator, sIndent + "   ");
  }

  /**
   * The line that reports the character the token manager is looking at, and where it came from.
   *
   * @param aCodeGenerator
   *        The code generator to write to. May not be <code>null</code>.
   * @param sIndent
   *        The indentation of the generated line. May not be <code>null</code>.
   * @param bMultipleLexicalStates
   *        <code>true</code> to name the lexical state as well. Only C++ prints it unconditionally.
   */
  public static void genCurrentCharacter (@NonNull final AbstractCodeGenerator aCodeGenerator,
                                          @NonNull final String sIndent,
                                          final boolean bMultipleLexicalStates)
  {
    switch (aCodeGenerator.getOutputLanguage ())
    {
      case JAVA -> aCodeGenerator.genCodeLine (sIndent +
                                               "debugStream.println(" +
                                               (bMultipleLexicalStates ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                       : "") +
                                               "\"Current character : \" + " +
                                               Options.getTokenMgrErrorClass () +
                                               ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                                               "at line \" + input_stream.getEndLine() + \" column \" + " +
                                               "input_stream.getEndColumn());");
      case CPP -> aCodeGenerator.genCodeLine (sIndent +
                                              "fprintf(debugStream, " +
                                              "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                              "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, curChar, " +
                                              "input_stream->getEndLine(), input_stream->getEndColumn());");
      default -> throw new UnsupportedOutputLanguageException (aCodeGenerator.getOutputLanguage ());
    }
  }
}
