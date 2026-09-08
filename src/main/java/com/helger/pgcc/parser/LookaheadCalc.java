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

import static com.helger.pgcc.parser.JavaCCGlobals.addEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.context.LookaheadState;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpChoice;
import com.helger.pgcc.parser.exp.ExpLookahead;
import com.helger.pgcc.parser.exp.ExpOneOrMore;
import com.helger.pgcc.parser.exp.ExpRStringLiteral;
import com.helger.pgcc.parser.exp.ExpSequence;
import com.helger.pgcc.parser.exp.ExpZeroOrMore;
import com.helger.pgcc.parser.exp.ExpZeroOrOne;
import com.helger.pgcc.parser.exp.Expansion;

public final class LookaheadCalc
{
  private LookaheadCalc ()
  {}

  @Nullable
  private static MatchInfo _overlap (final List <MatchInfo> v1, final List <MatchInfo> aV2)
  {
    MatchInfo aM1, aM2, aM3;
    int nSize;
    boolean bDiff;
    for (final MatchInfo element : v1)
    {
      aM1 = element;
      for (final MatchInfo element2 : aV2)
      {
        aM2 = element2;
        nSize = aM1.m_nFirstFreeLoc;
        aM3 = aM1;
        if (nSize > aM2.m_nFirstFreeLoc)
        {
          nSize = aM2.m_nFirstFreeLoc;
          aM3 = aM2;
        }
        if (nSize == 0)
          return null;

        // we wish to ignore empty expansions and the JAVACODE stuff here.
        bDiff = false;
        for (int k = 0; k < nSize; k++)
        {
          if (aM1.m_aMatch[k] != aM2.m_aMatch[k])
          {
            bDiff = true;
            break;
          }
        }
        if (!bDiff)
          return aM3;
      }
    }
    return null;
  }

  private static boolean _isJavaCodeCheck (final List <MatchInfo> v)
  {
    for (final MatchInfo mi : v)
      if (mi.m_nFirstFreeLoc == 0)
        return true;
    return false;
  }

  private static String _image (@NonNull final MatchInfo m)
  {
    String sRet = "";
    for (int i = 0; i < m.m_nFirstFreeLoc; i++)
    {
      if (m.m_aMatch[i] == 0)
      {
        sRet += " <EOF>";
      }
      else
      {
        final AbstractExpRegularExpression aRe = grammar ().rexpsOfTokens ().get (Integer.valueOf (m.m_aMatch[i]));
        if (aRe instanceof final ExpRStringLiteral aRStringLiteral)
        {
          sRet += " \"" + addEscapes (aRStringLiteral.m_sImage) + "\"";
        }
        else
          if (aRe.hasLabel ())
            sRet += " <" + aRe.getLabel () + ">";
          else
            sRet += " <token of kind " + i + ">";
      }
    }
    if (m.m_nFirstFreeLoc == 0)
      return "";
    return sRet.substring (1);
  }

