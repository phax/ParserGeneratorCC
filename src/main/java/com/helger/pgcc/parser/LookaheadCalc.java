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

import static com.helger.pgcc.parser.JavaCCGlobals.REXPS_OF_TOKENS;
import static com.helger.pgcc.parser.JavaCCGlobals.addEscapes;

import java.util.ArrayList;
import java.util.List;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpChoice;
import com.helger.pgcc.parser.exp.ExpLookahead;
import com.helger.pgcc.parser.exp.ExpOneOrMore;
import com.helger.pgcc.parser.exp.ExpRStringLiteral;
import com.helger.pgcc.parser.exp.ExpSequence;
import com.helger.pgcc.parser.exp.ExpZeroOrMore;
import com.helger.pgcc.parser.exp.ExpZeroOrOne;
import com.helger.pgcc.parser.exp.Expansion;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class LookaheadCalc
{
  private LookaheadCalc ()
  {}

  @Nullable
  private static MatchInfo _overlap (final List <MatchInfo> v1, final List <MatchInfo> v2)
  {
    MatchInfo m1, m2, m3;
    int size;
    boolean diff;
    for (final MatchInfo element : v1)
    {
      m1 = element;
      for (final MatchInfo element2 : v2)
      {
        m2 = element2;
        size = m1.m_firstFreeLoc;
        m3 = m1;
        if (size > m2.m_firstFreeLoc)
        {
          size = m2.m_firstFreeLoc;
          m3 = m2;
        }
        if (size == 0)
          return null;

        // we wish to ignore empty expansions and the JAVACODE stuff here.
        diff = false;
        for (int k = 0; k < size; k++)
        {
          if (m1.m_match[k] != m2.m_match[k])
          {
            diff = true;
            break;
          }
        }
        if (!diff)
          return m3;
      }
    }
    return null;
  }

  private static boolean _isJavaCodeCheck (final List <MatchInfo> v)
  {
    for (final MatchInfo mi : v)
      if (mi.m_firstFreeLoc == 0)
        return true;
    return false;
  }

  private static String _image (@Nonnull final MatchInfo m)
  {
    String ret = "";
    for (int i = 0; i < m.m_firstFreeLoc; i++)
    {
      if (m.m_match[i] == 0)
      {
        ret += " <EOF>";
      }
      else
      {
        final AbstractExpRegularExpression re = REXPS_OF_TOKENS.get (Integer.valueOf (m.m_match[i]));
        if (re instanceof ExpRStringLiteral)
        {
          ret += " \"" + addEscapes (((ExpRStringLiteral) re).m_image) + "\"";
        }
        else
          if (re.hasLabel ())
            ret += " <" + re.getLabel () + ">";
          else
            ret += " <token of kind " + i + ">";
      }
    }
    if (m.m_firstFreeLoc == 0)
      return "";
    return ret.substring (1);
  }

  public static void choiceCalc (final ExpChoice ch)
  {
    final int first = _firstChoice (ch);
    // dbl[i] and dbr[i] are lists of size limited matches for choice i
    // of ch. dbl ignores matches with semantic lookaheads (when force_la_check
    // is false), while dbr ignores semantic lookahead.
    @SuppressWarnings ("unchecked")
    final List <MatchInfo> [] dbl = new List [ch.getChoiceCount ()];
    @SuppressWarnings ("unchecked")
    final List <MatchInfo> [] dbr = new List [ch.getChoiceCount ()];
    final int [] minLA = new int [ch.getChoiceCount () - 1];
    final MatchInfo [] overlapInfo = new MatchInfo [ch.getChoiceCount () - 1];
    final int [] other = new int [ch.getChoiceCount () - 1];
    MatchInfo m;
    List <MatchInfo> v;
    boolean overlapDetected;
    for (int la = 1; la <= Options.getChoiceAmbiguityCheck (); la++)
    {
      MatchInfo.s_laLimit = la;
      LookaheadWalk.s_considerSemanticLA = !Options.isForceLaCheck ();
      for (int i = first; i < ch.getChoiceCount () - 1; i++)
      {
        LookaheadWalk.s_sizeLimitedMatches = new ArrayList <> ();
        m = new MatchInfo ();
        m.m_firstFreeLoc = 0;
        v = new ArrayList <> ();
        v.add (m);
        LookaheadWalk.genFirstSet (v, ch.getChoiceAt (i));
        dbl[i] = LookaheadWalk.s_sizeLimitedMatches;
      }
      LookaheadWalk.s_considerSemanticLA = false;
      for (int i = first + 1; i < ch.getChoiceCount (); i++)
      {
        LookaheadWalk.s_sizeLimitedMatches = new ArrayList <> ();
        m = new MatchInfo ();
        m.m_firstFreeLoc = 0;
        v = new ArrayList <> ();
        v.add (m);
        LookaheadWalk.genFirstSet (v, ch.getChoiceAt (i));
        dbr[i] = LookaheadWalk.s_sizeLimitedMatches;
      }
      if (la == 1)
      {
        for (int i = first; i < ch.getChoiceCount () - 1; i++)
        {
          final Expansion exp = ch.getChoiceAt (i);
          if (Semanticize.emptyExpansionExists (exp))
          {
            JavaCCErrors.warning (exp,
                                  "This choice can expand to the empty token sequence " +
                                       "and will therefore always be taken in favor of the choices appearing later.");
            break;
          }
          else
            if (_isJavaCodeCheck (dbl[i]))
            {
              JavaCCErrors.warning (exp,
                                    "JAVACODE non-terminal will force this choice to be taken " +
                                         "in favor of the choices appearing later.");
              break;
            }
        }
      }
      overlapDetected = false;
      for (int i = first; i < ch.getChoiceCount () - 1; i++)
      {
        for (int j = i + 1; j < ch.getChoiceCount (); j++)
        {
          if ((m = _overlap (dbl[i], dbr[j])) != null)
          {
            minLA[i] = la + 1;
            overlapInfo[i] = m;
            other[i] = j;
            overlapDetected = true;
            break;
          }
        }
      }
      if (!overlapDetected)
      {
        break;
      }
    }
    for (int i = first; i < ch.getChoiceCount () - 1; i++)
    {
      final Expansion exp = ch.getChoiceAt (i);
      if (_explicitLA (exp) && !Options.isForceLaCheck ())
      {
        continue;
      }
      if (minLA[i] > Options.getChoiceAmbiguityCheck ())
      {
        JavaCCErrors.warning ("Choice conflict involving two expansions at");
        PGPrinter.error ("         line " +
                         exp.getLine () +
                         ", column " +
                         exp.getColumn () +
                         " and line " +
                         ch.getChoiceAt (other[i]).getLine () +
                         ", column " +
                         ch.getChoiceAt (other[i]).getColumn () +
                         " respectively.");
        PGPrinter.error ("         A common prefix is: " + _image (overlapInfo[i]));
        PGPrinter.error ("         Consider using a lookahead of " + minLA[i] + " or more for earlier expansion.");
      }
      else
        if (minLA[i] > 1)
        {
          JavaCCErrors.warning ("Choice conflict involving two expansions at");
          PGPrinter.error ("         line " +
                           exp.getLine () +
                           ", column " +
                           exp.getColumn () +
                           " and line " +
                           ch.getChoiceAt (other[i]).getLine () +
                           ", column " +
                           ch.getChoiceAt (other[i]).getColumn () +
                           " respectively.");
          PGPrinter.error ("         A common prefix is: " + _image (overlapInfo[i]));
          PGPrinter.error ("         Consider using a lookahead of " + minLA[i] + " for earlier expansion.");
        }
    }
  }

  private static boolean _explicitLA (final Expansion exp)
  {
    if (exp instanceof ExpSequence)
    {
      final ExpSequence seq = (ExpSequence) exp;
      final Object obj = seq.getUnitAt (0);
      if (obj instanceof ExpLookahead)
      {
        final ExpLookahead la = (ExpLookahead) obj;
        return la.isExplicit ();
      }
    }
    return false;
  }

  private static int _firstChoice (final ExpChoice ch)
  {
    if (Options.isForceLaCheck ())
      return 0;

    int idx = 0;
    for (final Expansion element : ch.getChoices ())
    {
      if (!_explicitLA (element))
        return idx;
      idx++;
    }

    return ch.getChoiceCount ();
  }

  @Nonnull
  private static String _image (final Expansion exp)
  {
    if (exp instanceof ExpOneOrMore)
      return "(...)+";

    if (exp instanceof ExpZeroOrMore)
      return "(...)*";

    assert exp instanceof ExpZeroOrOne;
    return "[...]";
  }

  public static void ebnfCalc (final Expansion exp, final Expansion nested)
  {
    // exp is one of OneOrMore, ZeroOrMore, ZeroOrOne
    MatchInfo m, m1 = null;
    List <MatchInfo> v;
    List <MatchInfo> first, follow;
    int la;
    for (la = 1; la <= Options.getOtherAmbiguityCheck (); la++)
    {
      MatchInfo.s_laLimit = la;
      LookaheadWalk.s_sizeLimitedMatches = new ArrayList <> ();
      m = new MatchInfo ();
      m.m_firstFreeLoc = 0;
      v = new ArrayList <> ();
      v.add (m);
      LookaheadWalk.s_considerSemanticLA = !Options.isForceLaCheck ();
      LookaheadWalk.genFirstSet (v, nested);
      first = LookaheadWalk.s_sizeLimitedMatches;
      LookaheadWalk.s_sizeLimitedMatches = new ArrayList <> ();
      LookaheadWalk.s_considerSemanticLA = false;
      LookaheadWalk.genFollowSet (v, exp, Expansion.getNextGenerationIndex ());
      follow = LookaheadWalk.s_sizeLimitedMatches;
      if (la == 1)
      {
        if (_isJavaCodeCheck (first))
        {
          JavaCCErrors.warning (nested,
                                "JAVACODE non-terminal within " +
                                        _image (exp) +
                                        " construct will force this construct to be entered in favor of " +
                                        "expansions occurring after construct.");
        }
      }
      if ((m = _overlap (first, follow)) == null)
      {
        break;
      }
      m1 = m;
    }
    if (la > Options.getOtherAmbiguityCheck ())
    {
      JavaCCErrors.warning ("Choice conflict in " +
                            _image (exp) +
                            " construct " +
                            "at line " +
                            exp.getLine () +
                            ", column " +
                            exp.getColumn () +
                            ".");
      PGPrinter.error ("         Expansion nested within construct and expansion following construct");
      PGPrinter.error ("         have common prefixes, one of which is: " + _image (m1));
      PGPrinter.error ("         Consider using a lookahead of " + la + " or more for nested expansion.");
    }
    else
      if (la > 1)
      {
        JavaCCErrors.warning ("Choice conflict in " +
                              _image (exp) +
                              " construct " +
                              "at line " +
                              exp.getLine () +
                              ", column " +
                              exp.getColumn () +
                              ".");
        PGPrinter.error ("         Expansion nested within construct and expansion following construct");
        PGPrinter.error ("         have common prefixes, one of which is: " + _image (m1));
        PGPrinter.error ("         Consider using a lookahead of " + la + " for nested expansion.");
      }
  }

}
