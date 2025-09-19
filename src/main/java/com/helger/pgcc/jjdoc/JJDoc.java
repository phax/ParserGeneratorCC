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
package com.helger.pgcc.jjdoc;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.BNFProduction;
import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.RegExprSpec;
import com.helger.pgcc.parser.Token;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.*;

/**
 * The main entry point for JJDoc.
 */
public final class JJDoc
{
  static void start () throws IOException
  {
    JJDocGlobals.s_generator = JJDocGlobals.getGenerator ();
    JJDocGlobals.s_generator.documentStart ();
    _emitTokenProductions (JJDocGlobals.s_generator, JavaCCGlobals.REXPR_LIST);
    _emitNormalProductions (JJDocGlobals.s_generator, JavaCCGlobals.BNF_PRODUCTIONS);
    JJDocGlobals.s_generator.documentEnd ();
  }

  private static Token _getPrecedingSpecialToken (final Token tok)
  {
    Token t = tok;
    while (t.specialToken != null)
    {
      t = t.specialToken;
    }
    return t != tok ? t : null;
  }

  private static void _emitTopLevelSpecialTokens (final Token aTok, final IDocGenerator gen) throws IOException
  {
    if (aTok == null)
    {
      // Strange ...
      return;
    }
    Token tok = _getPrecedingSpecialToken (aTok);
    String s = "";
    if (tok != null)
    {
      JavaCCGlobals.s_cline = tok.beginLine;
      JavaCCGlobals.s_ccol = tok.beginColumn;
      while (tok != null)
      {
        s += JavaCCGlobals.printTokenOnly (tok);
        tok = tok.next;
      }
    }
    if (s.length () > 0)
      gen.specialTokens (s);
  }

  /*
   * private static boolean toplevelExpansion(Expansion exp) { return exp.parent
   * != null && ( (exp.parent instanceof NormalProduction) || (exp.parent
   * instanceof TokenProduction) ); }
   */

  private static void _emitTokenProductions (final IDocGenerator gen, final List <TokenProduction> prods) throws IOException
  {
    gen.tokensStart ();
    // FIXME there are many empty productions here
    for (final TokenProduction aTokenProduction : prods)
    {
      final TokenProduction tp = aTokenProduction;
      _emitTopLevelSpecialTokens (tp.m_firstToken, gen);

      gen.handleTokenProduction (tp);

      // if (!token.equals("")) {
      // gen.tokenStart(tp);
      // String token = getStandardTokenProductionText(tp);
      // gen.text(token);
      // gen.tokenEnd(tp);
      // }
    }
    gen.tokensEnd ();
  }

  public static String getStandardTokenProductionText (final TokenProduction tp)
  {
    String token = "";
    if (tp.m_isExplicit)
    {
      if (tp.m_lexStates == null)
      {
        token += "<*> ";
      }
      else
      {
        token += "<";
        for (int i = 0; i < tp.m_lexStates.length; ++i)
        {
          token += tp.m_lexStates[i];
          if (i < tp.m_lexStates.length - 1)
          {
            token += ",";
          }
        }
        token += "> ";
      }
      token += tp.m_kind.getImage ();
      if (tp.m_ignoreCase)
      {
        token += " [IGNORE_CASE]";
      }
      token += " : {\n";
      for (final Iterator <RegExprSpec> it2 = tp.m_respecs.iterator (); it2.hasNext ();)
      {
        final RegExprSpec res = it2.next ();

        token += emitRE (res.rexp);

        if (res.nsTok != null)
        {
          token += " : " + res.nsTok.image;
        }

        token += "\n";
        if (it2.hasNext ())
        {
          token += "| ";
        }
      }
      token += "}\n\n";
    }
    return token;
  }

