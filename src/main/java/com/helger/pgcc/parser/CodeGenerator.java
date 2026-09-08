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
package com.helger.pgcc.parser;

import org.jspecify.annotations.Nullable;

import static com.helger.pgcc.parser.JavaCCGlobals.addUnicodeEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.base.io.nonblocking.NonBlockingBufferedWriter;
import com.helger.base.io.nonblocking.NonBlockingStringWriter;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.utils.OutputFileGenerator;

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

  @NonNull
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

  protected final int getLineNumber ()
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

  public final void genStringLiteralArrayCPP (final String sVarName, @NonNull final String [] aArr)
  {
    // First generate char array vars
    for (int i = 0; i < aArr.length; i++)
    {
      genCodeLine ("static const JJChar " + sVarName + "_arr_" + i + "[] = ");
      genStringLiteralInCPP (aArr[i]);
      genCodeLine (";");
    }

    genCodeLine ("static const JJString " + sVarName + "[] = {");
    for (int i = 0; i < aArr.length; i++)
    {
      genCodeLine (sVarName + "_arr_" + i + ", ");
    }
    genCodeLine ("};");
  }

  public final void genStringLiteralInCPP (@NonNull final String s)
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

  /**
   * Emit the declaration of a static array constant, up to and including the "=", without a
   * newline. The two languages spell this differently - Java puts the brackets on the type and C++
   * on the name - and C++ additionally wants it in the statics file.
   *
   * @param sType
   *        The element type as the target language spells it. May not be <code>null</code>.
   * @param sName
   *        The name of the constant. May not be <code>null</code>.
   */
  public final void genStaticArrayDeclaration (@NonNull final String sType, @NonNull final String sName)
  {
    switch (getOutputLanguage ())
    {
      case JAVA:
        genCode ("static final " + sType + "[] " + sName + " = ");
        break;
      case CPP:
        switchToStaticsFile ();
        genCode ("static const " + sType + " " + sName + "[] = ");
        break;
      default:
        throw new UnsupportedOutputLanguageException (getOutputLanguage ());
    }
  }

  public final void saveOutput (@NonNull final String sFileName)
  {
    if (getOutputLanguage ().hasIncludeFile ())
    {
      final String sIncfilePath = sFileName.replace (".cc", ".h");
      final String sIncfileName = new File (sIncfilePath).getName ();

      final String sDefine = sIncfileName.replace ('.', '_').toUpperCase (Locale.US);
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

      if (grammar ().isJJTreeGenerated ())
      {
        m_aMainBuffer.insert (0, "#include \"SimpleNode.h\"\n");
      }
      if (Options.isTokenManagerUsesParser ())
        m_aMainBuffer.insert (0, "#include \"" + grammar ().getParserName () + ".h\"\n");
      m_aMainBuffer.insert (0, "#include \"TokenMgrError.h\"\n");
      m_aMainBuffer.insert (0, "#include \"" + sIncfileName + "\"\n");
      m_aIncludeBuffer.append ("#endif\n");
      saveOutput (sIncfilePath, m_aIncludeBuffer);
    }

    m_aMainBuffer.insert (0, "/* " + new File (sFileName).getName () + " */\n");
    saveOutput (sFileName, m_aMainBuffer);
  }

  public final void saveOutput (final String sFileName, @NonNull final StringBuilder aSB)
  {
    try (final NonBlockingBufferedWriter aFw = FileHelper.getBufferedWriter (new File (sFileName),
                                                                             Options.getOutputEncoding ()))
    {
      aFw.write (aSB.toString ());
    }
    catch (final IOException aIoe)
    {
      JavaCCErrors.fatal ("Could not create output file: " + sFileName);
    }
  }

  protected final void printTokenSetup (final Token t)
  {
    Token aTt = t;

    while (aTt.specialToken != null)
    {
      aTt = aTt.specialToken;
    }

    m_nLine = aTt.beginLine;
    m_nCol = aTt.beginColumn;
  }

  protected final void printTokenList (final List <Token> aList)
  {
    Token t = null;
    for (final Token aToken : aList)
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

  protected final String getStringForTokenOnly (@NonNull final Token t)
  {
    String sRetval = "";
    for (; m_nLine < t.beginLine; m_nLine++)
    {
      sRetval += "\n";
      m_nCol = 1;
    }
    for (; m_nCol < t.beginColumn; m_nCol++)
    {
      sRetval += " ";
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      sRetval += addUnicodeEscapes (t.image);
    else
      sRetval += t.image;
    m_nLine = t.endLine;
    m_nCol = t.endColumn + 1;
    if (t.image.length () > 0)
    {
      final char cLast = t.image.charAt (t.image.length () - 1);
      if (cLast == '\n' || cLast == '\r')
      {
        m_nLine++;
        m_nCol = 1;
      }
    }

    return sRetval;
  }

  protected final void printToken (@NonNull final Token t)
  {
    genCode (getStringToPrint (t));
  }

  protected final String getStringToPrint (@NonNull final Token t)
  {
    String sRetval = "";
    Token aTt = t.specialToken;
    if (aTt != null)
    {
      while (aTt.specialToken != null)
        aTt = aTt.specialToken;
      while (aTt != null)
      {
        sRetval += getStringForTokenOnly (aTt);
        aTt = aTt.next;
      }
    }

    return sRetval + getStringForTokenOnly (t);
  }

  protected final void printLeadingComments (final Token t)
  {
    genCode (getLeadingComments (t));
  }

  protected final String getLeadingComments (@NonNull final Token t)
  {
    String sRetval = "";
    if (t.specialToken == null)
      return sRetval;
    Token aTt = t.specialToken;
    while (aTt.specialToken != null)
      aTt = aTt.specialToken;
    while (aTt != null)
    {
      sRetval += getStringForTokenOnly (aTt);
      aTt = aTt.next;
    }
    if (m_nCol != 1 && m_nLine != t.beginLine)
    {
      sRetval += "\n";
      m_nLine++;
      m_nCol = 1;
    }

    return sRetval;
  }

  protected final void printTrailingComments (final Token t)
  {
    m_aOutputBuffer.append (getTrailingComments (t));
  }

  protected final String getTrailingComments (@NonNull final Token t)
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
   * @param sAnn
   *        annotation name
   */
  public final void genAnnotation (final String sAnn)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        genCode ("@" + sAnn);
        break;
      case CPP:
        // For now, it's only C++ for now
        genCode ("/*" + sAnn + "*/");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  /**
   * Generate a modifier
   *
   * @param sMod
   *        modifier
   */
  public final void genModifier (@NonNull final String sMod)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        genCode (sMod);
        break;
      case CPP:
        // For now, it's only C++ for now
        final String sOrigMod = sMod.trim ().toLowerCase (Locale.US);
        if (sOrigMod.equals ("public") || sOrigMod.equals ("protected") || sOrigMod.equals ("private"))
          genCode (sOrigMod + ": ");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  /**
   * Generate a class with a given name, an array of superclass and another array of super
   * interfaces
   *
   * @param sMod
   *        modifier
   * @param sName
   *        name
   * @param aSuperClasses
   *        super classes
   * @param aSuperInterfaces
   *        super interfaces
   */
  public final void genClassStart (@Nullable final String sMod,
                                   final String sName,
                                   @NonNull final String [] aSuperClasses,
                                   @NonNull final String [] aSuperInterfaces)
  {
    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        if (sMod != null)
          genModifier (sMod);
        genCode ("class " + sName);
        if (aSuperClasses.length == 1 && aSuperClasses[0] != null)
          genCode (" extends " + aSuperClasses[0]);
        if (aSuperInterfaces.length != 0)
          genCode (" implements ");
        _genCommaSeperatedString (aSuperInterfaces);
        genCodeLine (" {");
        break;
      case CPP:
        genCode ("class " + sName);
        if (aSuperClasses.length > 0 || aSuperInterfaces.length > 0)
          genCode (" : ");
        _genCommaSeperatedString (aSuperClasses);
        _genCommaSeperatedString (aSuperInterfaces);
        genCodeLine (" {");
        genCodeLine ("public:");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  private void _genCommaSeperatedString (@NonNull final String [] aStrings)
  {
    for (int i = 0; i < aStrings.length; i++)
    {
      if (i > 0)
        genCode (", ");
      genCode (aStrings[i]);
    }
  }

  public final void generateMethodDefHeader (final String sModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams)
  {
    generateMethodDefHeader (sModsAndRetType, sClassName, sNameAndParams, null);
  }

  public final void generateMethodDefHeader (@NonNull final String sQualifiedModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams,
                                             @Nullable final String sExceptions)
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

        String sModsAndRetType = null;
        int i = sQualifiedModsAndRetType.lastIndexOf (':');
        if (i >= 0)
          sModsAndRetType = sQualifiedModsAndRetType.substring (i + 1);

        if (sModsAndRetType != null)
        {
          i = sModsAndRetType.lastIndexOf ("virtual");
          if (i >= 0)
            sModsAndRetType = sModsAndRetType.substring (i + "virtual".length ());
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

  protected final String getClassQualifier (@Nullable final String sClassName)
  {
    return sClassName == null ? "" : sClassName + "::";
  }

  public static String getCharStreamName ()
  {
    if (Options.isJavaUserCharStream ())
    {
      // User interface name
      return "CharStream";
    }
    if (Options.isJavaUnicodeEscape ())
      return "JavaCharStream";
    if (Options.isCharSequenceCharStream ())
      return "CharSequenceCharStream";
    return "SimpleCharStream";
  }

  public void writeTemplate (final String sName, final Map <String, Object> aOptions) throws IOException
  {
    final OutputFileGenerator aGen = new OutputFileGenerator (sName, aOptions);
    try (final NonBlockingStringWriter aSw = new NonBlockingStringWriter ())
    {
      aGen.generate (aSw);
      genCode (aSw.getAsString ());
    }
  }
}
