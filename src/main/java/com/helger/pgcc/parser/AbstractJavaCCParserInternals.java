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

import java.util.HashMap;
import java.util.List;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;
import com.helger.pgcc.parser.exp.ExpREndOfFile;
import com.helger.pgcc.parser.exp.ExpTryBlock;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * Utilities.
 */
public abstract class AbstractJavaCCParserInternals
{
  /**
   * Class to hold modifiers.
   */
  public static final class ModifierSet
  {
    /* Definitions of the bits in the modifiers field. */
    public static final int PUBLIC = 0x0001;
    public static final int PROTECTED = 0x0002;
    public static final int PRIVATE = 0x0004;
    public static final int ABSTRACT = 0x0008;
    public static final int STATIC = 0x0010;
    public static final int FINAL = 0x0020;
    public static final int SYNCHRONIZED = 0x0040;
    public static final int NATIVE = 0x0080;
    public static final int TRANSIENT = 0x0100;
    public static final int VOLATILE = 0x0200;
    public static final int STRICTFP = 0x1000;

    private ModifierSet ()
    {}

    /*
     * A set of accessors that indicate whether the specified modifier is in the set.
     */

    public static boolean isPublic (final int nModifiers)
    {
      return (nModifiers & PUBLIC) != 0;
    }

    public static boolean isProtected (final int nModifiers)
    {
      return (nModifiers & PROTECTED) != 0;
    }

    public static boolean isPrivate (final int nModifiers)
    {
      return (nModifiers & PRIVATE) != 0;
    }

    public static boolean isStatic (final int nModifiers)
    {
      return (nModifiers & STATIC) != 0;
    }

    public static boolean isAbstract (final int nModifiers)
    {
      return (nModifiers & ABSTRACT) != 0;
    }

    public static boolean isFinal (final int nModifiers)
    {
      return (nModifiers & FINAL) != 0;
    }

    public static boolean isNative (final int nModifiers)
    {
      return (nModifiers & NATIVE) != 0;
    }

    public static boolean isStrictfp (final int nModifiers)
    {
      return (nModifiers & STRICTFP) != 0;
    }

    public static boolean isSynchronized (final int nModifiers)
    {
      return (nModifiers & SYNCHRONIZED) != 0;
    }

    public static boolean isTransient (final int nModifiers)
    {
      return (nModifiers & TRANSIENT) != 0;
    }

    public static boolean isVolatile (final int nModifiers)
    {
      return (nModifiers & VOLATILE) != 0;
    }

    /**
     * Removes the given modifier.
     */
    static int removeModifier (final int nModifiers, final int nModToRemove)
    {
      return nModifiers & ~nModToRemove;
    }
  }

  protected static void initialize ()
  {
    final Integer i = Integer.valueOf (0);
    grammar ().lexStateS2I ().put ("DEFAULT", i);
    grammar ().lexStateI2S ().put (i, "DEFAULT");
    grammar ().simpleTokensTable ().put ("DEFAULT", new HashMap <> ());
  }

  protected static void addcuname (final String sId)
  {
    grammar ().setParserName (sId);
  }

  protected static void compare (final Token t, final String sId1, final String sId2)
  {
    if (!sId2.equals (sId1))
    {
      JavaCCErrors.parse_error (t, "Name " + sId2 + " must be the same as that used at PARSER_BEGIN (" + sId1 + ")");
    }
  }

  protected static void setinsertionpoint (final Token t, final int nNo)
  {
    do
    {
      PGCCContext.current ()
                 .parserBuild ()
                 .getAddTokenHere ()
                 .add (PGCCContext.current ().parserBuild ().getFirstToken ());
      PGCCContext.current ().parserBuild ().setFirstToken (PGCCContext.current ().parserBuild ().getFirstToken ().next);
    } while (PGCCContext.current ().parserBuild ().getFirstToken () != t);
    if (nNo == 1)
    {
      if (PGCCContext.current ().parserBuild ().isInsertionPoint1Set ())
      {
        JavaCCErrors.parse_error (t, "Multiple declaration of parser class.");
      }
      else
      {
        PGCCContext.current ().parserBuild ().setInsertionPoint1Set (true);
        PGCCContext.current ().parserBuild ().switchToInsertionPoint2 ();
      }
    }
    else
    {
      PGCCContext.current ().parserBuild ().switchToAfterInsertionPoint2 ();
      PGCCContext.current ().parserBuild ().setInsertionPoint2Set (true);
    }
    PGCCContext.current ().parserBuild ().setFirstToken (t);
  }

