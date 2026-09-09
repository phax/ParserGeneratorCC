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

package com.helger.pgcc.output.java;

import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
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
import com.helger.pgcc.output.AbstractLexGenJavaLike;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.OutputHelper;
import com.helger.pgcc.parser.AbstractCodeGenerator;
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
public class LexGenJava extends AbstractLexGenJavaLike
{
  private static final String DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL = "/templates/java/DumpStaticVarDeclarations.template";
  private static final String DUMP_DEBUG_METHODS_TEMPLATE_RESOURCE_URL = "/templates/java/DumpDebugMethods.template";
  private static final String BOILERPLATER_METHOD_RESOURCE_URL = "/templates/java/TokenManagerBoilerPlateMethods.template";

  // Order is important!
  // Order is important!

  /** Default constructor. */
  public LexGenJava ()
  {}

  private void _printClassHead ()
  {
    final GrammarState aGrammar = grammar ();
    final List <String> aTn = new ArrayList <> (aGrammar.getToolNameList ());
    aTn.add (CPG.APP_NAME);
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    genCodeLine ("/* " + getIdString (aTn, lexer ().getTokenMgrClassName () + getFileExtension ()) + " */");

    boolean bHasImport = false;
    int nIndex = 0;
    int i = 1;
    for (;;)
    {
      if (aGrammar.cuToInsertionPoint1 ().size () <= nIndex)
        break;

      int nKind = aGrammar.cuToInsertionPoint1 ().get (nIndex).kind;
      if ((nKind != JavaCCParserConstants.PACKAGE) && (nKind != JavaCCParserConstants.IMPORT))
        break;
      if (nKind == JavaCCParserConstants.IMPORT)
        bHasImport = true;

      for (; i < aGrammar.cuToInsertionPoint1 ().size (); i++)
      {
        nKind = aGrammar.cuToInsertionPoint1 ().get (i).kind;
        if (nKind == JavaCCParserConstants.SEMICOLON ||
            nKind == JavaCCParserConstants.ABSTRACT ||
            nKind == JavaCCParserConstants.FINAL ||
            nKind == JavaCCParserConstants.PRIVATE ||
            nKind == JavaCCParserConstants.PROTECTED ||
            nKind == JavaCCParserConstants.PUBLIC ||
            nKind == JavaCCParserConstants.CLASS ||
            nKind == JavaCCParserConstants.INTERFACE ||
            nKind == JavaCCParserConstants.ENUM)
        {
          setLineAndCol (aGrammar.cuToInsertionPoint1 ().get (nIndex).beginLine,
                         aGrammar.cuToInsertionPoint1 ().get (nIndex).beginColumn);
          int j = nIndex;
          for (; j < i; j++)
          {
            printToken (aGrammar.cuToInsertionPoint1 ().get (j));
          }
          if (nKind == JavaCCParserConstants.SEMICOLON)
            printToken (aGrammar.cuToInsertionPoint1 ().get (j));
          genCodeNewLine ();
          break;
        }
      }
      ++i;
      nIndex = i;
    }

    genCodeNewLine ();
    genCodeLine ("/** Token Manager. */");

    // Emit only if an import is present
    if (bHasImport)
    {
      // For issue #14
      genCodeLine ("@SuppressWarnings (\"unused\")");
    }

    if (Options.isJavaSupportClassVisibilityPublic ())
    {
      genModifier ("public ");
    }
    // genCodeLine("class " + tokMgrClassName + " implements " +
    // cu_name + "Constants");
    // String superClass =
    // Options.stringValue(Options.USEROPTION__TOKEN_MANAGER_SUPER_CLASS);
    genClassStart (null,
                   lexer ().getTokenMgrClassName (),
                   new String [] {},
                   new String [] { aGrammar.getParserName () + "Constants" });
    // genCodeLine("{"); // }

    if (aGrammar.getTokenMgrDecls () != null && aGrammar.getTokenMgrDecls ().isNotEmpty ())
    {
      boolean bCommonTokenActionSeen = false;
      final boolean bCommonTokenActionNeeded = Options.isCommonTokenAction ();
      Token t = aGrammar.getTokenMgrDecls ().getFirstOrNull ();

      printTokenSetup (t);
      setColToStart ();

      for (final Token s_token_mgr_decl : aGrammar.getTokenMgrDecls ())
      {
        t = s_token_mgr_decl;
        if (t.kind == JavaCCParserConstants.IDENTIFIER && bCommonTokenActionNeeded && !bCommonTokenActionSeen)
          bCommonTokenActionSeen = t.image.equals ("CommonTokenAction");

        printToken (t);
      }

      genCodeNewLine ();
      if (bCommonTokenActionNeeded && !bCommonTokenActionSeen)
      {
        JavaCCErrors.warning ("You have the COMMON_TOKEN_ACTION option set. " +
                              "But it appears you have not defined the method :\n" +
                              "      " +
                              "void CommonTokenAction(Token t)\n" +
                              "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
      }
    }
    else
      if (Options.isCommonTokenAction ())
      {
        JavaCCErrors.warning ("You have the COMMON_TOKEN_ACTION option set. " +
                              "But you have not defined the method :\n" +
                              "      " +
                              "void CommonTokenAction(Token t)\n" +
                              "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
      }

    if (Options.isDebugTokenManager ())
    {
      genCodeNewLine ();
      genCodeLine ("  /** Debug output. */");
      genCodeLine ("  private java.io.PrintStream debugStream = System.out;");
      genCodeNewLine ();
      genCodeLine ("  /**");
      genCodeLine ("   * @return debug output");
      genCodeLine ("   */");
      genCodeLine ("  public java.io.PrintStream getDebugStream() {");
      genCodeLine ("    return debugStream;");
      genCodeLine ("  }");
      genCodeNewLine ();
      genCodeLine ("  /**");
      genCodeLine ("   * Set debug output");
      genCodeLine ("   * @param ds debug PrintStream. May not be <code>null</code>");
      genCodeLine ("   */");
      genCodeLine ("  public void setDebugStream(final java.io.PrintStream ds) {");
      genCodeLine ("    debugStream = ds;");
      genCodeLine ("  }");
    }

    if (Options.isTokenManagerUsesParser ())
    {
      genCodeNewLine ();
      genCodeLine ("  public " + aGrammar.getParserName () + " parser = null;");
    }
  }

  private void _dumpDebugMethods () throws IOException
  {
    writeTemplate (DUMP_DEBUG_METHODS_TEMPLATE_RESOURCE_URL, null);
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

      AbstractExpRegularExpression aRe;
      for (i = 0; i < aRespecs.size (); i++)
        if (aLexer.getMaxOrdinal () <= (aRe = aRespecs.get (i).getRexp ()).getOrdinal ())
          aLexer.setMaxOrdinal (aRe.getOrdinal () + 1);
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
   * Record a character that is skipped on its own, without an automaton.
   *
   * @param c
   *        The character to skip.
   * @param nKind
   *        The token ordinal it belongs to.
   */
  public static void addCharToSkip (final char c, final int nKind)
  {
    lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].addChar (c);
    lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].setKind (nKind);
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
    aLexer.setErrorHandlingClass (Options.getTokenMgrErrorClass ());
    final List <ExpRChoice> aChoices = new ArrayList <> ();

    final GrammarState aGrammar = grammar ();
    aLexer.setTokenMgrClassName (aGrammar.getParserName () + "TokenManager");

    if (!aLexer.isGenerateDataOnly ())
      _printClassHead ();
    _buildLexStatesTable ();

    boolean bIgnoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : aLexer.allTpsForState ().entrySet ())
    {
      int nStartState = -1;
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

          if (!Options.isNoDfa () &&
              aLexer.getCurRE () instanceof final ExpRStringLiteral aExpRStrLit &&
              StringHelper.isNotEmpty (aExpRStrLit.getImage ()))
          {
            aExpRStrLit.generateDfa ();
            if (i != 0 && !aLexer.getMixed ()[aLexer.getLexStateIndex ()] && bIgnoring != bIgnore)
            {
              aLexer.getMixed ()[aLexer.getLexStateIndex ()] = true;
            }
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

          if (respec.getAct () != null && respec.getAct ().getActionTokens ().isNotEmpty ())
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
        nStartState = aLexer.getInitialState ().generateInitMoves ();
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

      if (aLexer.isGenerateDataOnly ())
      {
        ExpRStringLiteral.updateStringLiteralData (aLexer.getLexStateIndex ());
        NfaState.updateNfaData (aLexer.getTotalNumStates (),
                                nStartState,
                                aLexer.getLexStateIndex (),
                                aLexer.getCanMatchAnyChar ()[aLexer.getLexStateIndex ()]);
      }
      else
      {
        ExpRStringLiteral.dumpDfaCode (this);
        if (aLexer.getHasNfa ()[aLexer.getLexStateIndex ()])
        {
          NfaState.dumpMoveNfa (this);
        }
      }
      aLexer.setTotalNumStates (aLexer.getTotalNumStates () + NfaState.nfa ().getGeneratedStates ());
      if (aLexer.getStateSetSize () < NfaState.nfa ().getGeneratedStates ())
        aLexer.setStateSetSize (NfaState.nfa ().getGeneratedStates ());
    }

    for (final ExpRChoice aItem : aChoices)
      aItem.checkUnmatchability ();

    checkEmptyStringMatch ();

    if (aLexer.isGenerateDataOnly ())
    {
      aLexer.getTokenizerData ().setParserName (aGrammar.getParserName ());
      NfaState.buildTokenizerData (aLexer.getTokenizerData ());
      ExpRStringLiteral.BuildTokenizerData (aLexer.getTokenizerData ());
      final int [] aNewLexStateIndices = new int [aLexer.getMaxOrdinal ()];

      final StringBuilder aTokenMgrDecls = new StringBuilder ();
      if (aGrammar.getTokenMgrDecls () != null)
        for (final Token t : aGrammar.getTokenMgrDecls ())
          aTokenMgrDecls.append (t.image).append (' ');
      aLexer.getTokenizerData ().setDecls (aTokenMgrDecls.toString ());

      final Map <Integer, String> aActionStrings = new HashMap <> ();
      for (int i = 0; i < aLexer.getMaxOrdinal (); i++)
      {
        if (aLexer.getNewLexState ()[i] == null)
        {
          aNewLexStateIndices[i] = -1;
        }
        else
        {
          aNewLexStateIndices[i] = _getIndex (aLexer.getNewLexState ()[i]);
        }
        // For java, we have this but for other languages, eventually we will
        // simply have a string.
        final ExpAction aAct = aLexer.getActions ()[i];
        if (aAct == null)
          continue;

        final StringBuilder aSB = new StringBuilder ();
        for (final Token t : aAct.getActionTokens ())
          aSB.append (t.image).append (' ');
        aActionStrings.put (Integer.valueOf (i), aSB.toString ());
      }
      aLexer.getTokenizerData ().setDefaultLexState (aLexer.getDefaultLexState ());
      aLexer.getTokenizerData ().setLexStateNames (aLexer.getLexStateName ());
      aLexer.getTokenizerData ()
              .updateMatchInfo (aActionStrings,
                                aNewLexStateIndices,
                                aLexer.getToSkip (),
                                aLexer.getToSpecial (),
                                aLexer.getToMore (),
                                aLexer.getToToken ());
      return;
    }

    final EOutputLanguage eOutputLanguage = getOutputLanguage ();

    ExpRStringLiteral.dumpStrLiteralImages (this);
    _dumpFillToken ();
    NfaState.dumpStateSets (this);
    NfaState.dumpNonAsciiMoveMethods (this);
    _dumpGetNextToken ();

    if (Options.isDebugTokenManager ())
    {
      NfaState.dumpStatesForKind (this);
      _dumpDebugMethods ();
    }

    if (aLexer.isHasLoop ())
    {
      genCodeLine ("int[] jjemptyLineNo = new int[" + aLexer.getMaxLexStates () + "];");
      genCodeLine ("int[] jjemptyColNo = new int[" + aLexer.getMaxLexStates () + "];");
      genCodeLine (eOutputLanguage.getTypeBoolean () +
                   "[] jjbeenHere = new " +
                   eOutputLanguage.getTypeBoolean () +
                   "[" +
                   aLexer.getMaxLexStates () +
                   "];");
    }

    _dumpSkipActions ();
    _dumpMoreActions ();
    _dumpTokenActions ();

    NfaState.printBoilerPlateJava (this);

    final String sCharStreamName = AbstractCodeGenerator.getCharStreamName ();

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", sCharStreamName);
    aOpts.put ("lexStateNameLength", Integer.toString (aLexer.getLexStateName ().length));
    aOpts.put ("defaultLexState", Integer.toString (aLexer.getDefaultLexState ()));
    aOpts.put ("noDfa", Boolean.toString (Options.isNoDfa ()));
    aOpts.put ("generatedStates", Integer.toString (aLexer.getTotalNumStates ()));
    writeTemplate (BOILERPLATER_METHOD_RESOURCE_URL, aOpts);

    _dumpStaticVarDeclarations (sCharStreamName);
    genCodeLine (/* { */ "}");

    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String sFileName = Options.getOutputDirectory () +
                             File.separator +
                             aLexer.getTokenMgrClassName () +
                             getFileExtension ();

    if (Options.isBuildParser ())
    {
      saveOutput (sFileName);
    }
  }

  private void _dumpStaticVarDeclarations (final String sCharStreamName) throws IOException
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();

    genCodeNewLine ();
    genCodeLine ("/** Lexer state names. */");
    genCodeLine ("public static final String[] lexStateNames = {");
    final LexerState aLexer = lexer ();
    for (int i = 0; i < aLexer.getMaxLexStates (); i++)
      genCodeLine ("   \"" + aLexer.getLexStateName ()[i] + "\",");
    genCodeLine ("};");

    {
      genCodeNewLine ();
      genCodeLine ("/** Lex State array. */");
      genCode ("public static final int[] jjnewLexState = {");

      for (int i = 0; i < aLexer.getMaxOrdinal (); i++)
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

    {
      // Bit vector for TOKEN
      genCode ("static final long[] jjtoToken = {");
      for (int i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToToken ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for SKIP
      genCode ("static final long[] jjtoSkip = {");
      for (int i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToSkip ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for SPECIAL
      genCode ("static final long[] jjtoSpecial = {");
      for (int i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToSpecial ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for MORE
      genCode ("static final long[] jjtoMore = {");
      for (int i = 0; i < aLexer.getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (aLexer.getToMore ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", sCharStreamName);
    aOpts.put ("protected", "protected");
    aOpts.put ("private", "private");
    aOpts.put ("final", "final");
    aOpts.put ("lexStateNameLength", Integer.toString (aLexer.getLexStateName ().length));
    writeTemplate (DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL, aOpts);
  }

  // Assumes l != 0L

  private void _dumpFillToken ()
  {
    final double TOKEN_VERSION = OutputHelper.getVersionDashStar ("Token.java");
    final boolean bHasBinaryNewToken = TOKEN_VERSION > 4.09;

    genCodeLine ("protected Token jjFillToken()");
    genCodeLine ("{");
    genCodeLine ("   final Token t;");
    genCodeLine ("   final String curTokenImage;");
    if (lexer ().isKeepLineCol ())
    {
      genCodeLine ("   final int beginLine;");
      genCodeLine ("   final int endLine;");
      genCodeLine ("   final int beginColumn;");
      genCodeLine ("   final int endColumn;");
    }

    if (lexer ().isHasEmptyMatch ())
    {
      genCodeLine ("   if (jjmatchedPos < 0)");
      genCodeLine ("   {");
      genCodeLine ("      if (image == null)");
      genCodeLine ("         curTokenImage = \"\";");
      genCodeLine ("      else");
      genCodeLine ("         curTokenImage = image.toString();");

      if (lexer ().isKeepLineCol ())
      {
        genCodeLine ("      beginLine = endLine = input_stream.getEndLine();");
        genCodeLine ("      beginColumn = endColumn = input_stream.getEndColumn();");
      }

      genCodeLine ("   }");
      genCodeLine ("   else");
      genCodeLine ("   {");
      genCodeLine ("      String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine ("      curTokenImage = im == null ? input_stream.getImage() : im;");

      if (lexer ().isKeepLineCol ())
      {
        genCodeLine ("      beginLine = input_stream.getBeginLine();");
        genCodeLine ("      beginColumn = input_stream.getBeginColumn();");
        genCodeLine ("      endLine = input_stream.getEndLine();");
        genCodeLine ("      endColumn = input_stream.getEndColumn();");
      }

      genCodeLine ("   }");
    }
    else
    {
      genCodeLine ("   String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine ("   curTokenImage = im == null ? input_stream.getImage() : im;");
      if (lexer ().isKeepLineCol ())
      {
        genCodeLine ("   beginLine = input_stream.getBeginLine();");
        genCodeLine ("   beginColumn = input_stream.getBeginColumn();");
        genCodeLine ("   endLine = input_stream.getEndLine();");
        genCodeLine ("   endColumn = input_stream.getEndColumn();");
      }
    }

    if (Options.getTokenFactory ().length () > 0)
    {
      genCodeLine ("   t = " + Options.getTokenFactory () + ".newToken(jjmatchedKind, curTokenImage);");
    }
    else
      if (bHasBinaryNewToken)
      {
        genCodeLine ("   t = Token.newToken(jjmatchedKind, curTokenImage);");
      }
      else
      {
        genCodeLine ("   t = Token.newToken(jjmatchedKind);");
        genCodeLine ("   t.kind = jjmatchedKind;");
        genCodeLine ("   t.image = curTokenImage;");
      }

    if (lexer ().isKeepLineCol ())
    {
      genCodeNewLine ();
      genCodeLine ("   t.beginLine = beginLine;");
      genCodeLine ("   t.endLine = endLine;");
      genCodeLine ("   t.beginColumn = beginColumn;");
      genCodeLine ("   t.endColumn = endColumn;");
    }

    genCodeNewLine ();
    genCodeLine ("   return t;");
    genCodeLine ("}");
  }

  private void _dumpGetNextToken ()
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();

    genCodeNewLine ();
    final LexerState aLexer = lexer ();
    genCodeLine ("int curLexState = " + aLexer.getDefaultLexState () + ";");
    genCodeLine ("int defaultLexState = " + aLexer.getDefaultLexState () + ";");
    genCodeLine ("int jjnewStateCnt;");
    genCodeLine ("int jjround;");
    genCodeLine ("int jjmatchedPos;");
    genCodeLine ("int jjmatchedKind;");
    genCodeNewLine ();
    genCodeLine ("/** Get the next Token. */");
    genCodeLine ("public " + "Token getNextToken()" + " ");
    genCodeLine ("{");
    if (aLexer.isHasSpecial ())
    {
      genCodeLine ("  Token specialToken = null;");
    }
    genCodeLine ("  Token matchedToken;");
    genCodeLine ("  int curPos = 0;");
    genCodeNewLine ();
    genCodeLine ("  EOFLoop:");
    genCodeLine ("  for (;;)");
    genCodeLine ("  {");
    genCodeLine ("   try");
    genCodeLine ("   {");
    genCodeLine ("      curChar = input_stream.beginToken();");
    genCodeLine ("   }");
    genCodeLine ("   catch(final Exception e)");
    genCodeLine ("   {");

    if (Options.isDebugTokenManager ())
      genCodeLine ("      debugStream.println(\"Returning the <EOF> token.\\n\");");

    genCodeLine ("      jjmatchedKind = 0;");
    genCodeLine ("      jjmatchedPos = -1;");
    genCodeLine ("      matchedToken = jjFillToken();");

    if (aLexer.isHasSpecial ())
      genCodeLine ("      matchedToken.specialToken = specialToken;");

    if (grammar ().getNextStateForEof () != null || grammar ().getActionForEof () != null)
      genCodeLine ("      TokenLexicalActions(matchedToken);");

    if (Options.isCommonTokenAction ())
      genCodeLine ("      CommonTokenAction(matchedToken);");

    genCodeLine ("      return matchedToken;");
    genCodeLine ("   }");

    if (aLexer.isHasMoreActions () || aLexer.isHasSkipActions () || aLexer.isHasTokenActions ())
    {
      genCodeLine ("   image = jjimage;");
      genCodeLine ("   image.setLength(0);");
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
        genCodeLine (sPrefix + "try {");
        genCodeLine (sPrefix + "  input_stream.backup(0);");
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

        if (Options.isDebugTokenManager ())
        {
          genCodeLine (sPrefix + "{");
          genCodeLine ("      debugStream.println(" +
                       (aLexer.getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                       "\"Skipping character : \" + " +
                       aLexer.getErrorHandlingClass () +
                       ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \")\");");
        }
        genCodeLine (sPrefix + "      curChar = input_stream.beginToken();");

        if (Options.isDebugTokenManager ())
          genCodeLine (sPrefix + "}");

        genCodeLine (sPrefix + "}");
        genCodeLine (sPrefix + "catch (final java.io.IOException e1) {");
        genCodeLine (sPrefix + "  continue EOFLoop;");
        genCodeLine (sPrefix + "}");
      }

      if (aLexer.getInitMatch ()[i] != Integer.MAX_VALUE && aLexer.getInitMatch ()[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      debugStream.println(\"   Matched the empty string as \" + tokenImage[" +
                       aLexer.getInitMatch ()[i] +
                       "] + \" token.\");");

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
        genCodeLine ("      debugStream.println(" +
                     (aLexer.getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                     "\"Current character : \" + " +
                     aLexer.getErrorHandlingClass () +
                     ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      genCodeLine (sPrefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (aLexer.getCanMatchAnyChar ()[i] != -1)
      {
        if (aLexer.getInitMatch ()[i] != Integer.MAX_VALUE && aLexer.getInitMatch ()[i] != 0)
        {
          genCodeLine (sPrefix +
                       "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
                       aLexer.getCanMatchAnyChar ()[i] +
                       "))");
        }
        else
          genCodeLine (sPrefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + aLexer.getCanMatchAnyChar ()[i] + ")");
        genCodeLine (sPrefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           debugStream.println(\"   Current character matched as a \" + tokenImage[" +
                       aLexer.getCanMatchAnyChar ()[i] +
                       "] + \" token.\");");
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
                     "         debugStream.println(" +
                     "\"   Putting back \" + (curPos - jjmatchedPos - 1) + \" characters into the input stream.\");");
      }

      genCodeLine (sPrefix + "         input_stream.backup(curPos - jjmatchedPos - 1);");

      if (Options.isDebugTokenManager ())
        genCodeLine (sPrefix + "      }");

      if (Options.isDebugTokenManager ())
      {
        if (Options.isJavaUnicodeEscape () || Options.isJavaUserCharStream ())
        {
          genCodeLine ("    debugStream.println(" +
                       "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                       "(\" + " +
                       aLexer.getErrorHandlingClass () +
                       ".addEscapes(new String(input_stream.getSuffix(jjmatchedPos + 1))) + " +
                       "\") ******\\n\");");
        }
        else
        {
          genCodeLine ("    debugStream.println(" +
                       "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                       "(\" + " +
                       aLexer.getErrorHandlingClass () +
                       ".addEscapes(new String(input_stream.getSuffix(jjmatchedPos + 1))) + " +
                       "\") ******\\n\");");
        }
      }

      if (aLexer.isHasSkip () || aLexer.isHasMore () || aLexer.isHasSpecial ())
      {
        genCodeLine (sPrefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (sPrefix + "      {");
      }

      genCodeLine (sPrefix + "         matchedToken = jjFillToken();");

      if (aLexer.isHasSpecial ())
        genCodeLine (sPrefix + "         matchedToken.specialToken = specialToken;");

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

            genCodeLine (sPrefix + "            if (specialToken == null)");
            genCodeLine (sPrefix + "               specialToken = matchedToken;");
            genCodeLine (sPrefix + "            else");
            genCodeLine (sPrefix + "            {");
            genCodeLine (sPrefix + "               matchedToken.specialToken = specialToken;");
            genCodeLine (sPrefix + "               specialToken = (specialToken.next = matchedToken);");
            genCodeLine (sPrefix + "            }");

            if (aLexer.isHasSkipActions ())
              genCodeLine (sPrefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (sPrefix + "         }");

            if (aLexer.isHasSkipActions ())
            {
              genCodeLine (sPrefix + "         else");
              genCodeLine (sPrefix + "            SkipLexicalActions(null);");
            }
          }
          else
            if (aLexer.isHasSkipActions ())
              genCodeLine (sPrefix + "         SkipLexicalActions(null);");

          if (aLexer.getMaxLexStates () > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (sPrefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (sPrefix + "         continue EOFLoop;");
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

          genCodeLine (sPrefix + "      try {");
          genCodeLine (sPrefix + "         curChar = input_stream.readChar();");

          if (Options.isDebugTokenManager ())
            genCodeLine ("   debugStream.println(" +
                         (aLexer.getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                         "\"Current character : \" + " +
                         aLexer.getErrorHandlingClass () +
                         ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                         "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          genCodeLine (sPrefix + "         continue;");
          genCodeLine (sPrefix + "      }");
          genCodeLine (sPrefix + "      catch (final java.io.IOException e1) { }");
        }
      }

      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix + "   int error_line = input_stream.getEndLine();");
      genCodeLine (sPrefix + "   int error_column = input_stream.getEndColumn();");
      genCodeLine (sPrefix + "   String error_after = null;");
      genCodeLine (sPrefix + "   " + eOutputLanguage.getTypeBoolean () + " EOFSeen = false;");
      genCodeLine (sPrefix + "   try {");
      genCodeLine (sPrefix + "     input_stream.readChar();");
      genCodeLine (sPrefix + "     input_stream.backup(1);");
      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix + "   catch (final java.io.IOException e1) {");
      genCodeLine (sPrefix + "      EOFSeen = true;");
      genCodeLine (sPrefix + "      error_after = curPos <= 1 ? \"\" : input_stream.getImage();");
      genCodeLine (sPrefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine (sPrefix + "         error_line++;");
      genCodeLine (sPrefix + "         error_column = 0;");
      genCodeLine (sPrefix + "      }");
      genCodeLine (sPrefix + "      else");
      genCodeLine (sPrefix + "         error_column++;");
      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix + "   if (!EOFSeen) {");
      genCodeLine (sPrefix + "      input_stream.backup(1);");
      genCodeLine (sPrefix + "      error_after = curPos <= 1 ? \"\" : input_stream.getImage();");
      genCodeLine (sPrefix + "   }");
      genCodeLine (sPrefix +
                   "   throw new " +
                   aLexer.getErrorHandlingClass () +
                   "(" +
                   "EOFSeen, curLexState, error_line, error_column, error_after, curChar, " +
                   aLexer.getErrorHandlingClass () +
                   ".LEXICAL_ERROR);");
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

    genCodeLine ("void SkipLexicalActions(Token matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    final LexerState aLexer = lexer ();
    Outer: for (int i = 0; i < aLexer.getMaxOrdinal (); i++)
    {
      if ((aLexer.getToSkip ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        aAct = aLexer.getActions ()[i];
        if ((aAct == null || aAct.getActionTokens ().isEmpty ()) &&
            !aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (aLexer.getInitMatch ()[aLexer.getLexStates ()[i]] == i &&
            aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + aLexer.getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       aLexer.getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       aLexer.getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + aLexer.getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + aLexer.getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        aAct = aLexer.getActions ()[i];
        if (aAct == null || aAct.getActionTokens ().isEmpty ())
          break;

        genCode ("         image.append");
        if (ExpRStringLiteral.strLit ().getAllImages ()[i] != null)
        {
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
          genCodeLine ("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
        }
        else
        {
          genCodeLine ("(input_stream.getSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
        }

        printTokenSetup (aAct.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : aAct.getActionTokens ())
          printToken (t);
        genCodeNewLine ();

        break;
      }

      genCodeLine ("         break;");
    }

    genCodeLine ("      default :");
    genCodeLine ("         break;");
    genCodeLine ("   }");
    genCodeLine ("}");
  }

  private void _dumpMoreActions ()
  {
    ExpAction aAct;

    genCodeLine ("void MoreLexicalActions()");
    genCodeLine ("{");
    genCodeLine ("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    final LexerState aLexer = lexer ();
    Outer: for (int i = 0; i < aLexer.getMaxOrdinal (); i++)
    {
      if ((aLexer.getToMore ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        aAct = aLexer.getActions ()[i];
        if ((aAct == null || aAct.getActionTokens ().isEmpty ()) &&
            !aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (aLexer.getInitMatch ()[aLexer.getLexStates ()[i]] == i &&
            aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + aLexer.getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       aLexer.getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       aLexer.getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + aLexer.getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + aLexer.getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        aAct = aLexer.getActions ()[i];
        if (aAct == null || aAct.getActionTokens ().isEmpty ())
        {
          break;
        }

        genCode ("         image.append");

        if (ExpRStringLiteral.strLit ().getAllImages ()[i] != null)
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
        else
          genCodeLine ("(input_stream.getSuffix(jjimageLen));");

        genCodeLine ("         jjimageLen = 0;");
        printTokenSetup (aAct.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : aAct.getActionTokens ())
          printToken (t);
        genCodeNewLine ();

        break;
      }

      genCodeLine ("         break;");
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

    genCodeLine ("void TokenLexicalActions(Token matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    final LexerState aLexer = lexer ();
    Outer: for (i = 0; i < aLexer.getMaxOrdinal (); i++)
    {
      if ((aLexer.getToToken ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        aAct = aLexer.getActions ()[i];
        if ((aAct == null || aAct.getActionTokens ().isEmpty ()) &&
            !aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (aLexer.getInitMatch ()[aLexer.getLexStates ()[i]] == i &&
            aLexer.getCanLoop ()[aLexer.getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + aLexer.getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       aLexer.getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       aLexer.getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + aLexer.getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       aLexer.getLexStates ()[i] +
                       "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + aLexer.getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        aAct = aLexer.getActions ()[i];
        if (aAct == null || aAct.getActionTokens ().isEmpty ())
          break;

        if (i == 0)
        {
          // For EOF no image is there
          genCodeLine ("      image.setLength(0);");
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
            genCodeLine ("(input_stream.getSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
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
    }

    genCodeLine ("      default :");
    genCodeLine ("         break;");
    genCodeLine ("   }");
    genCodeLine ("}");
  }

}
