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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingStringWriter;

/**
 * Test class for the template language of {@link OutputFileGenerator}.
 * <p>
 * The language was previously only exercised indirectly, by rendering the real templates and
 * checking that nothing blew up. These tests pin down what each construct actually means, which is
 * the only documentation of it besides the implementation.
 *
 * @author Philip Helger
 */
public final class OutputFileGeneratorLanguageTest
{
  /**
   * The generator resolves a template either from the classpath or below "src/main/resources", so
   * the test templates are written into the test classes directory, which is on the classpath.
   */
  private static final File DIR = new File ("target/test-classes/template-language");

  @BeforeClass
  public static void beforeClass ()
  {
    DIR.mkdirs ();
  }

  /**
   * Render a template from its text.
   *
   * @param sTemplate
   *        The template body. May not be <code>null</code>.
   * @param aOptions
   *        The options visible to the template. May not be <code>null</code>.
   * @return The rendered text with normalized line endings. Never <code>null</code>.
   * @throws IOException
   *         On IO error
   */
  private static String _render (final String sTemplate, final Map <String, Object> aOptions) throws IOException
  {
    final File aFile = new File (DIR, "t" + Math.abs (sTemplate.hashCode ()) + ".template");
    Files.write (aFile.toPath (), sTemplate.getBytes (StandardCharsets.UTF_8));

    final OutputFileGenerator aGenerator = new OutputFileGenerator ("/template-language/" + aFile.getName (), aOptions);
    try (final NonBlockingStringWriter aSW = new NonBlockingStringWriter ())
    {
      aGenerator.generate (aSW);
      return aSW.getAsString ().replace ("\r\n", "\n");
    }
  }

  private static Map <String, Object> _options (final Object... aNameValuePairs)
  {
    final Map <String, Object> aRet = new HashMap <> ();
    for (int i = 0; i < aNameValuePairs.length; i += 2)
      aRet.put ((String) aNameValuePairs[i], aNameValuePairs[i + 1]);
    return aRet;
  }

  @Test
  public void testPlainTextIsCopiedThrough () throws IOException
  {
    assertEquals ("hello\nworld\n", _render ("hello\nworld\n", _options ()));
  }

  @Test
  public void testVariableSubstitution () throws IOException
  {
    assertEquals ("class Foo {\n", _render ("class ${NAME} {\n", _options ("NAME", "Foo")));
    // Several on one line
    assertEquals ("a-b\n", _render ("${A}-${B}\n", _options ("A", "a", "B", "b")));
  }

  @Test
  public void testUnknownVariableIsEmpty () throws IOException
  {
    assertEquals ("[]\n", _render ("[${MISSING}]\n", _options ()));
  }

  @Test
  public void testVariableWithDefault () throws IOException
  {
    assertEquals ("[x]\n", _render ("[${MISSING:-x}]\n", _options ()));
    assertEquals ("[set]\n", _render ("[${NAME:-x}]\n", _options ("NAME", "set")));
    // An empty value counts as unset, so the default applies to it as well
    assertEquals ("[x]\n", _render ("[${NAME:-x}]\n", _options ("NAME", "")));
  }

  @Test
  public void testConditionalSubstitution () throws IOException
  {
    assertEquals ("[yes]\n", _render ("[${FLAG?yes:no}]\n", _options ("FLAG", Boolean.TRUE)));
    assertEquals ("[no]\n", _render ("[${FLAG?yes:no}]\n", _options ("FLAG", Boolean.FALSE)));
  }

  @Test
  public void testIfElseFi () throws IOException
  {
    final String sTemplate = "#if FLAG\nyes\n#else\nno\n#fi\n";
    assertEquals ("yes\n", _render (sTemplate, _options ("FLAG", Boolean.TRUE)));
    assertEquals ("no\n", _render (sTemplate, _options ("FLAG", Boolean.FALSE)));
  }

  @Test
  public void testIfWithoutElse () throws IOException
  {
    final String sTemplate = "before\n#if FLAG\ninside\n#fi\nafter\n";
    assertEquals ("before\ninside\nafter\n", _render (sTemplate, _options ("FLAG", Boolean.TRUE)));
    assertEquals ("before\nafter\n", _render (sTemplate, _options ("FLAG", Boolean.FALSE)));
  }

  @Test
  public void testElif () throws IOException
  {
    final String sTemplate = "#if A\na\n#elif B\nb\n#else\nc\n#fi\n";
    assertEquals ("a\n", _render (sTemplate, _options ("A", Boolean.TRUE, "B", Boolean.TRUE)));
    assertEquals ("b\n", _render (sTemplate, _options ("A", Boolean.FALSE, "B", Boolean.TRUE)));
    assertEquals ("c\n", _render (sTemplate, _options ("A", Boolean.FALSE, "B", Boolean.FALSE)));
  }

  @Test
  public void testNestedIf () throws IOException
  {
    final String sTemplate = "#if A\n#if B\nab\n#else\na\n#fi\n#else\nnone\n#fi\n";
    assertEquals ("ab\n", _render (sTemplate, _options ("A", Boolean.TRUE, "B", Boolean.TRUE)));
    assertEquals ("a\n", _render (sTemplate, _options ("A", Boolean.TRUE, "B", Boolean.FALSE)));
    assertEquals ("none\n", _render (sTemplate, _options ("A", Boolean.FALSE, "B", Boolean.TRUE)));
  }

  @Test
  public void testConditionOperators () throws IOException
  {
    final String sTemplate = "#if A && B\nboth\n#fi\n";
    assertEquals ("both\n", _render (sTemplate, _options ("A", Boolean.TRUE, "B", Boolean.TRUE)));
    assertEquals ("", _render (sTemplate, _options ("A", Boolean.TRUE, "B", Boolean.FALSE)));

    assertEquals ("either\n", _render ("#if A || B\neither\n#fi\n", _options ("A", Boolean.FALSE, "B", Boolean.TRUE)));
    assertEquals ("negated\n", _render ("#if !A\nnegated\n#fi\n", _options ("A", Boolean.FALSE)));
  }

  @Test
  public void testStringOptionIsTrueWhenNotEmpty () throws IOException
  {
    final String sTemplate = "#if NAME\nnamed\n#fi\n";
    assertEquals ("named\n", _render (sTemplate, _options ("NAME", "something")));
    assertEquals ("", _render (sTemplate, _options ("NAME", "")));
  }

  @Test
  public void testMissingFiIsAnError ()
  {
    try
    {
      _render ("#if FLAG\ninside\n", _options ("FLAG", Boolean.TRUE));
      fail ("Expected an error about the missing #fi");
    }
    catch (final IOException aEx)
    {
      assertEquals ("Missing \"#fi\"", aEx.getMessage ());
    }
  }

  @Test
  public void testMismatchedBraceIsAnError ()
  {
    try
    {
      _render ("${UNCLOSED\n", _options ());
      fail ("Expected an error about the unbalanced braces");
    }
    catch (final IOException aEx)
    {
      // Expected
    }
  }
}