  protected static void insertionpointerrors (final Token t)
  {
    while (PGCCContext.current ().parserBuild ().getFirstToken () != t)
    {
      PGCCContext.current ()
                 .parserBuild ()
                 .getAddTokenHere ()
                 .add (PGCCContext.current ().parserBuild ().getFirstToken ());
      PGCCContext.current ().parserBuild ().setFirstToken (PGCCContext.current ().parserBuild ().getFirstToken ().next);
    }
    if (!PGCCContext.current ().parserBuild ().isInsertionPoint1Set () ||
      !PGCCContext.current ().parserBuild ().isInsertionPoint2Set ())
    {
      JavaCCErrors.parse_error (t, "Parser class has not been defined between PARSER_BEGIN and PARSER_END.");
    }
  }

  protected static void set_initial_cu_token (final Token t)
  {
    PGCCContext.current ().parserBuild ().setFirstToken (t);
  }

  protected static void addProduction (final AbstractNormalProduction p)
  {
    grammar ().bnfProductions ().add (p);
  }

  protected static void productionAddExpansion (final BNFProduction p, final Expansion e)
  {
    e.setParent (p);
    p.setExpansion (e);
  }

  protected static void addregexpr (final TokenProduction p)
  {
    grammar ().rexprList ().add (p);
    if (Options.isUserTokenManager ())
    {
      if (p.m_aLexStates == null || p.m_aLexStates.length != 1 || !p.m_aLexStates[0].equals ("DEFAULT"))
      {
        JavaCCErrors.warning (p,
                              "Ignoring lexical state specifications since option " +
                                 "USER_TOKEN_MANAGER has been set to true.");
      }
    }
    if (p.m_aLexStates == null)
    {
      return;
    }
    for (int i = 0; i < p.m_aLexStates.length; i++)
    {
      for (int j = 0; j < i; j++)
      {
        if (p.m_aLexStates[i].equals (p.m_aLexStates[j]))
        {
          JavaCCErrors.parse_error (p, "Multiple occurrence of \"" + p.m_aLexStates[i] + "\" in lexical state list.");
        }
      }
      if (grammar ().lexStateS2I ().get (p.m_aLexStates[i]) == null)
      {
        final Integer aIi = Integer.valueOf (PGCCContext.current ().parserBuild ().getAndIncNextFreeLexState ());
        grammar ().lexStateS2I ().put (p.m_aLexStates[i], aIi);
        grammar ().lexStateI2S ().put (aIi, p.m_aLexStates[i]);
        grammar ().simpleTokensTable ().put (p.m_aLexStates[i], new HashMap <> ());
      }
    }
  }

  protected static void add_token_manager_decls (final Token t, final List <Token> aDecls)
  {
    if (grammar ().getTokenMgrDecls () != null)
    {
      JavaCCErrors.parse_error (t, "Multiple occurrence of \"TOKEN_MGR_DECLS\".");
    }
    else
    {
      grammar ().setTokenMgrDecls (new CommonsArrayList <> (aDecls));
      if (Options.isUserTokenManager ())
      {
        JavaCCErrors.warning (t,
                              "Ignoring declarations in \"TOKEN_MGR_DECLS\" since option " +
                                 "USER_TOKEN_MANAGER has been set to true.");
      }
    }
  }

  protected static void add_inline_regexpr (final AbstractExpRegularExpression r)
  {
    if (!(r instanceof ExpREndOfFile))
    {
      final TokenProduction p = new TokenProduction ();
      p.m_bIsExplicit = false;
      p.m_aLexStates = new String [] { "DEFAULT" };
      p.m_eKind = ETokenKind.TOKEN;
      final RegExprSpec aRes = new RegExprSpec ();
      aRes.m_aRexp = r;
      aRes.m_aRexp.m_aTpContext = p;
      aRes.m_aAct = new ExpAction ();
      aRes.m_sNextState = null;
      aRes.m_aNsTok = null;
      p.m_aRespecs.add (aRes);
      grammar ().rexprList ().add (p);
    }
  }

  private static boolean _isHexchar (final char cCh)
  {
    if (cCh >= '0' && cCh <= '9')
      return true;
    if (cCh >= 'A' && cCh <= 'F')
      return true;
    if (cCh >= 'a' && cCh <= 'f')
      return true;
    return false;
  }

  private static int _getHexVal (final char cCh)
  {
    if (cCh >= '0' && cCh <= '9')
      return (cCh) - ('0');
    if (cCh >= 'A' && cCh <= 'F')
      return (cCh) - ('A') + 10;
    return (cCh) - ('a') + 10;
  }

