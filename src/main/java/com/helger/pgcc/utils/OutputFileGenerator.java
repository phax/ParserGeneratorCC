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

import jakarta.annotation.Nonnull;

/**
 * Generates boiler-plate files from templates. Only very basic template
 * processing is supplied - if we need something more sophisticated I suggest we
 * use a third-party library.
 *
 * @author paulcager
 * @since 4.2
 */
public class OutputFileGenerator
{
  public static final Charset TEMPLATE_FILE_CHARSET = StandardCharsets.UTF_8;

  private final String m_sTemplateName;
  private final Map <String, Object> m_aOptions;
  private ENewLineMode m_eNewLineMode = ENewLineMode.DEFAULT;
  /** For testing, reading from file is necessary, to get the latest version */
  private boolean m_bReadFromClasspath = true;

  private String m_sCurrentLine;

  /**
   * @param templateName
   *        the name of the template. E.g. "/templates/Token.template".
   * @param options
   *        the processing options in force, such as "STATIC=yes"
   */
  public OutputFileGenerator (final String templateName, @Nonnull final Map <String, Object> options)
  {
    m_sTemplateName = templateName;
    m_aOptions = options;
  }

  @Nonnull
  public OutputFileGenerator setNewLineMode (@Nonnull final ENewLineMode eNewLineMode)
  {
    ValueEnforcer.notNull (eNewLineMode, "NewLineMode");
    m_eNewLineMode = eNewLineMode;
    return this;
  }

  @Nonnull
  public OutputFileGenerator setReadFromClasspath (final boolean bReadFromClasspath)
  {
    m_bReadFromClasspath = bReadFromClasspath;
    return this;
  }

  /**
   * Generate the output file.
   *
   * @param out
   *        writer
   * @throws IOException
   *         on IO error
   */
  public void generate (@WillNotClose final Writer out) throws IOException
  {
    final IReadableResource aRes = m_bReadFromClasspath ? new ClassPathResource (m_sTemplateName)
                                                        : new FileSystemResource ("src/main/resources" + m_sTemplateName);
    final InputStream is = aRes.getInputStream ();
    if (is == null)
      throw new IOException ("Invalid template name: " + m_sTemplateName);

    try (final NonBlockingBufferedReader in = new NonBlockingBufferedReader (new InputStreamReader (is, TEMPLATE_FILE_CHARSET)))
    {
      _process (in, out, false);
    }
  }

  private String _peekLine (final NonBlockingBufferedReader in) throws IOException
  {
    if (m_sCurrentLine == null)
      m_sCurrentLine = in.readLine ();

    return m_sCurrentLine;
  }

  private String _getLine (final NonBlockingBufferedReader in) throws IOException
  {
    final String line = m_sCurrentLine;
    m_sCurrentLine = null;

    if (line == null)
      in.readLine ();

    return line;
  }

  private boolean _evaluate (@Nonnull final String condition)
  {
    try
    {
      return new ConditionParser (condition.trim ()).CompilationUnit (m_aOptions);
    }
    catch (final ParseException e)
    {
      return false;
    }
  }

  private String _substitute (final String text) throws IOException
  {
    final int startPos = text.indexOf ("${");
    if (startPos == -1)
    {
      return text;
    }

    // Find matching "}".
    int nBraceDepth = 1;
    int nEndPos = startPos + 2;
    final int nTextLen = text.length ();

    while (nEndPos < nTextLen && nBraceDepth > 0)
    {
      final char c = text.charAt (nEndPos);
      if (c == '{')
        nBraceDepth++;
      else
        if (c == '}')
          nBraceDepth--;

      nEndPos++;
    }

    if (nBraceDepth != 0)
      throw new IOException ("Mismatched \"{}\" in template string: " + text);

    final String variableExpression = text.substring (startPos + 2, nEndPos - 1);

    // Find the end of the variable name
    String value = null;

    for (int i = 0; i < variableExpression.length (); i++)
    {
      final char ch = variableExpression.charAt (i);

      if (ch == ':' && i < variableExpression.length () - 1 && variableExpression.charAt (i + 1) == '-')
      {
        value = _substituteWithDefault (variableExpression.substring (0, i), variableExpression.substring (i + 2));
        break;
      }
      if (ch == '?')
      {
        value = _substituteWithConditional (variableExpression.substring (0, i), variableExpression.substring (i + 1));
        break;
      }
      if (ch != '_' && !Character.isJavaIdentifierPart (ch))
      {
        throw new IOException ("Invalid variable in " + text);
      }
    }

    if (value == null)
    {
      value = _substituteWithDefault (variableExpression, "");
    }

    return text.substring (0, startPos) + value + text.substring (nEndPos);
  }

