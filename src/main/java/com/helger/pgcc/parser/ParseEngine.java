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

import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import java.util.HashSet;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.context.GrammarState;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;
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
 * Turns the productions of the grammar into the methods of the generated parser, deciding at every
 * choice point how to tell the alternatives apart.
 */
public class ParseEngine
{
  /**
   * {@return the syntax of the target language, so that the engine says what it wants emitted
   * rather than switching on the language at every spot.}
   */
  @NonNull
  private int m_nGenSymbolIndex = 0;
  private int m_nIndentCount = 0;
  private boolean m_bJJ2LA = false;
  private AbstractCodeGenerator m_aCodeGenerator;

  /**
   * These lists are used to maintain expansions for which code generation in phase 2 and phase 3 is
   * required. Whenever a call is generated to a phase 2 or phase 3 routine, a corresponding entry
   * is added here if it has not already been added. The phase 3 routines have been optimized in
   * version 0.7pre2. Essentially only those methods (and only those portions of these methods) are
   * generated that are required. The lookahead amount is used to determine this. This change
   * requires the use of a hash table because it is now possible for the same phase 3 routine to be
   * requested multiple times with different lookaheads. The hash table provides a easily searchable
   * capability to determine the previous requests. The phase 3 routines now are performed in a two
   * step process - the first step gathers the requests (replacing requests with lower lookaheads
   * with those requiring larger lookaheads). The second step then generates these methods. This
   * optimization and the hashtable makes it look like we do not need the flag "phase3done" any
   * more. But this has not been removed yet.
   */
  private final ICommonsList <ExpLookahead> m_aPhase2list = new CommonsArrayList <> ();
  private final ICommonsList <Phase3Data> m_aPhase3list = new CommonsArrayList <> ();
  private final ICommonsMap <Expansion, Phase3Data> m_aPhase3table = new CommonsHashMap <> ();

  /**
   * Create the engine.
   */
  public ParseEngine ()
  {}

  @NonNull
  private EOutputLanguage _lang ()
  {
    return m_aCodeGenerator.getOutputLanguage ();
  }

  /**
   * The phase 1 routines generates their output into String's and dumps these String's once for
   * each method. These String's contain the special characters '\u0001' to indicate a positive
   * indent, and '\u0002' to indicate a negative indent. '\n' is used to indicate a line terminator.
   * The characters '\u0003' and '\u0004' are used to delineate portions of text where '\n's should
   * not be followed by an indentation.
   */

  private static final char INDENT_INC = '\u0001';
  private static final char INDENT_DEC = '\u0002';
  private static final char INDENT_OFF = '\u0003';
  private static final char INDENT_ON = '\u0004';

  /**
   * Returns true if there is a JAVACODE production that the argument expansion may directly expand
   * to (without consuming tokens or encountering lookahead).
   */
  private boolean _javaCodeCheck (final Expansion aExp)
  {
    if (aExp instanceof AbstractExpRegularExpression)
    {
      return false;
    }

    if (aExp instanceof final ExpNonTerminal aNonTerminal)
    {
      final AbstractNormalProduction aProd = aNonTerminal.getProd ();
      if (aProd instanceof AbstractCodeProduction)
        return true;
      return _javaCodeCheck (aProd.getExpansion ());
    }

    if (aExp instanceof final ExpChoice ch)
    {
      for (final Expansion choice : ch.getChoices ())
        if (_javaCodeCheck (choice))
          return true;
      return false;
    }

    if (aExp instanceof final ExpSequence seq)
    {
      for (int i = 0; i < seq.getUnitCount (); i++)
      {
        final Expansion aUnit = seq.getUnitAt (i);
        if (aUnit instanceof final ExpLookahead aLookahead && aLookahead.isExplicit ())
        {
          // An explicit lookahead (rather than one generated implicitly).
          // Assume
          // the user knows what he / she is doing, e.g.
          // "A" ( "B" | LOOKAHEAD("X") jcode() | "C" )* "D"
          return false;
        }
        if (_javaCodeCheck (aUnit))
          return true;
        if (!Semanticize.emptyExpansionExists (aUnit))
          return false;
      }
      return false;
    }

    if (aExp instanceof final ExpOneOrMore om)
    {
      return _javaCodeCheck (om.getExpansion ());
    }

    if (aExp instanceof final ExpZeroOrMore zm)
    {
      return _javaCodeCheck (zm.getExpansion ());
    }

    if (aExp instanceof final ExpZeroOrOne zo)
    {
      return _javaCodeCheck (zo.getExpansion ());
    }

    if (aExp instanceof final ExpTryBlock tb)
    {
      return _javaCodeCheck (tb.getExp ());
    }

    return false;
  }

  /**
   * An array used to store the first sets generated by the following method. A true entry means
   * that the corresponding token is in the first set.
   */
  private boolean [] m_aFirstSet;

  /**
   * Sets up the array "firstSet" above based on the Expansion argument passed to it. Since this is
   * a recursive function, it assumes that "firstSet" has been reset before the first call.
   */
  private void _genFirstSet (final Expansion aExp)
  {
    if (aExp instanceof final AbstractExpRegularExpression aRegularExpression)
    {
      m_aFirstSet[aRegularExpression.getOrdinal ()] = true;
    }
    else
      if (aExp instanceof final ExpNonTerminal aExpNonTerminal)
      {
        if (!(aExpNonTerminal.getProd () instanceof AbstractCodeProduction))
        {
          _genFirstSet ((aExpNonTerminal.getProd ()).getExpansion ());
        }
      }
      else
        if (aExp instanceof final ExpChoice ch)
        {
          for (final Expansion element : ch.getChoices ())
          {
            _genFirstSet ((element));
          }
        }
        else
          if (aExp instanceof final ExpSequence seq)
          {
            final Object aObj = seq.getUnitAt (0);
            if (aObj instanceof final ExpLookahead aLookahead && aLookahead.getActionTokens ().isNotEmpty ())
            {
              m_bJJ2LA = true;
            }
            for (int i = 0; i < seq.getUnitCount (); i++)
            {
              final Expansion aUnit = seq.getUnitAt (i);
              // Javacode productions can not have FIRST sets. Instead we
              // generate the FIRST set
              // for the preceding LOOKAHEAD (the semantic checks should have
              // made sure that
              // the LOOKAHEAD is suitable).
              if (aUnit instanceof final ExpNonTerminal aNonTerminal &&
                  aNonTerminal.getProd () instanceof AbstractCodeProduction)
              {
                if (i > 0 && seq.getUnitAt (i - 1) instanceof final ExpLookahead aLa)
                {
                  _genFirstSet (aLa.getLaExpansion ());
                }
              }
              else
              {
                _genFirstSet (seq.getUnitAt (i));
              }
              if (!Semanticize.emptyExpansionExists (seq.getUnitAt (i)))
              {
                break;
              }
            }
          }
          else
            if (aExp instanceof final ExpOneOrMore om)
            {
              _genFirstSet (om.getExpansion ());
            }
            else
              if (aExp instanceof final ExpZeroOrMore zm)
              {
                _genFirstSet (zm.getExpansion ());
              }
              else
                if (aExp instanceof final ExpZeroOrOne zo)
                {
                  _genFirstSet (zo.getExpansion ());
                }
                else
                  if (aExp instanceof final ExpTryBlock tb)
                  {
                    _genFirstSet (tb.getExp ());
                  }
  }

  /**
   * Constants used in the following method "buildLookaheadChecker".
   */
  enum EState
  {
    NOOPENSTM,
    OPENIF,
    OPENSWITCH
  }

  @SuppressWarnings ("unused")
  private void _dumpLookaheads (@NonNull final ExpLookahead [] aConds, final String [] aActions)
  {
    for (int i = 0; i < aConds.length; i++)
    {
      PGPrinter.error ("Lookahead: " + i);
      PGPrinter.error (aConds[i].dump (0, new HashSet <> ()).toString ());
      PGPrinter.error ("");
    }
  }