  protected static String remove_escapes_and_quotes (final Token t, final String sStr)
  {
    String sRetval = "";
    int nIndex = 1;
    char cCh, cCh1;
    int nOrdinal;
    while (nIndex < sStr.length () - 1)
    {
      if (sStr.charAt (nIndex) != '\\')
      {
        sRetval += sStr.charAt (nIndex);
        nIndex++;
        continue;
      }
      nIndex++;
      cCh = sStr.charAt (nIndex);
      if (cCh == 'b')
      {
        sRetval += '\b';
        nIndex++;
        continue;
      }
      if (cCh == 't')
      {
        sRetval += '\t';
        nIndex++;
        continue;
      }
      if (cCh == 'n')
      {
        sRetval += '\n';
        nIndex++;
        continue;
      }
      if (cCh == 'f')
      {
        sRetval += '\f';
        nIndex++;
        continue;
      }
      if (cCh == 'r')
      {
        sRetval += '\r';
        nIndex++;
        continue;
      }
      if (cCh == '"')
      {
        sRetval += '\"';
        nIndex++;
        continue;
      }
      if (cCh == '\'')
      {
        sRetval += '\'';
        nIndex++;
        continue;
      }
      if (cCh == '\\')
      {
        sRetval += '\\';
        nIndex++;
        continue;
      }
      if (cCh >= '0' && cCh <= '7')
      {
        nOrdinal = (cCh) - ('0');
        nIndex++;
        cCh1 = sStr.charAt (nIndex);
        if (cCh1 >= '0' && cCh1 <= '7')
        {
          nOrdinal = nOrdinal * 8 + (cCh1) - ('0');
          nIndex++;
          cCh1 = sStr.charAt (nIndex);
          if (cCh <= '3' && cCh1 >= '0' && cCh1 <= '7')
          {
            nOrdinal = nOrdinal * 8 + (cCh1) - ('0');
            nIndex++;
          }
        }
        sRetval += (char) nOrdinal;
        continue;
      }
      if (cCh == 'u')
      {
        nIndex++;
        cCh = sStr.charAt (nIndex);
        if (_isHexchar (cCh))
        {
          nOrdinal = _getHexVal (cCh);
          nIndex++;
          cCh = sStr.charAt (nIndex);
          if (_isHexchar (cCh))
          {
            nOrdinal = nOrdinal * 16 + _getHexVal (cCh);
            nIndex++;
            cCh = sStr.charAt (nIndex);
            if (_isHexchar (cCh))
            {
              nOrdinal = nOrdinal * 16 + _getHexVal (cCh);
              nIndex++;
              cCh = sStr.charAt (nIndex);
              if (_isHexchar (cCh))
              {
                nOrdinal = nOrdinal * 16 + _getHexVal (cCh);
                nIndex++;
                continue;
              }
            }
          }
        }
        JavaCCErrors.parse_error (t,
                                  "Encountered non-hex character '" +
                                     cCh +
                                     "' at position " +
                                     nIndex +
                                     " of string " +
                                     "- Unicode escape must have 4 hex digits after it.");
        return sRetval;
      }
      JavaCCErrors.parse_error (t, "Illegal escape sequence '\\" + cCh + "' at position " + nIndex + " of string.");
      return sRetval;
    }
    return sRetval;
  }

  protected static char character_descriptor_assign (final Token t, final String s)
  {
    if (s.length () != 1)
    {
      JavaCCErrors.parse_error (t, "String in character list may contain only one character.");
      return ' ';
    }
    return s.charAt (0);
  }

  protected static char character_descriptor_assign (final Token t, final String s, final String sLeft)
  {
    if (s.length () != 1)
    {
      JavaCCErrors.parse_error (t, "String in character list may contain only one character.");
      return ' ';
    }
    if (sLeft.charAt (0) > s.charAt (0))
    {
      JavaCCErrors.parse_error (t,
                                "Right end of character range \'" +
                                   s +
                                   "\' has a lower ordinal value than the left end of character range \'" +
                                   sLeft +
                                   "\'.");
      return sLeft.charAt (0);
    }
    return s.charAt (0);
  }

  protected static void makeTryBlock (final Token aTryLoc,
                                      final Container aResult,
                                      final Container aNestedExp,
                                      final List <List <Token>> types,
                                      final List <Token> ids,
                                      final List <List <Token>> catchblks,
                                      final List <Token> aFinallyblk)
  {
    if (catchblks.size () == 0 && aFinallyblk == null)
    {
      JavaCCErrors.parse_error (aTryLoc, "Try block must contain at least one catch or finally block.");
      return;
    }
    final ExpTryBlock aTblk = new ExpTryBlock ();
    aTblk.setLineNumber (aTryLoc.beginLine);
    aTblk.setColumnNumber (aTryLoc.beginColumn);
    aTblk.m_aExp = (Expansion) aNestedExp.m_aMember;
    aTblk.m_aExp.setParent (aTblk);
    aTblk.m_aExp.setOrdinalBase (0);
    aTblk.m_aTypes = types;
    aTblk.m_aIds = ids;
    aTblk.m_aCatchblks = catchblks;
    aTblk.m_aFinallyblk = aFinallyblk;
    aResult.m_aMember = aTblk;
  }

}