  private static void _emitNormalProductions (final IDocGenerator gen, final List <NormalProduction> prods) throws IOException
  {
    gen.nonterminalsStart ();
    for (final NormalProduction np : prods)
    {
      _emitTopLevelSpecialTokens (np.getFirstToken (), gen);
      if (np instanceof BNFProduction)
      {
        gen.productionStart (np);
        if (np.getExpansion () instanceof ExpChoice)
        {
          boolean first = true;
          final ExpChoice c = (ExpChoice) np.getExpansion ();
          for (final Expansion e : c.getChoices ())
          {
            gen.expansionStart (e, first);
            _emitExpansionTree (e, gen);
            gen.expansionEnd (e, first);
            first = false;
          }
        }
        else
        {
          gen.expansionStart (np.getExpansion (), true);
          _emitExpansionTree (np.getExpansion (), gen);
          gen.expansionEnd (np.getExpansion (), true);
        }
        gen.productionEnd (np);
      }
      else
        if (np instanceof CodeProductionCpp)
        {
          gen.cppcode ((CodeProductionCpp) np);
        }
        else
          if (np instanceof CodeProductionJava)
          {
            gen.javacode ((CodeProductionJava) np);
          }
    }
    gen.nonterminalsEnd ();
  }

  private static void _emitExpansionTree (final Expansion exp, final IDocGenerator gen) throws IOException
  {
    // gen.text("[->" + exp.getClass().getName() + "]");
    if (exp instanceof ExpAction)
    {
      _emitExpansionAction ((ExpAction) exp, gen);
    }
    else
      if (exp instanceof ExpChoice)
      {
        _emitExpansionChoice ((ExpChoice) exp, gen);
      }
      else
        if (exp instanceof ExpLookahead)
        {
          _emitExpansionLookahead ((ExpLookahead) exp, gen);
        }
        else
          if (exp instanceof ExpNonTerminal)
          {
            _emitExpansionNonTerminal ((ExpNonTerminal) exp, gen);
          }
          else
            if (exp instanceof ExpOneOrMore)
            {
              _emitExpansionOneOrMore ((ExpOneOrMore) exp, gen);
            }
            else
              if (exp instanceof AbstractExpRegularExpression)
              {
                _emitExpansionRegularExpression ((AbstractExpRegularExpression) exp, gen);
              }
              else
                if (exp instanceof ExpSequence)
                {
                  _emitExpansionSequence ((ExpSequence) exp, gen);
                }
                else
                  if (exp instanceof ExpTryBlock)
                  {
                    _emitExpansionTryBlock ((ExpTryBlock) exp, gen);
                  }
                  else
                    if (exp instanceof ExpZeroOrMore)
                    {
                      _emitExpansionZeroOrMore ((ExpZeroOrMore) exp, gen);
                    }
                    else
                      if (exp instanceof ExpZeroOrOne)
                      {
                        _emitExpansionZeroOrOne ((ExpZeroOrOne) exp, gen);
                      }
                      else
                      {
                        PGPrinter.error ("Oops: Unknown expansion type.");
                      }
    // gen.text("[<-" + exp.getClass().getName() + "]");
  }

  private static void _emitExpansionAction (final ExpAction a, final IDocGenerator gen)
  {
    gen.doNothing (a);
  }

  private static void _emitExpansionChoice (final ExpChoice c, final IDocGenerator gen) throws IOException
  {
    boolean first = true;
    for (final Expansion e : c.getChoices ())
    {
      if (first)
        first = false;
      else
        gen.text (" | ");
      _emitExpansionTree (e, gen);
    }
  }

  private static void _emitExpansionLookahead (final ExpLookahead l, final IDocGenerator gen)
  {
    gen.doNothing (l);
  }

  private static void _emitExpansionNonTerminal (final ExpNonTerminal nt, final IDocGenerator gen) throws IOException
  {
    gen.nonTerminalStart (nt);
    gen.text (nt.getName ());
    gen.nonTerminalEnd (nt);
  }

