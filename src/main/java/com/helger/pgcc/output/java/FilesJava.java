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
package com.helger.pgcc.output.java;

import static com.helger.pgcc.parser.JavaCCGlobals.grammar;
import static com.helger.pgcc.parser.JavaCCGlobals.printToken;
import static com.helger.pgcc.parser.JavaCCParserConstants.PACKAGE;
import static com.helger.pgcc.parser.JavaCCParserConstants.SEMICOLON;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.WillNotClose;
import com.helger.base.system.EJavaVersion;
import com.helger.pgcc.JavaVersionHelper;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.context.GrammarState;
import com.helger.pgcc.context.ProcessState;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.utils.OutputFileGenerator;

/**
 * Generate CharStream, TokenManager and Exceptions.
 */
public class FilesJava
{
  /**
   * ID of the latest version (of JavaCC) in which one of the CharStream classes or the CharStream
   * interface is modified.
   */
  private static final String CHAR_STREAM_VERSION = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the TokenManager interface is modified.
   */
  private static final String TOKEN_MANAGER_VERSION = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the Token class is modified.
   */
  private static final String TOKEN_VERSION = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the ParseException class is modified.
   */
  private static final String PARSE_EXCEPTION_VERSION = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the TokenMgrError class is modified.
   */
  private static final String TOKEN_MGR_ERROR_VERSION = PGVersion.MAJOR_DOT_MINOR;

  private FilesJava ()
  {}

  /**
   * Choose where the templates come from. The default reads them from the classpath, which is what
   * a released jar needs; a test that has to see the templates in the checkout turns it off. This
   * is process wide state, so a test that changes it has to change it back.
   *
   * @param bReadFromClassPath
   *        <code>true</code> to read from the classpath, <code>false</code> to read from the file
   *        system.
   */
  public static void setReadFromClassPath (final boolean bReadFromClassPath)
  {
    ProcessState.getInstance ().setReadTemplatesFromClassPath (bReadFromClassPath);
  }

  private static Map <String, Object> _getDefaultOptions ()
  {
    final EJavaVersion eJDKVersion = Options.getJdkVersion ();
    final Map <String, Object> aRet = Options.getAllOptions ();
    aRet.put ("AT_LEAST_JDK6", Boolean.valueOf (eJDKVersion.isNewerOrEqualsThan (EJavaVersion.JDK_1_6)));
    aRet.put ("AT_LEAST_JDK7", Boolean.valueOf (eJDKVersion.isNewerOrEqualsThan (EJavaVersion.JDK_1_7)));
    aRet.put ("BEFORE_JDK7", Boolean.valueOf (JavaVersionHelper.isOlderThan (eJDKVersion, EJavaVersion.JDK_1_7)));
    return aRet;
  }

  private static void _writePackageName (@NonNull @WillNotClose final PrintWriter aOstr)
  {
    final GrammarState aGrammar = grammar ();
    if (aGrammar.cuToInsertionPoint1 ().isNotEmpty () && aGrammar.cuToInsertionPoint1 ().get (0).kind == PACKAGE)
    {
      for (int i = 1; i < aGrammar.cuToInsertionPoint1 ().size (); i++)
      {
        if (aGrammar.cuToInsertionPoint1 ().get (i).kind == SEMICOLON)
        {
          aGrammar.setCurrentLine (aGrammar.cuToInsertionPoint1 ().get (0).beginLine);
          aGrammar.setCurrentColumn (aGrammar.cuToInsertionPoint1 ().get (0).beginColumn);
          for (int j = 0; j <= i; j++)
          {
            printToken (aGrammar.cuToInsertionPoint1 ().get (j), aOstr);
          }
          aOstr.println ();
          aOstr.println ();
          break;
        }
      }
    }
  }

