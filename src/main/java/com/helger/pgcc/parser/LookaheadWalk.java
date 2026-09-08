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

import java.util.ArrayList;
import java.util.List;

import com.helger.pgcc.context.LookaheadState;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpChoice;
import com.helger.pgcc.parser.exp.ExpLookahead;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.ExpOneOrMore;
import com.helger.pgcc.parser.exp.ExpSequence;
import com.helger.pgcc.parser.exp.ExpTryBlock;
import com.helger.pgcc.parser.exp.ExpZeroOrMore;
import com.helger.pgcc.parser.exp.ExpZeroOrOne;
import com.helger.pgcc.parser.exp.Expansion;

public final class LookaheadWalk
{
  private LookaheadWalk ()
  {}

  public static List <MatchInfo> genFirstSet (final List <MatchInfo> aPartialMatches, final Expansion aExp)
  {
    if (aExp instanceof final AbstractExpRegularExpression aRegularExpression)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      for (int i = 0; i < aPartialMatches.size (); i++)
      {
        final MatchInfo m = aPartialMatches.get (i);
        final MatchInfo aMnew = new MatchInfo ();
        for (int j = 0; j < m.m_firstFreeLoc; j++)
        {
          aMnew.m_match[j] = m.m_match[j];
        }
        aMnew.m_firstFreeLoc = m.m_firstFreeLoc;
        aMnew.m_match[aMnew.m_firstFreeLoc++] = aRegularExpression.getOrdinal ();
        if (aMnew.m_firstFreeLoc == LookaheadState.current ().getLimit ())
        {
          LookaheadState.current ().getSizeLimitedMatches ().add (aMnew);
        }
        else
        {
          aRetval.add (aMnew);
        }
      }
      return aRetval;
    }

    if (aExp instanceof final ExpNonTerminal aNonTerminal)
    {
      final NormalProduction aProd = aNonTerminal.getProd ();
      if (aProd instanceof AbstractCodeProduction)
      {
        return new ArrayList <> ();
      }
      return genFirstSet (aPartialMatches, aProd.getExpansion ());
    }

    if (aExp instanceof final ExpChoice ch)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      for (final Expansion element : ch.getChoices ())
      {
        final List <MatchInfo> v = genFirstSet (aPartialMatches, element);
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp instanceof final ExpSequence seq)
    {
      List <MatchInfo> v = aPartialMatches;
      for (final Expansion element : seq.getUnits ())
      {
        v = genFirstSet (v, element);
        if (v.size () == 0)
          break;
      }
      return v;
    }

    if (aExp instanceof final ExpOneOrMore om)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      List <MatchInfo> v = aPartialMatches;
      while (true)
      {
        v = genFirstSet (v, om.getExpansion ());
        if (v.size () == 0)
          break;
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp instanceof final ExpZeroOrMore zm)
    {
      final List <MatchInfo> aRetval = new ArrayList <> (aPartialMatches);
      List <MatchInfo> v = aPartialMatches;
      while (true)
      {
        v = genFirstSet (v, zm.getExpansion ());
        if (v.size () == 0)
          break;
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp instanceof final ExpZeroOrOne aZeroOrOne)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      aRetval.addAll (aPartialMatches);
      aRetval.addAll (genFirstSet (aPartialMatches, aZeroOrOne.getExpansion ()));
      return aRetval;
    }

    if (aExp instanceof final ExpTryBlock aTryBlock)
    {
      return genFirstSet (aPartialMatches, aTryBlock.m_exp);
    }

    if (LookaheadState.current ().isConsiderSemanticLA () &&
      aExp instanceof ExpLookahead &&
      ((ExpLookahead) aExp).getActionTokens ().isNotEmpty ())
    {
      return new ArrayList <> ();
    }