  public static void choiceCalc (final ExpChoice aCh)
  {
    final int nFirst = _firstChoice (aCh);
    // dbl[i] and dbr[i] are lists of size limited matches for choice i
    // of ch. dbl ignores matches with semantic lookaheads (when force_la_check
    // is false), while dbr ignores semantic lookahead.
    @SuppressWarnings ("unchecked")
    final List <MatchInfo> [] aDbl = new List [aCh.getChoiceCount ()];
    @SuppressWarnings ("unchecked")
    final List <MatchInfo> [] aDbr = new List [aCh.getChoiceCount ()];
    final int [] aMinLA = new int [aCh.getChoiceCount () - 1];
    final MatchInfo [] aOverlapInfo = new MatchInfo [aCh.getChoiceCount () - 1];
    final int [] aOther = new int [aCh.getChoiceCount () - 1];
    MatchInfo m;
    List <MatchInfo> v;
    boolean bOverlapDetected;
    for (int nLa = 1; nLa <= Options.getChoiceAmbiguityCheck (); nLa++)
    {
      LookaheadState.current ().setLimit (nLa);
      LookaheadState.current ().setConsiderSemanticLA (!Options.isForceLaCheck ());
      for (int i = nFirst; i < aCh.getChoiceCount () - 1; i++)
      {
        LookaheadState.current ().setSizeLimitedMatches (new ArrayList <> ());
        m = new MatchInfo ();
        m.m_nFirstFreeLoc = 0;
        v = new ArrayList <> ();
        v.add (m);
        LookaheadWalk.genFirstSetRecursive (v, aCh.getChoiceAt (i));
        aDbl[i] = LookaheadState.current ().getSizeLimitedMatches ();
      }
      LookaheadState.current ().setConsiderSemanticLA (false);
      for (int i = nFirst + 1; i < aCh.getChoiceCount (); i++)
      {
        LookaheadState.current ().setSizeLimitedMatches (new ArrayList <> ());
        m = new MatchInfo ();
        m.m_nFirstFreeLoc = 0;
        v = new ArrayList <> ();
        v.add (m);
        LookaheadWalk.genFirstSetRecursive (v, aCh.getChoiceAt (i));
        aDbr[i] = LookaheadState.current ().getSizeLimitedMatches ();
      }
      if (nLa == 1)
      {
        for (int i = nFirst; i < aCh.getChoiceCount () - 1; i++)
        {
          final Expansion aExp = aCh.getChoiceAt (i);
          if (Semanticize.emptyExpansionExists (aExp))
          {
            JavaCCErrors.warning (aExp,
                                  "This choice can expand to the empty token sequence " +
                                       "and will therefore always be taken in favor of the choices appearing later.");
            break;
          }
          else
            if (_isJavaCodeCheck (aDbl[i]))
            {
              JavaCCErrors.warning (aExp,
                                    "JAVACODE non-terminal will force this choice to be taken " +
                                         "in favor of the choices appearing later.");
              break;
            }
        }
      }
      bOverlapDetected = false;
      for (int i = nFirst; i < aCh.getChoiceCount () - 1; i++)
      {
        for (int j = i + 1; j < aCh.getChoiceCount (); j++)
        {
          if ((m = _overlap (aDbl[i], aDbr[j])) != null)
          {
            aMinLA[i] = nLa + 1;
            aOverlapInfo[i] = m;
            aOther[i] = j;
            bOverlapDetected = true;
            break;
          }
        }
      }
      if (!bOverlapDetected)
      {
        break;
      }
    }
    for (int i = nFirst; i < aCh.getChoiceCount () - 1; i++)
    {
      final Expansion aExp = aCh.getChoiceAt (i);
      if (_explicitLA (aExp) && !Options.isForceLaCheck ())
      {
        continue;
      }
      if (aMinLA[i] > Options.getChoiceAmbiguityCheck ())
      {
        JavaCCErrors.warning ("Choice conflict involving two expansions at");
        PGPrinter.error ("         line " +
                         aExp.getLineNumber () +
                         ", column " +
                         aExp.getColumnNumber () +
                         " and line " +
                         aCh.getChoiceAt (aOther[i]).getLineNumber () +
                         ", column " +
                         aCh.getChoiceAt (aOther[i]).getColumnNumber () +
                         " respectively.");
        PGPrinter.error ("         A common prefix is: " + _image (aOverlapInfo[i]));
        PGPrinter.error ("         Consider using a lookahead of " + aMinLA[i] + " or more for earlier expansion.");
      }
      else
        if (aMinLA[i] > 1)
        {
          JavaCCErrors.warning ("Choice conflict involving two expansions at");
          PGPrinter.error ("         line " +
                           aExp.getLineNumber () +
                           ", column " +
                           aExp.getColumnNumber () +
                           " and line " +
                           aCh.getChoiceAt (aOther[i]).getLineNumber () +
                           ", column " +
                           aCh.getChoiceAt (aOther[i]).getColumnNumber () +
                           " respectively.");
          PGPrinter.error ("         A common prefix is: " + _image (aOverlapInfo[i]));
          PGPrinter.error ("         Consider using a lookahead of " + aMinLA[i] + " for earlier expansion.");
        }
    }
  }

