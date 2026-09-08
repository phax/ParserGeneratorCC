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
package com.helger.pgcc.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pgcc.parser.AbstractNormalProduction;
import com.helger.pgcc.parser.Token;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;

/**
 * The grammar that is being processed by a single generator run: what the parser file is called,
 * the productions, the token definitions, the lexical states, and the counters that number them.
 * <p>
 * This is the instance state behind the static {@link com.helger.pgcc.parser.JavaCCGlobals} facade.
 * It is deliberately a plain model object rather than something that reaches back into the context,
 * so that it can be passed explicitly once the language backends are separated.
 *
 * @author Philip Helger
 */
public final class GrammarState
{
  /** Default constructor. */
  public GrammarState ()
  {}

  private final ICommonsList <Token> m_aCuToInsertionPoint1 = new CommonsArrayList <> ();
  private final ICommonsList <Token> m_aCuToInsertionPoint2 = new CommonsArrayList <> ();
  private final ICommonsList <Token> m_aCuFromInsertionPoint2 = new CommonsArrayList <> ();
  private final List <AbstractNormalProduction> m_aBnfProductions = new ArrayList <> ();
  private final Map <String, AbstractNormalProduction> m_aProductionTable = new HashMap <> ();
  private final Map <String, Integer> m_aLexStateS2I = new HashMap <> ();
  private final Map <Integer, String> m_aLexStateI2S = new HashMap <> ();
  private final List <TokenProduction> m_aRexprList = new ArrayList <> ();
  private final Map <String, AbstractExpRegularExpression> m_aNamedTokensTable = new HashMap <> ();
  private final List <AbstractExpRegularExpression> m_aOrderedNameTokens = new ArrayList <> ();
  private final Map <Integer, String> m_aNamesOfTokens = new HashMap <> ();
  private final Map <Integer, AbstractExpRegularExpression> m_aRexpsOfTokens = new HashMap <> ();
  private final Map <String, Map <String, Map <String, AbstractExpRegularExpression>>> m_aSimpleTokensTable = new HashMap <> ();
  private final List <int []> m_aMaskVals = new ArrayList <> ();

  private String m_sFileName;
  private boolean m_bJJTreeGenerated;
  private List <String> m_aToolNames;
  private String m_sParserName;
  private ICommonsList <Token> m_aTokenMgrDecls;
  private int m_nTokenCount;
  private int m_nMaskIndex;
  private int m_nJJ2Index;
  private boolean m_bLookAheadNeeded;
  private ExpAction m_aActionForEof;
  private String m_sNextStateForEof;
  private Token m_aOtherLanguageDeclTokenBegin;
  private Token m_aOtherLanguageDeclTokenEnd;
  private int m_nCurrentLine;
  private int m_nCurrentColumn;
  private long m_nNextExpansionGeneration = 1;

  /** {@return the tokens from the start of the file to the first insertion point} */
  @NonNull
  public ICommonsList <Token> cuToInsertionPoint1 ()
  {
    return m_aCuToInsertionPoint1;
  }

  /** {@return the tokens from the first to the second insertion point} */
  @NonNull
  public ICommonsList <Token> cuToInsertionPoint2 ()
  {
    return m_aCuToInsertionPoint2;
  }

  /** {@return the tokens from the second insertion point to the end of the parser class} */
  @NonNull
  public ICommonsList <Token> cuFromInsertionPoint2 ()
  {
    return m_aCuFromInsertionPoint2;
  }

  /** {@return all BNF productions in the order they were declared} */
  @NonNull
  public List <AbstractNormalProduction> bnfProductions ()
  {
    return m_aBnfProductions;
  }

  /** {@return production name to production} */
  @NonNull
  public Map <String, AbstractNormalProduction> productionTable ()
  {
    return m_aProductionTable;
  }

  /** {@return lexical state name to its index} */
  @NonNull
  public Map <String, Integer> lexStateS2I ()
  {
    return m_aLexStateS2I;
  }

  /** {@return lexical state index to its name} */
  @NonNull
  public Map <Integer, String> lexStateI2S ()
  {
    return m_aLexStateI2S;
  }

  /** {@return all token productions in the order they were declared} */
  @NonNull
  public List <TokenProduction> rexprList ()
  {
    return m_aRexprList;
  }

  /** {@return token label to the regular expression it names} */
  @NonNull
  public Map <String, AbstractExpRegularExpression> namedTokensTable ()
  {
    return m_aNamedTokensTable;
  }

  /** {@return the named regular expressions in the order they were declared} */
  @NonNull
  public List <AbstractExpRegularExpression> orderedNameTokens ()
  {
    return m_aOrderedNameTokens;
  }

  /** {@return token ordinal to its label} */
  @NonNull
  public Map <Integer, String> namesOfTokens ()
  {
    return m_aNamesOfTokens;
  }

