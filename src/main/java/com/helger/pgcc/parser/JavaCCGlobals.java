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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpAction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This package contains data created as a result of parsing and semanticizing a JavaCC input file.
 * This data is what is used by the back-ends of JavaCC as well as any other back-end of JavaCC
 * related tools such as JJTree.
 */
public final class JavaCCGlobals
{
  /**
   * The name of the grammar file being processed.
   */
  public static String s_fileName;

  /**
   * The name of the original file (before processing by JJTree). Currently this is the same as
   * fileName.
   */
  public static String s_origFileName;

  /**
   * Set to true if this file has been processed by JJTree.
   */
  public static boolean s_jjtreeGenerated;

  /**
   * The list of tools that have participated in generating the input grammar file.
   */
  public static List <String> s_toolNames;

  /**
   * This prints the banner line when the various tools are invoked. This takes as argument the
   * tool's full name and its version.
   *
   * @param fullName
   *        full application name
   * @param ver
   *        version string
   */
  public static void bannerLine (final String fullName, final String ver)
  {
    PGPrinter.info (CPG.APP_NAME +
                    " Version " +
                    PGVersion.VERSION_NUMBER +
                    " (" +
                    fullName +
                    (StringHelper.isNotEmpty (ver) ? " Version " + ver : "") +
                    ")");
  }

  /**
   * The name of the parser class (what appears in PARSER_BEGIN and PARSER_END).
   */
  public static String s_cu_name;

  /**
   * This is a list of tokens that appear after "PARSER_BEGIN(name)" all the way until (but not
   * including) the opening brace "{" of the class "name".
   */
  public static final ICommonsList <Token> CU_TO_INSERTION_POINT_1 = new CommonsArrayList <> ();

  /**
   * This is the list of all tokens that appear after the tokens in "cu_to_insertion_point_1" and
   * until (but not including) the closing brace "}" of the class "name".
   */
  public static final ICommonsList <Token> CU_TO_INSERTION_POINT_2 = new CommonsArrayList <> ();

  /**
   * This is the list of all tokens that appear after the tokens in "cu_to_insertion_point_2" and
   * until "PARSER_END(name)".
   */
  public static final ICommonsList <Token> CU_FROM_INSERTION_POINT_2 = new CommonsArrayList <> ();

  /**
   * A list of all grammar productions - normal and JAVACODE - in the order they appear in the input
   * file. Each entry here will be a subclass of "NormalProduction".
   */
  public static final List <NormalProduction> BNF_PRODUCTIONS = new ArrayList <> ();

  /**
   * A symbol table of all grammar productions - normal and JAVACODE. The symbol table is indexed by
   * the name of the left hand side non-terminal. Its contents are of type "NormalProduction".
   */
  public static final Map <String, NormalProduction> PRODUCTION_TABLE = new HashMap <> ();

  /**
   * A mapping of lexical state strings to their integer internal representation. Integers are
   * stored as java.lang.Integer's.
   */
  public static final Map <String, Integer> LEXSTATE_S2I = new HashMap <> ();

  /**
   * A mapping of the internal integer representations of lexical states to their strings. Integers
   * are stored as java.lang.Integer's.
   */
  public static final Map <Integer, String> LEXSTATE_I2S = new HashMap <> ();

  /**
   * The declarations to be inserted into the TokenManager class.
   */
  public static ICommonsList <Token> s_token_mgr_decls;

  /**
   * The list of all TokenProductions from the input file. This list includes implicit
   * TokenProductions that are created for uses of regular expressions within BNF productions.
   */
  public static final List <TokenProduction> REXPR_LIST = new ArrayList <> ();

  /**
   * The total number of distinct tokens. This is therefore one more than the largest assigned token
   * ordinal.
   */
  public static int s_tokenCount;

  /**
   * This is a symbol table that contains all named tokens (those that are defined with a label).
   * The index to the table is the image of the label and the contents of the table are of type
   * "RegularExpression".
   */
  public static final Map <String, AbstractExpRegularExpression> NAMED_TOKENS_TABLE = new HashMap <> ();

  /**
   * Contains the same entries as "named_tokens_table", but this is an ordered list which is ordered
   * by the order of appearance in the input file.
   */
  public static final List <AbstractExpRegularExpression> ORDERED_NAME_TOKENS = new ArrayList <> ();

  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to the corresponding
   * labels (of type "String"). An entry exists for an ordinal value only if there is a labeled
   * token corresponding to this entry. If there are multiple labels representing the same ordinal
   * value, then only one label is stored.
   */
  public static final Map <Integer, String> NAMES_OF_TOKENS = new HashMap <> ();

  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to the corresponding
   * RegularExpression's.
   */
  public static final Map <Integer, AbstractExpRegularExpression> REXPS_OF_TOKENS = new HashMap <> ();

