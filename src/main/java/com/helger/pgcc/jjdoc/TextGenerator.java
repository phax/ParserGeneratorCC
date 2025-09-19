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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;

import jakarta.annotation.Nonnull;

/**
 * Output BNF in text format.
 */
public class TextGenerator implements IDocGenerator
{
  protected Writer m_aPW;

  public TextGenerator ()
  {}

  public void text (final String s) throws IOException
  {
    print (s);
  }

  public void print (final String s) throws IOException
  {
    m_aPW.write (s);
  }

  public void documentStart () throws IOException
  {
    m_aPW = createPrintWriter ();
    m_aPW.write ("\nDOCUMENT START\n");
  }

  public void documentEnd () throws IOException
  {
    m_aPW.write ("\nDOCUMENT END\n");
    m_aPW.close ();
  }

  public void specialTokens (final String s) throws IOException
  {
    m_aPW.write (s);
  }

  public void nonterminalsStart () throws IOException
  {
    text ("NON-TERMINALS\n");
  }

  public void nonterminalsEnd () throws IOException
  {}

  public void tokensStart () throws IOException
  {
    text ("TOKENS\n");
  }

  public void handleTokenProduction (final TokenProduction tp) throws IOException
  {
    final String text = JJDoc.getStandardTokenProductionText (tp);
    text (text);
  }

  public void tokensEnd () throws IOException
  {}

  public void javacode (final CodeProductionJava jp) throws IOException
  {
    productionStart (jp);
    text ("java code");
    productionEnd (jp);
  }

  public void cppcode (final CodeProductionCpp cp) throws IOException
  {
    productionStart (cp);
    text ("c++ code");
    productionEnd (cp);
  }

  public void productionStart (final NormalProduction np) throws IOException
  {
    m_aPW.write ("\t" + np.getLhs () + "\t:=\t");
  }

  public void productionEnd (final NormalProduction np) throws IOException
  {
    m_aPW.write ("\n");
  }

  public void expansionStart (final Expansion e, final boolean first) throws IOException
  {
    if (!first)
      m_aPW.write ("\n\t\t|\t");
  }

  public void expansionEnd (final Expansion e, final boolean first) throws IOException
  {}

  public void nonTerminalStart (final ExpNonTerminal nt) throws IOException
  {}

  public void nonTerminalEnd (final ExpNonTerminal nt) throws IOException
  {}

  public void reStart (final AbstractExpRegularExpression r) throws IOException
  {}

  public void reEnd (final AbstractExpRegularExpression r) throws IOException
  {}

  /**
   * Create an output stream for the generated Jack code. Try to open a file
   * based on the name of the parser, but if that fails use the standard output
   * stream.
   *
   * @return Never <code>null</code>.
   */
  @Nonnull
  protected static Writer createPrintWriter ()
  {
    String ext = ".html";
    if (JJDocOptions.isText ())
      ext = ".txt";
    else
      if (JJDocOptions.isXText ())
        ext = ".xtext";

    return createPrintWriter (ext);
  }

  /**
   * Create an output stream for the generated Jack code. Try to open a file
   * based on the name of the parser, but if that fails use the standard output
   * stream.
   *
   * @return Never <code>null</code>.
   */
  @Nonnull
  protected static Writer createPrintWriter (@Nonnull final String ext)
  {
    if (StringHelper.isEmpty (JJDocOptions.getOutputFile ()))
    {
      if (JJDocGlobals.s_input_file.equals (JJDocGlobals.STANDARD_INPUT))
        return PGPrinter.getOutWriter ();

      final int i = JJDocGlobals.s_input_file.lastIndexOf ('.');
      if (i == -1)
      {
        JJDocGlobals.s_output_file = JJDocGlobals.s_input_file + ext;
      }
      else
      {
        final String suffix = JJDocGlobals.s_input_file.substring (i);
        if (suffix.equals (ext))
        {
          JJDocGlobals.s_output_file = JJDocGlobals.s_input_file + ext;
        }
        else
        {
          JJDocGlobals.s_output_file = JJDocGlobals.s_input_file.substring (0, i) + ext;
        }
      }
    }
    else
    {
      JJDocGlobals.s_output_file = JJDocOptions.getOutputFile ();
    }

    final Writer aWriter = FileHelper.getBufferedWriter (new File (JJDocGlobals.s_output_file), Options.getOutputEncoding ());
    if (aWriter != null)
      return new PrintWriter (aWriter);
    PGPrinter.error ("JJDoc: can't open output stream on file " + JJDocGlobals.s_output_file + ".  Using standard output.");
    return PGPrinter.getOutWriter ();
  }
}
