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

import org.jspecify.annotations.Nullable;

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

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.context.LexerState;
import com.helger.pgcc.context.PGCCContext;
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
public class LexGenJava extends AbstractCodeGenerator
{
  /** Default constructor. */
  public LexGenJava ()
  {}

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
    final List <String> aTn = new ArrayList <> (grammar ().getToolNameList ());
    aTn.add (CPG.APP_NAME);
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    genCodeLine ("/* " + getIdString (aTn, lexer ().getTokenMgrClassName () + getFileExtension ()) + " */");

    boolean bHasImport = false;
    int nIndex = 0;
    int i = 1;
    for (;;)
    {
      if (grammar ().cuToInsertionPoint1 ().size () <= nIndex)
        break;

      int nKind = grammar ().cuToInsertionPoint1 ().get (nIndex).kind;
      if ((nKind != JavaCCParserConstants.PACKAGE) && (nKind != JavaCCParserConstants.IMPORT))
        break;
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
                   new String [] { grammar ().getParserName () + "Constants" });
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
  public void writeTemplate (final String sName, @Nullable final Map <String, Object> aAdditionalOptions)
                                                                                                          throws IOException
  {
    final Map <String, Object> aOptions = Options.getAllOptions ();
    aOptions.put ("maxOrdinal", Integer.valueOf (lexer ().getMaxOrdinal ()));
    aOptions.put ("maxLexStates", Integer.valueOf (lexer ().getMaxLexStates ()));
    aOptions.put ("hasEmptyMatch", Boolean.valueOf (lexer ().isHasEmptyMatch ()));
    aOptions.put ("hasSkip", Boolean.valueOf (lexer ().isHasSkip ()));
    aOptions.put ("hasMore", Boolean.valueOf (lexer ().isHasMore ()));
    aOptions.put ("hasSpecial", Boolean.valueOf (lexer ().isHasSpecial ()));
    aOptions.put ("hasMoreActions", Boolean.valueOf (lexer ().isHasMoreActions ()));
    aOptions.put ("hasSkipActions", Boolean.valueOf (lexer ().isHasSkipActions ()));
    aOptions.put ("hasTokenActions", Boolean.valueOf (lexer ().isHasTokenActions ()));
    aOptions.put ("stateSetSize", Integer.valueOf (lexer ().getStateSetSize ()));
    aOptions.put ("hasActions",
                  Boolean.valueOf (lexer ().isHasMoreActions () ||
                                   lexer ().isHasSkipActions () ||
                                   lexer ().isHasTokenActions ()));
    aOptions.put ("tokMgrClassName", lexer ().getTokenMgrClassName ());
    int x = 0;
    for (final int l : lexer ().getMaxLongsReqd ())
      x = Math.max (x, l);
    aOptions.put ("maxLongs", Integer.valueOf (x));
    aOptions.put ("cu_name", grammar ().getParserName ());

    // options.put("", .valueOf(maxOrdinal));
    if (aAdditionalOptions != null)
      aOptions.putAll (aAdditionalOptions);

    super.writeTemplate (sName, aOptions);
  }

  private void _dumpDebugMethods () throws IOException
  {
    writeTemplate (DUMP_DEBUG_METHODS_TEMPLATE_RESOURCE_URL, null);
  }

