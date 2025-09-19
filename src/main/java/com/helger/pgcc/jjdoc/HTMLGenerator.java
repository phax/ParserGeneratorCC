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
import java.util.HashMap;
import java.util.Map;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;
import com.helger.xml.serialize.write.EXMLCharMode;
import com.helger.xml.serialize.write.EXMLIncorrectCharacterHandling;
import com.helger.xml.serialize.write.EXMLSerializeVersion;
import com.helger.xml.serialize.write.XMLMaskHelper;

import jakarta.annotation.Nonnull;

/**
 * Output BNF in HTML 3.2 format.
 */
public class HTMLGenerator extends TextGenerator
{
  private final Map <String, String> m_aIDMap = new HashMap <> ();
  private int m_nID = 1;

  public HTMLGenerator ()
  {}

  protected String getID (final String nt)
  {
    return m_aIDMap.computeIfAbsent (nt, k -> "prod" + m_nID++);
  }

  private void _println (@Nonnull final String s) throws IOException
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
                                 m_aPW);
  }

  @Override
  public void print (final String s) throws IOException
  {
    m_aPW.write (s);
  }

  @Override
  public void documentStart () throws IOException
  {
    m_aPW = createPrintWriter ();
    _println ("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">");
    _println ("<HTML>");
    _println ("<HEAD>");
    if (StringHelper.isNotEmpty (JJDocOptions.getCSS ()))
    {
      _println ("<LINK REL=\"stylesheet\" type=\"text/css\" href=\"" + JJDocOptions.getCSS () + "\"/>");
    }
    if (StringHelper.isNotEmpty (JJDocGlobals.s_input_file))
    {
      _println ("<TITLE>BNF for " + JJDocGlobals.s_input_file + "</TITLE>");
    }
    else
    {
      _println ("<TITLE>A BNF grammar by JJDoc</TITLE>");
    }
    _println ("</HEAD>");
    _println ("<BODY>");
    _println ("<H1 ALIGN=CENTER>BNF for " + JJDocGlobals.s_input_file + "</H1>");
  }

  @Override
  public void documentEnd () throws IOException
  {
    _println ("</BODY>");
    _println ("</HTML>");
    m_aPW.close ();
  }

  /*
   * Prints out comments, used for tokens and non-terminals.
   */

  @Override
  public void specialTokens (final String s) throws IOException
  {
    _println (" <!-- Special token -->");
    _println (" <TR>");
    _println ("  <TD>");
    _println ("    <PRE>");
    print (s);
    _println ("    </PRE>");
    _println ("  </TD>");
    _println (" </TR>");
  }

  @Override
  public void handleTokenProduction (final TokenProduction tp) throws IOException
  {
    _println (" <!-- Token -->");
    _println (" <TR>");
    _println ("  <TD>");
    _println ("   <PRE>");
    text (JJDoc.getStandardTokenProductionText (tp));
    _println ("   </PRE>");
    _println ("  </TD>");
    _println (" </TR>");
  }

  @Override
  public void nonterminalsStart () throws IOException
  {
    _println ("<H2 ALIGN='CENTER'>NON-TERMINALS</H2>");
    if (JJDocOptions.isOneTable ())
    {
      _println ("<TABLE>");
    }
  }

  @Override
  public void nonterminalsEnd () throws IOException
  {
    if (JJDocOptions.isOneTable ())
    {
      _println ("</TABLE>");
    }
  }

  @Override
  public void tokensStart () throws IOException
  {
    _println ("<H2 ALIGN=CENTER>TOKENS</H2>");
    _println ("<TABLE>");
  }

  @Override
  public void tokensEnd () throws IOException
  {
    _println ("</TABLE>");
  }

  @Override
  public void javacode (final CodeProductionJava jp) throws IOException
  {
    productionStart (jp);
    _println ("<I>java code</I></TD></TR>");
    productionEnd (jp);
  }

  @Override
  public void cppcode (final CodeProductionCpp cp) throws IOException
  {
    productionStart (cp);
    _println ("<I>cpp code</I></TD></TR>");
    productionEnd (cp);
  }

  @Override
  public void productionStart (final NormalProduction np) throws IOException
  {
    if (!JJDocOptions.isOneTable ())
    {
      _println ("");
      _println ("<TABLE ALIGN=CENTER>");
      _println ("<CAPTION><STRONG>" + np.getLhs () + "</STRONG></CAPTION>");
    }
    _println ("<TR>");
    _println ("<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME=\"" + getID (np.getLhs ()) + "\">" + np.getLhs () + "</A></TD>");
    _println ("<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>");
    print ("<TD ALIGN=LEFT VALIGN=BASELINE>");
  }

  @Override
  public void productionEnd (final NormalProduction np) throws IOException
  {
    if (!JJDocOptions.isOneTable ())
    {
      _println ("</TABLE>");
      _println ("<HR>");
    }
  }

  @Override
  public void expansionStart (final Expansion e, final boolean first) throws IOException
  {
    if (!first)
    {
      _println ("<TR>");
      _println ("<TD ALIGN=RIGHT VALIGN=BASELINE></TD>");
      _println ("<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>");
      print ("<TD ALIGN=LEFT VALIGN=BASELINE>");
    }
  }

  @Override
  public void expansionEnd (final Expansion e, final boolean first) throws IOException
  {
    _println ("</TD>");
    _println ("</TR>");
  }

  @Override
  public void nonTerminalStart (final ExpNonTerminal nt) throws IOException
  {
    print ("<A HREF=\"#" + getID (nt.getName ()) + "\">");
  }

  @Override
  public void nonTerminalEnd (final ExpNonTerminal nt) throws IOException
  {
    print ("</A>");
  }

  @Override
  public void reStart (final AbstractExpRegularExpression r)
  {}

  @Override
  public void reEnd (final AbstractExpRegularExpression r)
  {}
}
