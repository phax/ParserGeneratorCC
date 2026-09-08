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

import org.jspecify.annotations.NonNull;

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

/**
 * The FIRST and FOLLOW set computation that the LL(k) decisions are made from.
 * <p>
 * A {@link MatchInfo} is one possible sequence of token kinds, up to
 * {@link LookaheadState#getLimit()} of them. Both methods take the sequences matched so far and
 * return them extended by everything the expansion could match next, so a caller starting from one
 * empty sequence ends up with every token sequence of that length the expansion admits. Two
 * expansions are ambiguous at length k exactly when their sets share an entry, which is what
 * {@link LookaheadCalc} looks for.
 * <p>
 * The walk stops early in two ways. Reaching the limit records the sequence in
 * {@link LookaheadState#getSizeLimitedMatches()} rather than growing it, so the ambiguity report
 * can show what it found; and a cyclic grammar is cut off by the generation stamp described on
 * {@link #genFollowSetRecursive(List, Expansion, long)}.
 *
 * @author Philip Helger
 */
public final class LookaheadWalk
{
  private LookaheadWalk ()
  {}

  /**
   * Extend every sequence matched so far by what this expansion can match next: the FIRST set.
   * <p>
   * Recursive over the expansion tree, and through {@link ExpNonTerminal} into the productions it
   * calls, so a left recursive grammar would not terminate here - {@code Semanticize} rejects those
   * before this runs.
   *
   * @param aPartialMatches
   *        The sequences to extend. May not be <code>null</code>.
   * @param aExp
   *        The expansion to walk. May not be <code>null</code>.
   * @return The extended sequences. Never <code>null</code>, and empty when the expansion cannot
   *         match anything here.
   */
  public static List <MatchInfo> genFirstSetRecursive (@NonNull final List <MatchInfo> aPartialMatches,
                                                       final Expansion aExp)
  {
    if (aExp instanceof final AbstractExpRegularExpression aRegularExpression)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      for (int i = 0; i < aPartialMatches.size (); i++)
      {
        final MatchInfo m = aPartialMatches.get (i);
        final MatchInfo aMnew = new MatchInfo ();
        for (int j = 0; j < m.getFirstFreeLoc (); j++)
        {
          aMnew.getMatch ()[j] = m.getMatch ()[j];
        }
        aMnew.setFirstFreeLoc (m.getFirstFreeLoc ());
        aMnew.getMatch ()[aMnew.getFirstFreeLoc ()] = aRegularExpression.getOrdinal ();
        aMnew.setFirstFreeLoc (aMnew.getFirstFreeLoc () + 1);
        if (aMnew.getFirstFreeLoc () == LookaheadState.current ().getLimit ())
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
      final AbstractNormalProduction aProd = aNonTerminal.getProd ();
      // JAVACODE and CPPCODE productions have no expansion to walk into, so nothing
      // can be said about what they match
      if (aProd instanceof AbstractCodeProduction)
      {
        return new ArrayList <> ();
      }
      return genFirstSetRecursive (aPartialMatches, aProd.getExpansion ());
    }

    if (aExp instanceof final ExpChoice aChoice)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      for (final Expansion element : aChoice.getChoices ())
      {
        final List <MatchInfo> v = genFirstSetRecursive (aPartialMatches, element);
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp instanceof final ExpSequence aSequence)
    {
      List <MatchInfo> v = aPartialMatches;
      for (final Expansion element : aSequence.getUnits ())
      {
        v = genFirstSetRecursive (v, element);
        if (v.isEmpty ())
          break;
      }
      return v;
    }

    if (aExp instanceof final ExpOneOrMore aOneOrMore)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      List <MatchInfo> v = aPartialMatches;
      while (true)
      {
        v = genFirstSetRecursive (v, aOneOrMore.getExpansion ());
        if (v.isEmpty ())
          break;
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp instanceof final ExpZeroOrMore aZeroOrMore)
    {
      final List <MatchInfo> aRetval = new ArrayList <> (aPartialMatches);
      List <MatchInfo> v = aPartialMatches;
      while (true)
      {
        v = genFirstSetRecursive (v, aZeroOrMore.getExpansion ());
        if (v.isEmpty ())
          break;
        aRetval.addAll (v);
      }
      return aRetval;
    }

    if (aExp instanceof final ExpZeroOrOne aZeroOrOne)
    {
      final List <MatchInfo> aRetval = new ArrayList <> ();
      aRetval.addAll (aPartialMatches);
      aRetval.addAll (genFirstSetRecursive (aPartialMatches, aZeroOrOne.getExpansion ()));
      return aRetval;
    }

    if (aExp instanceof final ExpTryBlock aTryBlock)
    {
      return genFirstSetRecursive (aPartialMatches, aTryBlock.getExp ());
    }

    // A semantic lookahead can reject anything, so when it is being taken into account nothing
    // downstream of it is guaranteed to be reachable
    if (LookaheadState.current ().isConsiderSemanticLA () &&
      aExp instanceof final ExpLookahead aLookahead &&
      aLookahead.getActionTokens ().isNotEmpty ())
    {
      return new ArrayList <> ();
    }

    final List <MatchInfo> aRetval = new ArrayList <> (aPartialMatches);
    return aRetval;
  }

