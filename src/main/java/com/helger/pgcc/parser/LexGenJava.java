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

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.context.LexerState;
import com.helger.pgcc.context.PGCCContext;

import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
public class LexGenJava extends CodeGenerator
{
  /**
   * @return The token manager generation state of the current run. Never <code>null</code>. This
   *         replaces the 43 static fields this class used to keep.
   */
  @NonNull
  public static LexerState lexer ()
  {
    return PGCCContext.current ().lexer ();
  }

  private static final String DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL = "/templates/java/DumpStaticVarDeclarations.template";
  private static final String DUMP_DEBUG_METHODS_TEMPLATE_RESOURCE_URL = "/templates/java/DumpDebugMethods.template";
  private static final String BOILERPLATER_METHOD_RESOURCE_URL = "/templates/java/TokenManagerBoilerPlateMethods.template";

  // Order is important!
  // Order is important!

  private void _printClassHead ()
  {
    final List <String> tn = new ArrayList <> (grammar ().getToolNameList ());
    tn.add (CPG.APP_NAME);
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    genCodeLine ("/* " + getIdString (tn, lexer ().getTokenMgrClassName () + getFileExtension ()) + " */");

    boolean bHasImport = false;
    int nIndex = 0;
    int i = 1;
    for (;;)
    {
      if (grammar ().cuToInsertionPoint1 ().size () <= nIndex)
        break;

      int nKind = grammar ().cuToInsertionPoint1 ().get (nIndex).kind;
      if (nKind == JavaCCParserConstants.PACKAGE || nKind == JavaCCParserConstants.IMPORT)
      {
        if (nKind == JavaCCParserConstants.IMPORT)
          bHasImport = true;

        for (; i < grammar ().cuToInsertionPoint1 ().size (); i++)
        {
          nKind = grammar ().cuToInsertionPoint1 ().get (i).kind;
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
            setLineAndCol (grammar ().cuToInsertionPoint1 ().get (nIndex).beginLine,
                           grammar ().cuToInsertionPoint1 ().get (nIndex).beginColumn);
            int j = nIndex;
            for (; j < i; j++)
            {
              printToken (grammar ().cuToInsertionPoint1 ().get (j));
            }
            if (nKind == JavaCCParserConstants.SEMICOLON)
              printToken (grammar ().cuToInsertionPoint1 ().get (j));
            genCodeNewLine ();
            break;
          }
        }
        ++i;
        nIndex = i;
      }
      else
        break;
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
    genClassStart (null, lexer ().getTokenMgrClassName (), new String [] {}, new String [] { grammar ().getParserName () + "Constants" });
    // genCodeLine("{"); // }

    if (grammar ().getTokenMgrDecls () != null && grammar ().getTokenMgrDecls ().isNotEmpty ())
    {
      boolean bCommonTokenActionSeen = false;
      final boolean bCommonTokenActionNeeded = Options.isCommonTokenAction ();
      Token t = grammar ().getTokenMgrDecls ().getFirstOrNull ();

      printTokenSetup (t);
      setColToStart ();

      for (final Token s_token_mgr_decl : grammar ().getTokenMgrDecls ())
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
      genCodeLine ("  public " + grammar ().getParserName () + " parser = null;");
    }
  }