  /**
   * This is a three-level symbol table that contains all simple tokens (those that are defined
   * using a single string (with or without a label). The index to the first level table is a
   * lexical state which maps to a second level hashtable. The index to the second level hashtable
   * is the string of the simple token converted to upper case, and this maps to a third level
   * hashtable. This third level hashtable contains the actual string of the simple token and maps
   * it to its RegularExpression.
   */
  public static final Map <String, Map <String, Map <String, AbstractExpRegularExpression>>> SIMPLE_TOKENS_TABLE = new HashMap <> ();

  /**
   * maskindex, jj2index, maskVals are variables that are shared between ParseEngine and ParseGen.
   */
  protected static int s_maskindex = 0;
  protected static int s_jj2index = 0;
  private static boolean s_bLookAheadNeeded = false;
  protected static final List <int []> MASK_VALS = new ArrayList <> ();

  static ExpAction s_aActForEof;
  static String s_sNextStateForEof;
  private static Token s_aOtherLanguageDeclTokenBeg;
  private static Token s_aOtherLanguageDeclTokenEnd;

  // Some general purpose utilities follow.

  public static boolean isLookAheadNeeded ()
  {
    return s_bLookAheadNeeded;
  }

  public static void setLookAheadNeeded (final boolean bLookAheadNeeded)
  {
    s_bLookAheadNeeded = bLookAheadNeeded;
  }

  @Nullable
  public static Token getOtherLanguageDeclTokenBegin ()
  {
    return s_aOtherLanguageDeclTokenBeg;
  }

  public static void setOtherLanguageDeclTokenBegin (@Nullable final Token t)
  {
    s_aOtherLanguageDeclTokenBeg = t;
  }

  @Nullable
  public static Token getOtherLanguageDeclTokenEnd ()
  {
    return s_aOtherLanguageDeclTokenEnd;
  }

  public static void setOtherLanguageDeclTokenEnd (@Nullable final Token t)
  {
    s_aOtherLanguageDeclTokenEnd = t;
  }

  /**
   * Returns the identifying string for the file name, given a toolname used to generate it.
   *
   * @param toolName
   *        tool name
   * @param fileName
   *        file name
   * @return ID string
   */
  public static String getIdString (final String toolName, final String fileName)
  {
    return getIdString (new CommonsArrayList <> (toolName), fileName);
  }

  /**
   * Returns the identifying string for the file name, given a set of tool names that are used to
   * generate it. Total name may not exceed 200 characters.
   *
   * @param toolNames
   *        tool names
   * @param fileName
   *        file names
   * @return ID string
   */
  public static String getIdString (final List <String> toolNames, final String fileName)
  {
    final String toolNamePrefix = "Generated by: " +
                                  StringImplode.imploder ().source (toolNames).separator ('&').build () +
                                  ":";

    if (toolNamePrefix.length () > 200)
    {
      PGPrinter.error ("Tool names too long.");
      throw new IllegalStateException ("Tool names too long: " + toolNamePrefix);
    }

    return toolNamePrefix + " Do not edit this line. " + addUnicodeEscapes (fileName);
  }

  /**
   * @param toolName
   *        tool name
   * @param fileName
   *        file name
   * @return <code>true</code> if tool name passed is one of the tool names returned by
   *         getToolNames(fileName).
   */
  public static boolean isGeneratedBy (final String toolName, final String fileName)
  {
    final List <String> v = getToolNames (fileName);

    for (final String element : v)
      if (toolName.equals (element))
        return true;

    return false;
  }

  private static List <String> _makeToolNameList (final String str)
  {
    final List <String> retVal = new ArrayList <> ();

    int limit1 = str.indexOf ('\n');
    if (limit1 == -1)
      limit1 = 1000;
    int limit2 = str.indexOf ('\r');
    if (limit2 == -1)
      limit2 = 1000;
    final int limit = (limit1 < limit2) ? limit1 : limit2;

    String tmp;
    if (limit == 1000)
    {
      tmp = str;
    }
    else
    {
      tmp = str.substring (0, limit);
    }

    if (tmp.indexOf (':') == -1)
      return retVal;

    tmp = tmp.substring (tmp.indexOf (':') + 1);

    if (tmp.indexOf (':') == -1)
      return retVal;

    tmp = tmp.substring (0, tmp.indexOf (':'));

    int i = 0, j = 0;

    while (j < tmp.length () && (i = tmp.indexOf ('&', j)) != -1)
    {
      retVal.add (tmp.substring (j, i));
      j = i + 1;
    }

    if (j < tmp.length ())
      retVal.add (tmp.substring (j));

    return retVal;
  }

