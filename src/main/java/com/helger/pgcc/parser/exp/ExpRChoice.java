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
package com.helger.pgcc.parser.exp;

import java.util.ArrayList;
import java.util.List;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.LexGenJava;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;

import jakarta.annotation.Nonnull;

/**
 * Describes regular expressions which are choices from from among included
 * regular expressions.
 */

public class ExpRChoice extends AbstractExpRegularExpression
{
  /**
   * The list of choices of this regular expression. Each list component will
   * narrow to RegularExpression.
   */
  private final List <AbstractExpRegularExpression> m_choices = new ArrayList <> ();

  public ExpRChoice ()
  {}

  /**
   * @return the choices
   */
  @Nonnull
  public final List <AbstractExpRegularExpression> getChoices ()
  {
    return m_choices;
  }

  @Nonnegative
  public final int getChoiceCount ()
  {
    return m_choices.size ();
  }

  @Nonnull
  public final AbstractExpRegularExpression getChoiceAt (final int nIndex)
  {
    return m_choices.get (nIndex);
  }

  public final void addChoice (@Nonnull final AbstractExpRegularExpression a)
  {
    ValueEnforcer.notNull (a, "Expansion");
    m_choices.add (a);
  }

  @Override
  public Nfa generateNfa (final boolean ignoreCase)
  {
    compressCharLists ();

    if (getChoiceCount () == 1)
      return getChoiceAt (0).generateNfa (ignoreCase);

    final Nfa retVal = new Nfa ();
    final NfaState startState = retVal.start ();
    final NfaState finalState = retVal.end ();

    for (final AbstractExpRegularExpression curRE : getChoices ())
    {
      final Nfa temp = curRE.generateNfa (ignoreCase);

      startState.addMove (temp.start ());
      temp.end ().addMove (finalState);
    }

    return retVal;
  }

  void compressCharLists ()
  {
    compressChoices (); // Unroll nested choices
    AbstractExpRegularExpression curRE;
    ExpRCharacterList curCharList = null;

    for (int i = 0; i < getChoiceCount (); i++)
    {
      curRE = getChoiceAt (i);

      while (curRE instanceof ExpRJustName)
        curRE = ((ExpRJustName) curRE).m_regexpr;

      if (curRE instanceof ExpRStringLiteral && ((ExpRStringLiteral) curRE).m_image.length () == 1)
      {
        curRE = new ExpRCharacterList (((ExpRStringLiteral) curRE).m_image.charAt (0));
        getChoices ().set (i, curRE);
      }

      if (curRE instanceof ExpRCharacterList)
      {
        if (((ExpRCharacterList) curRE).isNegatedList ())
          ((ExpRCharacterList) curRE).removeNegation ();

        final List <ICCCharacter> tmp = ((ExpRCharacterList) curRE).getDescriptors ();

        if (curCharList == null)
        {
          curCharList = new ExpRCharacterList ();
          curRE = curCharList;
          getChoices ().set (i, curRE);
        }
        else
          getChoices ().remove (i--);

        for (int j = tmp.size (); j-- > 0;)
          curCharList.addDescriptor (tmp.get (j));
      }

    }
  }

  void compressChoices ()
  {
    for (int i = 0; i < getChoiceCount (); i++)
    {
      AbstractExpRegularExpression curRE = getChoiceAt (i);

      while (curRE instanceof ExpRJustName)
        curRE = ((ExpRJustName) curRE).m_regexpr;

      if (curRE instanceof ExpRChoice)
      {
        getChoices ().remove (i--);
        for (int j = ((ExpRChoice) curRE).getChoiceCount (); j-- > 0;)
          addChoice (((ExpRChoice) curRE).getChoiceAt (j));
      }
    }
  }

  public int checkUnmatchability ()
  {
    int numStrings = 0;

    for (final AbstractExpRegularExpression curRE : getChoices ())
    {
      if (!curRE.m_private_rexp &&
          // curRE instanceof RJustName &&
          curRE.getOrdinal () > 0 &&
          curRE.getOrdinal () < getOrdinal () &&
          LexGenJava.s_lexStates[curRE.getOrdinal ()] == LexGenJava.s_lexStates[getOrdinal ()])
      {
        if (hasLabel ())
          JavaCCErrors.warning (this, "Regular Expression choice : " + curRE.getLabel () + " can never be matched as : " + getLabel ());
        else
          JavaCCErrors.warning (this,
                                "Regular Expression choice : " +
                                      curRE.getLabel () +
                                      " can never be matched as token of kind : " +
                                      getOrdinal ());
      }

      if (!curRE.m_private_rexp && curRE instanceof ExpRStringLiteral)
        numStrings++;
    }
    return numStrings;
  }
}
