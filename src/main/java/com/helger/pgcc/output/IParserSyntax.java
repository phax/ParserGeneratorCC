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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;

/**
 * The pieces of parser syntax that differ between the target languages.
 * <p>
 * {@code ParseEngine} builds the body of every production as a string and used to decide these
 * with a <code>switch</code> on {@link EOutputLanguage} at each spot. They are gathered here
 * instead, so that the engine describes <em>what</em> it wants emitted and the implementation
 * decides <em>how</em>. Adding a target language becomes a matter of implementing this rather than
 * finding every branch.
 *
 * @author Philip Helger
 */
public interface IParserSyntax
{
  /**
   * @return How a member of a pointer or reference is reached - "." in Java, "-&gt;" in C++. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getMemberAccess ();

  /**
   * The head of a loop that the generated code may want to leave from the inside.
   *
   * @param nLabelIndex
   *        The unique number of this loop within the production.
   * @return The code that opens the loop, without the indentation change. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getLoopStart (int nLabelIndex);

  /**
   * @param nLabelIndex
   *        The number used by the matching {@link #getLoopStart(int)}.
   * @return The statement that leaves that loop. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getLoopBreak (int nLabelIndex);

  /**
   * @param nLabelIndex
   *        The number used by the matching {@link #getLoopStart(int)}.
   * @return What has to follow the closing brace of the loop - a landing label in C++, nothing in
   *         Java. Never <code>null</code>, but maybe empty.
   */
  @NonNull
  String getLoopEnd (int nLabelIndex);

  /**
   * @return The statement that a non-void production ends with when control can fall off the end -
   *         the compiler requires it in Java, and C++ needs its own spelling. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getMissingReturnStatement ();

  /**
   * @return What follows a production's parameter list to declare that it can fail - " throws
   *         ParseException" in Java, nothing in C++, which reports through its error handler. Never
   *         <code>null</code>, but maybe empty.
   */
  @NonNull
  String getThrowsClause ();

  /**
   * @param eLanguage
   *        The output language. May not be <code>null</code>.
   * @return The syntax for that language. Never <code>null</code>.
   */
  @NonNull
  static IParserSyntax of (@NonNull final EOutputLanguage eLanguage)
  {
    switch (eLanguage)
    {
      case JAVA:
        return JavaParserSyntax.INSTANCE;
      case CPP:
        return CppParserSyntax.INSTANCE;
    }
    throw new UnsupportedOutputLanguageException (eLanguage);
  }
}