  private static void _buildLexStatesTable ()
  {
    final Iterator <TokenProduction> aIt = grammar ().rexprList ().iterator ();
    TokenProduction aTp;
    int i;

    final String [] aTmpLexStateName = new String [grammar ().lexStateI2S ().size ()];
    while (aIt.hasNext ())
    {
      aTp = aIt.next ();
      final List <RegExprSpec> aRespecs = aTp.getRespecs ();
      List <TokenProduction> aTps;

      for (i = 0; i < aTp.getLexStates ().length; i++)
      {
        aTps = lexer ().allTpsForState ().get (aTp.getLexStates ()[i]);
        if (aTps == null)
        {
          aTmpLexStateName[lexer ().getMaxLexStates ()] = aTp.getLexStates ()[i];
          lexer ().setMaxLexStates (lexer ().getMaxLexStates () + 1);
          aTps = new ArrayList <> ();
          lexer ().allTpsForState ().put (aTp.getLexStates ()[i], aTps);
        }

        aTps.add (aTp);
      }

      if (aRespecs == null || aRespecs.isEmpty ())
        continue;

      AbstractExpRegularExpression aRe;
      for (i = 0; i < aRespecs.size (); i++)
        if (lexer ().getMaxOrdinal () <= (aRe = aRespecs.get (i).getRexp ()).getOrdinal ())
          lexer ().setMaxOrdinal (aRe.getOrdinal () + 1);
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
    System.arraycopy (aTmpLexStateName, 0, lexer ().getLexStateName (), 0, lexer ().getMaxLexStates ());

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

  private static int _getIndex (final String sName)
  {
    for (int i = 0; i < lexer ().getLexStateName ().length; i++)
      if (lexer ().getLexStateName ()[i] != null && lexer ().getLexStateName ()[i].equals (sName))
        return i;

    throw new IllegalStateException ("Should never come here");
  }

  public static void addCharToSkip (final char c, final int nKind)
  {
    lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].addChar (c);
    lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].setKind (nKind);
  }

  public void start () throws IOException
  {
    if (!Options.isBuildTokenManager () || Options.isUserTokenManager () || JavaCCErrors.getErrorCount () > 0)
      return;

    lexer ().setKeepLineCol (Options.isKeepLineColumn ());
    lexer ().setErrorHandlingClass (Options.getTokenMgrErrorClass ());
    final List <ExpRChoice> aChoices = new ArrayList <> ();

    lexer ().setTokenMgrClassName (grammar ().getParserName () + "TokenManager");

    if (!lexer ().isGenerateDataOnly ())
      _printClassHead ();
    _buildLexStatesTable ();

    boolean bIgnoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : lexer ().allTpsForState ().entrySet ())
    {
      int nStartState = -1;
      NfaState.reInitStatic ();
      ExpRStringLiteral.reInitStatic ();

      final String sKey = aEntry.getKey ();

      lexer ().setLexStateIndex (_getIndex (sKey));
      lexer ().setLexStateSuffix ("_" + lexer ().getLexStateIndex ());
      final List <TokenProduction> aAllTps = aEntry.getValue ();
      lexer ().setInitialState (new NfaState ());
      lexer ().initStates ().put (sKey, lexer ().getInitialState ());
      bIgnoring = false;

      lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()] = new NfaState ();
      lexer ().getSinglesToSkip ()[lexer ().getLexStateIndex ()].setDummy (true);

      if (sKey.equals ("DEFAULT"))
        lexer ().setDefaultLexState (lexer ().getLexStateIndex ());

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
          lexer ().setCurRE (respec.getRexp ());

          lexer ().setCurKind (lexer ().getCurRE ().getOrdinal ());
          lexer ().getRexprs ()[lexer ().getCurKind ()] = lexer ().getCurRE ();
          lexer ().getLexStates ()[lexer ().getCurRE ().getOrdinal ()] = lexer ().getLexStateIndex ();
          lexer ().getIgnoreCase ()[lexer ().getCurRE ().getOrdinal ()] = bIgnore;

          if (lexer ().getCurRE ().m_bPrivateRexp)
          {
            lexer ().getKinds ()[lexer ().getCurRE ().getOrdinal ()] = null;
            continue;
          }

          if (!Options.isNoDfa () &&
              lexer ().getCurRE () instanceof ExpRStringLiteral &&
              StringHelper.isNotEmpty (((ExpRStringLiteral) lexer ().getCurRE ()).getImage ()))
          {
            ((ExpRStringLiteral) lexer ().getCurRE ()).generateDfa ();
            if (i != 0 && !lexer ().getMixed ()[lexer ().getLexStateIndex ()] && bIgnoring != bIgnore)
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
              Nfa aTemp;

              if (lexer ().getCurRE () instanceof ExpRChoice)
                aChoices.add ((ExpRChoice) lexer ().getCurRE ());

              aTemp = lexer ().getCurRE ().generateNfa (bIgnore);
              aTemp.end ().setFinal (true);
              aTemp.end ().setKind (lexer ().getCurRE ().getOrdinal ());
              lexer ().getInitialState ().addMove (aTemp.start ());
            }

