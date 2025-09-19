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
package com.helger.pgcc.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.helger.pgcc.parser.exp.Expansion;

/**
 * Describes JavaCC productions.
 */
public abstract class NormalProduction
{
  protected static final String EOL = System.getProperty ("line.separator", "\n");

  /**
   * The line and column number of the construct that corresponds most closely
   * to this node.
   */
  private int m_column;

  private int m_line;

  /**
   * The NonTerminal nodes which refer to this production.
   */
  private List <Expansion> m_parents = new ArrayList <> ();

  /**
   * The access modifier of this production.
   */
  private String m_accessMod;

  /**
   * The name of the non-terminal of this production.
   */
  private String m_lhs;

  /**
   * The tokens that make up the return type of this production.
   */
  private final List <Token> m_return_type_tokens = new ArrayList <> ();

  /**
   * The tokens that make up the parameters of this production.
   */
  private final List <Token> m_parameter_list_tokens = new ArrayList <> ();

  /**
   * Each entry in this list is a list of tokens that represents an exception in
   * the throws list of this production. This list does not include
   * ParseException which is always thrown.
   */
  private List <List <Token>> m_throws_list = new ArrayList <> ();

  /**
   * The RHS of this production. Not used for JavaCodeProduction.
   */
  private Expansion m_expansion;

  /**
   * This boolean flag is true if this production can expand to empty.
   */
  private boolean m_emptyPossible = false;

  /**
   * A list of all non-terminals that this one can expand to without having to
   * consume any tokens. Also an index that shows how many pointers exist.
   */
  private NormalProduction [] m_leftExpansions = new NormalProduction [10];
  int m_leIndex = 0;

  /**
   * The following variable is used to maintain state information for the
   * left-recursion determination algorithm: It is initialized to 0, and set to
   * -1 if this node has been visited in a pre-order walk, and then it is set to
   * 1 if the pre-order walk of the whole graph from this node has been
   * traversed. i.e., -1 indicates partially processed, and 1 indicates fully
   * processed.
   */
  private int m_walkStatus = 0;

  /**
   * The first and last tokens from the input stream that represent this
   * production.
   */
  private Token m_lastToken;

  private Token m_firstToken;

  protected StringBuilder dumpPrefix (final int indent)
  {
    final StringBuilder sb = new StringBuilder (128);
    for (int i = 0; i < indent; i++)
      sb.append ("  ");
    return sb;
  }

  protected String getSimpleName ()
  {
    final String name = getClass ().getName ();
    return name.substring (name.lastIndexOf (".") + 1); // strip the package
                                                        // name
  }

  public StringBuilder dump (final int indent, final Set <? super NormalProduction> alreadyDumped)
  {
    final StringBuilder sb = dumpPrefix (indent).append (System.identityHashCode (this))
                                                .append (' ')
                                                .append (getSimpleName ())
                                                .append (' ')
                                                .append (getLhs ());
    if (!alreadyDumped.contains (this))
    {
      alreadyDumped.add (this);
      if (getExpansion () != null)
      {
        // cannot re-use already dumped
        sb.append (EOL).append (getExpansion ().dump (indent + 1, new HashSet <> ()));
      }
    }

    return sb;
  }

  /**
   * @param line
   *        the line to set
   */
  public void setLine (final int line)
  {
    this.m_line = line;
  }

  /**
   * @return the line
   */
  public int getLine ()
  {
    return m_line;
  }

  /**
   * @param column
   *        the column to set
   */
  public void setColumn (final int column)
  {
    this.m_column = column;
  }

  /**
   * @return the column
   */
  public int getColumn ()
  {
    return m_column;
  }

  /**
   * @param parents
   *        the parents to set
   */
  void setParents (final List <Expansion> parents)
  {
    this.m_parents = parents;
  }

  /**
   * @return the parents
   */
  List <Expansion> getParents ()
  {
    return m_parents;
  }

  /**
   * @param accessMod
   *        the accessMod to set
   */
  public void setAccessMod (final String accessMod)
  {
    this.m_accessMod = accessMod;
  }

  /**
   * @return the accessMod
   */
  public String getAccessMod ()
  {
    return m_accessMod;
  }

  /**
   * @param lhs
   *        the lhs to set
   */
  public void setLhs (final String lhs)
  {
    this.m_lhs = lhs;
  }

  /**
   * @return the lhs
   */
  public String getLhs ()
  {
    return m_lhs;
  }

  /**
   * @return the return_type_tokens
   */
  public List <Token> getReturnTypeTokens ()
  {
    return m_return_type_tokens;
  }

  /**
   * @return the parameter_list_tokens
   */
  public List <Token> getParameterListTokens ()
  {
    return m_parameter_list_tokens;
  }

  /**
   * @param throws_list
   *        the throws_list to set
   */
  public void setThrowsList (final List <List <Token>> throws_list)
  {
    this.m_throws_list = throws_list;
  }

  /**
   * @return the throws_list
   */
  public List <List <Token>> getThrowsList ()
  {
    return m_throws_list;
  }

  /**
   * @param expansion
   *        the expansion to set
   */
  public void setExpansion (final Expansion expansion)
  {
    this.m_expansion = expansion;
  }

  /**
   * @return the expansion
   */
  public Expansion getExpansion ()
  {
    return m_expansion;
  }

  /**
   * @param emptyPossible
   *        the emptyPossible to set
   */
  boolean setEmptyPossible (final boolean emptyPossible)
  {
    this.m_emptyPossible = emptyPossible;
    return emptyPossible;
  }

  /**
   * @return the emptyPossible
   */
  boolean isEmptyPossible ()
  {
    return m_emptyPossible;
  }

  /**
   * @param leftExpansions
   *        the leftExpansions to set
   */
  void setLeftExpansions (final NormalProduction [] leftExpansions)
  {
    this.m_leftExpansions = leftExpansions;
  }

  /**
   * @return the leftExpansions
   */
  NormalProduction [] getLeftExpansions ()
  {
    return m_leftExpansions;
  }

  /**
   * @param walkStatus
   *        the walkStatus to set
   */
  void setWalkStatus (final int walkStatus)
  {
    this.m_walkStatus = walkStatus;
  }

  /**
   * @return the walkStatus
   */
  int getWalkStatus ()
  {
    return m_walkStatus;
  }

  /**
   * @param firstToken
   *        the firstToken to set
   * @return parameter token
   */
  public Token setFirstToken (final Token firstToken)
  {
    this.m_firstToken = firstToken;
    return firstToken;
  }

  /**
   * @return the firstToken
   */
  public Token getFirstToken ()
  {
    return m_firstToken;
  }

  /**
   * @param lastToken
   *        the lastToken to set
   */
  public void setLastToken (final Token lastToken)
  {
    this.m_lastToken = lastToken;
  }

  /**
   * @return the lastToken
   */
  public Token getLastToken ()
  {
    return m_lastToken;
  }

}
