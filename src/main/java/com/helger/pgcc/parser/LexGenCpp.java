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

import static com.helger.pgcc.parser.JavaCCGlobals.LEXSTATE_I2S;
import static com.helger.pgcc.parser.JavaCCGlobals.REXPR_LIST;
import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.s_aActForEof;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_sNextStateForEof;
import static com.helger.pgcc.parser.JavaCCGlobals.s_token_mgr_decls;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolNames;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.OutputHelper;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;
import com.helger.pgcc.parser.exp.ExpRChoice;
import com.helger.pgcc.parser.exp.ExpRStringLiteral;

/**
 * Generate lexer.
 */
public class LexGenCpp extends LexGenJava
{
  private void _printClassHead ()
  {
    final List <String> tn = new ArrayList <> (s_toolNames);
    tn.add (CPG.APP_NAME);

    switchToStaticsFile ();

    // standard includes
    switchToIncludeFile ();
    genCodeLine ("#include \"stdio.h\"");
    genCodeLine ("#include \"JavaCC.h\"");
    genCodeLine ("#include \"CharStream.h\"");
    genCodeLine ("#include \"Token.h\"");
    genCodeLine ("#include \"ErrorHandler.h\"");
    genCodeLine ("#include \"TokenManager.h\"");
    genCodeLine ("#include \"" + s_cu_name + "Constants.h\"");

    if (Options.stringValue (Options.USEROPTION__CPP_TOKEN_MANAGER_INCLUDES).length () > 0)
    {
      genCodeLine ("#include \"" + Options.stringValue (Options.USEROPTION__CPP_TOKEN_MANAGER_INCLUDES) + "\"\n");
    }

    genCodeNewLine ();

    if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
    {
      genCodeLine ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
    }

    genCodeLine ("class " + s_cu_name + ";");

    /*
     * final int l = 0, kind; i = 1; namespace? for (;;) { if
     * (cu_to_insertion_point_1.size() <= l) break; kind =
     * ((Token)cu_to_insertion_point_1.get(l)).kind; if(kind == PACKAGE || kind
     * == IMPORT) { for (; i < cu_to_insertion_point_1.size(); i++) { kind =
     * ((Token)cu_to_insertion_point_1.get(i)).kind; if (kind == CLASS) { cline
     * = ((Token)(cu_to_insertion_point_1.get(l))).beginLine; ccol =
     * ((Token)(cu_to_insertion_point_1.get(l))).beginColumn; for (j = l; j < i;
     * j++) { printToken((Token)(cu_to_insertion_point_1.get(j))); } if (kind ==
     * SEMICOLON) printToken((Token)(cu_to_insertion_point_1.get(j)));
     * genCodeLine(""); break; } } l = ++i; } else break; }
     */

    genCodeNewLine ();
    genCodeLine ("/** Token Manager. */");
    final String superClass = Options.stringValue (Options.USEROPTION__TOKEN_MANAGER_SUPER_CLASS);
    genClassStart (null,
                   s_tokMgrClassName,
                   new String [] {},
                   new String [] { "public TokenManager" + (superClass == null ? "" : ", public " + superClass) });

    if (s_token_mgr_decls != null && s_token_mgr_decls.isNotEmpty ())
    {
      Token t = s_token_mgr_decls.get (0);
      boolean bCommonTokenActionSeen = false;
      final boolean bCommonTokenActionNeeded = Options.isCommonTokenAction ();

      printTokenSetup (s_token_mgr_decls.get (0));
      setColToStart ();

      switchToMainFile ();
      for (final Token s_token_mgr_decl : s_token_mgr_decls)
      {
        t = s_token_mgr_decl;
        if (t.kind == JavaCCParserConstants.IDENTIFIER && bCommonTokenActionNeeded && !bCommonTokenActionSeen)
        {
          bCommonTokenActionSeen = t.image.equals ("CommonTokenAction");
          if (bCommonTokenActionSeen)
            t.image = s_cu_name + "TokenManager::" + t.image;
        }

        printToken (t);
      }

      switchToIncludeFile ();
      genCodeLine ("  void CommonTokenAction(Token* token);");

      if (Options.isTokenManagerUsesParser ())
      {
        genCodeLine ("  void setParser(void* parser) {");
        genCodeLine ("      this->parser = (" + s_cu_name + "*) parser;");
        genCodeLine ("  }");
      }
      genCodeNewLine ();

      if (bCommonTokenActionNeeded && !bCommonTokenActionSeen)
      {
        JavaCCErrors.warning ("You have the COMMON_TOKEN_ACTION option set. " +
                              "But it appears you have not defined the method :\n" +
                              "      " +
                              "void CommonTokenAction(Token *t)\n" +
                              "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
      }
    }
    else
      if (Options.isCommonTokenAction ())
      {
        JavaCCErrors.warning ("You have the COMMON_TOKEN_ACTION option set. " +
                              "But you have not defined the method :\n" +
                              "      " +
                              "void CommonTokenAction(Token *t)\n" +
                              "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
      }

    genCodeNewLine ();
    genCodeLine ("  FILE *debugStream;");

    generateMethodDefHeader ("  void ", s_tokMgrClassName, "setDebugStream(FILE *ds)");
    genCodeLine ("{ debugStream = ds; }");

    switchToIncludeFile ();
    if (Options.isTokenManagerUsesParser ())
    {
      genCodeNewLine ();
      genCodeLine ("private:");
      genCodeLine ("  " + s_cu_name + "* parser = nullptr;");
    }
    switchToMainFile ();
  }

  private void _dumpDebugMethods () throws IOException
  {
    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("maxOrdinal", Integer.toString (s_maxOrdinal));
    aOpts.put ("stateSetSize", Integer.toString (s_stateSetSize));
    writeTemplate ("/templates/cpp/DumpDebugMethods.template", aOpts);
  }

  private static void _buildLexStatesTable ()
  {
    final Iterator <TokenProduction> it = REXPR_LIST.iterator ();
    TokenProduction tp;
    int i;

    final String [] tmpLexStateName = new String [LEXSTATE_I2S.size ()];
    while (it.hasNext ())
    {
      tp = it.next ();
      final List <RegExprSpec> respecs = tp.m_respecs;
      List <TokenProduction> tps;

      for (i = 0; i < tp.m_lexStates.length; i++)
      {
        tps = s_allTpsForState.get (tp.m_lexStates[i]);
        if (tps == null)
        {
          tmpLexStateName[s_maxLexStates++] = tp.m_lexStates[i];
          tps = new ArrayList <> ();
          s_allTpsForState.put (tp.m_lexStates[i], tps);
        }

        tps.add (tp);
      }

      if (respecs == null || respecs.size () == 0)
        continue;

      for (i = 0; i < respecs.size (); i++)
      {
        final AbstractExpRegularExpression re = respecs.get (i).rexp;
        if (s_maxOrdinal <= re.getOrdinal ())
          s_maxOrdinal = re.getOrdinal () + 1;
      }
    }

    s_kinds = new ETokenKind [s_maxOrdinal];
    s_toSkip = new long [s_maxOrdinal / 64 + 1];
    s_toSpecial = new long [s_maxOrdinal / 64 + 1];
    s_toMore = new long [s_maxOrdinal / 64 + 1];
    s_toToken = new long [s_maxOrdinal / 64 + 1];
    s_toToken[0] = 1L;
    s_actions = new ExpAction [s_maxOrdinal];
    s_actions[0] = s_aActForEof;
    s_hasTokenActions = s_aActForEof != null;
    s_initStates.clear ();
    s_canMatchAnyChar = new int [s_maxLexStates];
    s_canLoop = new boolean [s_maxLexStates];
    s_stateHasActions = new boolean [s_maxLexStates];
    s_lexStateName = new String [s_maxLexStates];
    s_singlesToSkip = new NfaState [s_maxLexStates];
    System.arraycopy (tmpLexStateName, 0, s_lexStateName, 0, s_maxLexStates);

    for (i = 0; i < s_maxLexStates; i++)
      s_canMatchAnyChar[i] = -1;

    s_hasNfa = new boolean [s_maxLexStates];
    s_mixed = new boolean [s_maxLexStates];
    s_maxLongsReqd = new int [s_maxLexStates];
    s_initMatch = new int [s_maxLexStates];
    s_newLexState = new String [s_maxOrdinal];
    s_newLexState[0] = s_sNextStateForEof;
    s_hasEmptyMatch = false;
    s_lexStates = new int [s_maxOrdinal];
    s_ignoreCase = new boolean [s_maxOrdinal];
    s_rexprs = new AbstractExpRegularExpression [s_maxOrdinal];
    ExpRStringLiteral.s_allImages = new String [s_maxOrdinal];
    s_canReachOnMore = new boolean [s_maxLexStates];
  }

  private static int _getIndex (final String name)
  {
    for (int i = 0; i < s_lexStateName.length; i++)
      if (s_lexStateName[i] != null && s_lexStateName[i].equals (name))
        return i;

    throw new IllegalStateException ("Should never come here");
  }

  @Override
  public void start () throws IOException
  {
    if (!Options.isBuildTokenManager () || Options.isUserTokenManager () || JavaCCErrors.getErrorCount () > 0)
      return;

    s_keepLineCol = Options.isKeepLineColumn ();
    final List <ExpRChoice> choices = new ArrayList <> ();

    s_tokMgrClassName = s_cu_name + "TokenManager";

    _printClassHead ();
    _buildLexStatesTable ();

    boolean ignoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : s_allTpsForState.entrySet ())
    {
      NfaState.reInitStatic ();
      ExpRStringLiteral.reInitStatic ();

      final String key = aEntry.getKey ();

      s_lexStateIndex = _getIndex (key);
      s_lexStateSuffix = "_" + s_lexStateIndex;
      final List <TokenProduction> allTps = aEntry.getValue ();
      s_initialState = new NfaState ();
      s_initStates.put (key, s_initialState);
      ignoring = false;

      s_singlesToSkip[s_lexStateIndex] = new NfaState ();
      s_singlesToSkip[s_lexStateIndex].m_dummy = true;

      if (key.equals ("DEFAULT"))
        s_defaultLexState = s_lexStateIndex;

      for (int i = 0; i < allTps.size (); i++)
      {
        final TokenProduction tp = allTps.get (i);
        final ETokenKind kind = tp.m_kind;
        final boolean ignore = tp.m_ignoreCase;
        final List <RegExprSpec> rexps = tp.m_respecs;

        if (i == 0)
          ignoring = ignore;

        for (final RegExprSpec respec : rexps)
        {
          s_curRE = respec.rexp;

          s_rexprs[s_curKind = s_curRE.getOrdinal ()] = s_curRE;
          s_lexStates[s_curRE.getOrdinal ()] = s_lexStateIndex;
          s_ignoreCase[s_curRE.getOrdinal ()] = ignore;

          if (s_curRE.m_private_rexp)
          {
            s_kinds[s_curRE.getOrdinal ()] = null;
            continue;
          }

          if (s_curRE instanceof ExpRStringLiteral && StringHelper.isNotEmpty (((ExpRStringLiteral) s_curRE).m_image))
          {
            ((ExpRStringLiteral) s_curRE).generateDfa ();
            if (i != 0 && !s_mixed[s_lexStateIndex] && ignoring != ignore)
              s_mixed[s_lexStateIndex] = true;
          }
          else
            if (s_curRE.canMatchAnyChar ())
            {
              if (s_canMatchAnyChar[s_lexStateIndex] == -1 || s_canMatchAnyChar[s_lexStateIndex] > s_curRE.getOrdinal ())
                s_canMatchAnyChar[s_lexStateIndex] = s_curRE.getOrdinal ();
            }
            else
            {
              Nfa temp;

              if (s_curRE instanceof ExpRChoice)
                choices.add ((ExpRChoice) s_curRE);

              temp = s_curRE.generateNfa (ignore);
              temp.end ().m_isFinal = true;
              temp.end ().m_kind = s_curRE.getOrdinal ();
              s_initialState.addMove (temp.start ());
            }

          if (s_kinds.length < s_curRE.getOrdinal ())
          {
            final ETokenKind [] tmp = new ETokenKind [s_curRE.getOrdinal () + 1];

            System.arraycopy (s_kinds, 0, tmp, 0, s_kinds.length);
            s_kinds = tmp;
          }
          // System.out.println(" ordina : " + curRE.ordinal);

          s_kinds[s_curRE.getOrdinal ()] = kind;

          if (respec.nextState != null && !respec.nextState.equals (s_lexStateName[s_lexStateIndex]))
            s_newLexState[s_curRE.getOrdinal ()] = respec.nextState;

          if (respec.act != null && respec.act.getActionTokens () != null && respec.act.getActionTokens ().size () > 0)
            s_actions[s_curRE.getOrdinal ()] = respec.act;

          switch (kind)
          {
            case SPECIAL:
              s_hasSkipActions |= (s_actions[s_curRE.getOrdinal ()] != null) || (s_newLexState[s_curRE.getOrdinal ()] != null);
              s_hasSpecial = true;
              s_toSpecial[s_curRE.getOrdinal () / 64] |= 1L << (s_curRE.getOrdinal () % 64);
              s_toSkip[s_curRE.getOrdinal () / 64] |= 1L << (s_curRE.getOrdinal () % 64);
              break;
            case SKIP:
              s_hasSkipActions |= (s_actions[s_curRE.getOrdinal ()] != null);
              s_hasSkip = true;
              s_toSkip[s_curRE.getOrdinal () / 64] |= 1L << (s_curRE.getOrdinal () % 64);
              break;
            case MORE:
              s_hasMoreActions |= (s_actions[s_curRE.getOrdinal ()] != null);
              s_hasMore = true;
              s_toMore[s_curRE.getOrdinal () / 64] |= 1L << (s_curRE.getOrdinal () % 64);

              if (s_newLexState[s_curRE.getOrdinal ()] != null)
                s_canReachOnMore[_getIndex (s_newLexState[s_curRE.getOrdinal ()])] = true;
              else
                s_canReachOnMore[s_lexStateIndex] = true;

              break;
            case TOKEN:
              s_hasTokenActions |= (s_actions[s_curRE.getOrdinal ()] != null);
              s_toToken[s_curRE.getOrdinal () / 64] |= 1L << (s_curRE.getOrdinal () % 64);
              break;
            default:
              throw new IllegalStateException ();
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      NfaState.computeClosures ();

      for (final NfaState aItem : s_initialState.m_epsilonMoves)
        aItem.generateCode ();

      s_hasNfa[s_lexStateIndex] = (NfaState.s_generatedStates != 0);
      if (s_hasNfa[s_lexStateIndex])
      {
        s_initialState.generateCode ();
        s_initialState.generateInitMoves ();
      }

      if (s_initialState.m_kind != Integer.MAX_VALUE && s_initialState.m_kind != 0)
      {
        if ((s_toSkip[s_initialState.m_kind / 64] & (1L << s_initialState.m_kind)) != 0L ||
            (s_toSpecial[s_initialState.m_kind / 64] & (1L << s_initialState.m_kind)) != 0L)
          s_hasSkipActions = true;
        else
          if ((s_toMore[s_initialState.m_kind / 64] & (1L << s_initialState.m_kind)) != 0L)
            s_hasMoreActions = true;
          else
            s_hasTokenActions = true;

        if (s_initMatch[s_lexStateIndex] == 0 || s_initMatch[s_lexStateIndex] > s_initialState.m_kind)
        {
          s_initMatch[s_lexStateIndex] = s_initialState.m_kind;
          s_hasEmptyMatch = true;
        }
      }
      else
        if (s_initMatch[s_lexStateIndex] == 0)
          s_initMatch[s_lexStateIndex] = Integer.MAX_VALUE;

      ExpRStringLiteral.fillSubString ();

      if (s_hasNfa[s_lexStateIndex] && !s_mixed[s_lexStateIndex])
        ExpRStringLiteral.generateNfaStartStates (this, s_initialState);

      ExpRStringLiteral.dumpDfaCode (this);

      if (s_hasNfa[s_lexStateIndex])
        NfaState.dumpMoveNfa (this);

      if (s_stateSetSize < NfaState.s_generatedStates)
        s_stateSetSize = NfaState.s_generatedStates;
    }

    for (final ExpRChoice aItem : choices)
      aItem.checkUnmatchability ();

    NfaState.dumpStateSets (this);
    checkEmptyStringMatch ();
    NfaState.dumpNonAsciiMoveMethods (this);
    ExpRStringLiteral.dumpStrLiteralImages (this);
    _dumpFillToken ();
    _dumpGetNextToken ();

    if (Options.isDebugTokenManager ())
    {
      NfaState.dumpStatesForKind (this);
      _dumpDebugMethods ();
    }

    if (s_hasLoop)
    {
      switchToStaticsFile ();
      genCodeLine ("static int  jjemptyLineNo[" + s_maxLexStates + "];");
      genCodeLine ("static int  jjemptyColNo[" + s_maxLexStates + "];");
      genCodeLine ("static bool jjbeenHere[" + s_maxLexStates + "];");
      switchToMainFile ();
    }

    if (s_hasSkipActions)
      _dumpSkipActions ();
    if (s_hasMoreActions)
      _dumpMoreActions ();
    if (s_hasTokenActions)
      _dumpTokenActions ();

    NfaState.printBoilerPlateCPP (this);

    {
      final Map <String, Object> aOpts = new HashMap <> ();
      aOpts.put ("charStreamName", "CharStream");
      aOpts.put ("parserClassName", s_cu_name);
      aOpts.put ("defaultLexState", "defaultLexState");
      aOpts.put ("lexStateNameLength", Integer.toString (s_lexStateName.length));
      writeTemplate ("/templates/cpp/TokenManagerBoilerPlateMethods.template", aOpts);
    }

    _dumpBoilerPlateInHeader ();

    // in the include file close the class signature´
    // static vars actually inst
    _dumpStaticVarDeclarations ();

    // remaining variables
    switchToIncludeFile ();
    {
      final Map <String, Object> aOpts = new HashMap <> ();
      aOpts.put ("charStreamName", "CharStream");
      aOpts.put ("lexStateNameLength", Integer.toString (s_lexStateName.length));
      writeTemplate ("/templates/cpp/DumpVarDeclarations.template", aOpts);
    }
    genCodeLine (/* { */ "};");

    switchToStaticsFile ();
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String fileName = Options.getOutputDirectory () + File.separator + s_tokMgrClassName + getFileExtension ();
    saveOutput (fileName);
  }

  private void _dumpBoilerPlateInHeader ()
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();

    switchToIncludeFile ();
    genCodeLine ("#ifndef JAVACC_CHARSTREAM");
    genCodeLine ("#define JAVACC_CHARSTREAM CharStream");
    genCodeLine ("#endif");
    genCodeNewLine ();

    genCodeLine ("private:");
    genCodeLine ("  void ReInitRounds();");
    genCodeNewLine ();
    genCodeLine ("public:");
    genCodeLine ("  " + s_tokMgrClassName + "(JAVACC_CHARSTREAM *stream, int lexState = " + s_defaultLexState + ");");
    genCodeLine ("  virtual ~" + s_tokMgrClassName + "();");
    genCodeLine ("  void ReInit(JAVACC_CHARSTREAM *stream, int lexState = " + s_defaultLexState + ");");
    genCodeLine ("  void SwitchTo(int lexState);");
    genCodeLine ("  void clear();");
    genCodeLine ("  const JJSimpleString jjKindsForBitVector(int i, " + eOutputLanguage.getTypeLong () + " vec);");
    genCodeLine ("  const JJSimpleString jjKindsForStateVector(int lexState, int vec[], int start, int end);");
    genCodeNewLine ();
  }

  private void _dumpStaticVarDeclarations ()
  {
    int i;

    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switchToStaticsFile (); // remaining variables
    genCodeNewLine ();
    genCodeLine ("/** Lexer state names. */");
    genStringLiteralArrayCPP ("lexStateNames", s_lexStateName);

    if (s_maxLexStates > 1)
    {
      genCodeNewLine ();
      genCodeLine ("/** Lex State array. */");
      genCode ("static const int jjnewLexState[] = {");

      for (i = 0; i < s_maxOrdinal; i++)
      {
        if (i % 25 == 0)
          genCode ("\n   ");

        if (s_newLexState[i] == null)
          genCode ("-1, ");
        else
          genCode (_getIndex (s_newLexState[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (s_hasSkip || s_hasMore || s_hasSpecial)
    {
      // Bit vector for TOKEN
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoToken[] = {");
      for (i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toToken[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (s_hasSkip || s_hasSpecial)
    {
      // Bit vector for SKIP
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoSkip[] = {");
      for (i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toSkip[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (s_hasSpecial)
    {
      // Bit vector for SPECIAL
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoSpecial[] = {");
      for (i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toSpecial[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (false)
      if (s_hasMore) // Not needed as we just use else
      {
        // Bit vector for MORE
        genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoMore[] = {");
        for (i = 0; i < s_maxOrdinal / 64 + 1; i++)
        {
          if (i % 4 == 0)
            genCode ("\n   ");
          genCode (eOutputLanguage.getLongHex (s_toMore[i]) + ", ");
        }
        genCodeLine ("\n};");
      }
  }

  private void _dumpFillToken ()
  {
    final double tokenVersion = OutputHelper.getVersionDashStar ("Token.java");
    final boolean hasBinaryNewToken = tokenVersion > 4.09;

    generateMethodDefHeader ("Token *", s_tokMgrClassName, "jjFillToken()");
    genCodeLine ("{");
    genCodeLine ("   Token *t;");
    genCodeLine ("   JJString curTokenImage;");
    if (s_keepLineCol)
    {
      genCodeLine ("   int beginLine   = -1;");
      genCodeLine ("   int endLine     = -1;");
      genCodeLine ("   int beginColumn = -1;");
      genCodeLine ("   int endColumn   = -1;");
    }

    if (s_hasEmptyMatch)
    {
      genCodeLine ("   if (jjmatchedPos < 0)");
      genCodeLine ("   {");
      genCodeLine ("       curTokenImage = image.c_str();");

      if (s_keepLineCol)
      {
        genCodeLine ("   if (input_stream->getTrackLineColumn()) {");
        genCodeLine ("      beginLine = endLine = input_stream->getEndLine();");
        genCodeLine ("      beginColumn = endColumn = input_stream->getEndColumn();");
        genCodeLine ("   }");
      }

      genCodeLine ("   }");
      genCodeLine ("   else");
      genCodeLine ("   {");
      genCodeLine ("      JJString im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine ("      curTokenImage = (im.length() == 0) ? input_stream->GetImage() : im;");

      if (s_keepLineCol)
      {
        genCodeLine ("   if (input_stream->getTrackLineColumn()) {");
        genCodeLine ("      beginLine = input_stream->getBeginLine();");
        genCodeLine ("      beginColumn = input_stream->getBeginColumn();");
        genCodeLine ("      endLine = input_stream->getEndLine();");
        genCodeLine ("      endColumn = input_stream->getEndColumn();");
        genCodeLine ("   }");
      }

      genCodeLine ("   }");
    }
    else
    {
      genCodeLine ("   JJString im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine ("   curTokenImage = (im.length() == 0) ? input_stream->GetImage() : im;");
      if (s_keepLineCol)
      {
        genCodeLine ("   if (input_stream->getTrackLineColumn()) {");
        genCodeLine ("     beginLine = input_stream->getBeginLine();");
        genCodeLine ("     beginColumn = input_stream->getBeginColumn();");
        genCodeLine ("     endLine = input_stream->getEndLine();");
        genCodeLine ("     endColumn = input_stream->getEndColumn();");
        genCodeLine ("   }");
      }
    }

    if (Options.getTokenFactory ().length () > 0)
    {
      genCodeLine ("   t = " + getClassQualifier (Options.getTokenFactory ()) + "newToken(jjmatchedKind, curTokenImage);");
    }
    else
      if (hasBinaryNewToken)
      {
        genCodeLine ("   t = " + getClassQualifier ("Token") + "newToken(jjmatchedKind, curTokenImage);");
      }
      else
      {
        genCodeLine ("   t = " + getClassQualifier ("Token") + "newToken(jjmatchedKind);");
        genCodeLine ("   t->kind = jjmatchedKind;");
        genCodeLine ("   t->image = curTokenImage;");
      }
    genCodeLine ("   t->specialToken = nullptr;");
    genCodeLine ("   t->next = nullptr;");

    if (s_keepLineCol)
    {
      genCodeNewLine ();
      genCodeLine ("   if (input_stream->getTrackLineColumn()) {");
      genCodeLine ("   t->beginLine = beginLine;");
      genCodeLine ("   t->endLine = endLine;");
      genCodeLine ("   t->beginColumn = beginColumn;");
      genCodeLine ("   t->endColumn = endColumn;");
      genCodeLine ("   }");
    }

    genCodeNewLine ();
    genCodeLine ("   return t;");
    genCodeLine ("}");
  }

  private void _dumpGetNextToken ()
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();

    switchToIncludeFile ();
    genCodeNewLine ();
    genCodeLine ("public:");
    genCodeLine ("    int curLexState;");
    genCodeLine ("    int jjnewStateCnt;");
    genCodeLine ("    int jjround;");
    genCodeLine ("    int jjmatchedPos;");
    genCodeLine ("    int jjmatchedKind;");
    genCodeNewLine ();
    switchToMainFile ();
    genCodeLine ("const int defaultLexState = " + s_defaultLexState + ";");
    genCodeLine ("/** Get the next Token. */");
    generateMethodDefHeader ("Token *", s_tokMgrClassName, "getNextToken()");
    genCodeLine ("{");
    if (s_hasSpecial)
    {
      genCodeLine ("  Token *specialToken = nullptr;");
    }
    genCodeLine ("  Token *matchedToken = nullptr;");
    genCodeLine ("  int curPos = 0;");
    genCodeNewLine ();
    genCodeLine ("  for (;;)");
    genCodeLine ("  {");
    genCodeLine ("   EOFLoop: ");
    // genCodeLine(" {");
    // genCodeLine(" curChar = input_stream->BeginToken();");
    // genCodeLine(" }");
    genCodeLine ("   if (input_stream->endOfInput())");
    genCodeLine ("   {");
    // genCodeLine(" input_stream->backup(1);");

    if (Options.isDebugTokenManager ())
      genCodeLine ("      fprintf(debugStream, \"Returning the <EOF> token.\\n\");");

    genCodeLine ("      jjmatchedKind = 0;");
    genCodeLine ("      jjmatchedPos = -1;");
    genCodeLine ("      matchedToken = jjFillToken();");

    if (s_hasSpecial)
      genCodeLine ("      matchedToken->specialToken = specialToken;");

    if (s_sNextStateForEof != null || s_aActForEof != null)
      genCodeLine ("      TokenLexicalActions(matchedToken);");

    if (Options.isCommonTokenAction ())
      genCodeLine ("      CommonTokenAction(matchedToken);");

    genCodeLine ("      return matchedToken;");
    genCodeLine ("   }");
    genCodeLine ("   curChar = input_stream->BeginToken();");

    if (s_hasMoreActions || s_hasSkipActions || s_hasTokenActions)
    {
      genCodeLine ("   image = jjimage;");
      genCodeLine ("   image.clear();");
      genCodeLine ("   jjimageLen = 0;");
    }

    genCodeNewLine ();

    String prefix = "";
    if (s_hasMore)
    {
      genCodeLine ("   for (;;)");
      genCodeLine ("   {");
      prefix = "  ";
    }

    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (s_maxLexStates > 1)
    {
      genCodeLine (prefix + "   switch(curLexState)");
      genCodeLine (prefix + "   {");
      endSwitch = prefix + "   }";
      caseStr = prefix + "     case ";
      prefix += "    ";
    }

    prefix += "   ";
    for (int i = 0; i < s_maxLexStates; i++)
    {
      if (s_maxLexStates > 1)
        genCodeLine (caseStr + i + ":");

      if (s_singlesToSkip[i].hasTransitions ())
      {
        // added the backup(0) to make JIT happy
        genCodeLine (prefix + "{ input_stream->backup(0);");
        if (s_singlesToSkip[i].m_asciiMoves[0] != 0L && s_singlesToSkip[i].m_asciiMoves[1] != 0L)
        {
          genCodeLine (prefix +
                       "   while ((curChar < 64" +
                       " && (" +
                       eOutputLanguage.getLongHex (s_singlesToSkip[i].m_asciiMoves[0]) +
                       " & (1L << curChar)) != 0L) || \n" +
                       prefix +
                       "          (curChar >> 6) == 1" +
                       " && (" +
                       eOutputLanguage.getLongHex (s_singlesToSkip[i].m_asciiMoves[1]) +
                       " & (1L << (curChar & 077))) != " +
                       eOutputLanguage.getLongPlain (0) +
                       ")");
        }
        else
          if (s_singlesToSkip[i].m_asciiMoves[1] == 0L)
          {
            genCodeLine (prefix +
                         "   while (curChar <= " +
                         (int) maxChar (s_singlesToSkip[i].m_asciiMoves[0]) +
                         " && (" +
                         eOutputLanguage.getLongHex (s_singlesToSkip[i].m_asciiMoves[0]) +
                         " & (1L << curChar)) != " +
                         eOutputLanguage.getLongPlain (0) +
                         ")");
          }
          else
            if (s_singlesToSkip[i].m_asciiMoves[0] == 0L)
            {
              genCodeLine (prefix +
                           "   while (curChar > 63 && curChar <= " +
                           (maxChar (s_singlesToSkip[i].m_asciiMoves[1]) + 64) +
                           " && (" +
                           eOutputLanguage.getLongHex (s_singlesToSkip[i].m_asciiMoves[1]) +
                           " & (1L << (curChar & 077))) != " +
                           eOutputLanguage.getLongPlain (0) +
                           ")");
            }

        genCodeLine (prefix + "{");
        if (Options.isDebugTokenManager ())
        {
          if (s_maxLexStates > 1)
          {
            genCodeLine ("      fprintf(debugStream, \"<%s>\" , addUnicodeEscapes(lexStateNames[curLexState]).c_str());");
          }

          genCodeLine ("      fprintf(debugStream, \"Skipping character : %c(%d)\\n\", curChar, (int)curChar);");
        }

        genCodeLine (prefix + "if (input_stream->endOfInput()) { goto EOFLoop; }");
        genCodeLine (prefix + "curChar = input_stream->BeginToken();");
        genCodeLine (prefix + "}");
        genCodeLine (prefix + "}");
      }

      if (s_initMatch[i] != Integer.MAX_VALUE && s_initMatch[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      fprintf(debugStream, \"   Matched the empty string as %s token.\\n\", addUnicodeEscapes(tokenImage[" +
                       s_initMatch[i] +
                       "]).c_str());");

        genCodeLine (prefix + "jjmatchedKind = " + s_initMatch[i] + ";");
        genCodeLine (prefix + "jjmatchedPos = -1;");
        genCodeLine (prefix + "curPos = 0;");
      }
      else
      {
        genCodeLine (prefix + "jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");
        genCodeLine (prefix + "jjmatchedPos = 0;");
      }

      if (Options.isDebugTokenManager ())
      {
        genCodeLine ("   fprintf(debugStream, " +
                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                     "input_stream->getEndLine(), input_stream->getEndColumn());");
      }

      genCodeLine (prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");

      if (s_canMatchAnyChar[i] != -1)
      {
        if (s_initMatch[i] != Integer.MAX_VALUE && s_initMatch[i] != 0)
          genCodeLine (prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " + s_canMatchAnyChar[i] + "))");
        else
          genCodeLine (prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + s_canMatchAnyChar[i] + ")");
        genCodeLine (prefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           fprintf(debugStream, \"   Current character matched as a %s token.\\n\", addUnicodeEscapes(tokenImage[" +
                       s_canMatchAnyChar[i] +
                       "]).c_str());");
        }
        genCodeLine (prefix + "   jjmatchedKind = " + s_canMatchAnyChar[i] + ";");

        if (s_initMatch[i] != Integer.MAX_VALUE && s_initMatch[i] != 0)
          genCodeLine (prefix + "   jjmatchedPos = 0;");

        genCodeLine (prefix + "}");
      }

      if (s_maxLexStates > 1)
        genCodeLine (prefix + "break;");
    }

    if (s_maxLexStates > 1)
      genCodeLine (endSwitch);
    else
      if (s_maxLexStates == 0)
        genCodeLine ("       jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");

    if (s_maxLexStates > 1)
      prefix = "  ";
    else
      prefix = "";

    if (s_maxLexStates > 0)
    {
      genCodeLine (prefix + "   if (jjmatchedKind != 0x" + Integer.toHexString (Integer.MAX_VALUE) + ")");
      genCodeLine (prefix + "   {");
      genCodeLine (prefix + "      if (jjmatchedPos + 1 < curPos)");

      if (Options.isDebugTokenManager ())
      {
        genCodeLine (prefix + "      {");
        genCodeLine (prefix +
                     "         fprintf(debugStream, " +
                     "\"   Putting back %d characters into the input stream.\\n\", (curPos - jjmatchedPos - 1));");
      }

      genCodeLine (prefix + "         input_stream->backup(curPos - jjmatchedPos - 1);");

      if (Options.isDebugTokenManager ())
      {
        genCodeLine (prefix + "      }");
      }

      if (Options.isDebugTokenManager ())
      {
        genCodeLine ("    fprintf(debugStream, " +
                     "\"****** FOUND A %d(%s) MATCH (%s) ******\\n\", jjmatchedKind, addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str(), addUnicodeEscapes(input_stream->GetSuffix(jjmatchedPos + 1)).c_str());");
      }

      if (s_hasSkip || s_hasMore || s_hasSpecial)
      {
        genCodeLine (prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (prefix + "      {");
      }

      genCodeLine (prefix + "         matchedToken = jjFillToken();");

      if (s_hasSpecial)
        genCodeLine (prefix + "         matchedToken->specialToken = specialToken;");

      if (s_hasTokenActions)
        genCodeLine (prefix + "         TokenLexicalActions(matchedToken);");

      if (s_maxLexStates > 1)
      {
        genCodeLine ("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine (prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      if (Options.isCommonTokenAction ())
        genCodeLine (prefix + "         CommonTokenAction(matchedToken);");

      genCodeLine (prefix + "         return matchedToken;");

      if (s_hasSkip || s_hasMore || s_hasSpecial)
      {
        genCodeLine (prefix + "      }");

        if (s_hasSkip || s_hasSpecial)
        {
          if (s_hasMore)
          {
            genCodeLine (prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
          }
          else
            genCodeLine (prefix + "      else");

          genCodeLine (prefix + "      {");

          if (s_hasSpecial)
          {
            genCodeLine (prefix + "         if ((jjtoSpecial[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
            genCodeLine (prefix + "         {");

            genCodeLine (prefix + "            matchedToken = jjFillToken();");

            genCodeLine (prefix + "            if (specialToken == nullptr)");
            genCodeLine (prefix + "               specialToken = matchedToken;");
            genCodeLine (prefix + "            else");
            genCodeLine (prefix + "            {");
            genCodeLine (prefix + "               matchedToken->specialToken = specialToken;");
            genCodeLine (prefix + "               specialToken = (specialToken->next = matchedToken);");
            genCodeLine (prefix + "            }");

            if (s_hasSkipActions)
              genCodeLine (prefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (prefix + "         }");

            if (s_hasSkipActions)
            {
              genCodeLine (prefix + "         else");
              genCodeLine (prefix + "            SkipLexicalActions(nullptr);");
            }
          }
          else
            if (s_hasSkipActions)
              genCodeLine (prefix + "         SkipLexicalActions(nullptr);");

          if (s_maxLexStates > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (prefix + "         goto EOFLoop;");
          genCodeLine (prefix + "      }");
        }

        if (s_hasMore)
        {
          if (s_hasMoreActions)
            genCodeLine (prefix + "      MoreLexicalActions();");
          else
            if (s_hasSkipActions || s_hasTokenActions)
              genCodeLine (prefix + "      jjimageLen += jjmatchedPos + 1;");

          if (s_maxLexStates > 1)
          {
            genCodeLine ("      if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (prefix + "      curLexState = jjnewLexState[jjmatchedKind];");
          }
          genCodeLine (prefix + "      curPos = 0;");
          genCodeLine (prefix + "      jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");

          genCodeLine (prefix + "   if (!input_stream->endOfInput()) {");
          genCodeLine (prefix + "         curChar = input_stream->readChar();");

          if (Options.isDebugTokenManager ())
          {
            genCodeLine ("   fprintf(debugStream, " +
                         "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                         "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                         "input_stream->getEndLine(), input_stream->getEndColumn());");
          }
          genCodeLine (prefix + "   continue;");
          genCodeLine (prefix + " }");
        }
      }

      genCodeLine (prefix + "   }");
      genCodeLine (prefix + "   int error_line = input_stream->getEndLine();");
      genCodeLine (prefix + "   int error_column = input_stream->getEndColumn();");
      genCodeLine (prefix + "   JJString error_after;");
      genCodeLine (prefix + "   bool EOFSeen = false;");
      genCodeLine (prefix + "   if (input_stream->endOfInput()) {");
      genCodeLine (prefix + "      EOFSeen = true;");
      genCodeLine (prefix + "      error_after = curPos <= 1 ? EMPTY : input_stream->GetImage();");
      genCodeLine (prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine (prefix + "         error_line++;");
      genCodeLine (prefix + "         error_column = 0;");
      genCodeLine (prefix + "      }");
      genCodeLine (prefix + "      else");
      genCodeLine (prefix + "         error_column++;");
      genCodeLine (prefix + "   }");
      genCodeLine (prefix + "   if (!EOFSeen) {");
      genCodeLine (prefix + "      error_after = curPos <= 1 ? EMPTY : input_stream->GetImage();");
      genCodeLine (prefix + "   }");
      genCodeLine (prefix + "   errorHandler->lexicalError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, this);");
    }

    if (s_hasMore)
      genCodeLine (prefix + " }");

    genCodeLine ("  }");
    genCodeLine ("}");
    genCodeNewLine ();
  }

  private void _dumpSkipActions ()
  {
    ExpAction act;

    generateMethodDefHeader ("void ", s_tokMgrClassName, "SkipLexicalActions(Token *matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < s_maxOrdinal; i++)
    {
      if ((s_toSkip[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((act = s_actions[i]) == null || act.getActionTokens () == null || act.getActionTokens ().size () == 0) &&
            !s_canLoop[s_lexStates[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (s_initMatch[s_lexStates[i]] == i && s_canLoop[s_lexStates[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + s_lexStates[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + s_lexStates[i] + "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + s_lexStates[i] + "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine ("            jjemptyLineNo[" + s_lexStates[i] + "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + s_lexStates[i] + "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + s_lexStates[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((act = s_actions[i]) == null || act.getActionTokens ().size () == 0)
          break;

        genCode ("         image.append");
        if (ExpRStringLiteral.s_allImages[i] != null)
        {
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
          genCodeLine ("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
        }
        else
        {
          genCodeLine ("(input_stream->GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
        }

        printTokenSetup (act.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : act.getActionTokens ())
          printToken (t);
        genCodeNewLine ();

        break;
      }

      genCodeLine ("         break;");
      genCodeLine ("       }");
    }

    genCodeLine ("      default :");
    genCodeLine ("         break;");
    genCodeLine ("   }");
    genCodeLine ("}");
  }

  private void _dumpMoreActions ()
  {
    ExpAction act;

    generateMethodDefHeader ("void ", s_tokMgrClassName, "MoreLexicalActions()");
    genCodeLine ("{");
    genCodeLine ("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < s_maxOrdinal; i++)
    {
      if ((s_toMore[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((act = s_actions[i]) == null || act.getActionTokens () == null || act.getActionTokens ().size () == 0) &&
            !s_canLoop[s_lexStates[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (s_initMatch[s_lexStates[i]] == i && s_canLoop[s_lexStates[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + s_lexStates[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + s_lexStates[i] + "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + s_lexStates[i] + "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine ("            jjemptyLineNo[" + s_lexStates[i] + "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + s_lexStates[i] + "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + s_lexStates[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((act = s_actions[i]) == null || act.getActionTokens ().size () == 0)
        {
          break;
        }

        genCode ("         image.append");

        if (ExpRStringLiteral.s_allImages[i] != null)
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
        else
          genCodeLine ("(input_stream->GetSuffix(jjimageLen));");

        genCodeLine ("         jjimageLen = 0;");
        printTokenSetup (act.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : act.getActionTokens ())
          printToken (t);
        genCodeNewLine ();

        break;
      }

      genCodeLine ("         break;");
      genCodeLine ("       }");
    }

    genCodeLine ("      default :");
    genCodeLine ("         break;");

    genCodeLine ("   }");
    genCodeLine ("}");
  }

  private void _dumpTokenActions ()
  {
    ExpAction act;
    int i;

    generateMethodDefHeader ("void ", s_tokMgrClassName, "TokenLexicalActions(Token *matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (i = 0; i < s_maxOrdinal; i++)
    {
      if ((s_toToken[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((act = s_actions[i]) == null || act.getActionTokens () == null || act.getActionTokens ().size () == 0) &&
            !s_canLoop[s_lexStates[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (s_initMatch[s_lexStates[i]] == i && s_canLoop[s_lexStates[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + s_lexStates[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + s_lexStates[i] + "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + s_lexStates[i] + "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream->getBeginLine() + \", " +
                       "column \" + input_stream->getBeginColumn() + \".\"), this);");
          genCodeLine ("            jjemptyLineNo[" + s_lexStates[i] + "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + s_lexStates[i] + "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + s_lexStates[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((act = s_actions[i]) == null || act.getActionTokens ().size () == 0)
          break;

        if (i == 0)
        {
          genCodeLine ("      image.setLength(0);"); // For EOF no image is
                                                     // there
        }
        else
        {
          genCode ("        image.append");

          if (ExpRStringLiteral.s_allImages[i] != null)
          {
            genCodeLine ("(jjstrLiteralImages[" + i + "]);");
            genCodeLine ("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
          }
          else
          {
            genCodeLine ("(input_stream->GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
          }
        }

        printTokenSetup (act.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : act.getActionTokens ())
          printToken (t);
        genCodeNewLine ();

        break;
      }

      genCodeLine ("         break;");
      genCodeLine ("       }");
    }

    genCodeLine ("      default :");
    genCodeLine ("         break;");
    genCodeLine ("   }");
    genCodeLine ("}");
  }
}
