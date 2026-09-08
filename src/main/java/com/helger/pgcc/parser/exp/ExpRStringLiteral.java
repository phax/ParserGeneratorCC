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
package com.helger.pgcc.parser.exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.base.reflection.GenericReflection;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.StringLiteralBuildState;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.output.java.LexGenJava;
import com.helger.pgcc.parser.CodeGenerator;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.Token;
import com.helger.pgcc.parser.TokenizerData;

/**
 * Describes string literals.
 */
public final class ExpRStringLiteral extends AbstractExpRegularExpression
{
  /**
   * @return The build state of the lexical state that is currently being generated. Never
   *         <code>null</code>.
   */
  public static StringLiteralBuildState strLit ()
  {
    return LexGenJava.lexer ().stringLiterals ();
  }

  /**
   * Which kinds can still match at one character position of the string literal trie. Public
   * because {@link com.helger.pgcc.context.StringLiteralBuildState} holds a list of them.
   */
  public static final class KindInfo
  {
    private final long [] m_aValidKinds;
    private final long [] m_aFinalKinds;
    private int m_nValidKindCnt = 0;
    private int m_nFinalKindCnt = 0;
    private final Set <Integer> m_aFinalKindSet = new HashSet <> ();
    private final Set <Integer> m_aValidKindSet = new HashSet <> ();

    KindInfo (final int nMaxKind)
    {
      m_aValidKinds = new long [nMaxKind / 64 + 1];
      m_aFinalKinds = new long [nMaxKind / 64 + 1];
    }

    public void insertValidKind (final int nKind)
    {
      m_aValidKinds[nKind / 64] |= (1L << (nKind % 64));
      m_nValidKindCnt++;
      m_aValidKindSet.add (Integer.valueOf (nKind));
    }

    public void insertFinalKind (final int nKind)
    {
      m_aFinalKinds[nKind / 64] |= (1L << (nKind % 64));
      m_nFinalKindCnt++;
      m_aFinalKindSet.add (Integer.valueOf (nKind));
    }
  }

  /**
   * The string image of the literal.
   */
  public String m_sImage;

  public ExpRStringLiteral (final Token t, final String sImage)
  {
    setLineNumber (t.beginLine);
    setColumnNumber (t.beginColumn);
    m_sImage = sImage;
  }

  // with single char keys;

  /**
   * Initialize all the static variables, so that there is no interference between the various
   * states of the lexer. Need to call this method after generating code for each lexical state.
   */
  public static void reInitStatic ()
  {
    strLit ().resetForLexicalState ();
  }

  public static void dumpStrLiteralImages (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        dumpStrLiteralImagesForJava (aCodeGenerator);
        return;
      case CPP:
        // For C++
        String sImage;
        int i;
        strLit ().setCharCnt (0); // Set to zero in reInit() but just to be sure

        aCodeGenerator.genCodeNewLine ();
        aCodeGenerator.genCodeLine ("/** Token literal values. */");
        int nLiteralCount = 0;
        aCodeGenerator.switchToStaticsFile ();

        if (strLit ().getAllImages () == null || strLit ().getAllImages ().length == 0)
        {
          aCodeGenerator.genCodeLine ("static const JJString jjstrLiteralImages[] = {};");
          return;
        }

        strLit ().getAllImages ()[0] = "";
        for (i = 0; i < strLit ().getAllImages ().length; i++)
        {
          if ((sImage = strLit ().getAllImages ()[i]) == null ||
            ((LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L &&
              (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) == 0L &&
              (LexGenJava.lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L) ||
            (LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) != 0L ||
            (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) != 0L ||
            LexGenJava.lexer ().getCanReachOnMore ()[LexGenJava.lexer ().getLexStates ()[i]] ||
            ((Options.isIgnoreCase () || LexGenJava.lexer ().getIgnoreCase ()[i]) &&
              (!sImage.equals (sImage.toLowerCase (Locale.US)) || !sImage.equals (sImage.toUpperCase (Locale.US)))))
          {
            strLit ().getAllImages ()[i] = null;
            strLit ().setCharCnt (strLit ().getCharCnt () + 6);
            if (strLit ().getCharCnt () > 80)
            {
              aCodeGenerator.genCodeNewLine ();
              strLit ().setCharCnt (0);
            }

            aCodeGenerator.genCodeLine ("static JJChar jjstrLiteralChars_" + nLiteralCount++ + "[] = {0};");
            continue;
          }

          String sToPrint = "static JJChar jjstrLiteralChars_" + nLiteralCount++ + "[] = {";
          for (int j = 0; j < sImage.length (); j++)
          {
            sToPrint += "0x" + Integer.toHexString (sImage.charAt (j)) + ", ";
          }

          // Null char
          sToPrint += "0 };";

          strLit ().setCharCnt (strLit ().getCharCnt () + sToPrint.length ());
          if (strLit ().getCharCnt () >= 80)
          {
            aCodeGenerator.genCodeNewLine ();
            strLit ().setCharCnt (0);
          }

          aCodeGenerator.genCodeLine (sToPrint);
        }

        while (++i < LexGenJava.lexer ().getMaxOrdinal ())
        {
          strLit ().setCharCnt (strLit ().getCharCnt () + 6);
          if (strLit ().getCharCnt () > 80)
          {
            aCodeGenerator.genCodeNewLine ();
            strLit ().setCharCnt (0);
          }

          aCodeGenerator.genCodeLine ("static JJChar jjstrLiteralChars_" + nLiteralCount++ + "[] = {0};");
          continue;
        }

        // Generate the array here.
        aCodeGenerator.genCodeLine ("static const JJString " + "jjstrLiteralImages[] = {");
        for (int j = 0; j < nLiteralCount; j++)
        {
          aCodeGenerator.genCodeLine ("jjstrLiteralChars_" + j + ", ");
        }
        aCodeGenerator.genCodeLine ("};");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  public static void dumpStrLiteralImagesForJava (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    String sImage;
    int i;
    strLit ().setCharCnt (0); // Set to zero in reInit() but just to be sure

    aCodeGenerator.genCodeNewLine ();
    aCodeGenerator.genCodeLine ("/** Token literal values. */");
    aCodeGenerator.genCodeLine ("public static final String[] jjstrLiteralImages = {");

    if (strLit ().getAllImages () == null || strLit ().getAllImages ().length == 0)
    {
      aCodeGenerator.genCodeLine ("};");
      return;
    }

    strLit ().getAllImages ()[0] = "";
    for (i = 0; i < strLit ().getAllImages ().length; i++)
    {
      if ((sImage = strLit ().getAllImages ()[i]) == null ||
        ((LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L &&
          (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) == 0L &&
          (LexGenJava.lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L) ||
        (LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) != 0L ||
        (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) != 0L ||
        LexGenJava.lexer ().getCanReachOnMore ()[LexGenJava.lexer ().getLexStates ()[i]] ||
        ((Options.isIgnoreCase () || LexGenJava.lexer ().getIgnoreCase ()[i]) &&
          (!sImage.equals (sImage.toLowerCase (Locale.US)) || !sImage.equals (sImage.toUpperCase (Locale.US)))))
      {
        strLit ().getAllImages ()[i] = null;
        strLit ().setCharCnt (strLit ().getCharCnt () + 6);
        if (strLit ().getCharCnt () > 80)
        {
          aCodeGenerator.genCodeNewLine ();
          strLit ().setCharCnt (0);
        }

        aCodeGenerator.genCode ("null, ");
        continue;
      }

      final StringBuilder aToPrint = new StringBuilder ("\"");
      for (int j = 0; j < sImage.length (); j++)
      {
        final char c = sImage.charAt (j);
        switch (eOutputLanguage)
        {
          case JAVA:
            if (c <= 0xff)
              aToPrint.append ('\\').append (Integer.toOctalString (c));
            else
            {
              String sHexVal = Integer.toHexString (c);
              if (sHexVal.length () == 3)
                sHexVal = "0" + sHexVal;
              aToPrint.append ("\\u").append (sHexVal);
            }
            break;
          case CPP:
            String sHexVal = Integer.toHexString (c);
            if (sHexVal.length () == 3)
              sHexVal = "0" + sHexVal;
            aToPrint.append ("\\u").append (sHexVal);
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }
      }

      aToPrint.append ("\", ");

      strLit ().setCharCnt (strLit ().getCharCnt () + aToPrint.length ());
      if (strLit ().getCharCnt () > 80)
      {
        // Break after 80 chars
        aCodeGenerator.genCodeNewLine ();
        strLit ().setCharCnt (0);
      }

      aCodeGenerator.genCode (aToPrint.toString ());
    }

    while (++i < LexGenJava.lexer ().getMaxOrdinal ())
    {
      strLit ().setCharCnt (strLit ().getCharCnt () + 6);
      if (strLit ().getCharCnt () > 80)
      {
        // Break after 80 chars
        aCodeGenerator.genCodeNewLine ();
        strLit ().setCharCnt (0);
      }

      aCodeGenerator.genCode ("null, ");
    }

    aCodeGenerator.genCodeLine ("};");
  }

  /**
   * Used for top level string literals.
   */
  public void generateDfa ()
  {
    String s;
    Map <String, KindInfo> aTemp;

    if (strLit ().getMaxStrKind () <= getOrdinal ())
      strLit ().setMaxStrKind (getOrdinal () + 1);

    final int nLen = m_sImage.length ();
    if (nLen > strLit ().getMaxLen ())
      strLit ().setMaxLen (nLen);

    for (int i = 0; i < nLen; i++)
    {
      final char c = m_sImage.charAt (i);
      if (Options.isIgnoreCase ())
        s = Character.toString (Character.toLowerCase (c));
      else
        s = Character.toString (c);

      if (!NfaState.nfa ().isUnicodeWarningGiven () &&
        c > 0xff &&
        !Options.isJavaUnicodeEscape () &&
        !Options.isJavaUserCharStream ())
      {
        NfaState.nfa ().setUnicodeWarningGiven (true);
        JavaCCErrors.warning (LexGenJava.lexer ().getCurRE (),
                              "Non-ASCII characters used in regular expression." +
                                                               "Please make sure you use the correct Reader when you create the parser, " +
                                                               "one that can handle your character set.");
      }

      if (i >= strLit ().getCharPosKind ().size ()) // Kludge, but OK
      {
        aTemp = new HashMap <> ();
        strLit ().getCharPosKind ().add (aTemp);
      }
      else
        aTemp = strLit ().getCharPosKind ().get (i);

      KindInfo aInfo = aTemp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.lexer ().getMaxOrdinal ()));

      if (i + 1 == nLen)
        aInfo.insertFinalKind (getOrdinal ());
      else
        aInfo.insertValidKind (getOrdinal ());

      if (!Options.isIgnoreCase () &&
        LexGenJava.lexer ().getIgnoreCase ()[getOrdinal ()] &&
        c != Character.toLowerCase (c))
      {
        s = Character.toString (Character.toLowerCase (c));

        if (i >= strLit ().getCharPosKind ().size ()) // Kludge, but OK
        {
          aTemp = new HashMap <> ();
          strLit ().getCharPosKind ().add (aTemp);
        }
        else
          aTemp = strLit ().getCharPosKind ().get (i);

        aInfo = aTemp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.lexer ().getMaxOrdinal ()));

        if (i + 1 == nLen)
          aInfo.insertFinalKind (getOrdinal ());
        else
          aInfo.insertValidKind (getOrdinal ());
      }

      if (!Options.isIgnoreCase () &&
        LexGenJava.lexer ().getIgnoreCase ()[getOrdinal ()] &&
        c != Character.toUpperCase (c))
      {
        s = Character.toString (Character.toUpperCase (c));

        // Kludge, but OK
        if (i >= strLit ().getCharPosKind ().size ())
        {
          aTemp = new HashMap <> ();
          strLit ().getCharPosKind ().add (aTemp);
        }
        else
          aTemp = strLit ().getCharPosKind ().get (i);

        aInfo = aTemp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.lexer ().getMaxOrdinal ()));

        if (i + 1 == nLen)
          aInfo.insertFinalKind (getOrdinal ());
        else
          aInfo.insertValidKind (getOrdinal ());
      }
    }

