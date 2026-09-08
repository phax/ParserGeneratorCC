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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.AbstractNormalProduction;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * Output BNF in text format.
 */
public class TextGenerator implements IDocGenerator
{
  private Writer m_aPW;

  /**
   * The p w.
   *
   * @return The value of m_aPW.
   */
  public Writer getPW ()
  {
    return m_aPW;
  }

  /**
   * The p w.
   *
   * @param aValue
   *        The new value of m_aPW.
   */
  public void setPW (final Writer aValue)
  {
    m_aPW = aValue;
  }

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

  public void handleTokenProduction (final TokenProduction aTp) throws IOException
  {
    final String sText = JJDoc.getStandardTokenProductionText (aTp);
    text (sText);
  }

  public void tokensEnd () throws IOException
  {}

  public void javacode (final CodeProductionJava aJp) throws IOException
  {
    productionStart (aJp);
    text ("java code");
    productionEnd (aJp);
  }

  public void cppcode (final CodeProductionCpp aCp) throws IOException
  {
    productionStart (aCp);
    text ("c++ code");
    productionEnd (aCp);
  }

  public void productionStart (@NonNull final AbstractNormalProduction aNp) throws IOException
  {
    m_aPW.write ("\t" + aNp.getLhs () + "\t:=\t");
  }

  public void productionEnd (final AbstractNormalProduction aNp) throws IOException
  {
    m_aPW.write ("\n");
  }

  public void expansionStart (final Expansion e, final boolean bFirst) throws IOException
  {
    if (!bFirst)
      m_aPW.write ("\n\t\t|\t");
  }

  public void expansionEnd (final Expansion e, final boolean bFirst) throws IOException
  {}

  public void nonTerminalStart (final ExpNonTerminal aNt) throws IOException
  {}

  public void nonTerminalEnd (final ExpNonTerminal aNt) throws IOException
  {}

  public void reStart (final AbstractExpRegularExpression r) throws IOException
  {}

  public void reEnd (final AbstractExpRegularExpression r) throws IOException
  {}

  /**
   * Create an output stream for the generated Jack code. Try to open a file based on the name of
   * the parser, but if that fails use the standard output stream.
   *
   * @return Never <code>null</code>.
   */
  @NonNull
  protected static Writer createPrintWriter ()
  {
    String sExt = ".html";
    if (JJDocOptions.isText ())
      sExt = ".txt";
    else
      if (JJDocOptions.isXText ())
        sExt = ".xtext";

    return createPrintWriter (sExt);
  }

  /**
   * Create an output stream for the generated Jack code. Try to open a file based on the name of
   * the parser, but if that fails use the standard output stream.
   *
   * @param sExt
   *        The file extension to use, dot included. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  protected static Writer createPrintWriter (@NonNull final String sExt)
  {
    if (StringHelper.isEmpty (JJDocOptions.getOutputFile ()))
    {
      if (PGCCContext.current ().jjdoc ().getInputFile ().equals (JJDocGlobals.STANDARD_INPUT))
        return PGPrinter.getOutWriter ();

      final int i = PGCCContext.current ().jjdoc ().getInputFile ().lastIndexOf ('.');
      if (i == -1)
      {
        PGCCContext.current ().jjdoc ().setOutputFile (PGCCContext.current ().jjdoc ().getInputFile () + sExt);
      }
      else
      {
        final String sSuffix = PGCCContext.current ().jjdoc ().getInputFile ().substring (i);
        if (sSuffix.equals (sExt))
        {
          PGCCContext.current ().jjdoc ().setOutputFile (PGCCContext.current ().jjdoc ().getInputFile () + sExt);
        }
        else
        {
          PGCCContext.current ()
                     .jjdoc ()
                     .setOutputFile (PGCCContext.current ().jjdoc ().getInputFile ().substring (0, i) + sExt);
        }
      }
    }
    else
    {
      PGCCContext.current ().jjdoc ().setOutputFile (JJDocOptions.getOutputFile ());
    }

    final Writer aWriter = FileHelper.getBufferedWriter (new File (PGCCContext.current ().jjdoc ().getOutputFile ()),
                                                         Options.getOutputEncoding ());
    if (aWriter != null)
      return new PrintWriter (aWriter);
    PGPrinter.error ("JJDoc: can't open output stream on file " +
                     PGCCContext.current ().jjdoc ().getOutputFile () +
                     ".  Using standard output.");
    return PGPrinter.getOutWriter ();
  }
}
