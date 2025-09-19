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
package com.helger.pgcc.parser.table;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.parser.CodeGenerator;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.TokenizerData;

/**
 * Class that implements a table driven code generator for the token manager in
 * java.
 */
public class TableDrivenJavaCodeGenerator implements TokenManagerCodeGenerator
{
  private static final String TokenManagerTemplate = "/templates/TableDrivenTokenManager.template";
  private final CodeGenerator m_codeGenerator = new CodeGenerator ();

  @Override
  public void generateCode (final TokenizerData tokenizerData)
  {
    final String superClass = (String) Options.getAllOptions ().get (Options.USEROPTION__TOKEN_MANAGER_SUPER_CLASS);

    final Map <String, Object> options = Options.getAllOptions ();
    options.put ("maxOrdinal", Integer.valueOf (tokenizerData.m_allMatches.size ()));
    options.put ("maxLexStates", Integer.valueOf (tokenizerData.m_lexStateNames.length));
    options.put ("stateSetSize", Integer.valueOf (tokenizerData.m_nfa.size ()));
    options.put ("parserName", tokenizerData.m_parserName);
    options.put ("maxLongs", Integer.valueOf (tokenizerData.m_allMatches.size () / 64 + 1));
    options.put ("charStreamName", CodeGenerator.getCharStreamName ());
    options.put ("defaultLexState", Integer.valueOf (tokenizerData.m_defaultLexState));
    options.put ("decls", tokenizerData.m_decls);
    options.put ("superClass", StringHelper.isEmpty (superClass) ? "" : "extends " + superClass);
    options.put ("noDfa", Boolean.valueOf (Options.isNoDfa ()));
    options.put ("generatedStates", Integer.valueOf (tokenizerData.m_nfa.size ()));
    try
    {
      m_codeGenerator.writeTemplate (TokenManagerTemplate, options);
      _dumpDfaTables (m_codeGenerator, tokenizerData);
      dumpNfaTables (m_codeGenerator, tokenizerData);
      _dumpMatchInfo (m_codeGenerator, tokenizerData);
    }
    catch (final IOException ioe)
    {
      assert (false);
    }
  }

  public void finish (final TokenizerData tokenizerData)
  {
    // TODO(sreeni) : Fix this mess.
    m_codeGenerator.genCodeLine ("\n}");
    if (!Options.isBuildParser ())
      return;
    final String fileName = Options.getOutputDirectory () + File.separator + tokenizerData.m_parserName + "TokenManager.java";
    m_codeGenerator.saveOutput (fileName);
  }

  private void _dumpDfaTables (final CodeGenerator codeGenerator, final TokenizerData tokenizerData)
  {
    final Map <Integer, int []> startAndSize = new HashMap <> ();
    int i = 0;

    codeGenerator.genCodeLine ("private static final int[] stringLiterals = {");
    for (final int key : tokenizerData.m_literalSequence.keySet ())
    {
      final int [] arr = new int [2];
      final List <String> l = tokenizerData.m_literalSequence.get (Integer.valueOf (key));
      final List <Integer> kinds = tokenizerData.m_literalKinds.get (Integer.valueOf (key));
      arr[0] = i;
      arr[1] = l.size ();
      int j = 0;
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      for (final String s : l)
      {
        if (j > 0)
          codeGenerator.genCodeLine (", ");
        codeGenerator.genCode (Integer.toString (s.length ()));
        for (int k = 0; k < s.length (); k++)
        {
          codeGenerator.genCode (", ");
          codeGenerator.genCode (Integer.toString (s.charAt (k)));
          i++;
        }
        final Integer kind = kinds.get (j);
        codeGenerator.genCode (", " + kind);
        codeGenerator.genCode (", " + tokenizerData.m_kindToNfaStartState.get (kind));
        i += 3;
        j++;
      }
      startAndSize.put (Integer.valueOf (key), arr);
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final java.util.Map<Integer, int[]> startAndSize =\n" +
                               "    new java.util.HashMap<Integer, int[]>();");

    // Static block to actually initialize the map from the int array above.
    codeGenerator.genCodeLine ("static {");
    for (final int key : tokenizerData.m_literalSequence.keySet ())
    {
      final int [] arr = startAndSize.get (Integer.valueOf (key));
      codeGenerator.genCodeLine ("startAndSize.put(" + key + ", new int[]{" + arr[0] + ", " + arr[1] + "});");
    }
    codeGenerator.genCodeLine ("}");
  }

