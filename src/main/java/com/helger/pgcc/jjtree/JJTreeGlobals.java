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
package com.helger.pgcc.jjtree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.context.PGCCContext;

public class JJTreeGlobals
{
  /**
   * This set stores the JJTree-specific options that should not be passed down to JavaCC
   */
  private static final Set <String> JJTREE_OPTIONS = new HashSet <> ();

  static final List <String> TOOL_LIST = new ArrayList <> ();

  static void initialize ()
  {
    TOOL_LIST.clear ();
    PGCCContext.current ().jjtree ().reset ();

    JJTREE_OPTIONS.clear ();
    JJTREE_OPTIONS.add ("JJTREE_OUTPUT_DIRECTORY");
    JJTREE_OPTIONS.add ("MULTI");
    JJTREE_OPTIONS.add ("NODE_PREFIX");
    JJTREE_OPTIONS.add ("NODE_PACKAGE");
    JJTREE_OPTIONS.add ("NODE_EXTENDS");
    JJTREE_OPTIONS.add ("NODE_CLASS");
    JJTREE_OPTIONS.add ("NODE_STACK_SIZE");
    JJTREE_OPTIONS.add ("NODE_DEFAULT_VOID");
    JJTREE_OPTIONS.add ("OUTPUT_FILE");
    JJTREE_OPTIONS.add ("CHECK_DEFINITE_NODE");
    JJTREE_OPTIONS.add ("NODE_SCOPE_HOOK");
    JJTREE_OPTIONS.add ("TRACK_TOKENS");
    JJTREE_OPTIONS.add ("NODE_FACTORY");
    JJTREE_OPTIONS.add ("NODE_USES_PARSER");
    JJTREE_OPTIONS.add ("BUILD_NODE_FILES");
    JJTREE_OPTIONS.add ("VISITOR");
    JJTREE_OPTIONS.add ("VISITOR_EXCEPTION");
    JJTREE_OPTIONS.add ("VISITOR_DATA_TYPE");
    JJTREE_OPTIONS.add ("VISITOR_RETURN_TYPE");
    JJTREE_OPTIONS.add ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME");
    JJTREE_OPTIONS.add ("NODE_INCLUDES");
  }

  static
  {
    initialize ();
  }

  public static boolean isOptionJJTreeOnly (@NonNull final String optionName)
  {
    return JJTREE_OPTIONS.contains (optionName.toUpperCase (Locale.US));
  }
}
