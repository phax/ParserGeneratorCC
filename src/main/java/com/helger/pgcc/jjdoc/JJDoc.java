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
package com.helger.pgcc.jjdoc;

import static com.helger.pgcc.parser.JavaCCGlobals.grammar;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.context.PGCCContext;
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
    PGCCContext.current ().jjdoc ().setGenerator (JJDocGlobals.getGenerator ());
    PGCCContext.current ().jjdoc ().getGenerator ().documentStart ();
    _emitTokenProductions (PGCCContext.current ().jjdoc ().getGenerator (), grammar ().rexprList ());
    _emitNormalProductions (PGCCContext.current ().jjdoc ().getGenerator (), grammar ().bnfProductions ());
    PGCCContext.current ().jjdoc ().getGenerator ().documentEnd ();
  }

  private static Token _getPrecedingSpecialToken (final Token aTok)
  {
    Token t = aTok;
    while (t.specialToken != null)
    {
      t = t.specialToken;
    }
    return t != aTok ? t : null;
  }

  private static void _emitTopLevelSpecialTokens (final Token aTok, final IDocGenerator aGen) throws IOException
  {
    if (aTok == null)
    {
      // Strange ...
      return;
    }
    Token aSpecial = _getPrecedingSpecialToken (aTok);
    String s = "";
    if (aSpecial != null)
    {
      grammar ().setCurrentLine (aSpecial.beginLine);
      grammar ().setCurrentColumn (aSpecial.beginColumn);
      while (aSpecial != null)
      {
        s += JavaCCGlobals.printTokenOnly (aSpecial);
        aSpecial = aSpecial.next;
      }
    }
    if (s.length () > 0)
      aGen.specialTokens (s);
  }

  /*
   * private static boolean toplevelExpansion(Expansion exp) { return exp.parent != null && (
   * (exp.parent instanceof NormalProduction) || (exp.parent instanceof TokenProduction) ); }
   */

  private static void _emitTokenProductions (final IDocGenerator aGen, final List <TokenProduction> aProds)
                                                                                                          throws IOException
  {
    aGen.tokensStart ();
    // FIXME there are many empty productions here
    for (final TokenProduction aTokenProduction : aProds)
    {
      final TokenProduction aTp = aTokenProduction;
      _emitTopLevelSpecialTokens (aTp.m_firstToken, aGen);

      aGen.handleTokenProduction (aTp);

      // if (!token.equals("")) {
      // gen.tokenStart(tp);
      // String token = getStandardTokenProductionText(tp);
      // gen.text(token);
      // gen.tokenEnd(tp);
      // }
    }
    aGen.tokensEnd ();
  }

  public static String getStandardTokenProductionText (final TokenProduction aTp)
  {
    String sToken = "";
    if (aTp.m_isExplicit)
    {
      if (aTp.m_lexStates == null)
      {
        sToken += "<*> ";
      }
      else
      {
        sToken += "<";
        for (int i = 0; i < aTp.m_lexStates.length; ++i)
        {
          sToken += aTp.m_lexStates[i];
          if (i < aTp.m_lexStates.length - 1)
          {
            sToken += ",";
          }
        }
        sToken += "> ";
      }
      sToken += aTp.m_kind.getImage ();
      if (aTp.m_ignoreCase)
      {
        sToken += " [IGNORE_CASE]";
      }
      sToken += " : {\n";
      for (final Iterator <RegExprSpec> aIt2 = aTp.m_respecs.iterator (); aIt2.hasNext ();)
      {
        final RegExprSpec aRes = aIt2.next ();

        sToken += emitRE (aRes.m_aRexp);

        if (aRes.m_aNsTok != null)
        {
          sToken += " : " + aRes.m_aNsTok.image;
        }

        sToken += "\n";
        if (aIt2.hasNext ())
        {
          sToken += "| ";
        }
      }
      sToken += "}\n\n";
    }
    return sToken;
  }

  private static void _emitNormalProductions (final IDocGenerator aGen, final List <NormalProduction> aProds)
                                                                                                            throws IOException
  {
    aGen.nonterminalsStart ();
    for (final NormalProduction np : aProds)
    {
      _emitTopLevelSpecialTokens (np.getFirstToken (), aGen);
      if (np instanceof BNFProduction)
      {
        aGen.productionStart (np);
        if (np.getExpansion () instanceof ExpChoice)
        {
          boolean bFirst = true;
          final ExpChoice c = (ExpChoice) np.getExpansion ();
          for (final Expansion e : c.getChoices ())
          {
            aGen.expansionStart (e, bFirst);
            _emitExpansionTree (e, aGen);
            aGen.expansionEnd (e, bFirst);
            bFirst = false;
          }
        }
        else
        {
          aGen.expansionStart (np.getExpansion (), true);
          _emitExpansionTree (np.getExpansion (), aGen);
          aGen.expansionEnd (np.getExpansion (), true);
        }
        aGen.productionEnd (np);
      }
      else
        if (np instanceof final CodeProductionCpp aCodeProductionCpp)
        {
          aGen.cppcode (aCodeProductionCpp);
        }
        else
          if (np instanceof final CodeProductionJava aCodeProductionJava)
          {
            aGen.javacode (aCodeProductionJava);
          }
    }
    aGen.nonterminalsEnd ();
  }

  private static void _emitExpansionTree (final Expansion aExp, final IDocGenerator aGen) throws IOException
  {
    // gen.text("[->" + exp.getClass().getName() + "]");
    if (aExp instanceof final ExpAction aAction)
    {
      _emitExpansionAction (aAction, aGen);
    }
    else
      if (aExp instanceof final ExpChoice aChoice)
      {
        _emitExpansionChoice (aChoice, aGen);
      }
      else
        if (aExp instanceof final ExpLookahead aLookahead)
        {
          _emitExpansionLookahead (aLookahead, aGen);
        }
        else
          if (aExp instanceof final ExpNonTerminal aNonTerminal)
          {
            _emitExpansionNonTerminal (aNonTerminal, aGen);
          }
          else
            if (aExp instanceof final ExpOneOrMore aOneOrMore)
            {
              _emitExpansionOneOrMore (aOneOrMore, aGen);
            }
            else
              if (aExp instanceof final AbstractExpRegularExpression aRegularExpression)
              {
                _emitExpansionRegularExpression (aRegularExpression, aGen);
              }
              else
                if (aExp instanceof final ExpSequence aSequence)
                {
                  _emitExpansionSequence (aSequence, aGen);
                }
                else
                  if (aExp instanceof final ExpTryBlock aTryBlock)
                  {
                    _emitExpansionTryBlock (aTryBlock, aGen);
                  }
                  else
                    if (aExp instanceof final ExpZeroOrMore aZeroOrMore)
                    {
                      _emitExpansionZeroOrMore (aZeroOrMore, aGen);
                    }
                    else
                      if (aExp instanceof final ExpZeroOrOne aZeroOrOne)
                      {
                        _emitExpansionZeroOrOne (aZeroOrOne, aGen);
                      }
                      else
                      {
                        PGPrinter.error ("Oops: Unknown expansion type.");
                      }
    // gen.text("[<-" + exp.getClass().getName() + "]");
  }

  private static void _emitExpansionAction (final ExpAction a, final IDocGenerator aGen)
  {
    aGen.doNothing (a);
  }

  private static void _emitExpansionChoice (final ExpChoice c, final IDocGenerator aGen) throws IOException
  {
    boolean bFirst = true;
    for (final Expansion e : c.getChoices ())
    {
      if (bFirst)
        bFirst = false;
      else
        aGen.text (" | ");
      _emitExpansionTree (e, aGen);
    }
  }

  private static void _emitExpansionLookahead (final ExpLookahead l, final IDocGenerator aGen)
  {
    aGen.doNothing (l);
  }

  private static void _emitExpansionNonTerminal (final ExpNonTerminal aNt, final IDocGenerator aGen) throws IOException
  {
    aGen.nonTerminalStart (aNt);
    aGen.text (aNt.getName ());
    aGen.nonTerminalEnd (aNt);
  }

  private static void _emitExpansionOneOrMore (final ExpOneOrMore o, final IDocGenerator aGen) throws IOException
  {
    aGen.text ("( ");
    _emitExpansionTree (o.getExpansion (), aGen);
    aGen.text (" )+");
  }

  private static void _emitExpansionRegularExpression (final AbstractExpRegularExpression r, final IDocGenerator aGen)
                                                                                                                      throws IOException
  {
    final String sReRendered = emitRE (r);
    if (StringHelper.isNotEmpty (sReRendered))
    {
      aGen.reStart (r);
      aGen.text (sReRendered);
      aGen.reEnd (r);
    }
  }

  private static void _emitExpansionSequence (final ExpSequence s, final IDocGenerator aGen) throws IOException
  {
    boolean bFirstUnit = true;
    for (final Expansion e : s.getUnits ())
    {
      if (e instanceof ExpLookahead || e instanceof ExpAction)
      {
        continue;
      }
      if (!bFirstUnit)
      {
        aGen.text (" ");
      }
      final boolean bNeedParens = (e instanceof ExpChoice) || (e instanceof ExpSequence);
      if (bNeedParens)
      {
        aGen.text ("( ");
      }
      _emitExpansionTree (e, aGen);
      if (bNeedParens)
      {
        aGen.text (" )");
      }
      bFirstUnit = false;
    }
  }

  private static void _emitExpansionTryBlock (final ExpTryBlock t, final IDocGenerator aGen) throws IOException
  {
    final boolean bNeedParens = t.m_exp instanceof ExpChoice;
    if (bNeedParens)
    {
      aGen.text ("( ");
    }
    _emitExpansionTree (t.m_exp, aGen);
    if (bNeedParens)
    {
      aGen.text (" )");
    }
  }

  private static void _emitExpansionZeroOrMore (final ExpZeroOrMore z, final IDocGenerator aGen) throws IOException
  {
    aGen.text ("( ");
    _emitExpansionTree (z.getExpansion (), aGen);
    aGen.text (" )*");
  }

  private static void _emitExpansionZeroOrOne (final ExpZeroOrOne z, final IDocGenerator aGen) throws IOException
  {
    aGen.text ("( ");
    _emitExpansionTree (z.getExpansion (), aGen);
    aGen.text (" )?");
  }

  public static String emitRE (final AbstractExpRegularExpression aRe)
  {
    String sReturnString = "";
    final boolean bHasLabel = StringHelper.isNotEmpty (aRe.getLabel ());
    final boolean bJustName = aRe instanceof ExpRJustName;
    final boolean bEof = aRe instanceof ExpREndOfFile;
    final boolean bIsString = aRe instanceof ExpRStringLiteral;
    final boolean bToplevelRE = aRe.m_aTpContext != null;
    final boolean bNeedBrackets = bJustName || bEof || bHasLabel || (!bIsString && bToplevelRE);
    if (bNeedBrackets)
    {
      sReturnString += "<";
      if (!bJustName)
      {
        if (aRe.m_bPrivateRexp)
        {
          sReturnString += "#";
        }
        if (bHasLabel)
        {
          sReturnString += aRe.getLabel ();
          sReturnString += ": ";
        }
      }
    }
    if (aRe instanceof final ExpRCharacterList cl)
    {
      if (cl.isNegatedList ())
      {
        sReturnString += "~";
      }
      sReturnString += "[";
      boolean bFirst = true;
      for (final ICCCharacter o : cl.getDescriptors ())
      {
        if (bFirst)
          bFirst = false;
        else
          sReturnString += ",";

        if (o instanceof final SingleCharacter aSingleCharacter)
        {
          sReturnString += "\"";
          final char s[] = { aSingleCharacter.getChar () };
          sReturnString += JavaCCGlobals.addEscapes (new String (s));
          sReturnString += "\"";
        }
        else
          if (o instanceof final CharacterRange aCharacterRange)
          {
            sReturnString += "\"";
            final char s[] = { aCharacterRange.getLeft () };
            sReturnString += JavaCCGlobals.addEscapes (new String (s));
            sReturnString += "\"-\"";
            s[0] = aCharacterRange.getRight ();
            sReturnString += JavaCCGlobals.addEscapes (new String (s));
            sReturnString += "\"";
          }
          else
          {
            PGPrinter.error ("Oops: unknown character list element type.");
          }
      }
      sReturnString += "]";
    }
    else
      if (aRe instanceof final ExpRChoice c)
      {
        for (final Iterator <AbstractExpRegularExpression> aIt = c.getChoices ().iterator (); aIt.hasNext ();)
        {
          final AbstractExpRegularExpression aSub = (aIt.next ());
          sReturnString += emitRE (aSub);
          if (aIt.hasNext ())
          {
            sReturnString += " | ";
          }
        }
      }
      else
        if (aRe instanceof ExpREndOfFile)
        {
          sReturnString += "EOF";
        }
        else
          if (aRe instanceof final ExpRJustName jn)
          {
            sReturnString += jn.getLabel ();
          }
          else
            if (aRe instanceof final ExpROneOrMore om)
            {
              sReturnString += "(";
              sReturnString += emitRE (om.getRegExpr ());
              sReturnString += ")+";
            }
            else
              if (aRe instanceof final ExpRSequence s)
              {
                boolean bFirst = true;
                for (final AbstractExpRegularExpression sub : s.getUnits ())
                {
                  if (bFirst)
                    bFirst = false;
                  else
                    sReturnString += " ";

                  final boolean bNeedParens = sub instanceof ExpRChoice;
                  if (bNeedParens)
                    sReturnString += "(";
                  sReturnString += emitRE (sub);
                  if (bNeedParens)
                    sReturnString += ")";
                }
              }
              else
                if (aRe instanceof final ExpRStringLiteral sl)
                {
                  sReturnString += ("\"" + JavaCCGlobals.addEscapes (sl.m_image) + "\"");
                }
                else
                  if (aRe instanceof final ExpRZeroOrMore zm)
                  {
                    sReturnString += "(";
                    sReturnString += emitRE (zm.getRegExpr ());
                    sReturnString += ")*";
                  }
                  else
                    if (aRe instanceof final ExpRZeroOrOne zo)
                    {
                      sReturnString += "(";
                      sReturnString += emitRE (zo.getRegExpr ());
                      sReturnString += ")?";
                    }
                    else
                      if (aRe instanceof final ExpRRepetitionRange zo)
                      {
                        sReturnString += "(";
                        sReturnString += emitRE (zo.getRegExpr ());
                        sReturnString += ")";
                        sReturnString += "{";
                        sReturnString += zo.getMin ();
                        if (zo.hasMax ())
                        {
                          sReturnString += ",";
                          sReturnString += zo.getMax ();
                        }
                        sReturnString += "}";
                      }
                      else
                      {
                        PGPrinter.error ("Oops: Unknown regular expression type.");
                      }
    if (bNeedBrackets)
    {
      sReturnString += ">";
    }
    return sReturnString;
  }

  /*
   * private static String v2s(List v, boolean newLine) { String s = ""; boolean firstToken = true;
   * for (Enumeration enumeration = v.elements(); enumeration.hasMoreElements();) { Token tok =
   * (Token)enumeration.nextElement(); Token stok = getPrecedingSpecialToken(tok); if (firstToken) {
   * if (stok != null) { cline = stok.beginLine; ccol = stok.beginColumn; } else { cline =
   * tok.beginLine; ccol = tok.beginColumn; } s = ws(ccol - 1); firstToken = false; } while (stok !=
   * null) { s += printToken(stok); stok = stok.next; } s += printToken(tok); } return s; }
   */
  /**
   * A utility to produce a string of blanks.
   */

  /*
   * private static String ws(int len) { String s = ""; for (int i = 0; i < len; ++i) { s += " "; }
   * return s; }
   */

}
