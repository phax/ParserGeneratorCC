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
// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package com.helger.pgcc.parser;

import static com.helger.pgcc.parser.JavaCCGlobals.addUnicodeEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jjtreeGenerated;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.helger.base.io.nonblocking.NonBlockingBufferedWriter;
import com.helger.base.io.nonblocking.NonBlockingStringWriter;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.utils.OutputFileGenerator;

import jakarta.annotation.Nonnull;

public class CodeGenerator
{
  private final StringBuilder m_aMainBuffer = new StringBuilder ();
  private final StringBuilder m_aIncludeBuffer = new StringBuilder ();
  private final StringBuilder m_aStaticsBuffer = new StringBuilder ();
  private StringBuilder m_aOutputBuffer = m_aMainBuffer;

  private int m_nLine;
  private int m_nCol;

  public CodeGenerator ()
  {}

  @Nonnull
  public final EOutputLanguage getOutputLanguage ()
  {
    return Options.getOutputLanguage ();
  }

  public final void switchToMainFile ()
  {
    m_aOutputBuffer = m_aMainBuffer;
  }

  public final void switchToStaticsFile ()
  {
    if (getOutputLanguage ().hasStaticsFile ())
    {
      m_aOutputBuffer = m_aStaticsBuffer;
    }
  }

  public final void switchToIncludeFile ()
  {
    if (getOutputLanguage ().hasIncludeFile ())
    {
      m_aOutputBuffer = m_aIncludeBuffer;
    }
  }

  protected final int getCol ()
  {
    return m_nCol;
  }

  protected final int getLine ()
  {
    return m_nLine;
  }

  protected final void setColToStart ()
  {
    m_nCol = 1;
  }

  protected final void setLineAndCol (final int nLine, final int nCol)
  {
    m_nLine = nLine;
    m_nCol = nCol;
  }

  public final void genStringLiteralArrayCPP (final String varName, final String [] arr)
  {
    // First generate char array vars
    for (int i = 0; i < arr.length; i++)
    {
      genCodeLine ("static const JJChar " + varName + "_arr_" + i + "[] = ");
      genStringLiteralInCPP (arr[i]);
      genCodeLine (";");
    }

    genCodeLine ("static const JJString " + varName + "[] = {");
    for (int i = 0; i < arr.length; i++)
    {
      genCodeLine (varName + "_arr_" + i + ", ");
    }
    genCodeLine ("};");
  }

  public final void genStringLiteralInCPP (final String s)
  {
    // String literals in CPP become char arrays
    m_aOutputBuffer.append ("{");
    for (final char c : s.toCharArray ())
    {
      m_aOutputBuffer.append ("0x").append (Integer.toHexString (c)).append (", ");
    }
    m_aOutputBuffer.append ("0}");
  }

  public final void genCode (final char c)
  {
    m_aOutputBuffer.append (c);
  }

  public final void genCode (final String s)
  {
    m_aOutputBuffer.append (s);
  }

  public final void genCodeNewLine ()
  {
    genCode ("\n");
  }

  public final void genCodeLine (final String s)
  {
    genCode (s);
    genCodeNewLine ();
  }

  public final void saveOutput (final String fileName)
  {
    if (getOutputLanguage ().hasIncludeFile ())
    {
      final String incfilePath = fileName.replace (".cc", ".h");
      final String incfileName = new File (incfilePath).getName ();

      final String sDefine = incfileName.replace ('.', '_').toUpperCase (Locale.US);
      m_aIncludeBuffer.insert (0, "#define " + sDefine + "\n");
      m_aIncludeBuffer.insert (0, "#ifndef " + sDefine + "\n");

      // dump the statics into the main file with the code.
      m_aMainBuffer.insert (0, m_aStaticsBuffer);

      // Finally enclose the whole thing in the namespace, if specified.
      if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
      {
        m_aMainBuffer.insert (0, "namespace " + Options.stringValue ("NAMESPACE_OPEN") + "\n");
        m_aMainBuffer.append (Options.stringValue ("NAMESPACE_CLOSE") + "\n");
        m_aIncludeBuffer.append (Options.stringValue ("NAMESPACE_CLOSE") + "\n");
      }

      if (s_jjtreeGenerated)
      {
        m_aMainBuffer.insert (0, "#include \"SimpleNode.h\"\n");
      }
      if (Options.isTokenManagerUsesParser ())
        m_aMainBuffer.insert (0, "#include \"" + s_cu_name + ".h\"\n");
      m_aMainBuffer.insert (0, "#include \"TokenMgrError.h\"\n");
      m_aMainBuffer.insert (0, "#include \"" + incfileName + "\"\n");
      m_aIncludeBuffer.append ("#endif\n");
      saveOutput (incfilePath, m_aIncludeBuffer);
    }

    m_aMainBuffer.insert (0, "/* " + new File (fileName).getName () + " */\n");
    saveOutput (fileName, m_aMainBuffer);
  }

