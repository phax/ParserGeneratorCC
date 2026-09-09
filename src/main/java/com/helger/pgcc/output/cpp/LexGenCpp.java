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
package com.helger.pgcc.output.cpp;

import org.jspecify.annotations.NonNull;

import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.context.GrammarState;
import com.helger.pgcc.context.LexerState;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.OutputHelper;
import com.helger.pgcc.output.AbstractLexGenJavaLike;
import com.helger.pgcc.parser.ETokenKind;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCParserConstants;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.RegExprSpec;
import com.helger.pgcc.parser.Token;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;
import com.helger.pgcc.parser.exp.ExpRChoice;
import com.helger.pgcc.parser.exp.ExpRStringLiteral;

/**
 * Generate lexer.
 */
public class LexGenCpp extends AbstractLexGenJavaLike
{
  /** Default constructor. */
  public LexGenCpp ()
  {}

  /**
   * Emit a C++ string literal as a char array, which is how the generated C++ carries them.
   *
   * @param s
   *        The literal content. May not be <code>null</code>.
   */
  private void _genStringLiteralInCpp (@NonNull final String s)
  {
    final StringBuilder aSB = new StringBuilder (s.length () * 6 + 4);
    aSB.append ('{');
    for (final char c : s.toCharArray ())
      aSB.append ("0x").append (Integer.toHexString (c)).append (", ");
    aSB.append ("0}");
    genCode (aSB.toString ());
  }

  /**
   * Emit an array of C++ string literals: one char array per entry, then an array of pointers to
   * them.
   *
   * @param sVarName
   *        The name of the generated variable. May not be <code>null</code>.
   * @param aArr
   *        The literals. May not be <code>null</code>.
   */
  private void _genStringLiteralArrayInCpp (@NonNull final String sVarName, @NonNull final String [] aArr)
  {
    for (int i = 0; i < aArr.length; i++)
    {
      genCodeLine ("static const JJChar " + sVarName + "_arr_" + i + "[] = ");
      _genStringLiteralInCpp (aArr[i]);
      genCodeLine (";");
    }

    genCodeLine ("static const JJString " + sVarName + "[] = {");
    for (int i = 0; i < aArr.length; i++)
      genCodeLine (sVarName + "_arr_" + i + ", ");
    genCodeLine ("};");
  }

