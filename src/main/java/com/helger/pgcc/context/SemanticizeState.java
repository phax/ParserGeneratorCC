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
package com.helger.pgcc.context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The scratch state of the semantic checks of a single generator run: the deferred removals that
 * cannot be applied while a list is being iterated, and the path that the recursion detection has
 * walked so far.
 * <p>
 * This is the instance state behind the static fields of
 * {@link com.helger.pgcc.parser.Semanticize}.
 *
 * @author Philip Helger
 */
public final class SemanticizeState
{
  /** Default constructor. */
  public SemanticizeState ()
  {}

  private final List <List <?>> m_aRemoveFrom = new ArrayList <> ();
  private final List <Object> m_aRemoveWhat = new ArrayList <> ();
  private String m_sLoopString;

  /**
   * Remember that an item has to be removed from a list once iterating it is finished.
   *
   * @param aList
   *        The list to remove from. May not be <code>null</code>.
   * @param aItem
   *        The item to remove. May not be <code>null</code>.
   */
  public void prepareToRemove (@NonNull final List <?> aList, @NonNull final Object aItem)
  {
    m_aRemoveFrom.add (aList);
    m_aRemoveWhat.add (aItem);
  }

  /** Apply and forget everything that {@link #prepareToRemove(List, Object)} collected. */
  public void removePreparedItems ()
  {
    for (int i = 0; i < m_aRemoveFrom.size (); i++)
      m_aRemoveFrom.get (i).remove (m_aRemoveWhat.get (i));
    m_aRemoveFrom.clear ();
    m_aRemoveWhat.clear ();
  }

  /** {@return the recursion path found so far, for the error message} */
  @Nullable
  public String getLoopString ()
  {
    return m_sLoopString;
  }

  public void setLoopString (@Nullable final String sLoopString)
  {
    m_sLoopString = sLoopString;
  }
}
