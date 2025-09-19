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
package com.helger.pgcc.jjdoc;

import java.io.IOException;
import java.io.Serializable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * A report generator for a grammar.
 *
 * @author timp
 * @since 11-Dec-2006
 */
public interface IDocGenerator extends Serializable
{
  /**
   * Output string with entity substitution for brackets and ampersands.
   *
   * @param s
   *        the String to output
   * @throws IOException
   *         on IO error
   */
  void text (String s) throws IOException;

  /**
   * Output String.
   *
   * @param s
   *        String to output
   * @throws IOException
   *         on IO error
   */
  void print (String s) throws IOException;

  /**
   * Output document header.
   * 
   * @throws IOException
   *         on IO error
   */
  void documentStart () throws IOException;

  /**
   * Output document footer.
   * 
   * @throws IOException
   *         on IO error
   */
  void documentEnd () throws IOException;

  /**
   * Output Special Tokens.
   *
   * @param s
   *        tokens to output
   * @throws IOException
   *         on IO error
   */
  void specialTokens (String s) throws IOException;

  void handleTokenProduction (TokenProduction tp) throws IOException;

  // /**
  // * Output start of a TokenProduction.
  // * @param tp the TokenProduction being output
  // */
  // void tokenStart(TokenProduction tp) throws IOException;
  //
  // /**
  // * Output end of a TokenProduction.
  // * @param tp the TokenProduction being output
  // */
  // void tokenEnd(TokenProduction tp) throws IOException;

  /**
   * Output start of non-terminal.
   * 
   * @throws IOException
   *         on IO error
   */
  void nonterminalsStart () throws IOException;

  /**
   * Output end of non-terminal.
   * 
   * @throws IOException
   *         on IO error
   */
  void nonterminalsEnd () throws IOException;

  /**
   * Output start of tokens.
   * 
   * @throws IOException
   *         on IO error
   */
  void tokensStart () throws IOException;

  /**
   * Output end of tokens.
   * 
   * @throws IOException
   *         on IO error
   */
  void tokensEnd () throws IOException;

  /**
   * Output comment from a production.
   *
   * @param jp
   *        the JavaCodeProduction to output
   * @throws IOException
   *         on IO error
   */
  void javacode (CodeProductionJava jp) throws IOException;

  /**
   * Output comment from a production.
   *
   * @param cp
   *        the CppCodeProduction to output
   * @throws IOException
   *         on IO error
   */
  void cppcode (CodeProductionCpp cp) throws IOException;

  /**
   * Output start of a normal production.
   *
   * @param np
   *        the NormalProduction being output
   * @throws IOException
   *         on IO error
   */
  void productionStart (NormalProduction np) throws IOException;

  /**
   * Output end of a normal production.
   *
   * @param np
   *        the NormalProduction being output
   * @throws IOException
   *         on IO error
   */
  void productionEnd (NormalProduction np) throws IOException;

  /**
   * Output start of an Expansion.
   *
   * @param e
   *        Expansion being output
   * @param first
   *        whether this is the first expansion
   * @throws IOException
   *         on IO error
   */
  void expansionStart (Expansion e, boolean first) throws IOException;

  /**
   * Output end of Expansion.
   *
   * @param e
   *        Expansion being output
   * @param first
   *        whether this is the first expansion
   * @throws IOException
   *         on IO error
   */
  void expansionEnd (Expansion e, boolean first) throws IOException;

  /**
   * Output start of non-terminal.
   *
   * @param nt
   *        the NonTerminal being output
   * @throws IOException
   *         on IO error
   */
  void nonTerminalStart (ExpNonTerminal nt) throws IOException;

  /**
   * Output end of non-terminal.
   *
   * @param nt
   *        the NonTerminal being output
   * @throws IOException
   *         on IO error
   */
  void nonTerminalEnd (ExpNonTerminal nt) throws IOException;

  /**
   * Output start of regular expression.
   *
   * @param re
   *        the RegularExpression being output
   * @throws IOException
   *         on IO error
   */
  void reStart (AbstractExpRegularExpression re) throws IOException;

  /**
   * Output end of regular expression.
   *
   * @param re
   *        the RegularExpression being output
   * @throws IOException
   *         on IO error
   */
  void reEnd (AbstractExpRegularExpression re) throws IOException;

  /**
   * Dummy method to ensure parameters are used...
   *
   * @param o
   *        anything
   */
  default void doNothing (final Object o)
  {
    ValueEnforcer.notNull (o, "any");
  }
}
