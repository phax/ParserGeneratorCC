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

import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.context.PGCCContext;

/**
 * The static facade over the JJTree part of the run state.
 */
public class JJTreeGlobals
{
  /** Default constructor. */
  public JJTreeGlobals ()
  {}

  /**
   * This set stores the JJTree-specific options that should not be passed down to JavaCC
   */
  /**
   * The JJTree specific options, which must not be passed down to JavaCC.
   * <p>
   * <code>NODE_STACK_SIZE</code> is in here so that setting it does not draw a complaint from
   * JavaCC, but nothing reads it: the generated node stack is a {@code java.util.List} that grows
   * as needed, so there is no size to configure. It is kept for grammars that still set it.
   */
  private static final Set <String> JJTREE_OPTIONS = Set.of ("JJTREE_OUTPUT_DIRECTORY",
                                                             "MULTI",
                                                             "NODE_PREFIX",
                                                             "NODE_PACKAGE",
                                                             "NODE_EXTENDS",
                                                             "NODE_CLASS",
                                                             "NODE_STACK_SIZE",
                                                             "NODE_DEFAULT_VOID",
                                                             "OUTPUT_FILE",
                                                             "CHECK_DEFINITE_NODE",
                                                             "NODE_SCOPE_HOOK",
                                                             "TRACK_TOKENS",
                                                             "NODE_FACTORY",
                                                             "NODE_USES_PARSER",
                                                             "BUILD_NODE_FILES",
                                                             "VISITOR",
                                                             "VISITOR_EXCEPTION",
                                                             "VISITOR_DATA_TYPE",
                                                             "VISITOR_RETURN_TYPE",
                                                             "VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME",
                                                             "NODE_INCLUDES");

  static void initialize ()
  {
    PGCCContext.current ().jjtree ().reset ();
  }

  /**
   * Whether an option is one JJTree handles itself rather than passing on to the parser generator.
   *
   * @param sOptionName
   *        The option name. May not be <code>null</code>.
   * @return <code>true</code> if JJTree keeps it.
   */
  public static boolean isOptionJJTreeOnly (@NonNull final String sOptionName)
  {
    return JJTREE_OPTIONS.contains (sOptionName.toUpperCase (Locale.US));
  }
}
