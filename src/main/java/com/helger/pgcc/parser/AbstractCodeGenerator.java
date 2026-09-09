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

import com.helger.base.string.StringHelper;
import com.helger.base.io.nonblocking.NonBlockingBufferedWriter;
import com.helger.base.io.nonblocking.NonBlockingStringWriter;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.context.LexerState;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.utils.OutputFileGenerator;

/**
 * The buffers and the emitting that the four generators share.
 * <p>
 * Output is collected in memory rather than written as it is produced, because C++ needs it split
 * three ways - the main file, the include file and a statics file - and which of them is current
 * changes as generation proceeds. {@link #saveOutput(String)} writes what is collected.
 * <p>
 * It also carries the position tracking used when grammar text is copied through verbatim, so that
 * the action code lands in the generated file with its original line and column.
 */
public abstract class AbstractCodeGenerator
{
  private final StringBuilder m_aMainBuffer = new StringBuilder ();
  private final StringBuilder m_aIncludeBuffer = new StringBuilder ();
  private final StringBuilder m_aStaticsBuffer = new StringBuilder ();
  private StringBuilder m_aOutputBuffer = m_aMainBuffer;

  private int m_nLine;
  private int m_nCol;

  /** Default constructor for the generators to extend. */
  protected AbstractCodeGenerator ()
  {}

  /**
   * {@return the language being generated. Never <code>null</code>}
   */
  public final EOutputLanguage getOutputLanguage ()
  {
    return Options.getOutputLanguage ();
  }

  /**
   * Send everything written from now on to the main output file. Java only ever has this one.
   */
  public final void switchToMainFile ()
  {
    m_aOutputBuffer = m_aMainBuffer;
  }

  /**
   * Send everything written from now on to the file that holds the constants. C++ keeps them
   * separate; in Java this does nothing.
   */
  public final void switchToStaticsFile ()
  {
    if (getOutputLanguage ().hasStaticsFile ())
    {
      m_aOutputBuffer = m_aStaticsBuffer;
    }
  }

  /**
   * Send everything written from now on to the header file. C++ only; in Java this does nothing.
   */
  public final void switchToIncludeFile ()
  {
    if (getOutputLanguage ().hasIncludeFile ())
    {
      m_aOutputBuffer = m_aIncludeBuffer;
    }
  }

  /**
   * {@return the column the next copied character would go to. 1-based}
   */
  protected final int getCol ()
  {
    return m_nCol;
  }

  /**
   * {@return the line the next copied character would go to. 1-based}
   */
  protected final int getLineNumber ()
  {
    return m_nLine;
  }

  /**
   * Put the copying position back at the first column, after a line break was emitted.
   */
  protected final void setColToStart ()
  {
    m_nCol = 1;
  }

  /**
   * Move the copying position, so that the next stretch of grammar text reproduces its original
   * layout.
   *
   * @param nLine
   *        The line to continue at, 1 based.
   * @param nCol
   *        The column to continue at, 1 based.
   */
  protected final void setLineAndCol (final int nLine, final int nCol)
  {
    m_nLine = nLine;
    m_nCol = nCol;
  }

  /**
   * Append one character to the current output buffer.
   *
   * @param c
   *        The character to emit.
   */
  public final void genCode (final char c)
  {
    m_aOutputBuffer.append (c);
  }

  /**
   * Append text to the current output buffer.
   *
   * @param s
   *        The text to emit. May not be <code>null</code>.
   */
  public final void genCode (final String s)
  {
    m_aOutputBuffer.append (s);
  }

  /** Append a line break to the current output buffer. */
  public final void genCodeNewLine ()
  {
    genCode ("\n");
  }