  @Override
  public void writeTemplate (final String name, final Map <String, Object> additionalOptions) throws IOException
  {
    final Map <String, Object> options = Options.getAllOptions ();
    options.put ("maxOrdinal", Integer.valueOf (lexer ().getMaxOrdinal ()));
    options.put ("maxLexStates", Integer.valueOf (lexer ().getMaxLexStates ()));
    options.put ("hasEmptyMatch", Boolean.valueOf (lexer ().isHasEmptyMatch ()));
    options.put ("hasSkip", Boolean.valueOf (lexer ().isHasSkip ()));
    options.put ("hasMore", Boolean.valueOf (lexer ().isHasMore ()));
    options.put ("hasSpecial", Boolean.valueOf (lexer ().isHasSpecial ()));
    options.put ("hasMoreActions", Boolean.valueOf (lexer ().isHasMoreActions ()));
    options.put ("hasSkipActions", Boolean.valueOf (lexer ().isHasSkipActions ()));
    options.put ("hasTokenActions", Boolean.valueOf (lexer ().isHasTokenActions ()));
    options.put ("stateSetSize", Integer.valueOf (lexer ().getStateSetSize ()));
    options.put ("hasActions", Boolean.valueOf (lexer ().isHasMoreActions () || lexer ().isHasSkipActions () || lexer ().isHasTokenActions ()));
    options.put ("tokMgrClassName", lexer ().getTokenMgrClassName ());
    int x = 0;
    for (final int l : lexer ().getMaxLongsReqd ())
      x = Math.max (x, l);
    options.put ("maxLongs", Integer.valueOf (x));
    options.put ("cu_name", grammar ().getParserName ());

    // options.put("", .valueOf(maxOrdinal));
    if (additionalOptions != null)
      options.putAll (additionalOptions);

    super.writeTemplate (name, options);
  }

  private void _dumpDebugMethods () throws IOException
  {
    writeTemplate (DUMP_DEBUG_METHODS_TEMPLATE_RESOURCE_URL, null);
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

      if (respecs == null || respecs.isEmpty ())
        continue;

      AbstractExpRegularExpression re;
      for (i = 0; i < respecs.size (); i++)
        if (lexer ().getMaxOrdinal () <= (re = respecs.get (i).rexp).getOrdinal ())
          lexer ().setMaxOrdinal (re.getOrdinal () + 1);
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
    ExpRStringLiteral.s_allImages = new String [lexer ().getMaxOrdinal ()];
    lexer ().setCanReachOnMore (new boolean [lexer ().getMaxLexStates ()]);
  }

  private static int _getIndex (final String name)
  {
    for (int i = 0; i < lexer ().getLexStateName ().length; i++)
      if (lexer ().getLexStateName ()[i] != null && lexer ().getLexStateName ()[i].equals (name))
        return i;

    throw new IllegalStateException ("Should never come here");
  }

