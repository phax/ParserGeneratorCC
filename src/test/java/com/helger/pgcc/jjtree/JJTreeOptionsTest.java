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
package com.helger.pgcc.jjtree;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;

/**
 * Test the JJTree-specific options.
 *
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public final class JJTreeOptionsTest
{
  @Before
  public void beforeEach ()
  {
    JJTreeOptions.init ();
    JavaCCErrors.reInit ();
  }

  @Test
  public void testOutputDirectory ()
  {
    assertEquals (new File ("."), Options.getOutputDirectory ());
    assertEquals (new File ("."), JJTreeOptions.getJJTreeOutputDirectory ());

    Options.setInputFileOption (null, null, Options.USEROPTION__OUTPUT_DIRECTORY, "test/output");
    assertEquals (new File ("test/output"), Options.getOutputDirectory ());
    assertEquals (new File ("test/output"), JJTreeOptions.getJJTreeOutputDirectory ());

    Options.setInputFileOption (null, null, "JJTREE_OUTPUT_DIRECTORY", "test/jjtreeoutput");
    assertEquals (new File ("test/output"), Options.getOutputDirectory ());
    assertEquals (new File ("test/jjtreeoutput"), JJTreeOptions.getJJTreeOutputDirectory ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testNodeFactory ()
  {
    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    Options.setInputFileOption (null, null, "NODE_FACTORY", Boolean.FALSE);
    assertEquals (JJTreeOptions.getNodeFactory (), "");

    JJTreeOptions.init ();
    Options.setInputFileOption (null, null, "NODE_FACTORY", Boolean.TRUE);
    assertEquals (JJTreeOptions.getNodeFactory (), "*");

    JJTreeOptions.init ();
    Options.setInputFileOption (null, null, "NODE_FACTORY", "mypackage.MyNode");
    assertEquals (JJTreeOptions.getNodeFactory (), "mypackage.MyNode");

    assertEquals (0, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  @Test
  public void testNodeClass ()
  {
    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());

    assertEquals ("", JJTreeOptions.getNodeClass ());
    // Need some functional tests, as well.
  }

  @Test
  public void testValidate ()
  {
    Options.setCmdLineOption ("VISITOR_DATA_TYPE=Object");
    JJTreeOptions.validate ();
    assertEquals (1, JavaCCErrors.getWarningCount ());

    JJTreeOptions.init ();
    JavaCCErrors.reInit ();

    Options.setCmdLineOption ("VISITOR_DATA_TYPE=Object");
    Options.setCmdLineOption ("VISITOR=true");
    JJTreeOptions.validate ();
    assertEquals (0, JavaCCErrors.getWarningCount ());

    JJTreeOptions.init ();
    JavaCCErrors.reInit ();

    Options.setCmdLineOption ("VISITOR_DATA_TYPE=Object");
    JJTreeOptions.validate ();
    assertEquals (1, JavaCCErrors.getWarningCount ());
  }

  @Test
  public void testValidateReturnType ()
  {
    Options.setCmdLineOption ("VISITOR_DATA_TYPE=String");
    JJTreeOptions.validate ();
    assertEquals (1, JavaCCErrors.getWarningCount ());

    JJTreeOptions.init ();
    JavaCCErrors.reInit ();

    Options.setCmdLineOption ("VISITOR_DATA_TYPE=String");
    Options.setCmdLineOption ("VISITOR=true");
    JJTreeOptions.validate ();
    assertEquals (0, JavaCCErrors.getWarningCount ());

    JJTreeOptions.init ();
    JavaCCErrors.reInit ();

    Options.setCmdLineOption ("VISITOR_DATA_TYPE=String");
    JJTreeOptions.validate ();
    assertEquals (1, JavaCCErrors.getWarningCount ());
  }
}
