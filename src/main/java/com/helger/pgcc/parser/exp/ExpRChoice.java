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
package com.helger.pgcc.parser.exp;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pgcc.output.java.LexGenJava;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Nfa;
import com.helger.pgcc.parser.NfaState;

/**
 * Describes regular expressions which are choices from from among included regular expressions.
 */

public final class ExpRChoice extends AbstractExpRegularExpression
{
  /**
   * The list of choices of this regular expression. Each list component will narrow to
   * RegularExpression.
   */
  private final List <AbstractExpRegularExpression> m_aChoices = new ArrayList <> ();

  public ExpRChoice ()
  {}

  /**
   * @return the choices
   */
  @NonNull
  public final List <AbstractExpRegularExpression> getChoices ()
  {
    return m_aChoices;
  }

  @Nonnegative
  public final int getChoiceCount ()
  {
    return m_aChoices.size ();
  }

  @NonNull
  public final AbstractExpRegularExpression getChoiceAt (final int nIndex)
  {
    return m_aChoices.get (nIndex);
  }

  public final void addChoice (@NonNull final AbstractExpRegularExpression a)
  {
    ValueEnforcer.notNull (a, "Expansion");
    m_aChoices.add (a);
  }

  @Override
  public Nfa generateNfa (final boolean bIgnoreCase)
  {
    compressCharLists ();

    if (getChoiceCount () == 1)
      return getChoiceAt (0).generateNfa (bIgnoreCase);

    final Nfa aRetVal = new Nfa ();
    final NfaState aStartState = aRetVal.start ();
    final NfaState aFinalState = aRetVal.end ();

    for (final AbstractExpRegularExpression curRE : getChoices ())
    {
      final Nfa aTemp = curRE.generateNfa (bIgnoreCase);

      aStartState.addMove (aTemp.start ());
      aTemp.end ().addMove (aFinalState);
    }

    return aRetVal;
  }

  void compressCharLists ()
  {
    compressChoices (); // Unroll nested choices
    AbstractExpRegularExpression aCurRE;
    ExpRCharacterList aCurCharList = null;

    for (int i = 0; i < getChoiceCount (); i++)
    {
      aCurRE = getChoiceAt (i);

      while (aCurRE instanceof ExpRJustName)
        aCurRE = ((ExpRJustName) aCurRE).m_aRegexpr;

      if (aCurRE instanceof ExpRStringLiteral && ((ExpRStringLiteral) aCurRE).m_sImage.length () == 1)
      {
        aCurRE = new ExpRCharacterList (((ExpRStringLiteral) aCurRE).m_sImage.charAt (0));
        getChoices ().set (i, aCurRE);
      }

      if (aCurRE instanceof final ExpRCharacterList aRCharacterList)
      {
        if (aRCharacterList.isNegatedList ())
          aRCharacterList.removeNegation ();

        final List <ICCCharacter> aTmp = aRCharacterList.getDescriptors ();

        if (aCurCharList == null)
        {
          aCurCharList = new ExpRCharacterList ();
          aCurRE = aCurCharList;
          getChoices ().set (i, aCurRE);
        }
        else
          getChoices ().remove (i--);

        for (int j = aTmp.size (); j-- > 0;)
          aCurCharList.addDescriptor (aTmp.get (j));
      }

    }
  }

  void compressChoices ()
  {
    for (int i = 0; i < getChoiceCount (); i++)
    {
      AbstractExpRegularExpression aCurRE = getChoiceAt (i);

      while (aCurRE instanceof ExpRJustName)
        aCurRE = ((ExpRJustName) aCurRE).m_aRegexpr;

      if (aCurRE instanceof final ExpRChoice aRChoice)
      {
        getChoices ().remove (i--);
        for (int j = aRChoice.getChoiceCount (); j-- > 0;)
          addChoice (aRChoice.getChoiceAt (j));
      }
    }
  }

  public int checkUnmatchability ()
  {
    int nNumStrings = 0;

    for (final AbstractExpRegularExpression curRE : getChoices ())
    {
      if (!curRE.m_bPrivateRexp &&
        // curRE instanceof RJustName &&
        curRE.getOrdinal () > 0 &&
        curRE.getOrdinal () < getOrdinal () &&
        LexGenJava.lexer ().getLexStates ()[curRE.getOrdinal ()] == LexGenJava.lexer ().getLexStates ()[getOrdinal ()])
      {
        if (hasLabel ())
          JavaCCErrors.warning (this,
                                "Regular Expression choice : " +
                                      curRE.getLabel () +
                                      " can never be matched as : " +
                                      getLabel ());
        else
          JavaCCErrors.warning (this,
                                "Regular Expression choice : " +
                                      curRE.getLabel () +
                                      " can never be matched as token of kind : " +
                                      getOrdinal ());
      }

      if (!curRE.m_bPrivateRexp && curRE instanceof ExpRStringLiteral)
        nNumStrings++;
    }
    return nNumStrings;
  }
}
