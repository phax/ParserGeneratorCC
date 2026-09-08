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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.pgcc.jjtree.ASTProduction;
import com.helger.pgcc.jjtree.Token;

/**
 * The state of a single JJTree run: the parser it is decorating, the packages the parser and the
 * node classes live in, and the pieces of the parser class declaration that have to be reproduced.
 * <p>
 * This is the instance state behind the static {@link com.helger.pgcc.jjtree.JJTreeGlobals} facade.
 *
 * @author Philip Helger
 */
public final class JJTreeState
{
  private final Map <String, ASTProduction> m_aProductions = new HashMap <> ();

  /*
   * What the node file generators have already written, and what they still have to write. These
   * were static in NodeFilesJava and NodeFilesCpp and were never cleared, so a second JJTree run in
   * the same JVM emitted the node classes of the first grammar as well: running Alpha and then Beta
   * produced ASTAlfa in Beta's output directory, and a reference to it in BetaTree.h.
   */
  /*
   * The node descriptors seen so far, in declaration order, and the tools that produced the file
   * being read. Both were static collections outside the context - in ASTNodeDescriptor and
   * JJTreeGlobals - with hand written clear () calls to undo the previous run.
   */
  private final List <String> m_aNodeIds = new ArrayList <> ();
  private final List <String> m_aNodeNames = new ArrayList <> ();
  private final Map <String, String> m_aNodeSeen = new HashMap <> ();
  private final List <String> m_aToolList = new ArrayList <> ();

  private final Set <String> m_aNodesGenerated = new HashSet <> ();
  private final Set <String> m_aNodesToGenerate = new HashSet <> ();
  private final List <String> m_aHeadersForJJTreeH = new ArrayList <> ();

  private String m_sParserName;
  private String m_sPackageName = "";
  private String m_sNodePackageName = "";
  private Token m_aParserImplements;
  private Token m_aParserClassBodyStart;
  private Token m_aParserImports;

  /**
   * @return The node ids, in the order the descriptors were seen. Never <code>null</code>.
   */
  @NonNull
  public List <String> nodeIds ()
  {
    return m_aNodeIds;
  }

  /**
   * @return The node names, in the same order as {@link #nodeIds()}. Never <code>null</code>.
   */
  @NonNull
  public List <String> nodeNames ()
  {
    return m_aNodeNames;
  }

  /**
   * @return The node ids already recorded, so that a repeated descriptor is only counted once.
   *         Never <code>null</code>.
   */
  @NonNull
  public Map <String, String> nodeSeen ()
  {
    return m_aNodeSeen;
  }

  /**
   * @return The tools that produced the file being read, JJTree included. Never <code>null</code>.
   */
  @NonNull
  public List <String> toolList ()
  {
    return m_aToolList;
  }

  /**
   * @return The names of the node files already written in this run. Never <code>null</code>.
   */
  @NonNull
  public Set <String> nodesGenerated ()
  {
    return m_aNodesGenerated;
  }

  /**
   * @return The node types the C++ backend still has to emit. Never <code>null</code>.
   */
  @NonNull
  public Set <String> nodesToGenerate ()
  {
    return m_aNodesToGenerate;
  }

  /**
   * @return The header file names to be included from the generated JJTree header. Never
   *         <code>null</code>.
   */
  @NonNull
  public List <String> headersForJJTreeH ()
  {
    return m_aHeadersForJJTreeH;
  }

  /** @return Production name to the production that declares it */
  @NonNull
  public Map <String, ASTProduction> productions ()
  {
    return m_aProductions;
  }

  @Nullable
  public String getParserName ()
  {
    return m_sParserName;
  }

  public void setParserName (@Nullable final String sParserName)
  {
    m_sParserName = sParserName;
  }

  /** @return The package of the generated parser. Never <code>null</code>, but maybe empty. */
  @NonNull
  public String getPackageName ()
  {
    return m_sPackageName;
  }

  public void setPackageName (@NonNull final String sPackageName)
  {
    m_sPackageName = sPackageName;
  }

  /**
   * @return The package of the generated node classes, which is <code>NODE_PACKAGE</code> if it was
   *         set and the parser's package otherwise. Never <code>null</code>, but maybe empty.
   */
  @NonNull
  public String getNodePackageName ()
  {
    return m_sNodePackageName;
  }

  public void setNodePackageName (@NonNull final String sNodePackageName)
  {
    m_sNodePackageName = sNodePackageName;
  }

  /** @return The token at which the parser class' implements clause starts */
  @Nullable
  public Token getParserImplements ()
  {
    return m_aParserImplements;
  }

  public void setParserImplements (@Nullable final Token aToken)
  {
    m_aParserImplements = aToken;
  }

  /** @return The token at which the parser class body starts */
  @Nullable
  public Token getParserClassBodyStart ()
  {
    return m_aParserClassBodyStart;
  }

  public void setParserClassBodyStart (@Nullable final Token aToken)
  {
    m_aParserClassBodyStart = aToken;
  }

  /** @return The token at which the parser's import declarations start */
  @Nullable
  public Token getParserImports ()
  {
    return m_aParserImports;
  }

  public void setParserImports (@Nullable final Token aToken)
  {
    m_aParserImports = aToken;
  }

  /** Start a fresh JJTree run without touching the rest of the context. */
  public void reset ()
  {
    m_aProductions.clear ();
    m_aNodeIds.clear ();
    m_aNodeNames.clear ();
    m_aNodeSeen.clear ();
    m_aToolList.clear ();
    m_aNodesGenerated.clear ();
    m_aNodesToGenerate.clear ();
    m_aHeadersForJJTreeH.clear ();
    m_sParserName = null;
    m_sPackageName = "";
    m_sNodePackageName = "";
    m_aParserImplements = null;
    m_aParserClassBodyStart = null;
    m_aParserImports = null;
  }
}
