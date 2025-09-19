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
// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2006, Sun Microsystems, Inc.
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

import static com.helger.pgcc.parser.JavaCCGlobals.BNF_PRODUCTIONS;
import static com.helger.pgcc.parser.JavaCCGlobals.MASK_VALS;
import static com.helger.pgcc.parser.JavaCCGlobals.NAMES_OF_TOKENS;
import static com.helger.pgcc.parser.JavaCCGlobals.PRODUCTION_TABLE;
import static com.helger.pgcc.parser.JavaCCGlobals.s_ccol;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cline;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jj2index;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskindex;
import static com.helger.pgcc.parser.JavaCCGlobals.s_tokenCount;

import java.util.HashSet;
import java.util.List;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.pgcc.PGPrinter;
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

import jakarta.annotation.Nonnull;

public class ParseEngine
{
  private int m_nGenSymbolIndex = 0;
  private int m_nIndentCount = 0;
  private boolean m_bJJ2LA = false;
  private CodeGenerator m_codeGenerator;

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
  private final ICommonsList <ExpLookahead> m_phase2list = new CommonsArrayList <> ();
  private final ICommonsList <Phase3Data> m_phase3list = new CommonsArrayList <> ();
  private final ICommonsMap <Expansion, Phase3Data> m_phase3table = new CommonsHashMap <> ();

  public ParseEngine ()
  {}

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
  private boolean _javaCodeCheck (final Expansion exp)
  {
    if (exp instanceof AbstractExpRegularExpression)
    {
      return false;
    }

    if (exp instanceof ExpNonTerminal)
    {
      final NormalProduction prod = ((ExpNonTerminal) exp).getProd ();
      if (prod instanceof AbstractCodeProduction)
        return true;
      return _javaCodeCheck (prod.getExpansion ());
    }

    if (exp instanceof ExpChoice)
    {
      final ExpChoice ch = (ExpChoice) exp;
      for (final Expansion choice : ch.getChoices ())
        if (_javaCodeCheck (choice))
          return true;
      return false;
    }

    if (exp instanceof ExpSequence)
    {
      final ExpSequence seq = (ExpSequence) exp;
      for (int i = 0; i < seq.getUnitCount (); i++)
      {
        final Expansion unit = seq.getUnitAt (i);
        if (unit instanceof ExpLookahead && ((ExpLookahead) unit).isExplicit ())
        {
          // An explicit lookahead (rather than one generated implicitly).
          // Assume
          // the user knows what he / she is doing, e.g.
          // "A" ( "B" | LOOKAHEAD("X") jcode() | "C" )* "D"
          return false;
        }
        if (_javaCodeCheck (unit))
          return true;
        if (!Semanticize.emptyExpansionExists (unit))
          return false;
      }
      return false;
    }

    if (exp instanceof ExpOneOrMore)
    {
      final ExpOneOrMore om = (ExpOneOrMore) exp;
      return _javaCodeCheck (om.getExpansion ());
    }

    if (exp instanceof ExpZeroOrMore)
    {
      final ExpZeroOrMore zm = (ExpZeroOrMore) exp;
      return _javaCodeCheck (zm.getExpansion ());
    }

    if (exp instanceof ExpZeroOrOne)
    {
      final ExpZeroOrOne zo = (ExpZeroOrOne) exp;
      return _javaCodeCheck (zo.getExpansion ());
    }

    if (exp instanceof ExpTryBlock)
    {
      final ExpTryBlock tb = (ExpTryBlock) exp;
      return _javaCodeCheck (tb.m_exp);
    }

    return false;
  }

  /**
   * An array used to store the first sets generated by the following method. A true entry means
   * that the corresponding token is in the first set.
   */
  private boolean [] m_firstSet;