  private static boolean _explicitLA (final Expansion aExp)
  {
    if (aExp instanceof final ExpSequence seq)
    {
      final Object aObj = seq.getUnitAt (0);
      if (aObj instanceof final ExpLookahead la)
      {
        return la.isExplicit ();
      }
    }
    return false;
  }

  private static int _firstChoice (final ExpChoice aCh)
  {
    if (Options.isForceLaCheck ())
      return 0;

    int nIdx = 0;
    for (final Expansion element : aCh.getChoices ())
    {
      if (!_explicitLA (element))
        return nIdx;
      nIdx++;
    }

    return aCh.getChoiceCount ();
  }

  @NonNull
  private static String _image (final Expansion aExp)
  {
    if (aExp instanceof ExpOneOrMore)
      return "(...)+";

    if (aExp instanceof ExpZeroOrMore)
      return "(...)*";

    assert aExp instanceof ExpZeroOrOne;
    return "[...]";
  }

  public static void ebnfCalc (final Expansion aExp, final Expansion aNested)
  {
    // exp is one of OneOrMore, ZeroOrMore, ZeroOrOne
    MatchInfo m, m1 = null;
    List <MatchInfo> v;
    List <MatchInfo> aFirst, aFollow;
    int nLa;
    for (nLa = 1; nLa <= Options.getOtherAmbiguityCheck (); nLa++)
    {
      LookaheadState.current ().setLimit (nLa);
      LookaheadState.current ().setSizeLimitedMatches (new ArrayList <> ());
      m = new MatchInfo ();
      m.m_nFirstFreeLoc = 0;
      v = new ArrayList <> ();
      v.add (m);
      LookaheadState.current ().setConsiderSemanticLA (!Options.isForceLaCheck ());
      LookaheadWalk.genFirstSetRecursive (v, aNested);
      aFirst = LookaheadState.current ().getSizeLimitedMatches ();
      LookaheadState.current ().setSizeLimitedMatches (new ArrayList <> ());
      LookaheadState.current ().setConsiderSemanticLA (false);
      LookaheadWalk.genFollowSetRecursive (v, aExp, Expansion.getNextGenerationIndex ());
      aFollow = LookaheadState.current ().getSizeLimitedMatches ();
      if (nLa == 1)
      {
        if (_isJavaCodeCheck (aFirst))
        {
          JavaCCErrors.warning (aNested,
                                "JAVACODE non-terminal within " +
                                        _image (aExp) +
                                        " construct will force this construct to be entered in favor of " +
                                        "expansions occurring after construct.");
        }
      }
      if ((m = _overlap (aFirst, aFollow)) == null)
      {
        break;
      }
      m1 = m;
    }
    if (nLa > Options.getOtherAmbiguityCheck ())
    {
      JavaCCErrors.warning ("Choice conflict in " +
                            _image (aExp) +
                            " construct " +
                            "at line " +
                            aExp.getLineNumber () +
                            ", column " +
                            aExp.getColumnNumber () +
                            ".");
      PGPrinter.error ("         Expansion nested within construct and expansion following construct");
      PGPrinter.error ("         have common prefixes, one of which is: " + _image (m1));
      PGPrinter.error ("         Consider using a lookahead of " + nLa + " or more for nested expansion.");
    }
    else
      if (nLa > 1)
      {
        JavaCCErrors.warning ("Choice conflict in " +
                              _image (aExp) +
                              " construct " +
                              "at line " +
                              aExp.getLineNumber () +
                              ", column " +
                              aExp.getColumnNumber () +
                              ".");
        PGPrinter.error ("         Expansion nested within construct and expansion following construct");
        PGPrinter.error ("         have common prefixes, one of which is: " + _image (m1));
        PGPrinter.error ("         Consider using a lookahead of " + nLa + " for nested expansion.");
      }
  }

}
