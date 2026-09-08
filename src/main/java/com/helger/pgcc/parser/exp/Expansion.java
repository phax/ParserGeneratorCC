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
package com.helger.pgcc.parser.exp;

import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.IGrammarLocation;

/**
 * Describes expansions - entities that may occur on the right hand sides of productions. This is
 * the base class of a bunch of other more specific classes.
 */
public sealed class Expansion implements IGrammarLocation permits
                              ExpAction,
                              ExpChoice,
                              ExpLookahead,
                              ExpNonTerminal,
                              ExpOneOrMore,
                              ExpSequence,
                              ExpTryBlock,
                              ExpZeroOrMore,
                              ExpZeroOrOne,
                              AbstractExpRegularExpression
{
  /** Default constructor. */
  public Expansion ()
  {}

  /**
   * The line separator the dump methods use.
   */
  protected static final String EOL = System.getProperty ("line.separator", "\n");

  /**
   * The line and column number of the construct that corresponds most closely to this node.
   */
  private int m_nLine;
  private int m_nColumn;

  /**
   * An internal name for this expansion. This is used to generate parser routines.
   */
  private String m_sInternalName = "";
  private int m_nInternalIndex = -1;

  /**
   * The parent of this expansion node. In case this is the top level expansion of the production it
   * is a reference to the production node otherwise it is a reference to another Expansion node. In
   * case this is the top level of a lookahead expansion,then the parent is null.
   */
  private Object m_aParent;

  /**
   * The ordinal of this node with respect to its parent.
   */
  private int m_nOrdinalBase;

  /**
   * To avoid right-recursive loops when calculating follow sets, we use a generation number which
   * indicates if this expansion was visited by LookaheadWalk.genFollowSetRecursive in the same
   * generation. New generations are obtained by incrementing the static counter below, and the
   * current generation is stored in the non-static variable below.
   */
  private long m_nMyGeneration = 0;

  /**
   * This flag is used for bookkeeping by the minimumSize method in class ParseEngine.
   */
  private boolean m_bInMinimumSize = false;

  /**
   * {@return a generation number that has not been used before in this run, for a follow set walk
   * to mark the expansions it has already visited}
   */
  public static long getNextGenerationIndex ()
  {
    return PGCCContext.current ().grammar ().getAndIncNextExpansionGeneration ();
  }

  /**
   * Give this expansion the name the generated parser routine will carry.
   *
   * @param sPrefix
   *        The name prefix, which says what kind of routine it is. May not be <code>null</code>.
   * @param nIndex
   *        The number that makes the name unique.
   */
  public final void setInternalName (final String sPrefix, final int nIndex)
  {
    m_sInternalName = sPrefix + nIndex;
    m_nInternalIndex = nIndex;
  }

  /**
   * Give this expansion a name without a number behind it, for the cases that do not need one.
   *
   * @param sName
   *        The name. May not be <code>null</code>.
   */
  public final void setInternalNameOnly (final String sName)
  {
    m_sInternalName = sName;
  }

  /**
   * {@return <code>true</code> if this expansion has not been named yet}
   */
  public final boolean hasNoInternalName ()
  {
    return StringHelper.isEmpty (m_sInternalName);
  }

  /**
   * {@return the name of the parser routine generated for this expansion, empty if it has none}
   */
  public final String getInternalName ()
  {
    return m_sInternalName;
  }

  /**
   * {@return the number behind the internal name, or -1 if the name carries none}
   */
  public final int getInternalIndex ()
  {
    return m_nInternalIndex;
  }

  private String _getSimpleName ()
  {
    final String sName = getClass ().getName ();
    // strip the package name
    return sName.substring (sName.lastIndexOf (".") + 1);
  }

  /**
   * Build the indentation the dump methods put in front of a line.
   *
   * @param nIndent
   *        The nesting depth.
   * @return A builder holding two spaces per level. Never <code>null</code>.
   */
  @NonNull
  protected static StringBuilder dumpPrefix (final int nIndent)
  {
    final StringBuilder aSB = new StringBuilder (nIndent * 2);
    for (int i = 0; i < nIndent; i++)
      aSB.append ("  ");
    return aSB;
  }

  /**
   * Render this expansion and everything below it, for debugging.
   *
   * @param nIndent
   *        indentation level
   * @param aAlreadyDumped
   *        what was already dumped?
   * @return String
   */
  @OverrideOnDemand
  public StringBuilder dump (final int nIndent, final Set <? super Expansion> aAlreadyDumped)
  {
    return dumpPrefix (nIndent).append (System.identityHashCode (this)).append (' ').append (_getSimpleName ());
  }

  /**
   * Where in the grammar this expansion is written.
   *
   * @return the column
   */
  public final int getColumnNumber ()
  {
    return m_nColumn;
  }

  /**
   * Where in the grammar this expansion is written.
   *
   * @param nColumn
   *        the column to set
   */
  public final void setColumnNumber (final int nColumn)
  {
    m_nColumn = nColumn;
  }

  /**
   * Where in the grammar this expansion is written.
   *
   * @return the line
   */
  public final int getLineNumber ()
  {
    return m_nLine;
  }

  /**
   * Where in the grammar this expansion is written.
   *
   * @param nLine
   *        the line to set
   */
  public final void setLineNumber (final int nLine)
  {
    m_nLine = nLine;
  }

  /**
   * {@return the expansion this one sits inside, the production if this is its top level, or
   * <code>null</code> if this is the top level of a lookahead}
   */
  public final Object getParent ()
  {
    return m_aParent;
  }

  /**
   * Record where this expansion sits.
   *
   * @param o
   *        The enclosing expansion or production. May be <code>null</code>.
   */
  public final void setParent (final Object o)
  {
    m_aParent = o;
  }

  /**
   * {@return the position of this expansion among its parent's children}
   */
  public final int getOrdinalBase ()
  {
    return m_nOrdinalBase;
  }

  /**
   * Record the position of this expansion among its parent's children.
   *
   * @param n
   *        The position.
   */
  public final void setOrdinalBase (final int n)
  {
    m_nOrdinalBase = n;
  }

  /**
   * {@return the generation this expansion was last visited in by a follow set walk}
   */
  public final long getMyGeneration ()
  {
    return m_nMyGeneration;
  }

  /**
   * Mark this expansion as visited in a generation, so that a right recursive grammar does not
   * send the follow set walk round forever.
   *
   * @param n
   *        The current generation, from {@link #getNextGenerationIndex()}.
   */
  public final void setMyGeneration (final long n)
  {
    m_nMyGeneration = n;
  }

  /**
   * {@return <code>true</code> while the minimum size computation in ParseEngine is inside this
   * expansion, which is how it recognises a cycle}
   */
  public final boolean isInMinimumSize ()
  {
    return m_bInMinimumSize;
  }

  /**
   * Mark that the minimum size computation has entered or left this expansion.
   *
   * @param b
   *        <code>true</code> on the way in, <code>false</code> on the way out.
   */
  public final void setInMinimumSize (final boolean b)
  {
    m_bInMinimumSize = b;
  }

  /**
   * A re-implementing of Object.hashCode() to be deterministic. This uses the line and column
   * fields to generate an arbitrary number - we assume that this method is called only after line
   * and column are set to their actual values.
   */
  @Override
  public int hashCode ()
  {
    return getLineNumber () + getColumnNumber ();
  }

  @Override
  public String toString ()
  {
    return "[" +
           getLineNumber () +
           "," +
           getColumnNumber () +
           " " +
           System.identityHashCode (this) +
           " " +
           _getSimpleName () +
           "]";
  }
}
