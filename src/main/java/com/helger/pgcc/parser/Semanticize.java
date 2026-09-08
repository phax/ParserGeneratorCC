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
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.exp.*;

public class Semanticize
{
  private static void prepareToRemove (final List <RegExprSpec> aVec, final Object aItem)
  {
    PGCCContext.current ().semanticize ().prepareToRemove (aVec, aItem);
  }

  private static void removePreparedItems ()
  {
    PGCCContext.current ().semanticize ().removePreparedItems ();
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
     * The following walks the entire parse tree to convert all LOOKAHEAD's that are not at choice
     * points (but at beginning of sequences) and converts them to trivial choices. This way, their
     * semantic lookahead specification can be evaluated during other lookahead evaluations.
     */
    for (final NormalProduction aNormalProduction : grammar ().bnfProductions ())
    {
      ExpansionTreeWalker.postOrderWalk (aNormalProduction.getExpansion (), new LookaheadFixer ());
    }

    /*
     * The following loop populates "production_table"
     */
    for (final NormalProduction p : grammar ().bnfProductions ())
    {
      if (grammar ().productionTable ().put (p.getLhs (), p) != null)
      {
        JavaCCErrors.semantic_error (p, p.getLhs () + " occurs on the left hand side of more than one production.");
      }
    }

    /*
     * The following walks the entire parse tree to make sure that all non-terminals on RHS's are
     * defined on the LHS.
     */
    for (final NormalProduction aNormalProduction : grammar ().bnfProductions ())
    {
      ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new ProductionDefinedChecker ());
    }

    /*
     * The following loop ensures that all target lexical states are defined. Also piggybacking on
     * this loop is the detection of <EOF> and <name> in token productions. After reporting an
     * error, these entries are removed. Also checked are definitions on inline private regular
     * expressions. This loop works slightly differently when USER_TOKEN_MANAGER is set to true. In
     * this case, <name> occurrences are OK, while regular expression specs generate a warning.
     */
    for (final TokenProduction aTokenProduction : grammar ().rexprList ())
    {
      final TokenProduction aTp = (aTokenProduction);
      final List <RegExprSpec> aRespecs = aTp.m_aRespecs;
      for (final RegExprSpec aRegExprSpec : aRespecs)
      {
        final RegExprSpec aRes = (aRegExprSpec);
        if (aRes.m_sNextState != null)
        {
          if (grammar ().lexStateS2I ().get (aRes.m_sNextState) == null)
          {
            JavaCCErrors.semantic_error (aRes.m_aNsTok, "Lexical state \"" + aRes.m_sNextState + "\" has not been defined.");
          }
        }
        if (aRes.m_aRexp instanceof ExpREndOfFile)
        {
          // JavaCCErrors.semantic_error(res.m_aRexp, "Badly placed <EOF>.");
          if (aTp.m_aLexStates != null)
            JavaCCErrors.semantic_error (aRes.m_aRexp,
                                         "EOF action/state change must be specified for all states, " +
                                                   "i.e., <*>TOKEN:.");
          if (aTp.m_eKind != ETokenKind.TOKEN)
            JavaCCErrors.semantic_error (aRes.m_aRexp,
                                         "EOF action/state change can be specified only in a " +
                                                   "TOKEN specification.");
          if (grammar ().getNextStateForEof () != null || grammar ().getActionForEof () != null)
            JavaCCErrors.semantic_error (aRes.m_aRexp, "Duplicate action/state change specification for <EOF>.");
          grammar ().setActionForEof (aRes.m_aAct);
          grammar ().setNextStateForEof (aRes.m_sNextState);
          prepareToRemove (aRespecs, aRes);
        }
        else
          if (aTp.m_bIsExplicit && Options.isUserTokenManager ())
          {
            JavaCCErrors.warning (aRes.m_aRexp,
                                  "Ignoring regular expression specification since " +
                                            "option USER_TOKEN_MANAGER has been set to true.");
          }
          else
            if (aTp.m_bIsExplicit && !Options.isUserTokenManager () && aRes.m_aRexp instanceof ExpRJustName)
            {
              JavaCCErrors.warning (aRes.m_aRexp,
                                    "Ignoring free-standing regular expression reference.  " +
                                              "If you really want this, you must give it a different label as <NEWLABEL:<" +
                                              aRes.m_aRexp.getLabel () +
                                              ">>.");
              prepareToRemove (aRespecs, aRes);
            }
            else
              if (!aTp.m_bIsExplicit && aRes.m_aRexp.m_bPrivateRexp)
              {
                JavaCCErrors.semantic_error (aRes.m_aRexp,
                                             "Private (#) regular expression cannot be defined within " +
                                                       "grammar productions.");
              }
      }
    }

