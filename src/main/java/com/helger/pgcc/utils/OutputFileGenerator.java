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
package com.helger.pgcc.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.WillNotClose;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingBufferedReader;
import com.helger.base.io.nonblocking.NonBlockingStringWriter;
import com.helger.base.system.ENewLineMode;
import com.helger.base.system.SystemHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;

/**
 * Generates boiler-plate files from templates. Only very basic template processing is supplied - if
 * we need something more sophisticated I suggest we use a third-party library.
 *
 * @author paulcager
 * @since 4.2
 */
public class OutputFileGenerator
{
  /**
   * The encoding every template file is read in.
   */
  public static final Charset TEMPLATE_FILE_CHARSET = StandardCharsets.UTF_8;

  private final String m_sTemplateName;
  private final Map <String, Object> m_aOptions;
  private ENewLineMode m_eNewLineMode = ENewLineMode.DEFAULT;
  /** For testing, reading from file is necessary, to get the latest version */
  private boolean m_bReadFromClasspath = true;

  private String m_sCurrentLine;

  /**
   * Create a generator for one template.
   *
   * @param sTemplateName
   *        the name of the template. E.g. "/templates/java/Token.template".
   * @param aOptions
   *        the processing options in force, such as "STATIC=yes"
   */
  public OutputFileGenerator (final String sTemplateName, @NonNull final Map <String, Object> aOptions)
  {
    m_sTemplateName = sTemplateName;
    m_aOptions = aOptions;
  }

  /**
   * Choose the line separator the generated file carries.
   *
   * @param eNewLineMode
   *        The mode. May not be <code>null</code>.
   * @return this for chaining. Never <code>null</code>.
   */
  @NonNull
  public OutputFileGenerator setNewLineMode (@NonNull final ENewLineMode eNewLineMode)
  {
    ValueEnforcer.notNull (eNewLineMode, "NewLineMode");
    m_eNewLineMode = eNewLineMode;
    return this;
  }

  /**
   * Choose where the template comes from. Reading from the classpath is what a released jar
   * needs; a test that has to see the template in the checkout reads from the file system
   * instead.
   *
   * @param bReadFromClasspath
   *        <code>true</code> to read from the classpath.
   * @return this for chaining. Never <code>null</code>.
   */
  @NonNull
  public OutputFileGenerator setReadFromClasspath (final boolean bReadFromClasspath)
  {
    m_bReadFromClasspath = bReadFromClasspath;
    return this;
  }

  /**
   * Generate the output file.
   *
   * @param aOut
   *        writer
   *
   * @throws IOException
   *         on IO error
   */
  public void generate (@WillNotClose final Writer aOut) throws IOException
  {
    final IReadableResource aRes = m_bReadFromClasspath ? new ClassPathResource (m_sTemplateName)
                                                        : new FileSystemResource ("src/main/resources" +
                                                                                  m_sTemplateName);
    final InputStream aIs = aRes.getInputStream ();
    if (aIs == null)
      throw new IOException ("Invalid template name: " + m_sTemplateName);

    try (final NonBlockingBufferedReader aIn = new NonBlockingBufferedReader (new InputStreamReader (aIs,
                                                                                                     TEMPLATE_FILE_CHARSET)))
    {
      _process (aIn, aOut, false);
    }
  }

  private String _peekLine (@NonNull final NonBlockingBufferedReader aIn) throws IOException
  {
    if (m_sCurrentLine == null)
      m_sCurrentLine = aIn.readLine ();

    return m_sCurrentLine;
  }

  private String _getLine (@NonNull final NonBlockingBufferedReader aIn) throws IOException
  {
    final String sLine = m_sCurrentLine;
    m_sCurrentLine = null;

    if (sLine == null)
      aIn.readLine ();

    return sLine;
  }

  private boolean _evaluate (@NonNull final String sCondition)
  {
    try
    {
      return new ConditionParser (sCondition.trim ()).CompilationUnit (m_aOptions);
    }
    catch (final ParseException e)
    {
      return false;
    }
  }

