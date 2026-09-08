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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
  /** Default constructor for the generated parser to extend. */
  protected AbstractJavaCCParserInternals ()
  {}

  /**
   * Class to hold modifiers.
   */
  public static final class ModifierSet
  {
    /** The bit standing for the <code>public</code> modifier. */
    public static final int PUBLIC = 0x0001;
    /** The bit standing for the <code>protected</code> modifier. */
    public static final int PROTECTED = 0x0002;
    /** The bit standing for the <code>private</code> modifier. */
    public static final int PRIVATE = 0x0004;
    /** The bit standing for the <code>abstract</code> modifier. */
    public static final int ABSTRACT = 0x0008;
    /** The bit standing for the <code>static</code> modifier. */
    public static final int STATIC = 0x0010;
    /** The bit standing for the <code>final</code> modifier. */
    public static final int FINAL = 0x0020;
    /** The bit standing for the <code>synchronized</code> modifier. */
    public static final int SYNCHRONIZED = 0x0040;
    /** The bit standing for the <code>native</code> modifier. */
    public static final int NATIVE = 0x0080;
    /** The bit standing for the <code>transient</code> modifier. */
    public static final int TRANSIENT = 0x0100;
    /** The bit standing for the <code>volatile</code> modifier. */
    public static final int VOLATILE = 0x0200;
    /** The bit standing for the <code>strictfp</code> modifier. */
    public static final int STRICTFP = 0x1000;

    private ModifierSet ()
    {}

    /*
     * A set of accessors that indicate whether the specified modifier is in the set.
     */

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>public</code> is among them.
     */
    public static boolean isPublic (final int nModifiers)
    {
      return (nModifiers & PUBLIC) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>protected</code> is among them.
     */
    public static boolean isProtected (final int nModifiers)
    {
      return (nModifiers & PROTECTED) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>private</code> is among them.
     */
    public static boolean isPrivate (final int nModifiers)
    {
      return (nModifiers & PRIVATE) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>static</code> is among them.
     */
    public static boolean isStatic (final int nModifiers)
    {
      return (nModifiers & STATIC) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>abstract</code> is among them.
     */
    public static boolean isAbstract (final int nModifiers)
    {
      return (nModifiers & ABSTRACT) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>final</code> is among them.
     */
    public static boolean isFinal (final int nModifiers)
    {
      return (nModifiers & FINAL) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>native</code> is among them.
     */
    public static boolean isNative (final int nModifiers)
    {
      return (nModifiers & NATIVE) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>strictfp</code> is among them.
     */
    public static boolean isStrictfp (final int nModifiers)
    {
      return (nModifiers & STRICTFP) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>synchronized</code> is among them.
     */
    public static boolean isSynchronized (final int nModifiers)
    {
      return (nModifiers & SYNCHRONIZED) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>transient</code> is among them.
     */
    public static boolean isTransient (final int nModifiers)
    {
      return (nModifiers & TRANSIENT) != 0;
    }

    /**
     * Test one modifier bit.
     *
     * @param nModifiers
     *        The modifier bits to test.
     * @return <code>true</code> if <code>volatile</code> is among them.
     */
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

  /** Start a fresh grammar: which compilation unit list is being filled, and the counters. */
  protected static void initialize ()
  {
    final Integer i = Integer.valueOf (0);
    grammar ().lexStateS2I ().put ("DEFAULT", i);
    grammar ().lexStateI2S ().put (i, "DEFAULT");
    grammar ().simpleTokensTable ().put ("DEFAULT", new HashMap <> ());
  }

  /**
   * Record the parser class name that PARSER_BEGIN named.
   *
   * @param sId
   *        The identifier that followed PARSER_BEGIN.
   */
  protected static void addcuname (final String sId)
  {
    grammar ().setParserName (sId);
  }

  /**
   * Complain unless PARSER_BEGIN and PARSER_END name the same class.
   *
   * @param t
   *        Where to report the error.
   * @param sId1
   *        The name from PARSER_BEGIN.
   * @param sId2
   *        The name from PARSER_END.
   */
  protected static void compare (final Token t, final String sId1, @NonNull final String sId2)
  {
    if (!sId2.equals (sId1))
    {
      JavaCCErrors.parse_error (t, "Name " + sId2 + " must be the same as that used at PARSER_BEGIN (" + sId1 + ")");
    }
  }

  /**
   * Mark where in the compilation unit the generated parser class body starts or ends.
   *
   * @param t
   *        The token the insertion point is at.
   * @param nNo
   *        1 for the first insertion point, 2 for the second.
   */
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

  /**
   * Complain about a parser class declaration the generator cannot find its way into.
   *
   * @param t
   *        Where to report the error.
   */
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

  /**
   * Remember the first token of the compilation unit, so that it can be copied out verbatim.
   *
   * @param t
   *        The first token.
   */
  protected static void set_initial_cu_token (final Token t)
  {
    PGCCContext.current ().parserBuild ().setFirstToken (t);
  }

  /**
   * Add a production to the grammar, in declaration order.
   *
   * @param p
   *        The production just parsed.
   */
  protected static void addProduction (final AbstractNormalProduction p)
  {
    grammar ().bnfProductions ().add (p);
  }

  /**
   * Give a BNF production its right hand side, and tell the expansion who its parent is.
   *
   * @param p
   *        The production. May not be <code>null</code>.
   * @param e
   *        Its expansion. May not be <code>null</code>.
   */
  protected static void productionAddExpansion (@NonNull final BNFProduction p, @NonNull final Expansion e)
  {
    e.setParent (p);
    p.setExpansion (e);
  }

  /**
   * Add a token production, and record the lexical states it belongs to.
   *
   * @param p
   *        The token production just parsed. May not be <code>null</code>.
   */
  protected static void addregexpr (@NonNull final TokenProduction p)
  {
    grammar ().rexprList ().add (p);
    if (Options.isUserTokenManager ())
    {
      if (p.getLexStates () == null || p.getLexStates ().length != 1 || !p.getLexStates ()[0].equals ("DEFAULT"))
      {
        JavaCCErrors.warning (p,
                              "Ignoring lexical state specifications since option " +
                                 "USER_TOKEN_MANAGER has been set to true.");
      }
    }
    if (p.getLexStates () == null)
    {
      return;
    }
    for (int i = 0; i < p.getLexStates ().length; i++)
    {
      for (int j = 0; j < i; j++)
      {
        if (p.getLexStates ()[i].equals (p.getLexStates ()[j]))
        {
          JavaCCErrors.parse_error (p, "Multiple occurrence of \"" + p.getLexStates ()[i] + "\" in lexical state list.");
        }
      }
      if (grammar ().lexStateS2I ().get (p.getLexStates ()[i]) == null)
      {
        final Integer aIi = Integer.valueOf (PGCCContext.current ().parserBuild ().getAndIncNextFreeLexState ());
        grammar ().lexStateS2I ().put (p.getLexStates ()[i], aIi);
        grammar ().lexStateI2S ().put (aIi, p.getLexStates ()[i]);
        grammar ().simpleTokensTable ().put (p.getLexStates ()[i], new HashMap <> ());
      }
    }
  }

  /**
   * Record the TOKEN_MGR_DECLS block, which is copied into the generated token manager.
   *
   * @param t
   *        Where to report a second block.
   * @param aDecls
   *        The declarations as written.
   */
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

  /**
   * Turn a regular expression written inline in a BNF production into an anonymous token
   * production, so that the token manager knows about it.
   *
   * @param r
   *        The inline regular expression.
   */
  protected static void add_inline_regexpr (final AbstractExpRegularExpression r)
  {
    if (!(r instanceof ExpREndOfFile))
    {
      final TokenProduction p = new TokenProduction ();
      p.setExplicit (false);
      p.setLexStates (new String [] { "DEFAULT" });
      p.setKind (ETokenKind.TOKEN);
      final RegExprSpec aRes = new RegExprSpec ();
      aRes.setRexp (r);
      aRes.getRexp ().m_aTpContext = p;
      aRes.setAct (new ExpAction ());
      aRes.setNextState (null);
      aRes.setNsTok (null);
      p.getRespecs ().add (aRes);
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

  /**
   * Turn a string literal as written in the grammar into the characters it stands for.
   *
   * @param t
   *        Where to report a bad escape.
   * @param sStr
   *        The literal including its quotes. May not be <code>null</code>.
   * @return The characters the literal denotes. Never <code>null</code>.
   */
  protected static String remove_escapes_and_quotes (final Token t, @NonNull final String sStr)
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

  /**
   * Read a single character of a character list.
   *
   * @param t
   *        Where to report an error.
   * @param s
   *        The character as written, escapes included. May not be <code>null</code>.
   * @return The character it denotes.
   */
  protected static char character_descriptor_assign (final Token t, @NonNull final String s)
  {
    if (s.length () != 1)
    {
      JavaCCErrors.parse_error (t, "String in character list may contain only one character.");
      return ' ';
    }
    return s.charAt (0);
  }

  /**
   * Read the right hand character of a character range, and check it is not below the left one.
   *
   * @param t
   *        Where to report an error.
   * @param s
   *        The character as written. May not be <code>null</code>.
   * @param sLeft
   *        The left hand character of the range. May not be <code>null</code>.
   * @return The character it denotes.
   */
  protected static char character_descriptor_assign (final Token t, @NonNull final String s, @NonNull final String sLeft)
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

  /**
   * Build the expansion for a TRY block written in a production.
   *
   * @param aTryLoc
   *        Where the block starts, for error reporting. May not be <code>null</code>.
   * @param aResult
   *        Receives the resulting expansion. May not be <code>null</code>.
   * @param aNestedExp
   *        Holds the expansion the block guards. May not be <code>null</code>.
   * @param types
   *        The exception type of each catch clause.
   * @param ids
   *        The variable name of each catch clause.
   * @param catchblks
   *        The body of each catch clause. May not be <code>null</code>.
   * @param aFinallyblk
   *        The body of the finally clause, or <code>null</code> if there is none.
   */
  protected static void makeTryBlock (@NonNull final Token aTryLoc,
                                      @NonNull final Container aResult,
                                      @NonNull final Container aNestedExp,
                                      final List <List <Token>> types,
                                      final List <Token> ids,
                                      @NonNull final List <List <Token>> catchblks,
                                      @Nullable final List <Token> aFinallyblk)
  {
    if (catchblks.size () == 0 && aFinallyblk == null)
    {
      JavaCCErrors.parse_error (aTryLoc, "Try block must contain at least one catch or finally block.");
      return;
    }
    final ExpTryBlock aTblk = new ExpTryBlock ();
    aTblk.setLineNumber (aTryLoc.beginLine);
    aTblk.setColumnNumber (aTryLoc.beginColumn);
    aTblk.setExp ((Expansion) aNestedExp.getMember ());
    aTblk.getExp ().setParent (aTblk);
    aTblk.getExp ().setOrdinalBase (0);
    aTblk.setTypes (types);
    aTblk.setIds (ids);
    aTblk.setCatchblks (catchblks);
    aTblk.setFinallyblk (aFinallyblk);
    aResult.setMember (aTblk);
  }

}
