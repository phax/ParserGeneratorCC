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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

public class ASTProduction extends JJTreeNode
{
  private final Map <NodeScope, Integer> m_aScopes = new HashMap <> ();
  private int m_nNextNodeScopeNumber = 0;
  private String m_sName;

  /**
   * @return The value of m_sName.
   */
  public String getName ()
  {
    return m_sName;
  }

  /**
   * @param aValue
   *        The new value of m_sName.
   */
  public void setName (final String aValue)
  {
    m_sName = aValue;
  }
  private List <String> m_aThrowsList = new ArrayList <> ();

  /**
   * @return The value of m_aThrowsList.
   */
  public List <String> getThrowsList ()
  {
    return m_aThrowsList;
  }

  /**
   * @param aValue
   *        The new value of m_aThrowsList.
   */
  public void setThrowsList (final List <String> aValue)
  {
    m_aThrowsList = aValue;
  }
  ASTProduction (final int nID)
  {
    super (nID);
  }

  int getNodeScopeNumber (@NonNull final NodeScope s)
  {
    final Integer aRet = m_aScopes.computeIfAbsent (s, k -> Integer.valueOf (m_nNextNodeScopeNumber++));
    return aRet.intValue ();
  }

  /** Accept the visitor. **/
  @Override
  public Object jjtAccept (@NonNull final JJTreeParserVisitor aVisitor, final Object aData)
  {
    return aVisitor.visit (this, aData);
  }
}