          if (lexer ().getKinds ().length < lexer ().getCurRE ().getOrdinal ())
          {
            final ETokenKind [] aTmp = new ETokenKind [lexer ().getCurRE ().getOrdinal () + 1];

            System.arraycopy (lexer ().getKinds (), 0, aTmp, 0, lexer ().getKinds ().length);
            lexer ().setKinds (aTmp);
          }
          // System.out.println(" ordina : " + curRE.ordinal);

          lexer ().getKinds ()[lexer ().getCurRE ().getOrdinal ()] = eKind;

          if (respec.getNextState () != null &&
              !respec.getNextState ().equals (lexer ().getLexStateName ()[lexer ().getLexStateIndex ()]))
            lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] = respec.getNextState ();

          if (respec.getAct () != null && respec.getAct ().getActionTokens ().isNotEmpty ())
            lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] = respec.getAct ();

          switch (eKind)
          {
            case SPECIAL:
              lexer ().setHasSkipActions (lexer ().isHasSkipActions () |
                                          (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null) ||
                                          (lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().setHasSpecial (true);
              lexer ().getToSpecial ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                   (lexer ().getCurRE ().getOrdinal () %
                                                                                    64);
              lexer ().getToSkip ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                (lexer ().getCurRE ().getOrdinal () %
                                                                                 64);
              break;
            case SKIP:
              lexer ().setHasSkipActions (lexer ().isHasSkipActions () |
                                          (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().setHasSkip (true);
              lexer ().getToSkip ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                (lexer ().getCurRE ().getOrdinal () %
                                                                                 64);
              break;
            case MORE:
              lexer ().setHasMoreActions (lexer ().isHasMoreActions () |
                                          (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().setHasMore (true);
              lexer ().getToMore ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                (lexer ().getCurRE ().getOrdinal () %
                                                                                 64);

              if (lexer ().getNewLexState ()[lexer ().getCurRE ().getOrdinal ()] != null)
                lexer ().getCanReachOnMore ()[_getIndex (lexer ().getNewLexState ()[lexer ().getCurRE ()
                                                                                            .getOrdinal ()])] = true;
              else
                lexer ().getCanReachOnMore ()[lexer ().getLexStateIndex ()] = true;
              break;
            case TOKEN:
              lexer ().setHasTokenActions (lexer ().isHasTokenActions () |
                                           (lexer ().getActions ()[lexer ().getCurRE ().getOrdinal ()] != null));
              lexer ().getToToken ()[lexer ().getCurRE ().getOrdinal () / 64] |= 1L <<
                                                                                 (lexer ().getCurRE ().getOrdinal () %
                                                                                  64);
              break;
            default:
              throw new IllegalStateException ();
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      NfaState.computeClosures ();

      for (final NfaState aItem : lexer ().getInitialState ().getEpsilonMoves ())
        aItem.generateCode ();

      lexer ().getHasNfa ()[lexer ().getLexStateIndex ()] = (NfaState.nfa ().getGeneratedStates () != 0);
      if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()])
      {
        lexer ().getInitialState ().generateCode ();
        nStartState = lexer ().getInitialState ().generateInitMoves ();
      }

      if (lexer ().getInitialState ().getKind () != Integer.MAX_VALUE && lexer ().getInitialState ().getKind () != 0)
      {
        if ((lexer ().getToSkip ()[lexer ().getInitialState ().getKind () / 64] &
             (1L << lexer ().getInitialState ().getKind ())) != 0L ||
            (lexer ().getToSpecial ()[lexer ().getInitialState ().getKind () / 64] &
             (1L << lexer ().getInitialState ().getKind ())) != 0L)
          lexer ().setHasSkipActions (true);
        else
          if ((lexer ().getToMore ()[lexer ().getInitialState ().getKind () / 64] &
               (1L << lexer ().getInitialState ().getKind ())) != 0L)
            lexer ().setHasMoreActions (true);
          else
            lexer ().setHasTokenActions (true);

        if (lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] == 0 ||
            lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] > lexer ().getInitialState ().getKind ())
        {
          lexer ().getInitMatch ()[lexer ().getLexStateIndex ()] = lexer ().getInitialState ().getKind ();
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
        NfaState.updateNfaData (lexer ().getTotalNumStates (),
                                nStartState,
                                lexer ().getLexStateIndex (),
                                lexer ().getCanMatchAnyChar ()[lexer ().getLexStateIndex ()]);
      }
      else
      {
        ExpRStringLiteral.dumpDfaCode (this);
        if (lexer ().getHasNfa ()[lexer ().getLexStateIndex ()])
        {
          NfaState.dumpMoveNfa (this);
        }
      }
      lexer ().setTotalNumStates (lexer ().getTotalNumStates () + NfaState.nfa ().getGeneratedStates ());
      if (lexer ().getStateSetSize () < NfaState.nfa ().getGeneratedStates ())
        lexer ().setStateSetSize (NfaState.nfa ().getGeneratedStates ());
    }

    for (final ExpRChoice aItem : aChoices)
      aItem.checkUnmatchability ();

    checkEmptyStringMatch ();

    if (lexer ().isGenerateDataOnly ())
    {
      lexer ().getTokenizerData ().setParserName (grammar ().getParserName ());
      NfaState.buildTokenizerData (lexer ().getTokenizerData ());
      ExpRStringLiteral.BuildTokenizerData (lexer ().getTokenizerData ());
      final int [] aNewLexStateIndices = new int [lexer ().getMaxOrdinal ()];

      final StringBuilder aTokenMgrDecls = new StringBuilder ();
      if (grammar ().getTokenMgrDecls () != null)
        for (final Token t : grammar ().getTokenMgrDecls ())
          aTokenMgrDecls.append (t.image).append (' ');
      lexer ().getTokenizerData ().setDecls (aTokenMgrDecls.toString ());

      final Map <Integer, String> aActionStrings = new HashMap <> ();
      for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
      {
        if (lexer ().getNewLexState ()[i] == null)
        {
          aNewLexStateIndices[i] = -1;
        }
        else
        {
          aNewLexStateIndices[i] = _getIndex (lexer ().getNewLexState ()[i]);
        }
        // For java, we have this but for other languages, eventually we will
        // simply have a string.
        final ExpAction aAct = lexer ().getActions ()[i];
        if (aAct == null)
          continue;

        final StringBuilder aSB = new StringBuilder ();
        for (final Token t : aAct.getActionTokens ())
          aSB.append (t.image).append (' ');
        aActionStrings.put (Integer.valueOf (i), aSB.toString ());
      }
      lexer ().getTokenizerData ().setDefaultLexState (lexer ().getDefaultLexState ());
      lexer ().getTokenizerData ().setLexStateNames (lexer ().getLexStateName ());
      lexer ().getTokenizerData ()
              .updateMatchInfo (aActionStrings,
                                aNewLexStateIndices,
                                lexer ().getToSkip (),
                                lexer ().getToSpecial (),
                                lexer ().getToMore (),
                                lexer ().getToToken ());
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

    final String sCharStreamName = AbstractCodeGenerator.getCharStreamName ();

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", sCharStreamName);
    aOpts.put ("lexStateNameLength", Integer.toString (lexer ().getLexStateName ().length));
    aOpts.put ("defaultLexState", Integer.toString (lexer ().getDefaultLexState ()));
    aOpts.put ("noDfa", Boolean.toString (Options.isNoDfa ()));
    aOpts.put ("generatedStates", Integer.toString (lexer ().getTotalNumStates ()));
    writeTemplate (BOILERPLATER_METHOD_RESOURCE_URL, aOpts);

    _dumpStaticVarDeclarations (sCharStreamName);
    genCodeLine (/* { */ "}");

    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String sFileName = Options.getOutputDirectory () +
                             File.separator +
                             lexer ().getTokenMgrClassName () +
                             getFileExtension ();

    if (Options.isBuildParser ())
    {
      saveOutput (sFileName);
    }
  }

  protected static void checkEmptyStringMatch ()
  {
    final boolean [] aSeen = new boolean [lexer ().getMaxLexStates ()];
    final boolean [] aDone = new boolean [lexer ().getMaxLexStates ()];

    Outer: for (int i = 0; i < lexer ().getMaxLexStates (); i++)
    {
      if (aDone[i] ||
          lexer ().getInitMatch ()[i] == 0 ||
          lexer ().getInitMatch ()[i] == Integer.MAX_VALUE ||
          lexer ().getCanMatchAnyChar ()[i] != -1)
        continue;

      aDone[i] = true;
      int nLen = 0;
      final StringBuilder aCycle = new StringBuilder ();
      String sReList = "";

      for (int k = 0; k < lexer ().getMaxLexStates (); k++)
        aSeen[k] = false;

      int j = i;
      aSeen[i] = true;
      aCycle.append (lexer ().getLexStateName ()[j]).append ("-->");
      while (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]] != null)
      {
        aCycle.append (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]]);
        if (aSeen[j = _getIndex (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]])])
          break;

        aCycle.append ("-->");
        aDone[j] = true;
        aSeen[j] = true;
        if (lexer ().getInitMatch ()[j] == 0 ||
            lexer ().getInitMatch ()[j] == Integer.MAX_VALUE ||
            lexer ().getCanMatchAnyChar ()[j] != -1)
          continue Outer;
        if (nLen != 0)
          sReList += "; ";
        sReList += "line " +
                   lexer ().getRexprs ()[lexer ().getInitMatch ()[j]].getLineNumber () +
                   ", column " +
                   lexer ().getRexprs ()[lexer ().getInitMatch ()[j]].getColumnNumber ();
        nLen++;
      }

      if (lexer ().getNewLexState ()[lexer ().getInitMatch ()[j]] == null)
        aCycle.append (lexer ().getLexStateName ()[lexer ().getLexStates ()[lexer ().getInitMatch ()[j]]]);

      for (int k = 0; k < lexer ().getMaxLexStates (); k++)
        lexer ().getCanLoop ()[k] |= aSeen[k];

      lexer ().setHasLoop (true);
      final String sLabel = lexer ().getRexprs ()[lexer ().getInitMatch ()[i]].getLabel ();
      if (nLen == 0)
      {
        JavaCCErrors.warning (lexer ().getRexprs ()[lexer ().getInitMatch ()[i]],
                              "Regular expression" +
                                                                                  (StringHelper.isEmpty (sLabel) ? ""
                                                                                                                 : " for " +
                                                                                                                   sLabel) +
                                                                                  " can be matched by the empty string (\"\") in lexical state " +
                                                                                  lexer ().getLexStateName ()[i] +
                                                                                  ". This can result in an endless loop of " +
                                                                                  "empty string matches.");
      }
      else
      {
        JavaCCErrors.warning (lexer ().getRexprs ()[lexer ().getInitMatch ()[i]],
                              "Regular expression" +
                                                                                  (StringHelper.isEmpty (sLabel) ? ""
                                                                                                                 : " for " +
                                                                                                                   sLabel) +
                                                                                  " can be matched by the empty string (\"\") in lexical state " +
                                                                                  lexer ().getLexStateName ()[i] +
                                                                                  ". This regular expression along with the " +
                                                                                  "regular expressions at " +
                                                                                  sReList +
                                                                                  " forms the cycle \n   " +
                                                                                  aCycle.append ("\ncontaining regular expressions with empty matches.")
                                                                                        .append (" This can result in an endless loop of empty string matches.")
                                                                                        .toString ());
      }
    }
  }

  private void _dumpStaticVarDeclarations (final String sCharStreamName) throws IOException
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
    aOpts.put ("charStreamName", sCharStreamName);
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

    String sPrefix = "";
    if (lexer ().isHasMore ())
    {
      genCodeLine ("   for (;;)");
      genCodeLine ("   {");
      sPrefix = "  ";
    }

    String sEndSwitch = "";
    String sCaseStr = "";
    // this also sets up the start state of the nfa
    if (lexer ().getMaxLexStates () > 1)
    {
      genCodeLine (sPrefix + "   switch(curLexState)");
      genCodeLine (sPrefix + "   {");
      sEndSwitch = sPrefix + "   }";
      sCaseStr = sPrefix + "     case ";
      sPrefix += "    ";
    }

    sPrefix += "   ";
    for (int i = 0; i < lexer ().getMaxLexStates (); i++)
    {
      if (lexer ().getMaxLexStates () > 1)
        genCodeLine (sCaseStr + i + ":");

      if (lexer ().getSinglesToSkip ()[i].hasTransitions ())
      {
        // added the backup(0) to make JIT happy
        genCodeLine (sPrefix + "try {");
        genCodeLine (sPrefix + "  input_stream.backup(0);");
        if (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[0] != 0L &&
            lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[1] != 0L)
        {
          genCodeLine (sPrefix +
                       "   while ((curChar < 64" +
                       " && (" +
                       eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[0]) +
                       " & (1L << curChar)) != 0L) || \n" +
                       sPrefix +
                       "          (curChar >> 6) == 1" +
                       " && (" +
                       eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[1]) +
                       " & (1L << (curChar & 077))) != " +
                       eOutputLanguage.getLongPlain (0) +
                       ")");
        }
        else
          if (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[1] == 0L)
          {
            genCodeLine (sPrefix +
                         "   while (curChar <= " +
                         (int) maxChar (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[0]) +
                         " && (" +
                         eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[0]) +
                         " & (1L << curChar)) != " +
                         eOutputLanguage.getLongPlain (0) +
                         ")");
          }
          else
            if (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[0] == 0L)
            {
              genCodeLine (sPrefix +
                           "   while (curChar > 63 && curChar <= " +
                           (maxChar (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[1]) + 64) +
                           " && (" +
                           eOutputLanguage.getLongHex (lexer ().getSinglesToSkip ()[i].getAsciiMoves ()[1]) +
                           " & (1L << (curChar & 077))) != " +
                           eOutputLanguage.getLongPlain (0) +
                           ")");
            }

        if (Options.isDebugTokenManager ())
        {
          genCodeLine (sPrefix + "{");
          genCodeLine ("      debugStream.println(" +
                       (lexer ().getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                       "\"Skipping character : \" + " +
                       lexer ().getErrorHandlingClass () +
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

      if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      debugStream.println(\"   Matched the empty string as \" + tokenImage[" +
                       lexer ().getInitMatch ()[i] +
                       "] + \" token.\");");

        genCodeLine (sPrefix + "jjmatchedKind = " + lexer ().getInitMatch ()[i] + ";");
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
                     (lexer ().getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                     "\"Current character : \" + " +
                     lexer ().getErrorHandlingClass () +
                     ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      genCodeLine (sPrefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (lexer ().getCanMatchAnyChar ()[i] != -1)
      {
        if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
        {
          genCodeLine (sPrefix +
                       "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
                       lexer ().getCanMatchAnyChar ()[i] +
                       "))");
        }
        else
          genCodeLine (sPrefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + lexer ().getCanMatchAnyChar ()[i] + ")");
        genCodeLine (sPrefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           debugStream.println(\"   Current character matched as a \" + tokenImage[" +
                       lexer ().getCanMatchAnyChar ()[i] +
                       "] + \" token.\");");
        }
        genCodeLine (sPrefix + "   jjmatchedKind = " + lexer ().getCanMatchAnyChar ()[i] + ";");

        if (lexer ().getInitMatch ()[i] != Integer.MAX_VALUE && lexer ().getInitMatch ()[i] != 0)
          genCodeLine (sPrefix + "   jjmatchedPos = 0;");

        genCodeLine (sPrefix + "}");
      }

      if (lexer ().getMaxLexStates () > 1)
        genCodeLine (sPrefix + "break;");
    }

    if (lexer ().getMaxLexStates () > 1)
      genCodeLine (sEndSwitch);
    else
      if (lexer ().getMaxLexStates () == 0)
        genCodeLine ("       jjmatchedKind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");

    if (lexer ().getMaxLexStates () > 1)
      sPrefix = "  ";
    else
      sPrefix = "";

    if (lexer ().getMaxLexStates () > 0)
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
        genCodeLine (sPrefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (sPrefix + "      {");
      }

      genCodeLine (sPrefix + "         matchedToken = jjFillToken();");

      if (lexer ().isHasSpecial ())
        genCodeLine (sPrefix + "         matchedToken.specialToken = specialToken;");

      if (lexer ().isHasTokenActions ())
        genCodeLine (sPrefix + "         TokenLexicalActions(matchedToken);");

      if (lexer ().getMaxLexStates () > 1)
      {
        genCodeLine ("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine (sPrefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      if (Options.isCommonTokenAction ())
        genCodeLine (sPrefix + "         CommonTokenAction(matchedToken);");

      genCodeLine (sPrefix + "         return matchedToken;");

      if (lexer ().isHasSkip () || lexer ().isHasMore () || lexer ().isHasSpecial ())
      {
        genCodeLine (sPrefix + "      }");

        if (lexer ().isHasSkip () || lexer ().isHasSpecial ())
        {
          if (lexer ().isHasMore ())
          {
            genCodeLine (sPrefix +
                         "      else if ((jjtoSkip[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
          }
          else
            genCodeLine (sPrefix + "      else");

          genCodeLine (sPrefix + "      {");

          if (lexer ().isHasSpecial ())
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

            if (lexer ().isHasSkipActions ())
              genCodeLine (sPrefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (sPrefix + "         }");

            if (lexer ().isHasSkipActions ())
            {
              genCodeLine (sPrefix + "         else");
              genCodeLine (sPrefix + "            SkipLexicalActions(null);");
            }
          }
          else
            if (lexer ().isHasSkipActions ())
              genCodeLine (sPrefix + "         SkipLexicalActions(null);");

          if (lexer ().getMaxLexStates () > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (sPrefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (sPrefix + "         continue EOFLoop;");
          genCodeLine (sPrefix + "      }");
        }

        if (lexer ().isHasMore ())
        {
          if (lexer ().isHasMoreActions ())
            genCodeLine (sPrefix + "      MoreLexicalActions();");
          else
            if (lexer ().isHasSkipActions () || lexer ().isHasTokenActions ())
              genCodeLine (sPrefix + "      jjimageLen += jjmatchedPos + 1;");

          if (lexer ().getMaxLexStates () > 1)
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
                         (lexer ().getMaxLexStates () > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                         "\"Current character : \" + " +
                         lexer ().getErrorHandlingClass () +
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
                   lexer ().getErrorHandlingClass () +
                   "(" +
                   "EOFSeen, curLexState, error_line, error_column, error_after, curChar, " +
                   lexer ().getErrorHandlingClass () +
                   ".LEXICAL_ERROR);");
    }

    if (lexer ().isHasMore ())
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

    Outer: for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToSkip ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        aAct = lexer ().getActions ()[i];
        if ((aAct == null || aAct.getActionTokens ().isEmpty ()) &&
            !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i &&
            lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       lexer ().getLexStates ()[i] +
                       "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       lexer ().getLexStates ()[i] +
                       "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       lexer ().getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       lexer ().getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       lexer ().getLexStates ()[i] +
                       "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        aAct = lexer ().getActions ()[i];
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

    Outer: for (int i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToMore ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        aAct = lexer ().getActions ()[i];
        if ((aAct == null || aAct.getActionTokens ().isEmpty ()) &&
            !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i &&
            lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       lexer ().getLexStates ()[i] +
                       "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       lexer ().getLexStates ()[i] +
                       "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       lexer ().getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       lexer ().getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       lexer ().getLexStates ()[i] +
                       "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        aAct = lexer ().getActions ()[i];
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

    Outer: for (i = 0; i < lexer ().getMaxOrdinal (); i++)
    {
      if ((lexer ().getToToken ()[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        aAct = lexer ().getActions ()[i];
        if ((aAct == null || aAct.getActionTokens ().isEmpty ()) &&
            !lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (lexer ().getInitMatch ()[lexer ().getLexStates ()[i]] == i &&
            lexer ().getCanLoop ()[lexer ().getLexStates ()[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + lexer ().getLexStates ()[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" +
                       lexer ().getLexStates ()[i] +
                       "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" +
                       lexer ().getLexStates ()[i] +
                       "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       lexer ().getErrorHandlingClass () +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       lexer ().getErrorHandlingClass () +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + lexer ().getLexStates ()[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" +
                       lexer ().getLexStates ()[i] +
                       "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + lexer ().getLexStates ()[i] + "] = true;");
          genCodeLine ("         }");
        }

        aAct = lexer ().getActions ()[i];
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