  /**
   * This method takes two parameters - an array of Lookahead's "conds", and an array of String's
   * "actions". "actions" contains exactly one element more than "conds". "actions" are Java source
   * code, and "conds" translate to conditions - so lets say "f(conds[i])" is true if the lookahead
   * required by "conds[i]" is indeed the case. This method returns a string corresponding to the
   * Java code for: if (f(conds[0]) actions[0] else if (f(conds[1]) actions[1] . . . else
   * actions[action.length-1] A particular action entry ("actions[i]") can be null, in which case, a
   * noop is generated for that action.
   */
  String buildLookaheadChecker (@NonNull final ExpLookahead [] aConds, @NonNull final String [] aActions)
  {
    // The state variables.
    EState eState = EState.NOOPENSTM;
    int nIndentAmt = 0;
    final GrammarState aGrammar = grammar ();
    final boolean [] aCasedValues = new boolean [aGrammar.getTokenCount ()];
    String sRetval = "";
    ExpLookahead aLa;
    Token t = null;
    final int nTokenMaskSize = (aGrammar.getTokenCount () - 1) / 32 + 1;
    int [] aTokenMask = null;
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();

    // Iterate over all the conditions.
    int nIndex = 0;
    while (nIndex < aConds.length)
    {
      aLa = aConds[nIndex];
      m_bJJ2LA = false;

      if (aLa.getAmount () == 0 ||
          Semanticize.emptyExpansionExists (aLa.getLaExpansion ()) ||
          _javaCodeCheck (aLa.getLaExpansion ()))
      {

        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly
        // specified as 0).
        // . If it is possible for the lookahead expansion to recognize the
        // empty string - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it
        // directly expands to - in which case the lookahead trivially passes.
        if (aLa.getActionTokens ().isEmpty ())
        {
          // In addition, if there is no semantic lookahead, then the
          // lookahead trivially succeeds. So break the main loop and
          // treat this case as the default last action.
          break;
        }
        // This case is when there is only semantic lookahead
        // (without any preceding syntactic lookahead). In this
        // case, an "if" statement is generated.
        switch (eState)
        {
          case NOOPENSTM:
            sRetval += "\n" + "if (";
            nIndentAmt++;
            break;
          case OPENIF:
            sRetval += INDENT_DEC + "\n" + "} else if (";
            break;
          case OPENSWITCH:
            sRetval += INDENT_DEC + "\n" + "default:" + INDENT_INC;
            if (Options.isErrorReporting ())
            {
              sRetval += "\njj_la1[" + aGrammar.getMaskIndex () + "] = jj_gen;";
              aGrammar.incMaskIndex ();
            }
            aGrammar.maskVals ().add (aTokenMask);
            sRetval += "\n" + "if (";
            nIndentAmt++;
            break;
          default:
            throw new IllegalStateException ();
        }
        m_aCodeGenerator.printTokenSetup (aLa.getActionTokens ().getFirstOrNull ());
        for (final Token aElement : aLa.getActionTokens ())
        {
          t = aElement;
          sRetval += m_aCodeGenerator.getStringToPrint (t);
        }
        sRetval += m_aCodeGenerator.getTrailingComments (t);
        sRetval += ") {" + INDENT_INC + aActions[nIndex];
        eState = EState.OPENIF;
      }
      else
        if (aLa.getAmount () == 1 && aLa.getActionTokens ().isEmpty ())
        {
          /*
           * Special optimal processing when the lookahead is exactly 1, and there is no semantic
           * lookahead.
           */
          if (m_aFirstSet == null)
          {
            m_aFirstSet = new boolean [aGrammar.getTokenCount ()];
          }
          for (int i = 0; i < aGrammar.getTokenCount (); i++)
          {
            m_aFirstSet[i] = false;
          }
          /*
           * jj2LA is set to false at the beginning of the containing "if" statement. It is checked
           * immediately after the end of the same statement to determine if lookaheads are to be
           * performed using calls to the jj2 methods.
           */
          _genFirstSet (aLa.getLaExpansion ());
          /*
           * genFirstSet may find that semantic attributes are appropriate for the next token. In
           * which case, it sets jj2LA to true.
           */
          if (!m_bJJ2LA)
          {
            /*
             * This case is if there is no applicable semantic lookahead and the lookahead is one
             * (excluding the earlier cases such as JAVACODE, etc.).
             */
            switch (eState)
            {
              case OPENIF:
                sRetval += INDENT_DEC + "\n" + "} else {" + INDENT_INC;
                // Control flows through to next case.
                // $FALL-THROUGH$
              case NOOPENSTM:
                sRetval += "\n" + "switch (";
                if (Options.isCacheTokens ())
                {
                  sRetval += "jj_nt" + _lang ().getMemberAccess () + "kind";
                }
                else
                  sRetval += "jj_ntk == -1 ? jj_ntk_f() : jj_ntk";
                sRetval += ") {" + INDENT_INC;
                for (int i = 0; i < aGrammar.getTokenCount (); i++)
                {
                  aCasedValues[i] = false;
                }
                nIndentAmt++;
                aTokenMask = new int [nTokenMaskSize];
                for (int i = 0; i < nTokenMaskSize; i++)
                {
                  aTokenMask[i] = 0;
                }
                break;
              case OPENSWITCH:
                // Don't need to do anything if state is OPENSWITCH.
                break;
              default:
                throw new IllegalStateException ();
            }
            for (int i = 0; i < aGrammar.getTokenCount (); i++)
            {
              if (m_aFirstSet[i] && !aCasedValues[i])
              {
                aCasedValues[i] = true;
                sRetval += INDENT_DEC + "\ncase ";

                final int nJ1 = i / 32;
                final int nJ2 = i % 32;
                aTokenMask[nJ1] |= 1 << nJ2;
                final String s = aGrammar.namesOfTokens ().get (Integer.valueOf (i));
                if (s == null)
                  sRetval += i;
                else
                  sRetval += s;
                sRetval += ":" + INDENT_INC;
              }
            }
            sRetval += "{";
            sRetval += aActions[nIndex];
            sRetval += "\nbreak;\n}";
            eState = EState.OPENSWITCH;
          }
        }
        else
        {
          // This is the case when lookahead is determined through calls to
          // jj2 methods. The other case is when lookahead is 1, but semantic
          // attributes need to be evaluated. Hence this crazy control
          // structure.
          m_bJJ2LA = true;
        }

      if (m_bJJ2LA)
      {
        // In this case lookahead is determined by the jj2 methods.
        switch (eState)
        {
          case NOOPENSTM:
            sRetval += "\nif (";
            nIndentAmt++;
            break;
          case OPENIF:
            sRetval += INDENT_DEC + "\n} else if (";
            break;
          case OPENSWITCH:
            sRetval += INDENT_DEC + "\ndefault:" + INDENT_INC;
            if (Options.isErrorReporting ())
            {
              sRetval += "\njj_la1[" + aGrammar.getMaskIndex () + "] = jj_gen;";
              aGrammar.incMaskIndex ();
            }
            aGrammar.maskVals ().add (aTokenMask);
            sRetval += "\nif (";
            nIndentAmt++;
            break;
          default:
            throw new IllegalStateException ();
        }

        final int nInternalIndex = aGrammar.incAndGetJJ2Index ();
        // At this point, la.la_expansion.internal_name must be "".
        assert aLa.getLaExpansion ().getInternalName ().equals ("");
        aLa.getLaExpansion ().setInternalName ("_", nInternalIndex);

        m_aPhase2list.add (aLa);
        sRetval += "jj_2" + aLa.getLaExpansion ().getInternalName () + "(" + aLa.getAmount () + ")";
        if (aLa.getActionTokens ().isNotEmpty ())
        {
          // In addition, there is also a semantic lookahead. So concatenate
          // the semantic check with the syntactic one.
          sRetval += " && (";
          m_aCodeGenerator.printTokenSetup (aLa.getActionTokens ().getFirstOrNull ());
          for (final Token aElement : aLa.getActionTokens ())
          {
            t = aElement;
            sRetval += m_aCodeGenerator.getStringToPrint (t);
          }
          sRetval += m_aCodeGenerator.getTrailingComments (t);
          sRetval += ")";
        }
        sRetval += ") {" + INDENT_INC + aActions[nIndex];
        eState = EState.OPENIF;
      }

      nIndex++;
    }

    // Generate code for the default case. Note this may not
    // be the last entry of "actions" if any condition can be
    // statically determined to be always "true".

    switch (eState)
    {
      case NOOPENSTM:
        sRetval += aActions[nIndex];
        break;
      case OPENIF:
        sRetval += INDENT_DEC + "\n" + "} else {" + INDENT_INC + aActions[nIndex];
        break;
      case OPENSWITCH:
        sRetval += INDENT_DEC + "\n" + "default:" + INDENT_INC;
        if (Options.isErrorReporting ())
        {
          sRetval += "\njj_la1[" + aGrammar.getMaskIndex () + "] = jj_gen;";
          aGrammar.maskVals ().add (aTokenMask);
          aGrammar.incMaskIndex ();
        }
        sRetval += aActions[nIndex];
        break;
      default:
        throw new IllegalStateException ();
    }
    for (int i = 0; i < nIndentAmt; i++)
    {
      sRetval += INDENT_DEC + "\n}";
    }

    return sRetval;
  }

  void dumpFormattedString (@NonNull final String sStr)
  {
    char cCh = ' ';
    char cPrevChar;
    boolean bIndentOn = true;
    for (int i = 0; i < sStr.length (); i++)
    {
      cPrevChar = cCh;
      cCh = sStr.charAt (i);
      if (cCh == '\n' && cPrevChar == '\r')
      {
        // do nothing - we've already printed a new line for the '\r'
        // during the previous iteration.
      }
      else
        if (cCh == '\n' || cCh == '\r')
        {
          if (bIndentOn)
          {
            phase1NewLine ();
          }
          else
          {
            m_aCodeGenerator.genCodeNewLine ();
          }
        }
        else
          if (cCh == INDENT_INC)
          {
            m_nIndentCount += 2;
          }
          else
            if (cCh == INDENT_DEC)
            {
              m_nIndentCount -= 2;
            }
            else
              if (cCh == INDENT_OFF)
              {
                bIndentOn = false;
              }
              else
                if (cCh == INDENT_ON)
                {
                  bIndentOn = true;
                }
                else
                {
                  m_aCodeGenerator.genCode (cCh);
                }
    }
  }

