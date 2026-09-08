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
package com.helger.pgcc.main;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.helger.io.file.SimpleFileIO;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.output.java.LexGenJava;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.Semanticize;
import com.helger.pgcc.parser.TokenizerData;

public class JavaCCInterpreter
{
  public static void main (final String [] aArgs) throws Exception
  {
    // Initialize all static state
    Main.reInitAll ();
    for (int nArg = 0; nArg < aArgs.length - 2; nArg++)
    {
      if (!Options.isOption (aArgs[nArg]))
      {
        PGPrinter.info ("Argument \"" + aArgs[nArg] + "\" must be an option setting.");
        System.exit (1);
      }
      Options.setCmdLineOption (aArgs[nArg]);
    }

    final File aFp = new File (aArgs[aArgs.length - 2]);
    final String sGrammar = SimpleFileIO.getFileAsString (aFp, Options.getGrammarEncoding ());

    final File aInputFile = new File (aArgs[aArgs.length - 1]);
    final String sInput = SimpleFileIO.getFileAsString (aInputFile, Options.getGrammarEncoding ());

    final long l = System.currentTimeMillis ();
    new JavaCCInterpreter ().runTokenizer (sGrammar, sInput);
    PGPrinter.error ("Tokenized in: " + (System.currentTimeMillis () - l));
  }

  /**
   * Tokenize an input against a grammar, printing every token, without generating any code.
   * <p>
   * The caller has to have started a run first - {@link com.helger.pgcc.parser.Main#reInitAll()},
   * then whatever options apply. {@link #main(String[])} does that; a direct caller must do it too,
   * or the option lookups fail.
   *
   * @param grammar
   *        The grammar source. May not be <code>null</code>.
   * @param input
   *        The text to tokenize. May not be <code>null</code>.
   */
  public void runTokenizer (final String sGrammar, final String sInput)
  {
    try
    {
      final JavaCCParser aParser = new JavaCCParser (sGrammar);
      aParser.javacc_input ();
      Semanticize.start ();
      final LexGenJava aLg = new LexGenJava ();
      LexGenJava.lexer ().setGenerateDataOnly (true);
      aLg.start ();
      final TokenizerData aTd = LexGenJava.lexer ().getTokenizerData ();
      if (JavaCCErrors.getErrorCount () == 0)
      {
        _tokenize (aTd, sInput);
      }
    }
    catch (final MetaParseException e)
    {
      PGPrinter.error ("Detected " +
                       JavaCCErrors.getErrorCount () +
                       " errors and " +
                       JavaCCErrors.getWarningCount () +
                       " warnings.");
    }
    catch (final Exception e)
    {
      PGPrinter.error ("Detected " +
                       (JavaCCErrors.getErrorCount () + 1) +
                       " errors and " +
                       JavaCCErrors.getWarningCount () +
                       " warnings.",
                       e);
    }
  }