  private static void _emitExpansionOneOrMore (final ExpOneOrMore o, final IDocGenerator gen) throws IOException
  {
    gen.text ("( ");
    _emitExpansionTree (o.getExpansion (), gen);
    gen.text (" )+");
  }

  private static void _emitExpansionRegularExpression (final AbstractExpRegularExpression r, final IDocGenerator gen) throws IOException
  {
    final String reRendered = emitRE (r);
    if (StringHelper.isNotEmpty (reRendered))
    {
      gen.reStart (r);
      gen.text (reRendered);
      gen.reEnd (r);
    }
  }

  private static void _emitExpansionSequence (final ExpSequence s, final IDocGenerator gen) throws IOException
  {
    boolean firstUnit = true;
    for (final Expansion e : s.getUnits ())
    {
      if (e instanceof ExpLookahead || e instanceof ExpAction)
      {
        continue;
      }
      if (!firstUnit)
      {
        gen.text (" ");
      }
      final boolean needParens = (e instanceof ExpChoice) || (e instanceof ExpSequence);
      if (needParens)
      {
        gen.text ("( ");
      }
      _emitExpansionTree (e, gen);
      if (needParens)
      {
        gen.text (" )");
      }
      firstUnit = false;
    }
  }

  private static void _emitExpansionTryBlock (final ExpTryBlock t, final IDocGenerator gen) throws IOException
  {
    final boolean needParens = t.m_exp instanceof ExpChoice;
    if (needParens)
    {
      gen.text ("( ");
    }
    _emitExpansionTree (t.m_exp, gen);
    if (needParens)
    {
      gen.text (" )");
    }
  }

  private static void _emitExpansionZeroOrMore (final ExpZeroOrMore z, final IDocGenerator gen) throws IOException
  {
    gen.text ("( ");
    _emitExpansionTree (z.getExpansion (), gen);
    gen.text (" )*");
  }

  private static void _emitExpansionZeroOrOne (final ExpZeroOrOne z, final IDocGenerator gen) throws IOException
  {
    gen.text ("( ");
    _emitExpansionTree (z.getExpansion (), gen);
    gen.text (" )?");
  }

