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

import static com.helger.pgcc.parser.JavaCCGlobals.BNF_PRODUCTIONS;
import static com.helger.pgcc.parser.JavaCCGlobals.CU_FROM_INSERTION_POINT_2;
import static com.helger.pgcc.parser.JavaCCGlobals.CU_TO_INSERTION_POINT_1;
import static com.helger.pgcc.parser.JavaCCGlobals.CU_TO_INSERTION_POINT_2;
import static com.helger.pgcc.parser.JavaCCGlobals.LEXSTATE_I2S;
import static com.helger.pgcc.parser.JavaCCGlobals.LEXSTATE_S2I;
import static com.helger.pgcc.parser.JavaCCGlobals.REXPR_LIST;
import static com.helger.pgcc.parser.JavaCCGlobals.SIMPLE_TOKENS_TABLE;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_token_mgr_decls;

import java.util.HashMap;
import java.util.List;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;
import com.helger.pgcc.parser.exp.ExpREndOfFile;
import com.helger.pgcc.parser.exp.ExpTryBlock;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * Utilities.
 */
public abstract class JavaCCParserInternals
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
     * A set of accessors that indicate whether the specified modifier is in the
     * set.
     */

    public static boolean isPublic (final int modifiers)
    {
      return (modifiers & PUBLIC) != 0;
    }

    public static boolean isProtected (final int modifiers)
    {
      return (modifiers & PROTECTED) != 0;
    }

    public static boolean isPrivate (final int modifiers)
    {
      return (modifiers & PRIVATE) != 0;
    }

    public static boolean isStatic (final int modifiers)
    {
      return (modifiers & STATIC) != 0;
    }

    public static boolean isAbstract (final int modifiers)
    {
      return (modifiers & ABSTRACT) != 0;
    }

    public static boolean isFinal (final int modifiers)
    {
      return (modifiers & FINAL) != 0;
    }

    public static boolean isNative (final int modifiers)
    {
      return (modifiers & NATIVE) != 0;
    }

    public static boolean isStrictfp (final int modifiers)
    {
      return (modifiers & STRICTFP) != 0;
    }

    public static boolean isSynchronized (final int modifiers)
    {
      return (modifiers & SYNCHRONIZED) != 0;
    }

    public static boolean isTransient (final int modifiers)
    {
      return (modifiers & TRANSIENT) != 0;
    }

    public static boolean isVolatile (final int modifiers)
    {
      return (modifiers & VOLATILE) != 0;
    }

    /**
     * Removes the given modifier.
     */
    static int removeModifier (final int modifiers, final int modToRemove)
    {
      return modifiers & ~modToRemove;
    }
  }

  protected static void initialize ()
  {
    final Integer i = Integer.valueOf (0);
    LEXSTATE_S2I.put ("DEFAULT", i);
    LEXSTATE_I2S.put (i, "DEFAULT");
    SIMPLE_TOKENS_TABLE.put ("DEFAULT", new HashMap <> ());
  }

  protected static void addcuname (final String id)
  {
    s_cu_name = id;
  }

  protected static void compare (final Token t, final String id1, final String id2)
  {
    if (!id2.equals (id1))
    {
      JavaCCErrors.parse_error (t, "Name " + id2 + " must be the same as that used at PARSER_BEGIN (" + id1 + ")");
    }
  }

  private static List <Token> s_add_cu_token_here = CU_TO_INSERTION_POINT_1;
  private static Token s_first_cu_token;
  private static boolean s_insertionpoint1set = false;
  private static boolean s_insertionpoint2set = false;

  protected static void setinsertionpoint (final Token t, final int no)
  {
    do
    {
      s_add_cu_token_here.add (s_first_cu_token);
      s_first_cu_token = s_first_cu_token.next;
    } while (s_first_cu_token != t);
    if (no == 1)
    {
      if (s_insertionpoint1set)
      {
        JavaCCErrors.parse_error (t, "Multiple declaration of parser class.");
      }
      else
      {
        s_insertionpoint1set = true;
        s_add_cu_token_here = CU_TO_INSERTION_POINT_2;
      }
    }
    else
    {
      s_add_cu_token_here = CU_FROM_INSERTION_POINT_2;
      s_insertionpoint2set = true;
    }
    s_first_cu_token = t;
  }

  protected static void insertionpointerrors (final Token t)
  {
    while (s_first_cu_token != t)
    {
      s_add_cu_token_here.add (s_first_cu_token);
      s_first_cu_token = s_first_cu_token.next;
    }
    if (!s_insertionpoint1set || !s_insertionpoint2set)
    {
      JavaCCErrors.parse_error (t, "Parser class has not been defined between PARSER_BEGIN and PARSER_END.");
    }
  }

  protected static void set_initial_cu_token (final Token t)
  {
    s_first_cu_token = t;
  }

  protected static void addProduction (final NormalProduction p)
  {
    BNF_PRODUCTIONS.add (p);
  }

  protected static void productionAddExpansion (final BNFProduction p, final Expansion e)
  {
    e.setParent (p);
    p.setExpansion (e);
  }

  private static int s_nextFreeLexState = 1;

  protected static void addregexpr (final TokenProduction p)
  {
    REXPR_LIST.add (p);
    if (Options.isUserTokenManager ())
    {
      if (p.m_lexStates == null || p.m_lexStates.length != 1 || !p.m_lexStates[0].equals ("DEFAULT"))
      {
        JavaCCErrors.warning (p, "Ignoring lexical state specifications since option " + "USER_TOKEN_MANAGER has been set to true.");
      }
    }
    if (p.m_lexStates == null)
    {
      return;
    }
    for (int i = 0; i < p.m_lexStates.length; i++)
    {
      for (int j = 0; j < i; j++)
      {
        if (p.m_lexStates[i].equals (p.m_lexStates[j]))
        {
          JavaCCErrors.parse_error (p, "Multiple occurrence of \"" + p.m_lexStates[i] + "\" in lexical state list.");
        }
      }
      if (LEXSTATE_S2I.get (p.m_lexStates[i]) == null)
      {
        final Integer ii = Integer.valueOf (s_nextFreeLexState);
        s_nextFreeLexState++;
        LEXSTATE_S2I.put (p.m_lexStates[i], ii);
        LEXSTATE_I2S.put (ii, p.m_lexStates[i]);
        SIMPLE_TOKENS_TABLE.put (p.m_lexStates[i], new HashMap <> ());
      }
    }
  }

  protected static void add_token_manager_decls (final Token t, final List <Token> decls)
  {
    if (s_token_mgr_decls != null)
    {
      JavaCCErrors.parse_error (t, "Multiple occurrence of \"TOKEN_MGR_DECLS\".");
    }
    else
    {
      s_token_mgr_decls = new CommonsArrayList <> (decls);
      if (Options.isUserTokenManager ())
      {
        JavaCCErrors.warning (t, "Ignoring declarations in \"TOKEN_MGR_DECLS\" since option " + "USER_TOKEN_MANAGER has been set to true.");
      }
    }
  }

  protected static void add_inline_regexpr (final AbstractExpRegularExpression r)
  {
    if (!(r instanceof ExpREndOfFile))
    {
      final TokenProduction p = new TokenProduction ();
      p.m_isExplicit = false;
      p.m_lexStates = new String [] { "DEFAULT" };
      p.m_kind = ETokenKind.TOKEN;
      final RegExprSpec res = new RegExprSpec ();
      res.rexp = r;
      res.rexp.m_tpContext = p;
      res.act = new ExpAction ();
      res.nextState = null;
      res.nsTok = null;
      p.m_respecs.add (res);
      REXPR_LIST.add (p);
    }
  }

  private static boolean _isHexchar (final char ch)
  {
    if (ch >= '0' && ch <= '9')
      return true;
    if (ch >= 'A' && ch <= 'F')
      return true;
    if (ch >= 'a' && ch <= 'f')
      return true;
    return false;
  }

  private static int _getHexVal (final char ch)
  {
    if (ch >= '0' && ch <= '9')
      return (ch) - ('0');
    if (ch >= 'A' && ch <= 'F')
      return (ch) - ('A') + 10;
    return (ch) - ('a') + 10;
  }

  protected static String remove_escapes_and_quotes (final Token t, final String str)
  {
    String retval = "";
    int index = 1;
    char ch, ch1;
    int ordinal;
    while (index < str.length () - 1)
    {
      if (str.charAt (index) != '\\')
      {
        retval += str.charAt (index);
        index++;
        continue;
      }
      index++;
      ch = str.charAt (index);
      if (ch == 'b')
      {
        retval += '\b';
        index++;
        continue;
      }
      if (ch == 't')
      {
        retval += '\t';
        index++;
        continue;
      }
      if (ch == 'n')
      {
        retval += '\n';
        index++;
        continue;
      }
      if (ch == 'f')
      {
        retval += '\f';
        index++;
        continue;
      }
      if (ch == 'r')
      {
        retval += '\r';
        index++;
        continue;
      }
      if (ch == '"')
      {
        retval += '\"';
        index++;
        continue;
      }
      if (ch == '\'')
      {
        retval += '\'';
        index++;
        continue;
      }
      if (ch == '\\')
      {
        retval += '\\';
        index++;
        continue;
      }
      if (ch >= '0' && ch <= '7')
      {
        ordinal = (ch) - ('0');
        index++;
        ch1 = str.charAt (index);
        if (ch1 >= '0' && ch1 <= '7')
        {
          ordinal = ordinal * 8 + (ch1) - ('0');
          index++;
          ch1 = str.charAt (index);
          if (ch <= '3' && ch1 >= '0' && ch1 <= '7')
          {
            ordinal = ordinal * 8 + (ch1) - ('0');
            index++;
          }
        }
        retval += (char) ordinal;
        continue;
      }
      if (ch == 'u')
      {
        index++;
        ch = str.charAt (index);
        if (_isHexchar (ch))
        {
          ordinal = _getHexVal (ch);
          index++;
          ch = str.charAt (index);
          if (_isHexchar (ch))
          {
            ordinal = ordinal * 16 + _getHexVal (ch);
            index++;
            ch = str.charAt (index);
            if (_isHexchar (ch))
            {
              ordinal = ordinal * 16 + _getHexVal (ch);
              index++;
              ch = str.charAt (index);
              if (_isHexchar (ch))
              {
                ordinal = ordinal * 16 + _getHexVal (ch);
                index++;
                continue;
              }
            }
          }
        }
        JavaCCErrors.parse_error (t,
                                  "Encountered non-hex character '" +
                                     ch +
                                     "' at position " +
                                     index +
                                     " of string " +
                                     "- Unicode escape must have 4 hex digits after it.");
        return retval;
      }
      JavaCCErrors.parse_error (t, "Illegal escape sequence '\\" + ch + "' at position " + index + " of string.");
      return retval;
    }
    return retval;
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

  protected static char character_descriptor_assign (final Token t, final String s, final String left)
  {
    if (s.length () != 1)
    {
      JavaCCErrors.parse_error (t, "String in character list may contain only one character.");
      return ' ';
    }
    if (left.charAt (0) > s.charAt (0))
    {
      JavaCCErrors.parse_error (t,
                                "Right end of character range \'" +
                                   s +
                                   "\' has a lower ordinal value than the left end of character range \'" +
                                   left +
                                   "\'.");
      return left.charAt (0);
    }
    return s.charAt (0);
  }

  protected static void makeTryBlock (final Token tryLoc,
                                      final Container result,
                                      final Container nestedExp,
                                      final List <List <Token>> types,
                                      final List <Token> ids,
                                      final List <List <Token>> catchblks,
                                      final List <Token> finallyblk)
  {
    if (catchblks.size () == 0 && finallyblk == null)
    {
      JavaCCErrors.parse_error (tryLoc, "Try block must contain at least one catch or finally block.");
      return;
    }
    final ExpTryBlock tblk = new ExpTryBlock ();
    tblk.setLine (tryLoc.beginLine);
    tblk.setColumn (tryLoc.beginColumn);
    tblk.m_exp = (Expansion) nestedExp.member;
    tblk.m_exp.setParent (tblk);
    tblk.m_exp.setOrdinalBase (0);
    tblk.m_types = types;
    tblk.m_ids = ids;
    tblk.m_catchblks = catchblks;
    tblk.m_finallyblk = finallyblk;
    result.member = tblk;
  }

  public static void reInit ()
  {
    s_add_cu_token_here = CU_TO_INSERTION_POINT_1;
    s_first_cu_token = null;
    s_insertionpoint1set = false;
    s_insertionpoint2set = false;
    s_nextFreeLexState = 1;
  }
}