  /**
   * Sets up the array "firstSet" above based on the Expansion argument passed to it. Since this is
   * a recursive function, it assumes that "firstSet" has been reset before the first call.
   */
  private void _genFirstSet (final Expansion exp)
  {
    if (exp instanceof AbstractExpRegularExpression)
    {
      m_firstSet[((AbstractExpRegularExpression) exp).getOrdinal ()] = true;
    }
    else
      if (exp instanceof ExpNonTerminal)
      {
        if (!(((ExpNonTerminal) exp).getProd () instanceof AbstractCodeProduction))
        {
          _genFirstSet ((((ExpNonTerminal) exp).getProd ()).getExpansion ());
        }
      }
      else
        if (exp instanceof ExpChoice)
        {
          final ExpChoice ch = (ExpChoice) exp;
          for (final Expansion element : ch.getChoices ())
          {
            _genFirstSet ((element));
          }
        }
        else
          if (exp instanceof ExpSequence)
          {
            final ExpSequence seq = (ExpSequence) exp;
            final Object obj = seq.getUnitAt (0);
            if (obj instanceof ExpLookahead && ((ExpLookahead) obj).getActionTokens ().isNotEmpty ())
            {
              m_bJJ2LA = true;
            }
            for (int i = 0; i < seq.getUnitCount (); i++)
            {
              final Expansion unit = seq.getUnitAt (i);
              // Javacode productions can not have FIRST sets. Instead we
              // generate the FIRST set
              // for the preceding LOOKAHEAD (the semantic checks should have
              // made sure that
              // the LOOKAHEAD is suitable).
              if (unit instanceof ExpNonTerminal &&
                  ((ExpNonTerminal) unit).getProd () instanceof AbstractCodeProduction)
              {
                if (i > 0 && seq.getUnitAt (i - 1) instanceof ExpLookahead)
                {
                  final ExpLookahead la = (ExpLookahead) seq.getUnitAt (i - 1);
                  _genFirstSet (la.getLaExpansion ());
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
            if (exp instanceof ExpOneOrMore)
            {
              final ExpOneOrMore om = (ExpOneOrMore) exp;
              _genFirstSet (om.getExpansion ());
            }
            else
              if (exp instanceof ExpZeroOrMore)
              {
                final ExpZeroOrMore zm = (ExpZeroOrMore) exp;
                _genFirstSet (zm.getExpansion ());
              }
              else
                if (exp instanceof ExpZeroOrOne)
                {
                  final ExpZeroOrOne zo = (ExpZeroOrOne) exp;
                  _genFirstSet (zo.getExpansion ());
                }
                else
                  if (exp instanceof ExpTryBlock)
                  {
                    final ExpTryBlock tb = (ExpTryBlock) exp;
                    _genFirstSet (tb.m_exp);
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
  private void _dumpLookaheads (final ExpLookahead [] conds, final String [] actions)
  {
    for (int i = 0; i < conds.length; i++)
    {
      PGPrinter.error ("Lookahead: " + i);
      PGPrinter.error (conds[i].dump (0, new HashSet <> ()).toString ());
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
  String buildLookaheadChecker (final ExpLookahead [] conds, final String [] actions)
  {
    // The state variables.
    EState eState = EState.NOOPENSTM;
    int indentAmt = 0;
    final boolean [] casedValues = new boolean [s_tokenCount];
    String retval = "";
    ExpLookahead la;
    Token t = null;
    final int tokenMaskSize = (s_tokenCount - 1) / 32 + 1;
    int [] tokenMask = null;
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();

    // Iterate over all the conditions.
    int index = 0;
    while (index < conds.length)
    {
      la = conds[index];
      m_bJJ2LA = false;

      if (la.getAmount () == 0 ||
          Semanticize.emptyExpansionExists (la.getLaExpansion ()) ||
          _javaCodeCheck (la.getLaExpansion ()))
      {

        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly
        // specified as 0).
        // . If it is possible for the lookahead expansion to recognize the
        // empty string - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it
        // directly expands to - in which case the lookahead trivially passes.
        if (la.getActionTokens ().isEmpty ())
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
            retval += "\n" + "if (";
            indentAmt++;
            break;
          case OPENIF:
            retval += INDENT_DEC + "\n" + "} else if (";
            break;
          case OPENSWITCH:
            retval += INDENT_DEC + "\n" + "default:" + INDENT_INC;
            if (Options.isErrorReporting ())
            {
              retval += "\njj_la1[" + s_maskindex + "] = jj_gen;";
              s_maskindex++;
            }
            MASK_VALS.add (tokenMask);
            retval += "\n" + "if (";
            indentAmt++;
            break;
          default:
            throw new IllegalStateException ();
        }
        m_codeGenerator.printTokenSetup (la.getActionTokens ().getFirstOrNull ());
        for (final Token aElement : la.getActionTokens ())
        {
          t = aElement;
          retval += m_codeGenerator.getStringToPrint (t);
        }
        retval += m_codeGenerator.getTrailingComments (t);
        retval += ") {" + INDENT_INC + actions[index];
        eState = EState.OPENIF;
      }
      else
        if (la.getAmount () == 1 && la.getActionTokens ().isEmpty ())
        {
          /*
           * Special optimal processing when the lookahead is exactly 1, and there is no semantic
           * lookahead.
           */
          if (m_firstSet == null)
          {
            m_firstSet = new boolean [s_tokenCount];
          }
          for (int i = 0; i < s_tokenCount; i++)
          {
            m_firstSet[i] = false;
          }
          /*
           * jj2LA is set to false at the beginning of the containing "if" statement. It is checked
           * immediately after the end of the same statement to determine if lookaheads are to be
           * performed using calls to the jj2 methods.
           */
          _genFirstSet (la.getLaExpansion ());
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
                retval += INDENT_DEC + "\n" + "} else {" + INDENT_INC;
                // Control flows through to next case.
                // $FALL-THROUGH$
              case NOOPENSTM:
                retval += "\n" + "switch (";
                if (Options.isCacheTokens ())
                {
                  switch (eOutputLanguage)
                  {
                    case JAVA:
                      retval += "jj_nt.kind";
                      break;
                    case CPP:
                      retval += "jj_nt->kind";
                      break;
                    default:
                      throw new UnsupportedOutputLanguageException (eOutputLanguage);
                  }
                }
                else
                  retval += "jj_ntk == -1 ? jj_ntk_f() : jj_ntk";
                retval += ") {" + INDENT_INC;
                for (int i = 0; i < s_tokenCount; i++)
                {
                  casedValues[i] = false;
                }
                indentAmt++;
                tokenMask = new int [tokenMaskSize];
                for (int i = 0; i < tokenMaskSize; i++)
                {
                  tokenMask[i] = 0;
                }
                break;
              case OPENSWITCH:
                // Don't need to do anything if state is OPENSWITCH.
                break;
              default:
                throw new IllegalStateException ();
            }
            for (int i = 0; i < s_tokenCount; i++)
            {
              if (m_firstSet[i] && !casedValues[i])
              {
                casedValues[i] = true;
                retval += INDENT_DEC + "\ncase ";

                final int j1 = i / 32;
                final int j2 = i % 32;
                tokenMask[j1] |= 1 << j2;
                final String s = NAMES_OF_TOKENS.get (Integer.valueOf (i));
                if (s == null)
                  retval += i;
                else
                  retval += s;
                retval += ":" + INDENT_INC;
              }
            }
            retval += "{";
            retval += actions[index];
            retval += "\nbreak;\n}";
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
            retval += "\nif (";
            indentAmt++;
            break;
          case OPENIF:
            retval += INDENT_DEC + "\n} else if (";
            break;
          case OPENSWITCH:
            retval += INDENT_DEC + "\ndefault:" + INDENT_INC;
            if (Options.isErrorReporting ())
            {
              retval += "\njj_la1[" + s_maskindex + "] = jj_gen;";
              s_maskindex++;
            }
            MASK_VALS.add (tokenMask);
            retval += "\nif (";
            indentAmt++;
            break;
          default:
            throw new IllegalStateException ();
        }

        final int nInternalIndex = ++s_jj2index;
        // At this point, la.la_expansion.internal_name must be "".
        assert la.getLaExpansion ().getInternalName ().equals ("");
        la.getLaExpansion ().setInternalName ("_", nInternalIndex);

        m_phase2list.add (la);
        retval += "jj_2" + la.getLaExpansion ().getInternalName () + "(" + la.getAmount () + ")";
        if (la.getActionTokens ().isNotEmpty ())
        {
          // In addition, there is also a semantic lookahead. So concatenate
          // the semantic check with the syntactic one.
          retval += " && (";
          m_codeGenerator.printTokenSetup (la.getActionTokens ().getFirstOrNull ());
          for (final Token aElement : la.getActionTokens ())
          {
            t = aElement;
            retval += m_codeGenerator.getStringToPrint (t);
          }
          retval += m_codeGenerator.getTrailingComments (t);
          retval += ")";
        }
        retval += ") {" + INDENT_INC + actions[index];
        eState = EState.OPENIF;
      }

      index++;
    }

    // Generate code for the default case. Note this may not
    // be the last entry of "actions" if any condition can be
    // statically determined to be always "true".

    switch (eState)
    {
      case NOOPENSTM:
        retval += actions[index];
        break;
      case OPENIF:
        retval += INDENT_DEC + "\n" + "} else {" + INDENT_INC + actions[index];
        break;
      case OPENSWITCH:
        retval += INDENT_DEC + "\n" + "default:" + INDENT_INC;
        if (Options.isErrorReporting ())
        {
          retval += "\njj_la1[" + s_maskindex + "] = jj_gen;";
          MASK_VALS.add (tokenMask);
          s_maskindex++;
        }
        retval += actions[index];
        break;
      default:
        throw new IllegalStateException ();
    }
    for (int i = 0; i < indentAmt; i++)
    {
      retval += INDENT_DEC + "\n}";
    }

    return retval;
  }

  void dumpFormattedString (final String str)
  {
    char ch = ' ';
    char prevChar;
    boolean indentOn = true;
    for (int i = 0; i < str.length (); i++)
    {
      prevChar = ch;
      ch = str.charAt (i);
      if (ch == '\n' && prevChar == '\r')
      {
        // do nothing - we've already printed a new line for the '\r'
        // during the previous iteration.
      }
      else
        if (ch == '\n' || ch == '\r')
        {
          if (indentOn)
          {
            phase1NewLine ();
          }
          else
          {
            m_codeGenerator.genCodeNewLine ();
          }
        }
        else
          if (ch == INDENT_INC)
          {
            m_nIndentCount += 2;
          }
          else
            if (ch == INDENT_DEC)
            {
              m_nIndentCount -= 2;
            }
            else
              if (ch == INDENT_OFF)
              {
                indentOn = false;
              }
              else
                if (ch == INDENT_ON)
                {
                  indentOn = true;
                }
                else
                {
                  m_codeGenerator.genCode (ch);
                }
    }
  }

  // Print CPPCODE method header.
  private String _generateCPPMethodheader (final CodeProductionCpp p)
  {
    final StringBuilder sig = new StringBuilder ();
    String ret, params;
    Token t = null;

    if (false)
    {
      m_codeGenerator.printTokenSetup (t);
      s_ccol = 1;
      final String comment1 = m_codeGenerator.getLeadingComments (t);
      s_cline = t.beginLine;
      s_ccol = t.beginColumn;
      sig.append (t.image);
    }

    for (final Token element : p.getReturnTypeTokens ())
    {
      t = element;
      final String s = m_codeGenerator.getStringToPrint (t);
      sig.append (t.toString ());
      sig.append (" ");
    }

    String comment2 = "";
    if (t != null)
      comment2 = m_codeGenerator.getTrailingComments (t);
    ret = sig.toString ();

    sig.setLength (0);
    sig.append ("(");
    if (p.getParameterListTokens ().size () != 0)
    {
      m_codeGenerator.printTokenSetup (p.getParameterListTokens ().get (0));
      for (final Token aElement : p.getParameterListTokens ())
      {
        t = aElement;
        sig.append (m_codeGenerator.getStringToPrint (t));
      }
      sig.append (m_codeGenerator.getTrailingComments (t));
    }
    sig.append (")");
    params = sig.toString ();

    // For now, just ignore comments
    m_codeGenerator.generateMethodDefHeader (ret, s_cu_name, p.getLhs () + params, sig.toString ());

    return "";
  }

  // Print method header and return the ERROR_RETURN string.
  private String _generateCPPMethodheader (final BNFProduction p, final Token t2)
  {
    final StringBuilder sig = new StringBuilder ();
    Token t = t2;

    final String method_name = p.getLhs ();
    boolean void_ret = false;
    boolean ptr_ret = false;

    m_codeGenerator.printTokenSetup (t);
    s_ccol = 1;
    final String comment1 = m_codeGenerator.getLeadingComments (t);
    s_cline = t.beginLine;
    s_ccol = t.beginColumn;
    sig.append (t.image);
    if (t.kind == JavaCCParserConstants.VOID)
      void_ret = true;
    if (t.kind == JavaCCParserConstants.STAR)
      ptr_ret = true;

    for (int i = 1; i < p.getReturnTypeTokens ().size (); i++)
    {
      t = p.getReturnTypeTokens ().get (i);
      sig.append (m_codeGenerator.getStringToPrint (t));
      if (t.kind == JavaCCParserConstants.VOID)
        void_ret = true;
      if (t.kind == JavaCCParserConstants.STAR)
        ptr_ret = true;
    }

    final String comment2 = m_codeGenerator.getTrailingComments (t);
    final String ret = sig.toString ();

    sig.setLength (0);
    sig.append ("(");
    if (p.getParameterListTokens ().size () != 0)
    {
      m_codeGenerator.printTokenSetup (p.getParameterListTokens ().get (0));
      for (final Token aElement : p.getParameterListTokens ())
      {
        t = aElement;
        sig.append (m_codeGenerator.getStringToPrint (t));
      }
      sig.append (m_codeGenerator.getTrailingComments (t));
    }
    sig.append (")");
    final String params = sig.toString ();

    // For now, just ignore comments
    m_codeGenerator.generateMethodDefHeader (ret, s_cu_name, p.getLhs () + params, sig.toString ());

    // Generate a default value for error return.
    String default_return;
    if (ptr_ret)
      default_return = "NULL";
    else
      if (void_ret)
        default_return = "";
      else
      {
        // 0 converts to most (all?) basic types.
        default_return = "0";
      }

    final String sDefine = "ERROR_RET_" + method_name;
    return "\n#if !defined " +
           sDefine +
           "\n#define " +
           sDefine +
           " " +
           default_return +
           "\n" +
           "#endif\n" +
           "#define __ERROR_RET__ " +
           sDefine +
           "\n";
  }

  private void _genStackCheck (final boolean voidReturn)
  {
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    if (Options.hasDepthLimit ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          m_codeGenerator.genCodeLine ("if(++jj_depth > " + Options.getDepthLimit () + ") {");
          m_codeGenerator.genCodeLine ("  jj_consume_token(-1);");
          m_codeGenerator.genCodeLine ("  throw new ParseException();");
          m_codeGenerator.genCodeLine ("}");
          m_codeGenerator.genCodeLine ("try {");
          break;
        case CPP:
          if (!voidReturn)
          {
            m_codeGenerator.genCodeLine ("if(jj_depth_error){ return __ERROR_RET__; }");
          }
          else
          {
            m_codeGenerator.genCodeLine ("if(jj_depth_error){ return; }");
          }
          m_codeGenerator.genCodeLine ("__jj_depth_inc __jj_depth_counter(this);");
          m_codeGenerator.genCodeLine ("if(jj_depth > " + Options.getDepthLimit () + ") {");
          m_codeGenerator.genCodeLine ("  jj_depth_error = true;");
          m_codeGenerator.genCodeLine ("  jj_consume_token(-1);");
          m_codeGenerator.genCodeLine ("  errorHandler->handleParseError(token, getToken(1), __FUNCTION__, this), hasError = true;");
          if (!voidReturn)
          {
            // Non-recoverable error
            m_codeGenerator.genCodeLine ("  return __ERROR_RET__;");
          }
          else
          {
            // Non-recoverable error
            m_codeGenerator.genCodeLine ("  return;");
          }
          m_codeGenerator.genCodeLine ("}");
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
      final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
      switch (eOutputLanguage)
      {
        case JAVA:
          m_codeGenerator.genCodeLine (" } finally {");
          m_codeGenerator.genCodeLine ("   --jj_depth;");
          m_codeGenerator.genCodeLine (" }");
          break;
        case CPP:
          // Nothing;
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
  }

  void buildPhase1Routine (final BNFProduction p)
  {
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    Token t = p.getReturnTypeTokens ().get (0);
    boolean voidReturn = false;
    if (t.kind == JavaCCParserConstants.VOID)
    {
      voidReturn = true;
    }
    String error_ret_cpp = null;
    switch (eOutputLanguage)
    {
      case JAVA:
        m_codeGenerator.printTokenSetup (t);
        s_ccol = 1;
        m_codeGenerator.printLeadingComments (t);
        m_codeGenerator.genCode ("  final " + (p.getAccessMod () != null ? p.getAccessMod () : "public") + " ");
        s_cline = t.beginLine;
        s_ccol = t.beginColumn;
        m_codeGenerator.printTokenOnly (t);
        for (int i = 1; i < p.getReturnTypeTokens ().size (); i++)
        {
          t = p.getReturnTypeTokens ().get (i);
          m_codeGenerator.printToken (t);
        }
        m_codeGenerator.printTrailingComments (t);
        m_codeGenerator.genCode (" " + p.getLhs () + "(");
        if (p.getParameterListTokens ().size () != 0)
        {
          m_codeGenerator.printTokenSetup ((p.getParameterListTokens ().get (0)));
          for (final Token aElement : p.getParameterListTokens ())
          {
            t = aElement;
            m_codeGenerator.printToken (t);
          }
          m_codeGenerator.printTrailingComments (t);
        }
        m_codeGenerator.genCode (")");
        m_codeGenerator.genCode (" throws ParseException");

        for (final List <Token> name : p.getThrowsList ())
        {
          m_codeGenerator.genCode (", ");
          for (final Token t2 : name)
            m_codeGenerator.genCode (t2.image);
        }
        break;
      case CPP:
        error_ret_cpp = _generateCPPMethodheader (p, t);
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }

    m_codeGenerator.genCode (" {");

    switch (eOutputLanguage)
    {
      case JAVA:
        // Nothing
        break;
      case CPP:
        if ((Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR) && error_ret_cpp != null) ||
            (Options.hasDepthLimit () && !voidReturn))
        {
          m_codeGenerator.genCode (error_ret_cpp);
        }
        else
        {
          error_ret_cpp = null;
        }
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    _genStackCheck (voidReturn);

    m_nIndentCount = 4;
    if (Options.isDebugParser ())
    {
      m_codeGenerator.genCodeNewLine ();
      switch (eOutputLanguage)
      {
        case JAVA:
          m_codeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) + "\");");
          break;
        case CPP:
          m_codeGenerator.genCodeLine ("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\"" +
                                       JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) +
                                       "\"); });");
          m_codeGenerator.genCodeLine ("    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\"" +
                                       JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) +
                                       "\"); });");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      m_codeGenerator.genCodeLine ("    try {");
      m_nIndentCount += 2;
    }

    if (!Options.booleanValue (Options.USEROPTION__CPP_IGNORE_ACTIONS) && p.getDeclarationTokens ().size () != 0)
    {
      m_codeGenerator.printTokenSetup (p.getDeclarationTokens ().get (0));
      s_cline--;
      for (final Token aElement : p.getDeclarationTokens ())
      {
        t = aElement;
        m_codeGenerator.printToken (t);
      }
      m_codeGenerator.printTrailingComments (t);
    }

    final String code = _phase1ExpansionGen (p.getExpansion ());
    dumpFormattedString (code);
    m_codeGenerator.genCodeNewLine ();

    if (p.isJumpPatched () && !voidReturn)
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          // This line is required for Java!
          m_codeGenerator.genCodeLine ("    throw new IllegalStateException (\"Missing return statement in function\");");
          break;
        case CPP:
          m_codeGenerator.genCodeLine ("    throw \"Missing return statement in function\";");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    if (Options.isDebugParser ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          m_codeGenerator.genCodeLine ("    } finally {");
          m_codeGenerator.genCodeLine ("      trace_return(\"" +
                                       JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) +
                                       "\");");
          m_codeGenerator.genCodeLine ("    }");
          break;
        case CPP:
          m_codeGenerator.genCodeLine ("    } catch(...) { }");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    if (!voidReturn)
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          // Nothing
          break;
        case CPP:
          m_codeGenerator.genCodeLine ("assert(false);");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }

    if (error_ret_cpp != null)
    {
      m_codeGenerator.genCodeLine ("\n#undef __ERROR_RET__\n");
    }
    genStackCheckEnd ();
    m_codeGenerator.genCodeLine ("}");
    m_codeGenerator.genCodeNewLine ();
  }

  void phase1NewLine ()
  {
    m_codeGenerator.genCodeNewLine ();
    m_codeGenerator.genCode (StringHelper.getRepeated (' ', m_nIndentCount));
  }

  private String _phase1ExpansionGen (final Expansion e)
  {
    String retval = "";
    Token t = null;
    ExpLookahead [] conds;
    String [] actions;
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    if (e instanceof AbstractExpRegularExpression)
    {
      final AbstractExpRegularExpression e_nrw = (AbstractExpRegularExpression) e;
      retval += "\n";
      if (!e_nrw.getLhsTokens ().isEmpty ())
      {
        m_codeGenerator.printTokenSetup (e_nrw.getLhsTokens ().get (0));
        for (final Token aElement : e_nrw.getLhsTokens ())
        {
          t = aElement;
          retval += m_codeGenerator.getStringToPrint (t);
        }
        retval += m_codeGenerator.getTrailingComments (t);
        retval += " = ";
      }
      final String tail;
      if (e_nrw.getRhsToken () == null)
        tail = ");";
      else
        switch (eOutputLanguage)
        {
          case JAVA:
            tail = ")." + e_nrw.getRhsToken ().image + ";";
            break;
          case CPP:
            tail = ")->" + e_nrw.getRhsToken ().image + ";";
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }

      if (e_nrw.hasLabel ())
      {
        retval += "jj_consume_token(" + e_nrw.getLabel () + tail;
      }
      else
      {
        final String label = NAMES_OF_TOKENS.get (Integer.valueOf (e_nrw.getOrdinal ()));
        if (label != null)
        {
          retval += "jj_consume_token(" + label + tail;
        }
        else
        {
          retval += "jj_consume_token(" + e_nrw.getOrdinal () + tail;
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
            retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
          }
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    else
      if (e instanceof ExpNonTerminal)
      {
        final ExpNonTerminal e_nrw = (ExpNonTerminal) e;
        retval += "\n";
        if (e_nrw.getLhsTokenCount () != 0)
        {
          m_codeGenerator.printTokenSetup (e_nrw.getLhsTokenAt (0));
          for (final Token aElement : e_nrw.getLhsTokens ())
          {
            t = aElement;
            retval += m_codeGenerator.getStringToPrint (t);
          }
          retval += m_codeGenerator.getTrailingComments (t);
          retval += " = ";
        }
        retval += e_nrw.getName () + "(";
        if (e_nrw.getArgumentTokenCount () != 0)
        {
          m_codeGenerator.printTokenSetup (e_nrw.getArgumentTokenAt (0));
          for (final Token aElement : e_nrw.getArgumentTokens ())
          {
            t = aElement;
            retval += m_codeGenerator.getStringToPrint (t);
          }
          retval += m_codeGenerator.getTrailingComments (t);
        }
        retval += ");";
        switch (eOutputLanguage)
        {
          case JAVA:
            // Nothing
            break;
          case CPP:
            if (Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR))
            {
              retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
            }
            break;
          default:
            throw new UnsupportedOutputLanguageException (eOutputLanguage);
        }
      }
      else
        if (e instanceof ExpAction)
        {
          final ExpAction e_nrw = (ExpAction) e;
          retval += INDENT_OFF + "\n";
          if (!Options.booleanValue (Options.USEROPTION__CPP_IGNORE_ACTIONS) && e_nrw.getActionTokens ().size () != 0)
          {
            m_codeGenerator.printTokenSetup (e_nrw.getActionTokens ().get (0));
            s_ccol = 1;
            for (final Token aElement : e_nrw.getActionTokens ())
            {
              t = aElement;
              retval += m_codeGenerator.getStringToPrint (t);
            }
            retval += m_codeGenerator.getTrailingComments (t);
          }
          retval += INDENT_ON;
        }
        else
          if (e instanceof ExpChoice)
          {
            final ExpChoice e_nrw = (ExpChoice) e;
            conds = new ExpLookahead [e_nrw.getChoiceCount ()];
            actions = new String [e_nrw.getChoiceCount () + 1];

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
            actions[e_nrw.getChoiceCount ()] = sChoice;

            // In previous line, the "throw" never throws an exception since the
            // evaluation of jj_consume_token(-1) causes ParseException to be
            // thrown first.
            for (int i = 0; i < e_nrw.getChoiceCount (); i++)
            {
              final ExpSequence nestedSeq = (ExpSequence) e_nrw.getChoiceAt (i);
              actions[i] = _phase1ExpansionGen (nestedSeq);
              conds[i] = (ExpLookahead) nestedSeq.getUnitAt (0);
            }
            retval = buildLookaheadChecker (conds, actions);
          }
          else
            if (e instanceof ExpSequence)
            {
              final ExpSequence e_nrw = (ExpSequence) e;
              // We skip the first element in the following iteration since it
              // is the
              // Lookahead object.
              for (int i = 1; i < e_nrw.getUnitCount (); i++)
              {
                // For C++, since we are not using exceptions, we will protect
                // all the
                // expansion choices with if (!error)
                boolean wrap_in_block = false;
                if (!JavaCCGlobals.s_jjtreeGenerated)
                {
                  switch (eOutputLanguage)
                  {
                    case JAVA:
                      // nothing
                      break;
                    case CPP:
                      // for the last one, if it's an action, we will not
                      // protect it.
                      final Expansion elem = e_nrw.getUnitAt (i);
                      if (!(elem instanceof ExpAction) ||
                          !(e.getParent () instanceof BNFProduction) ||
                          i != e_nrw.getUnitCount () - 1)
                      {
                        wrap_in_block = true;
                        retval += "\nif (!hasError) {";
                      }
                      break;
                    default:
                      throw new UnsupportedOutputLanguageException (eOutputLanguage);
                  }
                }
                retval += _phase1ExpansionGen (e_nrw.getUnitAt (i));
                if (wrap_in_block)
                {
                  retval += "\n}";
                }
              }
            }
            else
              if (e instanceof ExpOneOrMore)
              {
                final ExpOneOrMore e_nrw = (ExpOneOrMore) e;
                final Expansion nested_e = e_nrw.getExpansion ();
                ExpLookahead la;
                if (nested_e instanceof ExpSequence)
                {
                  la = (ExpLookahead) (((ExpSequence) nested_e).getUnitAt (0));
                }
                else
                {
                  la = new ExpLookahead ();
                  la.setAmount (Options.getLookahead ());
                  la.setLaExpansion (nested_e);
                }
                retval += "\n";
                final int labelIndex = ++m_nGenSymbolIndex;
                switch (eOutputLanguage)
                {
                  case JAVA:
                    retval += "label_" + labelIndex + ":\n";
                    retval += "while (true) {" + INDENT_INC;
                    break;
                  case CPP:
                    // nothing
                    retval += "while (!hasError) {" + INDENT_INC;
                    break;
                  default:
                    throw new UnsupportedOutputLanguageException (eOutputLanguage);
                }
                retval += _phase1ExpansionGen (nested_e);
                conds = new ExpLookahead [1];
                conds[0] = la;
                actions = new String [2];
                // [ph] empty statement needed???
                actions[0] = true ? "" : "\n;";

                switch (eOutputLanguage)
                {
                  case JAVA:
                    actions[1] = "\nbreak label_" + labelIndex + ";";
                    break;
                  case CPP:
                    actions[1] = "\ngoto end_label_" + labelIndex + ";";
                    break;
                  default:
                    throw new UnsupportedOutputLanguageException (eOutputLanguage);
                }

                retval += buildLookaheadChecker (conds, actions);
                retval += INDENT_DEC + "\n" + "}";

                switch (eOutputLanguage)
                {
                  case JAVA:
                    // nothing
                    break;
                  case CPP:
                    retval += "\nend_label_" + labelIndex + ": ;";
                    break;
                  default:
                    throw new UnsupportedOutputLanguageException (eOutputLanguage);
                }
              }
              else
                if (e instanceof ExpZeroOrMore)
                {
                  final ExpZeroOrMore e_nrw = (ExpZeroOrMore) e;
                  final Expansion nested_e = e_nrw.getExpansion ();
                  ExpLookahead la;
                  if (nested_e instanceof ExpSequence)
                  {
                    la = (ExpLookahead) (((ExpSequence) nested_e).getUnitAt (0));
                  }
                  else
                  {
                    la = new ExpLookahead ();
                    la.setAmount (Options.getLookahead ());
                    la.setLaExpansion (nested_e);
                  }
                  retval += "\n";
                  final int labelIndex = ++m_nGenSymbolIndex;
                  switch (eOutputLanguage)
                  {
                    case JAVA:
                      retval += "label_" + labelIndex + ":\n";
                      retval += "while (true) {" + INDENT_INC;
                      break;
                    case CPP:
                      // nothing
                      retval += "while (!hasError) {" + INDENT_INC;
                      break;
                    default:
                      throw new UnsupportedOutputLanguageException (eOutputLanguage);
                  }

                  conds = new ExpLookahead [1];
                  conds[0] = la;
                  actions = new String [2];
                  // [ph] empty statement needed???
                  actions[0] = true ? "" : "\n;";

                  switch (eOutputLanguage)
                  {
                    case JAVA:
                      actions[1] = "\nbreak label_" + labelIndex + ";";
                      break;
                    case CPP:
                      actions[1] = "\ngoto end_label_" + labelIndex + ";";
                      break;
                    default:
                      throw new UnsupportedOutputLanguageException (eOutputLanguage);
                  }

                  retval += buildLookaheadChecker (conds, actions);
                  retval += _phase1ExpansionGen (nested_e);
                  retval += INDENT_DEC + "\n" + "}";

                  switch (eOutputLanguage)
                  {
                    case JAVA:
                      // nothing
                      break;
                    case CPP:
                      retval += "\nend_label_" + labelIndex + ": ;";
                      break;
                    default:
                      throw new UnsupportedOutputLanguageException (eOutputLanguage);
                  }
                }
                else
                  if (e instanceof ExpZeroOrOne)
                  {
                    final ExpZeroOrOne e_nrw = (ExpZeroOrOne) e;
                    final Expansion nested_e = e_nrw.getExpansion ();
                    ExpLookahead la;
                    if (nested_e instanceof ExpSequence)
                    {
                      la = (ExpLookahead) (((ExpSequence) nested_e).getUnitAt (0));
                    }
                    else
                    {
                      la = new ExpLookahead ();
                      la.setAmount (Options.getLookahead ());
                      la.setLaExpansion (nested_e);
                    }
                    conds = new ExpLookahead [1];
                    conds[0] = la;
                    actions = new String [2];
                    actions[0] = _phase1ExpansionGen (nested_e);
                    // Empty statement is relevant for Lookup!
                    actions[1] = "\n;";
                    retval += buildLookaheadChecker (conds, actions);
                  }
                  else
                    if (e instanceof ExpTryBlock)
                    {
                      final ExpTryBlock e_nrw = (ExpTryBlock) e;
                      final Expansion nested_e = e_nrw.m_exp;
                      List <Token> list;
                      retval += "\n";
                      retval += "try {" + INDENT_INC;
                      retval += _phase1ExpansionGen (nested_e);
                      retval += INDENT_DEC + "\n" + "}";
                      for (int i = 0; i < e_nrw.m_catchblks.size (); i++)
                      {
                        retval += " catch (";
                        list = e_nrw.m_types.get (i);
                        if (list.size () != 0)
                        {
                          m_codeGenerator.printTokenSetup (list.get (0));
                          for (final Token aElement : list)
                          {
                            t = aElement;
                            retval += m_codeGenerator.getStringToPrint (t);
                          }
                          retval += m_codeGenerator.getTrailingComments (t);
                        }
                        retval += " ";
                        t = e_nrw.m_ids.get (i);
                        m_codeGenerator.printTokenSetup (t);
                        retval += m_codeGenerator.getStringToPrint (t);
                        retval += m_codeGenerator.getTrailingComments (t);
                        retval += ") {" + INDENT_OFF + "\n";
                        list = e_nrw.m_catchblks.get (i);
                        if (list.size () != 0)
                        {
                          m_codeGenerator.printTokenSetup (list.get (0));
                          s_ccol = 1;
                          for (final Token aElement : list)
                          {
                            t = aElement;
                            retval += m_codeGenerator.getStringToPrint (t);
                          }
                          retval += m_codeGenerator.getTrailingComments (t);
                        }
                        retval += INDENT_ON + "\n" + "}";
                      }
                      if (e_nrw.m_finallyblk != null)
                      {
                        switch (eOutputLanguage)
                        {
                          case JAVA:
                            retval += " finally {" + INDENT_OFF + "\n";
                            break;
                          case CPP:
                            retval += " finally {" + INDENT_OFF + "\n";
                            break;
                          default:
                            throw new UnsupportedOutputLanguageException (eOutputLanguage);
                        }

                        if (e_nrw.m_finallyblk.size () != 0)
                        {
                          m_codeGenerator.printTokenSetup (e_nrw.m_finallyblk.get (0));
                          s_ccol = 1;
                          for (final Token aElement : e_nrw.m_finallyblk)
                          {
                            t = aElement;
                            retval += m_codeGenerator.getStringToPrint (t);
                          }
                          retval += m_codeGenerator.getTrailingComments (t);
                        }
                        retval += INDENT_ON + "\n" + "}";
                      }
                    }
    return retval;
  }

  private void _buildPhase2Routine (final ExpLookahead la)
  {
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    final Expansion e = la.getLaExpansion ();
    switch (eOutputLanguage)
    {
      case JAVA:
        m_codeGenerator.genCodeLine ("  private boolean jj_2" + e.getInternalName () + "(int xla)");
        break;
      case CPP:
        m_codeGenerator.genCodeLine (" inline bool jj_2" + e.getInternalName () + "(int xla)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    m_codeGenerator.genCodeLine (" {");
    m_codeGenerator.genCodeLine ("    jj_la = xla;");
    m_codeGenerator.genCodeLine ("    jj_scanpos = token;");
    m_codeGenerator.genCodeLine ("    jj_lastpos = token;");

    String ret_suffix = "";
    if (Options.hasDepthLimit ())
    {
      ret_suffix = " && !jj_depth_error";
    }

    switch (eOutputLanguage)
    {
      case JAVA:
        m_codeGenerator.genCodeLine ("    try { return (!jj_3" + e.getInternalName () + "()" + ret_suffix + "); }");
        m_codeGenerator.genCodeLine ("    catch(LookaheadSuccess ls) { return true; }");
        break;
      case CPP:
        m_codeGenerator.genCodeLine ("    jj_done = false;");
        m_codeGenerator.genCodeLine ("    return (!jj_3" + e.getInternalName () + "() || jj_done)" + ret_suffix + ";");
        break;
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
    if (Options.isErrorReporting ())
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          m_codeGenerator.genCodeLine ("    finally { jj_save(" + (e.getInternalIndex () - 1) + ", xla); }");
          break;
        case CPP:
          m_codeGenerator.genCodeLine (" { jj_save(" + (e.getInternalIndex () - 1) + ", xla); }");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
    }
    m_codeGenerator.genCodeLine ("  }");
    m_codeGenerator.genCodeNewLine ();
    final Phase3Data p3d = new Phase3Data (e, la.getAmount ());
    m_phase3list.add (p3d);
    m_phase3table.put (e, p3d);
  }

  private boolean m_xsp_declared;

  private Expansion m_jj3_expansion;

  private String _genReturn (final boolean value)
  {
    final String retval = (value ? "true" : "false");
    if (Options.isDebugLookahead () && m_jj3_expansion != null)
    {
      String tracecode = "trace_return(\"" +
                         JavaCCGlobals.addUnicodeEscapes (((NormalProduction) m_jj3_expansion.getParent ()).getLhs ()) +
                         "(LOOKAHEAD " +
                         (value ? "FAILED" : "SUCCEEDED") +
                         ")\");";
      if (Options.isErrorReporting ())
      {
        tracecode = "if (!jj_rescan) " + tracecode;
      }
      return "{ " + tracecode + " return " + retval + "; }";
    }
    return "return " + retval + ";";
  }

  private void _generate3R (@Nonnull final Expansion e, final Phase3Data inf)
  {
    Expansion seq = e;
    if (e.hasNoInternalName ())
    {
      while (true)
      {
        if (seq instanceof ExpSequence && ((ExpSequence) seq).getUnitCount () == 2)
        {
          seq = ((ExpSequence) seq).getUnitAt (1);
        }
        else
          if (seq instanceof ExpNonTerminal)
          {
            final ExpNonTerminal e_nrw = (ExpNonTerminal) seq;
            final NormalProduction ntprod = (PRODUCTION_TABLE.get (e_nrw.getName ()));
            if (ntprod instanceof AbstractCodeProduction)
            {
              break; // nothing to do here
            }
            seq = ntprod.getExpansion ();
          }
          else
            break;
      }

      if (seq instanceof AbstractExpRegularExpression)
      {
        e.setInternalNameOnly ("jj_scan_token(" + ((AbstractExpRegularExpression) seq).getOrdinal () + ")");
        return;
      }

      m_nGenSymbolIndex++;
      e.setInternalName ("R_", m_nGenSymbolIndex);
    }
    Phase3Data p3d = (m_phase3table.get (e));
    if (p3d == null || p3d.m_count < inf.m_count)
    {
      p3d = new Phase3Data (e, inf.m_count);
      m_phase3list.add (p3d);
      m_phase3table.put (e, p3d);
    }
  }

  void setupPhase3Builds (final Phase3Data inf)
  {
    final Expansion e = inf.m_exp;
    if (e instanceof AbstractExpRegularExpression)
    {
      // nothing to here
    }
    else
      if (e instanceof ExpNonTerminal)
      {
        // All expansions of non-terminals have the "name" fields set. So
        // there's no need to check it below for "e_nrw" and "ntexp". In
        // fact, we rely here on the fact that the "name" fields of both these
        // variables are the same.
        final ExpNonTerminal e_nrw = (ExpNonTerminal) e;
        final NormalProduction ntprod = (PRODUCTION_TABLE.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          // nothing to do here
        }
        else
        {
          _generate3R (ntprod.getExpansion (), inf);
        }
      }
      else
        if (e instanceof ExpChoice)
        {
          final ExpChoice e_nrw = (ExpChoice) e;
          for (final Expansion element : e_nrw.getChoices ())
          {
            _generate3R ((element), inf);
          }
        }
        else
          if (e instanceof ExpSequence)
          {
            final ExpSequence e_nrw = (ExpSequence) e;
            // We skip the first element in the following iteration since it is
            // the
            // Lookahead object.
            int cnt = inf.m_count;
            for (int i = 1; i < e_nrw.getUnitCount (); i++)
            {
              final Expansion eseq = (e_nrw.getUnitAt (i));
              setupPhase3Builds (new Phase3Data (eseq, cnt));
              cnt -= minimumSize (eseq);
              if (cnt <= 0)
                break;
            }
          }
          else
            if (e instanceof ExpTryBlock)
            {
              final ExpTryBlock e_nrw = (ExpTryBlock) e;
              setupPhase3Builds (new Phase3Data (e_nrw.m_exp, inf.m_count));
            }
            else
              if (e instanceof ExpOneOrMore)
              {
                final ExpOneOrMore e_nrw = (ExpOneOrMore) e;
                _generate3R (e_nrw.getExpansion (), inf);
              }
              else
                if (e instanceof ExpZeroOrMore)
                {
                  final ExpZeroOrMore e_nrw = (ExpZeroOrMore) e;
                  _generate3R (e_nrw.getExpansion (), inf);
                }
                else
                  if (e instanceof ExpZeroOrOne)
                  {
                    final ExpZeroOrOne e_nrw = (ExpZeroOrOne) e;
                    _generate3R (e_nrw.getExpansion (), inf);
                  }
  }

  private String _getTypeForToken ()
  {
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    switch (eOutputLanguage)
    {
      case JAVA:
        return "Token";
      case CPP:
        return "Token *";
      default:
        throw new UnsupportedOutputLanguageException (eOutputLanguage);
    }
  }

  private String _genjj_3Call (final Expansion e)
  {
    final String sInternalName = e.getInternalName ();
    if (sInternalName.startsWith ("jj_scan_token"))
      return sInternalName;
    return "jj_3" + sInternalName + "()";
  }

  void buildPhase3Routine (final Phase3Data inf, final boolean recursive_call)
  {
    final Expansion e = inf.m_exp;
    Token t = null;
    if (e.getInternalName ().startsWith ("jj_scan_token"))
      return;

    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    if (!recursive_call)
    {
      switch (eOutputLanguage)
      {
        case JAVA:
          m_codeGenerator.genCodeLine ("  private " +
                                       eOutputLanguage.getTypeBoolean () +
                                       " jj_3" +
                                       e.getInternalName () +
                                       "()");
          break;
        case CPP:
          m_codeGenerator.genCodeLine (" inline bool jj_3" + e.getInternalName () + "()");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }

      m_codeGenerator.genCodeLine (" {");
      switch (eOutputLanguage)
      {
        case JAVA:
          break;
        case CPP:
          m_codeGenerator.genCodeLine ("    if (jj_done) return true;");
          if (Options.hasDepthLimit ())
            m_codeGenerator.genCodeLine ("#define __ERROR_RET__ true");
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      _genStackCheck (false);
      m_xsp_declared = false;
      if (Options.isDebugLookahead () && e.getParent () instanceof NormalProduction)
      {
        m_codeGenerator.genCode ("    ");
        if (Options.isErrorReporting ())
        {
          m_codeGenerator.genCode ("if (!jj_rescan) ");
        }
        m_codeGenerator.genCodeLine ("trace_call(\"" +
                                     JavaCCGlobals.addUnicodeEscapes (((NormalProduction) e.getParent ()).getLhs ()) +
                                     "(LOOKING AHEAD...)\");");
        m_jj3_expansion = e;
      }
      else
      {
        m_jj3_expansion = null;
      }
    }
    if (e instanceof AbstractExpRegularExpression)
    {
      final AbstractExpRegularExpression e_nrw = (AbstractExpRegularExpression) e;
      if (e_nrw.hasLabel ())
      {
        m_codeGenerator.genCodeLine ("    if (jj_scan_token(" + e_nrw.getLabel () + ")) " + _genReturn (true));
      }
      else
      {
        final Object label = NAMES_OF_TOKENS.get (Integer.valueOf (e_nrw.getOrdinal ()));
        if (label != null)
        {
          m_codeGenerator.genCodeLine ("    if (jj_scan_token(" + (String) label + ")) " + _genReturn (true));
        }
        else
        {
          m_codeGenerator.genCodeLine ("    if (jj_scan_token(" + e_nrw.getOrdinal () + ")) " + _genReturn (true));
        }
      }
      // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos)
      // " + genReturn(false));
    }
    else
      if (e instanceof ExpNonTerminal)
      {
        // All expansions of non-terminals have the "name" fields set. So
        // there's no need to check it below for "e_nrw" and "ntexp". In
        // fact, we rely here on the fact that the "name" fields of both these
        // variables are the same.
        final ExpNonTerminal e_nrw = (ExpNonTerminal) e;
        final NormalProduction ntprod = (PRODUCTION_TABLE.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          m_codeGenerator.genCodeLine ("    if (true) { jj_la = 0; jj_scanpos = jj_lastpos; " +
                                       _genReturn (false) +
                                       "}");
        }
        else
        {
          final Expansion ntexp = ntprod.getExpansion ();
          // codeGenerator.genCodeLine(" if (jj_3" + ntexp.internal_name + "())
          // " + genReturn(true));
          m_codeGenerator.genCodeLine ("    if (" + _genjj_3Call (ntexp) + ") " + _genReturn (true));
          // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
          // jj_lastpos) " + genReturn(false));
        }
      }
      else
        if (e instanceof ExpChoice)
        {
          ExpSequence nested_seq;
          final ExpChoice e_nrw = (ExpChoice) e;
          if (e_nrw.getChoiceCount () != 1)
          {
            if (!m_xsp_declared)
            {
              m_xsp_declared = true;
              m_codeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
            }
            m_codeGenerator.genCodeLine ("    xsp = jj_scanpos;");
          }
          for (int i = 0; i < e_nrw.getChoiceCount (); i++)
          {
            nested_seq = (ExpSequence) (e_nrw.getChoiceAt (i));
            final ExpLookahead la = (ExpLookahead) (nested_seq.getUnitAt (0));
            if (la.getActionTokens ().isNotEmpty ())
            {
              // We have semantic lookahead that must be evaluated.
              JavaCCGlobals.setLookAheadNeeded (true);
              m_codeGenerator.genCodeLine ("    jj_lookingAhead = true;");
              m_codeGenerator.genCode ("    jj_semLA = ");
              m_codeGenerator.printTokenSetup (la.getActionTokens ().getFirstOrNull ());
              for (final Token aElement : la.getActionTokens ())
              {
                t = aElement;
                m_codeGenerator.printToken (t);
              }
              m_codeGenerator.printTrailingComments (t);
              m_codeGenerator.genCodeLine (";");
              m_codeGenerator.genCodeLine ("    jj_lookingAhead = false;");
            }
            m_codeGenerator.genCode ("    if (");
            if (la.getActionTokens ().isNotEmpty ())
            {
              m_codeGenerator.genCode ("!jj_semLA || ");
            }
            if (i != e_nrw.getChoiceCount () - 1)
            {
              // codeGenerator.genCodeLine("jj_3" + nested_seq.internal_name +
              // "()) {");
              m_codeGenerator.genCodeLine (_genjj_3Call (nested_seq) + ") {");
              m_codeGenerator.genCodeLine ("    jj_scanpos = xsp;");
            }
            else
            {
              // codeGenerator.genCodeLine("jj_3" + nested_seq.internal_name +
              // "()) " + genReturn(true));
              m_codeGenerator.genCodeLine (_genjj_3Call (nested_seq) + ") " + _genReturn (true));
              // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
              // jj_lastpos) " + genReturn(false));
            }
          }
          for (int i = 1; i < e_nrw.getChoiceCount (); i++)
          {
            // codeGenerator.genCodeLine(" } else if (jj_la == 0 && jj_scanpos
            // == jj_lastpos) " + genReturn(false));
            m_codeGenerator.genCodeLine ("    }");
          }
        }
        else
          if (e instanceof ExpSequence)
          {
            final ExpSequence e_nrw = (ExpSequence) e;
            // We skip the first element in the following iteration since it is
            // the Lookahead object.
            int cnt = inf.m_count;
            for (int i = 1; i < e_nrw.getUnitCount (); i++)
            {
              final Expansion eseq = e_nrw.getUnitAt (i);
              buildPhase3Routine (new Phase3Data (eseq, cnt), true);

              // Test Code
              if (false)
                PGPrinter.info ("minimumSize: line: " +
                                eseq.getLine () +
                                ", column: " +
                                eseq.getColumn () +
                                ": " +
                                minimumSize (eseq));

              cnt -= minimumSize (eseq);
              if (cnt <= 0)
                break;
            }
          }
          else
            if (e instanceof ExpTryBlock)
            {
              final ExpTryBlock e_nrw = (ExpTryBlock) e;
              buildPhase3Routine (new Phase3Data (e_nrw.m_exp, inf.m_count), true);
            }
            else
              if (e instanceof ExpOneOrMore)
              {
                if (!m_xsp_declared)
                {
                  m_xsp_declared = true;
                  m_codeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
                }
                final ExpOneOrMore e_nrw = (ExpOneOrMore) e;
                final Expansion nested_e = e_nrw.getExpansion ();
                // codeGenerator.genCodeLine(" if (jj_3" +
                // nested_e.internal_name + "()) " + genReturn(true));
                m_codeGenerator.genCodeLine ("    if (" + _genjj_3Call (nested_e) + ") " + _genReturn (true));
                // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                // jj_lastpos) " + genReturn(false));
                m_codeGenerator.genCodeLine ("    while (true) {");
                m_codeGenerator.genCodeLine ("      xsp = jj_scanpos;");
                // codeGenerator.genCodeLine(" if (jj_3" +
                // nested_e.internal_name + "()) { jj_scanpos = xsp; break; }");
                m_codeGenerator.genCodeLine ("      if (" + _genjj_3Call (nested_e) + ") { jj_scanpos = xsp; break; }");
                // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                // jj_lastpos) " + genReturn(false));
                m_codeGenerator.genCodeLine ("    }");
              }
              else
                if (e instanceof ExpZeroOrMore)
                {
                  if (!m_xsp_declared)
                  {
                    m_xsp_declared = true;
                    m_codeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
                  }
                  final ExpZeroOrMore e_nrw = (ExpZeroOrMore) e;
                  final Expansion nested_e = e_nrw.getExpansion ();
                  m_codeGenerator.genCodeLine ("    while (true) {");
                  m_codeGenerator.genCodeLine ("      xsp = jj_scanpos;");
                  // codeGenerator.genCodeLine(" if (jj_3" +
                  // nested_e.internal_name + "()) { jj_scanpos = xsp; break;
                  // }");
                  m_codeGenerator.genCodeLine ("      if (" +
                                               _genjj_3Call (nested_e) +
                                               ") { jj_scanpos = xsp; break; }");
                  // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                  // jj_lastpos) " + genReturn(false));
                  m_codeGenerator.genCodeLine ("    }");
                }
                else
                  if (e instanceof ExpZeroOrOne)
                  {
                    if (!m_xsp_declared)
                    {
                      m_xsp_declared = true;
                      m_codeGenerator.genCodeLine ("    " + _getTypeForToken () + " xsp;");
                    }
                    final ExpZeroOrOne e_nrw = (ExpZeroOrOne) e;
                    final Expansion nested_e = e_nrw.getExpansion ();
                    m_codeGenerator.genCodeLine ("    xsp = jj_scanpos;");
                    // codeGenerator.genCodeLine(" if (jj_3" +
                    // nested_e.internal_name + "()) jj_scanpos = xsp;");
                    m_codeGenerator.genCodeLine ("    if (" + _genjj_3Call (nested_e) + ") jj_scanpos = xsp;");
                    // codeGenerator.genCodeLine(" else if (jj_la == 0 &&
                    // jj_scanpos == jj_lastpos) " + genReturn(false));
                  }
    if (!recursive_call)
    {
      m_codeGenerator.genCodeLine ("    " + _genReturn (false));
      genStackCheckEnd ();
      switch (eOutputLanguage)
      {
        case JAVA:
          // nothing;
          break;
        case CPP:
          if (Options.hasDepthLimit ())
          {
            m_codeGenerator.genCodeLine ("#undef __ERROR_RET__");
          }
          break;
        default:
          throw new UnsupportedOutputLanguageException (eOutputLanguage);
      }
      m_codeGenerator.genCodeLine ("  }");
      m_codeGenerator.genCodeNewLine ();
    }
  }

  int minimumSize (final Expansion e)
  {
    return minimumSize (e, Integer.MAX_VALUE);
  }

  /*
   * Returns the minimum number of tokens that can parse to this expansion.
   */
  int minimumSize (final Expansion e, final int oldMin)
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

      if (e instanceof ExpNonTerminal)
      {
        final ExpNonTerminal e_nrw = (ExpNonTerminal) e;
        final NormalProduction ntprod = (PRODUCTION_TABLE.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          return Integer.MAX_VALUE;
          // Make caller think this is unending (for we do not go beyond
          // JAVACODE during
          // phase3 execution).
        }
        final Expansion ntexp = ntprod.getExpansion ();
        return minimumSize (ntexp);
      }

      if (e instanceof ExpChoice)
      {
        int min = oldMin;
        Expansion nested_e;
        final ExpChoice e_nrw = (ExpChoice) e;
        for (int i = 0; min > 1 && i < e_nrw.getChoiceCount (); i++)
        {
          nested_e = (e_nrw.getChoiceAt (i));
          final int min1 = minimumSize (nested_e, min);
          if (min > min1)
            min = min1;
        }
        return min;
      }

      if (e instanceof ExpSequence)
      {
        int min = 0;
        final ExpSequence e_nrw = (ExpSequence) e;
        // We skip the first element in the following iteration since it
        // is
        // the
        // Lookahead object.
        for (int i = 1; i < e_nrw.getUnitCount (); i++)
        {
          final Expansion eseq = (e_nrw.getUnitAt (i));
          final int mineseq = minimumSize (eseq);
          if (min == Integer.MAX_VALUE || mineseq == Integer.MAX_VALUE)
          {
            min = Integer.MAX_VALUE; // Adding infinity to something
                                     // results
                                     // in infinity.
          }
          else
          {
            min += mineseq;
            if (min > oldMin)
              break;
          }
        }
        return min;
      }

      if (e instanceof ExpTryBlock)
      {
        final ExpTryBlock e_nrw = (ExpTryBlock) e;
        return minimumSize (e_nrw.m_exp);
      }

      if (e instanceof ExpOneOrMore)
      {
        final ExpOneOrMore e_nrw = (ExpOneOrMore) e;
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

  void build (final CodeGenerator codeGenerator)
  {
    m_codeGenerator = codeGenerator;
    final EOutputLanguage eOutputLanguage = m_codeGenerator.getOutputLanguage ();
    for (final NormalProduction p : BNF_PRODUCTIONS)
    {
      if (p instanceof CodeProductionCpp)
      {
        if (!eOutputLanguage.isJava ())
        {
          JavaCCErrors.semantic_error ("Cannot use JAVACODE productions with non-Java output.");
          continue;
        }

        final CodeProductionCpp cp = (CodeProductionCpp) p;

        _generateCPPMethodheader (cp);

        if (false)
        {
          Token t = (cp.getReturnTypeTokens ().get (0));
          codeGenerator.printTokenSetup (t);
          s_ccol = 1;
          codeGenerator.printLeadingComments (t);
          codeGenerator.genCode (" " + (p.getAccessMod () != null ? p.getAccessMod () + " " : ""));
          s_cline = t.beginLine;
          s_ccol = t.beginColumn;
          codeGenerator.printTokenOnly (t);
          for (int i = 1; i < cp.getReturnTypeTokens ().size (); i++)
          {
            t = (cp.getReturnTypeTokens ().get (i));
            codeGenerator.printToken (t);
          }
          codeGenerator.printTrailingComments (t);
          codeGenerator.genCode (" " + cp.getLhs () + "(");
          if (cp.getParameterListTokens ().size () != 0)
          {
            codeGenerator.printTokenSetup (cp.getParameterListTokens ().get (0));
            for (final Token aElement : cp.getParameterListTokens ())
            {
              t = aElement;
              codeGenerator.printToken (t);
            }
            codeGenerator.printTrailingComments (t);
          }
          codeGenerator.genCode (")");
          for (final List <Token> aElement : cp.getThrowsList ())
          {
            codeGenerator.genCode (", ");
            for (final Token aElement2 : aElement)
            {
              t = aElement2;
              codeGenerator.genCode (t.image);
            }
          }
        }

        codeGenerator.genCodeLine (" {");
        if (Options.isDebugParser ())
        {
          codeGenerator.genCodeNewLine ();
          switch (eOutputLanguage)
          {
            case JAVA:
              codeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (cp.getLhs ()) + "\");");
              codeGenerator.genCodeLine ("    try {");
              break;
            case CPP:
              codeGenerator.genCodeLine ("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\"" +
                                         JavaCCGlobals.addUnicodeEscapes (cp.getLhs ()) +
                                         "\"); });");
              codeGenerator.genCodeLine ("    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\"" +
                                         JavaCCGlobals.addUnicodeEscapes (cp.getLhs ()) +
                                         "\"); });");
              codeGenerator.genCodeLine ("    try {");
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }

        }
        if (cp.getCodeTokens ().size () != 0)
        {
          codeGenerator.printTokenSetup (cp.getCodeTokens ().get (0));
          s_cline--;
          codeGenerator.printTokenList (cp.getCodeTokens ());
        }
        codeGenerator.genCodeNewLine ();
        if (Options.isDebugParser ())
        {
          codeGenerator.genCodeLine ("    } catch(...) { }");
        }
        codeGenerator.genCodeLine ("  }");
        codeGenerator.genCodeNewLine ();
      }
      else
        if (p instanceof CodeProductionJava)
        {
          if (!eOutputLanguage.isJava ())
          {
            JavaCCErrors.semantic_error ("Cannot use JAVACODE productions with non-Java output.");
            continue;
          }
          final CodeProductionJava jp = (CodeProductionJava) p;
          Token t = jp.getReturnTypeTokens ().get (0);
          codeGenerator.printTokenSetup (t);
          s_ccol = 1;
          codeGenerator.printLeadingComments (t);
          codeGenerator.genCode ("  " + (p.getAccessMod () != null ? p.getAccessMod () + " " : ""));
          s_cline = t.beginLine;
          s_ccol = t.beginColumn;
          codeGenerator.printTokenOnly (t);
          for (int i = 1; i < jp.getReturnTypeTokens ().size (); i++)
          {
            t = jp.getReturnTypeTokens ().get (i);
            codeGenerator.printToken (t);
          }
          codeGenerator.printTrailingComments (t);
          codeGenerator.genCode (" " + jp.getLhs () + "(");
          if (jp.getParameterListTokens ().size () != 0)
          {
            codeGenerator.printTokenSetup (jp.getParameterListTokens ().get (0));
            for (final Token aElement2 : jp.getParameterListTokens ())
            {
              t = aElement2;
              codeGenerator.printToken (t);
            }
            codeGenerator.printTrailingComments (t);
          }
          codeGenerator.genCode (")");
          switch (eOutputLanguage)
          {
            case JAVA:
              codeGenerator.genCode (" throws ParseException");
              break;
            case CPP:
              // nothing
              break;
            default:
              throw new UnsupportedOutputLanguageException (eOutputLanguage);
          }
          for (final List <Token> aElement2 : jp.getThrowsList ())
          {
            codeGenerator.genCode (", ");
            for (final Token x : aElement2)
            {
              t = x;
              codeGenerator.genCode (t.image);
            }
          }
          codeGenerator.genCode (" {");
          if (Options.isDebugParser ())
          {
            codeGenerator.genCodeNewLine ();
            codeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (jp.getLhs ()) + "\");");
            codeGenerator.genCode ("    try {");
          }
          if (jp.getCodeTokens ().size () != 0)
          {
            codeGenerator.printTokenSetup ((jp.getCodeTokens ().get (0)));
            s_cline--;
            codeGenerator.printTokenList (jp.getCodeTokens ());
          }
          codeGenerator.genCodeNewLine ();
          if (Options.isDebugParser ())
          {
            codeGenerator.genCodeLine ("    } finally {");
            codeGenerator.genCodeLine ("      trace_return(\"" +
                                       JavaCCGlobals.addUnicodeEscapes (jp.getLhs ()) +
                                       "\");");
            codeGenerator.genCodeLine ("    }");
          }
          codeGenerator.genCodeLine ("  }");
          codeGenerator.genCodeNewLine ();
        }
        else
        {
          buildPhase1Routine ((BNFProduction) p);
        }
    }

    codeGenerator.switchToIncludeFile ();

    for (final ExpLookahead element : m_phase2list)
    {
      _buildPhase2Routine (element);
    }

    int phase3index = 0;
    while (phase3index < m_phase3list.size ())
    {
      for (; phase3index < m_phase3list.size (); phase3index++)
      {
        setupPhase3Builds (m_phase3list.get (phase3index));
      }
    }

    for (final Phase3Data data : m_phase3table.values ())
    {
      buildPhase3Routine (data, false);
    }

    if (false)
    {
      for (final Phase3Data inf : m_phase3table.values ())
      {
        PGPrinter.info ("**** Table for: " + inf.m_exp.getInternalName ());
        buildPhase3TableRec (inf);
        PGPrinter.info ("**** END TABLE *********");
      }
    }

    codeGenerator.switchToMainFile ();
  }

  public void reInit ()
  {
    m_nGenSymbolIndex = 0;
    m_nIndentCount = 0;
    m_bJJ2LA = false;
    m_phase2list.clear ();
    m_phase3list.clear ();
    m_phase3table.clear ();
    m_firstSet = null;
    m_xsp_declared = false;
    m_jj3_expansion = null;
  }

  // Table driven.
  void buildPhase3TableRec (final Phase3Data inf)
  {
    final Expansion e = inf.m_exp;
    if (e instanceof AbstractExpRegularExpression)
    {
      final AbstractExpRegularExpression e_nrw = (AbstractExpRegularExpression) e;
      PGPrinter.info ("TOKEN, " + e_nrw.getOrdinal ());
    }
    else
      if (e instanceof ExpNonTerminal)
      {
        final ExpNonTerminal e_nrw = (ExpNonTerminal) e;
        final NormalProduction ntprod = (PRODUCTION_TABLE.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          // javacode, true - always (warn?)
          PGPrinter.info ("JAVACODE_PROD, true");
        }
        else
        {
          final Expansion ntexp = ntprod.getExpansion ();
          // nt exp's table.
          PGPrinter.info ("PRODUCTION, " + ntexp.getInternalIndex ());
          if (false)
            buildPhase3TableRec (new Phase3Data (ntexp, inf.m_count));
        }
      }
      else
        if (e instanceof ExpChoice)
        {
          final ExpChoice e_nrw = (ExpChoice) e;
          PGPrinter.info ("CHOICE, ");
          for (int i = 0; i < e_nrw.getChoiceCount (); i++)
          {
            if (i > 0)
              PGPrinter.info ("\n|");
            final ExpSequence nested_seq = (ExpSequence) (e_nrw.getChoiceAt (i));
            final ExpLookahead la = (ExpLookahead) (nested_seq.getUnitAt (0));
            if (la.getActionTokens ().isNotEmpty ())
            {
              PGPrinter.info ("SEMANTIC,");
            }
            else
            {
              PGPrinter.info ("<start recurse>");
              buildPhase3TableRec (new Phase3Data (nested_seq, inf.m_count));
              PGPrinter.info ("<end recurse>");
            }
          }
          PGPrinter.info ();
        }
        else
          if (e instanceof ExpSequence)
          {
            final ExpSequence e_nrw = (ExpSequence) e;
            int cnt = inf.m_count;
            if (e_nrw.getUnitCount () > 2)
            {
              PGPrinter.info ("SEQ, " + cnt);
              for (int i = 1; i < e_nrw.getUnitCount (); i++)
              {
                final Expansion eseq = (e_nrw.getUnitAt (i));
                buildPhase3TableRec (new Phase3Data (eseq, cnt));
                cnt -= minimumSize (eseq);
                if (cnt <= 0)
                  break;
              }
            }
            else
            {
              Expansion tmp = e_nrw.getUnitAt (1);
              while (tmp instanceof ExpNonTerminal)
              {
                final NormalProduction ntprod = (PRODUCTION_TABLE.get (((ExpNonTerminal) tmp).getName ()));
                if (ntprod instanceof AbstractCodeProduction)
                  break;
                tmp = ntprod.getExpansion ();
              }
              buildPhase3TableRec (new Phase3Data (tmp, cnt));
            }
            PGPrinter.info ();
          }
          else
            if (e instanceof ExpTryBlock)
            {
              final ExpTryBlock e_nrw = (ExpTryBlock) e;
              buildPhase3TableRec (new Phase3Data (e_nrw.m_exp, inf.m_count));
            }
            else
              if (e instanceof ExpOneOrMore)
              {
                final ExpOneOrMore e_nrw = (ExpOneOrMore) e;
                PGPrinter.info ("SEQ PROD " + e_nrw.getExpansion ().getInternalIndex ());
                PGPrinter.info ("ZEROORMORE " + e_nrw.getExpansion ().getInternalIndex ());
              }
              else
                if (e instanceof ExpZeroOrMore)
                {
                  final ExpZeroOrMore e_nrw = (ExpZeroOrMore) e;
                  PGPrinter.info ("ZEROORMORE, " + e_nrw.getExpansion ().getInternalIndex ());
                }
                else
                  if (e instanceof ExpZeroOrOne)
                  {
                    final ExpZeroOrOne e_nrw = (ExpZeroOrOne) e;
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
final class Phase3Data
{
  /*
   * This is the expansion to generate the jj3 method for.
   */
  final Expansion m_exp;

  /*
   * This is the number of tokens that can still be consumed. This number is used to limit the
   * number of jj3 methods generated.
   */
  final int m_count;

  Phase3Data (final Expansion e, final int c)
  {
    m_exp = e;
    m_count = c;
  }
}
