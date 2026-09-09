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

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.context.PGCCContext;

/**
 * A node descriptor - the <code>#Name</code> or <code>#Name(expr)</code> that says which node a
 * production builds and when.
 */
public class ASTNodeDescriptor extends JJTreeNode
{
  static ASTNodeDescriptor indefinite (final String s)
  {
    final ASTNodeDescriptor aNd = new ASTNodeDescriptor (JJTreeParserTreeConstants.JJTNODEDESCRIPTOR);
    aNd.setName (s);
    aNd.setNodeIdValue ();
    aNd.m_bFaked = true;
    return aNd;
  }

  /**
   * {@return the constant name of every node type seen so far, in declaration order}
   */
  @NonNull
  public static List <String> getNodeIds ()
  {
    return PGCCContext.current ().jjtree ().nodeIds ();
  }

  /**
   * {@return the class name of every node type seen so far, in declaration order}
   */
  @NonNull
  public static List <String> getNodeNames ()
  {
    return PGCCContext.current ().jjtree ().nodeNames ();
  }

  /**
   * Whether this descriptor was invented rather than written by the user, which happens when a
   * production builds a node without saying so.
   */
  private boolean m_bFaked = false;
  /**
   * The node type this descriptor names.
   */
  private String m_sName;

  /**
   * The name.
   *
   * @return The value of m_sName.
   */
  public String getName ()
  {
    return m_sName;
  }

  /**
   * The name.
   *
   * @param aValue
   *        The new value of m_sName.
   */
  public void setName (final String aValue)
  {
    m_sName = aValue;
  }

  /**
   * Whether the descriptor was written as <code>#Name(&gt;expr)</code>, which means the expression
   * counts children rather than deciding whether to build the node at all.
   */
  private boolean m_bIsGT;

  /**
   * The is g t.
   *
   * @return The value of m_bIsGT.
   */
  public boolean isGT ()
  {
    return m_bIsGT;
  }

  /**
   * The is g t.
   *
   * @param aValue
   *        The new value of m_bIsGT.
   */
  public void setGT (final boolean aValue)
  {
    m_bIsGT = aValue;
  }

  /**
   * The expression in brackets after the node name, or <code>null</code> if there is none.
   */
  private ASTNodeDescriptorExpression m_aExpression;

  /**
   * The expression.
   *
   * @return The value of m_aExpression.
   */
  public ASTNodeDescriptorExpression getExpression ()
  {
    return m_aExpression;
  }

  /**
   * The expression.
   *
   * @param aValue
   *        The new value of m_aExpression.
   */
  public void setExpression (final ASTNodeDescriptorExpression aValue)
  {
    m_aExpression = aValue;
  }

  ASTNodeDescriptor (final int nID)
  {
    super (nID);
  }

  void setNodeIdValue ()
  {
    final String k = getNodeId ();
    if (!PGCCContext.current ().jjtree ().nodeSeen ().containsKey (k))
    {
      PGCCContext.current ().jjtree ().nodeSeen ().put (k, k);
      PGCCContext.current ().jjtree ().nodeNames ().add (m_sName);
      PGCCContext.current ().jjtree ().nodeIds ().add (k);
    }
  }

  String getNodeId ()
  {
    return "JJT" + m_sName.toUpperCase (Locale.US).replace ('.', '_');
  }

  boolean isVoid ()
  {
    return m_sName.equals ("void");
  }

  @Override
  public String toString ()
  {
    if (m_bFaked)
      return "(faked) " + m_sName;
    return super.toString () + ": " + m_sName;
  }

  String getDescriptor ()
  {
    if (m_aExpression == null)
    {
      return m_sName;
    }
    return "#" + m_sName + "(" + (m_bIsGT ? ">" : "") + expressionText () + ")";
  }

  String getNodeType ()
  {
    if (JJTreeOptions.isMulti ())
      return JJTreeOptions.getNodePrefix () + m_sName;
    return "SimpleNode";
  }

  String getNodeName ()
  {
    return m_sName;
  }

  String openNode (final String sNodeVar)
  {
    return "jjtree.openNodeScope(" + sNodeVar + ");";
  }

  String expressionText ()
  {
    if (m_aExpression.getFirstToken ().image.equals (")") && m_aExpression.getLastToken ().image.equals ("("))
    {
      return "true";
    }

    String s = "";
    Token t = m_aExpression.getFirstToken ();
    while (true)
    {
      s += " " + t.image;
      if (t == m_aExpression.getLastToken ())
      {
        break;
      }
      t = t.next;
    }
    return s;
  }

  String closeNode (final String sNodeVar)
  {
    if (m_aExpression == null)
      return "jjtree.closeNodeScope(" + sNodeVar + ", true);";
    if (m_bIsGT)
      return "jjtree.closeNodeScope(" + sNodeVar + ", jjtree.nodeArity() > " + expressionText () + ");";
    return "jjtree.closeNodeScope(" + sNodeVar + ", " + expressionText () + ");";
  }

  @Override
  String translateImage (final Token t)
  {
    return whiteOut (t);
  }

  /** Accept the visitor. **/
  @Override
  public Object jjtAccept (@NonNull final JJTreeParserVisitor aVisitor, final Object aData)
  {
    return aVisitor.visit (this, aData);
  }

}

/* end */
