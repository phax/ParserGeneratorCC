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

import org.jspecify.annotations.NonNull;

/**
 * The state of one generator run.
 * <p>
 * Historically the generator kept everything in static fields, which made two runs in the same JVM
 * depend on each other and ruled out running two of them at once. This class is where that state is
 * moving to, one piece at a time: whatever has already been migrated lives here, the old static
 * classes delegate to {@link #current()}, and the static shims disappear once nothing references
 * them any more.
 * <p>
 * The context is per thread, so two threads can generate independently. Within a thread,
 * {@link #reset()} starts a fresh run - that is what <code>Main.reInitAll</code> does.
 * <p>
 * <code>StateIsolationTest</code> is the guard rail for this migration: it generates a grammar,
 * generates something else, generates the first grammar again, and requires the results to be
 * identical.
 *
 * @author Philip Helger
 */
public final class PGCCContext
{
  private static final ThreadLocal <PGCCContext> CURRENT = ThreadLocal.withInitial (PGCCContext::new);

  private final ErrorCollector m_aErrors = new ErrorCollector ();
  private final OptionState m_aOptions = new OptionState ();
  private final LookaheadState m_aLookahead = new LookaheadState ();
  private final GrammarState m_aGrammar = new GrammarState ();
  private final ParserBuildState m_aParserBuild = new ParserBuildState (m_aGrammar);
  private final SemanticizeState m_aSemanticize = new SemanticizeState ();

  private PGCCContext ()
  {}

  /**
   * @return The context of the current thread, creating it on first access. Never
   *         <code>null</code>.
   */
  @NonNull
  public static PGCCContext current ()
  {
    return CURRENT.get ();
  }

  /**
   * Start a fresh run on the current thread. Everything that has been migrated into the context is
   * reset by this single call.
   */
  public static void reset ()
  {
    CURRENT.remove ();
  }

  @NonNull
  public ErrorCollector errors ()
  {
    return m_aErrors;
  }

  @NonNull
  public OptionState options ()
  {
    return m_aOptions;
  }

  @NonNull
  public LookaheadState lookahead ()
  {
    return m_aLookahead;
  }

  @NonNull
  public GrammarState grammar ()
  {
    return m_aGrammar;
  }

  @NonNull
  public ParserBuildState parserBuild ()
  {
    return m_aParserBuild;
  }

  @NonNull
  public SemanticizeState semanticize ()
  {
    return m_aSemanticize;
  }
}