  private void _printClassHead ()
  {
    final GrammarState aGrammar = grammar ();
    final List <String> aTn = new ArrayList <> (aGrammar.getToolNameList ());
    aTn.add (CPG.APP_NAME);

    switchToStaticsFile ();

    // standard includes
    switchToIncludeFile ();
    genCodeLine ("#include \"stdio.h\"");
    genCodeLine ("#include \"JavaCC.h\"");
    genCodeLine ("#include \"CharStream.h\"");
    genCodeLine ("#include \"Token.h\"");
    genCodeLine ("#include \"ErrorHandler.h\"");
    genCodeLine ("#include \"TokenManager.h\"");
    genCodeLine ("#include \"" + aGrammar.getParserName () + "Constants.h\"");

    if (Options.stringValue (Options.USEROPTION__CPP_TOKEN_MANAGER_INCLUDES).length () > 0)
    {
      genCodeLine ("#include \"" + Options.stringValue (Options.USEROPTION__CPP_TOKEN_MANAGER_INCLUDES) + "\"\n");
    }

    genCodeNewLine ();

    if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
    {
      genCodeLine ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
    }

    genCodeLine ("class " + aGrammar.getParserName () + ";");

    /*
     * final int l = 0, kind; i = 1; namespace? for (;;) { if (cu_to_insertion_point_1.size() <= l)
     * break; kind = ((Token)cu_to_insertion_point_1.get(l)).kind; if(kind == PACKAGE || kind ==
     * IMPORT) { for (; i < cu_to_insertion_point_1.size(); i++) { kind =
     * ((Token)cu_to_insertion_point_1.get(i)).kind; if (kind == CLASS) { cline =
     * ((Token)(cu_to_insertion_point_1.get(l))).beginLine; ccol =
     * ((Token)(cu_to_insertion_point_1.get(l))).beginColumn; for (j = l; j < i; j++) {
     * printToken((Token)(cu_to_insertion_point_1.get(j))); } if (kind == SEMICOLON)
     * printToken((Token)(cu_to_insertion_point_1.get(j))); genCodeLine(""); break; } } l = ++i; }
     * else break; }
     */

    genCodeNewLine ();
    genCodeLine ("/** Token Manager. */");
    final String sSuperClass = Options.stringValue (Options.USEROPTION__TOKEN_MANAGER_SUPER_CLASS);
    genClassStart (null,
                   lexer ().getTokenMgrClassName (),
                   new String [] {},
                   new String [] { "public TokenManager" + (sSuperClass == null ? "" : ", public " + sSuperClass) });

    if (aGrammar.getTokenMgrDecls () != null && aGrammar.getTokenMgrDecls ().isNotEmpty ())
    {
      Token t = aGrammar.getTokenMgrDecls ().get (0);
      boolean bCommonTokenActionSeen = false;
      final boolean bCommonTokenActionNeeded = Options.isCommonTokenAction ();

      printTokenSetup (aGrammar.getTokenMgrDecls ().get (0));
      setColToStart ();

      switchToMainFile ();
      for (final Token s_token_mgr_decl : aGrammar.getTokenMgrDecls ())
      {
        t = s_token_mgr_decl;
        if (t.kind == JavaCCParserConstants.IDENTIFIER && bCommonTokenActionNeeded && !bCommonTokenActionSeen)
        {
          bCommonTokenActionSeen = t.image.equals ("CommonTokenAction");
          if (bCommonTokenActionSeen)
            t.image = aGrammar.getParserName () + "TokenManager::" + t.image;
        }

        printToken (t);
      }

      switchToIncludeFile ();
      genCodeLine ("  void CommonTokenAction(Token* token);");

      if (Options.isTokenManagerUsesParser ())
      {
        genCodeLine ("  void setParser(void* parser) {");
        genCodeLine ("      this->parser = (" + aGrammar.getParserName () + "*) parser;");
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
      genCodeLine ("  " + aGrammar.getParserName () + "* parser = nullptr;");
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
    final GrammarState aGrammar = grammar ();
    final Iterator <TokenProduction> aIt = aGrammar.rexprList ().iterator ();
    TokenProduction aTp;
    int i;

    final String [] aTmpLexStateName = new String [aGrammar.lexStateI2S ().size ()];
    final LexerState aLexer = lexer ();
    while (aIt.hasNext ())
    {
      aTp = aIt.next ();
      final List <RegExprSpec> aRespecs = aTp.getRespecs ();
      List <TokenProduction> aTps;

      for (i = 0; i < aTp.getLexStates ().length; i++)
      {
        aTps = aLexer.allTpsForState ().get (aTp.getLexStates ()[i]);
        if (aTps == null)
        {
          aTmpLexStateName[aLexer.getMaxLexStates ()] = aTp.getLexStates ()[i];
          aLexer.setMaxLexStates (aLexer.getMaxLexStates () + 1);
          aTps = new ArrayList <> ();
          aLexer.allTpsForState ().put (aTp.getLexStates ()[i], aTps);
        }

        aTps.add (aTp);
      }

      if (aRespecs == null || aRespecs.isEmpty ())
        continue;

      for (i = 0; i < aRespecs.size (); i++)
      {
        final AbstractExpRegularExpression aRe = aRespecs.get (i).getRexp ();
        if (aLexer.getMaxOrdinal () <= aRe.getOrdinal ())
          aLexer.setMaxOrdinal (aRe.getOrdinal () + 1);
      }
    }

    aLexer.setKinds (new ETokenKind [aLexer.getMaxOrdinal ()]);
    aLexer.setToSkip (new long [aLexer.getMaxOrdinal () / 64 + 1]);
    aLexer.setToSpecial (new long [aLexer.getMaxOrdinal () / 64 + 1]);
    aLexer.setToMore (new long [aLexer.getMaxOrdinal () / 64 + 1]);
    aLexer.setToToken (new long [aLexer.getMaxOrdinal () / 64 + 1]);
    aLexer.getToToken ()[0] = 1L;
    aLexer.setActions (new ExpAction [aLexer.getMaxOrdinal ()]);
    aLexer.getActions ()[0] = aGrammar.getActionForEof ();
    aLexer.setHasTokenActions (aGrammar.getActionForEof () != null);
    aLexer.initStates ().clear ();
    aLexer.setCanMatchAnyChar (new int [aLexer.getMaxLexStates ()]);
    aLexer.setCanLoop (new boolean [aLexer.getMaxLexStates ()]);
    aLexer.setLexStateName (new String [aLexer.getMaxLexStates ()]);
    aLexer.setSinglesToSkip (new NfaState [aLexer.getMaxLexStates ()]);
    System.arraycopy (aTmpLexStateName, 0, aLexer.getLexStateName (), 0, aLexer.getMaxLexStates ());

    for (i = 0; i < aLexer.getMaxLexStates (); i++)
      aLexer.getCanMatchAnyChar ()[i] = -1;

    aLexer.setHasNfa (new boolean [aLexer.getMaxLexStates ()]);
    aLexer.setMixed (new boolean [aLexer.getMaxLexStates ()]);
    aLexer.setMaxLongsReqd (new int [aLexer.getMaxLexStates ()]);
    aLexer.setInitMatch (new int [aLexer.getMaxLexStates ()]);
    aLexer.setNewLexState (new String [aLexer.getMaxOrdinal ()]);
    aLexer.getNewLexState ()[0] = aGrammar.getNextStateForEof ();
    aLexer.setHasEmptyMatch (false);
    aLexer.setLexStates (new int [aLexer.getMaxOrdinal ()]);
    aLexer.setIgnoreCase (new boolean [aLexer.getMaxOrdinal ()]);
    aLexer.setRexprs (new AbstractExpRegularExpression [aLexer.getMaxOrdinal ()]);
    ExpRStringLiteral.strLit ().setAllImages (new String [aLexer.getMaxOrdinal ()]);
    aLexer.setCanReachOnMore (new boolean [aLexer.getMaxLexStates ()]);
  }

  /**
   * Write the token manager.
   *
   * @throws IOException
   *         if the output cannot be written
   */
  public void start () throws IOException
  {
    if (!Options.isBuildTokenManager () || Options.isUserTokenManager () || JavaCCErrors.getErrorCount () > 0)
      return;

    final LexerState aLexer = lexer ();
    aLexer.setKeepLineCol (Options.isKeepLineColumn ());
    final List <ExpRChoice> aChoices = new ArrayList <> ();

    aLexer.setTokenMgrClassName (grammar ().getParserName () + "TokenManager");

    _printClassHead ();
    _buildLexStatesTable ();

    boolean bIgnoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : aLexer.allTpsForState ().entrySet ())
    {
      NfaState.reInitStatic ();
      ExpRStringLiteral.reInitStatic ();

      final String sKey = aEntry.getKey ();

      aLexer.setLexStateIndex (_getIndex (sKey));
      aLexer.setLexStateSuffix ("_" + aLexer.getLexStateIndex ());
      final List <TokenProduction> aAllTps = aEntry.getValue ();
      aLexer.setInitialState (new NfaState ());
      aLexer.initStates ().put (sKey, aLexer.getInitialState ());
      bIgnoring = false;

      aLexer.getSinglesToSkip ()[aLexer.getLexStateIndex ()] = new NfaState ();
      aLexer.getSinglesToSkip ()[aLexer.getLexStateIndex ()].setDummy (true);

      if (sKey.equals ("DEFAULT"))
        aLexer.setDefaultLexState (aLexer.getLexStateIndex ());

      for (int i = 0; i < aAllTps.size (); i++)
      {
        final TokenProduction aTp = aAllTps.get (i);
        final ETokenKind eKind = aTp.getKind ();
        final boolean bIgnore = aTp.isIgnoreCase ();
        final List <RegExprSpec> aRexps = aTp.getRespecs ();

        if (i == 0)
          bIgnoring = bIgnore;

        for (final RegExprSpec respec : aRexps)
        {
          aLexer.setCurRE (respec.getRexp ());

          aLexer.setCurKind (aLexer.getCurRE ().getOrdinal ());
          aLexer.getRexprs ()[aLexer.getCurKind ()] = aLexer.getCurRE ();
          aLexer.getLexStates ()[aLexer.getCurRE ().getOrdinal ()] = aLexer.getLexStateIndex ();
          aLexer.getIgnoreCase ()[aLexer.getCurRE ().getOrdinal ()] = bIgnore;

          if (aLexer.getCurRE ().m_bPrivateRexp)
          {
            aLexer.getKinds ()[aLexer.getCurRE ().getOrdinal ()] = null;
            continue;
          }

          if (aLexer.getCurRE () instanceof final ExpRStringLiteral aExpRStrLit &&
              StringHelper.isNotEmpty (aExpRStrLit.getImage ()))
          {
            aExpRStrLit.generateDfa ();
            if (i != 0 && !aLexer.getMixed ()[aLexer.getLexStateIndex ()] && bIgnoring != bIgnore)
              aLexer.getMixed ()[aLexer.getLexStateIndex ()] = true;
          }
          else
            if (aLexer.getCurRE ().canMatchAnyChar ())
            {
              if (aLexer.getCanMatchAnyChar ()[aLexer.getLexStateIndex ()] == -1 ||
                  aLexer.getCanMatchAnyChar ()[aLexer.getLexStateIndex ()] > aLexer.getCurRE ().getOrdinal ())
                aLexer.getCanMatchAnyChar ()[aLexer.getLexStateIndex ()] = aLexer.getCurRE ().getOrdinal ();
            }
            else
            {
              Nfa aTemp;

              if (aLexer.getCurRE () instanceof final ExpRChoice aChoice)
                aChoices.add (aChoice);

              aTemp = aLexer.getCurRE ().generateNfa (bIgnore);
              aTemp.end ().setFinal (true);
              aTemp.end ().setKind (aLexer.getCurRE ().getOrdinal ());
              aLexer.getInitialState ().addMove (aTemp.start ());
            }

          if (aLexer.getKinds ().length < aLexer.getCurRE ().getOrdinal ())
          {
            final ETokenKind [] aTmp = new ETokenKind [aLexer.getCurRE ().getOrdinal () + 1];

            System.arraycopy (aLexer.getKinds (), 0, aTmp, 0, aLexer.getKinds ().length);
            aLexer.setKinds (aTmp);
          }
          // System.out.println(" ordina : " + curRE.ordinal);

          aLexer.getKinds ()[aLexer.getCurRE ().getOrdinal ()] = eKind;

          if (respec.getNextState () != null &&
              !respec.getNextState ().equals (aLexer.getLexStateName ()[aLexer.getLexStateIndex ()]))
            aLexer.getNewLexState ()[aLexer.getCurRE ().getOrdinal ()] = respec.getNextState ();

          if (respec.getAct () != null &&
              respec.getAct ().getActionTokens () != null &&
              !respec.getAct ().getActionTokens ().isEmpty ())
            aLexer.getActions ()[aLexer.getCurRE ().getOrdinal ()] = respec.getAct ();

          switch (eKind)
          {
            case SPECIAL:
              aLexer.setHasSkipActions (aLexer.isHasSkipActions () |
                                          (aLexer.getActions ()[aLexer.getCurRE ().getOrdinal ()] != null) ||
                                          (aLexer.getNewLexState ()[aLexer.getCurRE ().getOrdinal ()] != null));
              aLexer.setHasSpecial (true);
              aLexer.getToSpecial ()[aLexer.getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                   (aLexer.getCurRE ().getOrdinal () %
                                                                                    64);
              aLexer.getToSkip ()[aLexer.getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                (aLexer.getCurRE ().getOrdinal () %
                                                                                 64);
              break;
            case SKIP:
              aLexer.setHasSkipActions (aLexer.isHasSkipActions () |
                                          (aLexer.getActions ()[aLexer.getCurRE ().getOrdinal ()] != null));
              aLexer.setHasSkip (true);
              aLexer.getToSkip ()[aLexer.getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                (aLexer.getCurRE ().getOrdinal () %
                                                                                 64);
              break;
            case MORE:
              aLexer.setHasMoreActions (aLexer.isHasMoreActions () |
                                          (aLexer.getActions ()[aLexer.getCurRE ().getOrdinal ()] != null));
              aLexer.setHasMore (true);
              aLexer.getToMore ()[aLexer.getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                (aLexer.getCurRE ().getOrdinal () %
                                                                                 64);

              if (aLexer.getNewLexState ()[aLexer.getCurRE ().getOrdinal ()] != null)
                aLexer.getCanReachOnMore ()[_getIndex (aLexer.getNewLexState ()[aLexer.getCurRE ()
                                                                                            .getOrdinal ()])] = true;
              else
                aLexer.getCanReachOnMore ()[aLexer.getLexStateIndex ()] = true;

              break;
            case TOKEN:
              aLexer.setHasTokenActions (aLexer.isHasTokenActions () |
                                           (aLexer.getActions ()[aLexer.getCurRE ().getOrdinal ()] != null));
              aLexer.getToToken ()[aLexer.getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                 (aLexer.getCurRE ().getOrdinal () %
                                                                                  64);
              break;
            default:
              throw new IllegalStateException ();
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      NfaState.computeClosures ();

      for (final NfaState aItem : aLexer.getInitialState ().getEpsilonMoves ())
        aItem.generateCode ();

      aLexer.getHasNfa ()[aLexer.getLexStateIndex ()] = (NfaState.nfa ().getGeneratedStates () != 0);
      if (aLexer.getHasNfa ()[aLexer.getLexStateIndex ()])
      {
        aLexer.getInitialState ().generateCode ();
        aLexer.getInitialState ().generateInitMoves ();
      }

      if (aLexer.getInitialState ().getKind () != Integer.MAX_VALUE && aLexer.getInitialState ().getKind () != 0)
      {
        if ((aLexer.getToSkip ()[aLexer.getInitialState ().getKind () / 64] &
             (1L << aLexer.getInitialState ().getKind ())) != 0L ||
            (aLexer.getToSpecial ()[aLexer.getInitialState ().getKind () / 64] &
             (1L << aLexer.getInitialState ().getKind ())) != 0L)
          aLexer.setHasSkipActions (true);
        else
          if ((aLexer.getToMore ()[aLexer.getInitialState ().getKind () / 64] &
               (1L << aLexer.getInitialState ().getKind ())) != 0L)
            aLexer.setHasMoreActions (true);
          else
            aLexer.setHasTokenActions (true);

        if (aLexer.getInitMatch ()[aLexer.getLexStateIndex ()] == 0 ||
            aLexer.getInitMatch ()[aLexer.getLexStateIndex ()] > aLexer.getInitialState ().getKind ())
        {
          aLexer.getInitMatch ()[aLexer.getLexStateIndex ()] = aLexer.getInitialState ().getKind ();
          aLexer.setHasEmptyMatch (true);
        }
      }
      else
        if (aLexer.getInitMatch ()[aLexer.getLexStateIndex ()] == 0)
          aLexer.getInitMatch ()[aLexer.getLexStateIndex ()] = Integer.MAX_VALUE;

      ExpRStringLiteral.fillSubString ();

      if (aLexer.getHasNfa ()[aLexer.getLexStateIndex ()] && !aLexer.getMixed ()[aLexer.getLexStateIndex ()])
        ExpRStringLiteral.generateNfaStartStates (this, aLexer.getInitialState ());

      ExpRStringLiteral.dumpDfaCode (this);

      if (aLexer.getHasNfa ()[aLexer.getLexStateIndex ()])
        NfaState.dumpMoveNfa (this);

      if (aLexer.getStateSetSize () < NfaState.nfa ().getGeneratedStates ())
        aLexer.setStateSetSize (NfaState.nfa ().getGeneratedStates ());
    }

    for (final ExpRChoice aItem : aChoices)
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

    if (aLexer.isHasLoop ())
    {
      switchToStaticsFile ();
      genCodeLine ("static int  jjemptyLineNo[" + aLexer.getMaxLexStates () + "];");
      genCodeLine ("static int  jjemptyColNo[" + aLexer.getMaxLexStates () + "];");
      genCodeLine ("static bool jjbeenHere[" + aLexer.getMaxLexStates () + "];");
      switchToMainFile ();
    }

    if (aLexer.isHasSkipActions ())
      _dumpSkipActions ();
    if (aLexer.isHasMoreActions ())
      _dumpMoreActions ();
    if (aLexer.isHasTokenActions ())
      _dumpTokenActions ();

    NfaState.printBoilerPlateCpp (this);

    {
      final Map <String, Object> aOpts = new HashMap <> ();
      aOpts.put ("charStreamName", "CharStream");
      aOpts.put ("parserClassName", grammar ().getParserName ());
      aOpts.put ("defaultLexState", "defaultLexState");
      aOpts.put ("lexStateNameLength", Integer.toString (aLexer.getLexStateName ().length));
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
      aOpts.put ("lexStateNameLength", Integer.toString (aLexer.getLexStateName ().length));
      writeTemplate ("/templates/cpp/DumpVarDeclarations.template", aOpts);
    }
    genCodeLine (/* { */ "};");

    switchToStaticsFile ();
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String sFileName = Options.getOutputDirectory () +
                             File.separator +
                             aLexer.getTokenMgrClassName () +
                             getFileExtension ();
    saveOutput (sFileName);
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
    genCodeLine ("  " +
                 lexer ().getTokenMgrClassName () +
                 "(JAVACC_CHARSTREAM *stream, int lexState = " +
                 lexer ().getDefaultLexState () +
                 ");");
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
    // remaining variables
    switchToStaticsFile ();
    genCodeNewLine ();
    genCodeLine ("/** Lexer state names. */");
    final LexerState aLexer = lexer ();
    _genStringLiteralArrayInCpp ("lexStateNames", aLexer.getLexStateName ());

    if (aLexer.getMaxLexStates () > 1)
    {
      genCodeNewLine ();
      genCodeLine ("/** Lex State array. */");
      genCode ("static const int jjnewLexState[] = {");

      for (i = 0; i < aLexer.getMaxOrdinal (); i++)
      {
        if (i % 25 == 0)
          genCode ("\n   ");

        if (aLexer.getNewLexState ()[i] == null)
          genCode ("-1, ");
        else
          genCode (_getIndex (aLexer.getNewLexState ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (aLexer.isHasSkip () || aLexer.isHasMore () || aLexer.isHasSpecial ())
    {
      // Bit vector for TOKEN
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoToken[] = {");
      for (i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToToken ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (aLexer.isHasSkip () || aLexer.isHasSpecial ())
    {
      // Bit vector for SKIP
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoSkip[] = {");
      for (i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToSkip ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (aLexer.isHasSpecial ())
    {
      // Bit vector for SPECIAL
      genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoSpecial[] = {");
      for (i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToSpecial ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    if (false)
      // Not needed as we just use else
      if (aLexer.isHasMore ())
      {
        // Bit vector for MORE
        genCode ("static const " + eOutputLanguage.getTypeLong () + " jjtoMore[] = {");
        for (i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
        {
          if (i % 4 == 0)
            genCode ("\n   ");
          genCode (eOutputLanguage.getLongHex (aLexer.getToMore ()[i]) + ", ");
        }
        genCodeLine ("\n};");
      }
  }

  private void _dumpFillToken ()
  {
    final double TOKEN_VERSION = OutputHelper.getVersionDashStar ("Token.java");
    final boolean bHasBinaryNewToken = TOKEN_VERSION > 4.09;

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
      genCodeLine ("   t = " +
                   getClassQualifier (Options.getTokenFactory ()) +
                   "newToken(jjmatchedKind, curTokenImage);");
    }
    else
      if (bHasBinaryNewToken)
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
    final LexerState aLexer = lexer ();
    genCodeLine ("const int defaultLexState = " + aLexer.getDefaultLexState () + ";");
    genCodeLine ("/** Get the next Token. */");
    generateMethodDefHeader ("Token *", aLexer.getTokenMgrClassName (), "getNextToken()");
    genCodeLine ("{");
    if (aLexer.isHasSpecial ())
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

    if (aLexer.isHasSpecial ())
      genCodeLine ("      matchedToken->specialToken = specialToken;");

    if (grammar ().getNextStateForEof () != null || grammar ().getActionForEof () != null)
      genCodeLine ("      TokenLexicalActions(matchedToken);");

    if (Options.isCommonTokenAction ())
      genCodeLine ("      CommonTokenAction(matchedToken);");

    genCodeLine ("      return matchedToken;");
    genCodeLine ("   }");
    genCodeLine ("   curChar = input_stream->BeginToken();");

    if (aLexer.isHasMoreActions () || aLexer.isHasSkipActions () || aLexer.isHasTokenActions ())
    {
      genCodeLine ("   image = jjimage;");
      genCodeLine ("   image.clear();");
      genCodeLine ("   jjimageLen = 0;");
    }

    genCodeNewLine ();

    String sPrefix = "";
    if (aLexer.isHasMore ())
    {
      genCodeLine ("   for (;;)");
      genCodeLine ("   {");
      sPrefix = "  ";
    }

    String sEndSwitch = "";
    String sCaseStr = "";
    // this also sets up the start state of the nfa
    if (aLexer.getMaxLexStates () > 1)
    {
      genCodeLine (sPrefix + "   switch(curLexState)");
      genCodeLine (sPrefix + "   {");
      sEndSwitch = sPrefix + "   }";
      sCaseStr = sPrefix + "     case ";
      sPrefix += "    ";
    }

    sPrefix += "   ";
    for (int i = 0; i < aLexer.getMaxLexStates (); i++)
    {
      if (aLexer.getMaxLexStates () > 1)
        genCodeLine (sCaseStr + i + ":");

      if (aLexer.getSinglesToSkip ()[i].hasTransitions ())
      {
        // added the backup(0) to make JIT happy
        genCodeLine (sPrefix + "{ input_stream->backup(0);");
        if (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[0] != 0L &&
            aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[1] != 0L)
        {
          genCodeLine (sPrefix +
                       "   while ((curChar < 64" +
                       " && (" +
                       eOutputLanguage.getLongHex (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[0]) +
                       " & (1L << curChar)) != 0L) || \n" +
                       sPrefix +
                       "          (curChar >> 6) == 1" +
                       " && (" +
                       eOutputLanguage.getLongHex (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[1]) +
                       " & (1L << (curChar & 077))) != " +
                       eOutputLanguage.getLongPlain (0) +
                       ")");
        }
        else
          if (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[1] == 0L)
          {
            genCodeLine (sPrefix +
                         "   while (curChar <= " +
                         (int) maxChar (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[0]) +
                         " && (" +
                         eOutputLanguage.getLongHex (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[0]) +
                         " & (1L << curChar)) != " +
                         eOutputLanguage.getLongPlain (0) +
                         ")");
          }
          else
            if (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[0] == 0L)
            {
              genCodeLine (sPrefix +
                           "   while (curChar > 63 && curChar <= " +
                           (maxChar (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[1]) + 64) +
                           " && (" +
                           eOutputLanguage.getLongHex (aLexer.getSinglesToSkip ()[i].getAsciiMoves ()[1]) +
                           " & (1L << (curChar & 077))) != " +
                           eOutputLanguage.getLongPlain (0) +
                           ")");
            }

        genCodeLine (sPrefix + "{");
        if (Options.isDebugTokenManager ())
        {
          if (aLexer.getMaxLexStates () > 1)
          {
            genCodeLine ("      fprintf(debugStream, \"<%s>\" , addUnicodeEscapes(lexStateNames[curLexState]).c_str());");
          }

          genCodeLine ("      fprintf(debugStream, \"Skipping character : %c(%d)\\n\", curChar, (int)curChar);");
        }

        genCodeLine (sPrefix + "if (input_stream->endOfInput()) { goto EOFLoop; }");
        genCodeLine (sPrefix + "curChar = input_stream->BeginToken();");
        genCodeLine (sPrefix + "}");
        genCodeLine (sPrefix + "}");
      }

      if (aLexer.getInitMatch ()[i] != Integer.MAX_VALUE && aLexer.getInitMatch ()[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      fprintf(debugStream, \"   Matched the empty string as %s token.\\n\", addUnicodeEscapes(tokenImage[" +
                       aLexer.getInitMatch ()[i] +
                       "]).c_str());");

        genCodeLine (sPrefix + "jjmatchedKind = " + aLexer.getInitMatch ()[i] + ";");
        genCodeLine (sPrefix + "jjmatchedPos = -1;");
        genCodeLine (sPrefix + "curPos = 0;");
      }
      else
      {
        genCodeLine (sPrefix + "jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");
        genCodeLine (sPrefix + "jjmatchedPos = 0;");
      }

      if (Options.isDebugTokenManager ())
      {
        genCodeLine ("   fprintf(debugStream, " +
                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                     "input_stream->getEndLine(), input_stream->getEndColumn());");
      }

      genCodeLine (sPrefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");

      if (aLexer.getCanMatchAnyChar ()[i] != -1)
      {
        if (aLexer.getInitMatch ()[i] != Integer.MAX_VALUE && aLexer.getInitMatch ()[i] != 0)
          genCodeLine (sPrefix +
                       "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
                       aLexer.getCanMatchAnyChar ()[i] +
                       "))");
        else
          genCodeLine (sPrefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + aLexer.getCanMatchAnyChar ()[i] + ")");
        genCodeLine (sPrefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           fprintf(debugStream, \"   Current character matched as a %s token.\\n\", addUnicodeEscapes(tokenImage[" +
                       aLexer.getCanMatchAnyChar ()[i] +
                       "]).c_str());");
        }
        genCodeLine (sPrefix + "   jjmatchedKind = " + aLexer.getCanMatchAnyChar ()[i] + ";");

        if (aLexer.getInitMatch ()[i] != Integer.MAX_VALUE && aLexer.getInitMatch ()[i] != 0)
          genCodeLine (sPrefix + "   jjmatchedPos = 0;");

        genCodeLine (sPrefix + "}");
      }

      if (aLexer.getMaxLexStates () > 1)
        genCodeLine (sPrefix + "break;");
    }

    if (aLexer.getMaxLexStates () > 1)
      genCodeLine (sEndSwitch);
    else
      if (aLexer.getMaxLexStates () == 0)
        genCodeLine ("       jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");

    if (aLexer.getMaxLexStates () > 1)
      sPrefix = "  ";
    else
      sPrefix = "";

    if (aLexer.getMaxLexStates () > 0)
    {
      genCodeLine (sPrefix + "   if (jjmatchedKind != 0x" + Integer.toHexString (Integer.MAX_VALUE) + ")");
      genCodeLine (sPrefix + "   {");
      genCodeLine (sPrefix + "      if (jjmatchedPos + 1 < curPos)");

      if (Options.isDebugTokenManager ())
      {
        genCodeLine (sPrefix + "      {");
        genCodeLine (sPrefix +
                     "         fprintf(debugStream, " +
                     "\"   Putting back %d characters into the input stream.\\n\", (curPos - jjmatchedPos - 1));");
      }

      genCodeLine (sPrefix + "         input_stream->backup(curPos - jjmatchedPos - 1);");

      if (Options.isDebugTokenManager ())
      {
        genCodeLine (sPrefix + "      }");
      }

      if (Options.isDebugTokenManager ())
      {
        genCodeLine ("    fprintf(debugStream, " +
                     "\"****** FOUND A %d(%s) MATCH (%s) ******\\n\", jjmatchedKind, addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str(), addUnicodeEscapes(input_stream->GetSuffix(jjmatchedPos + 1)).c_str());");
      }

      if (aLexer.isHasSkip () || aLexer.isHasMore () || aLexer.isHasSpecial ())
      {
        genCodeLine (sPrefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (sPrefix + "      {");
      }

      genCodeLine (sPrefix + "         matchedToken = jjFillToken();");

      if (aLexer.isHasSpecial ())
        genCodeLine (sPrefix + "         matchedToken->specialToken = specialToken;");

      if (aLexer.isHasTokenActions ())
        genCodeLine (sPrefix + "         TokenLexicalActions(matchedToken);");

      if (aLexer.getMaxLexStates () > 1)
      {
        genCodeLine ("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine (sPrefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      if (Options.isCommonTokenAction ())
        genCodeLine (sPrefix + "         CommonTokenAction(matchedToken);");

      genCodeLine (sPrefix + "         return matchedToken;");

      if (aLexer.isHasSkip () || aLexer.isHasMore () || aLexer.isHasSpecial ())
      {
        genCodeLine (sPrefix + "      }");

        if (aLexer.isHasSkip () || aLexer.isHasSpecial ())
        {
          if (aLexer.isHasMore ())
          {
            genCodeLine (sPrefix +
                         "      else if ((jjtoSkip[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
          }
          else
            genCodeLine (sPrefix + "      else");

          genCodeLine (sPrefix + "      {");

          if (aLexer.isHasSpecial ())
          {
            genCodeLine (sPrefix +
                         "         if ((jjtoSpecial[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
            genCodeLine (sPrefix + "         {");

            genCodeLine (sPrefix + "            matchedToken = jjFillToken();");

            genCodeLine (sPrefix + "            if (specialToken == nullptr)");
            genCodeLine (sPrefix + "               specialToken = matchedToken;");
            genCodeLine (sPrefix + "            else");
            genCodeLine (sPrefix + "            {");
            genCodeLine (sPrefix + "               matchedToken->specialToken = specialToken;");
            genCodeLine (sPrefix + "               specialToken = (specialToken->next = matchedToken);");
            genCodeLine (sPrefix + "            }");

            if (aLexer.isHasSkipActions ())
              genCodeLine (sPrefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (sPrefix + "         }");

            if (aLexer.isHasSkipActions ())
            {
              genCodeLine (sPrefix + "         else");
              genCodeLine (sPrefix + "            SkipLexicalActions(nullptr);");
            }
          }
          else
            if (aLexer.isHasSkipActions ())
              genCodeLine (sPrefix + "         SkipLexicalActions(nullptr);");

          if (aLexer.getMaxLexStates () > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (sPrefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (sPrefix + "         goto EOFLoop;");
          genCodeLine (sPrefix + "      }");
        }

        if (aLexer.isHasMore ())
        {
          if (aLexer.isHasMoreActions ())
            genCodeLine (sPrefix + "      MoreLexicalActions();");
          else
            if (aLexer.isHasSkipActions () || aLexer.isHasTokenActions ())
              genCodeLine (sPrefix + "      jjimageLen += jjmatchedPos + 1;");

          if (aLexer.getMaxLexStates () > 1)
          {
            genCodeLine ("      if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (sPrefix + "      curLexState = jjnewLexState[jjmatchedKind];");
          }
          genCodeLine (sPrefix + "      curPos = 0;");
          genCodeLine (sPrefix + "      jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");

          genCodeLine (sPrefix + "   if (!input_stream->endOfInput()) {");
          genCodeLine (sPrefix + "         curChar = input_stream->readChar();");

          if (Options.isDebugTokenManager ())
          {
            genCodeLine ("   fprintf(debugStream, " +
                         "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                         "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                         "input_stream->getEndLine(), input_stream->getEndColumn());");
          }
          genCodeLine (sPrefix + "   continue;");
          genCodeLine (sPrefix + " }");
        }
      }

      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix + "   int error_line = input_stream->getEndLine();");
      genCodeLine (sPrefix + "   int error_column = input_stream->getEndColumn();");
      genCodeLine (sPrefix + "   JJString error_after;");
      genCodeLine (sPrefix + "   bool EOFSeen = false;");
      genCodeLine (sPrefix + "   if (input_stream->endOfInput()) {");
      genCodeLine (sPrefix + "      EOFSeen = true;");
      genCodeLine (sPrefix + "      error_after = curPos <= 1 ? EMPTY : input_stream->GetImage();");
      genCodeLine (sPrefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine (sPrefix + "         error_line++;");
      genCodeLine (sPrefix + "         error_column = 0;");
      genCodeLine (sPrefix + "      }");
      genCodeLine (sPrefix + "      else");
      genCodeLine (sPrefix + "         error_column++;");
      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix + "   if (!EOFSeen) {");
      genCodeLine (sPrefix + "      error_after = curPos <= 1 ? EMPTY : input_stream->GetImage();");
      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix +
                   "   errorHandler->lexicalError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, this);");
    }

    if (aLexer.isHasMore ())
      genCodeLine (sPrefix + " }");

    genCodeLine ("  }");
    genCodeLine ("}");
    genCodeNewLine ();
  }

  private void _dumpSkipActions ()
  {
    ExpAction aAct;

    final LexerState aLexer = lexer ();
    generateMethodDefHeader ("void ", aLexer.getTokenMgrClassName (), "SkipLexicalActions(Token *matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < aLexer.getMaxOrdinal (); i++)
    {
      if ((aLexer.getToSkip ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((aAct = aLexer.getActions ()[i]) == null ||
             aAct.getActionTokens () == null ||
             aAct.getActionTokens ().isEmpty ()) && !aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (aLexer.getInitMatch ()[aLexer.getLexStates ()[i]] == i &&
            aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + aLexer.getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine ("            jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + aLexer.getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((aAct = aLexer.getActions ()[i]) == null || aAct.getActionTokens ().isEmpty ())
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

        printTokenSetup (aAct.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : aAct.getActionTokens ())
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
    ExpAction aAct;

    final LexerState aLexer = lexer ();
    generateMethodDefHeader ("void ", aLexer.getTokenMgrClassName (), "MoreLexicalActions()");
    genCodeLine ("{");
    genCodeLine ("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < aLexer.getMaxOrdinal (); i++)
    {
      if ((aLexer.getToMore ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((aAct = aLexer.getActions ()[i]) == null ||
             aAct.getActionTokens () == null ||
             aAct.getActionTokens ().isEmpty ()) && !aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (aLexer.getInitMatch ()[aLexer.getLexStates ()[i]] == i &&
            aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + aLexer.getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine ("            jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + aLexer.getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((aAct = aLexer.getActions ()[i]) == null || aAct.getActionTokens ().isEmpty ())
        {
          break;
        }

        genCode ("         image.append");

        if (ExpRStringLiteral.strLit ().getAllImages ()[i] != null)
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
        else
          genCodeLine ("(input_stream->GetSuffix(jjimageLen));");

        genCodeLine ("         jjimageLen = 0;");
        printTokenSetup (aAct.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : aAct.getActionTokens ())
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
    ExpAction aAct;
    int i;

    final LexerState aLexer = lexer ();
    generateMethodDefHeader ("void ", aLexer.getTokenMgrClassName (), "TokenLexicalActions(Token *matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (i = 0; i < aLexer.getMaxOrdinal (); i++)
    {
      if ((aLexer.getToToken ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        if (((aAct = aLexer.getActions ()[i]) == null ||
             aAct.getActionTokens () == null ||
             aAct.getActionTokens ().isEmpty ()) && !aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " : {");

        if (aLexer.getInitMatch ()[aLexer.getLexStates ()[i]] == i &&
            aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + aLexer.getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream->getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream->getBeginColumn())");
          genCodeLine ("               errorHandler->lexicalError(JJString(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream->getBeginLine() + \", " +
                       "column \" + input_stream->getBeginColumn() + \".\"), this);");
          genCodeLine ("            jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream->getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream->getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + aLexer.getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        if ((aAct = aLexer.getActions ()[i]) == null || aAct.getActionTokens ().isEmpty ())
          break;

        if (i == 0)
        {
          // For EOF no image is
          genCodeLine ("      image.setLength(0);");
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

        printTokenSetup (aAct.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : aAct.getActionTokens ())
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
