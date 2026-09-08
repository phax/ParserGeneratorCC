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

import org.jspecify.annotations.Nullable;

public class NodeScope
{
  ASTProduction m_aProduction;
  ASTNodeDescriptor m_aNodeDescriptor;

  String m_sClosedVar;
  String m_sExceptionVar;
  String m_sNodeVar;
  int m_nScopeNumber;

  NodeScope (final ASTProduction p, @Nullable final ASTNodeDescriptor n)
  {
    m_aProduction = p;

    if (n == null)
    {
      String sNm = m_aProduction.m_sName;
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
  static NodeScope getEnclosingNodeScope (final Node aNode)
  {
    if (aNode instanceof final ASTBNFDeclaration aASTBNFDeclaration)
    {
      return aASTBNFDeclaration.m_aNodeScope;
    }
    for (Node n = aNode.jjtGetParent (); n != null; n = n.jjtGetParent ())
    {
      if (n instanceof ASTBNFDeclaration)
      {
        return ((ASTBNFDeclaration) n).m_aNodeScope;
      }
      else
        if (n instanceof final ASTBNFNodeScope aASTBNFNodeScope)
        {
          return aASTBNFNodeScope.m_aNodeScope;
        }
        else
          if (n instanceof final ASTExpansionNodeScope aASTExpansionNodeScope)
          {
            return aASTExpansionNodeScope.m_aNodeScope;
          }
    }
    return null;
  }

}