  private String _substitute (@NonNull final String sText) throws IOException
  {
    final int nStartPos = sText.indexOf ("${");
    if (nStartPos == -1)
    {
      return sText;
    }

    // Find matching "}".
    int nBraceDepth = 1;
    int nEndPos = nStartPos + 2;
    final int nTextLen = sText.length ();

    while (nEndPos < nTextLen && nBraceDepth > 0)
    {
      final char c = sText.charAt (nEndPos);
      if (c == '{')
        nBraceDepth++;
      else
        if (c == '}')
          nBraceDepth--;

      nEndPos++;
    }

    if (nBraceDepth != 0)
      throw new IOException ("Mismatched \"{}\" in template string: " + sText);

    final String sVariableExpression = sText.substring (nStartPos + 2, nEndPos - 1);

    // Find the end of the variable name
    String sValue = null;

    for (int i = 0; i < sVariableExpression.length (); i++)
    {
      final char cCh = sVariableExpression.charAt (i);

      if (cCh == ':' && i < sVariableExpression.length () - 1 && sVariableExpression.charAt (i + 1) == '-')
      {
        sValue = _substituteWithDefault (sVariableExpression.substring (0, i), sVariableExpression.substring (i + 2));
        break;
      }
      if (cCh == '?')
      {
        sValue = _substituteWithConditional (sVariableExpression.substring (0, i),
                                             sVariableExpression.substring (i + 1));
        break;
      }
      if (cCh != '_' && !Character.isJavaIdentifierPart (cCh))
      {
        throw new IOException ("Invalid variable in " + sText);
      }
    }

    if (sValue == null)
    {
      sValue = _substituteWithDefault (sVariableExpression, "");
    }

    return sText.substring (0, nStartPos) + sValue + sText.substring (nEndPos);
  }

  /**
   * Expand a <code>${name?then:else}</code> expression: the part before the colon if the variable
   * is set and true, the part after it otherwise.
   *
   * @param sVariableName
   *        The variable to look at. May not be <code>null</code>.
   * @param sValues
   *        The two alternatives, separated by a colon. May not be <code>null</code>.
   * @return The chosen alternative, itself substituted. Never <code>null</code>.
   * @throws IOException
   *         If the expression is malformed
   */
  private String _substituteWithConditional (final String sVariableName, @NonNull final String sValues)
                                                                                                        throws IOException
  {
    // Split values into true and false values.

    final int nPos = sValues.indexOf (':');
    if (nPos == -1)
      throw new IOException ("No ':' separator in " + sValues);

    if (_evaluate (sVariableName))
      return _substitute (sValues.substring (0, nPos));

    return _substitute (sValues.substring (nPos + 1));
  }

  /**
   * @param sVariableName
   * @param sDefaultValue
   * @return
   */
  private String _substituteWithDefault (@NonNull final String sVariableName, final String sDefaultValue)
                                                                                                          throws IOException
  {
    final Object aObj = m_aOptions.get (sVariableName.trim ());
    if (aObj == null || aObj.toString ().length () == 0)
      return _substitute (sDefaultValue);

    return aObj.toString ();
  }

  private void _write (@NonNull final Writer aOut, final String sText) throws IOException
  {
    String sExpanded = sText;
    while (sExpanded.indexOf ("${") != -1)
    {
      sExpanded = _substitute (sExpanded);
    }

    // TODO :: Added by Sreenivas on 12 June 2013 for 6.0 release, merged in to
    // 6.1 release for sake of compatibility by cainsley ... This needs to be
    // removed urgently!!!
    if (sExpanded.startsWith ("\\#"))
    {
      // Hack to escape # for C++
      sExpanded = sExpanded.substring (1);
    }

    aOut.write (sExpanded);
    aOut.write (m_eNewLineMode.getText ());
  }

