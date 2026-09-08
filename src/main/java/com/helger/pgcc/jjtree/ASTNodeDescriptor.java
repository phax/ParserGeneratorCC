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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.context.PGCCContext;

public class ASTNodeDescriptor extends JJTreeNode
{
  static ASTNodeDescriptor indefinite (final String s)
  {
    final ASTNodeDescriptor aNd = new ASTNodeDescriptor (JJTreeParserTreeConstants.JJTNODEDESCRIPTOR);
    aNd.m_name = s;
    aNd.setNodeIdValue ();
    aNd.m_faked = true;
    return aNd;
  }

  @NonNull
  public static List <String> getNodeIds ()
  {
    return PGCCContext.current ().jjtree ().nodeIds ();
  }

  @NonNull
  public static List <String> getNodeNames ()
  {
    return PGCCContext.current ().jjtree ().nodeNames ();
  }

  private boolean m_faked = false;
  String m_name;
  boolean m_isGT;
  ASTNodeDescriptorExpression m_expression;

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
      PGCCContext.current ().jjtree ().nodeNames ().add (m_name);
      PGCCContext.current ().jjtree ().nodeIds ().add (k);
    }
  }

  String getNodeId ()
  {
    return "JJT" + m_name.toUpperCase (Locale.US).replace ('.', '_');
  }

  boolean isVoid ()
  {
    return m_name.equals ("void");
  }

  @Override
  public String toString ()
  {
    if (m_faked)
      return "(faked) " + m_name;
    return super.toString () + ": " + m_name;
  }

  String getDescriptor ()
  {
    if (m_expression == null)
    {
      return m_name;
    }
    return "#" + m_name + "(" + (m_isGT ? ">" : "") + expression_text () + ")";
  }

  String getNodeType ()
  {
    if (JJTreeOptions.isMulti ())
      return JJTreeOptions.getNodePrefix () + m_name;
    return "SimpleNode";
  }

  String getNodeName ()
  {
    return m_name;
  }

  String openNode (final String sNodeVar)
  {
    return "jjtree.openNodeScope(" + sNodeVar + ");";
  }

  String expression_text ()
  {
    if (m_expression.getFirstToken ().image.equals (")") && m_expression.getLastToken ().image.equals ("("))
    {
      return "true";
    }

    String s = "";
    Token t = m_expression.getFirstToken ();
    while (true)
    {
      s += " " + t.image;
      if (t == m_expression.getLastToken ())
      {
        break;
      }
      t = t.next;
    }
    return s;
  }

  String closeNode (final String sNodeVar)
  {
    if (m_expression == null)
      return "jjtree.closeNodeScope(" + sNodeVar + ", true);";
    if (m_isGT)
      return "jjtree.closeNodeScope(" + sNodeVar + ", jjtree.nodeArity() > " + expression_text () + ");";
    return "jjtree.closeNodeScope(" + sNodeVar + ", " + expression_text () + ");";
  }

  @Override
  String translateImage (final Token t)
  {
    return whiteOut (t);
  }

  /** Accept the visitor. **/
  @Override
  public Object jjtAccept (final JJTreeParserVisitor aVisitor, final Object aData)
  {
    return aVisitor.visit (this, aData);
  }

}

/* end */