  /**
   * @param substring
   * @param defaultValue
   * @return
   * @throws IOException
   */
  private String _substituteWithConditional (final String variableName, final String values) throws IOException
  {
    // Split values into true and false values.

    final int pos = values.indexOf (':');
    if (pos == -1)
      throw new IOException ("No ':' separator in " + values);

    if (_evaluate (variableName))
      return _substitute (values.substring (0, pos));

    return _substitute (values.substring (pos + 1));
  }

  /**
   * @param variableName
   * @param defaultValue
   * @return
   */
  private String _substituteWithDefault (final String variableName, final String defaultValue) throws IOException
  {
    final Object obj = m_aOptions.get (variableName.trim ());
    if (obj == null || obj.toString ().length () == 0)
      return _substitute (defaultValue);

    return obj.toString ();
  }

  private void _write (final Writer out, final String sText) throws IOException
  {
    String text = sText;
    while (text.indexOf ("${") != -1)
    {
      text = _substitute (text);
    }

    // TODO :: Added by Sreenivas on 12 June 2013 for 6.0 release, merged in to
    // 6.1 release for sake of compatibility by cainsley ... This needs to be
    // removed urgently!!!
    if (text.startsWith ("\\#"))
    {
      // Hack to escape # for C++
      text = text.substring (1);
    }

    out.write (text);
    out.write (m_eNewLineMode.getText ());
  }

  private void _process (final NonBlockingBufferedReader in, final Writer out, final boolean ignoring) throws IOException
  {
    // out.println("*** process ignore=" + ignoring + " : " + peekLine(in));
    while (_peekLine (in) != null)
    {
      if (_peekLine (in).trim ().startsWith ("#if"))
      {
        _processIf (in, out, ignoring);
      }
      else
        if (_peekLine (in).trim ().startsWith ("#"))
        {
          break;
        }
        else
        {
          final String line = _getLine (in);
          if (!ignoring)
            _write (out, line);
        }
    }

    // Important to flush at the end!
    out.flush ();
  }

  private void _processIf (final NonBlockingBufferedReader in, final Writer out, final boolean ignoring) throws IOException
  {
    String line = _getLine (in).trim ();
    assert line.trim ().startsWith ("#if");
    boolean foundTrueCondition = false;

    boolean condition = _evaluate (line.substring (3).trim ());
    while (true)
    {
      _process (in, out, ignoring || foundTrueCondition || !condition);
      foundTrueCondition |= condition;

      if (_peekLine (in) == null || !_peekLine (in).trim ().startsWith ("#elif"))
        break;

      condition = _evaluate (_getLine (in).trim ().substring (5).trim ());
    }

    if (_peekLine (in) != null && _peekLine (in).trim ().startsWith ("#else"))
    {
      _getLine (in); // Discard the #else line
      _process (in, out, ignoring || foundTrueCondition);
    }

    line = _getLine (in);

    if (line == null)
      throw new IOException ("Missing \"#fi\"");

    if (!line.trim ().startsWith ("#fi"))
      throw new IOException ("Expected \"#fi\", got: " + line);
  }

  public static void main (final String [] args) throws Exception
  {
    final Map <String, Object> map = new HashMap <> ();
    map.put ("falseArg", Boolean.FALSE);
    map.put ("trueArg", Boolean.TRUE);
    map.put ("stringValue", "someString");

    try (final Writer aWriter = FileHelper.getBufferedWriter (new File (args[1]), SystemHelper.getSystemCharset ()))
    {
      new OutputFileGenerator (args[0], map).generate (aWriter);
    }
  }

  public static void generateFromTemplate (final String templateFile,
                                           final Map <String, Object> options,
                                           final String outputFileName,
                                           @Nonnull final Charset aOutputCharset) throws IOException
  {
    final OutputFileGenerator aOutputGenerator = new OutputFileGenerator (templateFile, options);
    try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
    {
      aOutputGenerator.generate (sw);
      SimpleFileIO.writeFile (new File (outputFileName), sw.getAsString (), aOutputCharset);
    }
  }
}