  public static void addCharToSkip (final char c, final int kind)
  {
    lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].addChar (c);
    lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].m_kind = kind;
  }

  public void start () throws IOException
  {
    if (!Options.isBuildTokenManager () || Options.isUserTokenManager () || JavaCCErrors.getErrorCount () > 0)
      return;

    lexer ().setKeepLineCol (Options.isKeepLineColumn ());
    lexer ().setErrorHandlingClass (Options.getTokenMgrErrorClass ());
    final List <ExpRChoice> choices = new ArrayList <> ();

    lexer ().setTokenMgrClassName (grammar ().getParserName () + "TokenManager");

    if (!lexer ().isGenerateDataOnly ())
      _printClassHead ();
    _buildLexStatesTable ();

    boolean ignoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : lexer ().allTpsForState ().entrySet ())
    {
      int startState = -1;
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

          if (!Options.isNoDfa () &&
              lexer ().getCurRE () instanceof ExpRStringLiteral &&
              StringHelper.isNotEmpty (((ExpRStringLiteral) lexer ().getCurRE ()).m_image))
          {
            ((ExpRStringLiteral) lexer ().getCurRE ()).generateDfa ();
            if (i != 0 && !lexer ().getMixed ()[lexer ().getLexStateIndex ()] && ignoring != ignore)
            {
              lexer ().getMixed ()[lexer ().getLexStateIndex ()] = true;
            }
          }
          else
            if (lexer ().getCurRE ().canMatchAnyChar ())
            {
              if (lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()] == -1 ||
                  lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()] > lexer ().getCurRE ().getOrdinal ())
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

          if (respec.act != null && respec.act.getActionTokens ().isNotEmpty ())
            lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] = respec.act;

          switch (kind)
          {
            case SPECIAL:
              lexer ().setHasSkipActions (lexer ().isHasSkipActions () | (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null) ||
                                  (lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] != null));
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

      lexer ().getHasNfa ()[lexer ().getLexStateIndex ()] = (NfaState.s_generatedStates != 0);
      if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()])
      {
        lexer ().getInitialState ().generateCode ();
        startState = lexer ().getInitialState ().generateInitMoves ();
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

      if (lexer ().isGenerateDataOnly ())
      {
        ExpRStringLiteral.updateStringLiteralData (lexer ().getLexStateIndex ());
        NfaState.updateNfaData (lexer ().getTotalNumStates (), startState, lexer ().getLexStateIndex (), lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()]);
      }
      else
      {
        ExpRStringLiteral.dumpDfaCode (this);
        if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()])
        {
          NfaState.dumpMoveNfa (this);
        }
      }
      lexer ().setTotalNumStates (lexer ().getTotalNumStates () + NfaState.s_generatedStates);
      if (lexer ().getStateSetSize () < NfaState.s_generatedStates)
        lexer ().setStateSetSize (NfaState.s_generatedStates);
    }

    for (final ExpRChoice aItem : choices)
      aItem.checkUnmatchability ();

    checkEmptyStringMatch ();

    if (lexer ().isGenerateDataOnly ())
    {
      lexer ().getTokenizerData ().setParserName (grammar ().getParserName ());
      NfaState.buildTokenizerData (lexer ().getTokenizerData ());
      ExpRStringLiteral.BuildTokenizerData (lexer ().getTokenizerData ());
      final int [] newLexStateIndices = new int [lexer ().getMaxOrdinal ()];

      final StringBuilder tokenMgrDecls = new StringBuilder ();
      if (grammar ().getTokenMgrDecls () != null)
        for (final Token t : grammar ().getTokenMgrDecls ())
          tokenMgrDecls.append (t.image).append (' ');
      lexer ().getTokenizerData ().setDecls (tokenMgrDecls.toString ());

      final Map <Integer, String> actionStrings = new HashMap <> ();
      for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
      {
        if (lexer ().getNewLexState ()[i] == null)
        {
          newLexStateIndices[i] = -1;
        }
        else
        {
          newLexStateIndices[i] = _getIndex (lexer ().getNewLexState ()[i]);
        }
        // For java, we have this but for other languages, eventually we will
        // simply have a string.
        final ExpAction act = lexer ().getActions ()[i];
        if (act == null)
          continue;

        final StringBuilder sb = new StringBuilder ();
        for (final Token t : act.getActionTokens ())
          sb.append (t.image).append (' ');
        actionStrings.put (Integer.valueOf (i), sb.toString ());
      }
      lexer ().getTokenizerData ().setDefaultLexState (lexer ().getDefaultLexState ());
      lexer ().getTokenizerData ().setLexStateNames (lexer ().getLexStateName ());
      lexer ().getTokenizerData ().updateMatchInfo (actionStrings, newLexStateIndices, lexer ().getToSkip (), lexer ().getToSpecial (), lexer ().getToMore (), lexer ().getToToken ());
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

    if (lexer ().isHasLoop ())
    {
      genCodeLine ("int[] jjemptyLineNo = new int[" + lexer ().getMaxLexStates () + "];");
      genCodeLine ("int[] jjemptyColNo = new int[" + lexer ().getMaxLexStates () + "];");
      genCodeLine (eOutputLanguage.getTypeBoolean () +
                   "[] jjbeenHere = new " +
                   eOutputLanguage.getTypeBoolean () +
                   "[" +
                   lexer ().getMaxLexStates () +
                   "];");
    }

    _dumpSkipActions ();
    _dumpMoreActions ();
    _dumpTokenActions ();

    NfaState.printBoilerPlateJava (this);

    final String charStreamName = CodeGenerator.getCharStreamName ();

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", charStreamName);
    aOpts.put ("lexStateNameLength", Integer.toString (lexer ().getLexStateName ().length));
    aOpts.put ("defaultLexState", Integer.toString (lexer ().getDefaultLexState ()));
    aOpts.put ("noDfa", Boolean.toString (Options.isNoDfa ()));
    aOpts.put ("generatedStates", Integer.toString (lexer ().getTotalNumStates ()));
    writeTemplate (BOILERPLATER_METHOD_RESOURCE_URL, aOpts);

    _dumpStaticVarDeclarations (charStreamName);
    genCodeLine (/* { */ "}");

    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String fileName = Options.getOutputDirectory () + File.separator + lexer ().getTokenMgrClassName () + getFileExtension ();

    if (Options.isBuildParser ())
    {
      saveOutput (fileName);
    }
  }

  protected static void checkEmptyStringMatch ()
  {
    final boolean [] seen = new boolean [lexer ().getMaxLexStates ()];
    final boolean [] done = new boolean [lexer ().getMaxLexStates ()];

    Outer: for (int i = 0; i < lexer ().getMaxLexStates (); i++)
    {
      if (done[i] || lexer ().getInitMatch ()[i] == 0 || lexer ().getInitMatch ()[i] == Integer.MAX_VALUE || lexer ().getCanMatchAnyChar ()[i] != -1)
        continue;

      done[i] = true;
      int len = 0;
      String cycle = "";
      String reList = "";

      for (int k = 0; k < lexer ().getMaxLexStates (); k++)
        seen[k] = false;

      int j = i;
      seen[i] = true;
      cycle += lexer ().getLexStateName ()[j] + "-->";
      while (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]] != null)
      {
        cycle += lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]];
        if (seen[j = _getIndex (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]])])
          break;

        cycle += "-->";
        done[j] = true;
        seen[j] = true;
        if (lexer ().getInitMatch ()[j] == 0 || lexer ().getInitMatch ()[j] == Integer.MAX_VALUE || lexer ().getCanMatchAnyChar ()[j] != -1)
          continue Outer;
        if (len != 0)
          reList += "; ";
        reList += "line " + lexer ().getRexprs ()[lexer ().getInitMatch ()[j]].getLine () + ", column " + lexer ().getRexprs ()[lexer ().getInitMatch ()[j]].getColumn ();
        len++;
      }

      if (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]] == null)
        cycle += lexer ().getLexStateName ()[lexer ().getLexStates ()[lexer ().getInitMatch ()[j]]];

      for (int k = 0; k < lexer ().getMaxLexStates (); k++)
        lexer ().getCanLoop ()[k] |= seen[k];

      lexer ().setHasLoop (true);
      final String sLabel = lexer ().getRexprs ()[lexer ().getInitMatch ()[i]].getLabel ();
      if (len == 0)
      {
        JavaCCErrors.warning (lexer ().getRexprs ()[lexer ().getInitMatch ()[i]],
                              "Regular expression" +
                                                        (StringHelper.isEmpty (sLabel) ? "" : " for " + sLabel) +
                                                        " can be matched by the empty string (\"\") in lexical state " +
                                                        lexer ().getLexStateName ()[i] +
                                                        ". This can result in an endless loop of " +
                                                        "empty string matches.");
      }
      else
      {
        JavaCCErrors.warning (lexer ().getRexprs ()[lexer ().getInitMatch ()[i]],
                              "Regular expression" +
                                                        (StringHelper.isEmpty (sLabel) ? "" : " for " + sLabel) +
                                                        " can be matched by the empty string (\"\") in lexical state " +
                                                        lexer ().getLexStateName ()[i] +
                                                        ". This regular expression along with the " +
                                                        "regular expressions at " +
                                                        reList +
                                                        " forms the cycle \n   " +
                                                        cycle +
                                                        "\ncontaining regular expressions with empty matches." +
                                                        " This can result in an endless loop of empty string matches.");
      }
    }
  }

  private void _dumpStaticVarDeclarations (final String charStreamName) throws IOException
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();

    genCodeNewLine ();
    genCodeLine ("/** Lexer state names. */");
    genCodeLine ("public static final String[] lexStateNames = {");
    for (int i = 0; i < lexer ().getMaxLexStates (); i++)
      genCodeLine ("   \"" + lexer ().getLexStateName ()[i] + "\",");
    genCodeLine ("};");

    {
      genCodeNewLine ();
      genCodeLine ("/** Lex State array. */");
      genCode ("public static final int[] jjnewLexState = {");

      for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
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

    {
      // Bit vector for TOKEN
      genCode ("static final long[] jjtoToken = {");
      for (int i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToToken ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for SKIP
      genCode ("static final long[] jjtoSkip = {");
      for (int i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToSkip ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for SPECIAL
      genCode ("static final long[] jjtoSpecial = {");
      for (int i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToSpecial ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for MORE
      genCode ("static final long[] jjtoMore = {");
      for (int i = 0; i < lexer ().getMaxOrdinal () / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (lexer ().getToMore ()[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", charStreamName);
    aOpts.put ("protected", "protected");
    aOpts.put ("private", "private");
    aOpts.put ("final", "final");
    aOpts.put ("lexStateNameLength", Integer.toString (lexer ().getLexStateName ().length));
    writeTemplate (DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL, aOpts);
  }

  // Assumes l != 0L
  protected static char maxChar (final long l)
  {
    for (int i = 64; i-- > 0;)
      if ((l & (1L << i)) != 0L)
        return (char) i;

    return 0xffff;
  }

  private void _dumpFillToken ()
  {
    final double tokenVersion = OutputHelper.getVersionDashStar ("Token.java");
    final boolean hasBinaryNewToken = tokenVersion > 4.09;

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
      if (hasBinaryNewToken)
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
    genCodeLine ("int curLexState = " + lexer ().getDefaultLexState () + ";");
    genCodeLine ("int defaultLexState = " + lexer ().getDefaultLexState () + ";");
    genCodeLine ("int jjnewStateCnt;");
    genCodeLine ("int jjround;");
    genCodeLine ("int jjmatchedPos;");
    genCodeLine ("int jjmatchedKind;");
    genCodeNewLine ();
    genCodeLine ("/** Get the next Token. */");
    genCodeLine ("public " + "Token getNextToken()" + " ");
    genCodeLine ("{");
    if (lexer ().isHasSpecial ())
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

    if (lexer ().isHasSpecial ())
      genCodeLine ("      matchedToken.specialToken = specialToken;");

    if (grammar ().getNextStateForEof () != null || grammar ().getActionForEof () != null)
      genCodeLine ("      TokenLexicalActions(matchedToken);");

    if (Options.isCommonTokenAction ())
      genCodeLine ("      CommonTokenAction(matchedToken);");

    genCodeLine ("      return matchedToken;");
    genCodeLine ("   }");

    if (lexer ().isHasMoreActions () || lexer ().isHasSkipActions () || lexer ().isHasTokenActions ())
    {
      genCodeLine ("   image = jjimage;");
      genCodeLine ("   image.setLength(0);");
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
        genCodeLine (prefix + "try {");
        genCodeLine (prefix + "  input_stream.backup(0);");
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

        if (Options.isDebugTokenManager ())
        {
          genCodeLine (prefix + "{");
          genCodeLine ("      debugStream.println(" +
                       (lexer ().getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                       "\"Skipping character : \" + " +
                       lexer ().getErrorHandlingClass () +
                       ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \")\");");
        }
        genCodeLine (prefix + "      curChar = input_stream.beginToken();");

        if (Options.isDebugTokenManager ())
          genCodeLine (prefix + "}");

        genCodeLine (prefix + "}");
        genCodeLine (prefix + "catch (final java.io.IOException e1) {");
        genCodeLine (prefix + "  continue EOFLoop;");
        genCodeLine (prefix + "}");
      }

      if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      debugStream.println(\"   Matched the empty string as \" + tokenImage[" +
                       lexer ().getInitMatch ()[i] +
                       "] + \" token.\");");

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
        genCodeLine ("      debugStream.println(" +
                     (lexer ().getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                     "\"Current character : \" + " +
                     lexer ().getErrorHandlingClass () +
                     ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      genCodeLine (prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (lexer ().getCanMatchAnyChar ()[i] != -1)
      {
        if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
        {
          genCodeLine (prefix +
                       "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
                       lexer ().getCanMatchAnyChar ()[i] +
                       "))");
        }
        else
          genCodeLine (prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + lexer ().getCanMatchAnyChar ()[i] + ")");
        genCodeLine (prefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           debugStream.println(\"   Current character matched as a \" + tokenImage[" +
                       lexer ().getCanMatchAnyChar ()[i] +
                       "] + \" token.\");");
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
                     "         debugStream.println(" +
                     "\"   Putting back \" + (curPos - jjmatchedPos - 1) + \" characters into the input stream.\");");
      }

      genCodeLine (prefix + "         input_stream.backup(curPos - jjmatchedPos - 1);");

      if (Options.isDebugTokenManager ())
        genCodeLine (prefix + "      }");

      if (Options.isDebugTokenManager ())
      {
        if (Options.isJavaUnicodeEscape () || Options.isJavaUserCharStream ())
        {
          genCodeLine ("    debugStream.println(" +
                       "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                       "(\" + " +
                       lexer ().getErrorHandlingClass () +
                       ".addEscapes(new String(input_stream.getSuffix(jjmatchedPos + 1))) + " +
                       "\") ******\\n\");");
        }
        else
        {
          genCodeLine ("    debugStream.println(" +
                       "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                       "(\" + " +
                       lexer ().getErrorHandlingClass () +
                       ".addEscapes(new String(input_stream.getSuffix(jjmatchedPos + 1))) + " +
                       "\") ******\\n\");");
        }
      }

      if (lexer ().isHasSkip () || lexer ().isHasMore () || lexer ().isHasSpecial ())
      {
        genCodeLine (prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (prefix + "      {");
      }

      genCodeLine (prefix + "         matchedToken = jjFillToken();");

      if (lexer ().isHasSpecial ())
        genCodeLine (prefix + "         matchedToken.specialToken = specialToken;");

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
            genCodeLine (prefix +
                         "      else if ((jjtoSkip[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
          }
          else
            genCodeLine (prefix + "      else");

          genCodeLine (prefix + "      {");

          if (lexer ().isHasSpecial ())
          {
            genCodeLine (prefix +
                         "         if ((jjtoSpecial[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
            genCodeLine (prefix + "         {");

            genCodeLine (prefix + "            matchedToken = jjFillToken();");

            genCodeLine (prefix + "            if (specialToken == null)");
            genCodeLine (prefix + "               specialToken = matchedToken;");
            genCodeLine (prefix + "            else");
            genCodeLine (prefix + "            {");
            genCodeLine (prefix + "               matchedToken.specialToken = specialToken;");
            genCodeLine (prefix + "               specialToken = (specialToken.next = matchedToken);");
            genCodeLine (prefix + "            }");

            if (lexer ().isHasSkipActions ())
              genCodeLine (prefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (prefix + "         }");

            if (lexer ().isHasSkipActions ())
            {
              genCodeLine (prefix + "         else");
              genCodeLine (prefix + "            SkipLexicalActions(null);");
            }
          }
          else
            if (lexer ().isHasSkipActions ())
              genCodeLine (prefix + "         SkipLexicalActions(null);");

          if (lexer ().getMaxLexStates () > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (prefix + "         continue EOFLoop;");
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

          genCodeLine (prefix + "      try {");
          genCodeLine (prefix + "         curChar = input_stream.readChar();");

          if (Options.isDebugTokenManager ())
            genCodeLine ("   debugStream.println(" +
                         (lexer ().getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                         "\"Current character : \" + " +
                         lexer ().getErrorHandlingClass () +
                         ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                         "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          genCodeLine (prefix + "         continue;");
          genCodeLine (prefix + "      }");
          genCodeLine (prefix + "      catch (final java.io.IOException e1) { }");
        }
      }

      genCodeLine (prefix + "   }");
      genCodeLine (prefix + "   int error_line = input_stream.getEndLine();");
      genCodeLine (prefix + "   int error_column = input_stream.getEndColumn();");
      genCodeLine (prefix + "   String error_after = null;");
      genCodeLine (prefix + "   " + eOutputLanguage.getTypeBoolean () + " EOFSeen = false;");
      genCodeLine (prefix + "   try {");
      genCodeLine (prefix + "     input_stream.readChar();");
      genCodeLine (prefix + "     input_stream.backup(1);");
      genCodeLine (prefix + "   }");
      genCodeLine (prefix + "   catch (final java.io.IOException e1) {");
      genCodeLine (prefix + "      EOFSeen = true;");
      genCodeLine (prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.getImage();");
      genCodeLine (prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine (prefix + "         error_line++;");
      genCodeLine (prefix + "         error_column = 0;");
      genCodeLine (prefix + "      }");
      genCodeLine (prefix + "      else");
      genCodeLine (prefix + "         error_column++;");
      genCodeLine (prefix + "   }");
      genCodeLine (prefix + "   if (!EOFSeen) {");
      genCodeLine (prefix + "      input_stream.backup(1);");
      genCodeLine (prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.getImage();");
      genCodeLine (prefix + "   }");
      genCodeLine (prefix +
                   "   throw new " +
                   lexer ().getErrorHandlingClass () +
                   "(" +
                   "EOFSeen, curLexState, error_line, error_column, error_after, curChar, " +
                   lexer ().getErrorHandlingClass () +
                   ".LEXICAL_ERROR);");
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

    genCodeLine ("void SkipLexicalActions(Token matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        act = lexer ().getActions ()[i];
        if ((act == null || act.getActionTokens ().isEmpty ()) && !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i && lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + lexer ().getLexStates ()[i] + "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       lexer ().getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       lexer ().getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        act = lexer ().getActions ()[i];
        if (act == null || act.getActionTokens ().isEmpty ())
          break;

        genCode ("         image.append");
        if (ExpRStringLiteral.s_allImages[i] != null)
        {
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
          genCodeLine ("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
        }
        else
        {
          genCodeLine ("(input_stream.getSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
        }

        printTokenSetup (act.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : act.getActionTokens ())
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
    ExpAction act;

    genCodeLine ("void MoreLexicalActions()");
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
        act = lexer ().getActions ()[i];
        if ((act == null || act.getActionTokens ().isEmpty ()) && !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i && lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + lexer ().getLexStates ()[i] + "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       lexer ().getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       lexer ().getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        act = lexer ().getActions ()[i];
        if (act == null || act.getActionTokens ().isEmpty ())
        {
          break;
        }

        genCode ("         image.append");

        if (ExpRStringLiteral.s_allImages[i] != null)
          genCodeLine ("(jjstrLiteralImages[" + i + "]);");
        else
          genCodeLine ("(input_stream.getSuffix(jjimageLen));");

        genCodeLine ("         jjimageLen = 0;");
        printTokenSetup (act.getActionTokens ().get (0));
        setColToStart ();

        for (final Token t : act.getActionTokens ())
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
    ExpAction act;
    int i;

    genCodeLine ("void TokenLexicalActions(Token matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        act = lexer ().getActions ()[i];
        if ((act == null || act.getActionTokens ().isEmpty ()) && !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i && lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + lexer ().getLexStates ()[i] + "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       lexer ().getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       lexer ().getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        act = lexer ().getActions ()[i];
        if (act == null || act.getActionTokens ().isEmpty ())
          break;

        if (i == 0)
        {
          // For EOF no image is there
          genCodeLine ("      image.setLength(0);");
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
            genCodeLine ("(input_stream.getSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
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
    }

    genCodeLine ("      default :");
    genCodeLine ("         break;");
    genCodeLine ("   }");
    genCodeLine ("}");
  }

}
