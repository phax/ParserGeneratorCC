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
package com.helger.pgcc.parser;

import org.jspecify.annotations.Nullable;

import com.helger.base.location.ILocation;
import com.helger.pgcc.context.PGCCContext;

/**
 * Something in the grammar that knows where it was written: a production, an expansion, a regular
 * expression, a character descriptor or a single token.
 * <p>
 * This exists so that {@link JavaCCErrors} can take a location instead of an {@link Object} and an
 * <code>instanceof</code> cascade over the six types that happen to have a line and a column.
 * <p>
 * It extends {@link ILocation} from ph-commons rather than starting from nothing, so that the
 * usual helpers - <code>getAsString ()</code>, <code>hasLineNumber ()</code> - come along. The two
 * abstract methods keep the names the grammar model has always used; the ph-commons spellings are
 * defaults on top of them.
 *
 * @author Philip Helger
 */
public interface IGrammarLocation extends ILocation
{
  /**
   * @return The line this was written on, or 0 if it is not known.
   */
  int getLine ();

  /**
   * @return The column this was written at, or 0 if it is not known.
   */
  int getColumn ();

  default int getLineNumber ()
  {
    return getLine ();
  }

  default int getColumnNumber ()
  {
    return getColumn ();
  }

  /**
   * @return The grammar file currently being read, because that is what every location in the
   *         model refers to. May be <code>null</code> before a file has been opened.
   */
  @Nullable
  default String getResourceID ()
  {
    return PGCCContext.current ().grammar ().getFileName ();
  }
}