  /** {@return token ordinal to its regular expression} */
  @NonNull
  public Map <Integer, AbstractExpRegularExpression> rexpsOfTokens ()
  {
    return m_aRexpsOfTokens;
  }

  /** {@return lexical state to image to the regular expression matching it} */
  @NonNull
  public Map <String, Map <String, Map <String, AbstractExpRegularExpression>>> simpleTokensTable ()
  {
    return m_aSimpleTokensTable;
  }

  /** {@return the token masks emitted for the error reporting arrays} */
  @NonNull
  public List <int []> maskVals ()
  {
    return m_aMaskVals;
  }

  /**
   * The grammar file this run is reading.
   *
   * @return The file name as it was given on the command line. May be <code>null</code> before parsing
   *   *         starts.
   */
  @Nullable
  public String getFileName ()
  {
    return m_sFileName;
  }

  /**
   * The grammar file this run is reading.
   *
   * @param sFileName
   *        The file name.
   */
  public void setFileName (@Nullable final String sFileName)
  {
    m_sFileName = sFileName;
  }



  /**
   * Whether the grammar handed to the parser generator came out of JJTree rather than straight
   * from the user. Error messages have to point back at the .jjt in that case.
   *
   * @return <code>true</code> if JJTree produced this grammar.
   */
  public boolean isJJTreeGenerated ()
  {
    return m_bJJTreeGenerated;
  }

  /**
   * Whether the grammar handed to the parser generator came out of JJTree rather than straight
   * from the user. Error messages have to point back at the .jjt in that case.
   *
   * @param bJJTreeGenerated
   *        <code>true</code> if JJTree produced this grammar.
   */
  public void setJJTreeGenerated (final boolean bJJTreeGenerated)
  {
    m_bJJTreeGenerated = bJJTreeGenerated;
  }

  /** {@return the tools that generated the grammar file, as read from its header} */
  @Nullable
  public List <String> getToolNameList ()
  {
    return m_aToolNames;
  }

  /**
   * The tools that have already written to this grammar - JJTree puts its name in the header of
   * what it generates, and the parser generator appends its own.
   *
   * @param aToolNames
   *        The tool names.
   */
  public void setToolNameList (@Nullable final List <String> aToolNames)
  {
    m_aToolNames = aToolNames;
  }

  /** {@return the name of the generated parser class} */
  @Nullable
  public String getParserName ()
  {
    return m_sParserName;
  }

  /**
   * The name of the parser class to generate, as the PARSER_BEGIN of the grammar gives it. Nearly
   * every generated file is named after it.
   *
   * @param sParserName
   *        The class name.
   */
  public void setParserName (@Nullable final String sParserName)
  {
    m_sParserName = sParserName;
  }

  /**
   * The declarations from the TOKEN_MGR_DECLS section, copied verbatim into the generated token
   * manager.
   *
   * @return The tokens of the section. May be <code>null</code> if the grammar has no such section.
   */
  @Nullable
  public ICommonsList <Token> getTokenMgrDecls ()
  {
    return m_aTokenMgrDecls;
  }

  /**
   * The declarations from the TOKEN_MGR_DECLS section, copied verbatim into the generated token
   * manager.
   *
   * @param aTokenMgrDecls
   *        The tokens of the section.
   */
  public void setTokenMgrDecls (@Nullable final ICommonsList <Token> aTokenMgrDecls)
  {
    m_aTokenMgrDecls = aTokenMgrDecls;
  }

  /**
   * The number of token kinds declared so far. Every regular expression that gets an ordinal
   * takes the current value and increments it.
   *
   * @return The count.
   */
  public int getTokenCount ()
  {
    return m_nTokenCount;
  }

  /**
   * The number of token kinds declared so far. Every regular expression that gets an ordinal
   * takes the current value and increments it.
   *
   * @param nTokenCount
   *        The count.
   */
  public void setTokenCount (final int nTokenCount)
  {
    m_nTokenCount = nTokenCount;
  }

  /** {@return the current token count, and increments it afterwards} */
  public int getAndIncTokenCount ()
  {
    return m_nTokenCount++;
  }

  /**
   * How many entries of the generated jj_la1 error reporting array are in use. Each choice point
   * that reports errors claims one.
   *
   * @return The next free index, and therefore the length the array needs.
   */
  public int getMaskIndex ()
  {
    return m_nMaskIndex;
  }


  /**
   * Claim one more entry of the generated jj_la1 error reporting array.
   */
  public void incMaskIndex ()
  {
    m_nMaskIndex++;
  }

  /**
   * How many jj2 lookahead routines the parser needs. A choice that cannot be decided with a
   * simple token mask gets one, and it is nonzero exactly when the generated parser needs the
   * whole backtracking machinery.
   *
   * @return The number of jj2 routines generated so far.
   */
  public int getJJ2Index ()
  {
    return m_nJJ2Index;
  }


  /** {@return the jj2 index after incrementing it} */
  public int incAndGetJJ2Index ()
  {
    return ++m_nJJ2Index;
  }

