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
package com.helger.pgcc.jjdoc;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.AbstractNormalProduction;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;

public class BNFGenerator implements IDocGenerator
{
  private final Map <String, String> m_aIDMap = new HashMap <> ();
  private int m_nID = 1;
  private Writer m_aPW;
  private boolean m_bPrinting = true;

  protected String getID (final String sNt)
  {
    return m_aIDMap.computeIfAbsent (sNt, k -> "prod" + m_nID++);
  }

  protected static Writer createOutputStream ()
  {
    return TextGenerator.createPrintWriter (".bnf");
  }

  public void text (@NonNull final String s) throws IOException
  {
    if (m_bPrinting && !(s.length () == 1 && (s.charAt (0) == '\n' || s.charAt (0) == '\r')))
    {
      print (s);
    }
  }

  public void print (final String s) throws IOException
  {
    m_aPW.write (s);
  }

  public void documentStart ()
  {
    m_aPW = createOutputStream ();
  }

  public void documentEnd () throws IOException
  {
    m_aPW.close ();
  }

  public void specialTokens (final String s)
  {}

  public void nonterminalsStart ()
  {}

  public void nonterminalsEnd ()
  {}

  @Override
  public void tokensStart ()
  {}

  @Override
  public void tokensEnd ()
  {}

  public void javacode (final CodeProductionJava aJp)
  {}

  public void cppcode (final CodeProductionCpp aCp)
  {}

  public void expansionEnd (final Expansion e, final boolean bFirst)
  {}

  public void nonTerminalStart (final ExpNonTerminal aNt)
  {}

  public void nonTerminalEnd (final ExpNonTerminal aNt)
  {}

  public void productionStart (@NonNull final AbstractNormalProduction aNp) throws IOException
  {
    print ("\n");
    print (aNp.getLhs () + " ::= ");
  }

  public void productionEnd (final AbstractNormalProduction aNp) throws IOException
  {
    print ("\n");
  }

  public void expansionStart (final Expansion e, final boolean bFirst) throws IOException
  {
    if (!bFirst)
    {
      print (" | ");
    }
  }

  public void reStart (final AbstractExpRegularExpression r)
  {
    // Nothing to do. Upstream switched printing off here for ExpRJustName and ExpRCharacterList,
    // which are exactly the two shapes a terminal takes inside a BNF production - a reference to a
    // named token and an inline character class. The result was a BNF with no terminals in it at
    // all: "sum ::= ( )* <EOF>" for a production reading "<NUMBER> ( <PLUS> <NUMBER> )* <EOF>".
    // Token productions are suppressed by handleTokenProduction, which is a separate path, so
    // nothing here needs to switch printing off.
  }

  public void reEnd (final AbstractExpRegularExpression r)
  {
    // Nothing to do
  }

  @Override
  public void handleTokenProduction (final TokenProduction aTp) throws IOException
  {
    m_bPrinting = false;
    final String sText = JJDoc.getStandardTokenProductionText (aTp);
    text (sText);
    m_bPrinting = true;
  }
}