  public final void saveOutput (final String fileName, final StringBuilder sb)
  {
    try (final NonBlockingBufferedWriter fw = FileHelper.getBufferedWriter (new File (fileName), Options.getOutputEncoding ()))
    {
      fw.write (sb.toString ());
    }
    catch (final IOException ioe)
    {
      JavaCCErrors.fatal ("Could not create output file: " + fileName);
    }
  }

  protected final void printTokenSetup (final Token t)
  {
    Token tt = t;

    while (tt.specialToken != null)
    {
      tt = tt.specialToken;
    }

    m_nLine = tt.beginLine;
    m_nCol = tt.beginColumn;
  }

  protected final void printTokenList (final List <Token> list)
  {
    Token t = null;
    for (final Token aToken : list)
    {
      t = aToken;
      printToken (t);
    }

    if (t != null)
      printTrailingComments (t);
  }

  protected final void printTokenOnly (final Token t)
  {
    genCode (getStringForTokenOnly (t));
  }

  protected final String getStringForTokenOnly (final Token t)
  {
    String retval = "";
    for (; m_nLine < t.beginLine; m_nLine++)
    {
      retval += "\n";
      m_nCol = 1;
    }
    for (; m_nCol < t.beginColumn; m_nCol++)
    {
      retval += " ";
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      retval += addUnicodeEscapes (t.image);
    else
      retval += t.image;
    m_nLine = t.endLine;
    m_nCol = t.endColumn + 1;
    if (t.image.length () > 0)
    {
      final char last = t.image.charAt (t.image.length () - 1);
      if (last == '\n' || last == '\r')
      {
        m_nLine++;
        m_nCol = 1;
      }
    }

    return retval;
  }

  protected final void printToken (@Nonnull final Token t)
  {
    genCode (getStringToPrint (t));
  }

  protected final String getStringToPrint (@Nonnull final Token t)
  {
    String retval = "";
    Token tt = t.specialToken;
    if (tt != null)
    {
      while (tt.specialToken != null)
        tt = tt.specialToken;
      while (tt != null)
      {
        retval += getStringForTokenOnly (tt);
        tt = tt.next;
      }
    }

    return retval + getStringForTokenOnly (t);
  }

  protected final void printLeadingComments (final Token t)
  {
    genCode (getLeadingComments (t));
  }

  protected final String getLeadingComments (final Token t)
  {
    String retval = "";
    if (t.specialToken == null)
      return retval;
    Token tt = t.specialToken;
    while (tt.specialToken != null)
      tt = tt.specialToken;
    while (tt != null)
    {
      retval += getStringForTokenOnly (tt);
      tt = tt.next;
    }
    if (m_nCol != 1 && m_nLine != t.beginLine)
    {
      retval += "\n";
      m_nLine++;
      m_nCol = 1;
    }

    return retval;
  }

  protected final void printTrailingComments (final Token t)
  {
    m_aOutputBuffer.append (getTrailingComments (t));
  }

  protected final String getTrailingComments (final Token t)
  {
    if (t.next == null)
      return "";
    return getLeadingComments (t.next);
  }

  /**
   * for testing
   *
   * @return the generated code + newline
   */
  public final String getGeneratedCode ()
  {
    return m_aOutputBuffer.toString () + "\n";
  }

  /**
   * Generate annotation. @XX syntax for java, comments in C++
   *
   * @param ann
   *        annotation name
   */
  public final void genAnnotation (final String ann)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        genCode ("@" + ann);
        break;
      case CPP:
        // For now, it's only C++ for now
        genCode ("/*" + ann + "*/");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  /**
   * Generate a modifier
   *
   * @param mod
   *        modifier
   */
  public final void genModifier (final String mod)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        genCode (mod);
        break;
      case CPP:
        // For now, it's only C++ for now
        final String origMod = mod.trim ().toLowerCase (Locale.US);
        if (origMod.equals ("public") || origMod.equals ("protected") || origMod.equals ("private"))
          genCode (origMod + ": ");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  /**
   * Generate a class with a given name, an array of superclass and another
   * array of super interfaces
   *
   * @param mod
   *        modifier
   * @param name
   *        name
   * @param superClasses
   *        super classes
   * @param superInterfaces
   *        super interfaces
   */
  public final void genClassStart (final String mod, final String name, final String [] superClasses, final String [] superInterfaces)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        if (mod != null)
          genModifier (mod);
        genCode ("class " + name);
        if (superClasses.length == 1 && superClasses[0] != null)
          genCode (" extends " + superClasses[0]);
        if (superInterfaces.length != 0)
          genCode (" implements ");
        _genCommaSeperatedString (superInterfaces);
        genCodeLine (" {");
        break;
      case CPP:
        genCode ("class " + name);
        if (superClasses.length > 0 || superInterfaces.length > 0)
          genCode (" : ");
        _genCommaSeperatedString (superClasses);
        _genCommaSeperatedString (superInterfaces);
        genCodeLine (" {");
        genCodeLine ("public:");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  private void _genCommaSeperatedString (final String [] strings)
  {
    for (int i = 0; i < strings.length; i++)
    {
      if (i > 0)
        genCode (", ");
      genCode (strings[i]);
    }
  }

  public final void generateMethodDefHeader (final String modsAndRetType, final String className, final String nameAndParams)
  {
    generateMethodDefHeader (modsAndRetType, className, nameAndParams, null);
  }

  public final void generateMethodDefHeader (final String sQualifiedModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams,
                                             final String sExceptions)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        genCode (sQualifiedModsAndRetType + " " + sNameAndParams);
        if (sExceptions != null)
        {
          genCode (" throws " + sExceptions);
        }
        genCodeNewLine ();
        break;
      case CPP:
        // for C++, we generate the signature in the header file and body in
        // main file
        m_aIncludeBuffer.append (sQualifiedModsAndRetType + " " + sNameAndParams);
        // if (exceptions != null)
        // includeBuffer.append(" throw(" + exceptions + ")");
        m_aIncludeBuffer.append (";\n");

        String modsAndRetType = null;
        int i = sQualifiedModsAndRetType.lastIndexOf (':');
        if (i >= 0)
          modsAndRetType = sQualifiedModsAndRetType.substring (i + 1);

        if (modsAndRetType != null)
        {
          i = modsAndRetType.lastIndexOf ("virtual");
          if (i >= 0)
            modsAndRetType = modsAndRetType.substring (i + "virtual".length ());
        }

        String sNonVirtual = sQualifiedModsAndRetType;
        i = sNonVirtual.lastIndexOf ("virtual");
        if (i >= 0)
          sNonVirtual = sNonVirtual.substring (i + "virtual".length ());
        m_aMainBuffer.append ("\n" + sNonVirtual + " " + getClassQualifier (sClassName) + sNameAndParams);
        // if (exceptions != null)
        // mainBuffer.append(" throw( " + exceptions + ")");
        switchToMainFile ();
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  protected final String getClassQualifier (final String className)
  {
    return className == null ? "" : className + "::";
  }

  public static String getCharStreamName ()
  {
    if (Options.isJavaUserCharStream ())
    {
      // User interface name
      return "CharStream";
    }
    return Options.isJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream";
  }

  public void writeTemplate (final String name, final Map <String, Object> options) throws IOException
  {
    final OutputFileGenerator gen = new OutputFileGenerator (name, options);
    try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
    {
      gen.generate (sw);
      genCode (sw.getAsString ());
    }
  }
}
