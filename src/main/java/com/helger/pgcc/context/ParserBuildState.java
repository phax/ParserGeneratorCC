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

import com.helger.pgcc.parser.Token;

/**
 * The scratch state of reading one grammar file: which of the three compilation unit token lists
 * the parser is currently filling, where the current run of tokens started, and how many lexical
 * states have been handed out.
 * <p>
 * This is the instance state behind the static fields of
 * {@link com.helger.pgcc.parser.AbstractJavaCCParserInternals}. Keeping it here also removes an
 * aliasing trap: the target list used to be captured in a static field initializer, so it pointed
 * at the list of whichever run happened to load the class first.
 *
 * @author Philip Helger
 */
public final class ParserBuildState
{
  private final GrammarState m_aGrammar;

  private List <Token> m_aAddTokenHere;
  private Token m_aFirstToken;
  private boolean m_bInsertionPoint1Set;
  private boolean m_bInsertionPoint2Set;
  private int m_nNextFreeLexState = 1;

  ParserBuildState (@NonNull final GrammarState aGrammar)
  {
    m_aGrammar = aGrammar;
    m_aAddTokenHere = aGrammar.cuToInsertionPoint1 ();
  }

  /**
   * {@return the compilation unit token list that is currently being filled. Never
   *         <code>null</code>.}
   */
  @NonNull
  public List <Token> getAddTokenHere ()
  {
    return m_aAddTokenHere;
  }

  /** Continue with the tokens between the first and the second insertion point. */
  public void switchToInsertionPoint2 ()
  {
    m_aAddTokenHere = m_aGrammar.cuToInsertionPoint2 ();
  }

  /** Continue with the tokens after the second insertion point. */
  public void switchToAfterInsertionPoint2 ()
  {
    m_aAddTokenHere = m_aGrammar.cuFromInsertionPoint2 ();
  }

  @Nullable
  public Token getFirstToken ()
  {
    return m_aFirstToken;
  }

  public void setFirstToken (@Nullable final Token aToken)
  {
    m_aFirstToken = aToken;
  }

  public boolean isInsertionPoint1Set ()
  {
    return m_bInsertionPoint1Set;
  }

  public void setInsertionPoint1Set (final boolean b)
  {
    m_bInsertionPoint1Set = b;
  }

  public boolean isInsertionPoint2Set ()
  {
    return m_bInsertionPoint2Set;
  }

  public void setInsertionPoint2Set (final boolean b)
  {
    m_bInsertionPoint2Set = b;
  }

  /** {@return the next unused lexical state index, and reserves it} */
  public int getAndIncNextFreeLexState ()
  {
    return m_nNextFreeLexState++;
  }

}
