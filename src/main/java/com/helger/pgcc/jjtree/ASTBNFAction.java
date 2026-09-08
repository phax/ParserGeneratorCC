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

/**
 * A Java action block inside a BNF production of a JJTree grammar.
 */
public class ASTBNFAction extends JJTreeNode
{
  /**
   * @param nID
   *        The node id assigned by JJTree.
   */
  ASTBNFAction (final int nID)
  {
    super (nID);
  }

  /**
   * Walk up the tree for the node scope this action sits inside, if it is a different one than
   * the scope given.
   *
   * @param aNs
   *        The scope to stop at. May be <code>null</code>.
   * @return The enclosing scoping node, or <code>null</code> if there is none above the given
   *         scope.
   */
  protected Node getScopingParent (final NodeScope aNs)
  {
    for (Node n = this.jjtGetParent (); n != null; n = n.jjtGetParent ())
    {
      if (n instanceof final ASTBNFNodeScope aASTBNFNodeScope)
      {
        if (aASTBNFNodeScope.m_aNodeScope == aNs)
        {
          return n;
        }
      }
      else
        if (n instanceof final ASTExpansionNodeScope aASTExpansionNodeScope)
        {
          if (aASTExpansionNodeScope.m_aNodeScope == aNs)
          {
            return n;
          }
        }
    }
    return null;
  }

  /** Accept the visitor. **/
  @Override
  public Object jjtAccept (@NonNull final JJTreeParserVisitor aVisitor, final Object aData)
  {
    return aVisitor.visit (this, aData);
  }
}