  /**
   * Whether any production uses syntactic lookahead, which decides whether the generated parser
   * carries the jj_lookingAhead flag at all.
   *
   * @return <code>true</code> if at least one production needs it.
   */
  public boolean isLookAheadNeeded ()
  {
    return m_bLookAheadNeeded;
  }

  /**
   * Whether any production uses syntactic lookahead, which decides whether the generated parser
   * carries the jj_lookingAhead flag at all.
   *
   * @param bLookAheadNeeded
   *        <code>true</code> if at least one production needs it.
   */
  public void setLookAheadNeeded (final boolean bLookAheadNeeded)
  {
    m_bLookAheadNeeded = bLookAheadNeeded;
  }

  /**
   * The lexical action the grammar attached to &lt;EOF&gt;, if any.
   *
   * @return The action. May be <code>null</code>.
   */
  @Nullable
  public ExpAction getActionForEof ()
  {
    return m_aActionForEof;
  }

  /**
   * The lexical action the grammar attached to &lt;EOF&gt;, if any.
   *
   * @param aActionForEof
   *        The action.
   */
  public void setActionForEof (@Nullable final ExpAction aActionForEof)
  {
    m_aActionForEof = aActionForEof;
  }

  /**
   * The lexical state the grammar wants to switch to on &lt;EOF&gt;, if any.
   *
   * @return The lexical state name. May be <code>null</code>.
   */
  @Nullable
  public String getNextStateForEof ()
  {
    return m_sNextStateForEof;
  }

  /**
   * The lexical state the grammar wants to switch to on &lt;EOF&gt;, if any.
   *
   * @param sNextStateForEof
   *        The lexical state name.
   */
  public void setNextStateForEof (@Nullable final String sNextStateForEof)
  {
    m_sNextStateForEof = sNextStateForEof;
  }

  /**
   * The first token of the class declaration a C++ grammar writes between PARSER_BEGIN and
   * PARSER_END, which the C++ backend copies into the generated header.
   *
   * @return The token. May be <code>null</code>.
   */
  @Nullable
  public Token getOtherLanguageDeclTokenBegin ()
  {
    return m_aOtherLanguageDeclTokenBegin;
  }

  /**
   * The first token of the class declaration a C++ grammar writes between PARSER_BEGIN and
   * PARSER_END, which the C++ backend copies into the generated header.
   *
   * @param aToken
   *        The token.
   */
  public void setOtherLanguageDeclTokenBegin (@Nullable final Token aToken)
  {
    m_aOtherLanguageDeclTokenBegin = aToken;
  }

  /**
   * The token just past the class declaration of a C++ grammar, the end of the range that starts
   * at {@link #getOtherLanguageDeclTokenBegin()}.
   *
   * @return The token. May be <code>null</code>.
   */
  @Nullable
  public Token getOtherLanguageDeclTokenEnd ()
  {
    return m_aOtherLanguageDeclTokenEnd;
  }

  /**
   * The token just past the class declaration of a C++ grammar, the end of the range that starts
   * at {@link #getOtherLanguageDeclTokenBegin()}.
   *
   * @param aToken
   *        The token.
   */
  public void setOtherLanguageDeclTokenEnd (@Nullable final Token aToken)
  {
    m_aOtherLanguageDeclTokenEnd = aToken;
  }

  /** {@return the line of the token that is currently being printed} */
  public int getCurrentLine ()
  {
    return m_nCurrentLine;
  }

  /**
   * Where the token printer has got to in the grammar file. Copying a stretch of the grammar into
   * the output means reproducing its blank lines, so the printer tracks the position it last
   * wrote.
   *
   * @param nCurrentLine
   *        The line number.
   */
  public void setCurrentLine (final int nCurrentLine)
  {
    m_nCurrentLine = nCurrentLine;
  }

  /**
   * Step the token printer's position back one line, for the caller that has looked one line too
   * far.
   */
  public void decCurrentLine ()
  {
    m_nCurrentLine--;
  }

  /**
   * Step the token printer's position on one line.
   */
  public void incCurrentLine ()
  {
    m_nCurrentLine++;
  }

  /**
   * {@return a generation number that is unique within this run. Used by the lookahead computation
   *         to mark the expansions it has already visited.}
   */
  public long getAndIncNextExpansionGeneration ()
  {
    return m_nNextExpansionGeneration++;
  }

  /** {@return the column of the token that is currently being printed} */
  public int getCurrentColumn ()
  {
    return m_nCurrentColumn;
  }

  /**
   * Where the token printer has got to within {@link #getCurrentLine()}.
   *
   * @param nCurrentColumn
   *        The column number.
   */
  public void setCurrentColumn (final int nCurrentColumn)
  {
    m_nCurrentColumn = nCurrentColumn;
  }

  /**
   * Step the token printer's position on one column.
   */
  public void incCurrentColumn ()
  {
    m_nCurrentColumn++;
  }
}
