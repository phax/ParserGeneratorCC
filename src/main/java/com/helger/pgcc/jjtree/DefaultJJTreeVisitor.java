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

public class DefaultJJTreeVisitor implements JJTreeParserVisitor
{
  public Object defaultVisit (final SimpleNode node, final Object data)
  {
    return node.childrenAccept (this, data);
  }

  public Object visit (final SimpleNode node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTGrammar node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTCompilationUnit node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTProductions node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTOptions node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTOptionBinding node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTJavacode node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTJavacodeBody node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNF node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFDeclaration node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFNodeScope node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTRE node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTTokenDecls node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTRESpec node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFChoice node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFSequence node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFLookahead node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTExpansionNodeScope node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFAction node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFZeroOrOne node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFTryBlock node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFNonTerminal node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFAssignment node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFOneOrMore node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFZeroOrMore node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTBNFParenthesized node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREStringLiteral node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTRENamed node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREReference node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREEOF node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREChoice node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTRESequence node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREOneOrMore node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREZeroOrMore node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREZeroOrOne node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTRRepetitionRange node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTREParenthesized node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTRECharList node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTCharDescriptor node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTNodeDescriptor node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTNodeDescriptorExpression node, final Object data)
  {
    return defaultVisit (node, data);
  }

  public Object visit (final ASTPrimaryExpression node, final Object data)
  {
    return defaultVisit (node, data);
  }
}