  /**
   * Append text and a line break to the current output buffer.
   *
   * @param s
   *        The text to emit. May not be <code>null</code>.
   */
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
    // C++ keeps its constants in a file of their own
    if (getOutputLanguage ().hasStaticsFile ())
      switchToStaticsFile ();
    genCode (getOutputLanguage ().getStaticArrayDeclaration (sType, sName));
  }

  /**
   * Write everything generated so far. For C++ this also assembles the header file, wraps both in
   * the namespace and prepends the includes.
   *
   * @param sFileName
   *        The main output file. May not be <code>null</code>.
   */
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

  /**
   * Write one buffer to one file, reporting a fatal error rather than throwing if it cannot.
   *
   * @param sFileName
   *        The file to write. May not be <code>null</code>.
   * @param aSB
   *        What to write. May not be <code>null</code>.
   */
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

  /**
   * Point the copying position at a token, so that the text copied after it reproduces the blank
   * lines and indentation of the grammar.
   *
   * @param t
   *        The token to start copying at. May not be <code>null</code>.
   */
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

  /**
   * Copy a run of grammar tokens into the output, ending with the comments that trail the last one.
   *
   * @param aList
   *        The tokens to copy. May not be <code>null</code>.
   */
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

  /**
   * Copy one grammar token and the whitespace in front of it, without the comments attached to it.
   *
   * @param t
   *        The token to copy. May not be <code>null</code>.
   */
  protected final void printTokenOnly (final Token t)
  {
    genCode (getStringForTokenOnly (t));
  }

  /**
   * Render one token, padded with the newlines and spaces needed to put it at its original line and
   * column.
   *
   * @param t
   *        The token to render. May not be <code>null</code>.
   * @return The text to emit. Never <code>null</code>.
   */
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

  /**
   * Copy one grammar token together with the comments attached to it.
   *
   * @param t
   *        The token to copy. May not be <code>null</code>.
   */
  protected final void printToken (@NonNull final Token t)
  {
    genCode (getStringToPrint (t));
  }

  /**
   * Render one token together with the comments in front of it.
   *
   * @param t
   *        The token to render. May not be <code>null</code>.
   * @return The text to emit. Never <code>null</code>.
   */
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

  /**
   * Copy only the comments attached in front of a grammar token.
   *
   * @param t
   *        The token whose comments to copy. May not be <code>null</code>.
   */
  protected final void printLeadingComments (final Token t)
  {
    genCode (getLeadingComments (t));
  }

  /**
   * Render the special tokens attached in front of a token, which is where comments live.
   *
   * @param t
   *        The token whose leading comments to render. May not be <code>null</code>.
   * @return The text to emit, empty if there are none. Never <code>null</code>.
   */
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

  /**
   * Copy the comments that follow a grammar token on the same line.
   *
   * @param t
   *        The token whose comments to copy. May not be <code>null</code>.
   */
  protected final void printTrailingComments (final Token t)
  {
    m_aOutputBuffer.append (getTrailingComments (t));
  }

  /**
   * The same as {@link #printTrailingComments(Token)} but hands the text back instead of emitting
   * it.
   *
   * @param t
   *        The token whose comments to collect. May not be <code>null</code>.
   * @return The comments after the token. Never <code>null</code>.
   */
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
    genCode (getOutputLanguage ().getAnnotation (sAnn));
  }

  /**
   * Generate a modifier
   *
   * @param sMod
   *        modifier
   */
  public final void genModifier (@NonNull final String sMod)
  {
    genCode (getOutputLanguage ().getModifier (sMod));
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
    genCode (getOutputLanguage ().getClassStart (sMod, sName, aSuperClasses, aSuperInterfaces));
  }

  /**
   * Write the header of a method that throws nothing.
   *
   * @param sModsAndRetType
   *        The modifiers and the return type, as this language spells them. May not be
   *        <code>null</code>.
   * @param sClassName
   *        The class the method belongs to. Only C++ needs it, to qualify the out of line
   *        definition. May be <code>null</code>.
   * @param sNameAndParams
   *        The method name and its parameter list. May not be <code>null</code>.
   */
  public final void generateMethodDefHeader (final String sModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams)
  {
    generateMethodDefHeader (sModsAndRetType, sClassName, sNameAndParams, null);
  }

  /**
   * Write the header of a method, ending the line afterwards.
   *
   * @param sQualifiedModsAndRetType
   *        The modifiers and the return type, as this language spells them. May not be
   *        <code>null</code>.
   * @param sClassName
   *        The class the method belongs to. Only C++ needs it, to qualify the out of line
   *        definition. May be <code>null</code>.
   * @param sNameAndParams
   *        The method name and its parameter list. May not be <code>null</code>.
   * @param sExceptions
   *        The checked exceptions. May be <code>null</code>.
   */
  public final void generateMethodDefHeader (@NonNull final String sQualifiedModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams,
                                             @Nullable final String sExceptions)
  {
    generateMethodDefHeader (sQualifiedModsAndRetType, sClassName, sNameAndParams, sExceptions, true);
  }

  /**
   * Write the header of a method, with a say over whether the line is ended afterwards.
   *
   * @param sQualifiedModsAndRetType
   *        The modifiers and the return type, as this language spells them. May not be
   *        <code>null</code>.
   * @param sClassName
   *        The class the method belongs to. Only C++ needs it, to qualify the out of line
   *        definition. May be <code>null</code>.
   * @param sNameAndParams
   *        The method name and its parameter list. May not be <code>null</code>.
   * @param sExceptions
   *        The checked exceptions. May be <code>null</code>.
   * @param bEndLine
   *        <code>true</code> to end the line after the signature. Java only - the C++ buffers never
   *        carry a line break here, so the caller's opening brace follows on the same line either
   *        way.
   */
  public final void generateMethodDefHeader (@NonNull final String sQualifiedModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams,
                                             @Nullable final String sExceptions,
                                             final boolean bEndLine)
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
        if (bEndLine)
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

  /**
   * The prefix an out of line member definition needs in this language.
   *
   * @param sClassName
   *        The class the member belongs to. May be <code>null</code>.
   * @return The "ClassName::" prefix C++ wants on an out of line definition, empty for Java. Never
   *         <code>null</code>.
   */
  protected final String getClassQualifier (@Nullable final String sClassName)
  {
    return sClassName == null ? "" : sClassName + "::";
  }

  /**
   * {@return the name of the char stream class the current options select. Never
   * <code>null</code>.}
   */
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

  /**
   * Expand one of the templates below <code>src/main/resources/templates</code> into the current
   * output buffer.
   *
   * @param sName
   *        The template resource path. May not be <code>null</code>.
   * @param aOptions
   *        The values the template substitutes. May not be <code>null</code>.
   * @throws IOException
   *         if the template cannot be read
   */
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
