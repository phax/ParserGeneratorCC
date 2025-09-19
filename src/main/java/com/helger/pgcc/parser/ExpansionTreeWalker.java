/*
 * Copyright 2017-2025 Philip Helger, pgcc@helger.com
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
   * Visits the nodes of the tree rooted at "node" in pre-order. i.e., it
   * executes opObj.action first and then visits the children.
   */
  static void preOrderWalk (final Expansion node, final ITreeWalkerOperation opObj)
  {
    opObj.action (node);
    if (opObj.goDeeper (node))
    {
      if (node instanceof ExpChoice)
      {
        for (final Expansion aElement : ((ExpChoice) node).getChoices ())
          preOrderWalk (aElement, opObj);
      }
      else
        if (node instanceof ExpSequence)
        {
          for (final Expansion aElement : ((ExpSequence) node).getUnits ())
            preOrderWalk (aElement, opObj);
        }
        else
          if (node instanceof ExpOneOrMore)
          {
            preOrderWalk (((ExpOneOrMore) node).getExpansion (), opObj);
          }
          else
            if (node instanceof ExpZeroOrMore)
            {
              preOrderWalk (((ExpZeroOrMore) node).getExpansion (), opObj);
            }
            else
              if (node instanceof ExpZeroOrOne)
              {
                preOrderWalk (((ExpZeroOrOne) node).getExpansion (), opObj);
              }
              else
                if (node instanceof ExpLookahead)
                {
                  final Expansion nested_e = ((ExpLookahead) node).getLaExpansion ();
                  if (!(nested_e instanceof ExpSequence && (((ExpSequence) nested_e).getUnitAt (0)) == node))
                    preOrderWalk (nested_e, opObj);
                }
                else
                  if (node instanceof ExpTryBlock)
                  {
                    preOrderWalk (((ExpTryBlock) node).m_exp, opObj);
                  }
                  else
                    if (node instanceof ExpRChoice)
                    {
                      for (final AbstractExpRegularExpression aExpansion : ((ExpRChoice) node).getChoices ())
                        preOrderWalk (aExpansion, opObj);
                    }
                    else
                      if (node instanceof ExpRSequence)
                      {
                        for (final AbstractExpRegularExpression aElement : ((ExpRSequence) node).getUnits ())
                          preOrderWalk (aElement, opObj);
                      }
                      else
                        if (node instanceof ExpROneOrMore)
                        {
                          preOrderWalk (((ExpROneOrMore) node).getRegExpr (), opObj);
                        }
                        else
                          if (node instanceof ExpRZeroOrMore)
                          {
                            preOrderWalk (((ExpRZeroOrMore) node).getRegExpr (), opObj);
                          }
                          else
                            if (node instanceof ExpRZeroOrOne)
                            {
                              preOrderWalk (((ExpRZeroOrOne) node).getRegExpr (), opObj);
                            }
                            else
                              if (node instanceof ExpRRepetitionRange)
                              {
                                preOrderWalk (((ExpRRepetitionRange) node).getRegExpr (), opObj);
                              }
    }
  }

  /**
   * Visits the nodes of the tree rooted at "node" in post-order. i.e., it
   * visits the children first and then executes opObj.action.
   */
  static void postOrderWalk (final Expansion node, final ITreeWalkerOperation opObj)
  {
    if (opObj.goDeeper (node))
    {
      if (node instanceof ExpChoice)
      {
        for (final Expansion aElement : ((ExpChoice) node).getChoices ())
          postOrderWalk (aElement, opObj);
      }
      else
        if (node instanceof ExpSequence)
        {
          for (final Expansion aElement : ((ExpSequence) node).getUnits ())
            postOrderWalk (aElement, opObj);
        }
        else
          if (node instanceof ExpOneOrMore)
          {
            postOrderWalk (((ExpOneOrMore) node).getExpansion (), opObj);
          }
          else
            if (node instanceof ExpZeroOrMore)
            {
              postOrderWalk (((ExpZeroOrMore) node).getExpansion (), opObj);
            }
            else
              if (node instanceof ExpZeroOrOne)
              {
                postOrderWalk (((ExpZeroOrOne) node).getExpansion (), opObj);
              }
              else
                if (node instanceof ExpLookahead)
                {
                  final Expansion nested_e = ((ExpLookahead) node).getLaExpansion ();
                  if (!(nested_e instanceof ExpSequence && (((ExpSequence) nested_e).getUnitAt (0)) == node))
                    postOrderWalk (nested_e, opObj);
                }
                else
                  if (node instanceof ExpTryBlock)
                  {
                    postOrderWalk (((ExpTryBlock) node).m_exp, opObj);
                  }
                  else
                    if (node instanceof ExpRChoice)
                    {
                      for (final AbstractExpRegularExpression aElement : ((ExpRChoice) node).getChoices ())
                        postOrderWalk (aElement, opObj);
                    }
                    else
                      if (node instanceof ExpRSequence)
                      {
                        for (final AbstractExpRegularExpression aElement : ((ExpRSequence) node).getUnits ())
                          postOrderWalk (aElement, opObj);
                      }
                      else
                        if (node instanceof ExpROneOrMore)
                        {
                          postOrderWalk (((ExpROneOrMore) node).getRegExpr (), opObj);
                        }
                        else
                          if (node instanceof ExpRZeroOrMore)
                          {
                            postOrderWalk (((ExpRZeroOrMore) node).getRegExpr (), opObj);
                          }
                          else
                            if (node instanceof ExpRZeroOrOne)
                            {
                              postOrderWalk (((ExpRZeroOrOne) node).getRegExpr (), opObj);
                            }
                            else
                              if (node instanceof ExpRRepetitionRange)
                              {
                                postOrderWalk (((ExpRRepetitionRange) node).getRegExpr (), opObj);
                              }
    }
    opObj.action (node);
  }

}
