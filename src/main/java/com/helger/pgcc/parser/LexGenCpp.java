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

import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;

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
    final List <String> tn = new ArrayList <> (grammar ().getToolNameList ());
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
    genCodeLine ("#include \"" + grammar ().getParserName () + "Constants.h\"");

    if (Options.stringValue (Options.USEROPTION__CPP_TOKEN_MANAGER_INCLUDES).length () > 0)
    {
      genCodeLine ("#include \"" + Options.stringValue (Options.USEROPTION__CPP_TOKEN_MANAGER_INCLUDES) + "\"\n");
    }

    genCodeNewLine ();

    if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
    {
      genCodeLine ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
    }

    genCodeLine ("class " + grammar ().getParserName () + ";");

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
                   lexer ().getTokenMgrClassName (),
                   new String [] {},
                   new String [] { "public TokenManager" + (superClass == null ? "" : ", public " + superClass) });

    if (grammar ().getTokenMgrDecls () != null && grammar ().getTokenMgrDecls ().isNotEmpty ())
    {
      Token t = grammar ().getTokenMgrDecls ().get (0);
      boolean bCommonTokenActionSeen = false;
      final boolean bCommonTokenActionNeeded = Options.isCommonTokenAction ();

      printTokenSetup (grammar ().getTokenMgrDecls ().get (0));
      setColToStart ();

      switchToMainFile ();
      for (final Token s_token_mgr_decl : grammar ().getTokenMgrDecls ())
      {
        t = s_token_mgr_decl;
        if (t.kind == JavaCCParserConstants.IDENTIFIER && bCommonTokenActionNeeded && !bCommonTokenActionSeen)
        {
          bCommonTokenActionSeen = t.image.equals ("CommonTokenAction");
          if (bCommonTokenActionSeen)
            t.image = grammar ().getParserName () + "TokenManager::" + t.image;
        }

        printToken (t);
      }

      switchToIncludeFile ();
      genCodeLine ("  void CommonTokenAction(Token* token);");

      if (Options.isTokenManagerUsesParser ())
      {
        genCodeLine ("  void setParser(void* parser) {");
        genCodeLine ("      this->parser = (" + grammar ().getParserName () + "*) parser;");
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

    generateMethodDefHeader ("  void ", lexer ().getTokenMgrClassName (), "setDebugStream(FILE *ds)");
    genCodeLine ("{ debugStream = ds; }");

    switchToIncludeFile ();
    if (Options.isTokenManagerUsesParser ())
    {
      genCodeNewLine ();
      genCodeLine ("private:");
      genCodeLine ("  " + grammar ().getParserName () + "* parser = nullptr;");
    }
    switchToMainFile ();
  }

  private void _dumpDebugMethods () throws IOException
  {
    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("maxOrdinal", Integer.toString (lexer ().getMaxOrdinal ()));
    aOpts.put ("stateSetSize", Integer.toString (lexer ().getStateSetSize ()));
    writeTemplate ("/templates/cpp/DumpDebugMethods.template", aOpts);
  }

  private static void _buildLexStatesTable ()
  {
    final Iterator <TokenProduction> it = grammar ().rexprList ().iterator ();
    TokenProduction tp;
    int i;

    final String [] tmpLexStateName = new String [grammar ().lexStateI2S ().size ()];
    while (it.hasNext ())
    {
      tp = it.next ();
      final List <RegExprSpec> respecs = tp.m_respecs;
      List <TokenProduction> tps;

      for (i = 0; i < tp.m_lexStates.length; i++)
      {
        tps = lexer ().allTpsForState ().get (tp.m_lexStates[i]);
        if (tps == null)
        {
          tmpLexStateName[lexer ().getMaxLexStates ()] = tp.m_lexStates[i];
          lexer ().setMaxLexStates (lexer ().getMaxLexStates () + 1);
          tps = new ArrayList <> ();
          lexer ().allTpsForState ().put (tp.m_lexStates[i], tps);
        }

        tps.add (tp);
      }

      if (respecs == null || respecs.size () == 0)
        continue;

      for (i = 0; i < respecs.size (); i++)
      {
        final AbstractExpRegularExpression re = respecs.get (i).rexp;
        if (lexer ().getMaxOrdinal () <= re.getOrdinal ())
          lexer ().setMaxOrdinal (re.getOrdinal () + 1);
      }
    }

    lexer ().setKinds (new ETokenKind [lexer ().getMaxOrdinal ()]);
    lexer ().setToSkip (new long [lexer ().getMaxOrdinal () / 64 + 1]);
    lexer ().setToSpecial (new long [lexer ().getMaxOrdinal () / 64 + 1]);
    lexer ().setToMore (new long [lexer ().getMaxOrdinal () / 64 + 1]);
    lexer ().setToToken (new long [lexer ().getMaxOrdinal () / 64 + 1]);
    lexer ().getToToken ()[0] = 1L;
    lexer ().setActions (new ExpAction [lexer ().getMaxOrdinal ()]);
    lexer ().getActions ()[0] = grammar ().getActionForEof ();
    lexer ().setHasTokenActions (grammar ().getActionForEof () != null);
    lexer ().initStates ().clear ();
    lexer ().setCanMatchAnyChar (new int [lexer ().getMaxLexStates ()]);
    lexer ().setCanLoop (new boolean [lexer ().getMaxLexStates ()]);
    lexer ().setStateHasActions (new boolean [lexer ().getMaxLexStates ()]);
    lexer ().setLexStateName (new String [lexer ().getMaxLexStates ()]);
    lexer ().setSinglesToSkip (new NfaState [lexer ().getMaxLexStates ()]);
    System.arraycopy (tmpLexStateName, 0, lexer ().getLexStateName (), 0, lexer ().getMaxLexStates ());

    for (i = 0; i < lexer ().getMaxLexStates (); i++)
      lexer ().getCanMatchAnyChar ()[i] = -1;

    lexer ().setHasNfa (new boolean [lexer ().getMaxLexStates ()]);
    lexer ().setMixed (new boolean [lexer ().getMaxLexStates ()]);
    lexer ().setMaxLongsReqd (new int [lexer ().getMaxLexStates ()]);
    lexer ().setInitMatch (new int [lexer ().getMaxLexStates ()]);
    lexer ().setNewLexState (new String [lexer ().getMaxOrdinal ()]);
    lexer ().getNewLexState ()[0] = grammar ().getNextStateForEof ();
    lexer ().setHasEmptyMatch (false);
    lexer ().setLexStates (new int [lexer ().getMaxOrdinal ()]);
    lexer ().setIgnoreCase (new boolean [lexer ().getMaxOrdinal ()]);
    lexer ().setRexprs (new AbstractExpRegularExpression [lexer ().getMaxOrdinal ()]);
    ExpRStringLiteral.strLit ().setAllImages (new String [lexer ().getMaxOrdinal ()]);
    lexer ().setCanReachOnMore (new boolean [lexer ().getMaxLexStates ()]);
  }

  private static int _getIndex (final String name)
  {
    for (int i = 0; i < lexer ().getLexStateName ().length; i++)
      if (lexer ().getLexStateName ()[i] != null && lexer ().getLexStateName ()[i].equals (name))
        return i;

    throw new IllegalStateException ("Should never come here");
  }

  @Override
  public void start () throws IOException
  {
    if (!Options.isBuildTokenManager () || Options.isUserTokenManager () || JavaCCErrors.getErrorCount () > 0)
      return;

    lexer ().setKeepLineCol (Options.isKeepLineColumn ());
    final List <ExpRChoice> choices = new ArrayList <> ();

    lexer ().setTokenMgrClassName (grammar ().getParserName () + "TokenManager");

    _printClassHead ();
    _buildLexStatesTable ();

    boolean ignoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : lexer ().allTpsForState ().entrySet ())
    {
      NfaState.reInitStatic ();
      ExpRStringLiteral.reInitStatic ();

      final String key = aEntry.getKey ();

      lexer ().setLexStateIndex (_getIndex (key));
      lexer ().setLexStateSuffix ("_" + lexer ().getLexStateIndex ());
      final List <TokenProduction> allTps = aEntry.getValue ();
      lexer ().setInitialState (new NfaState ());
      lexer ().initStates ().put (key, lexer ().getInitialState ());
      ignoring = false;

      lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()] = new NfaState ();
      lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].m_dummy = true;

      if (key.equals ("DEFAULT"))
        lexer ().setDefaultLexState (lexer ().getLexStateIndex ());

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
          lexer ().setCurRE (respec.rexp);

          lexer ().setCurKind (lexer ().getCurRE ().getOrdinal ());
          lexer ().getRexprs ()[lexer ().getCurKind ()] = lexer ().getCurRE ();
          lexer ().getLexStates ()[lexer ().getCurRE ().getOrdinal ()] = lexer ().getLexStateIndex ();
          lexer ().getIgnoreCase ()[lexer ().getCurRE ().getOrdinal ()] = ignore;

          if (lexer ().getCurRE ().m_bPrivateRexp)
          {
            lexer ().getKinds ()[lexer ().getCurRE ().getOrdinal ()] = null;
            continue;
          }

          if (lexer ().getCurRE () instanceof ExpRStringLiteral && StringHelper.isNotEmpty (((ExpRStringLiteral) lexer ().getCurRE ()).m_image))
          {
            ((ExpRStringLiteral) lexer ().getCurRE ()).generateDfa ();
            if (i != 0 && !lexer ().getMixed ()[lexer ().getLexStateIndex ()] && ignoring != ignore)
              lexer ().getMixed ()[lexer ().getLexStateIndex ()] = true;
          }
          else
            if (lexer ().getCurRE ().canMatchAnyChar ())
            {
              if (lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()] == -1 || lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()] > lexer ().getCurRE ().getOrdinal ())
                lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()] = lexer ().getCurRE ().getOrdinal ();
            }
            else
            {
              Nfa temp;

              if (lexer ().getCurRE () instanceof ExpRChoice)
                choices.add ((ExpRChoice) lexer ().getCurRE ());

              temp = lexer ().getCurRE ().generateNfa (ignore);
              temp.end ().m_isFinal = true;
              temp.end ().m_kind = lexer ().getCurRE ().getOrdinal ();
              lexer ().getInitialState ().addMove (temp.start ());
            }

          if (lexer ().getKinds ().length < lexer ().getCurRE ().getOrdinal ())
          {
            final ETokenKind [] tmp = new ETokenKind [lexer ().getCurRE ().getOrdinal () + 1];

            System.arraycopy (lexer ().getKinds (), 0, tmp, 0, lexer ().getKinds ().length);
            lexer ().setKinds (tmp);
          }
          // System.out.println(" ordina : " + curRE.ordinal);

          lexer ().getKinds ()[lexer ().getCurRE ().getOrdinal ()] = kind;

          if (respec.nextState != null && !respec.nextState.equals (lexer ().getLexStateName ()[lexer ().getLexStateIndex ()]))
            lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] = respec.nextState;

          if (respec.act != null && respec.act.getActionTokens () != null && respec.act.getActionTokens ().size () > 0)
            lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] = respec.act;

          switch (kind)
          {
            case SPECIAL:
              lexer ().setHasSkipActions (lexer ().isHasSkipActions () | (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null) || (lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().setHasSpecial (true);
              lexer ().getToSpecial ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L << (lexer ().getCurRE ().getOrdinal () % 64);
              lexer ().getToSkip ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L << (lexer ().getCurRE ().getOrdinal () % 64);
              break;
            case SKIP:
              lexer ().setHasSkipActions (lexer ().isHasSkipActions () | (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().setHasSkip (true);
              lexer ().getToSkip ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L << (lexer ().getCurRE ().getOrdinal () % 64);
              break;
            case MORE:
              lexer ().setHasMoreActions (lexer ().isHasMoreActions () | (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().setHasMore (true);
              lexer ().getToMore ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L << (lexer ().getCurRE ().getOrdinal () % 64);

              if (lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] != null)
                lexer ().getCanReachOnMore ()[_getIndex (lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()])] = true;
              else
                lexer ().getCanReachOnMore ()[lexer ().getLexStateIndex ()] = true;

              break;
            case TOKEN:
              lexer ().setHasTokenActions (lexer ().isHasTokenActions () | (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().getToToken ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L << (lexer ().getCurRE ().getOrdinal () % 64);
              break;
            default:
              throw new IllegalStateException ();
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      NfaState.computeClosures ();

      for (final NfaState aItem : lexer ().getInitialState ().m_epsilonMoves)
        aItem.generateCode ();

      lexer ().getHasNfa ()[lexer ().getLexStateIndex ()] = (NfaState.nfa ().getGeneratedStates () != 0);
      if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()])
      {
        lexer ().getInitialState ().generateCode ();
        lexer ().getInitialState ().generateInitMoves ();
      }

      if (lexer ().getInitialState ().m_kind != Integer.MAX_VALUE && lexer ().getInitialState ().m_kind != 0)
      {
        if ((lexer ().getToSkip ()[lexer ().getInitialState ().m_kind / 64] & (1L << lexer ().getInitialState ().m_kind)) != 0L ||
            (lexer ().getToSpecial ()[lexer ().getInitialState ().m_kind / 64] & (1L << lexer ().getInitialState ().m_kind)) != 0L)
          lexer ().setHasSkipActions (true);
        else
          if ((lexer ().getToMore ()[lexer ().getInitialState ().m_kind / 64] & (1L << lexer ().getInitialState ().m_kind)) != 0L)
            lexer ().setHasMoreActions (true);
          else
            lexer ().setHasTokenActions (true);

        if (lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] == 0 || lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] > lexer ().getInitialState ().m_kind)
        {
          lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] = lexer ().getInitialState ().m_kind;
          lexer ().setHasEmptyMatch (true);
        }
      }
      else
        if (lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] == 0)
          lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] = Integer.MAX_VALUE;

      ExpRStringLiteral.fillSubString ();

      if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()] && !lexer ().getMixed ()[lexer ().getLexStateIndex ()])
        ExpRStringLiteral.generateNfaStartStates (this, lexer ().getInitialState ());

      ExpRStringLiteral.dumpDfaCode (this);

      if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()])
        NfaState.dumpMoveNfa (this);

      if (lexer ().getStateSetSize () < NfaState.nfa ().getGeneratedStates ())
        lexer ().setStateSetSize (NfaState.nfa ().getGeneratedStates ());
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

    if (lexer ().isHasLoop ())
    {
      switchToStaticsFile ();
      genCodeLine ("static int  jjemptyLineNo[" + lexer ().getMaxLexStates () + "];");
      genCodeLine ("static int  jjemptyColNo[" + lexer ().getMaxLexStates () + "];");
      genCodeLine ("static bool jjbeenHere[" + lexer ().getMaxLexStates () + "];");
      switchToMainFile ();
    }

    if (lexer ().isHasSkipActions ())
      _dumpSkipActions ();
    if (lexer ().isHasMoreActions ())
      _dumpMoreActions ();
    if (lexer ().isHasTokenActions ())
      _dumpTokenActions ();

    NfaState.printBoilerPlateCPP (this);

    {
      final Map <String, Object> aOpts = new HashMap <> ();
      aOpts.put ("charStreamName", "CharStream");
      aOpts.put ("parserClassName", grammar ().getParserName ());
      aOpts.put ("defaultLexState", "defaultLexState");
      aOpts.put ("lexStateNameLength", Integer.toString (lexer ().getLexStateName ().length));
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
      aOpts.put ("lexStateNameLength", Integer.toString (lexer ().getLexStateName ().length));
      writeTemplate ("/templates/cpp/DumpVarDeclarations.template", aOpts);
    }
    genCodeLine (/* { */ "};");

    switchToStaticsFile ();
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String fileName = Options.getOutputDirectory () + File.separator + lexer ().getTokenMgrClassName () + getFileExtension ();
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
    genCodeLine ("  " + lexer ().getTokenMgrClassName () + "(JAVACC_CHARSTREAM *stream, int lexState = " + lexer ().getDefaultLexState () + ");");
    genCodeLine ("  virtual ~" + lexer ().getTokenMgrClassName () + "();");
    genCodeLine ("  void ReInit(JAVACC_CHARSTREAM *stream, int lexState = " + lexer ().getDefaultLexState () + ");");
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
    genStringLiteralArrayCPP ("lexStateNames", lexer ().getLexStateName ());

    if (lexer ().getMaxLexStates () > 1)
    {
      genCodeNewLine ();
      genCodeLine ("/** Lex State array. */");
      genCode ("static const int jjnewLexState[] = {");

      for (i = 0; i < lexer ().getMaxOrdinal (); i++)
      {
        if (i % 25 == 0)
          genCode ("\n   ");

        if (lexer ().getNewLexState ()[i] == null)
          genCode ("-1, ");
        else
          genCode (_getIndex (lexer ().getNewLexState ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (lexer ().isHasSkip () || lexer ().isHasMore () || lexer ().isHasSpecial ())
    {
      // Bit vector for TOKEN
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoToken[] = {");
      for (i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToToken ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (lexer ().isHasSkip () || lexer ().isHasSpecial ())
    {
      // Bit vector for SKIP
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoSkip[] = {");
      for (i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToSkip ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (lexer ().isHasSpecial ())
    {
      // Bit vector for SPECIAL
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoSpecial[] = {");
      for (i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToSpecial ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (false)
      if (lexer ().isHasMore ()) // Not needed as we just use else
      {
        // Bit vector for MORE
        genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoMore[] = {");
        for (i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
        {
          if (i % 4 == 0)
            genCode ("\n   ");
          genCode (eOutputLanguage.getLongHex (lexer ().getToMore ()[i]) + ", ");
        }
        genCodeLine ("\n};");
      }
  }

  private void _dumpFillToken ()
  {
    final double tokenVersion = OutputHelper.getVersionDashStar ("Token.java");
    final boolean hasBinaryNewToken = tokenVersion > 4.09;

    generateMethodDefHeader ("Token *", lexer ().getTokenMgrClassName (), "jjFillToken()");
    genCodeLine ("{");
    genCodeLine ("   Token *t;");
    genCodeLine ("   JJString curTokenImage;");
    if (lexer ().isKeepLineCol ())
    {
      genCodeLine ("   int beginLine   = -1;");
      genCodeLine ("   int endLine     = -1;");
      genCodeLine ("   int beginColumn = -1;");
      genCodeLine ("   int endColumn   = -1;");
    }

    if (lexer ().isHasEmptyMatch ())
    {
      genCodeLine ("   if (jjmatchedPos < 0)");
      genCodeLine ("   {");
      genCodeLine ("       curTokenImage = image.c_str();");

      if (lexer ().isKeepLineCol ())
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

      if (lexer ().isKeepLineCol ())
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
      if (lexer ().isKeepLineCol ())
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

    if (lexer ().isKeepLineCol ())
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
    genCodeLine ("const int defaultLexState = " + lexer ().getDefaultLexState () + ";");
    genCodeLine ("/** Get the next Token. */");
    generateMethodDefHeader ("Token *", lexer ().getTokenMgrClassName (), "getNextToken()");
    genCodeLine ("{");
    if (lexer ().isHasSpecial ())
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

    if (lexer ().isHasSpecial ())
      genCodeLine ("      matchedToken->specialToken = specialToken;");

    if (grammar ().getNextStateForEof () != null || grammar ().getActionForEof () != null)
      genCodeLine ("      TokenLexicalActions(matchedToken);");

    if (Options.isCommonTokenAction ())
      genCodeLine ("      CommonTokenAction(matchedToken);");

    genCodeLine ("      return matchedToken;");
    genCodeLine ("   }");
    genCodeLine ("   curChar = input_stream->BeginToken();");

    if (lexer ().isHasMoreActions () || lexer ().isHasSkipActions () || lexer ().isHasTokenActions ())
    {
      genCodeLine ("   image = jjimage;");
      genCodeLine ("   image.clear();");
      genCodeLine ("   jjimageLen = 0;");
    }

    genCodeNewLine ();

    String prefix = "";
    if (lexer ().isHasMore ())
    {
      genCodeLine ("   for (;;)");
      genCodeLine ("   {");
      prefix = "  ";
    }

    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (lexer ().getMaxLexStates () > 1)
    {
      genCodeLine (prefix + "   switch(curLexState)");
      genCodeLine (prefix + "   {");
      endSwitch = prefix + "   }";
      caseStr = prefix + "     case ";
      prefix += "    ";
    }

    prefix += "   ";
    for (int i = 0; i < lexer ().getMaxLexStates (); i++)
    {
      if (lexer ().getMaxLexStates () > 1)
        genCodeLine (caseStr + i + ":");

      if (lexer ().getSinglesToSkip ()[i].hasTransitions ())
      {
        // added the backup(0) to make JIT happy
        genCodeLine (prefix + "{ input_stream->backup(0);");
        if (lexer ().getSinglesToSkip ()[i].m_asciiMoves[0] != 0L && lexer ().getSinglesToSkip ()[i].m_asciiMoves[1] != 0L)
        {
          genCodeLine (prefix +
                       "   while ((curChar < 64" +
                       " && (" +
                       eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].m_asciiMoves[0]) +
                       " & (1L << curChar)) != 0L) || \n" +
                       prefix +
                       "          (curChar >> 6) == 1" +
                       " && (" +
                       eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].m_asciiMoves[1]) +
                       " & (1L << (curChar & 077))) != " +
                       eOutputLanguage.getLongPlain (0) +
                       ")");
        }
        else
          if (lexer ().getSinglesToSkip ()[i].m_asciiMoves[1] == 0L)
          {
            genCodeLine (prefix +
                         "   while (curChar <= " +
                         (int) maxChar (lexer ().getSinglesToSkip ()[i].m_asciiMoves[0]) +
                         " && (" +
                         eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].m_asciiMoves[0]) +
                         " & (1L << curChar)) != " +
                         eOutputLanguage.getLongPlain (0) +
                         ")");
          }
          else
            if (lexer ().getSinglesToSkip ()[i].m_asciiMoves[0] == 0L)
            {
              genCodeLine (prefix +
                           "   while (curChar > 63 && curChar <= " +
                           (maxChar (lexer ().getSinglesToSkip ()[i].m_asciiMoves[1]) + 64) +
                           " && (" +
                           eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].m_asciiMoves[1]) +
                           " & (1L << (curChar & 077))) != " +
                           eOutputLanguage.getLongPlain (0) +
                           ")");
            }

        genCodeLine (prefix + "{");
        if (Options.isDebugTokenManager ())
        {
          if (lexer ().getMaxLexStates () > 1)
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

      if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      fprintf(debugStream, \"   Matched the empty string as %s token.\\n\", addUnicodeEscapes(tokenImage[" +
                       lexer ().getInitMatch ()[i] +
                       "]).c_str());");

        genCodeLine (prefix + "jjmatchedKind = " + lexer ().getInitMatch ()[i] + ";");
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

      if (lexer ().getCanMatchAnyChar ()[i] != -1)
      {
        if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
          genCodeLine (prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " + lexer ().getCanMatchAnyChar ()[i] + "))");
        else
          genCodeLine (prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + lexer ().getCanMatchAnyChar ()[i] + ")");
        genCodeLine (prefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           fprintf(debugStream, \"   Current character matched as a %s token.\\n\", addUnicodeEscapes(tokenImage[" +
                       lexer ().getCanMatchAnyChar ()[i] +
                       "]).c_str());");
        }
        genCodeLine (prefix + "   jjmatchedKind = " + lexer ().getCanMatchAnyChar ()[i] + ";");

        if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
          genCodeLine (prefix + "   jjmatchedPos = 0;");

        genCodeLine (prefix + "}");
      }

      if (lexer ().getMaxLexStates () > 1)
        genCodeLine (prefix + "break;");
    }

    if (lexer ().getMaxLexStates () > 1)
      genCodeLine (endSwitch);
    else
      if (lexer ().getMaxLexStates () == 0)
        genCodeLine ("       jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");

    if (lexer ().getMaxLexStates () > 1)
      prefix = "  ";
    else
      prefix = "";

    if (lexer ().getMaxLexStates () > 0)
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

      if (lexer ().isHasSkip () || lexer ().isHasMore () || lexer ().isHasSpecial ())
      {
        genCodeLine (prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (prefix + "      {");
      }

      genCodeLine (prefix + "         matchedToken = jjFillToken();");

      if (lexer ().isHasSpecial ())
        genCodeLine (prefix + "         matchedToken->specialToken = specialToken;");

      if (lexer ().isHasTokenActions ())
        genCodeLine (prefix + "         TokenLexicalActions(matchedToken);");

      if (lexer ().getMaxLexStates () > 1)
      {
        genCodeLine ("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine (prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      if (Options.isCommonTokenAction ())
        genCodeLine (prefix + "         CommonTokenAction(matchedToken);");

      genCodeLine (prefix + "         return matchedToken;");

      if (lexer ().isHasSkip () || lexer ().isHasMore () || lexer ().isHasSpecial ())
      {
        genCodeLine (prefix + "      }");

        if (lexer ().isHasSkip () || lexer ().isHasSpecial ())
        {
          if (lexer ().isHasMore ())
          {
            genCodeLine (prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
          }
          else
            genCodeLine (prefix + "      else");

          genCodeLine (prefix + "      {");

          if (lexer ().isHasSpecial ())
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

            if (lexer ().isHasSkipActions ())
              genCodeLine (prefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (prefix + "         }");

            if (lexer ().isHasSkipActions ())
            {
              genCodeLine (prefix + "         else");
              genCodeLine (prefix + "            SkipLexicalActions(nullptr);");
            }
          }
          else
            if (lexer ().isHasSkipActions ())
              genCodeLine (prefix + "         SkipLexicalActions(nullptr);");

          if (lexer ().getMaxLexStates () > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (prefix + "         goto EOFLoop;");
          genCodeLine (prefix + "      }");
        }

        if (lexer ().isHasMore ())
        {
          if (lexer ().isHasMoreActions ())
            genCodeLine (prefix + "      MoreLexicalActions();");
          else
            if (lexer ().isHasSkipActions () || lexer ().isHasTokenActions ())
              genCodeLine (prefix + "      jjimageLen += jjmatchedPos + 1;");

          if (lexer ().getMaxLexStates () > 1)
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

    if (lexer ().isHasMore ())
      genCodeLine (prefix + " }");

    genCodeLine ("  }");
    genCodeLine ("}");
    genCodeNewLine ();
  }

  private void _dumpSkipActions ()
  {
    ExpAction act;

    generateMethodDefHeader ("void ", lexer ().getTokenMgrClassName (), "SkipLexicalActions(Token *matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((act = lexer ().getActions ()[i]) == null || act.getActionTokens () == null || act.getActionTokens ().size () == 0) &&
            !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i && lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + lexer ().getLexStates ()[i] + "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + lexer ().getLexStates ()[i] + "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((act = lexer ().getActions ()[i]) == null || act.getActionTokens ().size () == 0)
          break;

        genCode ("         image.append");
        if (ExpRStringLiteral.strLit ().getAllImages ()[i] != null)
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

    generateMethodDefHeader ("void ", lexer ().getTokenMgrClassName (), "MoreLexicalActions()");
    genCodeLine ("{");
    genCodeLine ("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToMore ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((act = lexer ().getActions ()[i]) == null || act.getActionTokens () == null || act.getActionTokens ().size () == 0) &&
            !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i && lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + lexer ().getLexStates ()[i] + "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + lexer ().getLexStates ()[i] + "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((act = lexer ().getActions ()[i]) == null || act.getActionTokens ().size () == 0)
        {
          break;
        }

        genCode ("         image.append");

        if (ExpRStringLiteral.strLit ().getAllImages ()[i] != null)
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

    generateMethodDefHeader ("void ", lexer ().getTokenMgrClassName (), "TokenLexicalActions(Token *matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((act = lexer ().getActions ()[i]) == null || act.getActionTokens () == null || act.getActionTokens ().size () == 0) &&
            !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i && lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + lexer ().getLexStates ()[i] + "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream->getBeginLine() + \", " +
                       "column \" + input_stream->getBeginColumn() + \".\"), this);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + lexer ().getLexStates ()[i] + "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((act = lexer ().getActions ()[i]) == null || act.getActionTokens ().size () == 0)
          break;

        if (i == 0)
        {
          genCodeLine ("      image.setLength(0);"); // For EOF no image is
                                                     // there
        }
        else
        {
          genCode ("        image.append");

          if (ExpRStringLiteral.strLit ().getAllImages ()[i] != null)
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
