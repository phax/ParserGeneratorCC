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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.pgcc.context.GrammarState;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.parser.exp.*;

/**
 * The checks that a grammar which parses also makes sense: every production is defined, no token is
 * unreachable, no loop can match the empty string.
 */
public class Semanticize
{
  /** Default constructor. */
  public Semanticize ()
  {}

  private static void prepareToRemove (final List <RegExprSpec> aVec, final Object aItem)
  {
    PGCCContext.current ().semanticize ().prepareToRemove (aVec, aItem);
  }

  private static void removePreparedItems ()
  {
    PGCCContext.current ().semanticize ().removePreparedItems ();
  }

  /**
   * Run every check over the grammar that has been read.
   *
   * @throws MetaParseException
   *         if a check cannot be completed
   */
  public static void start () throws MetaParseException
  {
    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    if (Options.getLookahead () > 1 && !Options.isForceLaCheck () && Options.isSanityCheck ())
    {
      JavaCCErrors.warning ("Lookahead adequacy checking not being performed since option LOOKAHEAD " +
                            "is more than 1.  Set option FORCE_LA_CHECK to true to force checking.");
    }

    final GrammarState grammar = grammar ();
    /*
     * The following walks the entire parse tree to convert all LOOKAHEAD's that are not at choice
     * points (but at beginning of sequences) and converts them to trivial choices. This way, their
     * semantic lookahead specification can be evaluated during other lookahead evaluations.
     */
    for (final AbstractNormalProduction aNormalProduction : grammar.bnfProductions ())
    {
      ExpansionTreeWalker.postOrderWalk (aNormalProduction.getExpansion (), new LookaheadFixer ());
    }

    /*
     * The following loop populates "production_table"
     */
    for (final AbstractNormalProduction p : grammar.bnfProductions ())
    {
      if (grammar.productionTable ().put (p.getLhs (), p) != null)
      {
        JavaCCErrors.semanticError (p, p.getLhs () + " occurs on the left hand side of more than one production.");
      }
    }

    /*
     * The following walks the entire parse tree to make sure that all non-terminals on RHS's are
     * defined on the LHS.
     */
    for (final AbstractNormalProduction aNormalProduction : grammar.bnfProductions ())
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
    for (final TokenProduction aTokenProduction : grammar.rexprList ())
    {
      final TokenProduction aTp = (aTokenProduction);
      final List <RegExprSpec> aRespecs = aTp.getRespecs ();
      for (final RegExprSpec aRegExprSpec : aRespecs)
      {
        final RegExprSpec aRes = (aRegExprSpec);
        if (aRes.getNextState () != null)
        {
          if (grammar.lexStateS2I ().get (aRes.getNextState ()) == null)
          {
            JavaCCErrors.semanticError (aRes.getNsTok (),
                                        "Lexical state \"" + aRes.getNextState () + "\" has not been defined.");
          }
        }
        if (aRes.getRexp () instanceof ExpREndOfFile)
        {
          // JavaCCErrors.semanticError(res.getRexp (), "Badly placed <EOF>.");
          if (aTp.getLexStates () != null)
            JavaCCErrors.semanticError (aRes.getRexp (),
                                        "EOF action/state change must be specified for all states, " +
                                                         "i.e., <*>TOKEN:.");
          if (aTp.getKind () != ETokenKind.TOKEN)
            JavaCCErrors.semanticError (aRes.getRexp (),
                                        "EOF action/state change can be specified only in a " + "TOKEN specification.");
          if (grammar.getNextStateForEof () != null || grammar.getActionForEof () != null)
            JavaCCErrors.semanticError (aRes.getRexp (), "Duplicate action/state change specification for <EOF>.");
          grammar.setActionForEof (aRes.getAct ());
          grammar.setNextStateForEof (aRes.getNextState ());
          prepareToRemove (aRespecs, aRes);
        }
        else
          if (aTp.isExplicit () && Options.isUserTokenManager ())
          {
            JavaCCErrors.warning (aRes.getRexp (),
                                  "Ignoring regular expression specification since " +
                                                   "option USER_TOKEN_MANAGER has been set to true.");
          }
          else
            if (aTp.isExplicit () && !Options.isUserTokenManager () && aRes.getRexp () instanceof ExpRJustName)
            {
              JavaCCErrors.warning (aRes.getRexp (),
                                    "Ignoring free-standing regular expression reference.  " +
                                                     "If you really want this, you must give it a different label as <NEWLABEL:<" +
                                                     aRes.getRexp ().getLabel () +
                                                     ">>.");
              prepareToRemove (aRespecs, aRes);
            }
            else
              if (!aTp.isExplicit () && aRes.getRexp ().m_bPrivateRexp)
              {
                JavaCCErrors.semanticError (aRes.getRexp (),
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
    for (final TokenProduction aTokenProduction : grammar.rexprList ())
    {
      final TokenProduction aTp = (aTokenProduction);
      final List <RegExprSpec> aRespecs = aTp.getRespecs ();
      for (final RegExprSpec aRegExprSpec : aRespecs)
      {
        final RegExprSpec aRes = (aRegExprSpec);
        if (!(aRes.getRexp () instanceof ExpRJustName) && aRes.getRexp ().hasLabel ())
        {
          final String s = aRes.getRexp ().getLabel ();
          final AbstractExpRegularExpression aObj = grammar.namedTokensTable ().put (s, aRes.getRexp ());
          if (aObj != null)
          {
            JavaCCErrors.semanticError (aRes.getRexp (), "Multiply defined lexical token name \"" + s + "\".");
          }
          else
          {
            grammar.orderedNameTokens ().add (aRes.getRexp ());
          }
          if (grammar.lexStateS2I ().get (s) != null)
          {
            JavaCCErrors.semanticError (aRes.getRexp (),
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

    grammar.setTokenCount (1);
    for (final TokenProduction tp : grammar.rexprList ())
    {
      final List <RegExprSpec> aRespecs = tp.getRespecs ();
      if (tp.getLexStates () == null)
      {
        tp.setLexStates (new String [grammar.lexStateI2S ().size ()]);
        grammar.lexStateI2S ().values ().toArray (tp.getLexStates ());
      }

      @SuppressWarnings ("unchecked")
      final Map <String, Map <String, AbstractExpRegularExpression>> table[] = new Map [tp.getLexStates ().length];
      for (int i = 0; i < tp.getLexStates ().length; i++)
      {
        table[i] = grammar.simpleTokensTable ().get (tp.getLexStates ()[i]);
      }

      for (final RegExprSpec aRegExprSpec : aRespecs)
      {
        final RegExprSpec aRes = (aRegExprSpec);
        if (aRes.getRexp () instanceof final ExpRStringLiteral sl)
        {
          // This loop performs the checks and actions with respect to each
          // lexical state.
          for (int i = 0; i < table.length; i++)
          {
            // Get table of all case variants of "sl.image" into table2.
            Map <String, AbstractExpRegularExpression> aTable2 = table[i].get (sl.getImage ().toUpperCase (Locale.US));
            if (aTable2 == null)
            {
              // There are no case variants of "sl.image" earlier than the
              // current one.
              // So go ahead and insert this item.
              if (sl.getOrdinal () == 0)
              {
                sl.setOrdinal (grammar.getAndIncTokenCount ());
              }
              aTable2 = new HashMap <> ();
              aTable2.put (sl.getImage (), sl);
              table[i].put (sl.getImage ().toUpperCase (Locale.US), aTable2);
            }
            else
              if (findIgnoreCase (aTable2, sl.getImage ()) != null)
              {
                // Since IGNORE_CASE version exists, current one is useless and
                // bad.
                final AbstractExpRegularExpression aOther = findIgnoreCase (aTable2, sl.getImage ());
                if (!sl.m_aTpContext.isExplicit ())
                {
                  // inline BNF string is used earlier with an IGNORE_CASE.
                  JavaCCErrors.semanticError (sl,
                                              "String \"" +
                                                  sl.getImage () +
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
                  JavaCCErrors.semanticError (sl,
                                              "Duplicate definition of string token \"" +
                                                  sl.getImage () +
                                                  "\" " +
                                                  "can never be matched.");
                }
              }
              else
                if (sl.m_aTpContext.isIgnoreCase ())
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
                    sl.setOrdinal (grammar.getAndIncTokenCount ());
                  }
                  aTable2.put (sl.getImage (), sl);
                  // The above "put" may override an existing entry (that is not
                  // IGNORE_CASE) and that's
                  // the desired behavior.
                }
                else
                {
                  // The rest of the cases do not involve IGNORE_CASE.
                  final AbstractExpRegularExpression aRe = aTable2.get (sl.getImage ());
                  if (aRe == null)
                  {
                    if (sl.getOrdinal () == 0)
                    {
                      sl.setOrdinal (grammar.getAndIncTokenCount ());
                    }
                    aTable2.put (sl.getImage (), sl);
                  }
                  else
                    if (tp.isExplicit ())
                    {
                      // This is an error even if the first occurrence was
                      // implicit.
                      if (tp.getLexStates ()[i].equals ("DEFAULT"))
                      {
                        JavaCCErrors.semanticError (sl,
                                                    "Duplicate definition of string token \"" + sl.getImage () + "\".");
                      }
                      else
                      {
                        JavaCCErrors.semanticError (sl,
                                                    "Duplicate definition of string token \"" +
                                                        sl.getImage () +
                                                        "\" in lexical state \"" +
                                                        tp.getLexStates ()[i] +
                                                        "\".");
                      }
                    }
                    else
                      if (aRe.m_aTpContext.getKind () != ETokenKind.TOKEN)
                      {
                        JavaCCErrors.semanticError (sl,
                                                    "String token \"" +
                                                        sl.getImage () +
                                                        "\" has been defined as a \"" +
                                                        aRe.m_aTpContext.getKind ().getImage () +
                                                        "\" token.");
                      }
                      else
                        if (aRe.m_bPrivateRexp)
                        {
                          JavaCCErrors.semanticError (sl,
                                                      "String token \"" +
                                                          sl.getImage () +
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
          if (!(aRes.getRexp () instanceof ExpRJustName))
          {
            aRes.getRexp ().setOrdinal (grammar.getAndIncTokenCount ());
          }
        if (!(aRes.getRexp () instanceof ExpRJustName) && aRes.getRexp ().hasLabel ())
        {
          grammar.namesOfTokens ().put (Integer.valueOf (aRes.getRexp ().getOrdinal ()), aRes.getRexp ().getLabel ());
        }
        if (!(aRes.getRexp () instanceof ExpRJustName))
        {
          grammar.rexpsOfTokens ().put (Integer.valueOf (aRes.getRexp ().getOrdinal ()), aRes.getRexp ());
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
        final List <RegExprSpec> aRespecs = aTp.getRespecs ();
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          aFrjn.m_aRoot = aRes.getRexp ();
          ExpansionTreeWalker.preOrderWalk (aRes.getRexp (), aFrjn);
          if (aRes.getRexp () instanceof ExpRJustName)
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
        final List <RegExprSpec> aRespecs = aTp.getRespecs ();
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          if (aRes.getRexp () instanceof final ExpRJustName jn)
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
        final List <RegExprSpec> aRespecs = aTp.getRespecs ();
        for (final RegExprSpec aRegExprSpec : aRespecs)
        {
          final RegExprSpec aRes = (aRegExprSpec);
          final Integer aIi = Integer.valueOf (aRes.getRexp ().getOrdinal ());
          if (grammar ().namesOfTokens ().get (aIi) == null)
          {
            JavaCCErrors.warning (aRes.getRexp (),
                                  "Unlabeled regular expression cannot be referred to by " +
                                                   "user generated token manager.");
          }
        }
      }
    }

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");

    // The following code sets the value of the "emptyPossible" field of
    // AbstractNormalProduction
    // nodes. This field is initialized to false, and then the entire list of
    // productions is processed. This is repeated as long as at least one item
    // got updated from false to true in the pass.
    boolean bEmptyUpdate = true;
    while (bEmptyUpdate)
    {
      bEmptyUpdate = false;
      for (final AbstractNormalProduction aNormalProduction : grammar ().bnfProductions ())
      {
        final AbstractNormalProduction aProd = aNormalProduction;
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
      for (final AbstractNormalProduction aNormalProduction : grammar ().bnfProductions ())
      {
        ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new EmptyChecker ());
      }

      // The following code goes through the productions and adds pointers to
      // other
      // productions that it can expand to without consuming any tokens. Once
      // this is
      // done, a left-recursion check can be performed.
      for (final AbstractNormalProduction prod : grammar ().bnfProductions ())
      {
        _addLeftMost (prod, prod.getExpansion ());
      }

      // Now the following loop calls a recursive walk routine that searches for
      // actual left recursions. The way the algorithm is coded, once a node has
      // been determined to participate in a left recursive loop, it is not
      // tried
      // in any other loop.
      for (final AbstractNormalProduction prod : grammar ().bnfProductions ())
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
          final List <RegExprSpec> aRespecs = aTp.getRespecs ();
          for (final RegExprSpec aRegExprSpec : aRespecs)
          {
            final RegExprSpec aRes = (aRegExprSpec);
            final AbstractExpRegularExpression aRexp = aRes.getRexp ();
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
                JavaCCErrors.semanticError (aRexp,
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
        for (final AbstractNormalProduction aNormalProduction : grammar ().bnfProductions ())
        {
          ExpansionTreeWalker.preOrderWalk (aNormalProduction.getExpansion (), new LookaheadChecker ());
        }
      }
      // matches "if (Options.getSanityCheck()) {"
    }

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ("Error count is already present!");
  }

  /**
   * Check whether "str" is superceded by another equal (except case) string in the table.
   *
   * @param aTable
   *        The string literals to search. May not be <code>null</code>.
   * @param sStr
   *        The image to check. May not be <code>null</code>.
   * @return The <code>IGNORE_CASE</code> regular expression that supercedes "str", or
   *         <code>null</code> if there is none. Used to be returned through a static field.
   */
  @Nullable
  public static AbstractExpRegularExpression findIgnoreCase (@NonNull final Map <String, AbstractExpRegularExpression> aTable,
                                                             final String sStr)
  {
    final AbstractExpRegularExpression aRexp = aTable.get (sStr);
    if (aRexp != null && !aRexp.m_aTpContext.isIgnoreCase ())
      return null;

    for (final AbstractExpRegularExpression aRegEx : aTable.values ())
      if (aRegEx.m_aTpContext.isIgnoreCase ())
        return aRegEx;
    return null;
  }

  // returns true if "exp" can expand to the empty string, returns false
  // otherwise.
  /**
   * Whether an expansion can match nothing at all, which is what makes a loop around it never
   * terminate.
   *
   * @param aExp
   *        The expansion. May not be <code>null</code>.
   * @return <code>true</code> if it can match the empty string.
   */
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
      return emptyExpansionExists (aTryBlock.getExp ());
    }

    // This should be dead code.
    return false;
  }

  // Updates prod.leftExpansions based on a walk of exp.
  static private void _addLeftMost (@NonNull final AbstractNormalProduction aProd, final Expansion aExp)
  {
    if (aExp instanceof final ExpNonTerminal aExpNonTerminal)
    {
      for (int i = 0; i < aProd.getLeIndex (); i++)
      {
        if (aProd.getLeftExpansions ()[i] == aExpNonTerminal.getProd ())
        {
          return;
        }
      }
      if (aProd.getLeIndex () == aProd.getLeftExpansions ().length)
      {
        final AbstractNormalProduction [] aNewle = new AbstractNormalProduction [aProd.getLeIndex () * 2];
        System.arraycopy (aProd.getLeftExpansions (), 0, aNewle, 0, aProd.getLeIndex ());
        aProd.setLeftExpansions (aNewle);
      }
      aProd.getLeftExpansions ()[aProd.getLeIndex ()] = aExpNonTerminal.getProd ();
      aProd.setLeIndex (aProd.getLeIndex () + 1);
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
                  _addLeftMost (aProd, aExpTryBlock.getExp ());
                }
  }

  // The string in which the following methods store information.

  // Returns true to indicate an unraveling of a detected left recursion loop,
  // and returns false otherwise.
  private static boolean _prodWalk (@NonNull final AbstractNormalProduction aProd)
  {
    aProd.setWalkStatus (-1);
    for (int i = 0; i < aProd.getLeIndex (); i++)
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
          JavaCCErrors.semanticError (aProd,
                                      "Left recursion detected: \"" +
                                             PGCCContext.current ().semanticize ().getLoopString () +
                                             "\"");
          return false;
        }
        aProd.setWalkStatus (1);
        return true;
      }
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
            JavaCCErrors.semanticError (aProd,
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
      if (jn.getRegexpr ().getWalkStatus () == -1)
      {
        jn.getRegexpr ().setWalkStatus (-2);
        PGCCContext.current ().semanticize ().setLoopString ("..." + jn.getRegexpr ().getLabel () + "...");
        // Note: Only the regexpr's of RJustName nodes and the top leve
        // regexpr's can have labels. Hence it is only in these cases that
        // the labels are checked for to be added to the loopString.
        return true;
      }
      if (jn.getRegexpr ().getWalkStatus () == 0)
      {
        jn.getRegexpr ().setWalkStatus (-1);
        if (_rexpWalk (jn.getRegexpr ()))
        {
          PGCCContext.current ()
                     .semanticize ()
                     .setLoopString ("..." +
                                     jn.getRegexpr ().getLabel () +
                                     "... --> " +
                                     PGCCContext.current ().semanticize ().getLoopString ());
          if (jn.getRegexpr ().getWalkStatus () == -2)
          {
            jn.getRegexpr ().setWalkStatus (1);
            JavaCCErrors.semanticError (jn.getRegexpr (),
                                        "Loop in regular expression detected: \"" +
                                                          PGCCContext.current ().semanticize ().getLoopString () +
                                                          "\"");
            return false;
          }
          jn.getRegexpr ().setWalkStatus (1);
          return true;
        }
        jn.getRegexpr ().setWalkStatus (1);
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
    private AbstractExpRegularExpression m_aRoot;

    /**
     * {@return the value of m_aRoot}
     */
    public AbstractExpRegularExpression getRoot ()
    {
      return m_aRoot;
    }

    /**
     * @param aValue
     *        The new value of m_aRoot.
     */
    public void setRoot (final AbstractExpRegularExpression aValue)
    {
      m_aRoot = aValue;
    }

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
          JavaCCErrors.semanticError (e, "Undefined lexical token name \"" + jn.getLabel () + "\".");
        }
        else
          if (jn == m_aRoot && !jn.m_aTpContext.isExplicit () && aRexp.m_bPrivateRexp)
          {
            JavaCCErrors.semanticError (e,
                                        "Token name \"" +
                                           jn.getLabel () +
                                           "\" refers to a private " +
                                           "(with a #) regular expression.");
          }
          else
            if (jn == m_aRoot && !jn.m_aTpContext.isExplicit () && aRexp.m_aTpContext.getKind () != ETokenKind.TOKEN)
            {
              JavaCCErrors.semanticError (e,
                                          "Token name \"" +
                                             jn.getLabel () +
                                             "\" refers to a non-token " +
                                             "(SKIP, MORE, IGNORE_IN_BNF) regular expression.");
            }
            else
            {
              jn.setOrdinal (aRexp.getOrdinal ());
              jn.setRegexpr (aRexp);
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

    public void action (@NonNull final Expansion e)
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
        final AbstractNormalProduction aNp = grammar ().productionTable ().get (nt.getName ());
        if (aNp == null)
        {
          JavaCCErrors.semanticError (e, "Non-terminal " + nt.getName () + " has not been defined.");
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
      if (e instanceof final ExpOneOrMore aOneOrMore)
      {
        if (Semanticize.emptyExpansionExists (aOneOrMore.getExpansion ()))
        {
          JavaCCErrors.semanticError (e, "Expansion within \"(...)+\" can be matched by empty string.");
        }
      }
      else
        if (e instanceof final ExpZeroOrMore aExpZeroOrMore)
        {
          if (Semanticize.emptyExpansionExists (aExpZeroOrMore.getExpansion ()))
          {
            JavaCCErrors.semanticError (e, "Expansion within \"(...)*\" can be matched by empty string.");
          }
        }
        else
          if (e instanceof final ExpZeroOrOne aExpZeroOrOne)
          {
            if (Semanticize.emptyExpansionExists (aExpZeroOrOne.getExpansion ()))
            {
              JavaCCErrors.semanticError (e, "Expansion within \"(...)?\" can be matched by empty string.");
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
      if (e instanceof final ExpChoice aChoice)
      {
        if (Options.getLookahead () == 1 || Options.isForceLaCheck ())
        {
          LookaheadCalc.choiceCalc (aChoice);
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
      if (!(aExp instanceof final ExpSequence aSeq))
        return true;

      final Object aObj = aSeq.getUnitAt (0);
      if (!(aObj instanceof final ExpLookahead aLa))
        return true;

      return !aLa.isExplicit ();
    }
  }

}