    removePreparedItems ();

    /*
     * The following loop inserts all names of regular expressions into "named_tokens_table" and
     * "ordered_named_tokens". Duplications are flagged as errors.
     */
    for (final TokenProduction aTokenProduction : grammar ().rexprList ())
    {
      final TokenProduction aTp = (aTokenProduction);
      final List <RegExprSpec> aRespecs = aTp.m_aRespecs;
      for (final RegExprSpec aRegExprSpec : aRespecs)
      {
        final RegExprSpec aRes = (aRegExprSpec);
        if (!(aRes.m_aRexp instanceof ExpRJustName) && aRes.m_aRexp.hasLabel ())
        {
          final String s = aRes.m_aRexp.getLabel ();
          final AbstractExpRegularExpression aObj = grammar ().namedTokensTable ().put (s, aRes.m_aRexp);
          if (aObj != null)
          {
            JavaCCErrors.semantic_error (aRes.m_aRexp, "Multiply defined lexical token name \"" + s + "\".");
          }
          else
          {
            grammar ().orderedNameTokens ().add (aRes.m_aRexp);
          }
          if (grammar ().lexStateS2I ().get (s) != null)
          {
            JavaCCErrors.semantic_error (aRes.m_aRexp,
                                         "Lexical token name \"" +
                                                   s +
                                                   "\" is the same as " +
                                                   "that of a lexical state.");
          }
        }
      }
    }

    /*
     * The following code merges multiple uses of the same string in the same lexical state and
     * produces error messages when there are multiple explicit occurrences (outside the BNF) of the
     * string in the same lexical state, or when within BNF occurrences of a string are duplicates
     * of those that occur as non-TOKEN's (SKIP, MORE, SPECIAL_TOKEN) or private regular
     * expressions. While doing this, this code also numbers all regular expressions (by setting
     * their ordinal values), and populates the table "names_of_tokens".
     */