  private static void _tokenize (final TokenizerData aTd, final String sInput)
  {
    // First match the string literals.
    final int nInput_size = sInput.length ();
    int nCurPos = 0;
    int nCurLexState = aTd.m_defaultLexState;
    Set <Integer> aCurStates = new HashSet <> ();
    Set <Integer> aNewStates = new HashSet <> ();
    // Where the token being assembled starts. A MORE production consumes characters and hands over
    // to the next match instead of producing a token, so the image of the token that finally comes
    // out starts before the match that produced it
    int nTokenBeg = -1;
    while (nCurPos < nInput_size)
    {
      final int nBeg = nCurPos;
      if (nTokenBeg == -1)
        nTokenBeg = nBeg;
      int nMatchedPos = nBeg;
      int nMatchedKind = Integer.MAX_VALUE;
      int nNfaStartState = aTd.m_initialStates.get (Integer.valueOf (nCurLexState)).intValue ();

      char c = sInput.charAt (nCurPos);
      if (Options.isIgnoreCase ())
        c = Character.toLowerCase (c);
      final int nKey = nCurLexState << 16 | c;
      final List <String> aLiterals = aTd.m_literalSequence.get (Integer.valueOf (nKey));
      if (aLiterals != null)
      {
        // We need to go in order so that the longest match works.
        int nLitIndex = 0;
        for (final String s : aLiterals)
        {
          int nIndex = 1;
          // See which literal matches.
          while (nIndex < s.length () && nCurPos + nIndex < nInput_size)
          {
            c = sInput.charAt (nCurPos + nIndex);
            if (Options.isIgnoreCase ())
              c = Character.toLowerCase (c);
            if (c != s.charAt (nIndex))
              break;
            nIndex++;
          }
          if (nIndex == s.length ())
          {
            // Found a string literal match.
            nMatchedKind = aTd.m_literalKinds.get (Integer.valueOf (nKey)).get (nLitIndex).intValue ();
            nMatchedPos = nCurPos + nIndex - 1;
            nNfaStartState = aTd.m_kindToNfaStartState.get (Integer.valueOf (nMatchedKind)).intValue ();
            nCurPos += nIndex;
            break;
          }
          nLitIndex++;
        }
      }

      if (nNfaStartState != -1)
      {
        // We need to add the composite states first.
        int nKind = Integer.MAX_VALUE;
        aCurStates.add (Integer.valueOf (nNfaStartState));
        aCurStates.addAll (aTd.m_nfa.get (Integer.valueOf (nNfaStartState)).m_compositeStates);
        do
        {
          c = sInput.charAt (nCurPos);
          if (Options.isIgnoreCase ())
            c = Character.toLowerCase (c);
          for (final int state : aCurStates)
          {
            final TokenizerData.NfaState aNfaState = aTd.m_nfa.get (Integer.valueOf (state));
            if (aNfaState.m_characters.contains (Character.valueOf (c)))
            {
              if (nKind > aNfaState.m_kind)
                nKind = aNfaState.m_kind;
              aNewStates.addAll (aNfaState.m_nextStates);
            }
          }
          final Set <Integer> aTmp = aNewStates;
          aNewStates = aCurStates;
          aCurStates = aTmp;
          aNewStates.clear ();
          if (nKind != Integer.MAX_VALUE)
          {
            nMatchedKind = nKind;
            nMatchedPos = nCurPos;
            nKind = Integer.MAX_VALUE;
          }
        } while (!aCurStates.isEmpty () && ++nCurPos < nInput_size);
      }
      if (nMatchedPos == nBeg && nMatchedKind > aTd.m_wildcardKind.get (Integer.valueOf (nCurLexState)).intValue ())
      {
        nMatchedKind = aTd.m_wildcardKind.get (Integer.valueOf (nCurLexState)).intValue ();
      }
      if (nMatchedKind != Integer.MAX_VALUE)
      {
        final TokenizerData.MatchInfo aMatchInfo = aTd.m_allMatches.get (Integer.valueOf (nMatchedKind));
        if (aMatchInfo.m_action != null)
        {
          PGPrinter.error ("Actions not implemented (yet) in intererpreted mode");
        }
        if (aMatchInfo.m_matchType == TokenizerData.EMatchType.TOKEN)
        {
          PGPrinter.error ("Token: " + nMatchedKind + "; image: \"" + sInput.substring (nTokenBeg, nMatchedPos + 1) + "\"");
        }
        if (aMatchInfo.m_matchType != TokenizerData.EMatchType.MORE)
        {
          // Anything that is not MORE finishes the token, whether it produced one or threw the
          // accumulated text away
          nTokenBeg = -1;
        }
        if (aMatchInfo.m_newLexState != -1)
        {
          nCurLexState = aMatchInfo.m_newLexState;
        }
        nCurPos = nMatchedPos + 1;
      }
      else
      {
        PGPrinter.error ("Encountered token error at char: " + sInput.charAt (nCurPos));
        return;
      }
    }
    PGPrinter.error ("Matched EOF");
  }
}