  // Print CPPCODE method header.
  private String _generateCppMethodHeader (@NonNull final CodeProductionCpp p)
  {
    final StringBuilder aSig = new StringBuilder ();
    String sRet, sParams;
    Token t = null;

    if (false)
    {
      m_aCodeGenerator.printTokenSetup (t);
      grammar ().setCurrentColumn (1);
      final String sComment1 = m_aCodeGenerator.getLeadingComments (t);
      grammar ().setCurrentLine (t.beginLine);
      grammar ().setCurrentColumn (t.beginColumn);
      aSig.append (t.image);
    }

    for (final Token element : p.getReturnTypeTokens ())
    {
      t = element;
      final String s = m_aCodeGenerator.getStringToPrint (t);
      aSig.append (t.toString ());
      aSig.append (" ");
    }

    String sComment2 = "";
    if (t != null)
      sComment2 = m_aCodeGenerator.getTrailingComments (t);
    sRet = aSig.toString ();

    aSig.setLength (0);
    aSig.append ("(");
    if (!p.getParameterListTokens ().isEmpty ())
    {
      m_aCodeGenerator.printTokenSetup (p.getParameterListTokens ().get (0));
      for (final Token aElement : p.getParameterListTokens ())
      {
        t = aElement;
        aSig.append (m_aCodeGenerator.getStringToPrint (t));
      }
      aSig.append (m_aCodeGenerator.getTrailingComments (t));
    }
    aSig.append (")");
    sParams = aSig.toString ();

    // For now, just ignore comments
    m_aCodeGenerator.generateMethodDefHeader (sRet,
                                              grammar ().getParserName (),
                                              p.getLhs () + sParams,
                                              aSig.toString ());

    return "";
  }

  // Print method header and return the ERROR_RETURN string.
  private String _generateCppMethodHeader (@NonNull final BNFProduction p, final Token aT2)
  {
    final StringBuilder aSig = new StringBuilder ();
    Token t = aT2;

    final String sMethod_name = p.getLhs ();
    boolean bVoid_ret = false;
    boolean bPtr_ret = false;

    m_aCodeGenerator.printTokenSetup (t);
    grammar ().setCurrentColumn (1);
    final String sComment1 = m_aCodeGenerator.getLeadingComments (t);
    grammar ().setCurrentLine (t.beginLine);
    grammar ().setCurrentColumn (t.beginColumn);
    aSig.append (t.image);
    if (t.kind == JavaCCParserConstants.VOID)
      bVoid_ret = true;
    if (t.kind == JavaCCParserConstants.STAR)
      bPtr_ret = true;

    for (int i = 1; i < p.getReturnTypeTokens ().size (); i++)
    {
      t = p.getReturnTypeTokens ().get (i);
      aSig.append (m_aCodeGenerator.getStringToPrint (t));
      if (t.kind == JavaCCParserConstants.VOID)
        bVoid_ret = true;
      if (t.kind == JavaCCParserConstants.STAR)
        bPtr_ret = true;
    }

    final String sComment2 = m_aCodeGenerator.getTrailingComments (t);
    final String sRet = aSig.toString ();

    aSig.setLength (0);
    aSig.append ("(");
    if (!p.getParameterListTokens ().isEmpty ())
    {
      m_aCodeGenerator.printTokenSetup (p.getParameterListTokens ().get (0));
      for (final Token aElement : p.getParameterListTokens ())
      {
        t = aElement;
        aSig.append (m_aCodeGenerator.getStringToPrint (t));
      }
      aSig.append (m_aCodeGenerator.getTrailingComments (t));
    }
    aSig.append (")");
    final String sParams = aSig.toString ();

    // For now, just ignore comments
    m_aCodeGenerator.generateMethodDefHeader (sRet,
                                              grammar ().getParserName (),
                                              p.getLhs () + sParams,
                                              aSig.toString ());

    // Generate a default value for error return.
    String sDefault_return;
    if (bPtr_ret)
      sDefault_return = "NULL";
    else
      if (bVoid_ret)
        sDefault_return = "";
      else
      {
        // 0 converts to most (all?) basic types.
        sDefault_return = "0";
      }

    final String sDefine = "ERROR_RET_" + sMethod_name;
    return "\n#if !defined " +
           sDefine +
           "\n#define " +
           sDefine +
           " " +
           sDefault_return +
           "\n" +
           "#endif\n" +
           "#define __ERROR_RET__ " +
           sDefine +
           "\n";
  }