  public static String emitRE (final AbstractExpRegularExpression re)
  {
    String returnString = "";
    final boolean hasLabel = StringHelper.isNotEmpty (re.getLabel ());
    final boolean justName = re instanceof ExpRJustName;
    final boolean eof = re instanceof ExpREndOfFile;
    final boolean isString = re instanceof ExpRStringLiteral;
    final boolean toplevelRE = re.m_tpContext != null;
    final boolean needBrackets = justName || eof || hasLabel || (!isString && toplevelRE);
    if (needBrackets)
    {
      returnString += "<";
      if (!justName)
      {
        if (re.m_private_rexp)
        {
          returnString += "#";
        }
        if (hasLabel)
        {
          returnString += re.getLabel ();
          returnString += ": ";
        }
      }
    }
    if (re instanceof ExpRCharacterList)
    {
      final ExpRCharacterList cl = (ExpRCharacterList) re;
      if (cl.isNegatedList ())
      {
        returnString += "~";
      }
      returnString += "[";
      boolean bFirst = true;
      for (final ICCCharacter o : cl.getDescriptors ())
      {
        if (bFirst)
          bFirst = false;
        else
          returnString += ",";

        if (o instanceof SingleCharacter)
        {
          returnString += "\"";
          final char s[] = { ((SingleCharacter) o).getChar () };
          returnString += JavaCCGlobals.addEscapes (new String (s));
          returnString += "\"";
        }
        else
          if (o instanceof CharacterRange)
          {
            returnString += "\"";
            final char s[] = { ((CharacterRange) o).getLeft () };
            returnString += JavaCCGlobals.addEscapes (new String (s));
            returnString += "\"-\"";
            s[0] = ((CharacterRange) o).getRight ();
            returnString += JavaCCGlobals.addEscapes (new String (s));
            returnString += "\"";
          }
          else
          {
            PGPrinter.error ("Oops: unknown character list element type.");
          }
      }
      returnString += "]";
    }
    else
      if (re instanceof ExpRChoice)
      {
        final ExpRChoice c = (ExpRChoice) re;
        for (final Iterator <AbstractExpRegularExpression> it = c.getChoices ().iterator (); it.hasNext ();)
        {
          final AbstractExpRegularExpression sub = (it.next ());
          returnString += emitRE (sub);
          if (it.hasNext ())
          {
            returnString += " | ";
          }
        }
      }
      else
        if (re instanceof ExpREndOfFile)
        {
          returnString += "EOF";
        }
        else
          if (re instanceof ExpRJustName)
          {
            final ExpRJustName jn = (ExpRJustName) re;
            returnString += jn.getLabel ();
          }
          else
            if (re instanceof ExpROneOrMore)
            {
              final ExpROneOrMore om = (ExpROneOrMore) re;
              returnString += "(";
              returnString += emitRE (om.getRegExpr ());
              returnString += ")+";
            }
            else
              if (re instanceof ExpRSequence)
              {
                final ExpRSequence s = (ExpRSequence) re;
                boolean bFirst = true;
                for (final AbstractExpRegularExpression sub : s.getUnits ())
                {
                  if (bFirst)
                    bFirst = false;
                  else
                    returnString += " ";

                  final boolean needParens = sub instanceof ExpRChoice;
                  if (needParens)
                    returnString += "(";
                  returnString += emitRE (sub);
                  if (needParens)
                    returnString += ")";
                }
              }
              else
                if (re instanceof ExpRStringLiteral)
                {
                  final ExpRStringLiteral sl = (ExpRStringLiteral) re;
                  returnString += ("\"" + JavaCCGlobals.addEscapes (sl.m_image) + "\"");
                }
                else
                  if (re instanceof ExpRZeroOrMore)
                  {
                    final ExpRZeroOrMore zm = (ExpRZeroOrMore) re;
                    returnString += "(";
                    returnString += emitRE (zm.getRegExpr ());
                    returnString += ")*";
                  }
                  else
                    if (re instanceof ExpRZeroOrOne)
                    {
                      final ExpRZeroOrOne zo = (ExpRZeroOrOne) re;
                      returnString += "(";
                      returnString += emitRE (zo.getRegExpr ());
                      returnString += ")?";
                    }
                    else
                      if (re instanceof ExpRRepetitionRange)
                      {
                        final ExpRRepetitionRange zo = (ExpRRepetitionRange) re;
                        returnString += "(";
                        returnString += emitRE (zo.getRegExpr ());
                        returnString += ")";
                        returnString += "{";
                        returnString += zo.getMin ();
                        if (zo.hasMax ())
                        {
                          returnString += ",";
                          returnString += zo.getMax ();
                        }
                        returnString += "}";
                      }
                      else
                      {
                        PGPrinter.error ("Oops: Unknown regular expression type.");
                      }
    if (needBrackets)
    {
      returnString += ">";
    }
    return returnString;
  }

  /*
   * private static String v2s(List v, boolean newLine) { String s = ""; boolean
   * firstToken = true; for (Enumeration enumeration = v.elements();
   * enumeration.hasMoreElements();) { Token tok =
   * (Token)enumeration.nextElement(); Token stok =
   * getPrecedingSpecialToken(tok); if (firstToken) { if (stok != null) { cline
   * = stok.beginLine; ccol = stok.beginColumn; } else { cline = tok.beginLine;
   * ccol = tok.beginColumn; } s = ws(ccol - 1); firstToken = false; } while
   * (stok != null) { s += printToken(stok); stok = stok.next; } s +=
   * printToken(tok); } return s; }
   */
  /**
   * A utility to produce a string of blanks.
   */

  /*
   * private static String ws(int len) { String s = ""; for (int i = 0; i < len;
   * ++i) { s += " "; } return s; }
   */

}
