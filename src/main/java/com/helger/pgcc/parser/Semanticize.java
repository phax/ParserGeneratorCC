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
import static com.helger.pgcc.parser.JavaCCGlobals.LEXSTATE_I2S;
import static com.helger.pgcc.parser.JavaCCGlobals.LEXSTATE_S2I;
import static com.helger.pgcc.parser.JavaCCGlobals.NAMED_TOKENS_TABLE;
import static com.helger.pgcc.parser.JavaCCGlobals.NAMES_OF_TOKENS;
import static com.helger.pgcc.parser.JavaCCGlobals.ORDERED_NAME_TOKENS;
import static com.helger.pgcc.parser.JavaCCGlobals.PRODUCTION_TABLE;
import static com.helger.pgcc.parser.JavaCCGlobals.REXPR_LIST;
import static com.helger.pgcc.parser.JavaCCGlobals.REXPS_OF_TOKENS;
import static com.helger.pgcc.parser.JavaCCGlobals.SIMPLE_TOKENS_TABLE;
import static com.helger.pgcc.parser.JavaCCGlobals.s_aActForEof;
import static com.helger.pgcc.parser.JavaCCGlobals.s_sNextStateForEof;
import static com.helger.pgcc.parser.JavaCCGlobals.s_tokenCount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.helger.pgcc.parser.exp.*;

public class Semanticize
{
  private static List <List <RegExprSpec>> s_aRemoveList = new ArrayList <> ();
  private static List <Object> s_aItemList = new ArrayList <> ();

  private static void prepareToRemove (final List <RegExprSpec> vec, final Object item)
  {
    s_aRemoveList.add (vec);
    s_aItemList.add (item);
  }

  private static void removePreparedItems ()
  {
    for (int i = 0; i < s_aRemoveList.size (); i++)
    {
      final List <RegExprSpec> list = s_aRemoveList.get (i);
      list.remove (s_aItemList.get (i));
    }
    s_aRemoveList.clear ();
    s_aItemList.clear ();
  }

