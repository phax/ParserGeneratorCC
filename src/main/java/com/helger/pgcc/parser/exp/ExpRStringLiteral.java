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
    private final long [] m_validKinds;
    private final long [] m_finalKinds;
    private int m_validKindCnt = 0;
    private int m_finalKindCnt = 0;
    private final Set <Integer> m_finalKindSet = new HashSet <> ();
    private final Set <Integer> m_validKindSet = new HashSet <> ();

    KindInfo (final int maxKind)
    {
      m_validKinds = new long [maxKind / 64 + 1];
      m_finalKinds = new long [maxKind / 64 + 1];
    }

    public void insertValidKind (final int kind)
    {
      m_validKinds[kind / 64] |= (1L << (kind % 64));
      m_validKindCnt++;
      m_validKindSet.add (Integer.valueOf (kind));
    }

    public void insertFinalKind (final int kind)
    {
      m_finalKinds[kind / 64] |= (1L << (kind % 64));
      m_finalKindCnt++;
      m_finalKindSet.add (Integer.valueOf (kind));
    }
  }

  /**
   * The string image of the literal.
   */
  public String m_image;

  public ExpRStringLiteral (final Token t, final String image)
  {
    setLine (t.beginLine);
    setColumn (t.beginColumn);
    m_image = image;
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

  public static void dumpStrLiteralImages (final CodeGenerator codeGenerator)
  {
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        dumpStrLiteralImagesForJava (codeGenerator);
        return;
      case CPP:
        // For C++
        String image;
        int i;
        strLit ().setCharCnt (0); // Set to zero in reInit() but just to be sure

        codeGenerator.genCodeNewLine ();
        codeGenerator.genCodeLine ("/** Token literal values. */");
        int literalCount = 0;
        codeGenerator.switchToStaticsFile ();

        if (strLit ().getAllImages () == null || strLit ().getAllImages ().length == 0)
        {
          codeGenerator.genCodeLine ("static const JJString jjstrLiteralImages[] = {};");
          return;
        }

        strLit ().getAllImages ()[0] = "";
        for (i = 0; i < strLit ().getAllImages ().length; i++)
        {
          if ((image = strLit ().getAllImages ()[i]) == null ||
            ((LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L &&
              (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) == 0L &&
              (LexGenJava.lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L) ||
            (LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) != 0L ||
            (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) != 0L ||
            LexGenJava.lexer ().getCanReachOnMore ()[LexGenJava.lexer ().getLexStates ()[i]] ||
            ((Options.isIgnoreCase () || LexGenJava.lexer ().getIgnoreCase ()[i]) &&
              (!image.equals (image.toLowerCase (Locale.US)) || !image.equals (image.toUpperCase (Locale.US)))))
          {
            strLit ().getAllImages ()[i] = null;
            strLit ().setCharCnt (strLit ().getCharCnt () + 6);
            if (strLit ().getCharCnt () > 80)
            {
              codeGenerator.genCodeNewLine ();
              strLit ().setCharCnt (0);
            }

            codeGenerator.genCodeLine ("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
            continue;
          }

          String toPrint = "static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {";
          for (int j = 0; j < image.length (); j++)
          {
            toPrint += "0x" + Integer.toHexString (image.charAt (j)) + ", ";
          }

          // Null char
          toPrint += "0 };";

          strLit ().setCharCnt (strLit ().getCharCnt () + toPrint.length ());
          if (strLit ().getCharCnt () >= 80)
          {
            codeGenerator.genCodeNewLine ();
            strLit ().setCharCnt (0);
          }

          codeGenerator.genCodeLine (toPrint);
        }

        while (++i < LexGenJava.lexer ().getMaxOrdinal ())
        {
          strLit ().setCharCnt (strLit ().getCharCnt () + 6);
          if (strLit ().getCharCnt () > 80)
          {
            codeGenerator.genCodeNewLine ();
            strLit ().setCharCnt (0);
          }

          codeGenerator.genCodeLine ("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
          continue;
        }

        // Generate the array here.
        codeGenerator.genCodeLine ("static const JJString " + "jjstrLiteralImages[] = {");
        for (int j = 0; j < literalCount; j++)
        {
          codeGenerator.genCodeLine ("jjstrLiteralChars_" + j + ", ");
        }
        codeGenerator.genCodeLine ("};");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  public static void dumpStrLiteralImagesForJava (final CodeGenerator codeGenerator)
  {
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();
    String image;
    int i;
    strLit ().setCharCnt (0); // Set to zero in reInit() but just to be sure

    codeGenerator.genCodeNewLine ();
    codeGenerator.genCodeLine ("/** Token literal values. */");
    codeGenerator.genCodeLine ("public static final String[] jjstrLiteralImages = {");

    if (strLit ().getAllImages () == null || strLit ().getAllImages ().length == 0)
    {
      codeGenerator.genCodeLine ("};");
      return;
    }

    strLit ().getAllImages ()[0] = "";
    for (i = 0; i < strLit ().getAllImages ().length; i++)
    {
      if ((image = strLit ().getAllImages ()[i]) == null ||
        ((LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L &&
          (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) == 0L &&
          (LexGenJava.lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L) ||
        (LexGenJava.lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) != 0L ||
        (LexGenJava.lexer ().getToMore ()[i / 64] & (1L << (i % 64))) != 0L ||
        LexGenJava.lexer ().getCanReachOnMore ()[LexGenJava.lexer ().getLexStates ()[i]] ||
        ((Options.isIgnoreCase () || LexGenJava.lexer ().getIgnoreCase ()[i]) &&
          (!image.equals (image.toLowerCase (Locale.US)) || !image.equals (image.toUpperCase (Locale.US)))))
      {
        strLit ().getAllImages ()[i] = null;
        strLit ().setCharCnt (strLit ().getCharCnt () + 6);
        if (strLit ().getCharCnt () > 80)
        {
          codeGenerator.genCodeNewLine ();
          strLit ().setCharCnt (0);
        }

        codeGenerator.genCode ("null, ");
        continue;
      }

      final StringBuilder toPrint = new StringBuilder ("\"");
      for (int j = 0; j < image.length (); j++)
      {
        final char c = image.charAt (j);
        switch (eOutputLanguage)
        {
          case JAVA:
            if (c <= 0xff)
              toPrint.append ('\\').append (Integer.toOctalString (c));
            else
            {
              String hexVal = Integer.toHexString (c);
              if (hexVal.length () == 3)
                hexVal = "0" + hexVal;
              toPrint.append ("\\u").append (hexVal);
            }
            break;
          case CPP:
            String hexVal = Integer.toHexString (c);
            if (hexVal.length () == 3)
              hexVal = "0" + hexVal;
            toPrint.append ("\\u").append (hexVal);
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }
      }

      toPrint.append ("\", ");

      strLit ().setCharCnt (strLit ().getCharCnt () + toPrint.length ());
      if (strLit ().getCharCnt () > 80)
      {
        // Break after 80 chars
        codeGenerator.genCodeNewLine ();
        strLit ().setCharCnt (0);
      }

      codeGenerator.genCode (toPrint.toString ());
    }

    while (++i < LexGenJava.lexer ().getMaxOrdinal ())
    {
      strLit ().setCharCnt (strLit ().getCharCnt () + 6);
      if (strLit ().getCharCnt () > 80)
      {
        // Break after 80 chars
        codeGenerator.genCodeNewLine ();
        strLit ().setCharCnt (0);
      }

      codeGenerator.genCode ("null, ");
    }

    codeGenerator.genCodeLine ("};");
  }

  /**
   * Used for top level string literals.
   */
  public void generateDfa ()
  {
    String s;
    Map <String, KindInfo> temp;

    if (strLit ().getMaxStrKind () <= getOrdinal ())
      strLit ().setMaxStrKind (getOrdinal () + 1);

    final int len = m_image.length ();
    if (len > strLit ().getMaxLen ())
      strLit ().setMaxLen (len);

    for (int i = 0; i < len; i++)
    {
      final char c = m_image.charAt (i);
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
        temp = new HashMap <> ();
        strLit ().getCharPosKind ().add (temp);
      }
      else
        temp = strLit ().getCharPosKind ().get (i);

      KindInfo info = temp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.lexer ().getMaxOrdinal ()));

      if (i + 1 == len)
        info.insertFinalKind (getOrdinal ());
      else
        info.insertValidKind (getOrdinal ());

      if (!Options.isIgnoreCase () &&
        LexGenJava.lexer ().getIgnoreCase ()[getOrdinal ()] &&
        c != Character.toLowerCase (c))
      {
        s = Character.toString (Character.toLowerCase (c));

        if (i >= strLit ().getCharPosKind ().size ()) // Kludge, but OK
        {
          temp = new HashMap <> ();
          strLit ().getCharPosKind ().add (temp);
        }
        else
          temp = strLit ().getCharPosKind ().get (i);

        info = temp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.lexer ().getMaxOrdinal ()));

        if (i + 1 == len)
          info.insertFinalKind (getOrdinal ());
        else
          info.insertValidKind (getOrdinal ());
      }

      if (!Options.isIgnoreCase () &&
        LexGenJava.lexer ().getIgnoreCase ()[getOrdinal ()] &&
        c != Character.toUpperCase (c))
      {
        s = Character.toString (Character.toUpperCase (c));

        // Kludge, but OK
        if (i >= strLit ().getCharPosKind ().size ())
        {
          temp = new HashMap <> ();
          strLit ().getCharPosKind ().add (temp);
        }
        else
          temp = strLit ().getCharPosKind ().get (i);

        info = temp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.lexer ().getMaxOrdinal ()));

        if (i + 1 == len)
          info.insertFinalKind (getOrdinal ());
        else
          info.insertValidKind (getOrdinal ());
      }
    }

    strLit ().getMaxLenForActive ()[getOrdinal () / 64] = Math.max (strLit ().getMaxLenForActive ()[getOrdinal () / 64],
                                                                    len - 1);
    strLit ().getAllImages ()[getOrdinal ()] = m_image;
  }

  @Override
  public Nfa generateNfa (final boolean ignoreCase)
  {
    if (m_image.length () == 1)
    {
      final ExpRCharacterList temp = new ExpRCharacterList (m_image.charAt (0));
      return temp.generateNfa (ignoreCase);
    }

    NfaState startState = new NfaState ();
    final NfaState theStartState = startState;
    NfaState finalState = null;

    if (m_image.length () == 0)
      return new Nfa (theStartState, theStartState);

    int i;

    for (i = 0; i < m_image.length (); i++)
    {
      finalState = new NfaState ();
      startState.m_charMoves = new char [1];
      startState.addChar (m_image.charAt (i));

      if (Options.isIgnoreCase () || ignoreCase)
      {
        startState.addChar (Character.toLowerCase (m_image.charAt (i)));
        startState.addChar (Character.toUpperCase (m_image.charAt (i)));
      }

      startState.m_next = finalState;
      startState = finalState;
    }

    return new Nfa (theStartState, finalState);
  }

  static void dumpNullStrLiterals (final CodeGenerator codeGenerator)
  {
    codeGenerator.genCodeLine ("{");

    if (NfaState.nfa ().getGeneratedStates () != 0)
      codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                 LexGenJava.lexer ().getLexStateSuffix () +
                                 "(" +
                                 NfaState.initStateName () +
                                 ", 0);");
    else
      codeGenerator.genCodeLine ("   return 1;");

    codeGenerator.genCodeLine ("}");
  }

  private static int _getStateSetForKind (final int pos, final int kind)
  {
    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] ||
      NfaState.nfa ().getGeneratedStates () == 0)
      return -1;

    final Map <String, long []> allStateSets = strLit ().getStatesForPos ()[pos];

    if (allStateSets == null)
      return -1;

    for (final Map.Entry <String, long []> aEntry : allStateSets.entrySet ())
    {
      String s = aEntry.getKey ();
      final long [] actives = aEntry.getValue ();

      s = s.substring (s.indexOf (", ") + 2);
      s = s.substring (s.indexOf (", ") + 2);

      if (s.equals ("null;"))
        continue;

      if (actives != null && (actives[kind / 64] & (1L << (kind % 64))) != 0L)
      {
        return NfaState.addStartStateSet (s);
      }
    }

    return -1;
  }

  static String getLabel (final int kind)
  {
    final AbstractExpRegularExpression re = LexGenJava.lexer ().getRexprs ()[kind];

    if (re instanceof ExpRStringLiteral)
      return " \"" + JavaCCGlobals.addEscapes (((ExpRStringLiteral) re).m_image) + "\"";
    if (re.hasLabel ())
      return " <" + re.getLabel () + ">";
    return " <token of kind " + kind + ">";
  }

  static int getLine (final int kind)
  {
    return LexGenJava.lexer ().getRexprs ()[kind].getLine ();
  }

  static int getColumn (final int kind)
  {
    return LexGenJava.lexer ().getRexprs ()[kind].getColumn ();
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

      final String image = strLit ().getAllImages ()[i];
      if (image == null || LexGenJava.lexer ().getLexStates ()[i] != LexGenJava.lexer ().getLexStateIndex ())
        continue;

      if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
      {
        // We will not optimize for mixed case
        strLit ().getSubString ()[i] = true;
        strLit ().getSubStringAtPos ()[image.length () - 1] = true;
        continue;
      }

      for (int j = 0; j < strLit ().getMaxStrKind (); j++)
      {
        if (j != i &&
          LexGenJava.lexer ().getLexStates ()[j] == LexGenJava.lexer ().getLexStateIndex () &&
          (strLit ().getAllImages ()[j]) != null)
        {
          if (strLit ().getAllImages ()[j].indexOf (image) == 0)
          {
            strLit ().getSubString ()[i] = true;
            strLit ().getSubStringAtPos ()[image.length () - 1] = true;
            break;
          }
          else
            if (Options.isIgnoreCase () && _startsWithIgnoreCase (strLit ().getAllImages ()[j], image))
            {
              strLit ().getSubString ()[i] = true;
              strLit ().getSubStringAtPos ()[image.length () - 1] = true;
              break;
            }
        }
      }
    }
  }

  static void dumpStartWithStates (final CodeGenerator codeGenerator)
  {
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        codeGenerator.genCodeLine ("private int jjStartNfaWithStates" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(int pos, int kind, int state)");
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader ("int",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStartNfaWithStates" +
                                                                                            LexGenJava.lexer ()
                                                                                                      .getLexStateSuffix () +
                                                                                            "(int pos, int kind, int state)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   jjmatchedKind = kind;");
    codeGenerator.genCodeLine ("   jjmatchedPos = pos;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          codeGenerator.genCodeLine ("   debugStream.println(\"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   debugStream.println(\"   Currently matched the first \" " +
                                     "+ (jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        codeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
        codeGenerator.genCodeLine ("   catch(java.io.IOException e) { return pos + 1; }");
        break;
      case CPP:
        codeGenerator.genCodeLine ("   if (input_stream->endOfInput()) { return pos + 1; }");
        codeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          codeGenerator.genCodeLine ("   debugStream.println(" +
                                     (LexGenJava.lexer ().getMaxLexStates () > 1
                                                                                 ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                 : "") +
                                     "\"Current character : \" + " +
                                     Options.getTokenMgrErrorClass () +
                                     ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, curChar, " +
                                     "input_stream->getEndLine(), input_stream->getEndColumn());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    codeGenerator.genCodeLine ("   return jjMoveNfa" + LexGenJava.lexer ().getLexStateSuffix () + "(state, pos + 1);");
    codeGenerator.genCodeLine ("}");
  }

  static void dumpBoilerPlate (final CodeGenerator codeGenerator)
  {
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        codeGenerator.genCodeLine ("private int jjStopAtPos(int pos, int kind)");
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader (" int ",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStopAtPos(int pos, int kind)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   jjmatchedKind = kind;");
    codeGenerator.genCodeLine ("   jjmatchedPos = pos;");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          codeGenerator.genCodeLine ("   debugStream.println(\"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   debugStream.println(\"   Currently matched the first \" + (jjmatchedPos + 1) + " +
                                     "\" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    codeGenerator.genCodeLine ("   return pos + 1;");
    codeGenerator.genCodeLine ("}");
  }

  private static String [] _reArrange (final Map <String, KindInfo> tab)
  {
    final String [] ret = new String [tab.size ()];
    int cnt = 0;

    for (final String s : tab.keySet ())
    {
      final char c = s.charAt (0);

      int i = 0;
      while (i < cnt && ret[i].charAt (0) < c)
        i++;

      if (i < cnt)
        for (int j = cnt - 1; j >= i; j--)
          ret[j + 1] = ret[j];

      ret[i] = s;
      cnt++;
    }

    return ret;
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

  public static void dumpDfaCode (final CodeGenerator codeGenerator)
  {
    Map <String, KindInfo> tab;
    String key;
    KindInfo info;
    final int maxLongsReqd = strLit ().getMaxStrKind () / 64 + 1;
    boolean ifGenerated;
    LexGenJava.lexer ().getMaxLongsReqd ()[LexGenJava.lexer ().getLexStateIndex ()] = maxLongsReqd;
    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();

    if (strLit ().getMaxLen () == 0)
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          codeGenerator.genCodeLine ("private int jjMoveStringLiteralDfa0" +
                                     LexGenJava.lexer ().getLexStateSuffix () +
                                     "()");
          break;
        case CPP:
          codeGenerator.generateMethodDefHeader (" int ",
                                                 LexGenJava.lexer ().getTokenMgrClassName (),
                                                 "jjMoveStringLiteralDfa0" +
                                                                                              LexGenJava.lexer ()
                                                                                                        .getLexStateSuffix () +
                                                                                              "()");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      dumpNullStrLiterals (codeGenerator);
      return;
    }

    if (!strLit ().isBoilerPlateDumped ())
    {
      dumpBoilerPlate (codeGenerator);
      strLit ().setBoilerPlateDumped (true);
    }

    boolean createStartNfa = false;
    for (int i = 0; i < strLit ().getMaxLen (); i++)
    {
      boolean atLeastOne = false;
      boolean startNfaNeeded = false;
      tab = strLit ().getCharPosKind ().get (i);
      final String [] keys = _reArrange (tab);

      final StringBuilder params = new StringBuilder ();
      params.append ("(");
      if (i != 0)
      {
        if (i == 1)
        {
          int j = 0;
          for (; j < maxLongsReqd - 1; j++)
            if (i <= strLit ().getMaxLenForActive ()[j])
            {
              if (atLeastOne)
                params.append (", ");
              else
                atLeastOne = true;
              params.append (eOutputLanguage.getTypeLong () + " active" + j);
            }

          if (i <= strLit ().getMaxLenForActive ()[j])
          {
            if (atLeastOne)
              params.append (", ");
            params.append (eOutputLanguage.getTypeLong () + " active" + j);
          }
        }
        else
        {
          int j = 0;
          for (; j < maxLongsReqd - 1; j++)
            if (i <= strLit ().getMaxLenForActive ()[j] + 1)
            {
              if (atLeastOne)
                params.append (", ");
              else
                atLeastOne = true;
              params.append (eOutputLanguage.getTypeLong () +
                             " old" +
                             j +
                             ", " +
                             eOutputLanguage.getTypeLong () +
                             " active" +
                             j);
            }

          if (i <= strLit ().getMaxLenForActive ()[j] + 1)
          {
            if (atLeastOne)
              params.append (", ");
            params.append (eOutputLanguage.getTypeLong () +
                           " old" +
                           j +
                           ", " +
                           eOutputLanguage.getTypeLong () +
                           " active" +
                           j);
          }
        }
      }
      params.append (")");

      switch (eOutputLanguage)
      {
        case JAVA:
          codeGenerator.genCode ("private int jjMoveStringLiteralDfa" +
                                 i +
                                 LexGenJava.lexer ().getLexStateSuffix () +
                                 params);
          break;
        case CPP:
          codeGenerator.generateMethodDefHeader (" int ",
                                                 LexGenJava.lexer ().getTokenMgrClassName (),
                                                 "jjMoveStringLiteralDfa" +
                                                                                              i +
                                                                                              LexGenJava.lexer ()
                                                                                                        .getLexStateSuffix () +
                                                                                              params);
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }

      codeGenerator.genCodeLine ("{");

      if (i != 0)
      {
        if (i > 1)
        {
          atLeastOne = false;
          codeGenerator.genCode ("   if ((");

          int j = 0;
          for (; j < maxLongsReqd - 1; j++)
            if (i <= strLit ().getMaxLenForActive ()[j] + 1)
            {
              if (atLeastOne)
                codeGenerator.genCode (" | ");
              else
                atLeastOne = true;
              codeGenerator.genCode ("(active" + j + " &= old" + j + ")");
            }

          if (i <= strLit ().getMaxLenForActive ()[j] + 1)
          {
            if (atLeastOne)
              codeGenerator.genCode (" | ");
            codeGenerator.genCode ("(active" + j + " &= old" + j + ")");
          }

          codeGenerator.genCodeLine (") == 0L)");
          if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
            NfaState.nfa ().getGeneratedStates () != 0)
          {
            codeGenerator.genCode ("      return jjStartNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(" +
                                   (i - 2) +
                                   ", ");
            for (j = 0; j < maxLongsReqd - 1; j++)
              if (i <= strLit ().getMaxLenForActive ()[j] + 1)
                codeGenerator.genCode ("old" + j + ", ");
              else
                codeGenerator.genCode ("0L, ");
            if (i <= strLit ().getMaxLenForActive ()[j] + 1)
              codeGenerator.genCodeLine ("old" + j + ");");
            else
              codeGenerator.genCodeLine ("0L);");
          }
          else
            if (NfaState.nfa ().getGeneratedStates () != 0)
              codeGenerator.genCodeLine ("      return jjMoveNfa" +
                                         LexGenJava.lexer ().getLexStateSuffix () +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", " +
                                         (i - 1) +
                                         ");");
            else
              codeGenerator.genCodeLine ("      return " + i + ";");
        }

        if (i != 0 && Options.isDebugTokenManager ())
        {
          switch (eOutputLanguage)
          {
            case JAVA:
              codeGenerator.genCodeLine ("   if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                         Integer.toHexString (Integer.MAX_VALUE) +
                                         ")");
              codeGenerator.genCodeLine ("      debugStream.println(\"   Currently matched the first \" + " +
                                         "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
              codeGenerator.genCodeLine ("   debugStream.println(\"   Possible string literal matches : { \"");
              break;
            case CPP:
              codeGenerator.genCodeLine ("   if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                         Integer.toHexString (Integer.MAX_VALUE) +
                                         ")");
              codeGenerator.genCodeLine ("      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1), addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
              codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Possible string literal matches : { \");");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }

          final StringBuilder fmt = new StringBuilder ();
          final StringBuilder args = new StringBuilder ();
          for (int vecs = 0; vecs < strLit ().getMaxStrKind () / 64 + 1; vecs++)
          {
            if (i <= strLit ().getMaxLenForActive ()[vecs])
            {
              switch (eOutputLanguage)
              {
                case JAVA:
                  codeGenerator.genCodeLine (" +");
                  codeGenerator.genCode ("         jjKindsForBitVector(" + vecs + ", ");
                  codeGenerator.genCode ("active" + vecs + ") ");
                  break;
                case CPP:
                  if (fmt.length () > 0)
                  {
                    fmt.append (", ");
                    args.append (", ");
                  }
                  fmt.append ("%s");
                  args.append ("         jjKindsForBitVector(" + vecs + ", ");
                  args.append ("active" + vecs + ").c_str() ");
                  break;
                default:
                  throw new UnsupportedOutputLanguageException (eOutputLanguage);
              }
            }
          }

          switch (eOutputLanguage)
          {
            case JAVA:
              codeGenerator.genCodeLine (" + \" } \");");
              break;
            case CPP:
              fmt.append ("}\\n");
              codeGenerator.genCodeLine ("    fprintf(debugStream, \"" + fmt + "\"," + args + ");");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }
        }

        switch (eOutputLanguage)
        {
          case JAVA:
            codeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
            codeGenerator.genCodeLine ("   catch(java.io.IOException e) {");
            break;
          case CPP:
            codeGenerator.genCodeLine ("   if (input_stream->endOfInput()) {");
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }

        if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
          NfaState.nfa ().getGeneratedStates () != 0)
        {
          codeGenerator.genCode ("      jjStopStringLiteralDfa" +
                                 LexGenJava.lexer ().getLexStateSuffix () +
                                 "(" +
                                 (i - 1) +
                                 ", ");

          int k = 0;
          for (; k < maxLongsReqd - 1; k++)
          {
            if (i <= strLit ().getMaxLenForActive ()[k])
              codeGenerator.genCode ("active" + k + ", ");
            else
              codeGenerator.genCode ("0L, ");
          }

          if (i <= strLit ().getMaxLenForActive ()[k])
          {
            codeGenerator.genCodeLine ("active" + k + ");");
          }
          else
          {
            codeGenerator.genCodeLine ("0L);");
          }

          if (i != 0 && Options.isDebugTokenManager ())
          {
            switch (eOutputLanguage)
            {
              case JAVA:
                codeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                           Integer.toHexString (Integer.MAX_VALUE) +
                                           ")");
                codeGenerator.genCodeLine ("         debugStream.println(\"   Currently matched the first \" + " +
                                           "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
                break;
              case CPP:
                codeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                           Integer.toHexString (Integer.MAX_VALUE) +
                                           ")");
                codeGenerator.genCodeLine ("      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
                break;
              default:
                throw new UnsupportedOutputLanguageException (eOutputLanguage);
            }
          }

          codeGenerator.genCodeLine ("      return " + i + ";");
        }
        else
          if (NfaState.nfa ().getGeneratedStates () != 0)
          {
            codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                       LexGenJava.lexer ().getLexStateSuffix () +
                                       "(" +
                                       NfaState.initStateName () +
                                       ", " +
                                       (i - 1) +
                                       ");");
          }
          else
          {
            codeGenerator.genCodeLine ("      return " + i + ";");
          }

        codeGenerator.genCodeLine ("   }");
      }

      if (i != 0)
      {
        switch (eOutputLanguage)
        {
          case JAVA:
            // Nothing
            break;
          case CPP:
            codeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }

        if (Options.isDebugTokenManager ())
        {
          switch (eOutputLanguage)
          {
            case JAVA:
              codeGenerator.genCodeLine ("   debugStream.println(" +
                                         (LexGenJava.lexer ().getMaxLexStates () > 1
                                                                                     ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                                     : "") +
                                         "\"Current character : \" + " +
                                         Options.getTokenMgrErrorClass () +
                                         ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                                         "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
              break;
            case CPP:
              codeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                         "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                         "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, curChar, " +
                                         "input_stream->getEndLine(), input_stream->getEndColumn());");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }
        }
      }

      codeGenerator.genCodeLine ("   switch(curChar)");
      codeGenerator.genCodeLine ("   {");

      CaseLoop: for (final String aKey : keys)
      {
        key = aKey;
        info = tab.get (key);
        ifGenerated = false;
        final char c = key.charAt (0);

        if (i == 0 &&
          c < 128 &&
          info.m_finalKindCnt != 0 &&
          (NfaState.nfa ().getGeneratedStates () == 0 || !NfaState.canStartNfaUsingAscii (c)))
        {
          int kind;
          int j = 0;
          for (; j < maxLongsReqd; j++)
            if (info.m_finalKinds[j] != 0L)
              break;

          for (int k = 0; k < 64; k++)
            if ((info.m_finalKinds[j] & (1L << k)) != 0L && !strLit ().getSubString ()[kind = (j * 64 + k)])
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
                if ((LexGenJava.lexer ().getToSkip ()[kind / 64] & (1L << (kind % 64))) != 0L &&
                  (LexGenJava.lexer ().getToSpecial ()[kind / 64] & (1L << (kind % 64))) == 0L &&
                  LexGenJava.lexer ().getActions ()[kind] == null &&
                  LexGenJava.lexer ().getNewLexState ()[kind] == null)
                {
                  LexGenJava.addCharToSkip (c, kind);

                  if (Options.isIgnoreCase ())
                  {
                    if (c != Character.toUpperCase (c))
                      LexGenJava.addCharToSkip (Character.toUpperCase (c), kind);

                    if (c != Character.toLowerCase (c))
                      LexGenJava.addCharToSkip (Character.toLowerCase (c), kind);
                  }
                  continue CaseLoop;
                }
            }
        }

        // Since we know key is a single character ...
        if (Options.isIgnoreCase ())
        {
          if (c != Character.toUpperCase (c))
            codeGenerator.genCodeLine ("      case " + _getCaseChar (Character.toUpperCase (c), eOutputLanguage) + ":");

          if (c != Character.toLowerCase (c))
            codeGenerator.genCodeLine ("      case " + _getCaseChar (Character.toLowerCase (c), eOutputLanguage) + ":");
        }

        codeGenerator.genCodeLine ("      case " + _getCaseChar (c, eOutputLanguage) + ":");

        long matchedKind;
        final String prefix = (i == 0) ? "         " : "            ";

        if (info.m_finalKindCnt != 0)
        {
          for (int j = 0; j < maxLongsReqd; j++)
          {
            if ((matchedKind = info.m_finalKinds[j]) == 0L)
              continue;

            for (int k = 0; k < 64; k++)
            {
              if ((matchedKind & (1L << k)) == 0L)
                continue;

              if (ifGenerated)
              {
                codeGenerator.genCode ("         else if ");
              }
              else
                if (i != 0)
                  codeGenerator.genCode ("         if ");

              ifGenerated = true;

              int kindToPrint;
              if (i != 0)
              {
                codeGenerator.genCodeLine ("((active" +
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
                                      getLine (j * 64 + k) +
                                      ", column " +
                                      getColumn (j * 64 + k) +
                                      ". It will be matched as " +
                                      getLabel (strLit ().getIntermediateKinds ()[(j * 64 + k)][i]) +
                                      ".");
                kindToPrint = strLit ().getIntermediateKinds ()[(j * 64 + k)][i];
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
                                        getLine (j * 64 + k) +
                                        ", column " +
                                        getColumn (j * 64 + k) +
                                        ". It will be matched as " +
                                        getLabel (LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ()
                                                                                                      .getLexStateIndex ()]) +
                                        ".");
                  kindToPrint = LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()];
                }
                else
                  kindToPrint = j * 64 + k;

              if (!strLit ().getSubString ()[(j * 64 + k)])
              {
                final int stateSetName = _getStateSetForKind (i, j * 64 + k);

                if (stateSetName != -1)
                {
                  createStartNfa = true;
                  codeGenerator.genCodeLine (prefix +
                                             "return jjStartNfaWithStates" +
                                             LexGenJava.lexer ().getLexStateSuffix () +
                                             "(" +
                                             i +
                                             ", " +
                                             kindToPrint +
                                             ", " +
                                             stateSetName +
                                             ");");
                }
                else
                  codeGenerator.genCodeLine (prefix + "return jjStopAtPos" + "(" + i + ", " + kindToPrint + ");");
              }
              else
              {
                if ((LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != 0 &&
                  LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != Integer.MAX_VALUE) ||
                  i != 0)
                {
                  codeGenerator.genCodeLine ("         {");
                  codeGenerator.genCodeLine (prefix + "jjmatchedKind = " + kindToPrint + ";");
                  codeGenerator.genCodeLine (prefix + "jjmatchedPos = " + i + ";");
                  codeGenerator.genCodeLine ("         }");
                }
                else
                  codeGenerator.genCodeLine (prefix + "jjmatchedKind = " + kindToPrint + ";");
              }
            }
          }
        }

        if (info.m_validKindCnt != 0)
        {
          atLeastOne = false;

          if (i == 0)
          {
            codeGenerator.genCode ("         return ");

            codeGenerator.genCode ("jjMoveStringLiteralDfa" + (i + 1) + LexGenJava.lexer ().getLexStateSuffix () + "(");
            int j = 0;
            for (; j < maxLongsReqd - 1; j++)
              if ((i + 1) <= strLit ().getMaxLenForActive ()[j])
              {
                if (atLeastOne)
                  codeGenerator.genCode (", ");
                else
                  atLeastOne = true;

                codeGenerator.genCode (eOutputLanguage.getLongHex (info.m_validKinds[j]));
              }

            if ((i + 1) <= strLit ().getMaxLenForActive ()[j])
            {
              if (atLeastOne)
                codeGenerator.genCode (", ");

              codeGenerator.genCode (eOutputLanguage.getLongHex (info.m_validKinds[j]));
            }
            codeGenerator.genCodeLine (");");
          }
          else
          {
            codeGenerator.genCode ("         return ");

            codeGenerator.genCode ("jjMoveStringLiteralDfa" + (i + 1) + LexGenJava.lexer ().getLexStateSuffix () + "(");

            int j = 0;
            for (; j < maxLongsReqd - 1; j++)
              if ((i + 1) <= strLit ().getMaxLenForActive ()[j] + 1)
              {
                if (atLeastOne)
                  codeGenerator.genCode (", ");
                else
                  atLeastOne = true;

                if (info.m_validKinds[j] != 0L)
                  codeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongHex (info.m_validKinds[j]));
                else
                  codeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongPlain (0));
              }

            if ((i + 1) <= strLit ().getMaxLenForActive ()[j] + 1)
            {
              if (atLeastOne)
                codeGenerator.genCode (", ");
              if (info.m_validKinds[j] != 0L)
                codeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongHex (info.m_validKinds[j]));
              else
                codeGenerator.genCode ("active" + j + ", " + eOutputLanguage.getLongPlain (0));
            }

            codeGenerator.genCodeLine (");");
          }
        }
        else
        {
          // A very special case.
          if (i == 0 && LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
          {
            if (NfaState.nfa ().getGeneratedStates () != 0)
              codeGenerator.genCodeLine ("         return jjMoveNfa" +
                                         LexGenJava.lexer ().getLexStateSuffix () +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", 0);");
            else
              codeGenerator.genCodeLine ("         return 1;");
          }
          else
            if (i != 0) // No more str literals to look for
            {
              codeGenerator.genCodeLine ("         break;");
              startNfaNeeded = true;
            }
        }
      }

      /*
       * default means that the current character is not in any of the strings at this position.
       */
      codeGenerator.genCodeLine ("      default :");

      if (Options.isDebugTokenManager ())
      {
        switch (eOutputLanguage)
        {
          case JAVA:
            codeGenerator.genCodeLine ("      debugStream.println(\"   No string literal matches possible.\");");
            break;
          case CPP:
            codeGenerator.genCodeLine ("      fprintf(debugStream, \"   No string literal matches possible.\\n\");");
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
          codeGenerator.genCodeLine ("         return jjMoveNfa" +
                                     LexGenJava.lexer ().getLexStateSuffix () +
                                     "(" +
                                     NfaState.initStateName () +
                                     ", 0);");
        }
        else
        {
          codeGenerator.genCodeLine ("         break;");
          startNfaNeeded = true;
        }
      }
      else
      {
        codeGenerator.genCodeLine ("         return " + (i + 1) + ";");
      }

      codeGenerator.genCodeLine ("   }");

      if (i != 0)
      {
        if (startNfaNeeded)
        {
          if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
            NfaState.nfa ().getGeneratedStates () != 0)
          {
            /*
             * Here, a string literal is successfully matched and no more string literals are
             * possible. So set the kind and state set upto and including this position for the
             * matched string.
             */

            codeGenerator.genCode ("   return jjStartNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(" +
                                   (i - 1) +
                                   ", ");

            int k = 0;
            for (; k < maxLongsReqd - 1; k++)
            {
              if (i <= strLit ().getMaxLenForActive ()[k])
                codeGenerator.genCode ("active" + k + ", ");
              else
                codeGenerator.genCode ("0L, ");
            }
            if (i <= strLit ().getMaxLenForActive ()[k])
              codeGenerator.genCodeLine ("active" + k + ");");
            else
              codeGenerator.genCodeLine ("0L);");
          }
          else
            if (NfaState.nfa ().getGeneratedStates () != 0)
              codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                         LexGenJava.lexer ().getLexStateSuffix () +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", " +
                                         i +
                                         ");");
            else
              codeGenerator.genCodeLine ("   return " + (i + 1) + ";");
        }
      }

      codeGenerator.genCodeLine ("}");
    }

    if (!LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()] &&
      NfaState.nfa ().getGeneratedStates () != 0 &&
      createStartNfa)
      dumpStartWithStates (codeGenerator);
  }

  static final int getStrKind (final String str)
  {
    for (int i = 0; i < strLit ().getMaxStrKind (); i++)
    {
      if (LexGenJava.lexer ().getLexStates ()[i] != LexGenJava.lexer ().getLexStateIndex ())
        continue;

      final String image = strLit ().getAllImages ()[i];
      if (image != null && image.equals (str))
        return i;
    }

    return Integer.MAX_VALUE;
  }

  public static void generateNfaStartStates (final CodeGenerator codeGenerator, final NfaState initialState)
  {
    final boolean [] seen = new boolean [NfaState.nfa ().getGeneratedStates ()];
    final Map <String, String> stateSets = new HashMap <> ();
    String stateSetString = "";
    int i, j, kind, jjmatchedPos = 0;
    final int maxKindsReqd = strLit ().getMaxStrKind () / 64 + 1;
    long [] actives;
    List <NfaState> newStates = new ArrayList <> ();
    List <NfaState> oldStates = null;
    List <NfaState> jjtmpStates;

    strLit ().setStatesForPos (GenericReflection.uncheckedCast (new Map [strLit ().getMaxLen ()]));
    strLit ().setIntermediateKinds (new int [strLit ().getMaxStrKind () + 1] []);
    strLit ().setIntermediateMatchedPos (new int [strLit ().getMaxStrKind () + 1] []);

    for (i = 0; i < strLit ().getMaxStrKind (); i++)
    {
      if (LexGenJava.lexer ().getLexStates ()[i] != LexGenJava.lexer ().getLexStateIndex ())
        continue;

      final String image = strLit ().getAllImages ()[i];

      if (image == null || image.length () < 1)
        continue;

      try
      {
        oldStates = new ArrayList <> (initialState.m_epsilonMoves);
        if (oldStates.size () == 0)
        {
          dumpNfaStartStatesCode (strLit ().getStatesForPos (), codeGenerator);
          return;
        }
      }
      catch (final Exception e)
      {
        JavaCCErrors.semantic_error ("Error cloning state vector");
      }

      strLit ().getIntermediateKinds ()[i] = new int [image.length ()];
      strLit ().getIntermediateMatchedPos ()[i] = new int [image.length ()];
      jjmatchedPos = 0;
      kind = Integer.MAX_VALUE;

      for (j = 0; j < image.length (); j++)
      {
        if (oldStates == null || oldStates.size () <= 0)
        {
          // Here, j > 0
          kind = strLit ().getIntermediateKinds ()[i][j] = strLit ().getIntermediateKinds ()[i][j - 1];
          jjmatchedPos = strLit ().getIntermediateMatchedPos ()[i][j] = strLit ().getIntermediateMatchedPos ()[i][j -
                                                                                                                  1];
        }
        else
        {
          kind = NfaState.moveFromSet (image.charAt (j), oldStates, newStates);
          oldStates.clear ();

          if (j == 0 &&
            kind != Integer.MAX_VALUE &&
            LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()] != -1 &&
            kind > LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()])
            kind = LexGenJava.lexer ().getCanMatchAnyChar ()[LexGenJava.lexer ().getLexStateIndex ()];

          if (getStrKind (image.substring (0, j + 1)) < kind)
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

          stateSetString = NfaState.getStateSetString (newStates);
        }

        if (kind == Integer.MAX_VALUE && (newStates == null || newStates.size () == 0))
          continue;

        int p;
        if (stateSets.get (stateSetString) == null)
        {
          stateSets.put (stateSetString, stateSetString);
          for (p = 0; p < newStates.size (); p++)
          {
            if (seen[newStates.get (p).m_stateName])
              newStates.get (p).m_inNextOf++;
            else
              seen[newStates.get (p).m_stateName] = true;
          }
        }
        else
        {
          for (p = 0; p < newStates.size (); p++)
            seen[newStates.get (p).m_stateName] = true;
        }

        jjtmpStates = oldStates;
        oldStates = newStates;
        (newStates = jjtmpStates).clear ();

        if (strLit ().getStatesForPos ()[j] == null)
          strLit ().getStatesForPos ()[j] = new HashMap <> ();

        actives = strLit ().getStatesForPos ()[j].computeIfAbsent (kind + ", " + jjmatchedPos + ", " + stateSetString,
                                                                   k -> new long [maxKindsReqd]);

        actives[i / 64] |= 1L << (i % 64);
        // String name = NfaState.StoreStateSet(stateSetString);
      }
    }

    dumpNfaStartStatesCode (strLit ().getStatesForPos (), codeGenerator);
  }

  static void dumpNfaStartStatesCode (final Map <String, long []> [] statesForPos, final CodeGenerator codeGenerator)
  {
    if (strLit ().getMaxStrKind () == 0)
    { // No need to generate this function
      return;
    }

    final EOutputLanguage eOutputLanguage = codeGenerator.getOutputLanguage ();
    int i;
    final int maxKindsReqd = strLit ().getMaxStrKind () / 64 + 1;
    boolean condGenerated = false;
    int ind = 0;

    final StringBuilder params = new StringBuilder ();
    for (i = 0; i < maxKindsReqd - 1; i++)
      params.append (eOutputLanguage.getTypeLong () + " active" + i + ", ");
    params.append (eOutputLanguage.getTypeLong () + " active" + i + ")");

    switch (eOutputLanguage)
    {
      case JAVA:
        codeGenerator.genCode ("private final int jjStopStringLiteralDfa" +
                               LexGenJava.lexer ().getLexStateSuffix () +
                               "(int pos, " +
                               params);
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader (" int",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStopStringLiteralDfa" +
                                                                                            LexGenJava.lexer ()
                                                                                                      .getLexStateSuffix () +
                                                                                            "(int pos, " +
                                                                                            params);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    codeGenerator.genCodeLine ("{");

    if (Options.isDebugTokenManager ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          codeGenerator.genCodeLine ("      debugStream.println(\"   No more string literal token matches are possible.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("      fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    codeGenerator.genCodeLine ("   switch (pos)");
    codeGenerator.genCodeLine ("   {");

    for (i = 0; i < strLit ().getMaxLen () - 1; i++)
    {
      if (statesForPos[i] == null)
        continue;

      codeGenerator.genCodeLine ("      case " + i + ":");

      for (final Map.Entry <String, long []> aEntry : statesForPos[i].entrySet ())
      {
        String stateSetString = aEntry.getKey ();
        final long [] actives = aEntry.getValue ();

        for (int j = 0; j < maxKindsReqd; j++)
        {
          if (actives[j] == 0L)
            continue;

          if (condGenerated)
            codeGenerator.genCode (" || ");
          else
            codeGenerator.genCode ("         if (");

          condGenerated = true;

          codeGenerator.genCode ("(active" +
                                 j +
                                 " & " +
                                 eOutputLanguage.getLongHex (actives[j]) +
                                 ") != " +
                                 eOutputLanguage.getLongPlain (0));
        }

        if (condGenerated)
        {
          codeGenerator.genCodeLine (")");

          String kindStr = stateSetString.substring (0, ind = stateSetString.indexOf (", "));
          String afterKind = stateSetString.substring (ind + 2);
          final int jjmatchedPos = Integer.parseInt (afterKind.substring (0, afterKind.indexOf (", ")));

          if (!kindStr.equals (String.valueOf (Integer.MAX_VALUE)))
            codeGenerator.genCodeLine ("         {");

          if (!kindStr.equals (String.valueOf (Integer.MAX_VALUE)))
          {
            if (i == 0)
            {
              codeGenerator.genCodeLine ("            jjmatchedKind = " + kindStr + ";");

              if ((LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != 0 &&
                LexGenJava.lexer ().getInitMatch ()[LexGenJava.lexer ().getLexStateIndex ()] != Integer.MAX_VALUE))
                codeGenerator.genCodeLine ("            jjmatchedPos = 0;");
            }
            else
              if (i == jjmatchedPos)
              {
                if (strLit ().getSubStringAtPos ()[i])
                {
                  codeGenerator.genCodeLine ("            if (jjmatchedPos != " + i + ")");
                  codeGenerator.genCodeLine ("            {");
                  codeGenerator.genCodeLine ("               jjmatchedKind = " + kindStr + ";");
                  codeGenerator.genCodeLine ("               jjmatchedPos = " + i + ";");
                  codeGenerator.genCodeLine ("            }");
                }
                else
                {
                  codeGenerator.genCodeLine ("            jjmatchedKind = " + kindStr + ";");
                  codeGenerator.genCodeLine ("            jjmatchedPos = " + i + ";");
                }
              }
              else
              {
                if (jjmatchedPos > 0)
                  codeGenerator.genCodeLine ("            if (jjmatchedPos < " + jjmatchedPos + ")");
                else
                  codeGenerator.genCodeLine ("            if (jjmatchedPos == 0)");
                codeGenerator.genCodeLine ("            {");
                codeGenerator.genCodeLine ("               jjmatchedKind = " + kindStr + ";");
                codeGenerator.genCodeLine ("               jjmatchedPos = " + jjmatchedPos + ";");
                codeGenerator.genCodeLine ("            }");
              }
          }

          kindStr = stateSetString.substring (0, ind = stateSetString.indexOf (", "));
          afterKind = stateSetString.substring (ind + 2);
          stateSetString = afterKind.substring (afterKind.indexOf (", ") + 2);

          if (stateSetString.equals ("null;"))
            codeGenerator.genCodeLine ("            return -1;");
          else
            codeGenerator.genCodeLine ("            return " + NfaState.addStartStateSet (stateSetString) + ";");

          if (!kindStr.equals (String.valueOf (Integer.MAX_VALUE)))
            codeGenerator.genCodeLine ("         }");
          condGenerated = false;
        }
      }

      codeGenerator.genCodeLine ("         return -1;");
    }

    codeGenerator.genCodeLine ("      default :");
    codeGenerator.genCodeLine ("         return -1;");
    codeGenerator.genCodeLine ("   }");
    codeGenerator.genCodeLine ("}");

    params.setLength (0);
    params.append ("(int pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++)
      params.append (eOutputLanguage.getTypeLong () + " active" + i + ", ");
    params.append (eOutputLanguage.getTypeLong () + " active" + i + ")");

    switch (eOutputLanguage)
    {
      case JAVA:
        codeGenerator.genCode ("private final int jjStartNfa" + LexGenJava.lexer ().getLexStateSuffix () + params);
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader ("int ",
                                               LexGenJava.lexer ().getTokenMgrClassName (),
                                               "jjStartNfa" + LexGenJava.lexer ().getLexStateSuffix () + params);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    codeGenerator.genCodeLine ("{");

    if (LexGenJava.lexer ().getMixed ()[LexGenJava.lexer ().getLexStateIndex ()])
    {
      if (NfaState.nfa ().getGeneratedStates () != 0)
        codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                   LexGenJava.lexer ().getLexStateSuffix () +
                                   "(" +
                                   NfaState.initStateName () +
                                   ", pos + 1);");
      else
        codeGenerator.genCodeLine ("   return pos + 1;");

      codeGenerator.genCodeLine ("}");
      return;
    }

    codeGenerator.genCode ("   return jjMoveNfa" +
                           LexGenJava.lexer ().getLexStateSuffix () +
                           "(" +
                           "jjStopStringLiteralDfa" +
                           LexGenJava.lexer ().getLexStateSuffix () +
                           "(pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++)
      codeGenerator.genCode ("active" + i + ", ");
    codeGenerator.genCode ("active" + i + ")");
    codeGenerator.genCodeLine (", pos + 1);");
    codeGenerator.genCodeLine ("}");
  }

  @Override
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    final StringBuilder sb = super.dump (indent, alreadyDumped).append (' ').append (m_image);
    return sb;
  }

  @Override
  public String toString ()
  {
    return super.toString () + " - " + m_image;
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


  public static void updateStringLiteralData (final int lexStateIndex)
  {
    for (int kind = 0; kind < strLit ().getAllImages ().length; kind++)
    {
      if (StringHelper.isEmpty (strLit ().getAllImages ()[kind]) ||
        LexGenJava.lexer ().getLexStates ()[kind] != lexStateIndex)
      {
        continue;
      }
      String s = strLit ().getAllImages ()[kind];
      int actualKind;
      if (strLit ().getIntermediateKinds () != null &&
        strLit ().getIntermediateKinds ()[kind][s.length () - 1] != Integer.MAX_VALUE &&
        strLit ().getIntermediateKinds ()[kind][s.length () - 1] < kind)
      {
        JavaCCErrors.warning ("Token: " +
                              s +
                              " will not be matched as " +
                              "specified. It will be matched as token " +
                              "of kind: " +
                              strLit ().getIntermediateKinds ()[kind][s.length () - 1] +
                              " instead.");
        actualKind = strLit ().getIntermediateKinds ()[kind][s.length () - 1];
      }
      else
      {
        actualKind = kind;
      }
      NfaState.tokenizerBuild ().kindToLexicalState ().put (Integer.valueOf (actualKind), Integer.valueOf (lexStateIndex));
      if (Options.isIgnoreCase ())
      {
        s = s.toLowerCase (Locale.US);
      }
      final char c = s.charAt (0);
      final int key = LexGenJava.lexer ().getLexStateIndex () << 16 | c;
      List <String> l = NfaState.tokenizerBuild ().literalsByLength ().get (Integer.valueOf (key));
      List <Integer> kinds = NfaState.tokenizerBuild ().literalKinds ().get (Integer.valueOf (key));
      int j = 0;
      if (l == null)
      {
        NfaState.tokenizerBuild ().literalsByLength ().put (Integer.valueOf (key), l = new ArrayList <> ());
        assert (kinds == null);
        kinds = new ArrayList <> ();
        NfaState.tokenizerBuild ().literalKinds ().put (Integer.valueOf (key), kinds = new ArrayList <> ());
      }
      while (j < l.size () && l.get (j).length () > s.length ())
        j++;
      l.add (j, s);
      kinds.add (j, Integer.valueOf (actualKind));
      final int stateIndex = _getStateSetForKind (s.length () - 1, kind);
      if (stateIndex != -1)
      {
        NfaState.tokenizerBuild ().nfaStateMap ().put (Integer.valueOf (actualKind), NfaState.getNfaState (stateIndex));
      }
      else
      {
        NfaState.tokenizerBuild ().nfaStateMap ().put (Integer.valueOf (actualKind), null);
      }
    }
  }

  public static void BuildTokenizerData (final TokenizerData tokenizerData)
  {
    final Map <Integer, Integer> nfaStateIndices = new HashMap <> ();
    for (final int kind : NfaState.tokenizerBuild ().nfaStateMap ().keySet ())
    {
      if (NfaState.tokenizerBuild ().nfaStateMap ().get (Integer.valueOf (kind)) != null)
      {
        nfaStateIndices.put (Integer.valueOf (kind),
                             Integer.valueOf (NfaState.tokenizerBuild ().nfaStateMap ().get (Integer.valueOf (kind)).m_stateName));
      }
      else
      {
        nfaStateIndices.put (Integer.valueOf (kind), Integer.valueOf (-1));
      }
    }
    tokenizerData.setLiteralSequence (NfaState.tokenizerBuild ().literalsByLength ());
    tokenizerData.setLiteralKinds (NfaState.tokenizerBuild ().literalKinds ());
    tokenizerData.setKindToNfaStartState (nfaStateIndices);
  }
}