  private void dumpNfaTables (final CodeGenerator codeGenerator, final TokenizerData tokenizerData)
  {
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();

    // WE do the following for java so that the generated code is reasonable
    // size and can be compiled. May not be needed for other languages.
    codeGenerator.genCodeLine ("private static final " + eOutputLanguage.getTypeLong () + "[][] jjCharData = {");
    final Map <Integer, TokenizerData.NfaState> nfa = tokenizerData.m_nfa;
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (Integer.valueOf (i));
      if (i > 0)
        codeGenerator.genCodeLine (",");
      if (tmp == null)
      {
        codeGenerator.genCode ("{}");
        continue;
      }
      codeGenerator.genCode ("{");
      final BitSet bits = new BitSet ();
      for (final char c : tmp.m_characters)
      {
        bits.set (c);
      }
      final long [] longs = bits.toLongArray ();
      for (int k = 0; k < longs.length; k++)
      {
        int rep = 1;
        while (k + rep < longs.length && longs[k + rep] == longs[k])
          rep++;
        if (k > 0)
          codeGenerator.genCode (", ");
        codeGenerator.genCode (rep + ", ");
        codeGenerator.genCode (eOutputLanguage.getLongHex (longs[k]));
        k += rep - 1;
      }
      codeGenerator.genCode ("}");
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final long[][] jjChars = ");
    codeGenerator.genCodeLine ("    new long[" + tokenizerData.m_nfa.size () + "][(Character.MAX_VALUE >> 6) + 1]; ");
    codeGenerator.genCodeLine ("static { ");
    codeGenerator.genCodeLine ("  for (int i = 0; i < " + tokenizerData.m_nfa.size () + "; i++) { ");
    codeGenerator.genCodeLine ("    int ind = 0; ");
    codeGenerator.genCodeLine ("    for (int j = 0; j < jjCharData[i].length; j += 2) { ");
    codeGenerator.genCodeLine ("      for (int k = 0; k < (int)jjCharData[i][j]; k++) { ");
    codeGenerator.genCodeLine ("        jjChars[i][ind++] = jjCharData[i][j + 1]; ");
    codeGenerator.genCodeLine ("      } ");
    codeGenerator.genCodeLine ("    } ");
    codeGenerator.genCodeLine ("  } ");
    codeGenerator.genCodeLine ("} ");

    codeGenerator.genCodeLine ("private static final int[][] jjcompositeState = {");
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (Integer.valueOf (i));
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      if (tmp == null)
      {
        codeGenerator.genCode ("{}");
        continue;
      }
      codeGenerator.genCode ("{");
      int k = 0;
      for (final Integer st : tmp.m_compositeStates)
      {
        if (k++ > 0)
          codeGenerator.genCode (", ");
        codeGenerator.genCode (st.toString ());
      }
      codeGenerator.genCode ("}");
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[] jjmatchKinds = {");
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (Integer.valueOf (i));
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      // TODO(sreeni) : Fix this mess.
      if (tmp == null)
      {
        codeGenerator.genCode (Integer.toString (Integer.MAX_VALUE));
        continue;
      }
      codeGenerator.genCode (Integer.toString (tmp.m_kind));
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[][] jjnextStateSet = {");
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (Integer.valueOf (i));
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      if (tmp == null)
      {
        codeGenerator.genCode ("{}");
        continue;
      }
      int k = 0;
      codeGenerator.genCode ("{");
      for (final Integer s : tmp.m_nextStates)
      {
        if (k++ > 0)
          codeGenerator.genCode (", ");
        codeGenerator.genCode (s.toString ());
      }
      codeGenerator.genCode ("}");
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[] jjInitStates = {");
    int k = 0;
    for (final Integer a : tokenizerData.m_initialStates.values ())
    {
      if (k++ > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode (a.toString ());
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[] canMatchAnyChar = {");
    k = 0;
    for (final Integer a : tokenizerData.m_wildcardKind.values ())
    {
      if (k++ > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode (a.toString ());
    }
    codeGenerator.genCodeLine ("};");
  }

  private void _dumpMatchInfo (final CodeGenerator codeGenerator, final TokenizerData tokenizerData)
  {
    final Map <Integer, TokenizerData.MatchInfo> allMatches = tokenizerData.m_allMatches;

    // A bit ugly.

    final BitSet toSkip = new BitSet (allMatches.size ());
    final BitSet toSpecial = new BitSet (allMatches.size ());
    final BitSet toMore = new BitSet (allMatches.size ());
    final BitSet toToken = new BitSet (allMatches.size ());
    final int [] newStates = new int [allMatches.size ()];
    toSkip.set (allMatches.size () + 1, true);
    toToken.set (allMatches.size () + 1, true);
    toMore.set (allMatches.size () + 1, true);
    toSpecial.set (allMatches.size () + 1, true);
    // Kind map.
    codeGenerator.genCodeLine ("public static final String[] jjstrLiteralImages = {");

    int k = 0;
    for (final Map.Entry <Integer, TokenizerData.MatchInfo> aEntry : allMatches.entrySet ())
    {
      final int i = aEntry.getKey ().intValue ();
      final TokenizerData.MatchInfo matchInfo = aEntry.getValue ();
      switch (matchInfo.m_matchType)
      {
        case SKIP:
          toSkip.set (i);
          break;
        case SPECIAL_TOKEN:
          toSpecial.set (i);
          break;
        case MORE:
          toMore.set (i);
          break;
        case TOKEN:
          toToken.set (i);
          break;
        default:
          throw new IllegalStateException ();
      }
      newStates[i] = matchInfo.m_newLexState;
      final String image = matchInfo.m_image;
      if (k++ > 0)
        codeGenerator.genCodeLine (", ");
      if (image != null)
      {
        codeGenerator.genCode ("\"");
        for (int j = 0; j < image.length (); j++)
        {
          if (image.charAt (j) <= 0xff)
          {
            codeGenerator.genCode ("\\" + Integer.toOctalString (image.charAt (j)));
          }
          else
          {
            String hexVal = Integer.toHexString (image.charAt (j));
            if (hexVal.length () == 3)
              hexVal = "0" + hexVal;
            codeGenerator.genCode ("\\u" + hexVal);
          }
        }
        codeGenerator.genCode ("\"");
      }
      else
      {
        codeGenerator.genCodeLine ("null");
      }
    }
    codeGenerator.genCodeLine ("};");

    // Now generate the bit masks.
    generateBitVector ("jjtoSkip", toSkip, codeGenerator);
    generateBitVector ("jjtoSpecial", toSpecial, codeGenerator);
    generateBitVector ("jjtoMore", toMore, codeGenerator);
    generateBitVector ("jjtoToken", toToken, codeGenerator);

    codeGenerator.genCodeLine ("private static final int[] jjnewLexState = {");
    for (int i = 0; i < newStates.length; i++)
    {
      if (i > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode ("0x" + Integer.toHexString (newStates[i]));
    }
    codeGenerator.genCodeLine ("};");

    // Action functions.

    // Token actions.
    codeGenerator.genCodeLine ("void TokenLexicalActions(Token matchedToken) {");
    _dumpLexicalActions (allMatches, TokenizerData.EMatchType.TOKEN, "matchedToken.kind", codeGenerator);
    codeGenerator.genCodeLine ("}");

    // Skip actions.
    // TODO(sreeni) : Streamline this mess.

    codeGenerator.genCodeLine ("void SkipLexicalActions(Token matchedToken) {");
    _dumpLexicalActions (allMatches, TokenizerData.EMatchType.SKIP, "jjmatchedKind", codeGenerator);
    _dumpLexicalActions (allMatches, TokenizerData.EMatchType.SPECIAL_TOKEN, "jjmatchedKind", codeGenerator);
    codeGenerator.genCodeLine ("}");

    // More actions.
    codeGenerator.genCodeLine ("void MoreLexicalActions() {");
    codeGenerator.genCodeLine ("jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    _dumpLexicalActions (allMatches, TokenizerData.EMatchType.MORE, "jjmatchedKind", codeGenerator);
    codeGenerator.genCodeLine ("}");

    codeGenerator.genCodeLine ("public String[] lexStateNames = {");
    for (int i = 0; i < tokenizerData.m_lexStateNames.length; i++)
    {
      if (i > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode ("\"" + tokenizerData.m_lexStateNames[i] + "\"");
    }
    codeGenerator.genCodeLine ("};");
  }

  private void _dumpLexicalActions (final Map <Integer, TokenizerData.MatchInfo> allMatches,
                                    final TokenizerData.EMatchType matchType,
                                    final String kindString,
                                    final CodeGenerator codeGenerator)
  {
    codeGenerator.genCodeLine ("  switch(" + kindString + ") {");
    for (final int i : allMatches.keySet ())
    {
      final TokenizerData.MatchInfo matchInfo = allMatches.get (Integer.valueOf (i));
      if (matchInfo.m_action == null || matchInfo.m_matchType != matchType)
      {
        continue;
      }
      codeGenerator.genCodeLine ("    case " + i + ": {\n");
      codeGenerator.genCodeLine ("      " + matchInfo.m_action);
      codeGenerator.genCodeLine ("      break;");
      codeGenerator.genCodeLine ("    }");
    }
    codeGenerator.genCodeLine ("    default: break;");
    codeGenerator.genCodeLine ("  }");
  }

  private static void generateBitVector (final String name, final BitSet bits, final CodeGenerator codeGenerator)
  {
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();
    codeGenerator.genCodeLine ("private static final " + eOutputLanguage.getTypeLong () + "[] " + name + " = {");
    final long [] longs = bits.toLongArray ();
    for (int i = 0; i < longs.length; i++)
    {
      if (i > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode (eOutputLanguage.getLongHex (longs[i]));
    }
    codeGenerator.genCodeLine ("};");
  }
}
