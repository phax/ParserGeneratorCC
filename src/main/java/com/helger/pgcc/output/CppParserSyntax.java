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
package com.helger.pgcc.output;

import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * The {@link IParserSyntax} of C++.
 *
 * @author Philip Helger
 */
final class CppParserSyntax implements IParserSyntax
{
  static final CppParserSyntax INSTANCE = new CppParserSyntax ();

  private CppParserSyntax ()
  {}

  @NonNull
  public String getMemberAccess ()
  {
    return "->";
  }

  @NonNull
  public String getLoopStart (final int nLabelIndex)
  {
    return "while (!hasError) {";
  }

  @NonNull
  public String getLoopBreak (final int nLabelIndex)
  {
    return "\ngoto end_label_" + nLabelIndex + ";";
  }

  @NonNull
  public String getLoopEnd (final int nLabelIndex)
  {
    return "\nend_label_" + nLabelIndex + ": ;";
  }

  @NonNull
  public String getMissingReturnStatement ()
  {
    return "    throw \"Missing return statement in function\";";
  }

  @NonNull
  public String getThrowsClause ()
  {
    return "";
  }

  @NonNull
  public List <String> getTraceEnterLines (@NonNull final String sProductionName)
  {
    // No finally in C++, so a pair of scope guards does the entry and the exit
    return List.of ("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\"" +
                    sProductionName +
                    "\"); });",
                    "    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\"" +
                                                                                              sProductionName +
                                                                                              "\"); });");
  }

  @NonNull
  public List <String> getTraceExitLines (@NonNull final String sProductionName)
  {
    return List.of ("    } catch(...) { }");
  }

  @NonNull
  public String getLookaheadEntryDeclaration (@NonNull final String sInternalName)
  {
    return " inline bool jj_2" + sInternalName + "(int xla)";
  }

  @NonNull
  public String getLookaheadScanDeclaration (@NonNull final String sInternalName)
  {
    return " inline bool jj_3" + sInternalName + "()";
  }
}
