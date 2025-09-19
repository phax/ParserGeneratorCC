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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;

public class JJTreeGlobals
{
  /**
   * This set stores the JJTree-specific options that should not be passed down
   * to JavaCC
   */
  private static final Set <String> s_jjtreeOptions = new HashSet <> ();

  static final List <String> toolList = new ArrayList <> ();

  /**
   * Use this like className.
   **/
  public static String s_parserName;

  /**
   * The package that the parser lives in. If the grammar doesn't specify a
   * package it is the empty string.
   **/
  public static String s_packageName = "";

  /**
   * The package the node files live in. If the NODE_PACKAGE option is not set,
   * then this defaults to packageName.
   **/
  public static String s_nodePackageName = "";

  /**
   * The <code>implements</code> token of the parser class. If the parser
   * doesn't have one then it is the first "{" of the parser class body.
   **/
  public static Token s_parserImplements;

  /**
   * The first token of the parser class body (the <code>{</code>). The JJTree
   * state is inserted after this token.
   **/
  public static Token s_parserClassBodyStart;

  /**
   * The first token of the <code>import</code> list, or the position where such
   * a list should be inserted. The import for the Node Package is inserted
   * after this token.
   **/
  public static Token s_parserImports;

  /**
   * This is mapping from production names to ASTProduction objects.
   **/
  static final Map <String, ASTProduction> s_productions = new HashMap <> ();

  static void initialize ()
  {
    toolList.clear ();
    s_parserName = null;
    s_packageName = "";
    s_parserImplements = null;
    s_parserClassBodyStart = null;
    s_parserImports = null;
    s_productions.clear ();

    s_jjtreeOptions.clear ();
    s_jjtreeOptions.add ("JJTREE_OUTPUT_DIRECTORY");
    s_jjtreeOptions.add ("MULTI");
    s_jjtreeOptions.add ("NODE_PREFIX");
    s_jjtreeOptions.add ("NODE_PACKAGE");
    s_jjtreeOptions.add ("NODE_EXTENDS");
    s_jjtreeOptions.add ("NODE_CLASS");
    s_jjtreeOptions.add ("NODE_STACK_SIZE");
    s_jjtreeOptions.add ("NODE_DEFAULT_VOID");
    s_jjtreeOptions.add ("OUTPUT_FILE");
    s_jjtreeOptions.add ("CHECK_DEFINITE_NODE");
    s_jjtreeOptions.add ("NODE_SCOPE_HOOK");
    s_jjtreeOptions.add ("TRACK_TOKENS");
    s_jjtreeOptions.add ("NODE_FACTORY");
    s_jjtreeOptions.add ("NODE_USES_PARSER");
    s_jjtreeOptions.add ("BUILD_NODE_FILES");
    s_jjtreeOptions.add ("VISITOR");
    s_jjtreeOptions.add ("VISITOR_EXCEPTION");
    s_jjtreeOptions.add ("VISITOR_DATA_TYPE");
    s_jjtreeOptions.add ("VISITOR_RETURN_TYPE");
    s_jjtreeOptions.add ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME");
    s_jjtreeOptions.add ("NODE_INCLUDES");
  }

  static
  {
    initialize ();
  }

  public static boolean isOptionJJTreeOnly (@Nonnull final String optionName)
  {
    return s_jjtreeOptions.contains (optionName.toUpperCase (Locale.US));
  }
}
