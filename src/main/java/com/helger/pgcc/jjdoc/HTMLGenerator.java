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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.AbstractNormalProduction;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;
import com.helger.xml.serialize.write.EXMLCharMode;
import com.helger.xml.serialize.write.EXMLIncorrectCharacterHandling;
import com.helger.xml.serialize.write.EXMLSerializeVersion;
import com.helger.xml.serialize.write.XMLMaskHelper;

/**
 * Output BNF as HTML5.
 * <p>
 * This used to emit HTML 3.2, which is from 1997: upper case tags, <code>ALIGN</code> and
 * <code>VALIGN</code> attributes on every cell, and <code>&lt;a name&gt;</code> anchors. The layout
 * is a small default stylesheet now, so the markup says what things are rather than where to put
 * them, and a stylesheet given with the <code>CSS</code> option is linked after it and therefore
 * wins.
 */
public class HTMLGenerator extends TextGenerator
{
  /** Enough layout to replace what the ALIGN attributes used to do, and no more. */
  private static final String DEFAULT_CSS = "body { font-family: sans-serif; margin: 2em; }\n" +
                                            "h1, h2 { text-align: center; }\n" +
                                            "table { border-collapse: collapse; margin: 0 auto; }\n" +
                                            "caption { font-weight: bold; }\n" +
                                            "td { vertical-align: baseline; padding: 0.1em 0.4em; }\n" +
                                            "td.lhs { text-align: right; }\n" +
                                            "td.op { text-align: center; }\n" +
                                            "td.rhs { text-align: left; }\n" +
                                            "pre { margin: 0; }";

  private final Map <String, String> m_aIDMap = new HashMap <> ();
  private int m_nID = 1;

  public HTMLGenerator ()
  {}

  protected String getID (final String sNt)
  {
    return m_aIDMap.computeIfAbsent (sNt, k -> "prod" + m_nID++);
  }

  private void _println (@NonNull final String s) throws IOException
  {
    print (s + "\n");
  }

  @Override
  public void text (final String s) throws IOException
  {
    // Efficient masking
    XMLMaskHelper.maskXMLTextTo (EXMLSerializeVersion.HTML,
                                 EXMLCharMode.TEXT,
                                 EXMLIncorrectCharacterHandling.DO_NOT_WRITE_LOG_WARNING,
                                 s,
                                 getPW ());
  }

  @Override
  public void print (final String s) throws IOException
  {
    getPW ().write (s);
  }

  @Override
  public void documentStart () throws IOException
  {
    setPW (createPrintWriter ());
    _println ("<!DOCTYPE html>");
    _println ("<html lang=\"en\">");
    _println ("<head>");
    _println ("<meta charset=\"" + Options.getOutputEncoding () + "\">");
    if (StringHelper.isNotEmpty (PGCCContext.current ().jjdoc ().getInputFile ()))
    {
      _println ("<title>BNF for " + PGCCContext.current ().jjdoc ().getInputFile () + "</title>");
    }
    else
    {
      _println ("<title>A BNF grammar by JJDoc</title>");
    }
    _println ("<style>");
    _println (DEFAULT_CSS);
    _println ("</style>");
    if (StringHelper.isNotEmpty (JJDocOptions.getCSS ()))
    {
      // After the default, so that it can override it
      _println ("<link rel=\"stylesheet\" href=\"" + JJDocOptions.getCSS () + "\">");
    }
    _println ("</head>");
    _println ("<body>");
    _println ("<h1>BNF for " + PGCCContext.current ().jjdoc ().getInputFile () + "</h1>");
  }

  @Override
  public void documentEnd () throws IOException
  {
    _println ("</body>");
    _println ("</html>");
    getPW ().close ();
  }

  /*
   * Prints out comments, used for tokens and non-terminals.
   */

  @Override
  public void specialTokens (final String s) throws IOException
  {
    _println (" <tr>");
    _println ("  <td class=\"special\">");
    _println ("   <pre>");
    print (s);
    _println ("   </pre>");
    _println ("  </td>");
    _println (" </tr>");
  }

  @Override
  public void handleTokenProduction (final TokenProduction aTp) throws IOException
  {
    final String sText = JJDoc.getStandardTokenProductionText (aTp);
    if (StringHelper.isEmpty (sText.trim ()))
    {
      // A production with nothing to show used to produce an empty row
      return;
    }
    _println (" <tr>");
    _println ("  <td>");
    _println ("   <pre>");
    text (sText);
    _println ("   </pre>");
    _println ("  </td>");
    _println (" </tr>");
  }

  @Override
  public void nonterminalsStart () throws IOException
  {
    _println ("<h2>NON-TERMINALS</h2>");
    if (JJDocOptions.isOneTable ())
    {
      _println ("<table>");
    }
  }

  @Override
  public void nonterminalsEnd () throws IOException
  {
    if (JJDocOptions.isOneTable ())
    {
      _println ("</table>");
    }
  }

  @Override
  public void tokensStart () throws IOException
  {
    _println ("<h2>TOKENS</h2>");
    _println ("<table>");
  }

  @Override
  public void tokensEnd () throws IOException
  {
    _println ("</table>");
  }

  @Override
  public void javacode (final CodeProductionJava aJp) throws IOException
  {
    productionStart (aJp);
    _println ("<em>java code</em></td></tr>");
    productionEnd (aJp);
  }

  @Override
  public void cppcode (final CodeProductionCpp aCp) throws IOException
  {
    productionStart (aCp);
    _println ("<em>cpp code</em></td></tr>");
    productionEnd (aCp);
  }

  @Override
  public void productionStart (@NonNull final AbstractNormalProduction aNp) throws IOException
  {
    if (!JJDocOptions.isOneTable ())
    {
      _println ("");
      _println ("<table>");
      _println ("<caption>" + aNp.getLhs () + "</caption>");
    }
    _println ("<tr>");
    _println ("<td class=\"lhs\" id=\"" + getID (aNp.getLhs ()) + "\">" + aNp.getLhs () + "</td>");
    _println ("<td class=\"op\">::=</td>");
    print ("<td class=\"rhs\">");
  }

  @Override
  public void productionEnd (final AbstractNormalProduction aNp) throws IOException
  {
    if (!JJDocOptions.isOneTable ())
    {
      _println ("</table>");
      _println ("<HR>");
    }
  }

  @Override
  public void expansionStart (final Expansion e, final boolean bFirst) throws IOException
  {
    if (!bFirst)
    {
      _println ("<tr>");
      _println ("<td class=\"lhs\"></td>");
      _println ("<td class=\"op\">|</td>");
      print ("<td class=\"rhs\">");
    }
  }

  @Override
  public void expansionEnd (final Expansion e, final boolean bFirst) throws IOException
  {
    _println ("</td>");
    _println ("</tr>");
  }

  @Override
  public void nonTerminalStart (@NonNull final ExpNonTerminal aNt) throws IOException
  {
    print ("<a href=\"#" + getID (aNt.getName ()) + "\">");
  }

  @Override
  public void nonTerminalEnd (final ExpNonTerminal aNt) throws IOException
  {
    print ("</a>");
  }

  @Override
  public void reStart (final AbstractExpRegularExpression r)
  {}

  @Override
  public void reEnd (final AbstractExpRegularExpression r)
  {}
}