  private void _genStackCheck (final boolean bVoidReturn)
  {
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    if (Options.hasDepthLimit ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          m_aCodeGenerator.genCodeLine ("if(++jj_depth > " + Options.getDepthLimit () + ") {");
          m_aCodeGenerator.genCodeLine ("  jj_consume_token(-1);");
          m_aCodeGenerator.genCodeLine ("  throw new ParseException();");
          m_aCodeGenerator.genCodeLine ("}");
          m_aCodeGenerator.genCodeLine ("try {");
          break;
        case CPP:
          if (!bVoidReturn)
          {
            m_aCodeGenerator.genCodeLine ("if(jj_depth_error){ return __ERROR_RET__; }");
          }
          else
          {
            m_aCodeGenerator.genCodeLine ("if(jj_depth_error){ return; }");
          }
          m_aCodeGenerator.genCodeLine ("__jj_depth_inc __jj_depth_counter(this);");
          m_aCodeGenerator.genCodeLine ("if(jj_depth > " + Options.getDepthLimit () + ") {");
          m_aCodeGenerator.genCodeLine ("  jj_depth_error = true;");
          m_aCodeGenerator.genCodeLine ("  jj_consume_token(-1);");
          m_aCodeGenerator.genCodeLine ("  errorHandler->handleParseError(token, getToken(1), __FUNCTION__, this), hasError = true;");
          if (!bVoidReturn)
          {
            // Non-recoverable error
            m_aCodeGenerator.genCodeLine ("  return __ERROR_RET__;");
          }
          else
          {
            // Non-recoverable error
            m_aCodeGenerator.genCodeLine ("  return;");
          }
          m_aCodeGenerator.genCodeLine ("}");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
  }

  void genStackCheckEnd ()
  {
    if (Options.hasDepthLimit ())
    {
      final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
      switch (eOutputLanguage)
      {
        case JAVA:
          m_aCodeGenerator.genCodeLine (" } finally {");
          m_aCodeGenerator.genCodeLine ("   --jj_depth;");
          m_aCodeGenerator.genCodeLine (" }");
          break;
        case CPP:
          // Nothing;
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
  }

  void buildPhase1Routine (@NonNull final BNFProduction p)
  {
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    Token t = p.getReturnTypeTokens ().get (0);
    boolean bVoidReturn = false;
    if (t.kind == JavaCCParserConstants.VOID)
    {
      bVoidReturn = true;
    }
    String sError_ret_cpp = null;
    switch (eOutputLanguage)
    {
      case JAVA:
        m_aCodeGenerator.printTokenSetup (t);
        grammar ().setCurrentColumn (1);
        m_aCodeGenerator.printLeadingComments (t);
        m_aCodeGenerator.genCode ("  final " + (p.getAccessMod () != null ? p.getAccessMod () : "public") + " ");
        grammar ().setCurrentLine (t.beginLine);
        grammar ().setCurrentColumn (t.beginColumn);
        m_aCodeGenerator.printTokenOnly (t);
        for (int i = 1; i < p.getReturnTypeTokens ().size (); i++)
        {
          t = p.getReturnTypeTokens ().get (i);
          m_aCodeGenerator.printToken (t);
        }
        m_aCodeGenerator.printTrailingComments (t);
        m_aCodeGenerator.genCode (" " + p.getLhs () + "(");
        if (!p.getParameterListTokens ().isEmpty ())
        {
          m_aCodeGenerator.printTokenSetup ((p.getParameterListTokens ().get (0)));
          for (final Token aElement : p.getParameterListTokens ())
          {
            t = aElement;
            m_aCodeGenerator.printToken (t);
          }
          m_aCodeGenerator.printTrailingComments (t);
        }
        m_aCodeGenerator.genCode (")");
        m_aCodeGenerator.genCode (" throws ParseException");

        for (final List <Token> name : p.getThrowsList ())
        {
          m_aCodeGenerator.genCode (", ");
          for (final Token t2 : name)
            m_aCodeGenerator.genCode (t2.image);
        }
        break;
      case CPP:
        sError_ret_cpp = _generateCppMethodHeader (p, t);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    m_aCodeGenerator.genCode (" {");

    switch (eOutputLanguage)
    {
      case JAVA:
        // Nothing
        break;
      case CPP:
        if ((Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR) && sError_ret_cpp != null) ||
            (Options.hasDepthLimit () && !bVoidReturn))
        {
          m_aCodeGenerator.genCode (sError_ret_cpp);
        }
        else
        {
          sError_ret_cpp = null;
        }
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    _genStackCheck (bVoidReturn);

    m_nIndentCount = 4;
    if (Options.isDebugParser ())
    {
      m_aCodeGenerator.genCodeNewLine ();
      for (final String sLine : _lang ().getTraceEnterLines (JavaCCGlobals.addUnicodeEscapes (p.getLhs ())))
        m_aCodeGenerator.genCodeLine (sLine);
      m_aCodeGenerator.genCodeLine ("    try {");
      m_nIndentCount += 2;
    }

    if (!Options.booleanValue (Options.USEROPTION__CPP_IGNORE_ACTIONS) && !p.getDeclarationTokens ().isEmpty ())
    {
      m_aCodeGenerator.printTokenSetup (p.getDeclarationTokens ().get (0));
      grammar ().decCurrentLine ();
      for (final Token aElement : p.getDeclarationTokens ())
      {
        t = aElement;
        m_aCodeGenerator.printToken (t);
      }
      m_aCodeGenerator.printTrailingComments (t);
    }

    final String sCode = _phase1ExpansionGen (p.getExpansion ());
    dumpFormattedString (sCode);
    m_aCodeGenerator.genCodeNewLine ();

    if (p.isJumpPatched () && !bVoidReturn)
    {
      m_aCodeGenerator.genCodeLine (_lang ().getMissingReturnStatement ());
    }
    if (Options.isDebugParser ())
    {
      for (final String sLine : _lang ().getTraceExitLines (JavaCCGlobals.addUnicodeEscapes (p.getLhs ())))
        m_aCodeGenerator.genCodeLine (sLine);
    }
    if (!bVoidReturn)
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          // Nothing
          break;
        case CPP:
          m_aCodeGenerator.genCodeLine ("assert(false);");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    if (sError_ret_cpp != null)
    {
      m_aCodeGenerator.genCodeLine ("\n#undef __ERROR_RET__\n");
    }
    genStackCheckEnd ();
    m_aCodeGenerator.genCodeLine ("}");
    m_aCodeGenerator.genCodeNewLine ();
  }

  void phase1NewLine ()
  {
    m_aCodeGenerator.genCodeNewLine ();
    m_aCodeGenerator.genCode (StringHelper.getRepeated (' ', m_nIndentCount));
  }

  private String _phase1ExpansionGen (@NonNull final Expansion e)
  {
    String sRetval = "";
    Token t = null;
    ExpLookahead [] aConds;
    String [] aActions;
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    final GrammarState aGrammar = grammar ();
    if (e instanceof final AbstractExpRegularExpression e_nrw)
    {
      sRetval += "\n";
      if (!e_nrw.getLhsTokens ().isEmpty ())
      {
        m_aCodeGenerator.printTokenSetup (e_nrw.getLhsTokens ().get (0));
        for (final Token aElement : e_nrw.getLhsTokens ())
        {
          t = aElement;
          sRetval += m_aCodeGenerator.getStringToPrint (t);
        }
        sRetval += m_aCodeGenerator.getTrailingComments (t);
        sRetval += " = ";
      }
      final String sTail;
      if (e_nrw.getRhsToken () == null)
        sTail = ");";
      else
        sTail = ")" + _lang ().getMemberAccess () + e_nrw.getRhsToken ().image + ";";

      if (e_nrw.hasLabel ())
      {
        sRetval += "jj_consume_token(" + e_nrw.getLabel () + sTail;
      }
      else
      {
        final String sLabel = aGrammar.namesOfTokens ().get (Integer.valueOf (e_nrw.getOrdinal ()));
        if (sLabel != null)
        {
          sRetval += "jj_consume_token(" + sLabel + sTail;
        }
        else
        {
          sRetval += "jj_consume_token(" + e_nrw.getOrdinal () + sTail;
        }
      }

      switch (eOutputLanguage)
      {
        case JAVA:
          // Nothing
          break;
        case CPP:
          if (Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR))
          {
            sRetval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
          }
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    else
      if (e instanceof final ExpNonTerminal e_nrw)
      {
        sRetval += "\n";
        if (e_nrw.getLhsTokenCount () != 0)
        {
          m_aCodeGenerator.printTokenSetup (e_nrw.getLhsTokenAt (0));
          for (final Token aElement : e_nrw.getLhsTokens ())
          {
            t = aElement;
            sRetval += m_aCodeGenerator.getStringToPrint (t);
          }
          sRetval += m_aCodeGenerator.getTrailingComments (t);
          sRetval += " = ";
        }
        sRetval += e_nrw.getName () + "(";
        if (e_nrw.getArgumentTokenCount () != 0)
        {
          m_aCodeGenerator.printTokenSetup (e_nrw.getArgumentTokenAt (0));
          for (final Token aElement : e_nrw.getArgumentTokens ())
          {
            t = aElement;
            sRetval += m_aCodeGenerator.getStringToPrint (t);
          }
          sRetval += m_aCodeGenerator.getTrailingComments (t);
        }
        sRetval += ");";
        switch (eOutputLanguage)
        {
          case JAVA:
            // Nothing
            break;
          case CPP:
            if (Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR))
            {
              sRetval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
            }
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }
      }
      else
        if (e instanceof final ExpAction e_nrw)
        {
          sRetval += INDENT_OFF + "\n";
          if (!Options.booleanValue (Options.USEROPTION__CPP_IGNORE_ACTIONS) && !e_nrw.getActionTokens ().isEmpty ())
          {
            m_aCodeGenerator.printTokenSetup (e_nrw.getActionTokens ().get (0));
            aGrammar.setCurrentColumn (1);
            for (final Token aElement : e_nrw.getActionTokens ())
            {
              t = aElement;
              sRetval += m_aCodeGenerator.getStringToPrint (t);
            }
            sRetval += m_aCodeGenerator.getTrailingComments (t);
          }
          sRetval += INDENT_ON;
        }
        else
          if (e instanceof final ExpChoice e_nrw)
          {
            aConds = new ExpLookahead [e_nrw.getChoiceCount ()];
            aActions = new String [e_nrw.getChoiceCount () + 1];

            String sChoice;
            switch (eOutputLanguage)
            {
              case JAVA:
                sChoice = "\n" + "jj_consume_token(-1);\n" + "throw new ParseException();";
                break;
              case CPP:
                sChoice = "\n" +
                          "jj_consume_token(-1);\n" +
                          "errorHandler->handleParseError(token, getToken(1), __FUNCTION__, this), hasError = true;" +
                          (Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR) ? "return __ERROR_RET__;\n"
                                                                                              : "");
                break;
              default:
                throw new UnsupportedOutputLanguageException (eOutputLanguage);
            }
            aActions[e_nrw.getChoiceCount ()] = sChoice;

            // In previous line, the "throw" never throws an exception since the
            // evaluation of jj_consume_token(-1) causes ParseException to be
            // thrown first.
            for (int i = 0; i < e_nrw.getChoiceCount (); i++)
            {
              final ExpSequence aNestedSeq = (ExpSequence) e_nrw.getChoiceAt (i);
              aActions[i] = _phase1ExpansionGen (aNestedSeq);
              aConds[i] = (ExpLookahead) aNestedSeq.getUnitAt (0);
            }
            sRetval = buildLookaheadChecker (aConds, aActions);
          }
          else
            if (e instanceof final ExpSequence e_nrw)
            {
              // We skip the first element in the following iteration since it
              // is the
              // Lookahead object.
              for (int i = 1; i < e_nrw.getUnitCount (); i++)
              {
                // For C++, since we are not using exceptions, we will protect
                // all the
                // expansion choices with if (!error)
                boolean bWrap_in_block = false;
                if (!aGrammar.isJJTreeGenerated ())
                {
                  switch (eOutputLanguage)
                  {
                    case JAVA:
                      // nothing
                      break;
                    case CPP:
                      // for the last one, if it's an action, we will not
                      // protect it.
                      final Expansion aElem = e_nrw.getUnitAt (i);
                      if (!(aElem instanceof ExpAction) ||
                          !(e.getParent () instanceof BNFProduction) ||
                          i != e_nrw.getUnitCount () - 1)
                      {
                        bWrap_in_block = true;
                        sRetval += "\nif (!hasError) {";
                      }
                      break;
                    default:
                      throw new UnsupportedOutputLanguageException (eOutputLanguage);
                  }
                }
                sRetval += _phase1ExpansionGen (e_nrw.getUnitAt (i));
                if (bWrap_in_block)
                {
                  sRetval += "\n}";
                }
              }
            }
            else
              if (e instanceof final ExpOneOrMore e_nrw)
              {
                final Expansion aNested_e = e_nrw.getExpansion ();
                ExpLookahead aLa;
                if (aNested_e instanceof final ExpSequence aSequence)
                {
                  aLa = (ExpLookahead) (aSequence.getUnitAt (0));
                }
                else
                {
                  aLa = new ExpLookahead ();
                  aLa.setAmount (Options.getLookahead ());
                  aLa.setLaExpansion (aNested_e);
                }
                sRetval += "\n";
                final int nLabelIndex = ++m_nGenSymbolIndex;
                sRetval += _lang ().getLoopStart (nLabelIndex) + INDENT_INC;
                sRetval += _phase1ExpansionGen (aNested_e);
                aConds = new ExpLookahead [1];
                aConds[0] = aLa;
                aActions = new String [2];
                // [ph] empty statement needed???
                aActions[0] = true ? "" : "\n;";

                aActions[1] = _lang ().getLoopBreak (nLabelIndex);

                sRetval += buildLookaheadChecker (aConds, aActions);
                sRetval += INDENT_DEC + "\n" + "}";

                sRetval += _lang ().getLoopEnd (nLabelIndex);
              }
              else
                if (e instanceof final ExpZeroOrMore e_nrw)
                {
                  final Expansion aNested_e = e_nrw.getExpansion ();
                  ExpLookahead aLa;
                  if (aNested_e instanceof final ExpSequence aExpSequence)
                  {
                    aLa = (ExpLookahead) (aExpSequence.getUnitAt (0));
                  }
                  else
                  {
                    aLa = new ExpLookahead ();
                    aLa.setAmount (Options.getLookahead ());
                    aLa.setLaExpansion (aNested_e);
                  }
                  sRetval += "\n";
                  final int nLabelIndex = ++m_nGenSymbolIndex;
                  sRetval += _lang ().getLoopStart (nLabelIndex) + INDENT_INC;

                  aConds = new ExpLookahead [1];
                  aConds[0] = aLa;
                  aActions = new String [2];
                  // [ph] empty statement needed???
                  aActions[0] = true ? "" : "\n;";

                  aActions[1] = _lang ().getLoopBreak (nLabelIndex);

                  sRetval += buildLookaheadChecker (aConds, aActions);
                  sRetval += _phase1ExpansionGen (aNested_e);
                  sRetval += INDENT_DEC + "\n" + "}";

                  sRetval += _lang ().getLoopEnd (nLabelIndex);
                }
                else
                  if (e instanceof final ExpZeroOrOne e_nrw)
                  {
                    final Expansion aNested_e = e_nrw.getExpansion ();
                    ExpLookahead aLa;
                    if (aNested_e instanceof final ExpSequence aSequence)
                    {
                      aLa = (ExpLookahead) (aSequence.getUnitAt (0));
                    }
                    else
                    {
                      aLa = new ExpLookahead ();
                      aLa.setAmount (Options.getLookahead ());
                      aLa.setLaExpansion (aNested_e);
                    }
                    aConds = new ExpLookahead [1];
                    aConds[0] = aLa;
                    aActions = new String [2];
                    aActions[0] = _phase1ExpansionGen (aNested_e);
                    // Empty statement is relevant for Lookup!
                    aActions[1] = "\n;";
                    sRetval += buildLookaheadChecker (aConds, aActions);
                  }
                  else
                    if (e instanceof final ExpTryBlock e_nrw)
                    {
                      final Expansion aNested_e = e_nrw.getExp ();
                      List <Token> aList;
                      sRetval += "\n";
                      sRetval += "try {" + INDENT_INC;
                      sRetval += _phase1ExpansionGen (aNested_e);
                      sRetval += INDENT_DEC + "\n" + "}";
                      for (int i = 0; i < e_nrw.getCatchblks ().size (); i++)
                      {
                        sRetval += " catch (";
                        aList = e_nrw.getTypes ().get (i);
                        if (!aList.isEmpty ())
                        {
                          m_aCodeGenerator.printTokenSetup (aList.get (0));
                          for (final Token aElement : aList)
                          {
                            t = aElement;
                            sRetval += m_aCodeGenerator.getStringToPrint (t);
                          }
                          sRetval += m_aCodeGenerator.getTrailingComments (t);
                        }
                        sRetval += " ";
                        t = e_nrw.getIds ().get (i);
                        m_aCodeGenerator.printTokenSetup (t);
                        sRetval += m_aCodeGenerator.getStringToPrint (t);
                        sRetval += m_aCodeGenerator.getTrailingComments (t);
                        sRetval += ") {" + INDENT_OFF + "\n";
                        aList = e_nrw.getCatchblks ().get (i);
                        if (!aList.isEmpty ())
                        {
                          m_aCodeGenerator.printTokenSetup (aList.get (0));
                          aGrammar.setCurrentColumn (1);
                          for (final Token aElement : aList)
                          {
                            t = aElement;
                            sRetval += m_aCodeGenerator.getStringToPrint (t);
                          }
                          sRetval += m_aCodeGenerator.getTrailingComments (t);
                        }
                        sRetval += INDENT_ON + "\n" + "}";
                      }
                      if (e_nrw.getFinallyblk () != null)
                      {
                        // Both languages emit the same thing here - C++ gets a "finally" block
                        // that its own runtime header defines
                        sRetval += " finally {" + INDENT_OFF + "\n";

                        if (!e_nrw.getFinallyblk ().isEmpty ())
                        {
                          m_aCodeGenerator.printTokenSetup (e_nrw.getFinallyblk ().get (0));
                          aGrammar.setCurrentColumn (1);
                          for (final Token aElement : e_nrw.getFinallyblk ())
                          {
                            t = aElement;
                            sRetval += m_aCodeGenerator.getStringToPrint (t);
                          }
                          sRetval += m_aCodeGenerator.getTrailingComments (t);
                        }
                        sRetval += INDENT_ON + "\n" + "}";
                      }
                    }
    return sRetval;
  }

  private void _buildPhase2Routine (@NonNull final ExpLookahead aLa)
  {
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    final Expansion e = aLa.getLaExpansion ();
    m_aCodeGenerator.genCodeLine (_lang ().getLookaheadEntryDeclaration (e.getInternalName ()));
    m_aCodeGenerator.genCodeLine (" {");
    m_aCodeGenerator.genCodeLine ("    jj_la = xla;");
    m_aCodeGenerator.genCodeLine ("    jj_scanpos = token;");
    m_aCodeGenerator.genCodeLine ("    jj_lastpos = token;");

    String sRet_suffix = "";
    if (Options.hasDepthLimit ())
    {
      sRet_suffix = " && !jj_depth_error";
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        m_aCodeGenerator.genCodeLine ("    try { return (!jj_3" + e.getInternalName () + "()" + sRet_suffix + "); }");
        m_aCodeGenerator.genCodeLine ("    catch(LookaheadSuccess ls) { return true; }");
        break;
      case CPP:
        m_aCodeGenerator.genCodeLine ("    jj_done = false;");
        m_aCodeGenerator.genCodeLine ("    return (!jj_3" +
                                      e.getInternalName () +
                                      "() || jj_done)" +
                                      sRet_suffix +
                                      ";");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    if (Options.isErrorReporting ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          m_aCodeGenerator.genCodeLine ("    finally { jj_save(" + (e.getInternalIndex () - 1) + ", xla); }");
          break;
        case CPP:
          m_aCodeGenerator.genCodeLine (" { jj_save(" + (e.getInternalIndex () - 1) + ", xla); }");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    m_aCodeGenerator.genCodeLine ("  }");
    m_aCodeGenerator.genCodeNewLine ();
    final Phase3Data aP3d = new Phase3Data (e, aLa.getAmount ());
    m_aPhase3list.add (aP3d);
    m_aPhase3table.put (e, aP3d);
  }

  private boolean m_bXspDeclared;

  private Expansion m_aJj3Expansion;

  private String _genReturn (final boolean bValue)
  {
    final String sRetval = (bValue ? "true" : "false");
    if (Options.isDebugLookahead () && m_aJj3Expansion != null)
    {
      String sTracecode = "trace_return(\"" +
                          JavaCCGlobals.addUnicodeEscapes (((AbstractNormalProduction) m_aJj3Expansion.getParent ()).getLhs ()) +
                          "(LOOKAHEAD " +
                          (bValue ? "FAILED" : "SUCCEEDED") +
                          ")\");";
      if (Options.isErrorReporting ())
      {
        sTracecode = "if (!jj_rescan) " + sTracecode;
      }
      return "{ " + sTracecode + " return " + sRetval + "; }";
    }
    return "return " + sRetval + ";";
  }

  private void _generate3R (@NonNull final Expansion e, @NonNull final Phase3Data aInf)
  {
    Expansion aSeq = e;
    if (e.hasNoInternalName ())
    {
      while (true)
      {
        if (aSeq instanceof final ExpSequence aSequence && aSequence.getUnitCount () == 2)
        {
          aSeq = aSequence.getUnitAt (1);
        }
        else
          if (aSeq instanceof final ExpNonTerminal e_nrw)
          {
            final AbstractNormalProduction aNtprod = (grammar ().productionTable ().get (e_nrw.getName ()));
            if (aNtprod instanceof AbstractCodeProduction)
            {
              // nothing to do here
              break;
            }
            aSeq = aNtprod.getExpansion ();
          }
          else
            break;
      }

      if (aSeq instanceof final AbstractExpRegularExpression aAbstractExpRegularExpression)
      {
        e.setInternalNameOnly ("jj_scan_token(" + aAbstractExpRegularExpression.getOrdinal () + ")");
        return;
      }

      m_nGenSymbolIndex++;
      e.setInternalName ("R_", m_nGenSymbolIndex);
    }
    Phase3Data aP3d = (m_aPhase3table.get (e));
    if (aP3d == null || aP3d.count () < aInf.count ())
    {
      aP3d = new Phase3Data (e, aInf.count ());
      m_aPhase3list.add (aP3d);
      m_aPhase3table.put (e, aP3d);
    }
  }

  void setupPhase3Builds (@NonNull final Phase3Data aInf)
  {
    final Expansion e = aInf.exp ();
    if (e instanceof AbstractExpRegularExpression)
    {
      // nothing to here
    }
    else
      if (e instanceof final ExpNonTerminal e_nrw)
      {
        // All expansions of non-terminals have the "name" fields set. So
        // there's no need to check it below for "e_nrw" and "ntexp". In
        // fact, we rely here on the fact that the "name" fields of both these
        // variables are the same.
        final AbstractNormalProduction aNtprod = (grammar ().productionTable ().get (e_nrw.getName ()));
        if (aNtprod instanceof AbstractCodeProduction)
        {
          // nothing to do here
        }
        else
        {
          _generate3R (aNtprod.getExpansion (), aInf);
        }
      }
      else
        if (e instanceof final ExpChoice e_nrw)
        {
          for (final Expansion element : e_nrw.getChoices ())
          {
            _generate3R ((element), aInf);
          }
        }
        else
          if (e instanceof final ExpSequence e_nrw)
          {
            // We skip the first element in the following iteration since it is
            // the
            // Lookahead object.
            int nCnt = aInf.count ();
            for (int i = 1; i < e_nrw.getUnitCount (); i++)
            {
              final Expansion aEseq = (e_nrw.getUnitAt (i));
              setupPhase3Builds (new Phase3Data (aEseq, nCnt));
              nCnt -= minimumSize (aEseq);
              if (nCnt <= 0)
                break;
            }
          }
          else
            if (e instanceof final ExpTryBlock e_nrw)
            {
              setupPhase3Builds (new Phase3Data (e_nrw.getExp (), aInf.count ()));
            }
            else
              if (e instanceof final ExpOneOrMore e_nrw)
              {
                _generate3R (e_nrw.getExpansion (), aInf);
              }
              else
                if (e instanceof final ExpZeroOrMore e_nrw)
                {
                  _generate3R (e_nrw.getExpansion (), aInf);
                }
                else
                  if (e instanceof final ExpZeroOrOne e_nrw)
                  {
                    _generate3R (e_nrw.getExpansion (), aInf);
                  }
  }

  private String _getTypeForToken ()
  {
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    return switch (eOutputLanguage)
    {
      case JAVA -> "Token";
      case CPP -> "Token *";
    };
  }

  private String _genjj_3Call (@NonNull final Expansion e)
  {
    final String sInternalName = e.getInternalName ();
    if (sInternalName.startsWith ("jj_scan_token"))
      return sInternalName;
    return "jj_3" + sInternalName + "()";
  }

  void buildPhase3Routine (@NonNull final Phase3Data aInf, final boolean bRecursive_call)
  {
    final Expansion e = aInf.exp ();
    Token t = null;
    if (e.getInternalName ().startsWith ("jj_scan_token"))
      return;

    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    if (!bRecursive_call)
    {
      m_aCodeGenerator.genCodeLine (_lang ().getLookaheadScanDeclaration (e.getInternalName ()));

      m_aCodeGenerator.genCodeLine (" {");
      switch (eOutputLanguage)
      {
        case JAVA:
          break;
        case CPP:
          m_aCodeGenerator.genCodeLine ("    if (jj_done) return true;");
          if (Options.hasDepthLimit ())
            m_aCodeGenerator.genCodeLine ("#define __ERROR_RET__ true");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      _genStackCheck (false);
      m_bXspDeclared = false;
      if (Options.isDebugLookahead () && e.getParent () instanceof final AbstractNormalProduction aNormalProduction)
      {
        m_aCodeGenerator.genCode ("    ");
        if (Options.isErrorReporting ())
        {
          m_aCodeGenerator.genCode ("if (!jj_rescan) ");
        }
        m_aCodeGenerator.genCodeLine ("trace_call(\"" +
                                      JavaCCGlobals.addUnicodeEscapes (aNormalProduction.getLhs ()) +
                                      "(LOOKING AHEAD...)\");");
        m_aJj3Expansion = e;
      }
      else
      {
        m_aJj3Expansion = null;
      }
    }
    if (e instanceof final AbstractExpRegularExpression e_nrw)
    {
      if (e_nrw.hasLabel ())
      {
        m_aCodeGenerator.genCodeLine ("    if (jj_scan_token(" + e_nrw.getLabel () + ")) " + _genReturn (true));
      }
      else
      {
        final Object aLabel = grammar ().namesOfTokens ().get (Integer.valueOf (e_nrw.getOrdinal ()));
        if (aLabel != null)
        {
          m_aCodeGenerator.genCodeLine ("    if (jj_scan_token(" + (String) aLabel + ")) " + _genReturn (true));
        }
        else
        {
          m_aCodeGenerator.genCodeLine ("    if (jj_scan_token(" + e_nrw.getOrdinal () + ")) " + _genReturn (true));
        }
      }
      // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos)
      // " + genReturn(false));
    }
    else
      if (e instanceof final ExpNonTerminal e_nrw)
      {
        // All expansions of non-terminals have the "name" fields set. So
        // there's no need to check it below for "e_nrw" and "ntexp". In
        // fact, we rely here on the fact that the "name" fields of both these
        // variables are the same.
        final AbstractNormalProduction aNtprod = (grammar ().productionTable ().get (e_nrw.getName ()));
        if (aNtprod instanceof AbstractCodeProduction)
        {
          m_aCodeGenerator.genCodeLine ("    if (true) { jj_la = 0; jj_scanpos = jj_lastpos; " +
                                        _genReturn (false) +
                                        "}");
        }
        else
        {
          final Expansion aNtexp = aNtprod.getExpansion ();
          // codeGenerator.genCodeLine(" if (jj_3" + ntexp.internal_name + "())
          // " + genReturn(true));
          m_aCodeGenerator.genCodeLine ("    if (" + _genjj_3Call (aNtexp) + ") " + _genReturn (true));
          // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
          // jj_lastpos) " + genReturn(false));
        }
      }
      else
        if (e instanceof final ExpChoice e_nrw)
        {
          ExpSequence aNested_seq;
          if (e_nrw.getChoiceCount () != 1)
          {
            if (!m_bXspDeclared)
            {
              m_bXspDeclared = true;
              m_aCodeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
            }
            m_aCodeGenerator.genCodeLine ("    xsp = jj_scanpos;");
          }
          for (int i = 0; i < e_nrw.getChoiceCount (); i++)
          {
            aNested_seq = (ExpSequence) (e_nrw.getChoiceAt (i));
            final ExpLookahead aLa = (ExpLookahead) (aNested_seq.getUnitAt (0));
            if (aLa.getActionTokens ().isNotEmpty ())
            {
              // We have semantic lookahead that must be evaluated.
              JavaCCGlobals.setLookAheadNeeded (true);
              m_aCodeGenerator.genCodeLine ("    jj_lookingAhead = true;");
              m_aCodeGenerator.genCode ("    jj_semLA = ");
              m_aCodeGenerator.printTokenSetup (aLa.getActionTokens ().getFirstOrNull ());
              for (final Token aElement : aLa.getActionTokens ())
              {
                t = aElement;
                m_aCodeGenerator.printToken (t);
              }
              m_aCodeGenerator.printTrailingComments (t);
              m_aCodeGenerator.genCodeLine (";");
              m_aCodeGenerator.genCodeLine ("    jj_lookingAhead = false;");
            }
            m_aCodeGenerator.genCode ("    if (");
            if (aLa.getActionTokens ().isNotEmpty ())
            {
              m_aCodeGenerator.genCode ("!jj_semLA || ");
            }
            if (i != e_nrw.getChoiceCount () - 1)
            {
              // codeGenerator.genCodeLine("jj_3" + nested_seq.internal_name +
              // "()) {");
              m_aCodeGenerator.genCodeLine (_genjj_3Call (aNested_seq) + ") {");
              m_aCodeGenerator.genCodeLine ("    jj_scanpos = xsp;");
            }
            else
            {
              // codeGenerator.genCodeLine("jj_3" + nested_seq.internal_name +
              // "()) " + genReturn(true));
              m_aCodeGenerator.genCodeLine (_genjj_3Call (aNested_seq) + ") " + _genReturn (true));
              // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
              // jj_lastpos) " + genReturn(false));
            }
          }
          for (int i = 1; i < e_nrw.getChoiceCount (); i++)
          {
            // codeGenerator.genCodeLine(" } else if (jj_la == 0 && jj_scanpos
            // == jj_lastpos) " + genReturn(false));
            m_aCodeGenerator.genCodeLine ("    }");
          }
        }
        else
          if (e instanceof final ExpSequence e_nrw)
          {
            // We skip the first element in the following iteration since it is
            // the Lookahead object.
            int nCnt = aInf.count ();
            for (int i = 1; i < e_nrw.getUnitCount (); i++)
            {
              final Expansion aEseq = e_nrw.getUnitAt (i);
              buildPhase3Routine (new Phase3Data (aEseq, nCnt), true);

              // Test Code
              if (false)
                PGPrinter.info ("minimumSize: line: " +
                                aEseq.getLineNumber () +
                                ", column: " +
                                aEseq.getColumnNumber () +
                                ": " +
                                minimumSize (aEseq));

              nCnt -= minimumSize (aEseq);
              if (nCnt <= 0)
                break;
            }
          }
          else
            if (e instanceof final ExpTryBlock e_nrw)
            {
              buildPhase3Routine (new Phase3Data (e_nrw.getExp (), aInf.count ()), true);
            }
            else
              if (e instanceof final ExpOneOrMore e_nrw)
              {
                if (!m_bXspDeclared)
                {
                  m_bXspDeclared = true;
                  m_aCodeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
                }
                final Expansion aNested_e = e_nrw.getExpansion ();
                // codeGenerator.genCodeLine(" if (jj_3" +
                // nested_e.internal_name + "()) " + genReturn(true));
                m_aCodeGenerator.genCodeLine ("    if (" + _genjj_3Call (aNested_e) + ") " + _genReturn (true));
                // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                // jj_lastpos) " + genReturn(false));
                m_aCodeGenerator.genCodeLine ("    while (true) {");
                m_aCodeGenerator.genCodeLine ("      xsp = jj_scanpos;");
                // codeGenerator.genCodeLine(" if (jj_3" +
                // nested_e.internal_name + "()) { jj_scanpos = xsp; break; }");
                m_aCodeGenerator.genCodeLine ("      if (" +
                                              _genjj_3Call (aNested_e) +
                                              ") { jj_scanpos = xsp; break; }");
                // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                // jj_lastpos) " + genReturn(false));
                m_aCodeGenerator.genCodeLine ("    }");
              }
              else
                if (e instanceof final ExpZeroOrMore e_nrw)
                {
                  if (!m_bXspDeclared)
                  {
                    m_bXspDeclared = true;
                    m_aCodeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
                  }
                  final Expansion aNested_e = e_nrw.getExpansion ();
                  m_aCodeGenerator.genCodeLine ("    while (true) {");
                  m_aCodeGenerator.genCodeLine ("      xsp = jj_scanpos;");
                  // codeGenerator.genCodeLine(" if (jj_3" +
                  // nested_e.internal_name + "()) { jj_scanpos = xsp; break;
                  // }");
                  m_aCodeGenerator.genCodeLine ("      if (" +
                                                _genjj_3Call (aNested_e) +
                                                ") { jj_scanpos = xsp; break; }");
                  // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                  // jj_lastpos) " + genReturn(false));
                  m_aCodeGenerator.genCodeLine ("    }");
                }
                else
                  if (e instanceof final ExpZeroOrOne e_nrw)
                  {
                    if (!m_bXspDeclared)
                    {
                      m_bXspDeclared = true;
                      m_aCodeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
                    }
                    final Expansion aNested_e = e_nrw.getExpansion ();
                    m_aCodeGenerator.genCodeLine ("    xsp = jj_scanpos;");
                    // codeGenerator.genCodeLine(" if (jj_3" +
                    // nested_e.internal_name + "()) jj_scanpos = xsp;");
                    m_aCodeGenerator.genCodeLine ("    if (" + _genjj_3Call (aNested_e) + ") jj_scanpos = xsp;");
                    // codeGenerator.genCodeLine(" else if (jj_la == 0 &&
                    // jj_scanpos == jj_lastpos) " + genReturn(false));
                  }
    if (!bRecursive_call)
    {
      m_aCodeGenerator.genCodeLine ("    " + _genReturn (false));
      genStackCheckEnd ();
      switch (eOutputLanguage)
      {
        case JAVA:
          // nothing;
          break;
        case CPP:
          if (Options.hasDepthLimit ())
          {
            m_aCodeGenerator.genCodeLine ("#undef __ERROR_RET__");
          }
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      m_aCodeGenerator.genCodeLine ("  }");
      m_aCodeGenerator.genCodeNewLine ();
    }
  }

  int minimumSize (final Expansion e)
  {
    return minimumSize (e, Integer.MAX_VALUE);
  }

  /*
   * Returns the minimum number of tokens that can parse to this expansion.
   */
  int minimumSize (@NonNull final Expansion e, final int nOldMin)
  {
    if (e.isInMinimumSize ())
    {
      // recursive search for minimum size unnecessary.
      return Integer.MAX_VALUE;
    }
    e.setInMinimumSize (true);
    try
    {
      if (e instanceof AbstractExpRegularExpression)
        return 1;

      if (e instanceof final ExpNonTerminal e_nrw)
      {
        final AbstractNormalProduction aNtprod = (grammar ().productionTable ().get (e_nrw.getName ()));
        if (aNtprod instanceof AbstractCodeProduction)
        {
          return Integer.MAX_VALUE;
          // Make caller think this is unending (for we do not go beyond
          // JAVACODE during
          // phase3 execution).
        }
        final Expansion aNtexp = aNtprod.getExpansion ();
        return minimumSize (aNtexp);
      }

      if (e instanceof final ExpChoice e_nrw)
      {
        int nMin = nOldMin;
        Expansion aNested_e;
        for (int i = 0; nMin > 1 && i < e_nrw.getChoiceCount (); i++)
        {
          aNested_e = (e_nrw.getChoiceAt (i));
          final int nMin1 = minimumSize (aNested_e, nMin);
          if (nMin > nMin1)
            nMin = nMin1;
        }
        return nMin;
      }

      if (e instanceof final ExpSequence e_nrw)
      {
        int nMin = 0;
        // We skip the first element in the following iteration since it
        // is
        // the
        // Lookahead object.
        for (int i = 1; i < e_nrw.getUnitCount (); i++)
        {
          final Expansion aEseq = (e_nrw.getUnitAt (i));
          final int nMineseq = minimumSize (aEseq);
          if (nMin == Integer.MAX_VALUE || nMineseq == Integer.MAX_VALUE)
          {
            // Adding infinity to something
            nMin = Integer.MAX_VALUE;
            // results
            // in infinity.
          }
          else
          {
            nMin += nMineseq;
            if (nMin > nOldMin)
              break;
          }
        }
        return nMin;
      }

      if (e instanceof final ExpTryBlock e_nrw)
      {
        return minimumSize (e_nrw.getExp ());
      }

      if (e instanceof final ExpOneOrMore e_nrw)
      {
        return minimumSize (e_nrw.getExpansion ());
      }

      if (e instanceof ExpZeroOrMore)
        return 0;

      if (e instanceof ExpZeroOrOne)
        return 0;

      if (e instanceof ExpLookahead)
        return 0;

      if (e instanceof ExpAction)
        return 0;

      PGPrinter.warn ("Found unsupported Expansion - " + e);
      return 0;
    }
    finally
    {
      e.setInMinimumSize (false);
    }
  }

  /**
   * Write the parser methods of every production.
   *
   * @param aCodeGenerator
   *        The generator to write to. May not be <code>null</code>.
   */
  public void build (@NonNull final AbstractCodeGenerator aCodeGenerator)
  {
    m_aCodeGenerator = aCodeGenerator;
    final EOutputLanguage eOutputLanguage = m_aCodeGenerator.getOutputLanguage ();
    final GrammarState aGrammar = grammar ();
    for (final AbstractNormalProduction p : aGrammar.bnfProductions ())
    {
      if (p instanceof final CodeProductionCpp cp)
      {
        if (!eOutputLanguage.isJava ())
        {
          JavaCCErrors.semanticError ("Cannot use JAVACODE productions with non-Java output.");
          continue;
        }

        _generateCppMethodHeader (cp);

        if (false)
        {
          Token t = (cp.getReturnTypeTokens ().get (0));
          aCodeGenerator.printTokenSetup (t);
          aGrammar.setCurrentColumn (1);
          aCodeGenerator.printLeadingComments (t);
          aCodeGenerator.genCode (" " + (p.getAccessMod () != null ? p.getAccessMod () + " " : ""));
          aGrammar.setCurrentLine (t.beginLine);
          aGrammar.setCurrentColumn (t.beginColumn);
          aCodeGenerator.printTokenOnly (t);
          for (int i = 1; i < cp.getReturnTypeTokens ().size (); i++)
          {
            t = (cp.getReturnTypeTokens ().get (i));
            aCodeGenerator.printToken (t);
          }
          aCodeGenerator.printTrailingComments (t);
          aCodeGenerator.genCode (" " + cp.getLhs () + "(");
          if (!cp.getParameterListTokens ().isEmpty ())
          {
            aCodeGenerator.printTokenSetup (cp.getParameterListTokens ().get (0));
            for (final Token aElement : cp.getParameterListTokens ())
            {
              t = aElement;
              aCodeGenerator.printToken (t);
            }
            aCodeGenerator.printTrailingComments (t);
          }
          aCodeGenerator.genCode (")");
          for (final List <Token> aElement : cp.getThrowsList ())
          {
            aCodeGenerator.genCode (", ");
            for (final Token aElement2 : aElement)
            {
              t = aElement2;
              aCodeGenerator.genCode (t.image);
            }
          }
        }

        aCodeGenerator.genCodeLine (" {");
        if (Options.isDebugParser ())
        {
          aCodeGenerator.genCodeNewLine ();
          for (final String sLine : eOutputLanguage.getTraceEnterLines (JavaCCGlobals.addUnicodeEscapes (cp.getLhs ())))
            aCodeGenerator.genCodeLine (sLine);
          aCodeGenerator.genCodeLine ("    try {");

        }
        if (!cp.getCodeTokens ().isEmpty ())
        {
          aCodeGenerator.printTokenSetup (cp.getCodeTokens ().get (0));
          aGrammar.decCurrentLine ();
          aCodeGenerator.printTokenList (cp.getCodeTokens ());
        }
        aCodeGenerator.genCodeNewLine ();
        if (Options.isDebugParser ())
        {
          aCodeGenerator.genCodeLine ("    } catch(...) { }");
        }
        aCodeGenerator.genCodeLine ("  }");
        aCodeGenerator.genCodeNewLine ();
      }
      else
        if (p instanceof final CodeProductionJava jp)
        {
          if (!eOutputLanguage.isJava ())
          {
            JavaCCErrors.semanticError ("Cannot use JAVACODE productions with non-Java output.");
            continue;
          }
          Token t = jp.getReturnTypeTokens ().get (0);
          aCodeGenerator.printTokenSetup (t);
          aGrammar.setCurrentColumn (1);
          aCodeGenerator.printLeadingComments (t);
          aCodeGenerator.genCode ("  " + (p.getAccessMod () != null ? p.getAccessMod () + " " : ""));
          aGrammar.setCurrentLine (t.beginLine);
          aGrammar.setCurrentColumn (t.beginColumn);
          aCodeGenerator.printTokenOnly (t);
          for (int i = 1; i < jp.getReturnTypeTokens ().size (); i++)
          {
            t = jp.getReturnTypeTokens ().get (i);
            aCodeGenerator.printToken (t);
          }
          aCodeGenerator.printTrailingComments (t);
          aCodeGenerator.genCode (" " + jp.getLhs () + "(");
          if (!jp.getParameterListTokens ().isEmpty ())
          {
            aCodeGenerator.printTokenSetup (jp.getParameterListTokens ().get (0));
            for (final Token aElement2 : jp.getParameterListTokens ())
            {
              t = aElement2;
              aCodeGenerator.printToken (t);
            }
            aCodeGenerator.printTrailingComments (t);
          }
          aCodeGenerator.genCode (")");
          aCodeGenerator.genCode (eOutputLanguage.getThrowsClause ());
          for (final List <Token> aElement2 : jp.getThrowsList ())
          {
            aCodeGenerator.genCode (", ");
            for (final Token x : aElement2)
            {
              t = x;
              aCodeGenerator.genCode (t.image);
            }
          }
          aCodeGenerator.genCode (" {");
          if (Options.isDebugParser ())
          {
            aCodeGenerator.genCodeNewLine ();
            aCodeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (jp.getLhs ()) + "\");");
            aCodeGenerator.genCode ("    try {");
          }
          if (!jp.getCodeTokens ().isEmpty ())
          {
            aCodeGenerator.printTokenSetup ((jp.getCodeTokens ().get (0)));
            aGrammar.decCurrentLine ();
            aCodeGenerator.printTokenList (jp.getCodeTokens ());
          }
          aCodeGenerator.genCodeNewLine ();
          if (Options.isDebugParser ())
          {
            aCodeGenerator.genCodeLine ("    } finally {");
            aCodeGenerator.genCodeLine ("      trace_return(\"" +
                                        JavaCCGlobals.addUnicodeEscapes (jp.getLhs ()) +
                                        "\");");
            aCodeGenerator.genCodeLine ("    }");
          }
          aCodeGenerator.genCodeLine ("  }");
          aCodeGenerator.genCodeNewLine ();
        }
        else
        {
          buildPhase1Routine ((BNFProduction) p);
        }
    }

    aCodeGenerator.switchToIncludeFile ();

    for (final ExpLookahead element : m_aPhase2list)
    {
      _buildPhase2Routine (element);
    }

    int nPhase3index = 0;
    while (nPhase3index < m_aPhase3list.size ())
    {
      for (; nPhase3index < m_aPhase3list.size (); nPhase3index++)
      {
        setupPhase3Builds (m_aPhase3list.get (nPhase3index));
      }
    }

    for (final Phase3Data data : m_aPhase3table.values ())
    {
      buildPhase3Routine (data, false);
    }

    if (false)
    {
      for (final Phase3Data inf : m_aPhase3table.values ())
      {
        PGPrinter.info ("**** Table for: " + inf.exp ().getInternalName ());
        buildPhase3TableRec (inf);
        PGPrinter.info ("**** END TABLE *********");
      }
    }

    aCodeGenerator.switchToMainFile ();
  }

  /**
   * Forget everything, ready for the next grammar.
   */
  public void reInit ()
  {
    m_nGenSymbolIndex = 0;
    m_nIndentCount = 0;
    m_bJJ2LA = false;
    m_aPhase2list.clear ();
    m_aPhase3list.clear ();
    m_aPhase3table.clear ();
    m_aFirstSet = null;
    m_bXspDeclared = false;
    m_aJj3Expansion = null;
  }

  // Table driven.
  void buildPhase3TableRec (@NonNull final Phase3Data aInf)
  {
    final Expansion e = aInf.exp ();
    if (e instanceof final AbstractExpRegularExpression e_nrw)
    {
      PGPrinter.info ("TOKEN, " + e_nrw.getOrdinal ());
    }
    else
      if (e instanceof final ExpNonTerminal e_nrw)
      {
        final AbstractNormalProduction aNtprod = (grammar ().productionTable ().get (e_nrw.getName ()));
        if (aNtprod instanceof AbstractCodeProduction)
        {
          // javacode, true - always (warn?)
          PGPrinter.info ("JAVACODE_PROD, true");
        }
        else
        {
          final Expansion aNtexp = aNtprod.getExpansion ();
          // nt exp's table.
          PGPrinter.info ("PRODUCTION, " + aNtexp.getInternalIndex ());
          if (false)
            buildPhase3TableRec (new Phase3Data (aNtexp, aInf.count ()));
        }
      }
      else
        if (e instanceof final ExpChoice e_nrw)
        {
          PGPrinter.info ("CHOICE, ");
          for (int i = 0; i < e_nrw.getChoiceCount (); i++)
          {
            if (i > 0)
              PGPrinter.info ("\n|");
            final ExpSequence aNested_seq = (ExpSequence) (e_nrw.getChoiceAt (i));
            final ExpLookahead aLa = (ExpLookahead) (aNested_seq.getUnitAt (0));
            if (aLa.getActionTokens ().isNotEmpty ())
            {
              PGPrinter.info ("SEMANTIC,");
            }
            else
            {
              PGPrinter.info ("<start recurse>");
              buildPhase3TableRec (new Phase3Data (aNested_seq, aInf.count ()));
              PGPrinter.info ("<end recurse>");
            }
          }
          PGPrinter.info ();
        }
        else
          if (e instanceof final ExpSequence e_nrw)
          {
            int nCnt = aInf.count ();
            if (e_nrw.getUnitCount () > 2)
            {
              PGPrinter.info ("SEQ, " + nCnt);
              for (int i = 1; i < e_nrw.getUnitCount (); i++)
              {
                final Expansion aEseq = (e_nrw.getUnitAt (i));
                buildPhase3TableRec (new Phase3Data (aEseq, nCnt));
                nCnt -= minimumSize (aEseq);
                if (nCnt <= 0)
                  break;
              }
            }
            else
            {
              Expansion aTmp = e_nrw.getUnitAt (1);
              while (aTmp instanceof final ExpNonTerminal aNonTerminal)
              {
                final AbstractNormalProduction aNtprod = (grammar ().productionTable ()
                                                                    .get (aNonTerminal.getName ()));
                if (aNtprod instanceof AbstractCodeProduction)
                  break;
                aTmp = aNtprod.getExpansion ();
              }
              buildPhase3TableRec (new Phase3Data (aTmp, nCnt));
            }
            PGPrinter.info ();
          }
          else
            if (e instanceof final ExpTryBlock e_nrw)
            {
              buildPhase3TableRec (new Phase3Data (e_nrw.getExp (), aInf.count ()));
            }
            else
              if (e instanceof final ExpOneOrMore e_nrw)
              {
                PGPrinter.info ("SEQ PROD " + e_nrw.getExpansion ().getInternalIndex ());
                PGPrinter.info ("ZEROORMORE " + e_nrw.getExpansion ().getInternalIndex ());
              }
              else
                if (e instanceof final ExpZeroOrMore e_nrw)
                {
                  PGPrinter.info ("ZEROORMORE, " + e_nrw.getExpansion ().getInternalIndex ());
                }
                else
                  if (e instanceof final ExpZeroOrOne e_nrw)
                  {
                    PGPrinter.info ("ZERORONE, " + e_nrw.getExpansion ().getInternalIndex ());
                  }
                  else
                  {
                    assert (false);
                    // table for nested_e - optional
                  }
  }
}

/**
 * This class stores information to pass from phase 2 to phase 3.
 */
/**
 * One entry of the phase 3 work list: an expansion and how far ahead it may still look.
 *
 * @param exp
 *        The expansion to generate the jj3 method for. May not be <code>null</code>.
 * @param count
 *        The number of tokens that may still be consumed, which is what limits how many jj3 methods
 *        are generated.
 */
record Phase3Data (Expansion exp, int count)
{}
