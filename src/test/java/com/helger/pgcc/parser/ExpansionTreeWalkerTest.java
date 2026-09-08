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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.helger.pgcc.parser.exp.ExpAction;
import com.helger.pgcc.parser.exp.ExpChoice;
import com.helger.pgcc.parser.exp.ExpLookahead;
import com.helger.pgcc.parser.exp.ExpSequence;
import com.helger.pgcc.parser.exp.ExpZeroOrMore;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * Test class for {@link ExpansionTreeWalker}, the traversal that {@link Semanticize},
 * {@link ParseEngine} and JJDoc all run their checks on top of.
 * <p>
 * The walker is one long <code>instanceof</code> cascade over the expansion hierarchy, so a branch
 * that stops descending is invisible - the affected check simply never sees part of the grammar and
 * silently reports nothing. Only the golden files covered it before, and those cannot say which
 * branch went missing.
 *
 * @author Philip Helger
 */
public final class ExpansionTreeWalkerTest
{
  /** Names the nodes by identity, so that a visit order can be asserted as readable text. */
  private final Map <Expansion, String> m_aNames = new IdentityHashMap <> ();

  /** Records what it is asked to visit, and optionally refuses to descend into one node. */
  private final class RecordingOperation implements ITreeWalkerOperation
  {
    private final List <String> m_aVisited = new ArrayList <> ();
    private final Expansion m_aDoNotEnter;

    RecordingOperation (final Expansion aDoNotEnter)
    {
      m_aDoNotEnter = aDoNotEnter;
    }

    public boolean goDeeper (final Expansion e)
    {
      return e != m_aDoNotEnter;
    }

    public void action (final Expansion e)
    {
      m_aVisited.add (m_aNames.getOrDefault (e, "?"));
    }

    String getVisited ()
    {
      return String.join (" ", m_aVisited);
    }
  }

  private <T extends Expansion> T _named (final String sName, final T aExp)
  {
    m_aNames.put (aExp, sName);
    return aExp;
  }

  /**
   * <code>choice( sequence( (leafX)*, leafY ), leafZ )</code> - one node of every shape that has
   * children, plus three leaves to prove the descent actually arrives.
   *
   * @return The root. Never <code>null</code>.
   */
  private ExpChoice _buildTree ()
  {
    final Expansion aLeafX = _named ("X", new ExpAction ());
    final Expansion aLeafY = _named ("Y", new ExpAction ());
    final Expansion aLeafZ = _named ("Z", new ExpAction ());

    final ExpZeroOrMore aStar = _named ("star", new ExpZeroOrMore (new Token (), aLeafX));
    final ExpSequence aSeq = _named ("seq", new ExpSequence ());
    aSeq.addUnit (aStar);
    aSeq.addUnit (aLeafY);

    final ExpChoice aChoice = _named ("choice", new ExpChoice ());
    aChoice.addChoice (aSeq);
    aChoice.addChoice (aLeafZ);
    return aChoice;
  }

  @Test
  public void testPreOrderVisitsTheParentBeforeItsChildren ()
  {
    final RecordingOperation aOp = new RecordingOperation (null);
    ExpansionTreeWalker.preOrderWalk (_buildTree (), aOp);
    assertEquals ("choice seq star X Y Z", aOp.getVisited ());
  }

  @Test
  public void testPostOrderVisitsTheChildrenBeforeTheParent ()
  {
    final RecordingOperation aOp = new RecordingOperation (null);
    ExpansionTreeWalker.postOrderWalk (_buildTree (), aOp);
    assertEquals ("X star Y seq Z choice", aOp.getVisited ());
  }

  @Test
  public void testGoDeeperFalsePrunesTheChildrenButNotTheNode ()
  {
    final ExpChoice aRoot = _buildTree ();
    // Refuse to enter the sequence: it is still visited, its subtree is not
    Expansion aSeq = null;
    for (final Expansion aChild : aRoot.getChoices ())
      if ("seq".equals (m_aNames.get (aChild)))
        aSeq = aChild;

    final RecordingOperation aOp = new RecordingOperation (aSeq);
    ExpansionTreeWalker.preOrderWalk (aRoot, aOp);
    assertEquals ("choice seq Z", aOp.getVisited ());
  }

  @Test
  public void testWalksIntoTheExpansionOfALookahead ()
  {
    final Expansion aLeaf = _named ("X", new ExpAction ());
    final ExpSequence aSeq = _named ("seq", new ExpSequence ());
    aSeq.addUnit (aLeaf);

    final ExpLookahead aLA = _named ("la", new ExpLookahead ());
    aLA.setLaExpansion (aSeq);

    final RecordingOperation aOp = new RecordingOperation (null);
    ExpansionTreeWalker.preOrderWalk (aLA, aOp);
    assertEquals ("la seq X", aOp.getVisited ());
  }

  @Test
  public void testDoesNotFollowALookaheadBackIntoItself ()
  {
    // A syntactic lookahead sits at the head of the sequence it describes, so its expansion
    // points back at the sequence that contains it. Descending would not terminate, and the
    // walker guards against exactly this shape
    final ExpLookahead aLA = _named ("la", new ExpLookahead ());
    final ExpSequence aSeq = _named ("seq", new ExpSequence ());
    aSeq.addUnit (aLA);
    aSeq.addUnit (_named ("X", new ExpAction ()));
    aLA.setLaExpansion (aSeq);

    final RecordingOperation aOp = new RecordingOperation (null);
    ExpansionTreeWalker.preOrderWalk (aLA, aOp);
    assertEquals ("la", aOp.getVisited ());
  }
}
