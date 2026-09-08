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

public class DefaultJJTreeVisitor implements JJTreeParserVisitor
{
  public Object defaultVisit (final SimpleNode aNode, final Object aData)
  {
    return aNode.childrenAccept (this, aData);
  }

  public Object visit (final SimpleNode aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTGrammar aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTCompilationUnit aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTProductions aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTOptions aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTOptionBinding aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTJavacode aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTJavacodeBody aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNF aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFDeclaration aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFNodeScope aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTRE aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTTokenDecls aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTRESpec aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFChoice aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFSequence aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFLookahead aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTExpansionNodeScope aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFAction aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFZeroOrOne aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFTryBlock aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFNonTerminal aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFAssignment aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFOneOrMore aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFZeroOrMore aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTBNFParenthesized aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREStringLiteral aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTRENamed aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREReference aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREEOF aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREChoice aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTRESequence aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREOneOrMore aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREZeroOrMore aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREZeroOrOne aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTRRepetitionRange aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTREParenthesized aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTRECharList aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTCharDescriptor aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTNodeDescriptor aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTNodeDescriptorExpression aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }

  public Object visit (final ASTPrimaryExpression aNode, final Object aData)
  {
    return defaultVisit (aNode, aData);
  }
}
