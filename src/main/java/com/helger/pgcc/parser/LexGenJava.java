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

import static com.helger.pgcc.parser.JavaCCGlobals.CU_TO_INSERTION_POINT_1;
import static com.helger.pgcc.parser.JavaCCGlobals.LEXSTATE_I2S;
import static com.helger.pgcc.parser.JavaCCGlobals.REXPR_LIST;
import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
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
import com.helger.pgcc.parser.table.TokenManagerCodeGenerator;

/**
 * Generate lexer.
 */
public class LexGenJava extends CodeGenerator
{
  private static final String DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL = "/templates/DumpStaticVarDeclarations.template";
  private static final String DUMP_DEBUG_METHODS_TEMPLATE_RESOURCE_URL = "/templates/DumpDebugMethods.template";
  private static final String BOILERPLATER_METHOD_RESOURCE_URL = "/templates/TokenManagerBoilerPlateMethods.template";

  public static String s_tokMgrClassName;

  // Order is important!
  static final Map <String, List <TokenProduction>> s_allTpsForState = new LinkedHashMap <> ();
  public static int s_lexStateIndex = 0;
  static ETokenKind [] s_kinds;
  public static int s_maxOrdinal = 1;
  public static String s_lexStateSuffix;
  public static String [] s_newLexState;
  public static int [] s_lexStates;
  public static boolean [] s_ignoreCase;
  public static ExpAction [] s_actions;
  // Order is important!
  public static final Map <String, NfaState> s_initStates = new LinkedHashMap <> ();
  public static int s_stateSetSize;
  public static int s_totalNumStates;
  public static int s_maxLexStates;
  public static String [] s_lexStateName;
  static NfaState [] s_singlesToSkip;
  public static long [] s_toSkip;
  public static long [] s_toSpecial;
  public static long [] s_toMore;
  public static long [] s_toToken;
  public static int s_defaultLexState;
  public static AbstractExpRegularExpression [] s_rexprs;
  public static int [] s_maxLongsReqd;
  public static int [] s_initMatch;
  public static int [] s_canMatchAnyChar;
  public static boolean s_hasEmptyMatch;
  public static boolean [] s_canLoop;
  public static boolean [] s_stateHasActions;
  public static boolean s_hasLoop = false;
  public static boolean [] s_canReachOnMore;
  public static boolean [] s_hasNfa;
  public static boolean [] s_mixed;
  public static NfaState s_initialState;
  public static int s_curKind;
  static boolean s_hasSkipActions = false;
  static boolean s_hasMoreActions = false;
  static boolean s_hasTokenActions = false;
  static boolean s_hasSpecial = false;
  static boolean s_hasSkip = false;
  static boolean s_hasMore = false;
  public static AbstractExpRegularExpression s_curRE;
  public static boolean s_keepLineCol;
  public static String s_errorHandlingClass;
  public static TokenizerData s_tokenizerData;
  public static boolean s_generateDataOnly;