    strLit ().getMaxLenForActive ()[getOrdinal () / 64] = Math.max (strLit ().getMaxLenForActive ()[getOrdinal () / 64],
                                                                    nLen - 1);
    strLit ().getAllImages ()[getOrdinal ()] = m_sImage;
  }

  @Override
  public Nfa generateNfa (final boolean bIgnoreCase)
  {
    if (m_sImage.length () == 1)
    {
      final ExpRCharacterList aTemp = new ExpRCharacterList (m_sImage.charAt (0));
      return aTemp.generateNfa (bIgnoreCase);
    }

    NfaState aStartState = new NfaState ();
    final NfaState aTheStartState = aStartState;
    NfaState aFinalState = null;

    if (m_sImage.length () == 0)
      return new Nfa (aTheStartState, aTheStartState);

    int i;

    for (i = 0; i < m_sImage.length (); i++)
    {
      aFinalState = new NfaState ();
      aStartState.m_aCharMoves = new char [1];
      aStartState.addChar (m_sImage.charAt (i));

      if (Options.isIgnoreCase () || bIgnoreCase)
      {
        aStartState.addChar (Character.toLowerCase (m_sImage.charAt (i)));
        aStartState.addChar (Character.toUpperCase (m_sImage.charAt (i)));
      }

      aStartState.m_aNext = aFinalState;
      aStartState = aFinalState;
    }

    return new Nfa (aTheStartState, aFinalState);
  }

  static void dumpNullStrLiterals (final CodeGenerator aCodeGenerator)
  {
    aCodeGenerator.genCodeLine ("{");

    if (NfaState.nfa ().getGeneratedStates () != 0)
      aCodeGenerator.genCodeLine ("   return jjMoveNfa" +
                                 LexGenJava.lexer ().getLexStateSuffix () +
                                 "(" +
                                 NfaState.initStateName () +
                                 ", 0);");
    else
      aCodeGenerator.genCodeLine ("   return 1;");

    aCodeGenerator.genCodeLine ("}");
  }

  private static int _getStateSetForKind (final int nPos, final int nKind)
  {
    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] ||
      NfaState.nfa ().getGeneratedStates () == 0)
      return -1;

    final Map <String, long []> aAllStateSets = strLit ().getStatesForPos ()[nPos];

    if (aAllStateSets == null)
      return -1;

    for (final Map.Entry <String, long []> aEntry : aAllStateSets.entrySet ())
    {
      String s = aEntry.getKey ();
      final long [] aActives = aEntry.getValue ();

      s = s.substring (s.indexOf (", ") + 2);
      s = s.substring (s.indexOf (", ") + 2);

      if (s.equals ("null;"))
        continue;

      if (aActives != null && (aActives[nKind / 64] & (1L << (nKind % 64))) != 0L)
      {
        return NfaState.addStartStateSet (s);
      }
    }

    return -1;
  }

  static String getLabel (final int nKind)
  {
    final AbstractExpRegularExpression aRe = LexGenJava.lexer ().getRexprs ()[nKind];

    if (aRe instanceof ExpRStringLiteral)
      return " \"" + JavaCCGlobals.addEscapes (((ExpRStringLiteral) aRe).m_sImage) + "\"";
    if (aRe.hasLabel ())
      return " <" + aRe.getLabel () + ">";
    return " <token of kind " + nKind + ">";
  }

  static int getLineNumber (final int nKind)
  {
    return LexGenJava.lexer ().getRexprs ()[nKind].getLineNumber ();
  }

  static int getColumnNumber (final int nKind)
  {
    return LexGenJava.lexer ().getRexprs ()[nKind].getColumnNumber ();
  }

  /**
   * Returns true if s1 starts with s2 (ignoring case for each character).
   */
  private static boolean _startsWithIgnoreCase (final String s1, final String s2)
  {
    if (s1.length () < s2.length ())
      return false;

    for (int i = 0; i < s2.length (); i++)
    {
      final char c1 = s1.charAt (i);
      final char c2 = s2.charAt (i);

      if (c1 != c2 && Character.toLowerCase (c2) != c1 && Character.toUpperCase (c2) != c1)
        return false;
    }

    return true;
  }

  public static void fillSubString ()
  {
    strLit ().setSubString (new boolean [strLit ().getMaxStrKind () + 1]);
    strLit ().setSubStringAtPos (new boolean [strLit ().getMaxLen ()]);

    for (int i = 0; i < strLit ().getMaxStrKind (); i++)
    {
      strLit ().getSubString ()[i] = false;

      final String sImage = strLit ().getAllImages ()[i];
      if (sImage == null || LexGenJava.lexer ().getLexStates ()[i] != LexGenJava.lexer ().getLexStateIndex ())
        continue;

      if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
      {
        // We will not optimize for mixed case
        strLit ().getSubString ()[i] = true;
        strLit ().getSubStringAtPos ()[sImage.length () - 1] = true;
        continue;
      }

      for (int j = 0; j < strLit ().getMaxStrKind (); j++)
      {
        if (j != i &&
          LexGenJava.lexer ().getLexStates ()[j] == LexGenJava.lexer ().getLexStateIndex () &&
          (strLit ().getAllImages ()[j]) != null)
        {
          if (strLit ().getAllImages ()[j].indexOf (sImage) == 0)
          {
            strLit ().getSubString ()[i] = true;
            strLit ().getSubStringAtPos ()[sImage.length () - 1] = true;
            break;
          }
          else
            if (Options.isIgnoreCase () && _startsWithIgnoreCase (strLit ().getAllImages ()[j], sImage))
            {
              strLit ().getSubString ()[i] = true;
              strLit ().getSubStringAtPos ()[sImage.length () - 1] = true;
              break;
            }
        }
      }
    }
  }

  static void dumpStartWithStates (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("private int jjStartNfaWithStates" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(int pos, int kind, int state)");
        break;
      case CPP:
        aCodeGenerator.generateMethodDefHeader ("int",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStartNfaWithStates" +
                                                                                            LexGenJava.lexer ()
                                                                                                      .getLexStateSuffix () +
                                                                                            "(int pos, int kind, int state)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   jjmatchedKind = kind;");
    aCodeGenerator.genCodeLine ("   jjmatchedPos = pos;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("   debugStream.println(\"   No more string literal token matches are possible.\");");
          aCodeGenerator.genCodeLine ("   debugStream.println(\"   Currently matched the first \" " +
                                     "+ (jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
        aCodeGenerator.genCodeLine ("   catch(java.io.IOException e) { return pos + 1; }");
        break;
      case CPP:
        aCodeGenerator.genCodeLine ("   if (input_stream->endOfInput()) { return pos + 1; }");
        aCodeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("   debugStream.println(" +
                                     (LexGenJava.lexer ().getMaxLexStates () > 1
                                                                                 ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                 : "") +
                                     "\"Current character : \" + " +
                                     Options.getTokenMgrErrorClass () +
                                     ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, curChar, " +
                                     "input_stream->getEndLine(), input_stream->getEndColumn());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    aCodeGenerator.genCodeLine ("   return jjMoveNfa" + LexGenJava.lexer ().getLexStateSuffix () + "(state, pos + 1);");
    aCodeGenerator.genCodeLine ("}");
  }

  static void dumpBoilerPlate (final CodeGenerator aCodeGenerator)
  {
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCodeLine ("private int jjStopAtPos(int pos, int kind)");
        break;
      case CPP:
        aCodeGenerator.generateMethodDefHeader (" int ",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStopAtPos(int pos, int kind)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    aCodeGenerator.genCodeLine ("{");
    aCodeGenerator.genCodeLine ("   jjmatchedKind = kind;");
    aCodeGenerator.genCodeLine ("   jjmatchedPos = pos;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("   debugStream.println(\"   No more string literal token matches are possible.\");");
          aCodeGenerator.genCodeLine ("   debugStream.println(\"   Currently matched the first \" + (jjmatchedPos + 1) + " +
                                     "\" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          aCodeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    aCodeGenerator.genCodeLine ("   return pos + 1;");
    aCodeGenerator.genCodeLine ("}");
  }

  private static String [] _reArrange (final Map <String, KindInfo> aTab)
  {
    final String [] aRet = new String [aTab.size ()];
    int nCnt = 0;

    for (final String s : aTab.keySet ())
    {
      final char c = s.charAt (0);

      int i = 0;
      while (i < nCnt && aRet[i].charAt (0) < c)
        i++;

      if (i < nCnt)
        for (int j = nCnt - 1; j >= i; j--)
          aRet[j + 1] = aRet[j];

      aRet[i] = s;
      nCnt++;
    }

    return aRet;
  }

  @NonNull
  private static String _getCaseChar (final char c, final EOutputLanguage eOutputLanguage)
  {
    if (false)
      return Integer.toString (c);

    // Just for better readability
    if (c < 0x20 || c >= 0x7f)
      return Integer.toString (c);

    if (eOutputLanguage.isJava ())
      if (c == '\'' || c == '\\')
        return "'\\" + c + "'";

    return "'" + c + "'";
  }

  public static void dumpDfaCode (final CodeGenerator aCodeGenerator)
  {
    Map <String, KindInfo> aTab;
    String sKey;
    KindInfo aInfo;
    final int nMaxLongsReqd = strLit ().getMaxStrKind () / 64 + 1;
    boolean bIfGenerated;
    LexGenJava.lexer ().getMaxLongsReqd ()[LexGenJava.lexer ().getLexStateIndex ()] = nMaxLongsReqd;
    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();

    if (strLit ().getMaxLen () == 0)
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("private int jjMoveStringLiteralDfa0" +
                                     LexGenJava.lexer ().getLexStateSuffix () +
                                     "()");
          break;
        case CPP:
          aCodeGenerator.generateMethodDefHeader (" int ",
                                                 LexGenJava.lexer ().getTokenMgrClassName (),
                                                 "jjMoveStringLiteralDfa0" +
                                                                                              LexGenJava.lexer ()
                                                                                                        .getLexStateSuffix () +
                                                                                              "()");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      dumpNullStrLiterals (aCodeGenerator);
      return;
    }

    if (!strLit ().isBoilerPlateDumped ())
    {
      dumpBoilerPlate (aCodeGenerator);
      strLit ().setBoilerPlateDumped (true);
    }

    boolean bCreateStartNfa = false;
    for (int i = 0; i < strLit ().getMaxLen (); i++)
    {
      boolean bAtLeastOne = false;
      boolean bStartNfaNeeded = false;
      aTab = strLit ().getCharPosKind ().get (i);
      final String [] aKeys = _reArrange (aTab);

      final StringBuilder aParams = new StringBuilder ();
      aParams.append ("(");
      if (i != 0)
      {
        if (i == 1)
        {
          int j = 0;
          for (; j < nMaxLongsReqd - 1; j++)
            if (i <= strLit ().getMaxLenForActive ()[j])
            {
              if (bAtLeastOne)
                aParams.append (", ");
              else
                bAtLeastOne = true;
              aParams.append (eOutputLanguage.getTypeLong () + " active" + j);
            }

          if (i <= strLit ().getMaxLenForActive ()[j])
          {
            if (bAtLeastOne)
              aParams.append (", ");
            aParams.append (eOutputLanguage.getTypeLong () + " active" + j);
          }
        }
        else
        {
          int j = 0;
          for (; j < nMaxLongsReqd - 1; j++)
            if (i <= strLit ().getMaxLenForActive ()[j] + 1)
            {
              if (bAtLeastOne)
                aParams.append (", ");
              else
                bAtLeastOne = true;
              aParams.append (eOutputLanguage.getTypeLong () +
                             " old" +
                             j +
                             ", " +
                             eOutputLanguage.getTypeLong () +
                             " active" +
                             j);
            }

          if (i <= strLit ().getMaxLenForActive ()[j] + 1)
          {
            if (bAtLeastOne)
              aParams.append (", ");
            aParams.append (eOutputLanguage.getTypeLong () +
                           " old" +
                           j +
                           ", " +
                           eOutputLanguage.getTypeLong () +
                           " active" +
                           j);
          }
        }
      }
      aParams.append (")");

      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCode ("private int jjMoveStringLiteralDfa" +
                                 i +
                                 LexGenJava.lexer ().getLexStateSuffix () +
                                 aParams);
          break;
        case CPP:
          aCodeGenerator.generateMethodDefHeader (" int ",
                                                 LexGenJava.lexer ().getTokenMgrClassName (),
                                                 "jjMoveStringLiteralDfa" +
                                                                                              i +
                                                                                              LexGenJava.lexer ()
                                                                                                        .getLexStateSuffix () +
                                                                                              aParams);
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }

      aCodeGenerator.genCodeLine ("{");

      if (i != 0)
      {
        if (i > 1)
        {
          bAtLeastOne = false;
          aCodeGenerator.genCode ("   if ((");

          int j = 0;
          for (; j < nMaxLongsReqd - 1; j++)
            if (i <= strLit ().getMaxLenForActive ()[j] + 1)
            {
              if (bAtLeastOne)
                aCodeGenerator.genCode (" | ");
              else
                bAtLeastOne = true;
              aCodeGenerator.genCode ("(active" + j + " &= old" + j + ")");
            }

          if (i <= strLit ().getMaxLenForActive ()[j] + 1)
          {
            if (bAtLeastOne)
              aCodeGenerator.genCode (" | ");
            aCodeGenerator.genCode ("(active" + j + " &= old" + j + ")");
          }

          aCodeGenerator.genCodeLine (") == 0L)");
          if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
            NfaState.nfa ().getGeneratedStates () != 0)
          {
            aCodeGenerator.genCode ("      return jjStartNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(" +
                                   (i - 2) +
                                   ", ");
            for (j = 0; j < nMaxLongsReqd - 1; j++)
              if (i <= strLit ().getMaxLenForActive ()[j] + 1)
                aCodeGenerator.genCode ("old" + j + ", ");
              else
                aCodeGenerator.genCode ("0L, ");
            if (i <= strLit ().getMaxLenForActive ()[j] + 1)
              aCodeGenerator.genCodeLine ("old" + j + ");");
            else
              aCodeGenerator.genCodeLine ("0L);");
          }
          else
            if (NfaState.nfa ().getGeneratedStates () != 0)
              aCodeGenerator.genCodeLine ("      return jjMoveNfa" +
                                         LexGenJava.lexer ().getLexStateSuffix () +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", " +
                                         (i - 1) +
                                         ");");
            else
              aCodeGenerator.genCodeLine ("      return " + i + ";");
        }

        if (i != 0 && Options.isDebugTokenManager ())
        {
          switch (eOutputLanguage)
          {
            case JAVA:
              aCodeGenerator.genCodeLine ("   if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                         Integer.toHexString (Integer.MAX_VALUE) +
                                         ")");
              aCodeGenerator.genCodeLine ("      debugStream.println(\"   Currently matched the first \" + " +
                                         "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
              aCodeGenerator.genCodeLine ("   debugStream.println(\"   Possible string literal matches : { \"");
              break;
            case CPP:
              aCodeGenerator.genCodeLine ("   if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                         Integer.toHexString (Integer.MAX_VALUE) +
                                         ")");
              aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1), addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
              aCodeGenerator.genCodeLine ("   fprintf(debugStream, \"   Possible string literal matches : { \");");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }

          final StringBuilder aFmt = new StringBuilder ();
          final StringBuilder aArgs = new StringBuilder ();
          for (int nVecs = 0; nVecs < strLit ().getMaxStrKind () / 64 + 1; nVecs++)
          {
            if (i <= strLit ().getMaxLenForActive ()[nVecs])
            {
              switch (eOutputLanguage)
              {
                case JAVA:
                  aCodeGenerator.genCodeLine (" +");
                  aCodeGenerator.genCode ("         jjKindsForBitVector(" + nVecs + ", ");
                  aCodeGenerator.genCode ("active" + nVecs + ") ");
                  break;
                case CPP:
                  if (aFmt.length () > 0)
                  {
                    aFmt.append (", ");
                    aArgs.append (", ");
                  }
                  aFmt.append ("%s");
                  aArgs.append ("         jjKindsForBitVector(" + nVecs + ", ");
                  aArgs.append ("active" + nVecs + ").c_str() ");
                  break;
                default:
                  throw new UnsupportedOutputLanguageException (eOutputLanguage);
              }
            }
          }

          switch (eOutputLanguage)
          {
            case JAVA:
              aCodeGenerator.genCodeLine (" + \" } \");");
              break;
            case CPP:
              aFmt.append ("}\\n");
              aCodeGenerator.genCodeLine ("    fprintf(debugStream, \"" + aFmt + "\"," + aArgs + ");");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }
        }

        switch (eOutputLanguage)
        {
          case JAVA:
            aCodeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
            aCodeGenerator.genCodeLine ("   catch(java.io.IOException e) {");
            break;
          case CPP:
            aCodeGenerator.genCodeLine ("   if (input_stream->endOfInput()) {");
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }

        if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
          NfaState.nfa ().getGeneratedStates () != 0)
        {
          aCodeGenerator.genCode ("      jjStopStringLiteralDfa" +
                                 LexGenJava.lexer ().getLexStateSuffix () +
                                 "(" +
                                 (i - 1) +
                                 ", ");

          int k = 0;
          for (; k < nMaxLongsReqd - 1; k++)
          {
            if (i <= strLit ().getMaxLenForActive ()[k])
              aCodeGenerator.genCode ("active" + k + ", ");
            else
              aCodeGenerator.genCode ("0L, ");
          }

          if (i <= strLit ().getMaxLenForActive ()[k])
          {
            aCodeGenerator.genCodeLine ("active" + k + ");");
          }
          else
          {
            aCodeGenerator.genCodeLine ("0L);");
          }

          if (i != 0 && Options.isDebugTokenManager ())
          {
            switch (eOutputLanguage)
            {
              case JAVA:
                aCodeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                           Integer.toHexString (Integer.MAX_VALUE) +
                                           ")");
                aCodeGenerator.genCodeLine ("         debugStream.println(\"   Currently matched the first \" + " +
                                           "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
                break;
              case CPP:
                aCodeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                           Integer.toHexString (Integer.MAX_VALUE) +
                                           ")");
                aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
                break;
              default:
                throw new UnsupportedOutputLanguageException (eOutputLanguage);
            }
          }

          aCodeGenerator.genCodeLine ("      return " + i + ";");
        }
        else
          if (NfaState.nfa ().getGeneratedStates () != 0)
          {
            aCodeGenerator.genCodeLine ("   return jjMoveNfa" +
                                       LexGenJava.lexer ().getLexStateSuffix () +
                                       "(" +
                                       NfaState.initStateName () +
                                       ", " +
                                       (i - 1) +
                                       ");");
          }
          else
          {
            aCodeGenerator.genCodeLine ("      return " + i + ";");
          }

        aCodeGenerator.genCodeLine ("   }");
      }

      if (i != 0)
      {
        switch (eOutputLanguage)
        {
          case JAVA:
            // Nothing
            break;
          case CPP:
            aCodeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }

        if (Options.isDebugTokenManager ())
        {
          switch (eOutputLanguage)
          {
            case JAVA:
              aCodeGenerator.genCodeLine ("   debugStream.println(" +
                                         (LexGenJava.lexer ().getMaxLexStates () > 1
                                                                                     ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                     : "") +
                                         "\"Current character : \" + " +
                                         Options.getTokenMgrErrorClass () +
                                         ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                                         "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
              break;
            case CPP:
              aCodeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                         "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                         "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, curChar, " +
                                         "input_stream->getEndLine(), input_stream->getEndColumn());");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }
        }
      }

      aCodeGenerator.genCodeLine ("   switch(curChar)");
      aCodeGenerator.genCodeLine ("   {");

      CaseLoop: for (final String aKey : aKeys)
      {
        sKey = aKey;
        aInfo = aTab.get (sKey);
        bIfGenerated = false;
        final char c = sKey.charAt (0);

        if (i == 0 &&
          c < 128 &&
          aInfo.m_nFinalKindCnt != 0 &&
          (NfaState.nfa ().getGeneratedStates () == 0 || !NfaState.canStartNfaUsingAscii (c)))
        {
          int nKind;
          int j = 0;
          for (; j < nMaxLongsReqd; j++)
            if (aInfo.m_aFinalKinds[j] != 0L)
              break;

          for (int k = 0; k < 64; k++)
            if ((aInfo.m_aFinalKinds[j] & (1L << k)) != 0L && !strLit ().getSubString ()[nKind = (j * 64 + k)])
            {
              if ((strLit ().getIntermediateKinds () != null &&
                strLit ().getIntermediateKinds ()[(j * 64 + k)] != null &&
                strLit ().getIntermediateKinds ()[(j * 64 + k)][i] < (j * 64 + k) &&
                strLit ().getIntermediateMatchedPos () != null &&
                strLit ().getIntermediateMatchedPos ()[(j * 64 + k)][i] == i) ||
                (LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()] >= 0 &&
                  LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()] < (j * 64 + k)))
                break;
              else
                if ((LexGenJava.lexer ().getToSkip ()[nKind / 64] & (1L << (nKind % 64))) != 0L &&
                  (LexGenJava.lexer ().getToSpecial ()[nKind / 64] & (1L << (nKind % 64))) == 0L &&
                  LexGenJava.lexer ().getActions ()[nKind] == null &&
                  LexGenJava.lexer ().getNewLexState ()[nKind] == null)
                {
                  LexGenJava.addCharToSkip (c, nKind);

                  if (Options.isIgnoreCase ())
                  {
                    if (c != Character.toUpperCase (c))
                      LexGenJava.addCharToSkip (Character.toUpperCase (c), nKind);

                    if (c != Character.toLowerCase (c))
                      LexGenJava.addCharToSkip (Character.toLowerCase (c), nKind);
                  }
                  continue CaseLoop;
                }
            }
        }

        // Since we know key is a single character ...
        if (Options.isIgnoreCase ())
        {
          if (c != Character.toUpperCase (c))
            aCodeGenerator.genCodeLine ("      case " + _getCaseChar (Character.toUpperCase (c), eOutputLanguage) + ":");

          if (c != Character.toLowerCase (c))
            aCodeGenerator.genCodeLine ("      case " + _getCaseChar (Character.toLowerCase (c), eOutputLanguage) + ":");
        }

        aCodeGenerator.genCodeLine ("      case " + _getCaseChar (c, eOutputLanguage) + ":");

        long nMatchedKind;
        final String sPrefix = (i == 0) ? "         " : "            ";

        if (aInfo.m_nFinalKindCnt != 0)
        {
          for (int j = 0; j < nMaxLongsReqd; j++)
          {
            if ((nMatchedKind = aInfo.m_aFinalKinds[j]) == 0L)
              continue;

            for (int k = 0; k < 64; k++)
            {
              if ((nMatchedKind & (1L << k)) == 0L)
                continue;

              if (bIfGenerated)
              {
                aCodeGenerator.genCode ("         else if ");
              }
              else
                if (i != 0)
                  aCodeGenerator.genCode ("         if ");

              bIfGenerated = true;

              int nKindToPrint;
              if (i != 0)
              {
                aCodeGenerator.genCodeLine ("((active" +
                                           j +
                                           " & " +
                                           eOutputLanguage.getLongHex (1L << k) +
                                           ") != " +
                                           eOutputLanguage.getLongPlain (0) +
                                           ")");
              }

              if (strLit ().getIntermediateKinds () != null &&
                strLit ().getIntermediateKinds ()[(j * 64 + k)] != null &&
                strLit ().getIntermediateKinds ()[(j * 64 + k)][i] < (j * 64 + k) &&
                strLit ().getIntermediateMatchedPos () != null &&
                strLit ().getIntermediateMatchedPos ()[(j * 64 + k)][i] == i)
              {
                JavaCCErrors.warning (" \"" +
                                      JavaCCGlobals.addEscapes (strLit ().getAllImages ()[j * 64 + k]) +
                                      "\" cannot be matched as a string literal token " +
                                      "at line " +
                                      getLineNumber (j * 64 + k) +
                                      ", column " +
                                      getColumnNumber (j * 64 + k) +
                                      ". It will be matched as " +
                                      getLabel (strLit ().getIntermediateKinds ()[(j * 64 + k)][i]) +
                                      ".");
                nKindToPrint = strLit ().getIntermediateKinds ()[(j * 64 + k)][i];
              }
              else
                if (i == 0 &&
                  LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()] >= 0 &&
                  LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()] < (j * 64 + k))
                {
                  JavaCCErrors.warning (" \"" +
                                        JavaCCGlobals.addEscapes (strLit ().getAllImages ()[j * 64 + k]) +
                                        "\" cannot be matched as a string literal token " +
                                        "at line " +
                                        getLineNumber (j * 64 + k) +
                                        ", column " +
                                        getColumnNumber (j * 64 + k) +
                                        ". It will be matched as " +
                                        getLabel (LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ()
                                                                                                      .getLexStateIndex ()]) +
                                        ".");
                  nKindToPrint = LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()];
                }
                else
                  nKindToPrint = j * 64 + k;

              if (!strLit ().getSubString ()[(j * 64 + k)])
              {
                final int nStateSetName = _getStateSetForKind (i, j * 64 + k);

                if (nStateSetName != -1)
                {
                  bCreateStartNfa = true;
                  aCodeGenerator.genCodeLine (sPrefix +
                                             "return jjStartNfaWithStates" +
                                             LexGenJava.lexer ().getLexStateSuffix () +
                                             "(" +
                                             i +
                                             ", " +
                                             nKindToPrint +
                                             ", " +
                                             nStateSetName +
                                             ");");
                }
                else
                  aCodeGenerator.genCodeLine (sPrefix + "return jjStopAtPos" + "(" + i + ", " + nKindToPrint + ");");
              }
              else
              {
                if ((LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != 0 &&
                  LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != Integer.MAX_VALUE) ||
                  i != 0)
                {
                  aCodeGenerator.genCodeLine ("         {");
                  aCodeGenerator.genCodeLine (sPrefix + "jjmatchedKind = " + nKindToPrint + ";");
                  aCodeGenerator.genCodeLine (sPrefix + "jjmatchedPos = " + i + ";");
                  aCodeGenerator.genCodeLine ("         }");
                }
                else
                  aCodeGenerator.genCodeLine (sPrefix + "jjmatchedKind = " + nKindToPrint + ";");
              }
            }
          }
        }

        if (aInfo.m_nValidKindCnt != 0)
        {
          bAtLeastOne = false;

          if (i == 0)
          {
            aCodeGenerator.genCode ("         return ");

            aCodeGenerator.genCode ("jjMoveStringLiteralDfa" + (i + 1) + LexGenJava.lexer ().getLexStateSuffix () + "(");
            int j = 0;
            for (; j < nMaxLongsReqd - 1; j++)
              if ((i + 1) <= strLit ().getMaxLenForActive ()[j])
              {
                if (bAtLeastOne)
                  aCodeGenerator.genCode (", ");
                else
                  bAtLeastOne = true;

                aCodeGenerator.genCode (eOutputLanguage.getLongHex (aInfo.m_aValidKinds[j]));
              }

            if ((i + 1) <= strLit ().getMaxLenForActive ()[j])
            {
              if (bAtLeastOne)
                aCodeGenerator.genCode (", ");

              aCodeGenerator.genCode (eOutputLanguage.getLongHex (aInfo.m_aValidKinds[j]));
            }
            aCodeGenerator.genCodeLine (");");
          }
          else
          {
            aCodeGenerator.genCode ("         return ");

            aCodeGenerator.genCode ("jjMoveStringLiteralDfa" + (i + 1) + LexGenJava.lexer ().getLexStateSuffix () + "(");

            int j = 0;
            for (; j < nMaxLongsReqd - 1; j++)
              if ((i + 1) <= strLit ().getMaxLenForActive ()[j] + 1)
              {
                if (bAtLeastOne)
                  aCodeGenerator.genCode (", ");
                else
                  bAtLeastOne = true;

                if (aInfo.m_aValidKinds[j] != 0L)
                  aCodeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongHex (aInfo.m_aValidKinds[j]));
                else
                  aCodeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongPlain (0));
              }

            if ((i + 1) <= strLit ().getMaxLenForActive ()[j] + 1)
            {
              if (bAtLeastOne)
                aCodeGenerator.genCode (", ");
              if (aInfo.m_aValidKinds[j] != 0L)
                aCodeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongHex (aInfo.m_aValidKinds[j]));
              else
                aCodeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongPlain (0));
            }

            aCodeGenerator.genCodeLine (");");
          }
        }
        else
        {
          // A very special case.
          if (i == 0 && LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
          {
            if (NfaState.nfa ().getGeneratedStates () != 0)
              aCodeGenerator.genCodeLine ("         return jjMoveNfa" +
                                         LexGenJava.lexer ().getLexStateSuffix () +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", 0);");
            else
              aCodeGenerator.genCodeLine ("         return 1;");
          }
          else
            if (i != 0) // No more str literals to look for
            {
              aCodeGenerator.genCodeLine ("         break;");
              bStartNfaNeeded = true;
            }
        }
      }

      /*
       * default means that the current character is not in any of the strings at this position.
       */
      aCodeGenerator.genCodeLine ("      default :");

      if (Options.isDebugTokenManager ())
      {
        switch (eOutputLanguage)
        {
          case JAVA:
            aCodeGenerator.genCodeLine ("      debugStream.println(\"   No string literal matches possible.\");");
            break;
          case CPP:
            aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   No string literal matches possible.\\n\");");
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }
      }

      if (NfaState.nfa ().getGeneratedStates () != 0)
      {
        if (i == 0)
        {
          /*
           * This means no string literal is possible. Just move nfa with this guy and return.
           */
          aCodeGenerator.genCodeLine ("         return jjMoveNfa" +
                                     LexGenJava.lexer ().getLexStateSuffix () +
                                     "(" +
                                     NfaState.initStateName () +
                                     ", 0);");
        }
        else
        {
          aCodeGenerator.genCodeLine ("         break;");
          bStartNfaNeeded = true;
        }
      }
      else
      {
        aCodeGenerator.genCodeLine ("         return " + (i + 1) + ";");
      }

      aCodeGenerator.genCodeLine ("   }");

      if (i != 0)
      {
        if (bStartNfaNeeded)
        {
          if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
            NfaState.nfa ().getGeneratedStates () != 0)
          {
            /*
             * Here, a string literal is successfully matched and no more string literals are
             * possible. So set the kind and state set upto and including this position for the
             * matched string.
             */

            aCodeGenerator.genCode ("   return jjStartNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(" +
                                   (i - 1) +
                                   ", ");

            int k = 0;
            for (; k < nMaxLongsReqd - 1; k++)
            {
              if (i <= strLit ().getMaxLenForActive ()[k])
                aCodeGenerator.genCode ("active" + k + ", ");
              else
                aCodeGenerator.genCode ("0L, ");
            }
            if (i <= strLit ().getMaxLenForActive ()[k])
              aCodeGenerator.genCodeLine ("active" + k + ");");
            else
              aCodeGenerator.genCodeLine ("0L);");
          }
          else
            if (NfaState.nfa ().getGeneratedStates () != 0)
              aCodeGenerator.genCodeLine ("   return jjMoveNfa" +
                                         LexGenJava.lexer ().getLexStateSuffix () +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", " +
                                         i +
                                         ");");
            else
              aCodeGenerator.genCodeLine ("   return " + (i + 1) + ";");
        }
      }

      aCodeGenerator.genCodeLine ("}");
    }

    if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
      NfaState.nfa ().getGeneratedStates () != 0 &&
      bCreateStartNfa)
      dumpStartWithStates (aCodeGenerator);
  }

  static final int getStrKind (final String sStr)
  {
    for (int i = 0; i < strLit ().getMaxStrKind (); i++)
    {
      if (LexGenJava.lexer ().getLexStates ()[i] != LexGenJava.lexer ().getLexStateIndex ())
        continue;

      final String sImage = strLit ().getAllImages ()[i];
      if (sImage != null && sImage.equals (sStr))
        return i;
    }

    return Integer.MAX_VALUE;
  }

  public static void generateNfaStartStates (final CodeGenerator aCodeGenerator, final NfaState aInitialState)
  {
    final boolean [] aSeen = new boolean [NfaState.nfa ().getGeneratedStates ()];
    final Map <String, String> aStateSets = new HashMap <> ();
    String sStateSetString = "";
    int i, j, kind, jjmatchedPos = 0;
    final int nMaxKindsReqd = strLit ().getMaxStrKind () / 64 + 1;
    long [] aActives;
    List <NfaState> aNewStates = new ArrayList <> ();
    List <NfaState> aOldStates = null;
    List <NfaState> aJjtmpStates;

    strLit ().setStatesForPos (GenericReflection.uncheckedCast (new Map [strLit ().getMaxLen ()]));
    strLit ().setIntermediateKinds (new int [strLit ().getMaxStrKind () + 1] []);
    strLit ().setIntermediateMatchedPos (new int [strLit ().getMaxStrKind () + 1] []);

    for (i = 0; i < strLit ().getMaxStrKind (); i++)
    {
      if (LexGenJava.lexer ().getLexStates ()[i] != LexGenJava.lexer ().getLexStateIndex ())
        continue;

      final String sImage = strLit ().getAllImages ()[i];

      if (sImage == null || sImage.length () < 1)
        continue;

      try
      {
        aOldStates = new ArrayList <> (aInitialState.m_aEpsilonMoves);
        if (aOldStates.size () == 0)
        {
          dumpNfaStartStatesCode (strLit ().getStatesForPos (), aCodeGenerator);
          return;
        }
      }
      catch (final Exception e)
      {
        JavaCCErrors.semantic_error ("Error cloning state vector");
      }

      strLit ().getIntermediateKinds ()[i] = new int [sImage.length ()];
      strLit ().getIntermediateMatchedPos ()[i] = new int [sImage.length ()];
      jjmatchedPos = 0;
      kind = Integer.MAX_VALUE;

      for (j = 0; j < sImage.length (); j++)
      {
        if (aOldStates == null || aOldStates.size () <= 0)
        {
          // Here, j > 0
          kind = strLit ().getIntermediateKinds ()[i][j] = strLit ().getIntermediateKinds ()[i][j - 1];
          jjmatchedPos = strLit ().getIntermediateMatchedPos ()[i][j] = strLit ().getIntermediateMatchedPos ()[i][j -
                                                                                                                  1];
        }
        else
        {
          kind = NfaState.moveFromSet (sImage.charAt (j), aOldStates, aNewStates);
          aOldStates.clear ();

          if (j == 0 &&
            kind != Integer.MAX_VALUE &&
            LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()] != -1 &&
            kind > LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()])
            kind = LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()];

          if (getStrKind (sImage.substring (0, j + 1)) < kind)
          {
            strLit ().getIntermediateKinds ()[i][j] = kind = Integer.MAX_VALUE;
            jjmatchedPos = 0;
          }
          else
            if (kind != Integer.MAX_VALUE)
            {
              strLit ().getIntermediateKinds ()[i][j] = kind;
              jjmatchedPos = strLit ().getIntermediateMatchedPos ()[i][j] = j;
            }
            else
              if (j == 0)
                kind = strLit ().getIntermediateKinds ()[i][j] = Integer.MAX_VALUE;
              else
              {
                kind = strLit ().getIntermediateKinds ()[i][j] = strLit ().getIntermediateKinds ()[i][j - 1];
                jjmatchedPos = strLit ().getIntermediateMatchedPos ()[i][j] = strLit ().getIntermediateMatchedPos ()[i][j -
                                                                                                                        1];
              }

          sStateSetString = NfaState.getStateSetString (aNewStates);
        }

        if (kind == Integer.MAX_VALUE && (aNewStates == null || aNewStates.size () == 0))
          continue;

        int p;
        if (aStateSets.get (sStateSetString) == null)
        {
          aStateSets.put (sStateSetString, sStateSetString);
          for (p = 0; p < aNewStates.size (); p++)
          {
            if (aSeen[aNewStates.get (p).m_nStateName])
              aNewStates.get (p).m_nInNextOf++;
            else
              aSeen[aNewStates.get (p).m_nStateName] = true;
          }
        }
        else
        {
          for (p = 0; p < aNewStates.size (); p++)
            aSeen[aNewStates.get (p).m_nStateName] = true;
        }

        aJjtmpStates = aOldStates;
        aOldStates = aNewStates;
        (aNewStates = aJjtmpStates).clear ();

        if (strLit ().getStatesForPos ()[j] == null)
          strLit ().getStatesForPos ()[j] = new HashMap <> ();

        aActives = strLit ().getStatesForPos ()[j].computeIfAbsent (kind + ", " + jjmatchedPos + ", " + sStateSetString,
                                                                   k -> new long [nMaxKindsReqd]);

        aActives[i / 64] |= 1L << (i % 64);
        // String name = NfaState.StoreStateSet(stateSetString);
      }
    }

    dumpNfaStartStatesCode (strLit ().getStatesForPos (), aCodeGenerator);
  }

  static void dumpNfaStartStatesCode (final Map <String, long []> [] aStatesForPos, final CodeGenerator aCodeGenerator)
  {
    if (strLit ().getMaxStrKind () == 0)
    { // No need to generate this function
      return;
    }

    final EOutputLanguage eOutputLanguage = aCodeGenerator.getOutputLanguage ();
    int i;
    final int nMaxKindsReqd = strLit ().getMaxStrKind () / 64 + 1;
    boolean bCondGenerated = false;
    int nInd = 0;

    final StringBuilder aParams = new StringBuilder ();
    for (i = 0; i < nMaxKindsReqd - 1; i++)
      aParams.append (eOutputLanguage.getTypeLong () + " active" + i + ", ");
    aParams.append (eOutputLanguage.getTypeLong () + " active" + i + ")");

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCode ("private final int jjStopStringLiteralDfa" +
                               LexGenJava.lexer ().getLexStateSuffix () +
                               "(int pos, " +
                               aParams);
        break;
      case CPP:
        aCodeGenerator.generateMethodDefHeader (" int",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStopStringLiteralDfa" +
                                                                                            LexGenJava.lexer ()
                                                                                                      .getLexStateSuffix () +
                                                                                            "(int pos, " +
                                                                                            aParams);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    aCodeGenerator.genCodeLine ("{");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          aCodeGenerator.genCodeLine ("      debugStream.println(\"   No more string literal token matches are possible.\");");
          break;
        case CPP:
          aCodeGenerator.genCodeLine ("      fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    aCodeGenerator.genCodeLine ("   switch (pos)");
    aCodeGenerator.genCodeLine ("   {");

    for (i = 0; i < strLit ().getMaxLen () - 1; i++)
    {
      if (aStatesForPos[i] == null)
        continue;

      aCodeGenerator.genCodeLine ("      case " + i + ":");

      for (final Map.Entry <String, long []> aEntry : aStatesForPos[i].entrySet ())
      {
        String sStateSetString = aEntry.getKey ();
        final long [] aActives = aEntry.getValue ();

        for (int j = 0; j < nMaxKindsReqd; j++)
        {
          if (aActives[j] == 0L)
            continue;

          if (bCondGenerated)
            aCodeGenerator.genCode (" || ");
          else
            aCodeGenerator.genCode ("         if (");

          bCondGenerated = true;

          aCodeGenerator.genCode ("(active" +
                                 j +
                                 " & " +
                                 eOutputLanguage.getLongHex (aActives[j]) +
                                 ") != " +
                                 eOutputLanguage.getLongPlain (0));
        }

        if (bCondGenerated)
        {
          aCodeGenerator.genCodeLine (")");

          String sKindStr = sStateSetString.substring (0, nInd = sStateSetString.indexOf (", "));
          String sAfterKind = sStateSetString.substring (nInd + 2);
          final int nJjmatchedPos = Integer.parseInt (sAfterKind.substring (0, sAfterKind.indexOf (", ")));

          if (!sKindStr.equals (String.valueOf (Integer.MAX_VALUE)))
            aCodeGenerator.genCodeLine ("         {");

          if (!sKindStr.equals (String.valueOf (Integer.MAX_VALUE)))
          {
            if (i == 0)
            {
              aCodeGenerator.genCodeLine ("            jjmatchedKind = " + sKindStr + ";");

              if ((LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != 0 &&
                LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != Integer.MAX_VALUE))
                aCodeGenerator.genCodeLine ("            jjmatchedPos = 0;");
            }
            else
              if (i == nJjmatchedPos)
              {
                if (strLit ().getSubStringAtPos ()[i])
                {
                  aCodeGenerator.genCodeLine ("            if (jjmatchedPos != " + i + ")");
                  aCodeGenerator.genCodeLine ("            {");
                  aCodeGenerator.genCodeLine ("               jjmatchedKind = " + sKindStr + ";");
                  aCodeGenerator.genCodeLine ("               jjmatchedPos = " + i + ";");
                  aCodeGenerator.genCodeLine ("            }");
                }
                else
                {
                  aCodeGenerator.genCodeLine ("            jjmatchedKind = " + sKindStr + ";");
                  aCodeGenerator.genCodeLine ("            jjmatchedPos = " + i + ";");
                }
              }
              else
              {
                if (nJjmatchedPos > 0)
                  aCodeGenerator.genCodeLine ("            if (jjmatchedPos < " + nJjmatchedPos + ")");
                else
                  aCodeGenerator.genCodeLine ("            if (jjmatchedPos == 0)");
                aCodeGenerator.genCodeLine ("            {");
                aCodeGenerator.genCodeLine ("               jjmatchedKind = " + sKindStr + ";");
                aCodeGenerator.genCodeLine ("               jjmatchedPos = " + nJjmatchedPos + ";");
                aCodeGenerator.genCodeLine ("            }");
              }
          }

          sKindStr = sStateSetString.substring (0, nInd = sStateSetString.indexOf (", "));
          sAfterKind = sStateSetString.substring (nInd + 2);
          sStateSetString = sAfterKind.substring (sAfterKind.indexOf (", ") + 2);

          if (sStateSetString.equals ("null;"))
            aCodeGenerator.genCodeLine ("            return -1;");
          else
            aCodeGenerator.genCodeLine ("            return " + NfaState.addStartStateSet (sStateSetString) + ";");

          if (!sKindStr.equals (String.valueOf (Integer.MAX_VALUE)))
            aCodeGenerator.genCodeLine ("         }");
          bCondGenerated = false;
        }
      }

      aCodeGenerator.genCodeLine ("         return -1;");
    }

    aCodeGenerator.genCodeLine ("      default :");
    aCodeGenerator.genCodeLine ("         return -1;");
    aCodeGenerator.genCodeLine ("   }");
    aCodeGenerator.genCodeLine ("}");

    aParams.setLength (0);
    aParams.append ("(int pos, ");
    for (i = 0; i < nMaxKindsReqd - 1; i++)
      aParams.append (eOutputLanguage.getTypeLong () + " active" + i + ", ");
    aParams.append (eOutputLanguage.getTypeLong () + " active" + i + ")");

    switch (eOutputLanguage)
    {
      case JAVA:
        aCodeGenerator.genCode ("private final int jjStartNfa" + LexGenJava.lexer ().getLexStateSuffix () + aParams);
        break;
      case CPP:
        aCodeGenerator.generateMethodDefHeader ("int ",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStartNfa" + LexGenJava.lexer ().getLexStateSuffix () + aParams);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    aCodeGenerator.genCodeLine ("{");

    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
    {
      if (NfaState.nfa ().getGeneratedStates () != 0)
        aCodeGenerator.genCodeLine ("   return jjMoveNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(" +
                                   NfaState.initStateName () +
                                   ", pos + 1);");
      else
        aCodeGenerator.genCodeLine ("   return pos + 1;");

      aCodeGenerator.genCodeLine ("}");
      return;
    }

    aCodeGenerator.genCode ("   return jjMoveNfa" +
                           LexGenJava.lexer ().getLexStateSuffix () +
                           "(" +
                           "jjStopStringLiteralDfa" +
                           LexGenJava.lexer ().getLexStateSuffix () +
                           "(pos, ");
    for (i = 0; i < nMaxKindsReqd - 1; i++)
      aCodeGenerator.genCode ("active" + i + ", ");
    aCodeGenerator.genCode ("active" + i + ")");
    aCodeGenerator.genCodeLine (", pos + 1);");
    aCodeGenerator.genCodeLine ("}");
  }

  @Override
  public StringBuilder dump (final int nIndent, final Set <? super Expansion> aAlreadyDumped)
  {
    final StringBuilder aSb = super.dump (nIndent, aAlreadyDumped).append (' ').append (m_sImage);
    return aSb;
  }

  @Override
  public String toString ()
  {
    return super.toString () + " - " + m_sImage;
  }

  /*
   * static void GenerateData(TokenizerData tokenizerData) { Map tab; String key; KindInfo info; for
   * (int i = 0; i < maxLen; i++) { tab = (Map)charPosKind.get(i); String[] keys = ReArrange(tab);
   * if (Options.getIgnoreCase()) { for (String s : keys) { char c = s.charAt(0);
   * tab.put(Character.toLowerCase(c), tab.get(c)); tab.put(Character.toUpperCase(c), tab.get(c)); }
   * } for (int q = 0; q < keys.length; q++) { key = keys[q]; info = (KindInfo)tab.get(key); char c
   * = key.charAt(0); for (int kind : info.finalKindSet) { tokenizerData.addDfaFinalKindAndState( i,
   * c, kind, GetStateSetForKind(i, kind)); } for (int kind : info.validKindSet) {
   * tokenizerData.addDfaValidKind(i, c, kind); } } } for (int i = 0; i < maxLen; i++) { Enumeration
   * e = statesForPos[i].keys(); while (e.hasMoreElements()) { String stateSetString =
   * (String)e.nextElement(); long[] actives = (long[])statesForPos[i].get(stateSetString); int ind
   * = stateSetString.indexOf(", "); String kindStr = stateSetString.substring(0, ind); String
   * afterKind = stateSetString.substring(ind + 2); stateSetString =
   * afterKind.substring(afterKind.indexOf(", ") + 2); BitSet bits = BitSet.valueOf(actives); for
   * (int j = 0; j < bits.length(); j++) { if (bits.get(j)) tokenizerData.addFinalDfaKind(j); } //
   * Pos codeGenerator.genCode( ", " + afterKind.substring(0, afterKind.indexOf(", "))); // Kind
   * codeGenerator.genCode(", " + kindStr); // State if (stateSetString.equals("null;")) {
   * codeGenerator.genCodeLine(", -1"); } else { codeGenerator.genCodeLine( ", " +
   * NfaState.AddStartStateSet(stateSetString)); } } codeGenerator.genCode("}"); }
   * codeGenerator.genCodeLine("};"); }
   */

  public static void updateStringLiteralData (final int nLexStateIndex)
  {
    for (int nKind = 0; nKind < strLit ().getAllImages ().length; nKind++)
    {
      if (StringHelper.isEmpty (strLit ().getAllImages ()[nKind]) ||
        LexGenJava.lexer ().getLexStates ()[nKind] != nLexStateIndex)
      {
        continue;
      }
      String s = strLit ().getAllImages ()[nKind];
      int nActualKind;
      if (strLit ().getIntermediateKinds () != null &&
        strLit ().getIntermediateKinds ()[nKind][s.length () - 1] != Integer.MAX_VALUE &&
        strLit ().getIntermediateKinds ()[nKind][s.length () - 1] < nKind)
      {
        JavaCCErrors.warning ("Token: " +
                              s +
                              " will not be matched as " +
                              "specified. It will be matched as token " +
                              "of kind: " +
                              strLit ().getIntermediateKinds ()[nKind][s.length () - 1] +
                              " instead.");
        nActualKind = strLit ().getIntermediateKinds ()[nKind][s.length () - 1];
      }
      else
      {
        nActualKind = nKind;
      }
      NfaState.tokenizerBuild ()
              .kindToLexicalState ()
              .put (Integer.valueOf (nActualKind), Integer.valueOf (nLexStateIndex));
      if (Options.isIgnoreCase ())
      {
        s = s.toLowerCase (Locale.US);
      }
      final char c = s.charAt (0);
      final int nKey = LexGenJava.lexer ().getLexStateIndex () << 16 | c;
      List <String> l = NfaState.tokenizerBuild ().literalsByLength ().get (Integer.valueOf (nKey));
      List <Integer> aKinds = NfaState.tokenizerBuild ().literalKinds ().get (Integer.valueOf (nKey));
      int j = 0;
      if (l == null)
      {
        NfaState.tokenizerBuild ().literalsByLength ().put (Integer.valueOf (nKey), l = new ArrayList <> ());
        assert (aKinds == null);
        aKinds = new ArrayList <> ();
        NfaState.tokenizerBuild ().literalKinds ().put (Integer.valueOf (nKey), aKinds = new ArrayList <> ());
      }
      while (j < l.size () && l.get (j).length () > s.length ())
        j++;
      l.add (j, s);
      aKinds.add (j, Integer.valueOf (nActualKind));
      final int nStateIndex = _getStateSetForKind (s.length () - 1, nKind);
      NfaState.tokenizerBuild ()
              .nfaStateMap ()
              .put (Integer.valueOf (nActualKind), nStateIndex == -1 ? null : NfaState.getNfaState (nStateIndex));
    }
  }

  public static void BuildTokenizerData (final TokenizerData aTokenizerData)
  {
    final Map <Integer, Integer> aNfaStateIndices = new HashMap <> ();
    for (final int kind : NfaState.tokenizerBuild ().nfaStateMap ().keySet ())
    {
      final NfaState aState = NfaState.tokenizerBuild ().nfaStateMap ().get (Integer.valueOf (kind));
      aNfaStateIndices.put (Integer.valueOf (kind), Integer.valueOf (aState == null ? -1 : aState.m_nStateName));
    }
    aTokenizerData.setLiteralSequence (NfaState.tokenizerBuild ().literalsByLength ());
    aTokenizerData.setLiteralKinds (NfaState.tokenizerBuild ().literalKinds ());
    aTokenizerData.setKindToNfaStartState (aNfaStateIndices);
  }
}
