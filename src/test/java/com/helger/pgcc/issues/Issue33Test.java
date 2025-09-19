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
package com.helger.pgcc.issues;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.Test;

public class Issue33Test
{
  @Test
  public void testParse () throws Exception
  {
    final File aSrc = new File ("src/test/resources/issues/33/grammar.jj");
    final File aData = new File ("src/test/resources/issues/33/sample.txt");
    final File aOutDir = new File ("target/issue33");
    com.helger.pgcc.parser.Main.mainProgram ("-JDK_VERSION=1.8",
                                             "-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath (),
                                             aSrc.getAbsolutePath ());

    /*
     * Compile the resulting output
     */
    final JavaCompiler javac = ToolProvider.getSystemJavaCompiler ();
    final StandardJavaFileManager fileManager = javac.getStandardFileManager (null, null, null);

    final List <File> files = Arrays.asList (aOutDir.listFiles (f -> f.getName ().endsWith (".java")));
    final Iterable <? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles (files);
    javac.getTask (null, fileManager, null, null, null, compilationUnits1).call ();

    /*
     * Load and run the parser on a test dataset
     */
    try (final InputStream in = new FileInputStream (aData))
    {
      final ClassLoader loader = URLClassLoader.newInstance (new URL [] { aOutDir.toURI ().toURL () },
                                                             getClass ().getClassLoader ());
      final Class <?> clazz = Class.forName ("IssueParser", true, loader);
      final Constructor <? extends Object> constructor = clazz.getConstructor (new Class [] { InputStream.class,
                                                                                              Charset.class });
      final Object obj = constructor.newInstance (new Object [] { in, Charset.defaultCharset () });
      final Method parse = clazz.getDeclaredMethod ("parse", new Class [] {});
      final int i = ((Integer) parse.invoke (obj, new Object [] {})).intValue ();
      assertEquals (i, 10);
    }
  }
}