  private void _printClassHead ()
  {
    final List <String> tn = new ArrayList <> (s_toolNames);
    tn.add (CPG.APP_NAME);
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    genCodeLine ("/* " + getIdString (tn, s_tokMgrClassName + getFileExtension ()) + " */");

    boolean bHasImport = false;
    int nIndex = 0;
    int i = 1;
    for (;;)
    {
      if (CU_TO_INSERTION_POINT_1.size () <= nIndex)
        break;

      int nKind = CU_TO_INSERTION_POINT_1.get (nIndex).kind;
      if (nKind == JavaCCParserConstants.PACKAGE || nKind == JavaCCParserConstants.IMPORT)
      {
        if (nKind == JavaCCParserConstants.IMPORT)
          bHasImport = true;

        for (; i < CU_TO_INSERTION_POINT_1.size (); i++)
        {
          nKind = CU_TO_INSERTION_POINT_1.get (i).kind;
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
            setLineAndCol (CU_TO_INSERTION_POINT_1.get (nIndex).beginLine,
                           CU_TO_INSERTION_POINT_1.get (nIndex).beginColumn);
            int j = nIndex;
            for (; j < i; j++)
            {
              printToken (CU_TO_INSERTION_POINT_1.get (j));
            }
            if (nKind == JavaCCParserConstants.SEMICOLON)
              printToken (CU_TO_INSERTION_POINT_1.get (j));
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
    genClassStart (null, s_tokMgrClassName, new String [] {}, new String [] { s_cu_name + "Constants" });
    // genCodeLine("{"); // }

    if (s_token_mgr_decls != null && s_token_mgr_decls.isNotEmpty ())
    {
      boolean bCommonTokenActionSeen = false;
      final boolean bCommonTokenActionNeeded = Options.isCommonTokenAction ();
      Token t = s_token_mgr_decls.getFirstOrNull ();

      printTokenSetup (t);
      setColToStart ();

      for (final Token s_token_mgr_decl : s_token_mgr_decls)
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
      genCodeLine ("  public " + s_cu_name + " parser = null;");
    }
  }

  @Override
  public void writeTemplate (final String name, final Map <String, Object> additionalOptions) throws IOException
  {
    final Map <String, Object> options = Options.getAllOptions ();
    options.put ("maxOrdinal", Integer.valueOf (s_maxOrdinal));
    options.put ("maxLexStates", Integer.valueOf (s_maxLexStates));
    options.put ("hasEmptyMatch", Boolean.valueOf (s_hasEmptyMatch));
    options.put ("hasSkip", Boolean.valueOf (s_hasSkip));
    options.put ("hasMore", Boolean.valueOf (s_hasMore));
    options.put ("hasSpecial", Boolean.valueOf (s_hasSpecial));
    options.put ("hasMoreActions", Boolean.valueOf (s_hasMoreActions));
    options.put ("hasSkipActions", Boolean.valueOf (s_hasSkipActions));
    options.put ("hasTokenActions", Boolean.valueOf (s_hasTokenActions));
    options.put ("stateSetSize", Integer.valueOf (s_stateSetSize));
    options.put ("hasActions", Boolean.valueOf (s_hasMoreActions || s_hasSkipActions || s_hasTokenActions));
    options.put ("tokMgrClassName", s_tokMgrClassName);
    int x = 0;
    for (final int l : s_maxLongsReqd)
      x = Math.max (x, l);
    options.put ("maxLongs", Integer.valueOf (x));
    options.put ("cu_name", s_cu_name);

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

      if (respecs == null || respecs.isEmpty ())
        continue;

      AbstractExpRegularExpression re;
      for (i = 0; i < respecs.size (); i++)
        if (s_maxOrdinal <= (re = respecs.get (i).rexp).getOrdinal ())
          s_maxOrdinal = re.getOrdinal () + 1;
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

  public static void addCharToSkip (final char c, final int kind)
  {
    s_singlesToSkip[s_lexStateIndex].addChar (c);
    s_singlesToSkip[s_lexStateIndex].m_kind = kind;
  }

  public void start () throws IOException
  {
    if (!Options.isBuildTokenManager () || Options.isUserTokenManager () || JavaCCErrors.getErrorCount () > 0)
      return;

    final String codeGeneratorClass = Options.getTokenManagerCodeGenerator ();
    s_keepLineCol = Options.isKeepLineColumn ();
    s_errorHandlingClass = Options.getTokenMgrErrorClass ();
    final List <ExpRChoice> choices = new ArrayList <> ();

    s_tokMgrClassName = s_cu_name + "TokenManager";

    if (!s_generateDataOnly && codeGeneratorClass == null)
      _printClassHead ();
    _buildLexStatesTable ();

    boolean ignoring = false;

    for (final Map.Entry <String, List <TokenProduction>> aEntry : s_allTpsForState.entrySet ())
    {
      int startState = -1;
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

          if (!Options.isNoDfa () &&
              s_curRE instanceof ExpRStringLiteral &&
              StringHelper.isNotEmpty (((ExpRStringLiteral) s_curRE).m_image))
          {
            ((ExpRStringLiteral) s_curRE).generateDfa ();
            if (i != 0 && !s_mixed[s_lexStateIndex] && ignoring != ignore)
            {
              s_mixed[s_lexStateIndex] = true;
            }
          }
          else
            if (s_curRE.canMatchAnyChar ())
            {
              if (s_canMatchAnyChar[s_lexStateIndex] == -1 ||
                  s_canMatchAnyChar[s_lexStateIndex] > s_curRE.getOrdinal ())
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

          if (respec.act != null && respec.act.getActionTokens ().isNotEmpty ())
            s_actions[s_curRE.getOrdinal ()] = respec.act;

          switch (kind)
          {
            case SPECIAL:
              s_hasSkipActions |= (s_actions[s_curRE.getOrdinal ()] != null) ||
                                  (s_newLexState[s_curRE.getOrdinal ()] != null);
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
        startState = s_initialState.generateInitMoves ();
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

      if (s_generateDataOnly || codeGeneratorClass != null)
      {
        ExpRStringLiteral.updateStringLiteralData (s_lexStateIndex);
        NfaState.updateNfaData (s_totalNumStates, startState, s_lexStateIndex, s_canMatchAnyChar[s_lexStateIndex]);
      }
      else
      {
        ExpRStringLiteral.dumpDfaCode (this);
        if (s_hasNfa[s_lexStateIndex])
        {
          NfaState.dumpMoveNfa (this);
        }
      }
      s_totalNumStates += NfaState.s_generatedStates;
      if (s_stateSetSize < NfaState.s_generatedStates)
        s_stateSetSize = NfaState.s_generatedStates;
    }

    for (final ExpRChoice aItem : choices)
      aItem.checkUnmatchability ();

    checkEmptyStringMatch ();

    if (s_generateDataOnly || codeGeneratorClass != null)
    {
      s_tokenizerData.setParserName (s_cu_name);
      NfaState.buildTokenizerData (s_tokenizerData);
      ExpRStringLiteral.BuildTokenizerData (s_tokenizerData);
      final int [] newLexStateIndices = new int [s_maxOrdinal];

      final StringBuilder tokenMgrDecls = new StringBuilder ();
      if (s_token_mgr_decls != null)
        for (final Token t : s_token_mgr_decls)
          tokenMgrDecls.append (t.image).append (' ');
      s_tokenizerData.setDecls (tokenMgrDecls.toString ());

      final Map <Integer, String> actionStrings = new HashMap <> ();
      for (int i = 0; i < s_maxOrdinal; i++)
      {
        if (s_newLexState[i] == null)
        {
          newLexStateIndices[i] = -1;
        }
        else
        {
          newLexStateIndices[i] = _getIndex (s_newLexState[i]);
        }
        // For java, we have this but for other languages, eventually we will
        // simply have a string.
        final ExpAction act = s_actions[i];
        if (act == null)
          continue;

        final StringBuilder sb = new StringBuilder ();
        for (final Token t : act.getActionTokens ())
          sb.append (t.image).append (' ');
        actionStrings.put (Integer.valueOf (i), sb.toString ());
      }
      s_tokenizerData.setDefaultLexState (s_defaultLexState);
      s_tokenizerData.setLexStateNames (s_lexStateName);
      s_tokenizerData.updateMatchInfo (actionStrings, newLexStateIndices, s_toSkip, s_toSpecial, s_toMore, s_toToken);
      if (!s_generateDataOnly)
      {
        TokenManagerCodeGenerator gen;
        try
        {
          final Class <?> codeGenClazz = Class.forName (codeGeneratorClass);
          gen = (TokenManagerCodeGenerator) codeGenClazz.getDeclaredConstructor ().newInstance ();
        }
        catch (final Exception ee)
        {
          JavaCCErrors.semantic_error ("Cound not load the token manager code generator class: " +
                                       codeGeneratorClass +
                                       "\nError: " +
                                       ee.getMessage ());
          return;
        }
        gen.generateCode (s_tokenizerData);
        gen.finish (s_tokenizerData);
      }
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

    if (s_hasLoop)
    {
      genCodeLine ("int[] jjemptyLineNo = new int[" + s_maxLexStates + "];");
      genCodeLine ("int[] jjemptyColNo = new int[" + s_maxLexStates + "];");
      genCodeLine (eOutputLanguage.getTypeBoolean () +
                   "[] jjbeenHere = new " +
                   eOutputLanguage.getTypeBoolean () +
                   "[" +
                   s_maxLexStates +
                   "];");
    }

    _dumpSkipActions ();
    _dumpMoreActions ();
    _dumpTokenActions ();

    NfaState.printBoilerPlateJava (this);

    final String charStreamName = CodeGenerator.getCharStreamName ();

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", charStreamName);
    aOpts.put ("lexStateNameLength", Integer.toString (s_lexStateName.length));
    aOpts.put ("defaultLexState", Integer.toString (s_defaultLexState));
    aOpts.put ("noDfa", Boolean.toString (Options.isNoDfa ()));
    aOpts.put ("generatedStates", Integer.toString (s_totalNumStates));
    writeTemplate (BOILERPLATER_METHOD_RESOURCE_URL, aOpts);

    _dumpStaticVarDeclarations (charStreamName);
    genCodeLine (/* { */ "}");

    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    final String fileName = Options.getOutputDirectory () + File.separator + s_tokMgrClassName + getFileExtension ();

    if (Options.isBuildParser ())
    {
      saveOutput (fileName);
    }
  }

  protected static void checkEmptyStringMatch ()
  {
    final boolean [] seen = new boolean [s_maxLexStates];
    final boolean [] done = new boolean [s_maxLexStates];

    Outer: for (int i = 0; i < s_maxLexStates; i++)
    {
      if (done[i] || s_initMatch[i] == 0 || s_initMatch[i] == Integer.MAX_VALUE || s_canMatchAnyChar[i] != -1)
        continue;

      done[i] = true;
      int len = 0;
      String cycle = "";
      String reList = "";

      for (int k = 0; k < s_maxLexStates; k++)
        seen[k] = false;

      int j = i;
      seen[i] = true;
      cycle += s_lexStateName[j] + "-->";
      while (s_newLexState[s_initMatch[j]] != null)
      {
        cycle += s_newLexState[s_initMatch[j]];
        if (seen[j = _getIndex (s_newLexState[s_initMatch[j]])])
          break;

        cycle += "-->";
        done[j] = true;
        seen[j] = true;
        if (s_initMatch[j] == 0 || s_initMatch[j] == Integer.MAX_VALUE || s_canMatchAnyChar[j] != -1)
          continue Outer;
        if (len != 0)
          reList += "; ";
        reList += "line " + s_rexprs[s_initMatch[j]].getLine () + ", column " + s_rexprs[s_initMatch[j]].getColumn ();
        len++;
      }

      if (s_newLexState[s_initMatch[j]] == null)
        cycle += s_lexStateName[s_lexStates[s_initMatch[j]]];

      for (int k = 0; k < s_maxLexStates; k++)
        s_canLoop[k] |= seen[k];

      s_hasLoop = true;
      final String sLabel = s_rexprs[s_initMatch[i]].getLabel ();
      if (len == 0)
      {
        JavaCCErrors.warning (s_rexprs[s_initMatch[i]],
                              "Regular expression" +
                                                        (StringHelper.isEmpty (sLabel) ? "" : " for " + sLabel) +
                                                        " can be matched by the empty string (\"\") in lexical state " +
                                                        s_lexStateName[i] +
                                                        ". This can result in an endless loop of " +
                                                        "empty string matches.");
      }
      else
      {
        JavaCCErrors.warning (s_rexprs[s_initMatch[i]],
                              "Regular expression" +
                                                        (StringHelper.isEmpty (sLabel) ? "" : " for " + sLabel) +
                                                        " can be matched by the empty string (\"\") in lexical state " +
                                                        s_lexStateName[i] +
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
    for (int i = 0; i < s_maxLexStates; i++)
      genCodeLine ("   \"" + s_lexStateName[i] + "\",");
    genCodeLine ("};");

    {
      genCodeNewLine ();
      genCodeLine ("/** Lex State array. */");
      genCode ("public static final int[] jjnewLexState = {");

      for (int i = 0; i < s_maxOrdinal; i++)
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

    {
      // Bit vector for TOKEN
      genCode ("static final long[] jjtoToken = {");
      for (int i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toToken[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for SKIP
      genCode ("static final long[] jjtoSkip = {");
      for (int i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toSkip[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for SPECIAL
      genCode ("static final long[] jjtoSpecial = {");
      for (int i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toSpecial[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    {
      // Bit vector for MORE
      genCode ("static final long[] jjtoMore = {");
      for (int i = 0; i < s_maxOrdinal / 64 + 1; i++)
      {
        if (i % 4 == 0)
          genCode ("\n   ");
        genCode (eOutputLanguage.getLongHex (s_toMore[i]) + ", ");
      }
      genCodeLine ("\n};");
    }

    final Map <String, Object> aOpts = new HashMap <> ();
    aOpts.put ("charStreamName", charStreamName);
    aOpts.put ("protected", "protected");
    aOpts.put ("private", "private");
    aOpts.put ("final", "final");
    aOpts.put ("lexStateNameLength", Integer.toString (s_lexStateName.length));
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
    if (s_keepLineCol)
    {
      genCodeLine ("   final int beginLine;");
      genCodeLine ("   final int endLine;");
      genCodeLine ("   final int beginColumn;");
      genCodeLine ("   final int endColumn;");
    }

    if (s_hasEmptyMatch)
    {
      genCodeLine ("   if (jjmatchedPos < 0)");
      genCodeLine ("   {");
      genCodeLine ("      if (image == null)");
      genCodeLine ("         curTokenImage = \"\";");
      genCodeLine ("      else");
      genCodeLine ("         curTokenImage = image.toString();");

      if (s_keepLineCol)
      {
        genCodeLine ("      beginLine = endLine = input_stream.getEndLine();");
        genCodeLine ("      beginColumn = endColumn = input_stream.getEndColumn();");
      }

      genCodeLine ("   }");
      genCodeLine ("   else");
      genCodeLine ("   {");
      genCodeLine ("      String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine ("      curTokenImage = im == null ? input_stream.getImage() : im;");

      if (s_keepLineCol)
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
      if (s_keepLineCol)
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

    if (s_keepLineCol)
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
    genCodeLine ("int curLexState = " + s_defaultLexState + ";");
    genCodeLine ("int defaultLexState = " + s_defaultLexState + ";");
    genCodeLine ("int jjnewStateCnt;");
    genCodeLine ("int jjround;");
    genCodeLine ("int jjmatchedPos;");
    genCodeLine ("int jjmatchedKind;");
    genCodeNewLine ();
    genCodeLine ("/** Get the next Token. */");
    genCodeLine ("public " + "Token getNextToken()" + " ");
    genCodeLine ("{");
    if (s_hasSpecial)
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

    if (s_hasSpecial)
      genCodeLine ("      matchedToken.specialToken = specialToken;");

    if (s_sNextStateForEof != null || s_aActForEof != null)
      genCodeLine ("      TokenLexicalActions(matchedToken);");

    if (Options.isCommonTokenAction ())
      genCodeLine ("      CommonTokenAction(matchedToken);");

    genCodeLine ("      return matchedToken;");
    genCodeLine ("   }");

    if (s_hasMoreActions || s_hasSkipActions || s_hasTokenActions)
    {
      genCodeLine ("   image = jjimage;");
      genCodeLine ("   image.setLength(0);");
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
        genCodeLine (prefix + "try {");
        genCodeLine (prefix + "  input_stream.backup(0);");
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

        if (Options.isDebugTokenManager ())
        {
          genCodeLine (prefix + "{");
          genCodeLine ("      debugStream.println(" +
                       (s_maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                       "\"Skipping character : \" + " +
                       s_errorHandlingClass +
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

      if (s_initMatch[i] != Integer.MAX_VALUE && s_initMatch[i] != 0)
      {
        if (Options.isDebugTokenManager ())
          genCodeLine ("      debugStream.println(\"   Matched the empty string as \" + tokenImage[" +
                       s_initMatch[i] +
                       "] + \" token.\");");

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
        genCodeLine ("      debugStream.println(" +
                     (s_maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                     "\"Current character : \" + " +
                     s_errorHandlingClass +
                     ".addEscapes(String.valueOf(curChar)) + \" (\" + curChar + \") " +
                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      genCodeLine (prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (s_canMatchAnyChar[i] != -1)
      {
        if (s_initMatch[i] != Integer.MAX_VALUE && s_initMatch[i] != 0)
        {
          genCodeLine (prefix +
                       "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
                       s_canMatchAnyChar[i] +
                       "))");
        }
        else
          genCodeLine (prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + s_canMatchAnyChar[i] + ")");
        genCodeLine (prefix + "{");

        if (Options.isDebugTokenManager ())
        {
          genCodeLine ("           debugStream.println(\"   Current character matched as a \" + tokenImage[" +
                       s_canMatchAnyChar[i] +
                       "] + \" token.\");");
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
                       s_errorHandlingClass +
                       ".addEscapes(new String(input_stream.getSuffix(jjmatchedPos + 1))) + " +
                       "\") ******\\n\");");
        }
        else
        {
          genCodeLine ("    debugStream.println(" +
                       "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                       "(\" + " +
                       s_errorHandlingClass +
                       ".addEscapes(new String(input_stream.getSuffix(jjmatchedPos + 1))) + " +
                       "\") ******\\n\");");
        }
      }

      if (s_hasSkip || s_hasMore || s_hasSpecial)
      {
        genCodeLine (prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine (prefix + "      {");
      }

      genCodeLine (prefix + "         matchedToken = jjFillToken();");

      if (s_hasSpecial)
        genCodeLine (prefix + "         matchedToken.specialToken = specialToken;");

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
            genCodeLine (prefix +
                         "      else if ((jjtoSkip[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
          }
          else
            genCodeLine (prefix + "      else");

          genCodeLine (prefix + "      {");

          if (s_hasSpecial)
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

            if (s_hasSkipActions)
              genCodeLine (prefix + "            SkipLexicalActions(matchedToken);");

            genCodeLine (prefix + "         }");

            if (s_hasSkipActions)
            {
              genCodeLine (prefix + "         else");
              genCodeLine (prefix + "            SkipLexicalActions(null);");
            }
          }
          else
            if (s_hasSkipActions)
              genCodeLine (prefix + "         SkipLexicalActions(null);");

          if (s_maxLexStates > 1)
          {
            genCodeLine ("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine (prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine (prefix + "         continue EOFLoop;");
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

          genCodeLine (prefix + "      try {");
          genCodeLine (prefix + "         curChar = input_stream.readChar();");

          if (Options.isDebugTokenManager ())
            genCodeLine ("   debugStream.println(" +
                         (s_maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                         "\"Current character : \" + " +
                         s_errorHandlingClass +
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
                   s_errorHandlingClass +
                   "(" +
                   "EOFSeen, curLexState, error_line, error_column, error_after, curChar, " +
                   s_errorHandlingClass +
                   ".LEXICAL_ERROR);");
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

    genCodeLine ("void SkipLexicalActions(Token matchedToken)");
    genCodeLine ("{");
    genCodeLine ("   switch(jjmatchedKind)");
    genCodeLine ("   {");

    Outer: for (int i = 0; i < s_maxOrdinal; i++)
    {
      if ((s_toSkip[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        act = s_actions[i];
        if ((act == null || act.getActionTokens ().isEmpty ()) && !s_canLoop[s_lexStates[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (s_initMatch[s_lexStates[i]] == i && s_canLoop[s_lexStates[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + s_lexStates[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + s_lexStates[i] + "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + s_lexStates[i] + "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       s_errorHandlingClass +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       s_errorHandlingClass +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + s_lexStates[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + s_lexStates[i] + "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + s_lexStates[i] + "] = true;");
          genCodeLine ("         }");
        }

        act = s_actions[i];
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

    Outer: for (int i = 0; i < s_maxOrdinal; i++)
    {
      if ((s_toMore[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        act = s_actions[i];
        if ((act == null || act.getActionTokens ().isEmpty ()) && !s_canLoop[s_lexStates[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (s_initMatch[s_lexStates[i]] == i && s_canLoop[s_lexStates[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + s_lexStates[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + s_lexStates[i] + "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + s_lexStates[i] + "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       s_errorHandlingClass +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       s_errorHandlingClass +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + s_lexStates[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + s_lexStates[i] + "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + s_lexStates[i] + "] = true;");
          genCodeLine ("         }");
        }

        act = s_actions[i];
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

    Outer: for (i = 0; i < s_maxOrdinal; i++)
    {
      if ((s_toToken[i / 64] & (1L << (i % 64))) == 0L)
        continue;

      for (;;)
      {
        act = s_actions[i];
        if ((act == null || act.getActionTokens ().isEmpty ()) && !s_canLoop[s_lexStates[i]])
          continue Outer;

        genCodeLine ("      case " + i + " :");

        if (s_initMatch[s_lexStates[i]] == i && s_canLoop[s_lexStates[i]])
        {
          genCodeLine ("         if (jjmatchedPos == -1)");
          genCodeLine ("         {");
          genCodeLine ("            if (jjbeenHere[" + s_lexStates[i] + "] &&");
          genCodeLine ("                jjemptyLineNo[" + s_lexStates[i] + "] == input_stream.getBeginLine() &&");
          genCodeLine ("                jjemptyColNo[" + s_lexStates[i] + "] == input_stream.getBeginColumn())");
          genCodeLine ("               throw new " +
                       s_errorHandlingClass +
                       "(" +
                       "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
                       "at line \" + input_stream.getBeginLine() + \", " +
                       "column \" + input_stream.getBeginColumn() + \".\"), " +
                       s_errorHandlingClass +
                       ".LOOP_DETECTED);");
          genCodeLine ("            jjemptyLineNo[" + s_lexStates[i] + "] = input_stream.getBeginLine();");
          genCodeLine ("            jjemptyColNo[" + s_lexStates[i] + "] = input_stream.getBeginColumn();");
          genCodeLine ("            jjbeenHere[" + s_lexStates[i] + "] = true;");
          genCodeLine ("         }");
        }

        act = s_actions[i];
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

  public static void reInit ()
  {
    s_actions = null;
    s_allTpsForState.clear ();
    s_canLoop = null;
    s_canMatchAnyChar = null;
    s_canReachOnMore = null;
    s_curKind = 0;
    s_curRE = null;
    s_defaultLexState = 0;
    s_errorHandlingClass = null;
    s_hasEmptyMatch = false;
    s_hasLoop = false;
    s_hasMore = false;
    s_hasMoreActions = false;
    s_hasNfa = null;
    s_hasSkip = false;
    s_hasSkipActions = false;
    s_hasSpecial = false;
    s_hasTokenActions = false;
    s_ignoreCase = null;
    s_initMatch = null;
    s_initStates.clear ();
    s_initialState = null;
    s_keepLineCol = false;
    s_kinds = null;
    s_lexStateIndex = 0;
    s_lexStateName = null;
    s_lexStateSuffix = null;
    s_lexStates = null;
    s_maxLexStates = 0;
    s_maxLongsReqd = null;
    s_maxOrdinal = 1;
    s_mixed = null;
    s_newLexState = null;
    s_rexprs = null;
    s_singlesToSkip = null;
    s_stateHasActions = null;
    s_stateSetSize = 0;
    s_toMore = null;
    s_toSkip = null;
    s_toSpecial = null;
    s_toToken = null;
    s_tokMgrClassName = null;
    s_tokenizerData = new TokenizerData ();
    s_generateDataOnly = false;
  }
}
