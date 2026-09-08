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

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.pgcc.parser.MatchInfo;

/**
 * The scratch state of the lookahead analysis of a single generator run: how many tokens deep the
 * current lookahead computation goes, whether semantic lookahead is taken into account, and the
 * matches that hit the depth limit.
 * <p>
 * This is the instance state behind the static fields of {@link com.helger.pgcc.parser.MatchInfo}
 * and {@link com.helger.pgcc.parser.LookaheadWalk}.
 *
 * @author Philip Helger
 */
public final class LookaheadState
{
  private int m_nLimit;
  private boolean m_bConsiderSemanticLA;
  private List <MatchInfo> m_aSizeLimitedMatches;

  /**
   * @return How many tokens deep the current lookahead computation goes. 0 if no computation is
   *         running.
   */
  public int getLimit ()
  {
    return m_nLimit;
  }

  public void setLimit (final int nLimit)
  {
    m_nLimit = nLimit;
  }

  public boolean isConsiderSemanticLA ()
  {
    return m_bConsiderSemanticLA;
  }

  public void setConsiderSemanticLA (final boolean bConsiderSemanticLA)
  {
    m_bConsiderSemanticLA = bConsiderSemanticLA;
  }

  /**
   * @return The matches that reached the lookahead limit. <code>null</code> if they are not being
   *         collected.
   */
  @Nullable
  public List <MatchInfo> getSizeLimitedMatches ()
  {
    return m_aSizeLimitedMatches;
  }

  public void setSizeLimitedMatches (@Nullable final List <MatchInfo> aMatches)
  {
    m_aSizeLimitedMatches = aMatches;
  }

  public void reset ()
  {
    m_nLimit = 0;
    m_bConsiderSemanticLA = false;
    m_aSizeLimitedMatches = null;
  }

  @NonNull
  public static LookaheadState current ()
  {
    return PGCCContext.current ().lookahead ();
  }
}
