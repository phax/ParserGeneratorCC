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

import org.jspecify.annotations.NonNull;

import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpChoice;
import com.helger.pgcc.parser.exp.ExpLookahead;
import com.helger.pgcc.parser.exp.ExpOneOrMore;
import com.helger.pgcc.parser.exp.ExpRChoice;
import com.helger.pgcc.parser.exp.ExpROneOrMore;
import com.helger.pgcc.parser.exp.ExpRRepetitionRange;
import com.helger.pgcc.parser.exp.ExpRSequence;
import com.helger.pgcc.parser.exp.ExpRZeroOrMore;
import com.helger.pgcc.parser.exp.ExpRZeroOrOne;
import com.helger.pgcc.parser.exp.ExpSequence;
import com.helger.pgcc.parser.exp.ExpTryBlock;
import com.helger.pgcc.parser.exp.ExpZeroOrMore;
import com.helger.pgcc.parser.exp.ExpZeroOrOne;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * A set of routines that walk down the Expansion tree in various ways.
 */
public final class ExpansionTreeWalker
{
  private ExpansionTreeWalker ()
  {}

  /**
   * Visits the nodes of the tree rooted at "node" in pre-order. i.e., it executes opObj.action
   * first and then visits the children.
   */
  static void preOrderWalk (final Expansion aNode, @NonNull final ITreeWalkerOperation aOpObj)
  {
    aOpObj.action (aNode);
    if (aOpObj.goDeeper (aNode))
    {
      if (aNode instanceof final ExpChoice aChoice)
      {
        for (final Expansion aElement : aChoice.getChoices ())
          preOrderWalk (aElement, aOpObj);
      }
      else
        if (aNode instanceof final ExpSequence aSequence)
        {
          for (final Expansion aElement : aSequence.getUnits ())
            preOrderWalk (aElement, aOpObj);
        }
        else
          if (aNode instanceof final ExpOneOrMore aOneOrMore)
          {
            preOrderWalk (aOneOrMore.getExpansion (), aOpObj);
          }
          else
            if (aNode instanceof final ExpZeroOrMore aZeroOrMore)
            {
              preOrderWalk (aZeroOrMore.getExpansion (), aOpObj);
            }
            else
              if (aNode instanceof final ExpZeroOrOne aZeroOrOne)
              {
                preOrderWalk (aZeroOrOne.getExpansion (), aOpObj);
              }
              else
                if (aNode instanceof final ExpLookahead aLookahead)
                {
                  final Expansion aNested_e = aLookahead.getLaExpansion ();
                  if (!(aNested_e instanceof final ExpSequence aSeq && aSeq.getUnitAt (0) == aNode))
                    preOrderWalk (aNested_e, aOpObj);
                }
                else
                  if (aNode instanceof final ExpTryBlock aTryBlock)
                  {
                    preOrderWalk (aTryBlock.getExp (), aOpObj);
                  }
                  else
                    if (aNode instanceof final ExpRChoice aRChoice)
                    {
                      for (final AbstractExpRegularExpression aExpansion : aRChoice.getChoices ())
                        preOrderWalk (aExpansion, aOpObj);
                    }
                    else
                      if (aNode instanceof final ExpRSequence aRSequence)
                      {
                        for (final AbstractExpRegularExpression aElement : aRSequence.getUnits ())
                          preOrderWalk (aElement, aOpObj);
                      }
                      else
                        if (aNode instanceof final ExpROneOrMore aROneOrMore)
                        {
                          preOrderWalk (aROneOrMore.getRegExpr (), aOpObj);
                        }
                        else
                          if (aNode instanceof final ExpRZeroOrMore aRZeroOrMore)
                          {
                            preOrderWalk (aRZeroOrMore.getRegExpr (), aOpObj);
                          }
                          else
                            if (aNode instanceof final ExpRZeroOrOne aRZeroOrOne)
                            {
                              preOrderWalk (aRZeroOrOne.getRegExpr (), aOpObj);
                            }
                            else
                              if (aNode instanceof final ExpRRepetitionRange aRRepetitionRange)
                              {
                                preOrderWalk (aRRepetitionRange.getRegExpr (), aOpObj);
                              }
    }
  }

  /**
   * Visits the nodes of the tree rooted at "node" in post-order. i.e., it visits the children first
   * and then executes opObj.action.
   */
  static void postOrderWalk (final Expansion aNode, @NonNull final ITreeWalkerOperation aOpObj)
  {
    if (aOpObj.goDeeper (aNode))
    {
      if (aNode instanceof final ExpChoice aExpChoice)
      {
        for (final Expansion aElement : aExpChoice.getChoices ())
          postOrderWalk (aElement, aOpObj);
      }
      else
        if (aNode instanceof final ExpSequence aExpSequence)
        {
          for (final Expansion aElement : aExpSequence.getUnits ())
            postOrderWalk (aElement, aOpObj);
        }
        else
          if (aNode instanceof final ExpOneOrMore aExpOneOrMore)
          {
            postOrderWalk (aExpOneOrMore.getExpansion (), aOpObj);
          }
          else
            if (aNode instanceof final ExpZeroOrMore aExpZeroOrMore)
            {
              postOrderWalk (aExpZeroOrMore.getExpansion (), aOpObj);
            }
            else
              if (aNode instanceof final ExpZeroOrOne aExpZeroOrOne)
              {
                postOrderWalk (aExpZeroOrOne.getExpansion (), aOpObj);
              }
              else
                if (aNode instanceof final ExpLookahead aExpLookahead)
                {
                  final Expansion aNested_e = aExpLookahead.getLaExpansion ();
                  if (!(aNested_e instanceof final ExpSequence aSeq && aSeq.getUnitAt (0) == aNode))
                    postOrderWalk (aNested_e, aOpObj);
                }
                else
                  if (aNode instanceof final ExpTryBlock aExpTryBlock)
                  {
                    postOrderWalk (aExpTryBlock.getExp (), aOpObj);
                  }
                  else
                    if (aNode instanceof final ExpRChoice aExpRChoice)
                    {
                      for (final AbstractExpRegularExpression aElement : aExpRChoice.getChoices ())
                        postOrderWalk (aElement, aOpObj);
                    }
                    else
                      if (aNode instanceof final ExpRSequence aExpRSequence)
                      {
                        for (final AbstractExpRegularExpression aElement : aExpRSequence.getUnits ())
                          postOrderWalk (aElement, aOpObj);
                      }
                      else
                        if (aNode instanceof final ExpROneOrMore aExpROneOrMore)
                        {
                          postOrderWalk (aExpROneOrMore.getRegExpr (), aOpObj);
                        }
                        else
                          if (aNode instanceof final ExpRZeroOrMore aExpRZeroOrMore)
                          {
                            postOrderWalk (aExpRZeroOrMore.getRegExpr (), aOpObj);
                          }
                          else
                            if (aNode instanceof final ExpRZeroOrOne aExpRZeroOrOne)
                            {
                              postOrderWalk (aExpRZeroOrOne.getRegExpr (), aOpObj);
                            }
                            else
                              if (aNode instanceof final ExpRRepetitionRange aExpRRepetitionRange)
                              {
                                postOrderWalk (aExpRRepetitionRange.getRegExpr (), aOpObj);
                              }
    }
    aOpObj.action (aNode);
  }

}
