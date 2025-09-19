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

public interface JJTreeParserVisitor
{
  Object visit (SimpleNode node, Object data);

  Object visit (ASTGrammar node, Object data);

  Object visit (ASTCompilationUnit node, Object data);

  Object visit (ASTProductions node, Object data);

  Object visit (ASTOptions node, Object data);

  Object visit (ASTOptionBinding node, Object data);

  Object visit (ASTJavacode node, Object data);

  Object visit (ASTJavacodeBody node, Object data);

  Object visit (ASTBNF node, Object data);

  Object visit (ASTBNFDeclaration node, Object data);

  Object visit (ASTBNFNodeScope node, Object data);

  Object visit (ASTRE node, Object data);

  Object visit (ASTTokenDecls node, Object data);

  Object visit (ASTRESpec node, Object data);

  Object visit (ASTBNFChoice node, Object data);

  Object visit (ASTBNFSequence node, Object data);

  Object visit (ASTBNFLookahead node, Object data);

  Object visit (ASTExpansionNodeScope node, Object data);

  Object visit (ASTBNFAction node, Object data);

  Object visit (ASTBNFZeroOrOne node, Object data);

  Object visit (ASTBNFTryBlock node, Object data);

  Object visit (ASTBNFNonTerminal node, Object data);

  Object visit (ASTBNFAssignment node, Object data);

  Object visit (ASTBNFOneOrMore node, Object data);

  Object visit (ASTBNFZeroOrMore node, Object data);

  Object visit (ASTBNFParenthesized node, Object data);

  Object visit (ASTREStringLiteral node, Object data);

  Object visit (ASTRENamed node, Object data);

  Object visit (ASTREReference node, Object data);

  Object visit (ASTREEOF node, Object data);

  Object visit (ASTREChoice node, Object data);

  Object visit (ASTRESequence node, Object data);

  Object visit (ASTREOneOrMore node, Object data);

  Object visit (ASTREZeroOrMore node, Object data);

  Object visit (ASTREZeroOrOne node, Object data);

  Object visit (ASTRRepetitionRange node, Object data);

  Object visit (ASTREParenthesized node, Object data);

  Object visit (ASTRECharList node, Object data);

  Object visit (ASTCharDescriptor node, Object data);

  Object visit (ASTNodeDescriptor node, Object data);

  Object visit (ASTNodeDescriptorExpression node, Object data);

  Object visit (ASTPrimaryExpression node, Object data);
}
