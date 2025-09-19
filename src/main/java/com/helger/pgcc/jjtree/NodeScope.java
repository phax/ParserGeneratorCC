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
package com.helger.pgcc.jjtree;

import jakarta.annotation.Nullable;

public class NodeScope
{
  ASTProduction m_production;
  ASTNodeDescriptor m_node_descriptor;

  String m_closedVar;
  String m_exceptionVar;
  String m_nodeVar;
  int m_scopeNumber;

  NodeScope (final ASTProduction p, @Nullable final ASTNodeDescriptor n)
  {
    m_production = p;

    if (n == null)
    {
      String nm = m_production.m_name;
      if (JJTreeOptions.isNodeDefaultVoid ())
      {
        nm = "void";
      }
      m_node_descriptor = ASTNodeDescriptor.indefinite (nm);
    }
    else
    {
      m_node_descriptor = n;
    }

    m_scopeNumber = m_production.getNodeScopeNumber (this);
    m_nodeVar = constructVariable ("n");
    m_closedVar = constructVariable ("c");
    m_exceptionVar = constructVariable ("e");
  }

  boolean isVoid ()
  {
    return m_node_descriptor.isVoid ();
  }

  ASTNodeDescriptor getNodeDescriptor ()
  {
    return m_node_descriptor;
  }

  String getNodeDescriptorText ()
  {
    return m_node_descriptor.getDescriptor ();
  }

  String getNodeVariable ()
  {
    return m_nodeVar;
  }

  private String constructVariable (final String id)
  {
    final String s = "000" + m_scopeNumber;
    return "jjt" + id + s.substring (s.length () - 3, s.length ());
  }

  boolean usesCloseNodeVar ()
  {
    return true;
  }

  @Nullable
  static NodeScope getEnclosingNodeScope (final Node node)
  {
    if (node instanceof ASTBNFDeclaration)
    {
      return ((ASTBNFDeclaration) node).m_node_scope;
    }
    for (Node n = node.jjtGetParent (); n != null; n = n.jjtGetParent ())
    {
      if (n instanceof ASTBNFDeclaration)
      {
        return ((ASTBNFDeclaration) n).m_node_scope;
      }
      else
        if (n instanceof ASTBNFNodeScope)
        {
          return ((ASTBNFNodeScope) n).m_node_scope;
        }
        else
          if (n instanceof ASTExpansionNodeScope)
          {
            return ((ASTExpansionNodeScope) n).m_node_scope;
          }
    }
    return null;
  }

}