    grammar ().setTokenCount (1);
    for (final TokenProduction tp : grammar ().rexprList ())
    {
      final List <RegExprSpec> aRespecs = tp.m_aRespecs;
      if (tp.m_aLexStates == null)
      {
        tp.m_aLexStates = new String [grammar ().lexStateI2S ().size ()];
        grammar ().lexStateI2S ().values ().toArray (tp.m_aLexStates);
      }

      @SuppressWarnings ("unchecked")
      final Map <String, Map <String, AbstractExpRegularExpression>> table[] = new Map [tp.m_aLexStates.length];
      for (int i = 0; i < tp.m_aLexStates.length; i++)
      {
        table[i] = grammar ().simpleTokensTable ().get (tp.m_aLexStates[i]);
      }

      for (final RegExprSpec aRegExprSpec : aRespecs)
      {
        final RegExprSpec aRes = (aRegExprSpec);
        if (aRes.m_aRexp instanceof final ExpRStringLiteral sl)
        {
          // This loop performs the checks and actions with respect to each
          // lexical state.
          for (int i = 0; i < table.length; i++)
          {
            // Get table of all case variants of "sl.image" into table2.
            Map <String, AbstractExpRegularExpression> aTable2 = table[i].get (sl.m_sImage.toUpperCase (Locale.US));
            if (aTable2 == null)
            {
              // There are no case variants of "sl.image" earlier than the
              // current one.
              // So go ahead and insert this item.
              if (sl.getOrdinal () == 0)
              {
                sl.setOrdinal (grammar ().getAndIncTokenCount ());
              }
              aTable2 = new HashMap <> ();
              aTable2.put (sl.m_sImage, sl);
              table[i].put (sl.m_sImage.toUpperCase (Locale.US), aTable2);
            }
            else
              if (findIgnoreCase (aTable2, sl.m_sImage) != null)
              {
                // Since IGNORE_CASE version exists, current one is useless and
                // bad.
                final AbstractExpRegularExpression aOther = findIgnoreCase (aTable2, sl.m_sImage);
                if (!sl.m_aTpContext.m_bIsExplicit)
                {
                  // inline BNF string is used earlier with an IGNORE_CASE.
                  JavaCCErrors.semantic_error (sl,
                                               "String \"" +
                                                   sl.m_sImage +
                                                   "\" can never be matched " +
                                                   "due to presence of more general (IGNORE_CASE) regular expression " +
                                                   "at line " +
                                                   aOther.getLineNumber () +
                                                   ", column " +
                                                   aOther.getColumnNumber () +
                                                   ".");
                }
                else
                {
                  // give the standard error message.
                  JavaCCErrors.semantic_error (sl,
                                               "Duplicate definition of string token \"" +
                                                   sl.m_sImage +
                                                   "\" " +
                                                   "can never be matched.");
                }
              }
              else
                if (sl.m_aTpContext.m_bIgnoreCase)
                {
                  // This has to be explicit. A warning needs to be given with
                  // respect
                  // to all previous strings.
                  final StringBuilder aPos = new StringBuilder ();
                  int nCount = 0;
                  for (final AbstractExpRegularExpression rexp : aTable2.values ())
                  {
                    if (nCount != 0)
                      aPos.append (",");
                    aPos.append (" line ").append (rexp.getLineNumber ());
                    nCount++;
                  }
                  if (nCount == 1)
                  {
                    JavaCCErrors.warning (sl,
                                          "String with IGNORE_CASE is partially superceded by string at" + aPos + ".");
                  }
                  else
                  {
                    JavaCCErrors.warning (sl,
                                          "String with IGNORE_CASE is partially superceded by strings at" + aPos + ".");
                  }
                  // This entry is legitimate. So insert it.
                  if (sl.getOrdinal () == 0)
                  {
                    sl.setOrdinal (grammar ().getAndIncTokenCount ());
                  }
                  aTable2.put (sl.m_sImage, sl);
                  // The above "put" may override an existing entry (that is not
                  // IGNORE_CASE) and that's
                  // the desired behavior.
                }
                else
                {
                  // The rest of the cases do not involve IGNORE_CASE.
                  final AbstractExpRegularExpression aRe = aTable2.get (sl.m_sImage);
                  if (aRe == null)
                  {
                    if (sl.getOrdinal () == 0)
                    {
                      sl.setOrdinal (grammar ().getAndIncTokenCount ());
                    }
                    aTable2.put (sl.m_sImage, sl);
                  }
                  else
                    if (tp.m_bIsExplicit)
                    {
                      // This is an error even if the first occurrence was
                      // implicit.
                      if (tp.m_aLexStates[i].equals ("DEFAULT"))
                      {
                        JavaCCErrors.semantic_error (sl,
                                                     "Duplicate definition of string token \"" + sl.m_sImage + "\".");
                      }
                      else
                      {
                        JavaCCErrors.semantic_error (sl,
                                                     "Duplicate definition of string token \"" +
                                                         sl.m_sImage +
                                                         "\" in lexical state \"" +
                                                         tp.m_aLexStates[i] +
                                                         "\".");
                      }
                    }
                    else
                      if (aRe.m_aTpContext.m_eKind != ETokenKind.TOKEN)
                      {
                        JavaCCErrors.semantic_error (sl,
                                                     "String token \"" +
                                                         sl.m_sImage +
                                                         "\" has been defined as a \"" +
                                                         aRe.m_aTpContext.m_eKind.getImage () +
                                                         "\" token.");
                      }
                      else
                        if (aRe.m_bPrivateRexp)
                        {
                          JavaCCErrors.semantic_error (sl,
                                                       "String token \"" +
                                                           sl.m_sImage +
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
                          sl.setOrdinal (aRe.getOrdinal ());
                          prepareToRemove (aRespecs, aRes);
                        }
                }
          }
        }
        else
          if (!(aRes.m_aRexp instanceof ExpRJustName))
          {
            aRes.m_aRexp.setOrdinal (grammar ().getAndIncTokenCount ());
          }
        if (!(aRes.m_aRexp instanceof ExpRJustName) && aRes.m_aRexp.hasLabel ())
        {
          grammar ().namesOfTokens ().put (Integer.valueOf (aRes.m_aRexp.getOrdinal ()), aRes.m_aRexp.getLabel ());
        }
        if (!(aRes.m_aRexp instanceof ExpRJustName))
        {
          grammar ().rexpsOfTokens ().put (Integer.valueOf (aRes.m_aRexp.getOrdinal ()), aRes.m_aRexp);
        }
      }
    }

    removePreparedItems ();

    /*
     * The following code performs a tree walk on all regular expressions attaching links to
     * "RJustName"s. Error messages are given if undeclared names are used, or if "RJustNames" refer
     * to private regular expressions or to regular expressions of any kind other than TOKEN. In
     * addition, this loop also removes top level "RJustName"s from "rexprlist". This code is not
     * executed if Options.getUserTokenManager() is set to true. Instead the following block of code
     * is executed.
     */

    if (!Options.isUserTokenManager ())
    {
      final FixRJustNames aFrjn = new FixRJustNames ();
      for (final TokenProduction aTokenProduction : grammar ().rexprList ())
      {
        final TokenProduction aTp = (aTokenProduction);
        final List <RegExprSpec> aRespecs = aTp.m_aRespecs;
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          aFrjn.m_aRoot = aRes.m_aRexp;
          ExpansionTreeWalker.preOrderWalk (aRes.m_aRexp, aFrjn);
          if (aRes.m_aRexp instanceof ExpRJustName)
          {
            prepareToRemove (aRespecs, aRes);
          }
        }
      }
    }