  private void _process (final NonBlockingBufferedReader aIn, @NonNull final Writer aOut, final boolean bIgnoring)
                                                                                                                   throws IOException
  {
    // out.println("*** process ignore=" + ignoring + " : " + peekLine(in));
    while (_peekLine (aIn) != null)
    {
      if (_peekLine (aIn).trim ().startsWith ("#if"))
      {
        _processIf (aIn, aOut, bIgnoring);
      }
      else
        if (_peekLine (aIn).trim ().startsWith ("#"))
        {
          break;
        }
        else
        {
          final String sLine = _getLine (aIn);
          if (!bIgnoring)
            _write (aOut, sLine);
        }
    }

    // Important to flush at the end!
    aOut.flush ();
  }

  private void _processIf (final NonBlockingBufferedReader aIn, final Writer aOut, final boolean bIgnoring)
                                                                                                            throws IOException
  {
    String sLine = _getLine (aIn).trim ();
    assert sLine.trim ().startsWith ("#if");
    boolean bFoundTrueCondition = false;

    boolean bCondition = _evaluate (sLine.substring (3).trim ());
    while (true)
    {
      _process (aIn, aOut, bIgnoring || bFoundTrueCondition || !bCondition);
      bFoundTrueCondition |= bCondition;

      if (_peekLine (aIn) == null || !_peekLine (aIn).trim ().startsWith ("#elif"))
        break;

      bCondition = _evaluate (_getLine (aIn).trim ().substring (5).trim ());
    }

    if (_peekLine (aIn) != null && _peekLine (aIn).trim ().startsWith ("#else"))
    {
      // Discard the #else line
      _getLine (aIn);
      _process (aIn, aOut, bIgnoring || bFoundTrueCondition);
    }

    sLine = _getLine (aIn);

    if (sLine == null)
      throw new IOException ("Missing \"#fi\"");

    if (!sLine.trim ().startsWith ("#fi"))
      throw new IOException ("Expected \"#fi\", got: " + sLine);
  }

  /**
   * Expand one template from the command line, with a fixed set of substitutions, for looking at
   * what a template produces without running the generator.
   *
   * @param aArgs
   *        The template file and the output file. May not be <code>null</code>.
   *
   * @throws Exception
   *         if the template cannot be read or the output cannot be written
   */
  public static void main (@NonNull final String [] aArgs) throws Exception
  {
    final Map <String, Object> aMap = new HashMap <> ();
    aMap.put ("falseArg", Boolean.FALSE);
    aMap.put ("trueArg", Boolean.TRUE);
    aMap.put ("stringValue", "someString");

    try (final Writer aWriter = FileHelper.getBufferedWriter (new File (aArgs[1]), SystemHelper.getSystemCharset ()))
    {
      new OutputFileGenerator (aArgs[0], aMap).generate (aWriter);
    }
  }

  /**
   * Expand one template straight into a file.
   *
   * @param sTemplateFile
   *        The template resource path. May not be <code>null</code>.
   * @param aOptions
   *        The values the template substitutes. May not be <code>null</code>.
   * @param sOutputFileName
   *        The file to write. May not be <code>null</code>.
   * @param aOutputCharset
   *        The encoding to write it in. May not be <code>null</code>.
   *
   * @throws IOException
   *         if the template cannot be read or the file cannot be written
   */
  public static void generateFromTemplate (final String sTemplateFile,
                                           final Map <String, Object> aOptions,
                                           final String sOutputFileName,
                                           @NonNull final Charset aOutputCharset) throws IOException
  {
    final OutputFileGenerator aOutputGenerator = new OutputFileGenerator (sTemplateFile, aOptions);
    try (final NonBlockingStringWriter aSw = new NonBlockingStringWriter ())
    {
      aOutputGenerator.generate (aSw);
      SimpleFileIO.writeFile (new File (sOutputFileName), aSw.getAsString (), aOutputCharset);
    }
  }
}