  /**
   * Returns a List of names of the tools that have been used to generate the given file.
   *
   * @param fileName
   *        file name
   * @return tool names
   */
  @Nonnull
  public static List <String> getToolNames (final String fileName)
  {
    final Charset aCS = Options.getOutputEncoding ();
    final char [] buf = new char [256];
    int total = 0;

    try (final Reader stream = FileHelper.getBufferedReader (new File (fileName), aCS))
    {
      int read;
      while (true)
      {
        read = stream.read (buf, total, buf.length - total);
        if (read != -1)
        {
          total += read;
          if (total == buf.length)
            break;
        }
        else
          break;
      }

      return _makeToolNameList (new String (buf, 0, total));
    }
    catch (final FileNotFoundException e1)
    {}
    catch (final IOException e2)
    {
      if (total > 0)
        return _makeToolNameList (new String (buf, 0, total));
    }

    return new ArrayList <> ();
  }

  public static void createOutputDir (final File outputDir)
  {
    if (!outputDir.exists ())
    {
      JavaCCErrors.warning ("Output directory \"" + outputDir + "\" does not exist. Creating the directory.");

      if (!outputDir.mkdirs ())
      {
        JavaCCErrors.semantic_error ("Cannot create the output directory : " + outputDir);
        return;
      }
    }

    if (!outputDir.isDirectory ())
    {
      JavaCCErrors.semantic_error ("\"" + outputDir + " is not a valid output directory.");
      return;
    }

    if (!outputDir.canWrite ())
    {
      JavaCCErrors.semantic_error ("Cannot write to the output output directory : \"" + outputDir + "\"");
    }
  }

  @Nonnull
  public static String addEscapes (@Nonnull final String str)
  {
    final StringBuilder retval = new StringBuilder (str.length () * 2);
    for (final char ch : str.toCharArray ())
    {
      if (ch == '\b')
      {
        retval.append ("\\b");
      }
      else
        if (ch == '\t')
        {
          retval.append ("\\t");
        }
        else
          if (ch == '\n')
          {
            retval.append ("\\n");
          }
          else
            if (ch == '\f')
            {
              retval.append ("\\f");
            }
            else
              if (ch == '\r')
              {
                retval.append ("\\r");
              }
              else
                if (ch == '\"')
                {
                  retval.append ("\\\"");
                }
                else
                  if (ch == '\'')
                  {
                    retval.append ("\\\'");
                  }
                  else
                    if (ch == '\\')
                    {
                      retval.append ("\\\\");
                    }
                    else
                      if (ch < 0x20 || ch > 0x7e)
                      {
                        final String s = "0000" + Integer.toString (ch, 16);
                        retval.append ("\\u").append (s.substring (s.length () - 4));
                      }
                      else
                      {
                        retval.append (ch);
                      }
    }
    return retval.toString ();
  }

  public static String addUnicodeEscapes (final String str)
  {
    switch (Options.getOutputLanguage ())
    {
      case JAVA:
      {
        final StringBuilder retval = new StringBuilder (str.length () * 2);
        for (final char ch : str.toCharArray ())
        {
          if (ch < 0x20 || ch > 0x7e /* || ch == '\\' -- cba commented out 20140305 */ )
          {
            final String s = "0000" + Integer.toString (ch, 16);
            retval.append ("\\u").append (s.substring (s.length () - 4));
          }
          else
          {
            retval.append (ch);
          }
        }
        return retval.toString ();
      }
      case CPP:
        return str;
      default:
        throw new UnsupportedOutputLanguageException (Options.getOutputLanguage ());
    }
  }

  public static int s_cline;
  public static int s_ccol;

  public static void printTokenSetup (final Token t)
  {
    Token tt = t;
    while (tt.specialToken != null)
      tt = tt.specialToken;
    s_cline = tt.beginLine;
    s_ccol = tt.beginColumn;
  }

