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

import org.jspecify.annotations.Nullable;

  /**
   * One node built by a production, and everything the generated code needs to build it: the
   * variable names, the child count and the point at which the node is closed.
   */
public class NodeScope
{
  private ASTProduction m_aProduction;

  /**
   * The production.
   *
   * @return The value of m_aProduction.
   */
  public ASTProduction getProduction ()
  {
    return m_aProduction;
  }

  /**
   * The production.
   *
   * @param aValue
   *        The new value of m_aProduction.
   */
  public void setProduction (final ASTProduction aValue)
  {
    m_aProduction = aValue;
  }

  private ASTNodeDescriptor m_aNodeDescriptor;

  /**
   * The node descriptor.
   *
   * @param aValue
   *        The new value of m_aNodeDescriptor.
   */
  public void setNodeDescriptor (final ASTNodeDescriptor aValue)
  {
    m_aNodeDescriptor = aValue;
  }

  private String m_sClosedVar;

  /**
   * The closed var.
   *
   * @return The value of m_sClosedVar.
   */
  public String getClosedVar ()
  {
    return m_sClosedVar;
  }

  /**
   * The closed var.
   *
   * @param aValue
   *        The new value of m_sClosedVar.
   */
  public void setClosedVar (final String aValue)
  {
    m_sClosedVar = aValue;
  }

  private String m_sExceptionVar;

  /**
   * The exception var.
   *
   * @return The value of m_sExceptionVar.
   */
  public String getExceptionVar ()
  {
    return m_sExceptionVar;
  }

  /**
   * The exception var.
   *
   * @param aValue
   *        The new value of m_sExceptionVar.
   */
  public void setExceptionVar (final String aValue)
  {
    m_sExceptionVar = aValue;
  }

  private String m_sNodeVar;

  /**
   * The node var.
   *
   * @return The value of m_sNodeVar.
   */
  public String getNodeVar ()
  {
    return m_sNodeVar;
  }

  /**
   * The node var.
   *
   * @param aValue
   *        The new value of m_sNodeVar.
   */
  public void setNodeVar (final String aValue)
  {
    m_sNodeVar = aValue;
  }

  private int m_nScopeNumber;

  /**
   * The scope number.
   *
   * @return The value of m_nScopeNumber.
   */
  public int getScopeNumber ()
  {
    return m_nScopeNumber;
  }

  /**
   * The scope number.
   *
   * @param aValue
   *        The new value of m_nScopeNumber.
   */
  public void setScopeNumber (final int aValue)
  {
    m_nScopeNumber = aValue;
  }

  NodeScope (final ASTProduction p, @Nullable final ASTNodeDescriptor n)
  {
    m_aProduction = p;

    if (n == null)
    {
      String sNm = m_aProduction.getName ();
      if (JJTreeOptions.isNodeDefaultVoid ())
      {
        sNm = "void";
      }
      m_aNodeDescriptor = ASTNodeDescriptor.indefinite (sNm);
    }
    else
    {
      m_aNodeDescriptor = n;
    }

    m_nScopeNumber = m_aProduction.getNodeScopeNumber (this);
    m_sNodeVar = constructVariable ("n");
    m_sClosedVar = constructVariable ("c");
    m_sExceptionVar = constructVariable ("e");
  }

  boolean isVoid ()
  {
    return m_aNodeDescriptor.isVoid ();
  }

  ASTNodeDescriptor getNodeDescriptor ()
  {
    return m_aNodeDescriptor;
  }

  String getNodeDescriptorText ()
  {
    return m_aNodeDescriptor.getDescriptor ();
  }

  String getNodeVariable ()
  {
    return m_sNodeVar;
  }

  private String constructVariable (final String sId)
  {
    final String s = "000" + m_nScopeNumber;
    return "jjt" + sId + s.substring (s.length () - 3, s.length ());
  }

  boolean usesCloseNodeVar ()
  {
    return true;
  }

  @Nullable
  static NodeScope getEnclosingNodeScope (@NonNull final Node aNode)
  {
    if (aNode instanceof final ASTBNFDeclaration aASTBNFDeclaration)
    {
      return aASTBNFDeclaration.getNodeScope ();
    }
    for (Node n = aNode.jjtGetParent (); n != null; n = n.jjtGetParent ())
    {
      if (n instanceof ASTBNFDeclaration)
      {
        return ((ASTBNFDeclaration) n).getNodeScope ();
      }
      else
        if (n instanceof final ASTBNFNodeScope aASTBNFNodeScope)
        {
          return aASTBNFNodeScope.getNodeScope ();
        }
        else
          if (n instanceof final ASTExpansionNodeScope aASTExpansionNodeScope)
          {
            return aASTExpansionNodeScope.getNodeScope ();
          }
    }
    return null;
  }

}
