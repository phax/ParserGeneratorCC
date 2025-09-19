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
package com.helger.pgcc.parser.exp;

import java.util.Set;

import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.string.StringHelper;

import jakarta.annotation.Nonnull;

/**
 * Describes expansions - entities that may occur on the right hand sides of
 * productions. This is the base class of a bunch of other more specific
 * classes.
 */
public class Expansion
{
  protected static final String EOL = System.getProperty ("line.separator", "\n");

  /**
   * The line and column number of the construct that corresponds most closely
   * to this node.
   */
  private int m_nLine;
  private int m_nColumn;

  /**
   * An internal name for this expansion. This is used to generate parser
   * routines.
   */
  private String m_sInternalName = "";
  private int m_nInternalIndex = -1;

  /**
   * The parent of this expansion node. In case this is the top level expansion
   * of the production it is a reference to the production node otherwise it is
   * a reference to another Expansion node. In case this is the top level of a
   * lookahead expansion,then the parent is null.
   */
  private Object m_parent;

  /**
   * The ordinal of this node with respect to its parent.
   */
  private int m_ordinalBase;

  /**
   * To avoid right-recursive loops when calculating follow sets, we use a
   * generation number which indicates if this expansion was visited by
   * LookaheadWalk.genFollowSet in the same generation. New generations are
   * obtained by incrementing the static counter below, and the current
   * generation is stored in the non-static variable below.
   */
  private static long s_nextGenerationIndex = 1;
  private long m_myGeneration = 0;

  /**
   * This flag is used for bookkeeping by the minimumSize method in class
   * ParseEngine.
   */
  private boolean m_inMinimumSize = false;

  public static void reInit ()
  {
    s_nextGenerationIndex = 1;
  }

  public static long getNextGenerationIndex ()
  {
    return s_nextGenerationIndex++;
  }

  public final void setInternalName (final String sPrefix, final int nIndex)
  {
    m_sInternalName = sPrefix + nIndex;
    m_nInternalIndex = nIndex;
  }

  public final void setInternalNameOnly (final String sName)
  {
    m_sInternalName = sName;
  }

  public final boolean hasNoInternalName ()
  {
    return StringHelper.isEmpty (m_sInternalName);
  }

  public final String getInternalName ()
  {
    return m_sInternalName;
  }

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

  @Nonnull
  protected static StringBuilder dumpPrefix (final int indent)
  {
    final StringBuilder sb = new StringBuilder (indent * 2);
    for (int i = 0; i < indent; i++)
      sb.append ("  ");
    return sb;
  }

  /**
   * @param indent
   *        indentation level
   * @param alreadyDumped
   *        what was already dumped?
   * @return String
   */
  @OverrideOnDemand
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    return dumpPrefix (indent).append (System.identityHashCode (this)).append (" ").append (_getSimpleName ());
  }

  /**
   * @return the column
   */
  public final int getColumn ()
  {
    return m_nColumn;
  }

  /**
   * @param column
   *        the column to set
   */
  public final void setColumn (final int column)
  {
    m_nColumn = column;
  }

  /**
   * @return the line
   */
  public final int getLine ()
  {
    return m_nLine;
  }

  /**
   * @param line
   *        the line to set
   */
  public final void setLine (final int line)
  {
    m_nLine = line;
  }

  public final Object getParent ()
  {
    return m_parent;
  }

  public final void setParent (final Object o)
  {
    m_parent = o;
  }

  public final int getOrdinalBase ()
  {
    return m_ordinalBase;
  }

  public final void setOrdinalBase (final int n)
  {
    m_ordinalBase = n;
  }

  public final long getMyGeneration ()
  {
    return m_myGeneration;
  }

  public final void setMyGeneration (final long n)
  {
    m_myGeneration = n;
  }

  public final boolean isInMinimumSize ()
  {
    return m_inMinimumSize;
  }

  public final void setInMinimumSize (final boolean b)
  {
    m_inMinimumSize = b;
  }

  /**
   * A reimplementing of Object.hashCode() to be deterministic. This uses the
   * line and column fields to generate an arbitrary number - we assume that
   * this method is called only after line and column are set to their actual
   * values.
   */
  @Override
  public int hashCode ()
  {
    return getLine () + getColumn ();
  }

  @Override
  public String toString ()
  {
    return "[" + getLine () + "," + getColumn () + " " + System.identityHashCode (this) + " " + _getSimpleName () + "]";
  }
}