  /**
   * Write CharStream.java, the interface the generated token manager reads through.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genCharStream (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "CharStream.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        CHAR_STREAM_VERSION,
                                                        new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getCharStreamTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file CharStream.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write AbstractCharStream.java, the part every char stream implementation shares.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genAbstractCharStream (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "AbstractCharStream.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        CHAR_STREAM_VERSION,
                                                        new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        // Copy package name
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getAbstractCharStreamTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file AbstractCharStream.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write JavaCharStream.java, the char stream that understands Java unicode escapes in the input.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genJavaCharStream (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "JavaCharStream.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        CHAR_STREAM_VERSION,
                                                        new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        // Copy package name
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getJavaCharStreamTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file JavaCharStream.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write SimpleCharStream.java, the char stream that reads from a Reader.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genSimpleCharStream (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "SimpleCharStream.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        CHAR_STREAM_VERSION,
                                                        new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getSimpleCharStreamTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file SimpleCharStream.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write CharSequenceCharStream.java, the char stream that reads from something already in memory.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genCharSequenceCharStream (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "CharSequenceCharStream.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        CHAR_STREAM_VERSION,
                                                        new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getCharSequenceCharStreamTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file CharSequenceCharStream.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write the extra files the modern template type needs, the Provider interfaces and their
   * implementations.
   */
  public static void genJavaModernFiles ()
  {
    // Abstraction for char reader
    _genMiscFile ("Provider.java", "/templates/java/stream/modern/Provider.template");
    _genMiscFile ("StringProvider.java", "/templates/java/stream/modern/StringProvider.template");
    _genMiscFile ("StreamProvider.java", "/templates/java/stream/modern/StreamProvider.template");
  }

  private static void _genMiscFile (final String sFileName, final String sTemplatePath) throws Error
  {
    final File aFile = new File (Options.getOutputDirectory (), sFileName);
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        PARSE_EXCEPTION_VERSION,
                                                        new String [] { Options.USEROPTION__KEEP_LINE_COLUMN }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (sTemplatePath, aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file " + sFileName + " for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write ParseException.java, the exception the generated parser throws.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genParseException (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "ParseException.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        PARSE_EXCEPTION_VERSION,
                                                        new String [] { Options.USEROPTION__KEEP_LINE_COLUMN }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getParseExceptionTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file ParseException.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write the exception the generated token manager throws when it cannot match.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genTokenMgrError (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final String sFilename = Options.getTokenMgrErrorClass () + ".java";
    final File aFile = new File (Options.getOutputDirectory (), sFilename);

    try (final OutputFile aOutputFile = new OutputFile (aFile, TOKEN_MGR_ERROR_VERSION, new String [0]))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getTokenMgrErrorTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file " + sFilename + " for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write Token.java, the token the generated parser works with.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genToken (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "Token.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        TOKEN_VERSION,
                                                        new String [] { Options.USEROPTION__TOKEN_EXTENDS,
                                                                        Options.USEROPTION__KEEP_LINE_COLUMN,
                                                                        Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = _getDefaultOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getTokenTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file Token.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

  /**
   * Write TokenManager.java, the interface the generated parser pulls tokens through.
   *
   * @param aLocations
   *        Where the templates live. May not be <code>null</code>.
   */
  public static void genTokenManager (@NonNull final IJavaResourceTemplateLocations aLocations)
  {
    final File aFile = new File (Options.getOutputDirectory (), "TokenManager.java");
    try (final OutputFile aOutputFile = new OutputFile (aFile,
                                                        TOKEN_MANAGER_VERSION,
                                                        new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!aOutputFile.needToWrite ())
        return;

      try (final PrintWriter aOstr = aOutputFile.getPrintWriter ())
      {
        _writePackageName (aOstr);

        final Map <String, Object> aOptions = Options.getAllOptions ();
        final OutputFileGenerator aGenerator = new OutputFileGenerator (aLocations.getTokenManagerTemplateResourceUrl (),
                                                                        aOptions);
        aGenerator.setReadFromClasspath (ProcessState.getInstance ().isReadTemplatesFromClassPath ());
        aGenerator.generate (aOstr);
      }
    }
    catch (final IOException e)
    {
      JavaCCErrors.semanticError ("Could not open file TokenManager.java for writing.", e);
      throw new UncheckedIOException (e);
    }
  }

}
