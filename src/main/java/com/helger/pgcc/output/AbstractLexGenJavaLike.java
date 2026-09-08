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

import java.io.IOException;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.LexerState;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.AbstractCodeGenerator;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;
import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

/**
 * What the two token manager generators genuinely share.
 * <p>
 * LexGenCpp used to extend LexGenJava, which read as "the C++ generator is a kind of Java
 * generator". It is not: it redeclares its own private copy of every method it appears to inherit -
 * _printClassHead, _dumpGetNextToken and the rest - and the only things it actually took from its
 * parent are here. Chief among them is the writeTemplate override, which fills in the values the
 * token manager templates expect; without it the C++ output silently loses ${stateSetSize} and
 * emits "jjrounds[]".
 */
public abstract class AbstractLexGenJavaLike extends AbstractCodeGenerator
{
  /** Default constructor. */
  protected AbstractLexGenJavaLike ()
  {}

  public static LexerState lexer ()
  {
    return PGCCContext.current ().lexer ();
  }

  protected static int _getIndex (final String sName)
  {
    for (int i = 0; i < lexer ().getLexStateName ().length; i++)
      if (lexer ().getLexStateName ()[i] != null && lexer ().getLexStateName ()[i].equals (sName))
        return i;

    throw new IllegalStateException ("Should never come here");
  }

  protected static void checkEmptyStringMatch ()
  {
    final boolean [] aSeen = new boolean [lexer ().getMaxLexStates ()];
    final boolean [] aDone = new boolean [lexer ().getMaxLexStates ()];

    Outer: for (int i = 0; i < lexer ().getMaxLexStates (); i++)
    {
      if (aDone[i] ||
          lexer ().getInitMatch ()[i] == 0 ||
          lexer ().getInitMatch ()[i] == Integer.MAX_VALUE ||
          lexer ().getCanMatchAnyChar ()[i] != -1)
        continue;

      aDone[i] = true;
      int nLen = 0;
      final StringBuilder aCycle = new StringBuilder ();
      String sReList = "";

      for (int k = 0; k < lexer ().getMaxLexStates (); k++)
        aSeen[k] = false;

      int j = i;
      aSeen[i] = true;
      aCycle.append (lexer ().getLexStateName ()[j]).append ("-->");
      while (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]] != null)
      {
        aCycle.append (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]]);
        if (aSeen[j = _getIndex (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]])])
          break;

        aCycle.append ("-->");
        aDone[j] = true;
        aSeen[j] = true;
        if (lexer ().getInitMatch ()[j] == 0 ||
            lexer ().getInitMatch ()[j] == Integer.MAX_VALUE ||
            lexer ().getCanMatchAnyChar ()[j] != -1)
          continue Outer;
        if (nLen != 0)
          sReList += "; ";
        sReList += "line " +
                   lexer ().getRexprs ()[lexer ().getInitMatch ()[j]].getLineNumber () +
                   ", column " +
                   lexer ().getRexprs ()[lexer ().getInitMatch ()[j]].getColumnNumber ();
        nLen++;
      }

      if (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]] == null)
        aCycle.append (lexer ().getLexStateName ()[lexer ().getLexStates ()[lexer ().getInitMatch ()[j]]]);

      for (int k = 0; k < lexer ().getMaxLexStates (); k++)
        lexer ().getCanLoop ()[k] |= aSeen[k];

      lexer ().setHasLoop (true);
      final String sLabel = lexer ().getRexprs ()[lexer ().getInitMatch ()[i]].getLabel ();
      if (nLen == 0)
      {
        JavaCCErrors.warning (lexer ().getRexprs ()[lexer ().getInitMatch ()[i]],
                              "Regular expression" +
                                                                                  (StringHelper.isEmpty (sLabel) ? ""
                                                                                                                 : " for " +
                                                                                                                   sLabel) +
                                                                                  " can be matched by the empty string (\"\") in lexical state " +
                                                                                  lexer ().getLexStateName ()[i] +
                                                                                  ". This can result in an endless loop of " +
                                                                                  "empty string matches.");
      }
      else
      {
        JavaCCErrors.warning (lexer ().getRexprs ()[lexer ().getInitMatch ()[i]],
                              "Regular expression" +
                                                                                  (StringHelper.isEmpty (sLabel) ? ""
                                                                                                                 : " for " +
                                                                                                                   sLabel) +
                                                                                  " can be matched by the empty string (\"\") in lexical state " +
                                                                                  lexer ().getLexStateName ()[i] +
                                                                                  ". This regular expression along with the " +
                                                                                  "regular expressions at " +
                                                                                  sReList +
                                                                                  " forms the cycle \n   " +
                                                                                  aCycle.append ("\ncontaining regular expressions with empty matches.")
                                                                                        .append (" This can result in an endless loop of empty string matches.")
                                                                                        .toString ());
      }
    }
  }

  protected static char maxChar (final long l)
  {
    for (int i = 64; i-- > 0;)
      if ((l & (1L << i)) != 0L)
        return (char) i;

    return 0xffff;
  }

  public void writeTemplate (final String sName, @Nullable final Map <String, Object> aAdditionalOptions)
                                                                                                          throws IOException
  {
    final Map <String, Object> aOptions = Options.getAllOptions ();
    aOptions.put ("maxOrdinal", Integer.valueOf (lexer ().getMaxOrdinal ()));
    aOptions.put ("maxLexStates", Integer.valueOf (lexer ().getMaxLexStates ()));
    aOptions.put ("hasEmptyMatch", Boolean.valueOf (lexer ().isHasEmptyMatch ()));
    aOptions.put ("hasSkip", Boolean.valueOf (lexer ().isHasSkip ()));
    aOptions.put ("hasMore", Boolean.valueOf (lexer ().isHasMore ()));
    aOptions.put ("hasSpecial", Boolean.valueOf (lexer ().isHasSpecial ()));
    aOptions.put ("hasMoreActions", Boolean.valueOf (lexer ().isHasMoreActions ()));
    aOptions.put ("hasSkipActions", Boolean.valueOf (lexer ().isHasSkipActions ()));
    aOptions.put ("hasTokenActions", Boolean.valueOf (lexer ().isHasTokenActions ()));
    aOptions.put ("stateSetSize", Integer.valueOf (lexer ().getStateSetSize ()));
    aOptions.put ("hasActions",
                  Boolean.valueOf (lexer ().isHasMoreActions () ||
                                   lexer ().isHasSkipActions () ||
                                   lexer ().isHasTokenActions ()));
    aOptions.put ("tokMgrClassName", lexer ().getTokenMgrClassName ());
    int x = 0;
    for (final int l : lexer ().getMaxLongsReqd ())
      x = Math.max (x, l);
    aOptions.put ("maxLongs", Integer.valueOf (x));
    aOptions.put ("cu_name", grammar ().getParserName ());

    // options.put("", .valueOf(maxOrdinal));
    if (aAdditionalOptions != null)
      aOptions.putAll (aAdditionalOptions);

    super.writeTemplate (sName, aOptions);
  }

}