    final List <MatchInfo> aRetval = new ArrayList <> (aPartialMatches);
    return aRetval;
  }

  private static void _listSplit (final List <MatchInfo> toSplit,
                                  final List <MatchInfo> mask,
                                  final List <MatchInfo> partInMask,
                                  final List <MatchInfo> aRest)
  {
    OuterLoop: for (int i = 0; i < toSplit.size (); i++)
    {
      for (int j = 0; j < mask.size (); j++)
      {
        if (toSplit.get (i) == mask.get (j))
        {
          partInMask.add (toSplit.get (i));
          continue OuterLoop;
        }
      }
      aRest.add (toSplit.get (i));
    }
  }

  public static List <MatchInfo> genFollowSet (final List <MatchInfo> aPartialMatches,
                                               final Expansion aExp,
                                               final long nGeneration)
  {
    if (aExp.getMyGeneration () == nGeneration)
    {
      return new ArrayList <> ();
    }
    // System.out.println("*** Parent: " + exp.parent);
    aExp.setMyGeneration (nGeneration);
    if (aExp.getParent () == null)
    {
      final List <MatchInfo> aRetval = new ArrayList <> (aPartialMatches);
      return aRetval;
    }

    if (aExp.getParent () instanceof NormalProduction)
    {
      final List <Expansion> aParents = ((NormalProduction) aExp.getParent ()).getParents ();
      final List <MatchInfo> aRetval = new ArrayList <> ();
      // System.out.println("1; gen: " + generation + "; exp: " + exp);
      for (final Expansion parent : aParents)
      {
        final List <MatchInfo> v = genFollowSet (aPartialMatches, parent, nGeneration);
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp.getParent () instanceof ExpSequence)
    {
      final ExpSequence aSeq = (ExpSequence) aExp.getParent ();
      List <MatchInfo> v = aPartialMatches;
      for (int i = aExp.getOrdinalBase () + 1; i < aSeq.getUnitCount (); i++)
      {
        v = genFirstSet (v, aSeq.getUnitAt (i));
        if (v.isEmpty ())
          return v;
      }
      List <MatchInfo> aV1 = new ArrayList <> ();
      List <MatchInfo> aV2 = new ArrayList <> ();
      _listSplit (v, aPartialMatches, aV1, aV2);
      if (!aV1.isEmpty ())
      {
        // System.out.println("2; gen: " + generation + "; exp: " + exp);
        aV1 = genFollowSet (aV1, aSeq, nGeneration);
      }
      if (!aV2.isEmpty ())
      {
        // System.out.println("3; gen: " + generation + "; exp: " + exp);
        aV2 = genFollowSet (aV2, aSeq, Expansion.getNextGenerationIndex ());
      }
      aV2.addAll (aV1);
      return aV2;
    }

    if (aExp.getParent () instanceof ExpOneOrMore || aExp.getParent () instanceof ExpZeroOrMore)
    {
      final Expansion aParent = (Expansion) aExp.getParent ();
      final List <MatchInfo> aMoreMatches = new ArrayList <> (aPartialMatches);
      List <MatchInfo> v = aPartialMatches;
      while (true)
      {
        v = genFirstSet (v, aExp);
        if (v.size () == 0)
          break;
        aMoreMatches.addAll (v);
      }
      List <MatchInfo> aV1 = new ArrayList <> ();
      List <MatchInfo> aV2 = new ArrayList <> ();
      _listSplit (aMoreMatches, aPartialMatches, aV1, aV2);
      if (aV1.size () != 0)
      {
        // System.out.println("4; gen: " + generation + "; exp: " + exp);
        aV1 = genFollowSet (aV1, aParent, nGeneration);
      }
      if (aV2.size () != 0)
      {
        // System.out.println("5; gen: " + generation + "; exp: " + exp);
        aV2 = genFollowSet (aV2, aParent, Expansion.getNextGenerationIndex ());
      }
      aV2.addAll (aV1);
      return aV2;
    }

    // System.out.println("6; gen: " + generation + "; exp: " + exp);
    return genFollowSet (aPartialMatches, (Expansion) aExp.getParent (), nGeneration);
  }
}
