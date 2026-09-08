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
  private String m_sOrigFileName;
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

  /** @return The tokens from the start of the file to the first insertion point */
  @NonNull
  public ICommonsList <Token> cuToInsertionPoint1 ()
  {
    return m_aCuToInsertionPoint1;
  }

  /** @return The tokens from the first to the second insertion point */
  @NonNull
  public ICommonsList <Token> cuToInsertionPoint2 ()
  {
    return m_aCuToInsertionPoint2;
  }

  /** @return The tokens from the second insertion point to the end of the parser class */
  @NonNull
  public ICommonsList <Token> cuFromInsertionPoint2 ()
  {
    return m_aCuFromInsertionPoint2;
  }

  /** @return All BNF productions in the order they were declared */
  @NonNull
  public List <AbstractNormalProduction> bnfProductions ()
  {
    return m_aBnfProductions;
  }

  /** @return Production name to production */
  @NonNull
  public Map <String, AbstractNormalProduction> productionTable ()
  {
    return m_aProductionTable;
  }

  /** @return Lexical state name to its index */
  @NonNull
  public Map <String, Integer> lexStateS2I ()
  {
    return m_aLexStateS2I;
  }

  /** @return Lexical state index to its name */
  @NonNull
  public Map <Integer, String> lexStateI2S ()
  {
    return m_aLexStateI2S;
  }

  /** @return All token productions in the order they were declared */
  @NonNull
  public List <TokenProduction> rexprList ()
  {
    return m_aRexprList;
  }

  /** @return Token label to the regular expression it names */
  @NonNull
  public Map <String, AbstractExpRegularExpression> namedTokensTable ()
  {
    return m_aNamedTokensTable;
  }

  /** @return The named regular expressions in the order they were declared */
  @NonNull
  public List <AbstractExpRegularExpression> orderedNameTokens ()
  {
    return m_aOrderedNameTokens;
  }

  /** @return Token ordinal to its label */
  @NonNull
  public Map <Integer, String> namesOfTokens ()
  {
    return m_aNamesOfTokens;
  }

  /** @return Token ordinal to its regular expression */
  @NonNull
  public Map <Integer, AbstractExpRegularExpression> rexpsOfTokens ()
  {
    return m_aRexpsOfTokens;
  }

  /** @return Lexical state to image to the regular expression matching it */
  @NonNull
  public Map <String, Map <String, Map <String, AbstractExpRegularExpression>>> simpleTokensTable ()
  {
    return m_aSimpleTokensTable;
  }

  /** @return The token masks emitted for the error reporting arrays */
  @NonNull
  public List <int []> maskVals ()
  {
    return m_aMaskVals;
  }

  @Nullable
  public String getFileName ()
  {
    return m_sFileName;
  }

  public void setFileName (@Nullable final String sFileName)
  {
    m_sFileName = sFileName;
  }

  @Nullable
  public String getOrigFileName ()
  {
    return m_sOrigFileName;
  }

  public void setOrigFileName (@Nullable final String sOrigFileName)
  {
    m_sOrigFileName = sOrigFileName;
  }

  public boolean isJJTreeGenerated ()
  {
    return m_bJJTreeGenerated;
  }

  public void setJJTreeGenerated (final boolean bJJTreeGenerated)
  {
    m_bJJTreeGenerated = bJJTreeGenerated;
  }

  /** @return The tools that generated the grammar file, as read from its header */
  @Nullable
  public List <String> getToolNameList ()
  {
    return m_aToolNames;
  }

  public void setToolNameList (@Nullable final List <String> aToolNames)
  {
    m_aToolNames = aToolNames;
  }

  /** @return The name of the generated parser class */
  @Nullable
  public String getParserName ()
  {
    return m_sParserName;
  }

  public void setParserName (@Nullable final String sParserName)
  {
    m_sParserName = sParserName;
  }

  @Nullable
  public ICommonsList <Token> getTokenMgrDecls ()
  {
    return m_aTokenMgrDecls;
  }

  public void setTokenMgrDecls (@Nullable final ICommonsList <Token> aTokenMgrDecls)
  {
    m_aTokenMgrDecls = aTokenMgrDecls;
  }

  public int getTokenCount ()
  {
    return m_nTokenCount;
  }

  public void setTokenCount (final int nTokenCount)
  {
    m_nTokenCount = nTokenCount;
  }

  /** @return The current token count, and increments it afterwards. */
  public int getAndIncTokenCount ()
  {
    return m_nTokenCount++;
  }

  public int getMaskIndex ()
  {
    return m_nMaskIndex;
  }

  public void setMaskIndex (final int nMaskIndex)
  {
    m_nMaskIndex = nMaskIndex;
  }

  public void incMaskIndex ()
  {
    m_nMaskIndex++;
  }

  public int getJJ2Index ()
  {
    return m_nJJ2Index;
  }

  public void setJJ2Index (final int nJJ2Index)
  {
    m_nJJ2Index = nJJ2Index;
  }

  /** @return The jj2 index after incrementing it. */
  public int incAndGetJJ2Index ()
  {
    return ++m_nJJ2Index;
  }

  public boolean isLookAheadNeeded ()
  {
    return m_bLookAheadNeeded;
  }

  public void setLookAheadNeeded (final boolean bLookAheadNeeded)
  {
    m_bLookAheadNeeded = bLookAheadNeeded;
  }

  @Nullable
  public ExpAction getActionForEof ()
  {
    return m_aActionForEof;
  }

  public void setActionForEof (@Nullable final ExpAction aActionForEof)
  {
    m_aActionForEof = aActionForEof;
  }

  @Nullable
  public String getNextStateForEof ()
  {
    return m_sNextStateForEof;
  }

  public void setNextStateForEof (@Nullable final String sNextStateForEof)
  {
    m_sNextStateForEof = sNextStateForEof;
  }

  @Nullable
  public Token getOtherLanguageDeclTokenBegin ()
  {
    return m_aOtherLanguageDeclTokenBegin;
  }

  public void setOtherLanguageDeclTokenBegin (@Nullable final Token aToken)
  {
    m_aOtherLanguageDeclTokenBegin = aToken;
  }

  @Nullable
  public Token getOtherLanguageDeclTokenEnd ()
  {
    return m_aOtherLanguageDeclTokenEnd;
  }

  public void setOtherLanguageDeclTokenEnd (@Nullable final Token aToken)
  {
    m_aOtherLanguageDeclTokenEnd = aToken;
  }

  /** @return The line of the token that is currently being printed */
  public int getCurrentLine ()
  {
    return m_nCurrentLine;
  }

  public void setCurrentLine (final int nCurrentLine)
  {
    m_nCurrentLine = nCurrentLine;
  }

  public void decCurrentLine ()
  {
    m_nCurrentLine--;
  }

  public void incCurrentLine ()
  {
    m_nCurrentLine++;
  }

  /**
   * @return A generation number that is unique within this run. Used by the lookahead computation
   *         to mark the expansions it has already visited.
   */
  public long getAndIncNextExpansionGeneration ()
  {
    return m_nNextExpansionGeneration++;
  }

  /** @return The column of the token that is currently being printed */
  public int getCurrentColumn ()
  {
    return m_nCurrentColumn;
  }

  public void setCurrentColumn (final int nCurrentColumn)
  {
    m_nCurrentColumn = nCurrentColumn;
  }

  public void incCurrentColumn ()
  {
    m_nCurrentColumn++;
  }
}