  protected static void printTokenOnly (@Nonnull final Token t, @Nonnull final PrintWriter ostr)
  {
    for (; s_cline < t.beginLine; s_cline++)
    {
      ostr.println ();
      s_ccol = 1;
    }
    for (; s_ccol < t.beginColumn; s_ccol++)
    {
      ostr.print (" ");
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      ostr.print (addUnicodeEscapes (t.image));
    else
      ostr.print (t.image);
    s_cline = t.endLine;
    s_ccol = t.endColumn + 1;
    final char last = t.image.charAt (t.image.length () - 1);
    if (last == '\n' || last == '\r')
    {
      s_cline++;
      s_ccol = 1;
    }
  }

  public static void printToken (@Nonnull final Token t, @Nonnull final PrintWriter ostr)
  {
    Token tt = t.specialToken;
    if (tt != null)
    {
      while (tt.specialToken != null)
        tt = tt.specialToken;
      while (tt != null)
      {
        printTokenOnly (tt, ostr);
        tt = tt.next;
      }
    }
    printTokenOnly (t, ostr);
  }

  protected static void printTokenList (@Nonnull final List <Token> list, @Nonnull final PrintWriter ostr)
  {
    Token t = null;
    for (final Iterator <Token> it = list.iterator (); it.hasNext ();)
    {
      t = it.next ();
      printToken (t, ostr);
    }

    if (t != null)
      printTrailingComments (t);
  }

  protected static void printLeadingComments (@Nonnull final Token t, @Nonnull final PrintWriter ostr)
  {
    if (t.specialToken == null)
      return;
    Token tt = t.specialToken;
    while (tt.specialToken != null)
      tt = tt.specialToken;
    while (tt != null)
    {
      printTokenOnly (tt, ostr);
      tt = tt.next;
    }
    if (s_ccol != 1 && s_cline != t.beginLine)
    {
      ostr.println ();
      s_cline++;
      s_ccol = 1;
    }
  }

  @Nonnull
  public static String printTokenOnly (@Nonnull final Token t)
  {
    final StringBuilder aSB = new StringBuilder (t.image.length () * 2);
    for (; s_cline < t.beginLine; s_cline++)
    {
      aSB.append ('\n');
      s_ccol = 1;
    }
    for (; s_ccol < t.beginColumn; s_ccol++)
    {
      aSB.append (' ');
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      aSB.append (addUnicodeEscapes (t.image));
    else
      aSB.append (t.image);
    s_cline = t.endLine;
    s_ccol = t.endColumn + 1;
    final char last = t.image.charAt (t.image.length () - 1);
    if (last == '\n' || last == '\r')
    {
      s_cline++;
      s_ccol = 1;
    }
    return aSB.toString ();
  }

  @Nonnull
  public static String printToken (@Nonnull final Token t)
  {
    final StringBuilder aSB = new StringBuilder ();
    Token tt = t.specialToken;
    if (tt != null)
    {
      while (tt.specialToken != null)
        tt = tt.specialToken;
      while (tt != null)
      {
        aSB.append (printTokenOnly (tt));
        tt = tt.next;
      }
    }
    aSB.append (printTokenOnly (t));
    return aSB.toString ();
  }

  @Nonnull
  protected static String printLeadingComments (@Nonnull final Token t)
  {
    if (t.specialToken == null)
      return "";

    final StringBuilder aSB = new StringBuilder ();
    Token tt = t.specialToken;
    while (tt.specialToken != null)
      tt = tt.specialToken;
    while (tt != null)
    {
      aSB.append (printTokenOnly (tt));
      tt = tt.next;
    }
    if (s_ccol != 1 && s_cline != t.beginLine)
    {
      aSB.append ('\n');
      s_cline++;
      s_ccol = 1;
    }
    return aSB.toString ();
  }

  @Nonnull
  public static String printTrailingComments (@Nonnull final Token t)
  {
    if (t.next == null)
      return "";
    return printLeadingComments (t.next);
  }

  public static void reInitStatic ()
  {
    s_fileName = null;
    s_origFileName = null;
    s_jjtreeGenerated = false;
    s_toolNames = null;
    s_cu_name = null;
    CU_TO_INSERTION_POINT_1.clear ();
    CU_TO_INSERTION_POINT_2.clear ();
    CU_FROM_INSERTION_POINT_2.clear ();
    BNF_PRODUCTIONS.clear ();
    PRODUCTION_TABLE.clear ();
    LEXSTATE_S2I.clear ();
    LEXSTATE_I2S.clear ();
    s_token_mgr_decls = null;
    REXPR_LIST.clear ();
    s_tokenCount = 0;
    NAMED_TOKENS_TABLE.clear ();
    ORDERED_NAME_TOKENS.clear ();
    NAMES_OF_TOKENS.clear ();
    REXPS_OF_TOKENS.clear ();
    SIMPLE_TOKENS_TABLE.clear ();
    s_maskindex = 0;
    s_jj2index = 0;
    s_bLookAheadNeeded = false;
    MASK_VALS.clear ();
    s_cline = 0;
    s_ccol = 0;
    s_aActForEof = null;
    s_sNextStateForEof = null;
  }

  @Nonnull
  @Nonempty
  static String getFileExtension ()
  {
    switch (Options.getOutputLanguage ())
    {
      case JAVA:
        return ".java";
      case CPP:
        return ".cc";
      default:
        throw new UnsupportedOutputLanguageException (Options.getOutputLanguage ());
    }
  }

  /**
   * Replaces all backslashes with double backslashes.
   *
   * @param str
   *        source string
   * @return result string
   */
  public static String replaceBackslash (@Nonnull final String str)
  {
    if (str.indexOf ('\\') < 0)
    {
      // No backslash found.
      return str;
    }

    final StringBuilder aSB = new StringBuilder (str.length () * 2);
    for (final char c : str.toCharArray ())
      if (c == '\\')
        aSB.append ("\\\\");
      else
        aSB.append (c);
    return aSB.toString ();
  }
}
