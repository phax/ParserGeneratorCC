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
package com.helger.pgcc.jjtree;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.OverrideOnDemand;

/**
 * The base of every node in JJTree's own syntax tree. On top of what SimpleNode gives it, a node
 * remembers the first and the last token it covers, which is what lets JJTree copy stretches of the
 * grammar back out with their original layout.
 */
public class JJTreeNode extends SimpleNode
{
  /**
   * The position of this node among its parent's children.
   */
  private int m_nMyOrdinal;

  /**
   * Create a node of the given kind.
   *
   * @param nID
   *        The node kind, one of the constants JJTree generates.
   */
  public JJTreeNode (final int nID)
  {
    super (nID);
  }

  /**
   * The constructor the generated parser calls. The parser argument is not used.
   *
   * @param p
   *        The parser that is building the tree. Ignored.
   * @param nID
   *        The node kind, one of the constants JJTree generates.
   */
  public JJTreeNode (@SuppressWarnings ("unused") final JJTreeParser p, final int nID)
  {
    // Ignore parser - whysoever
    this (nID);
  }

  /**
   * The factory method the generated parser calls.
   *
   * @param nId
   *        The node kind, one of the constants JJTree generates.
   * @return The new node. Never <code>null</code>.
   */
  public static Node jjtCreate (final int nId)
  {
    return new JJTreeNode (nId);
  }

  @Override
  public void jjtAddChild (final Node n, final int i)
  {
    super.jjtAddChild (n, i);
    ((JJTreeNode) n).setOrdinal (i);
  }

  /**
   * {@return the position of this node among its parent's children}
   */
  public int getOrdinal ()
  {
    return m_nMyOrdinal;
  }

  /**
   * Record the position of this node among its parent's children.
   *
   * @param o
   *        The position.
   */
  public void setOrdinal (final int o)
  {
    m_nMyOrdinal = o;
  }

  /*****************************************************************
   * The following is added manually to enhance all tree nodes with attributes that store the first
   * and last tokens corresponding to each node, as well as to print the tokens back to the
   * specified output stream.
   *****************************************************************/

  /**
   * The first token this node covers.
   */
  private Token m_aFirst;
  /**
   * The last token this node covers.
   */
  private Token m_aLast;

  /**
   * {@return the first token this node covers, or <code>null</code> if it covers none}
   */
  public Token getFirstToken ()
  {
    return m_aFirst;
  }

  /**
   * Record where the text of this node begins.
   *
   * @param t
   *        The first token. May be <code>null</code>.
   */
  public void setFirstToken (final Token t)
  {
    m_aFirst = t;
  }

  /**
   * {@return the last token this node covers, or <code>null</code> if it covers none}
   */
  public Token getLastToken ()
  {
    return m_aLast;
  }

  /**
   * Record where the text of this node ends.
   *
   * @param t
   *        The last token. May be <code>null</code>.
   */
  public void setLastToken (final Token t)
  {
    m_aLast = t;
  }

  @OverrideOnDemand
  String translateImage (@NonNull final Token t)
  {
    return t.image;
  }

  static String whiteOut (@NonNull final Token t)
  {
    final StringBuilder aSB = new StringBuilder (t.image.length ());

    for (final char ch : t.image.toCharArray ())
    {
      if (ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f')
        aSB.append (' ');
      else
        aSB.append (ch);
    }

    return aSB.toString ();
  }

  /**
   * Indicates whether the token should be replaced by white space or replaced with the actual node
   * variable.
   */
  private boolean m_bWhitingOut = false;

  /**
   * Copy one token and the comments attached to it into the output, keeping its original position.
   *
   * @param t
   *        The token to copy. May not be <code>null</code>.
   * @param aIo
   *        Where to copy it. May not be <code>null</code>.
   */
  protected void print (@NonNull final Token t, @NonNull final JJTreeIO aIo)
  {
    Token aTt = t.specialToken;
    if (aTt != null)
    {
      while (aTt.specialToken != null)
        aTt = aTt.specialToken;
      while (aTt != null)
      {
        aIo.print (TokenUtils.addUnicodeEscapes (translateImage (aTt)));
        aTt = aTt.next;
      }
    }

    /*
     * If we're within a node scope we modify the source in the following ways: 1) we rename all
     * references to `jjtThis' to be references to the actual node variable. 2) we replace all calls
     * to `jjtree.currentNode()' with references to the node variable.
     */

    final NodeScope s = NodeScope.getEnclosingNodeScope (this);
    if (s == null)
    {
      /*
       * Not within a node scope so we don't need to modify the source.
       */
      aIo.print (TokenUtils.addUnicodeEscapes (translateImage (t)));
      return;
    }

    if (t.image.equals ("jjtThis"))
    {
      aIo.print (s.getNodeVariable ());
      return;
    }
    else
      if (t.image.equals ("jjtree"))
      {
        if (t.next.image.equals ("."))
        {
          if (t.next.next.image.equals ("currentNode"))
          {
            if (t.next.next.next.image.equals ("("))
            {
              if (t.next.next.next.next.image.equals (")"))
              {
                /*
                 * Found `jjtree.currentNode()' so go into white out mode. We'll stay in this mode
                 * until we find the closing parenthesis.
                 */
                m_bWhitingOut = true;
              }
            }
          }
        }
      }
    if (m_bWhitingOut)
    {
      if (t.image.equals ("jjtree"))
      {
        aIo.print (s.getNodeVariable ());
        aIo.print (" ");
      }
      else
        if (t.image.equals (")"))
        {
          aIo.print (" ");
          m_bWhitingOut = false;
        }
        else
        {
          for (int i = 0; i < t.image.length (); ++i)
          {
            aIo.print (" ");
          }
        }
      return;
    }

    aIo.print (TokenUtils.addUnicodeEscapes (translateImage (t)));
  }
}
