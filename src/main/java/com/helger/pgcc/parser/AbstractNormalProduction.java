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
package com.helger.pgcc.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.helger.pgcc.parser.exp.Expansion;

/**
 * Describes JavaCC productions.
 */
public abstract class AbstractNormalProduction implements IGrammarLocation
{
  protected static final String EOL = System.getProperty ("line.separator", "\n");

  /**
   * The line and column number of the construct that corresponds most closely to this node.
   */
  private int m_nColumn;

  private int m_nLine;

  /**
   * The NonTerminal nodes which refer to this production.
   */
  private List <Expansion> m_aParents = new ArrayList <> ();

  /**
   * The access modifier of this production.
   */
  private String m_sAccessMod;

  /**
   * The name of the non-terminal of this production.
   */
  private String m_sLhs;

  /**
   * The tokens that make up the return type of this production.
   */
  private final List <Token> m_aReturnTypeTokens = new ArrayList <> ();

  /**
   * The tokens that make up the parameters of this production.
   */
  private final List <Token> m_aParameterListTokens = new ArrayList <> ();

  /**
   * Each entry in this list is a list of tokens that represents an exception in the throws list of
   * this production. This list does not include ParseException which is always thrown.
   */
  private List <List <Token>> m_aThrowsList = new ArrayList <> ();

  /**
   * The RHS of this production. Not used for JavaCodeProduction.
   */
  private Expansion m_aExpansion;

  /**
   * This boolean flag is true if this production can expand to empty.
   */
  private boolean m_bEmptyPossible = false;

  /**
   * A list of all non-terminals that this one can expand to without having to consume any tokens.
   * Also an index that shows how many pointers exist.
   */
  private AbstractNormalProduction [] m_aLeftExpansions = new AbstractNormalProduction [10];
  int m_nLeIndex = 0;

  /**
   * The following variable is used to maintain state information for the left-recursion
   * determination algorithm: It is initialized to 0, and set to -1 if this node has been visited in
   * a pre-order walk, and then it is set to 1 if the pre-order walk of the whole graph from this
   * node has been traversed. i.e., -1 indicates partially processed, and 1 indicates fully
   * processed.
   */
  private int m_nWalkStatus = 0;

  /**
   * The first and last tokens from the input stream that represent this production.
   */
  private Token m_aLastToken;

  private Token m_aFirstToken;

  protected StringBuilder dumpPrefix (final int nIndent)
  {
    final StringBuilder aSb = new StringBuilder (128);
    for (int i = 0; i < nIndent; i++)
      aSb.append ("  ");
    return aSb;
  }

  protected String getSimpleName ()
  {
    final String sName = getClass ().getName ();
    // strip the package name
    return sName.substring (sName.lastIndexOf (".") + 1);
  }

  public StringBuilder dump (final int nIndent, final Set <? super AbstractNormalProduction> aAlreadyDumped)
  {
    final StringBuilder aSB = dumpPrefix (nIndent).append (System.identityHashCode (this))
                                                  .append (' ')
                                                  .append (getSimpleName ())
                                                  .append (' ')
                                                  .append (getLhs ());
    if (!aAlreadyDumped.contains (this))
    {
      aAlreadyDumped.add (this);
      if (getExpansion () != null)
      {
        // cannot re-use already dumped
        aSB.append (EOL).append (getExpansion ().dump (nIndent + 1, new HashSet <> ()));
      }
    }

    return aSB;
  }

  /**
   * @param nLine
   *        the line to set
   */
  public void setLineNumber (final int nLine)
  {
    m_nLine = nLine;
  }

  /**
   * @return the line
   */
  public int getLineNumber ()
  {
    return m_nLine;
  }

  /**
   * @param nColumn
   *        the column to set
   */
  public void setColumnNumber (final int nColumn)
  {
    m_nColumn = nColumn;
  }

  /**
   * @return the column
   */
  public int getColumnNumber ()
  {
    return m_nColumn;
  }

  /**
   * @param aParents
   *        the parents to set
   */
  void setParents (final List <Expansion> aParents)
  {
    m_aParents = aParents;
  }

  /**
   * @return the parents
   */
  List <Expansion> getParents ()
  {
    return m_aParents;
  }

  /**
   * @param sAccessMod
   *        the accessMod to set
   */
  public void setAccessMod (final String sAccessMod)
  {
    m_sAccessMod = sAccessMod;
  }

  /**
   * @return the accessMod
   */
  public String getAccessMod ()
  {
    return m_sAccessMod;
  }

  /**
   * @param sLhs
   *        the lhs to set
   */
  public void setLhs (final String sLhs)
  {
    m_sLhs = sLhs;
  }

  /**
   * @return the lhs
   */
  public String getLhs ()
  {
    return m_sLhs;
  }

  /**
   * @return the return_type_tokens
   */
  public List <Token> getReturnTypeTokens ()
  {
    return m_aReturnTypeTokens;
  }

  /**
   * @return the parameter_list_tokens
   */
  public List <Token> getParameterListTokens ()
  {
    return m_aParameterListTokens;
  }

  /**
   * @param aThrowsList
   *        the throws_list to set
   */
  public void setThrowsList (final List <List <Token>> aThrowsList)
  {
    m_aThrowsList = aThrowsList;
  }

  /**
   * @return the throws_list
   */
  public List <List <Token>> getThrowsList ()
  {
    return m_aThrowsList;
  }

  /**
   * @param aExpansion
   *        the expansion to set
   */
  public void setExpansion (final Expansion aExpansion)
  {
    m_aExpansion = aExpansion;
  }

  /**
   * @return the expansion
   */
  public Expansion getExpansion ()
  {
    return m_aExpansion;
  }

  /**
   * @param bEmptyPossible
   *        the emptyPossible to set
   */
  boolean setEmptyPossible (final boolean bEmptyPossible)
  {
    m_bEmptyPossible = bEmptyPossible;
    return bEmptyPossible;
  }

  /**
   * @return the emptyPossible
   */
  boolean isEmptyPossible ()
  {
    return m_bEmptyPossible;
  }

  /**
   * @param aLeftExpansions
   *        the leftExpansions to set
   */
  void setLeftExpansions (final AbstractNormalProduction [] aLeftExpansions)
  {
    m_aLeftExpansions = aLeftExpansions;
  }

  /**
   * @return the leftExpansions
   */
  AbstractNormalProduction [] getLeftExpansions ()
  {
    return m_aLeftExpansions;
  }

  /**
   * @param nWalkStatus
   *        the walkStatus to set
   */
  void setWalkStatus (final int nWalkStatus)
  {
    m_nWalkStatus = nWalkStatus;
  }

  /**
   * @return the walkStatus
   */
  int getWalkStatus ()
  {
    return m_nWalkStatus;
  }

  /**
   * @param aFirstToken
   *        the firstToken to set
   * @return parameter token
   */
  public Token setFirstToken (final Token aFirstToken)
  {
    m_aFirstToken = aFirstToken;
    return aFirstToken;
  }

  /**
   * @return the firstToken
   */
  public Token getFirstToken ()
  {
    return m_aFirstToken;
  }

  /**
   * @param aLastToken
   *        the lastToken to set
   */
  public void setLastToken (final Token aLastToken)
  {
    m_aLastToken = aLastToken;
  }

  /**
   * @return the lastToken
   */
  public Token getLastToken ()
  {
    return m_aLastToken;
  }

}