    removePreparedItems ();

    /*
     * The following code is executed only if Options.getUserTokenManager() is set to true. This
     * code visits all top-level "RJustName"s (ignores "RJustName"s nested within regular
     * expressions). Since regular expressions are optional in this case, "RJustName"s without
     * corresponding regular expressions are given ordinal values here. If "RJustName"s refer to a
     * named regular expression, their ordinal values are set to reflect this. All but one
     * "RJustName" node is removed from the lists by the end of execution of this code.
     */

    if (Options.isUserTokenManager ())
    {
      for (final TokenProduction aTokenProduction : grammar ().rexprList ())
      {
        final TokenProduction aTp = (aTokenProduction);
        final List <RegExprSpec> aRespecs = aTp.m_aRespecs;
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          if (aRes.m_aRexp instanceof final ExpRJustName jn)
          {
            final AbstractExpRegularExpression aRexp = grammar ().namedTokensTable ().get (jn.getLabel ());
            if (aRexp == null)
            {
              jn.setOrdinal (grammar ().getAndIncTokenCount ());
              grammar ().namedTokensTable ().put (jn.getLabel (), jn);
              grammar ().orderedNameTokens ().add (jn);
              grammar ().namesOfTokens ().put (Integer.valueOf (jn.getOrdinal ()), jn.getLabel ());
            }
            else
            {
              jn.setOrdinal (aRexp.getOrdinal ());
              prepareToRemove (aRespecs, aRes);
            }
          }
        }
      }
    }

    removePreparedItems ();

    /*
     * The following code is executed only if Options.getUserTokenManager() is set to true. This
     * loop labels any unlabeled regular expression and prints a warning that it is doing so. These
     * labels are added to "ordered_named_tokens" so that they may be generated into the
     * ...Constants file.
     */
    if (Options.isUserTokenManager ())
    {
      for (final TokenProduction aTokenProduction : grammar ().rexprList ())
      {
        final TokenProduction aTp = (aTokenProduction);
        final List <RegExprSpec> aRespecs = aTp.m_aRespecs;
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          final Integer aIi = Integer.valueOf (aRes.m_aRexp.getOrdinal ());
          if (grammar ().namesOfTokens ().get (aIi) == null)
          {
            JavaCCErrors.warning (aRes.m_aRexp,
                                  "Unlabeled regular expression cannot be referred to by " +
                                            "user generated token manager.");
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
    boolean bEmptyUpdate = true;
    while (bEmptyUpdate)
    {
      bEmptyUpdate = false;
      for (final NormalProduction aNormalProduction : grammar ().bnfProductions ())
      {
        final NormalProduction aProd = aNormalProduction;
        if (emptyExpansionExists (aProd.getExpansion ()))
        {
          if (!aProd.isEmptyPossible ())
          {
            bEmptyUpdate = aProd.setEmptyPossible (true);
          }
        }
      }
    }

    if (Options.isSanityCheck () && JavaCCErrors.getErrorCount () == 0)
    {

      // The following code checks that all ZeroOrMore, ZeroOrOne, and OneOrMore
      // nodes
      // do not contain expansions that can expand to the empty token list.
      for (final NormalProduction aNormalProduction : grammar ().bnfProductions ())
      {
        ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new EmptyChecker ());
      }

      // The following code goes through the productions and adds pointers to
      // other
      // productions that it can expand to without consuming any tokens. Once
      // this is
      // done, a left-recursion check can be performed.
      for (final NormalProduction prod : grammar ().bnfProductions ())
      {
        _addLeftMost (prod, prod.getExpansion ());
      }

      // Now the following loop calls a recursive walk routine that searches for
      // actual left recursions. The way the algorithm is coded, once a node has
      // been determined to participate in a left recursive loop, it is not
      // tried
      // in any other loop.
      for (final NormalProduction prod : grammar ().bnfProductions ())
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
        for (final TokenProduction aTokenProduction : grammar ().rexprList ())
        {
          final TokenProduction aTp = (aTokenProduction);
          final List <RegExprSpec> aRespecs = aTp.m_aRespecs;
          for (final RegExprSpec aRegExprSpec : aRespecs)
          {
            final RegExprSpec aRes = (aRegExprSpec);
            final AbstractExpRegularExpression aRexp = aRes.m_aRexp;
            if (aRexp.getWalkStatus () == 0)
            {
              aRexp.setWalkStatus (-1);
              if (_rexpWalk (aRexp))
              {
                PGCCContext.current ()
                           .semanticize ()
                           .setLoopString ("..." +
                                           aRexp.getLabel () +
                                           "... --> " +
                                           PGCCContext.current ().semanticize ().getLoopString ());
                JavaCCErrors.semantic_error (aRexp,
                                             "Loop in regular expression detected: \"" +
                                                   PGCCContext.current ().semanticize ().getLoopString () +
                                                   "\"");
              }
              aRexp.setWalkStatus (1);
            }
          }
        }
      }

      /*
       * The following code performs the lookahead ambiguity checking.
       */
      if (JavaCCErrors.getErrorCount () == 0)
      {
        for (final NormalProduction aNormalProduction : grammar ().bnfProductions ())
        {
          ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new LookaheadChecker ());
        }
      }
    } // matches "if (Options.getSanityCheck()) {"

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");
  }

  /**
   * Check whether "str" is superceded by another equal (except case) string in the table.
   *
   * @param table
   *        The string literals to search. May not be <code>null</code>.
   * @param str
   *        The image to check. May not be <code>null</code>.
   * @return The <code>IGNORE_CASE</code> regular expression that supercedes "str", or
   *         <code>null</code> if there is none. Used to be returned through a static field.
   */
  @Nullable
  public static AbstractExpRegularExpression findIgnoreCase (final Map <String, AbstractExpRegularExpression> aTable,
                                                             final String sStr)
  {
    final AbstractExpRegularExpression aRexp = aTable.get (sStr);
    if (aRexp != null && !aRexp.m_aTpContext.m_bIgnoreCase)
      return null;

    for (final AbstractExpRegularExpression aRegEx : aTable.values ())
      if (aRegEx.m_aTpContext.m_bIgnoreCase)
        return aRegEx;
    return null;
  }

  // returns true if "exp" can expand to the empty string, returns false
  // otherwise.
  public static boolean emptyExpansionExists (final Expansion aExp)
  {
    if (aExp instanceof final ExpNonTerminal aNonTerminal)
    {
      return aNonTerminal.getProd ().isEmptyPossible ();
    }

    if (aExp instanceof ExpAction)
    {
      return true;
    }

    if (aExp instanceof AbstractExpRegularExpression)
    {
      return false;
    }

    if (aExp instanceof final ExpOneOrMore aOneOrMore)
    {
      return emptyExpansionExists (aOneOrMore.getExpansion ());
    }

    if (aExp instanceof ExpZeroOrMore || aExp instanceof ExpZeroOrOne)
    {
      return true;
    }

    if (aExp instanceof ExpLookahead)
    {
      return true;
    }

    if (aExp instanceof final ExpChoice aChoice)
    {
      for (final Expansion aElement : aChoice.getChoices ())
        if (emptyExpansionExists (aElement))
          return true;
      return false;
    }

    if (aExp instanceof final ExpSequence aSequence)
    {
      for (final Expansion aElement : aSequence.getUnits ())
        if (!emptyExpansionExists (aElement))
          return false;
      return true;
    }

    if (aExp instanceof final ExpTryBlock aTryBlock)
    {
      return emptyExpansionExists (aTryBlock.m_aExp);
    }

    // This should be dead code.
    return false;
  }

  // Updates prod.leftExpansions based on a walk of exp.
  static private void _addLeftMost (final NormalProduction aProd, final Expansion aExp)
  {
    if (aExp instanceof final ExpNonTerminal aExpNonTerminal)
    {
      for (int i = 0; i < aProd.m_nLeIndex; i++)
      {
        if (aProd.getLeftExpansions ()[i] == aExpNonTerminal.getProd ())
        {
          return;
        }
      }
      if (aProd.m_nLeIndex == aProd.getLeftExpansions ().length)
      {
        final NormalProduction [] aNewle = new NormalProduction [aProd.m_nLeIndex * 2];
        System.arraycopy (aProd.getLeftExpansions (), 0, aNewle, 0, aProd.m_nLeIndex);
        aProd.setLeftExpansions (aNewle);
      }
      aProd.getLeftExpansions ()[aProd.m_nLeIndex++] = aExpNonTerminal.getProd ();
    }
    else
      if (aExp instanceof final ExpOneOrMore aExpOneOrMore)
      {
        _addLeftMost (aProd, aExpOneOrMore.getExpansion ());
      }
      else
        if (aExp instanceof final ExpZeroOrMore aZeroOrMore)
        {
          _addLeftMost (aProd, aZeroOrMore.getExpansion ());
        }
        else
          if (aExp instanceof final ExpZeroOrOne aZeroOrOne)
          {
            _addLeftMost (aProd, aZeroOrOne.getExpansion ());
          }
          else
            if (aExp instanceof final ExpChoice aExpChoice)
            {
              for (final Expansion aObject : aExpChoice.getChoices ())
                _addLeftMost (aProd, aObject);
            }
            else
              if (aExp instanceof final ExpSequence aExpSequence)
              {
                for (final Expansion aObject : aExpSequence.getUnits ())
                {
                  _addLeftMost (aProd, aObject);
                  if (!emptyExpansionExists (aObject))
                    break;
                }
              }
              else
                if (aExp instanceof final ExpTryBlock aExpTryBlock)
                {
                  _addLeftMost (aProd, aExpTryBlock.m_aExp);
                }
  }

  // The string in which the following methods store information.

  // Returns true to indicate an unraveling of a detected left recursion loop,
  // and returns false otherwise.
  private static boolean _prodWalk (final NormalProduction aProd)
  {
    aProd.setWalkStatus (-1);
    for (int i = 0; i < aProd.m_nLeIndex; i++)
    {
      if (aProd.getLeftExpansions ()[i].getWalkStatus () == -1)
      {
        aProd.getLeftExpansions ()[i].setWalkStatus (-2);
        PGCCContext.current ()
                   .semanticize ()
                   .setLoopString (aProd.getLhs () + "... --> " + aProd.getLeftExpansions ()[i].getLhs () + "...");
        if (aProd.getWalkStatus () == -2)
        {
          aProd.setWalkStatus (1);
          JavaCCErrors.semantic_error (aProd,
                                       "Left recursion detected: \"" +
                                             PGCCContext.current ().semanticize ().getLoopString () +
                                             "\"");
          return false;
        }
        aProd.setWalkStatus (1);
        return true;
      }
      else
        if (aProd.getLeftExpansions ()[i].getWalkStatus () == 0)
        {
          if (_prodWalk (aProd.getLeftExpansions ()[i]))
          {
            PGCCContext.current ()
                       .semanticize ()
                       .setLoopString (aProd.getLhs () +
                                       "... --> " +
                                       PGCCContext.current ().semanticize ().getLoopString ());
            if (aProd.getWalkStatus () == -2)
            {
              aProd.setWalkStatus (1);
              JavaCCErrors.semantic_error (aProd,
                                           "Left recursion detected: \"" +
                                                 PGCCContext.current ().semanticize ().getLoopString () +
                                                 "\"");
              return false;
            }
            aProd.setWalkStatus (1);
            return true;
          }
        }
    }
    aProd.setWalkStatus (1);
    return false;
  }

  // Returns true to indicate an unraveling of a detected loop,
  // and returns false otherwise.
  static private boolean _rexpWalk (final AbstractExpRegularExpression aRexp)
  {
    if (aRexp instanceof final ExpRJustName jn)
    {
      if (jn.m_aRegexpr.getWalkStatus () == -1)
      {
        jn.m_aRegexpr.setWalkStatus (-2);
        PGCCContext.current ().semanticize ().setLoopString ("..." + jn.m_aRegexpr.getLabel () + "...");
        // Note: Only the regexpr's of RJustName nodes and the top leve
        // regexpr's can have labels. Hence it is only in these cases that
        // the labels are checked for to be added to the loopString.
        return true;
      }
      else
        if (jn.m_aRegexpr.getWalkStatus () == 0)
        {
          jn.m_aRegexpr.setWalkStatus (-1);
          if (_rexpWalk (jn.m_aRegexpr))
          {
            PGCCContext.current ()
                       .semanticize ()
                       .setLoopString ("..." +
                                       jn.m_aRegexpr.getLabel () +
                                       "... --> " +
                                       PGCCContext.current ().semanticize ().getLoopString ());
            if (jn.m_aRegexpr.getWalkStatus () == -2)
            {
              jn.m_aRegexpr.setWalkStatus (1);
              JavaCCErrors.semantic_error (jn.m_aRegexpr,
                                           "Loop in regular expression detected: \"" +
                                                         PGCCContext.current ().semanticize ().getLoopString () +
                                                         "\"");
              return false;
            }
            jn.m_aRegexpr.setWalkStatus (1);
            return true;
          }
          jn.m_aRegexpr.setWalkStatus (1);
          return false;
        }
    }

    if (aRexp instanceof final ExpRChoice aRChoice)
    {
      for (final AbstractExpRegularExpression aElement : aRChoice.getChoices ())
        if (_rexpWalk (aElement))
          return true;
      return false;
    }

    if (aRexp instanceof final ExpRSequence aRSequence)
    {
      for (final AbstractExpRegularExpression aElement : aRSequence.getUnits ())
        if (_rexpWalk (aElement))
          return true;
      return false;
    }

    if (aRexp instanceof final ExpROneOrMore aROneOrMore)
    {
      return _rexpWalk (aROneOrMore.getRegExpr ());
    }

    if (aRexp instanceof final ExpRZeroOrMore aRZeroOrMore)
    {
      return _rexpWalk (aRZeroOrMore.getRegExpr ());
    }

    if (aRexp instanceof final ExpRZeroOrOne aRZeroOrOne)
    {
      return _rexpWalk (aRZeroOrOne.getRegExpr ());
    }

    if (aRexp instanceof final ExpRRepetitionRange aRRepetitionRange)
    {
      return _rexpWalk (aRRepetitionRange.getRegExpr ());
    }

    return false;
  }

  /**
   * Objects of this class are created from class Semanticize to work on references to regular
   * expressions from RJustName's.
   */
  static final class FixRJustNames implements ITreeWalkerOperation
  {
    public AbstractExpRegularExpression m_aRoot;

    public boolean goDeeper (final Expansion e)
    {
      return true;
    }

    public void action (final Expansion e)
    {
      if (e instanceof final ExpRJustName jn)
      {
        final AbstractExpRegularExpression aRexp = grammar ().namedTokensTable ().get (jn.getLabel ());
        if (aRexp == null)
        {
          JavaCCErrors.semantic_error (e, "Undefined lexical token name \"" + jn.getLabel () + "\".");
        }
        else
          if (jn == m_aRoot && !jn.m_aTpContext.m_bIsExplicit && aRexp.m_bPrivateRexp)
          {
            JavaCCErrors.semantic_error (e,
                                         "Token name \"" +
                                            jn.getLabel () +
                                            "\" refers to a private " +
                                            "(with a #) regular expression.");
          }
          else
            if (jn == m_aRoot && !jn.m_aTpContext.m_bIsExplicit && aRexp.m_aTpContext.m_eKind != ETokenKind.TOKEN)
            {
              JavaCCErrors.semantic_error (e,
                                           "Token name \"" +
                                              jn.getLabel () +
                                              "\" refers to a non-token " +
                                              "(SKIP, MORE, IGNORE_IN_BNF) regular expression.");
            }
            else
            {
              jn.setOrdinal (aRexp.getOrdinal ());
              jn.m_aRegexpr = aRexp;
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
      if (e instanceof final ExpSequence seq)
      {
        if (e.getParent () instanceof ExpChoice ||
          e.getParent () instanceof ExpZeroOrMore ||
          e.getParent () instanceof ExpOneOrMore ||
          e.getParent () instanceof ExpZeroOrOne)
        {
          return;
        }
        final ExpLookahead aLa = (ExpLookahead) (seq.getUnitAt (0));
        if (!aLa.isExplicit ())
          return;

        // Create a singleton choice with an empty action.
        final ExpChoice aCh = new ExpChoice ();
        aCh.setLineNumber (aLa.getLineNumber ());
        aCh.setColumnNumber (aLa.getColumnNumber ());
        aCh.setParent (seq);

        final ExpSequence aSeq1 = new ExpSequence ();
        aSeq1.setLineNumber (aLa.getLineNumber ());
        aSeq1.setColumnNumber (aLa.getColumnNumber ());
        aSeq1.setParent (aCh);
        aSeq1.addUnit (aLa);
        aLa.setParent (aSeq1);

        final ExpAction aAct = new ExpAction ();
        aAct.setLineNumber (aLa.getLineNumber ());
        aAct.setColumnNumber (aLa.getColumnNumber ());
        aAct.setParent (aSeq1);

        aSeq1.addUnit (aAct);
        aCh.addChoice (aSeq1);
        if (aLa.getAmount () != 0)
        {
          if (aLa.getActionTokens ().isNotEmpty ())
          {
            JavaCCErrors.warning (aLa,
                                  "Encountered LOOKAHEAD(...) at a non-choice location.  " +
                                      "Only semantic lookahead will be considered here.");
          }
          else
          {
            JavaCCErrors.warning (aLa, "Encountered LOOKAHEAD(...) at a non-choice location.  This will be ignored.");
          }
        }
        // Now we have moved the lookahead into the singleton choice. Now create
        // a new dummy lookahead node to replace this one at its original
        // location.
        final ExpLookahead aLa1 = new ExpLookahead ();
        aLa1.setExplicit (false);
        aLa1.setLineNumber (aLa.getLineNumber ());
        aLa1.setColumnNumber (aLa.getColumnNumber ());
        aLa1.setParent (seq);

        // Now set the la_expansion field of la and la1 with a dummy expansion
        // (we use EOF).
        aLa.setLaExpansion (new ExpREndOfFile ());
        aLa1.setLaExpansion (new ExpREndOfFile ());
        seq.setUnit (0, aLa1);
        seq.addUnit (1, aCh);
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
      if (e instanceof final ExpNonTerminal nt)
      {
        final NormalProduction aNp = grammar ().productionTable ().get (nt.getName ());
        if (aNp == null)
        {
          JavaCCErrors.semantic_error (e, "Non-terminal " + nt.getName () + " has not been defined.");
        }
        else
        {
          nt.setProd (aNp);
          aNp.getParents ().add (nt);
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
        if (e instanceof final ExpZeroOrMore aExpZeroOrMore)
        {
          if (Semanticize.emptyExpansionExists (aExpZeroOrMore.getExpansion ()))
          {
            JavaCCErrors.semantic_error (e, "Expansion within \"(...)*\" can be matched by empty string.");
          }
        }
        else
          if (e instanceof final ExpZeroOrOne aExpZeroOrOne)
          {
            if (Semanticize.emptyExpansionExists (aExpZeroOrOne.getExpansion ()))
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
        if (e instanceof final ExpOneOrMore exp)
        {
          if (Options.isForceLaCheck () || (implicitLA (exp.getExpansion ()) && Options.getLookahead () == 1))
          {
            LookaheadCalc.ebnfCalc (exp, exp.getExpansion ());
          }
        }
        else
          if (e instanceof final ExpZeroOrMore exp)
          {
            if (Options.isForceLaCheck () || (implicitLA (exp.getExpansion ()) && Options.getLookahead () == 1))
            {
              LookaheadCalc.ebnfCalc (exp, exp.getExpansion ());
            }
          }
          else
            if (e instanceof final ExpZeroOrOne exp)
            {
              if (Options.isForceLaCheck () || (implicitLA (exp.getExpansion ()) && Options.getLookahead () == 1))
              {
                LookaheadCalc.ebnfCalc (exp, exp.getExpansion ());
              }
            }
    }

    static boolean implicitLA (final Expansion aExp)
    {
      if (!(aExp instanceof ExpSequence))
        return true;

      final ExpSequence aSeq = (ExpSequence) aExp;
      final Object aObj = aSeq.getUnitAt (0);
      if (!(aObj instanceof ExpLookahead))
        return true;

      final ExpLookahead aLa = (ExpLookahead) aObj;
      return !aLa.isExplicit ();
    }
  }

}
