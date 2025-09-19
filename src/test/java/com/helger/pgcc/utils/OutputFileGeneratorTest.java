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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.io.nonblocking.NonBlockingStringWriter;
import com.helger.io.file.FileSystemRecursiveIterator;
import com.helger.io.file.IFileFilter;

public final class OutputFileGeneratorTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OutputFileGeneratorTest.class);

  @Test
  public void testReadTemplates () throws IOException
  {
    final File fBaseDir = new File ("src/main/resources").getAbsoluteFile ();
    // Find all template files
    for (final File f : new FileSystemRecursiveIterator (new File (fBaseDir,
                                                                   "templates/")).withFilter (IFileFilter.filenameEndsWith (".template")))
    {
      final String sTemplateName = f.getAbsolutePath ().substring (fBaseDir.getAbsolutePath ().length () + 1);
      LOGGER.info ("Parsing template file " + sTemplateName);
      final Map <String, Object> aOptions = new HashMap <> ();
      aOptions.put ("AT_LEAST_JDK6", Boolean.TRUE);
      aOptions.put ("AT_LEAST_JDK7", Boolean.FALSE);

      // Main parsing
      final OutputFileGenerator aOutputGenerator = new OutputFileGenerator (sTemplateName, aOptions);
      try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
      {
        aOutputGenerator.generate (sw);
      }
    }
  }
}