  public static void start () throws MetaParseException
  {
    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    if (Options.getLookahead () > 1 && !Options.isForceLaCheck () && Options.isSanityCheck ())
    {
      JavaCCErrors.warning ("Lookahead adequacy checking not being performed since option LOOKAHEAD " +
                            "is more than 1.  Set option FORCE_LA_CHECK to true to force checking.");
    }

    /*
     * The following walks the entire parse tree to convert all LOOKAHEAD's that
     * are not at choice points (but at beginning of sequences) and converts
     * them to trivial choices. This way, their semantic lookahead specification
     * can be evaluated during other lookahead evaluations.
     */
    for (final NormalProduction aNormalProduction : BNF_PRODUCTIONS)
    {
      ExpansionTreeWalker.postOrderWalk (aNormalProduction.getExpansion (), new LookaheadFixer ());
    }

    /*
     * The following loop populates "production_table"
     */
    for (final NormalProduction p : BNF_PRODUCTIONS)
    {
      if (JavaCCGlobals.PRODUCTION_TABLE.put (p.getLhs (), p) != null)
      {
        JavaCCErrors.semantic_error (p, p.getLhs () + " occurs on the left hand side of more than one production.");
      }
    }

    /*
     * The following walks the entire parse tree to make sure that all
     * non-terminals on RHS's are defined on the LHS.
     */
    for (final NormalProduction aNormalProduction : BNF_PRODUCTIONS)
    {
      ExpansionTreeWalker.preOrderWalk ((aNormalProduction).getExpansion (), new ProductionDefinedChecker ());
    }

    /*
     * The following loop ensures that all target lexical states are defined.
     * Also piggybacking on this loop is the detection of <EOF> and <name> in
     * token productions. After reporting an error, these entries are removed.
     * Also checked are definitions on inline private regular expressions. This
     * loop works slightly differently when USER_TOKEN_MANAGER is set to true.
     * In this case, <name> occurrences are OK, while regular expression specs
     * generate a warning.
     */
    for (final TokenProduction aTokenProduction : REXPR_LIST)
    {
      final TokenProduction tp = (aTokenProduction);
      final List <RegExprSpec> respecs = tp.m_respecs;
      for (final RegExprSpec aRegExprSpec : respecs)
      {
        final RegExprSpec res = (aRegExprSpec);
        if (res.nextState != null)
        {
          if (LEXSTATE_S2I.get (res.nextState) == null)
          {
            JavaCCErrors.semantic_error (res.nsTok, "Lexical state \"" + res.nextState + "\" has not been defined.");
          }
        }
        if (res.rexp instanceof ExpREndOfFile)
        {
          // JavaCCErrors.semantic_error(res.rexp, "Badly placed <EOF>.");
          if (tp.m_lexStates != null)
            JavaCCErrors.semantic_error (res.rexp, "EOF action/state change must be specified for all states, " + "i.e., <*>TOKEN:.");
          if (tp.m_kind != ETokenKind.TOKEN)
            JavaCCErrors.semantic_error (res.rexp, "EOF action/state change can be specified only in a " + "TOKEN specification.");
          if (s_sNextStateForEof != null || s_aActForEof != null)
            JavaCCErrors.semantic_error (res.rexp, "Duplicate action/state change specification for <EOF>.");
          s_aActForEof = res.act;
          s_sNextStateForEof = res.nextState;
          prepareToRemove (respecs, res);
        }
        else
          if (tp.m_isExplicit && Options.isUserTokenManager ())
          {
            JavaCCErrors.warning (res.rexp,
                                  "Ignoring regular expression specification since " + "option USER_TOKEN_MANAGER has been set to true.");
          }
          else
            if (tp.m_isExplicit && !Options.isUserTokenManager () && res.rexp instanceof ExpRJustName)
            {
              JavaCCErrors.warning (res.rexp,
                                    "Ignoring free-standing regular expression reference.  " +
                                              "If you really want this, you must give it a different label as <NEWLABEL:<" +
                                              res.rexp.getLabel () +
                                              ">>.");
              prepareToRemove (respecs, res);
            }
            else
              if (!tp.m_isExplicit && res.rexp.m_private_rexp)
              {
                JavaCCErrors.semantic_error (res.rexp, "Private (#) regular expression cannot be defined within " + "grammar productions.");
              }
      }
    }

    removePreparedItems ();

    /*
     * The following loop inserts all names of regular expressions into
     * "named_tokens_table" and "ordered_named_tokens". Duplications are flagged
     * as errors.
     */
    for (final TokenProduction aTokenProduction : REXPR_LIST)
    {
      final TokenProduction tp = (aTokenProduction);
      final List <RegExprSpec> respecs = tp.m_respecs;
      for (final RegExprSpec aRegExprSpec : respecs)
      {
        final RegExprSpec res = (aRegExprSpec);
        if (!(res.rexp instanceof ExpRJustName) && res.rexp.hasLabel ())
        {
          final String s = res.rexp.getLabel ();
          final AbstractExpRegularExpression obj = NAMED_TOKENS_TABLE.put (s, res.rexp);
          if (obj != null)
          {
            JavaCCErrors.semantic_error (res.rexp, "Multiply defined lexical token name \"" + s + "\".");
          }
          else
          {
            ORDERED_NAME_TOKENS.add (res.rexp);
          }
          if (LEXSTATE_S2I.get (s) != null)
          {
            JavaCCErrors.semantic_error (res.rexp, "Lexical token name \"" + s + "\" is the same as " + "that of a lexical state.");
          }
        }
      }
    }

    /*
     * The following code merges multiple uses of the same string in the same
     * lexical state and produces error messages when there are multiple
     * explicit occurrences (outside the BNF) of the string in the same lexical
     * state, or when within BNF occurrences of a string are duplicates of those
     * that occur as non-TOKEN's (SKIP, MORE, SPECIAL_TOKEN) or private regular
     * expressions. While doing this, this code also numbers all regular
     * expressions (by setting their ordinal values), and populates the table
     * "names_of_tokens".
     */

    s_tokenCount = 1;
    for (final TokenProduction tp : REXPR_LIST)
    {
      final List <RegExprSpec> respecs = tp.m_respecs;
      if (tp.m_lexStates == null)
      {
        tp.m_lexStates = new String [LEXSTATE_I2S.size ()];
        LEXSTATE_I2S.values ().toArray (tp.m_lexStates);
      }

      @SuppressWarnings ("unchecked")
      final Map <String, Map <String, AbstractExpRegularExpression>> table[] = new Map [tp.m_lexStates.length];
      for (int i = 0; i < tp.m_lexStates.length; i++)
      {
        table[i] = SIMPLE_TOKENS_TABLE.get (tp.m_lexStates[i]);
      }

      for (final RegExprSpec aRegExprSpec : respecs)
      {
        final RegExprSpec res = (aRegExprSpec);
        if (res.rexp instanceof ExpRStringLiteral)
        {
          final ExpRStringLiteral sl = (ExpRStringLiteral) res.rexp;
          // This loop performs the checks and actions with respect to each
          // lexical state.
          for (int i = 0; i < table.length; i++)
          {
            // Get table of all case variants of "sl.image" into table2.
            Map <String, AbstractExpRegularExpression> table2 = table[i].get (sl.m_image.toUpperCase (Locale.US));
            if (table2 == null)
            {
              // There are no case variants of "sl.image" earlier than the
              // current one.
              // So go ahead and insert this item.
              if (sl.getOrdinal () == 0)
              {
                sl.setOrdinal (s_tokenCount++);
              }
              table2 = new HashMap <> ();
              table2.put (sl.m_image, sl);
              table[i].put (sl.m_image.toUpperCase (Locale.US), table2);
            }
            else
              if (hasIgnoreCase (table2, sl.m_image))
              { // hasIgnoreCase sets "other" if it is found.
                // Since IGNORE_CASE version exists, current one is useless and
                // bad.
                if (!sl.m_tpContext.m_isExplicit)
                {
                  // inline BNF string is used earlier with an IGNORE_CASE.
                  JavaCCErrors.semantic_error (sl,
                                               "String \"" +
                                                   sl.m_image +
                                                   "\" can never be matched " +
                                                   "due to presence of more general (IGNORE_CASE) regular expression " +
                                                   "at line " +
                                                   other.getLine () +
                                                   ", column " +
                                                   other.getColumn () +
                                                   ".");
                }
                else
                {
                  // give the standard error message.
                  JavaCCErrors.semantic_error (sl,
                                               "Duplicate definition of string token \"" + sl.m_image + "\" " + "can never be matched.");
                }
              }
              else
                if (sl.m_tpContext.m_ignoreCase)
                {
                  // This has to be explicit. A warning needs to be given with
                  // respect
                  // to all previous strings.
                  final StringBuilder pos = new StringBuilder ();
                  int count = 0;
                  for (final AbstractExpRegularExpression rexp : table2.values ())
                  {
                    if (count != 0)
                      pos.append (",");
                    pos.append (" line ").append (rexp.getLine ());
                    count++;
                  }
                  if (count == 1)
                  {
                    JavaCCErrors.warning (sl, "String with IGNORE_CASE is partially superceded by string at" + pos + ".");
                  }
                  else
                  {
                    JavaCCErrors.warning (sl, "String with IGNORE_CASE is partially superceded by strings at" + pos + ".");
                  }
                  // This entry is legitimate. So insert it.
                  if (sl.getOrdinal () == 0)
                  {
                    sl.setOrdinal (s_tokenCount++);
                  }
                  table2.put (sl.m_image, sl);
                  // The above "put" may override an existing entry (that is not
                  // IGNORE_CASE) and that's
                  // the desired behavior.
                }
                else
                {
                  // The rest of the cases do not involve IGNORE_CASE.
                  final AbstractExpRegularExpression re = table2.get (sl.m_image);
                  if (re == null)
                  {
                    if (sl.getOrdinal () == 0)
                    {
                      sl.setOrdinal (s_tokenCount++);
                    }
                    table2.put (sl.m_image, sl);
                  }
                  else
                    if (tp.m_isExplicit)
                    {
                      // This is an error even if the first occurrence was
                      // implicit.
                      if (tp.m_lexStates[i].equals ("DEFAULT"))
                      {
                        JavaCCErrors.semantic_error (sl, "Duplicate definition of string token \"" + sl.m_image + "\".");
                      }
                      else
                      {
                        JavaCCErrors.semantic_error (sl,
                                                     "Duplicate definition of string token \"" +
                                                         sl.m_image +
                                                         "\" in lexical state \"" +
                                                         tp.m_lexStates[i] +
                                                         "\".");
                      }
                    }
                    else
                      if (re.m_tpContext.m_kind != ETokenKind.TOKEN)
                      {
                        JavaCCErrors.semantic_error (sl,
                                                     "String token \"" +
                                                         sl.m_image +
                                                         "\" has been defined as a \"" +
                                                         re.m_tpContext.m_kind.getImage () +
                                                         "\" token.");
                      }
                      else
                        if (re.m_private_rexp)
                        {
                          JavaCCErrors.semantic_error (sl,
                                                       "String token \"" +
                                                           sl.m_image +
                                                           "\" has been defined as a private regular expression.");
                        }
                        else
                        {
                          // This is now a legitimate reference to an existing
                          // RStringLiteral.
                          // So we assign it a number and take it out of
                          // "rexprlist".
                          // Therefore, if all is OK (no errors), then there
                          // will be only unequal
                          // string literals in each lexical state. Note that
                          // the only way
                          // this can be legal is if this is a string declared
                          // inline within the
                          // BNF. Hence, it belongs to only one lexical state -
                          // namely "DEFAULT".
                          sl.setOrdinal (re.getOrdinal ());
                          prepareToRemove (respecs, res);
                        }
                }
          }
        }
        else
          if (!(res.rexp instanceof ExpRJustName))
          {
            res.rexp.setOrdinal (s_tokenCount++);
          }
        if (!(res.rexp instanceof ExpRJustName) && res.rexp.hasLabel ())
        {
          NAMES_OF_TOKENS.put (Integer.valueOf (res.rexp.getOrdinal ()), res.rexp.getLabel ());
        }
        if (!(res.rexp instanceof ExpRJustName))
        {
          REXPS_OF_TOKENS.put (Integer.valueOf (res.rexp.getOrdinal ()), res.rexp);
        }
      }
    }

    removePreparedItems ();

    /*
     * The following code performs a tree walk on all regular expressions
     * attaching links to "RJustName"s. Error messages are given if undeclared
     * names are used, or if "RJustNames" refer to private regular expressions
     * or to regular expressions of any kind other than TOKEN. In addition, this
     * loop also removes top level "RJustName"s from "rexprlist". This code is
     * not executed if Options.getUserTokenManager() is set to true. Instead the
     * following block of code is executed.
     */

    if (!Options.isUserTokenManager ())
    {
      final FixRJustNames frjn = new FixRJustNames ();
      for (final TokenProduction aTokenProduction : REXPR_LIST)
      {
        final TokenProduction tp = (aTokenProduction);
        final List <RegExprSpec> respecs = tp.m_respecs;
        for (final RegExprSpec aRegExprSpec : respecs)
        {
          final RegExprSpec res = (aRegExprSpec);
          frjn.m_root = res.rexp;
          ExpansionTreeWalker.preOrderWalk (res.rexp, frjn);
          if (res.rexp instanceof ExpRJustName)
          {
            prepareToRemove (respecs, res);
          }
        }
      }
    }

    removePreparedItems ();

    /*
     * The following code is executed only if Options.getUserTokenManager() is
     * set to true. This code visits all top-level "RJustName"s (ignores
     * "RJustName"s nested within regular expressions). Since regular
     * expressions are optional in this case, "RJustName"s without corresponding
     * regular expressions are given ordinal values here. If "RJustName"s refer
     * to a named regular expression, their ordinal values are set to reflect
     * this. All but one "RJustName" node is removed from the lists by the end
     * of execution of this code.
     */

    if (Options.isUserTokenManager ())
    {
      for (final TokenProduction aTokenProduction : REXPR_LIST)
      {
        final TokenProduction tp = (aTokenProduction);
        final List <RegExprSpec> respecs = tp.m_respecs;
        for (final RegExprSpec aRegExprSpec : respecs)
        {
          final RegExprSpec res = (aRegExprSpec);
          if (res.rexp instanceof ExpRJustName)
          {
            final ExpRJustName jn = (ExpRJustName) res.rexp;
            final AbstractExpRegularExpression rexp = NAMED_TOKENS_TABLE.get (jn.getLabel ());
            if (rexp == null)
            {
              jn.setOrdinal (s_tokenCount++);
              NAMED_TOKENS_TABLE.put (jn.getLabel (), jn);
              ORDERED_NAME_TOKENS.add (jn);
              NAMES_OF_TOKENS.put (Integer.valueOf (jn.getOrdinal ()), jn.getLabel ());
            }
            else
            {
              jn.setOrdinal (rexp.getOrdinal ());
              prepareToRemove (respecs, res);
            }
          }
        }
      }
    }

    removePreparedItems ();

    /*
     * The following code is executed only if Options.getUserTokenManager() is
     * set to true. This loop labels any unlabeled regular expression and prints
     * a warning that it is doing so. These labels are added to
     * "ordered_named_tokens" so that they may be generated into the
     * ...Constants file.
     */
    if (Options.isUserTokenManager ())
    {
      for (final TokenProduction aTokenProduction : REXPR_LIST)
      {
        final TokenProduction tp = (aTokenProduction);
        final List <RegExprSpec> respecs = tp.m_respecs;
        for (final RegExprSpec aRegExprSpec : respecs)
        {
          final RegExprSpec res = (aRegExprSpec);
          final Integer ii = Integer.valueOf (res.rexp.getOrdinal ());
          if (NAMES_OF_TOKENS.get (ii) == null)
          {
            JavaCCErrors.warning (res.rexp, "Unlabeled regular expression cannot be referred to by " + "user generated token manager.");
          }
        }
      }
    }

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    // The following code sets the value of the "emptyPossible" field of
    // NormalProduction
    // nodes. This field is initialized to false, and then the entire list of
    // productions is processed. This is repeated as long as at least one item
    // got updated from false to true in the pass.
    boolean emptyUpdate = true;
    while (emptyUpdate)
    {
      emptyUpdate = false;
      for (final NormalProduction aNormalProduction : BNF_PRODUCTIONS)
      {
        final NormalProduction prod = aNormalProduction;
        if (emptyExpansionExists (prod.getExpansion ()))
        {
          if (!prod.isEmptyPossible ())
          {
            emptyUpdate = prod.setEmptyPossible (true);
          }
        }
      }
    }

    if (Options.isSanityCheck () && JavaCCErrors.getErrorCount () == 0)
    {

      // The following code checks that all ZeroOrMore, ZeroOrOne, and OneOrMore
      // nodes
      // do not contain expansions that can expand to the empty token list.
      for (final NormalProduction aNormalProduction : BNF_PRODUCTIONS)
      {
        ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new EmptyChecker ());
      }

      // The following code goes through the productions and adds pointers to
      // other
      // productions that it can expand to without consuming any tokens. Once
      // this is
      // done, a left-recursion check can be performed.
      for (final NormalProduction prod : BNF_PRODUCTIONS)
      {
        _addLeftMost (prod, prod.getExpansion ());
      }

      // Now the following loop calls a recursive walk routine that searches for
      // actual left recursions. The way the algorithm is coded, once a node has
      // been determined to participate in a left recursive loop, it is not
      // tried
      // in any other loop.
      for (final NormalProduction prod : BNF_PRODUCTIONS)
      {
        if (prod.getWalkStatus () == 0)
        {
          _prodWalk (prod);
        }
      }

      // Now we do a similar, but much simpler walk for the regular expression
      // part of
      // the grammar. Here we are looking for any kind of loop, not just left
      // recursions,
      // so we only need to do the equivalent of the above walk.
      // This is not done if option USER_TOKEN_MANAGER is set to true.
      if (!Options.isUserTokenManager ())
      {
        for (final TokenProduction aTokenProduction : REXPR_LIST)
        {
          final TokenProduction tp = (aTokenProduction);
          final List <RegExprSpec> respecs = tp.m_respecs;
          for (final RegExprSpec aRegExprSpec : respecs)
          {
            final RegExprSpec res = (aRegExprSpec);
            final AbstractExpRegularExpression rexp = res.rexp;
            if (rexp.getWalkStatus () == 0)
            {
              rexp.setWalkStatus (-1);
              if (_rexpWalk (rexp))
              {
                s_loopString = "..." + rexp.getLabel () + "... --> " + s_loopString;
                JavaCCErrors.semantic_error (rexp, "Loop in regular expression detected: \"" + s_loopString + "\"");
              }
              rexp.setWalkStatus (1);
            }
          }
        }
      }

      /*
       * The following code performs the lookahead ambiguity checking.
       */
      if (JavaCCErrors.getErrorCount () == 0)
      {
        for (final NormalProduction aNormalProduction : BNF_PRODUCTIONS)
        {
          ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new LookaheadChecker ());
        }
      }
    } // matches "if (Options.getSanityCheck()) {"

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");
  }

  public static AbstractExpRegularExpression other;

  // Checks to see if the "str" is superceded by another equal (except case)
  // string in table.
  public static boolean hasIgnoreCase (final Map <String, AbstractExpRegularExpression> table, final String str)
  {
    final AbstractExpRegularExpression rexp = table.get (str);
    if (rexp != null && !rexp.m_tpContext.m_ignoreCase)
    {
      return false;
    }

    for (final AbstractExpRegularExpression aRegEx : table.values ())
    {
      if (aRegEx.m_tpContext.m_ignoreCase)
      {
        other = aRegEx;
        return true;
      }
    }
    return false;
  }

  // returns true if "exp" can expand to the empty string, returns false
  // otherwise.
  public static boolean emptyExpansionExists (final Expansion exp)
  {
    if (exp instanceof ExpNonTerminal)
    {
      return ((ExpNonTerminal) exp).getProd ().isEmptyPossible ();
    }

    if (exp instanceof ExpAction)
    {
      return true;
    }

    if (exp instanceof AbstractExpRegularExpression)
    {
      return false;
    }

    if (exp instanceof ExpOneOrMore)
    {
      return emptyExpansionExists (((ExpOneOrMore) exp).getExpansion ());
    }

    if (exp instanceof ExpZeroOrMore || exp instanceof ExpZeroOrOne)
    {
      return true;
    }

    if (exp instanceof ExpLookahead)
    {
      return true;
    }

    if (exp instanceof ExpChoice)
    {
      for (final Expansion aElement : ((ExpChoice) exp).getChoices ())
        if (emptyExpansionExists (aElement))
          return true;
      return false;
    }

    if (exp instanceof ExpSequence)
    {
      for (final Expansion aElement : ((ExpSequence) exp).getUnits ())
        if (!emptyExpansionExists (aElement))
          return false;
      return true;
    }

    if (exp instanceof ExpTryBlock)
    {
      return emptyExpansionExists (((ExpTryBlock) exp).m_exp);
    }

    // This should be dead code.
    return false;
  }

  // Updates prod.leftExpansions based on a walk of exp.
  static private void _addLeftMost (final NormalProduction prod, final Expansion exp)
  {
    if (exp instanceof ExpNonTerminal)
    {
      for (int i = 0; i < prod.m_leIndex; i++)
      {
        if (prod.getLeftExpansions ()[i] == ((ExpNonTerminal) exp).getProd ())
        {
          return;
        }
      }
      if (prod.m_leIndex == prod.getLeftExpansions ().length)
      {
        final NormalProduction [] newle = new NormalProduction [prod.m_leIndex * 2];
        System.arraycopy (prod.getLeftExpansions (), 0, newle, 0, prod.m_leIndex);
        prod.setLeftExpansions (newle);
      }
      prod.getLeftExpansions ()[prod.m_leIndex++] = ((ExpNonTerminal) exp).getProd ();
    }
    else
      if (exp instanceof ExpOneOrMore)
      {
        _addLeftMost (prod, ((ExpOneOrMore) exp).getExpansion ());
      }
      else
        if (exp instanceof ExpZeroOrMore)
        {
          _addLeftMost (prod, ((ExpZeroOrMore) exp).getExpansion ());
        }
        else
          if (exp instanceof ExpZeroOrOne)
          {
            _addLeftMost (prod, ((ExpZeroOrOne) exp).getExpansion ());
          }
          else
            if (exp instanceof ExpChoice)
            {
              for (final Expansion aObject : ((ExpChoice) exp).getChoices ())
                _addLeftMost (prod, aObject);
            }
            else
              if (exp instanceof ExpSequence)
              {
                for (final Expansion aObject : ((ExpSequence) exp).getUnits ())
                {
                  _addLeftMost (prod, aObject);
                  if (!emptyExpansionExists (aObject))
                    break;
                }
              }
              else
                if (exp instanceof ExpTryBlock)
                {
                  _addLeftMost (prod, ((ExpTryBlock) exp).m_exp);
                }
  }

  // The string in which the following methods store information.
  private static String s_loopString;

  // Returns true to indicate an unraveling of a detected left recursion loop,
  // and returns false otherwise.
  private static boolean _prodWalk (final NormalProduction prod)
  {
    prod.setWalkStatus (-1);
    for (int i = 0; i < prod.m_leIndex; i++)
    {
      if (prod.getLeftExpansions ()[i].getWalkStatus () == -1)
      {
        prod.getLeftExpansions ()[i].setWalkStatus (-2);
        s_loopString = prod.getLhs () + "... --> " + prod.getLeftExpansions ()[i].getLhs () + "...";
        if (prod.getWalkStatus () == -2)
        {
          prod.setWalkStatus (1);
          JavaCCErrors.semantic_error (prod, "Left recursion detected: \"" + s_loopString + "\"");
          return false;
        }
        prod.setWalkStatus (1);
        return true;
      }
      else
        if (prod.getLeftExpansions ()[i].getWalkStatus () == 0)
        {
          if (_prodWalk (prod.getLeftExpansions ()[i]))
          {
            s_loopString = prod.getLhs () + "... --> " + s_loopString;
            if (prod.getWalkStatus () == -2)
            {
              prod.setWalkStatus (1);
              JavaCCErrors.semantic_error (prod, "Left recursion detected: \"" + s_loopString + "\"");
              return false;
            }
            prod.setWalkStatus (1);
            return true;
          }
        }
    }
    prod.setWalkStatus (1);
    return false;
  }

  // Returns true to indicate an unraveling of a detected loop,
  // and returns false otherwise.
  static private boolean _rexpWalk (final AbstractExpRegularExpression rexp)
  {
    if (rexp instanceof ExpRJustName)
    {
      final ExpRJustName jn = (ExpRJustName) rexp;
      if (jn.m_regexpr.getWalkStatus () == -1)
      {
        jn.m_regexpr.setWalkStatus (-2);
        s_loopString = "..." + jn.m_regexpr.getLabel () + "...";
        // Note: Only the regexpr's of RJustName nodes and the top leve
        // regexpr's can have labels. Hence it is only in these cases that
        // the labels are checked for to be added to the loopString.
        return true;
      }
      else
        if (jn.m_regexpr.getWalkStatus () == 0)
        {
          jn.m_regexpr.setWalkStatus (-1);
          if (_rexpWalk (jn.m_regexpr))
          {
            s_loopString = "..." + jn.m_regexpr.getLabel () + "... --> " + s_loopString;
            if (jn.m_regexpr.getWalkStatus () == -2)
            {
              jn.m_regexpr.setWalkStatus (1);
              JavaCCErrors.semantic_error (jn.m_regexpr, "Loop in regular expression detected: \"" + s_loopString + "\"");
              return false;
            }
            jn.m_regexpr.setWalkStatus (1);
            return true;
          }
          jn.m_regexpr.setWalkStatus (1);
          return false;
        }
    }

    if (rexp instanceof ExpRChoice)
    {
      for (final AbstractExpRegularExpression aElement : ((ExpRChoice) rexp).getChoices ())
        if (_rexpWalk (aElement))
          return true;
      return false;
    }

    if (rexp instanceof ExpRSequence)
    {
      for (final AbstractExpRegularExpression aElement : ((ExpRSequence) rexp).getUnits ())
        if (_rexpWalk (aElement))
          return true;
      return false;
    }

    if (rexp instanceof ExpROneOrMore)
    {
      return _rexpWalk (((ExpROneOrMore) rexp).getRegExpr ());
    }

    if (rexp instanceof ExpRZeroOrMore)
    {
      return _rexpWalk (((ExpRZeroOrMore) rexp).getRegExpr ());
    }

    if (rexp instanceof ExpRZeroOrOne)
    {
      return _rexpWalk (((ExpRZeroOrOne) rexp).getRegExpr ());
    }

    if (rexp instanceof ExpRRepetitionRange)
    {
      return _rexpWalk (((ExpRRepetitionRange) rexp).getRegExpr ());
    }

    return false;
  }

  /**
   * Objects of this class are created from class Semanticize to work on
   * references to regular expressions from RJustName's.
   */
  static final class FixRJustNames implements ITreeWalkerOperation
  {
    public AbstractExpRegularExpression m_root;

    public boolean goDeeper (final Expansion e)
    {
      return true;
    }

    public void action (final Expansion e)
    {
      if (e instanceof ExpRJustName)
      {
        final ExpRJustName jn = (ExpRJustName) e;
        final AbstractExpRegularExpression rexp = NAMED_TOKENS_TABLE.get (jn.getLabel ());
        if (rexp == null)
        {
          JavaCCErrors.semantic_error (e, "Undefined lexical token name \"" + jn.getLabel () + "\".");
        }
        else
          if (jn == m_root && !jn.m_tpContext.m_isExplicit && rexp.m_private_rexp)
          {
            JavaCCErrors.semantic_error (e,
                                         "Token name \"" + jn.getLabel () + "\" refers to a private " + "(with a #) regular expression.");
          }
          else
            if (jn == m_root && !jn.m_tpContext.m_isExplicit && rexp.m_tpContext.m_kind != ETokenKind.TOKEN)
            {
              JavaCCErrors.semantic_error (e,
                                           "Token name \"" +
                                              jn.getLabel () +
                                              "\" refers to a non-token " +
                                              "(SKIP, MORE, IGNORE_IN_BNF) regular expression.");
            }
            else
            {
              jn.setOrdinal (rexp.getOrdinal ());
              jn.m_regexpr = rexp;
            }
      }
    }

  }

  static class LookaheadFixer implements ITreeWalkerOperation
  {
    public boolean goDeeper (final Expansion e)
    {
      if (e instanceof AbstractExpRegularExpression)
        return false;
      return true;
    }

    public void action (final Expansion e)
    {
      if (e instanceof ExpSequence)
      {
        if (e.getParent () instanceof ExpChoice ||
            e.getParent () instanceof ExpZeroOrMore ||
            e.getParent () instanceof ExpOneOrMore ||
            e.getParent () instanceof ExpZeroOrOne)
        {
          return;
        }
        final ExpSequence seq = (ExpSequence) e;
        final ExpLookahead la = (ExpLookahead) (seq.getUnitAt (0));
        if (!la.isExplicit ())
          return;

        // Create a singleton choice with an empty action.
        final ExpChoice ch = new ExpChoice ();
        ch.setLine (la.getLine ());
        ch.setColumn (la.getColumn ());
        ch.setParent (seq);

        final ExpSequence seq1 = new ExpSequence ();
        seq1.setLine (la.getLine ());
        seq1.setColumn (la.getColumn ());
        seq1.setParent (ch);
        seq1.addUnit (la);
        la.setParent (seq1);

        final ExpAction act = new ExpAction ();
        act.setLine (la.getLine ());
        act.setColumn (la.getColumn ());
        act.setParent (seq1);

        seq1.addUnit (act);
        ch.addChoice (seq1);
        if (la.getAmount () != 0)
        {
          if (la.getActionTokens ().isNotEmpty ())
          {
            JavaCCErrors.warning (la,
                                  "Encountered LOOKAHEAD(...) at a non-choice location.  " +
                                      "Only semantic lookahead will be considered here.");
          }
          else
          {
            JavaCCErrors.warning (la, "Encountered LOOKAHEAD(...) at a non-choice location.  This will be ignored.");
          }
        }
        // Now we have moved the lookahead into the singleton choice. Now create
        // a new dummy lookahead node to replace this one at its original
        // location.
        final ExpLookahead la1 = new ExpLookahead ();
        la1.setExplicit (false);
        la1.setLine (la.getLine ());
        la1.setColumn (la.getColumn ());
        la1.setParent (seq);

        // Now set the la_expansion field of la and la1 with a dummy expansion
        // (we use EOF).
        la.setLaExpansion (new ExpREndOfFile ());
        la1.setLaExpansion (new ExpREndOfFile ());
        seq.setUnit (0, la1);
        seq.addUnit (1, ch);
      }
    }

  }

  static class ProductionDefinedChecker implements ITreeWalkerOperation
  {
    public boolean goDeeper (final Expansion e)
    {
      if (e instanceof AbstractExpRegularExpression)
        return false;
      return true;
    }

    public void action (final Expansion e)
    {
      if (e instanceof ExpNonTerminal)
      {
        final ExpNonTerminal nt = (ExpNonTerminal) e;
        final NormalProduction np = PRODUCTION_TABLE.get (nt.getName ());
        if (np == null)
        {
          JavaCCErrors.semantic_error (e, "Non-terminal " + nt.getName () + " has not been defined.");
        }
        else
        {
          nt.setProd (np);
          np.getParents ().add (nt);
        }
      }
    }

  }

  static final class EmptyChecker implements ITreeWalkerOperation
  {
    public boolean goDeeper (final Expansion e)
    {
      if (e instanceof AbstractExpRegularExpression)
        return false;
      return true;
    }

    public void action (final Expansion e)
    {
      if (e instanceof ExpOneOrMore)
      {
        if (Semanticize.emptyExpansionExists (((ExpOneOrMore) e).getExpansion ()))
        {
          JavaCCErrors.semantic_error (e, "Expansion within \"(...)+\" can be matched by empty string.");
        }
      }
      else
        if (e instanceof ExpZeroOrMore)
        {
          if (Semanticize.emptyExpansionExists (((ExpZeroOrMore) e).getExpansion ()))
          {
            JavaCCErrors.semantic_error (e, "Expansion within \"(...)*\" can be matched by empty string.");
          }
        }
        else
          if (e instanceof ExpZeroOrOne)
          {
            if (Semanticize.emptyExpansionExists (((ExpZeroOrOne) e).getExpansion ()))
            {
              JavaCCErrors.semantic_error (e, "Expansion within \"(...)?\" can be matched by empty string.");
            }
          }
    }

  }

  static class LookaheadChecker implements ITreeWalkerOperation
  {

    public boolean goDeeper (final Expansion e)
    {
      if (e instanceof AbstractExpRegularExpression)
        return false;
      if (e instanceof ExpLookahead)
        return false;
      return true;
    }

    public void action (final Expansion e)
    {
      if (e instanceof ExpChoice)
      {
        if (Options.getLookahead () == 1 || Options.isForceLaCheck ())
        {
          LookaheadCalc.choiceCalc ((ExpChoice) e);
        }
      }
      else
        if (e instanceof ExpOneOrMore)
        {
          final ExpOneOrMore exp = (ExpOneOrMore) e;
          if (Options.isForceLaCheck () || (implicitLA (exp.getExpansion ()) && Options.getLookahead () == 1))
          {
            LookaheadCalc.ebnfCalc (exp, exp.getExpansion ());
          }
        }
        else
          if (e instanceof ExpZeroOrMore)
          {
            final ExpZeroOrMore exp = (ExpZeroOrMore) e;
            if (Options.isForceLaCheck () || (implicitLA (exp.getExpansion ()) && Options.getLookahead () == 1))
            {
              LookaheadCalc.ebnfCalc (exp, exp.getExpansion ());
            }
          }
          else
            if (e instanceof ExpZeroOrOne)
            {
              final ExpZeroOrOne exp = (ExpZeroOrOne) e;
              if (Options.isForceLaCheck () || (implicitLA (exp.getExpansion ()) && Options.getLookahead () == 1))
              {
                LookaheadCalc.ebnfCalc (exp, exp.getExpansion ());
              }
            }
    }

    static boolean implicitLA (final Expansion exp)
    {
      if (!(exp instanceof ExpSequence))
        return true;

      final ExpSequence seq = (ExpSequence) exp;
      final Object obj = seq.getUnitAt (0);
      if (!(obj instanceof ExpLookahead))
        return true;

      final ExpLookahead la = (ExpLookahead) obj;
      return !la.isExplicit ();
    }
  }

  public static void reInit ()
  {
    s_aRemoveList.clear ();
    s_aItemList.clear ();
    other = null;
    s_loopString = null;
  }
}