  private static void _listSplit (@NonNull final List <MatchInfo> toSplit,
                                  @NonNull final List <MatchInfo> mask,
                                  @NonNull final List <MatchInfo> partInMask,
                                  @NonNull final List <MatchInfo> aRest)
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

  /**
   * Extend every sequence matched so far by what can follow this expansion: the FOLLOW set.
   * <p>
   * Recursive upwards rather than downwards - it asks what surrounds the expansion, then what
   * surrounds that. A grammar can be cyclic, so each expansion is stamped with the generation it
   * was last visited in and a second visit within the same generation returns nothing. The caller
   * gets a fresh generation from {@link Expansion#getNextGenerationIndex()}; the sequence branch
   * below deliberately takes another one, so that the two halves of a split are followed
   * independently rather than the second being cut off by the first.
   *
   * @param aPartialMatches
   *        The sequences to extend. May not be <code>null</code>.
   * @param aExp
   *        The expansion whose surroundings to walk. May not be <code>null</code>.
   * @param nGeneration
   *        The visit stamp for this walk.
   * @return The extended sequences. Never <code>null</code>.
   */
  public static List <MatchInfo> genFollowSetRecursive (final List <MatchInfo> aPartialMatches,
                                                        @NonNull final Expansion aExp,
                                                        final long nGeneration)
  {
    if (aExp.getMyGeneration () == nGeneration)
    {
      return new ArrayList <> ();
    }
    aExp.setMyGeneration (nGeneration);
    if (aExp.getParent () == null)
    {
      final List <MatchInfo> aRetval = new ArrayList <> (aPartialMatches);
      return aRetval;
    }

    // At the top of a production, what follows is whatever follows any of its call sites
    if (aExp.getParent () instanceof final AbstractNormalProduction aProduction)
    {
      final List <Expansion> aParents = aProduction.getParents ();
      final List <MatchInfo> aRetval = new ArrayList <> ();
      for (final Expansion parent : aParents)
      {
        final List <MatchInfo> v = genFollowSetRecursive (aPartialMatches, parent, nGeneration);
        aRetval.addAll (v);
      }
      return aRetval;
    }

    // Inside a sequence, what follows is the rest of the sequence, and then whatever follows the
    // sequence itself if the rest can match nothing
    if (aExp.getParent () instanceof final ExpSequence aSeq)
    {
      List <MatchInfo> v = aPartialMatches;
      for (int i = aExp.getOrdinalBase () + 1; i < aSeq.getUnitCount (); i++)
      {
        v = genFirstSetRecursive (v, aSeq.getUnitAt (i));
        if (v.isEmpty ())
          return v;
      }
      List <MatchInfo> aV1 = new ArrayList <> ();
      List <MatchInfo> aV2 = new ArrayList <> ();
      _listSplit (v, aPartialMatches, aV1, aV2);
      if (!aV1.isEmpty ())
      {
        aV1 = genFollowSetRecursive (aV1, aSeq, nGeneration);
      }
      if (!aV2.isEmpty ())
      {
        aV2 = genFollowSetRecursive (aV2, aSeq, Expansion.getNextGenerationIndex ());
      }
      aV2.addAll (aV1);
      return aV2;
    }

    // Inside a loop, the body can follow itself, so keep extending until nothing new comes back.
    // No pattern variable here: one binding cannot span the two loop types
    if (aExp.getParent () instanceof ExpOneOrMore || aExp.getParent () instanceof ExpZeroOrMore)
    {
      final Expansion aParent = (Expansion) aExp.getParent ();
      final List <MatchInfo> aMoreMatches = new ArrayList <> (aPartialMatches);
      List <MatchInfo> v = aPartialMatches;
      while (true)
      {
        v = genFirstSetRecursive (v, aExp);
        if (v.isEmpty ())
          break;
        aMoreMatches.addAll (v);
      }
      List <MatchInfo> aV1 = new ArrayList <> ();
      List <MatchInfo> aV2 = new ArrayList <> ();
      _listSplit (aMoreMatches, aPartialMatches, aV1, aV2);
      if (!aV1.isEmpty ())
      {
        aV1 = genFollowSetRecursive (aV1, aParent, nGeneration);
      }
      if (!aV2.isEmpty ())
      {
        aV2 = genFollowSetRecursive (aV2, aParent, Expansion.getNextGenerationIndex ());
      }
      aV2.addAll (aV1);
      return aV2;
    }

    return genFollowSetRecursive (aPartialMatches, (Expansion) aExp.getParent (), nGeneration);
  }
}
