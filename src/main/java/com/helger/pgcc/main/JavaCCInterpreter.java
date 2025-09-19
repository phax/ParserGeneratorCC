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
package com.helger.pgcc.main;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.helger.io.file.SimpleFileIO;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.LexGenJava;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.Semanticize;
import com.helger.pgcc.parser.TokenizerData;

public class JavaCCInterpreter
{
  public static void main (final String [] args) throws Exception
  {
    // Initialize all static state
    Main.reInitAll ();
    for (int arg = 0; arg < args.length - 2; arg++)
    {
      if (!Options.isOption (args[arg]))
      {
        PGPrinter.info ("Argument \"" + args[arg] + "\" must be an option setting.");
        System.exit (1);
      }
      Options.setCmdLineOption (args[arg]);
    }

    final File fp = new File (args[args.length - 2]);
    final String grammar = SimpleFileIO.getFileAsString (fp, Options.getGrammarEncoding ());

    final File inputFile = new File (args[args.length - 1]);
    final String input = SimpleFileIO.getFileAsString (inputFile, Options.getGrammarEncoding ());

    final long l = System.currentTimeMillis ();
    new JavaCCInterpreter ().runTokenizer (grammar, input);
    PGPrinter.error ("Tokenized in: " + (System.currentTimeMillis () - l));
  }

  public void runTokenizer (final String grammar, final String input)
  {
    try
    {
      final JavaCCParser parser = new JavaCCParser (grammar);
      parser.javacc_input ();
      Semanticize.start ();
      final LexGenJava lg = new LexGenJava ();
      LexGenJava.s_generateDataOnly = true;
      lg.start ();
      final TokenizerData td = LexGenJava.s_tokenizerData;
      if (JavaCCErrors.getErrorCount () == 0)
      {
        _tokenize (td, input);
      }
    }
    catch (final MetaParseException e)
    {
      PGPrinter.error ("Detected " + JavaCCErrors.getErrorCount () + " errors and " + JavaCCErrors.getWarningCount () + " warnings.");
    }
    catch (final Exception e)
    {
      PGPrinter.error ("Detected " + (JavaCCErrors.getErrorCount () + 1) + " errors and " + JavaCCErrors.getWarningCount () + " warnings.",
                       e);
    }
  }

  private static void _tokenize (final TokenizerData td, final String input)
  {
    // First match the string literals.
    final int input_size = input.length ();
    int curPos = 0;
    int curLexState = td.m_defaultLexState;
    Set <Integer> curStates = new HashSet <> ();
    Set <Integer> newStates = new HashSet <> ();
    while (curPos < input_size)
    {
      final int beg = curPos;
      int matchedPos = beg;
      int matchedKind = Integer.MAX_VALUE;
      int nfaStartState = td.m_initialStates.get (Integer.valueOf (curLexState)).intValue ();

      char c = input.charAt (curPos);
      if (Options.isIgnoreCase ())
        c = Character.toLowerCase (c);
      final int key = curLexState << 16 | c;
      final List <String> literals = td.m_literalSequence.get (Integer.valueOf (key));
      if (literals != null)
      {
        // We need to go in order so that the longest match works.
        int litIndex = 0;
        for (final String s : literals)
        {
          int index = 1;
          // See which literal matches.
          while (index < s.length () && curPos + index < input_size)
          {
            c = input.charAt (curPos + index);
            if (Options.isIgnoreCase ())
              c = Character.toLowerCase (c);
            if (c != s.charAt (index))
              break;
            index++;
          }
          if (index == s.length ())
          {
            // Found a string literal match.
            matchedKind = td.m_literalKinds.get (Integer.valueOf (key)).get (litIndex).intValue ();
            matchedPos = curPos + index - 1;
            nfaStartState = td.m_kindToNfaStartState.get (Integer.valueOf (matchedKind)).intValue ();
            curPos += index;
            break;
          }
          litIndex++;
        }
      }

      if (nfaStartState != -1)
      {
        // We need to add the composite states first.
        int kind = Integer.MAX_VALUE;
        curStates.add (Integer.valueOf (nfaStartState));
        curStates.addAll (td.m_nfa.get (Integer.valueOf (nfaStartState)).m_compositeStates);
        do
        {
          c = input.charAt (curPos);
          if (Options.isIgnoreCase ())
            c = Character.toLowerCase (c);
          for (final int state : curStates)
          {
            final TokenizerData.NfaState nfaState = td.m_nfa.get (Integer.valueOf (state));
            if (nfaState.m_characters.contains (Character.valueOf (c)))
            {
              if (kind > nfaState.m_kind)
                kind = nfaState.m_kind;
              newStates.addAll (nfaState.m_nextStates);
            }
          }
          final Set <Integer> tmp = newStates;
          newStates = curStates;
          curStates = tmp;
          newStates.clear ();
          if (kind != Integer.MAX_VALUE)
          {
            matchedKind = kind;
            matchedPos = curPos;
            kind = Integer.MAX_VALUE;
          }
        } while (!curStates.isEmpty () && ++curPos < input_size);
      }
      if (matchedPos == beg && matchedKind > td.m_wildcardKind.get (Integer.valueOf (curLexState)).intValue ())
      {
        matchedKind = td.m_wildcardKind.get (Integer.valueOf (curLexState)).intValue ();
      }
      if (matchedKind != Integer.MAX_VALUE)
      {
        final TokenizerData.MatchInfo matchInfo = td.m_allMatches.get (Integer.valueOf (matchedKind));
        if (matchInfo.m_action != null)
        {
          PGPrinter.error ("Actions not implemented (yet) in intererpreted mode");
        }
        if (matchInfo.m_matchType == TokenizerData.EMatchType.TOKEN)
        {
          PGPrinter.error ("Token: " + matchedKind + "; image: \"" + input.substring (beg, matchedPos + 1) + "\"");
        }
        if (matchInfo.m_newLexState != -1)
        {
          curLexState = matchInfo.m_newLexState;
        }
        curPos = matchedPos + 1;
      }
      else
      {
        PGPrinter.error ("Encountered token error at char: " + input.charAt (curPos));
        return;
      }
    }
    PGPrinter.error ("Matched EOF");
  }
}
