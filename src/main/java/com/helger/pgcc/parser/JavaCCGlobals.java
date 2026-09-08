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

import com.helger.pgcc.context.GrammarState;
import com.helger.pgcc.context.PGCCContext;

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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

/**
 * This package contains data created as a result of parsing and semanticizing a JavaCC input file.
 * This data is what is used by the back-ends of JavaCC as well as any other back-end of JavaCC
 * related tools such as JJTree.
 */
public final class JavaCCGlobals
{
  /**
   * @return The grammar that is being processed by the current run. Never <code>null</code>. This
   *         replaces the pile of static fields this class used to be.
   */
  @NonNull
  public static GrammarState grammar ()
  {
    return PGCCContext.current ().grammar ();
  }

  public static boolean isLookAheadNeeded ()
  {
    return grammar ().isLookAheadNeeded ();
  }

  public static void setLookAheadNeeded (final boolean bLookAheadNeeded)
  {
    grammar ().setLookAheadNeeded (bLookAheadNeeded);
  }

  @Nullable
  public static Token getOtherLanguageDeclTokenBegin ()
  {
    return grammar ().getOtherLanguageDeclTokenBegin ();
  }

  public static void setOtherLanguageDeclTokenBegin (@Nullable final Token t)
  {
    grammar ().setOtherLanguageDeclTokenBegin (t);
  }

  @Nullable
  public static Token getOtherLanguageDeclTokenEnd ()
  {
    return grammar ().getOtherLanguageDeclTokenEnd ();
  }

  public static void setOtherLanguageDeclTokenEnd (@Nullable final Token t)
  {
    grammar ().setOtherLanguageDeclTokenEnd (t);
  }





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

  // Some general purpose utilities follow.







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

    // Note: the tool names are surrounded by blanks in the ID string (see
    // getIdString) and must therefore be trimmed here
    while (j < tmp.length () && (i = tmp.indexOf ('&', j)) != -1)
    {
      retVal.add (tmp.substring (j, i).trim ());
      j = i + 1;
    }

    if (j < tmp.length ())
      retVal.add (tmp.substring (j).trim ());

    return retVal;
  }

  /**
   * Returns a List of names of the tools that have been used to generate the given file.
   *
   * @param fileName
   *        file name
   * @return tool names
   */
  @NonNull
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

  @NonNull
  public static String addEscapes (@NonNull final String str)
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

  public static void printTokenSetup (final Token t)
  {
    Token tt = t;
    while (tt.specialToken != null)
      tt = tt.specialToken;
    grammar ().setCurrentLine (tt.beginLine);
    grammar ().setCurrentColumn (tt.beginColumn);
  }

  protected static void printTokenOnly (@NonNull final Token t, @NonNull final PrintWriter ostr)
  {
    for (; grammar ().getCurrentLine () < t.beginLine; grammar ().setCurrentLine (grammar ().getCurrentLine () + 1))
    {
      ostr.println ();
      grammar ().setCurrentColumn (1);
    }
    for (; grammar ().getCurrentColumn () < t.beginColumn; grammar ().setCurrentColumn (grammar ().getCurrentColumn () + 1))
    {
      ostr.print (" ");
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      ostr.print (addUnicodeEscapes (t.image));
    else
      ostr.print (t.image);
    grammar ().setCurrentLine (t.endLine);
    grammar ().setCurrentColumn (t.endColumn + 1);
    final char last = t.image.charAt (t.image.length () - 1);
    if (last == '\n' || last == '\r')
    {
      grammar ().setCurrentLine (grammar ().getCurrentLine () + 1);
      grammar ().setCurrentColumn (1);
    }
  }

  public static void printToken (@NonNull final Token t, @NonNull final PrintWriter ostr)
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

  protected static void printTokenList (@NonNull final List <Token> list, @NonNull final PrintWriter ostr)
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

  protected static void printLeadingComments (@NonNull final Token t, @NonNull final PrintWriter ostr)
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
    if (grammar ().getCurrentColumn () != 1 && grammar ().getCurrentLine () != t.beginLine)
    {
      ostr.println ();
      grammar ().setCurrentLine (grammar ().getCurrentLine () + 1);
      grammar ().setCurrentColumn (1);
    }
  }

  @NonNull
  public static String printTokenOnly (@NonNull final Token t)
  {
    final StringBuilder aSB = new StringBuilder (t.image.length () * 2);
    for (; grammar ().getCurrentLine () < t.beginLine; grammar ().setCurrentLine (grammar ().getCurrentLine () + 1))
    {
      aSB.append ('\n');
      grammar ().setCurrentColumn (1);
    }
    for (; grammar ().getCurrentColumn () < t.beginColumn; grammar ().setCurrentColumn (grammar ().getCurrentColumn () + 1))
    {
      aSB.append (' ');
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      aSB.append (addUnicodeEscapes (t.image));
    else
      aSB.append (t.image);
    grammar ().setCurrentLine (t.endLine);
    grammar ().setCurrentColumn (t.endColumn + 1);
    final char last = t.image.charAt (t.image.length () - 1);
    if (last == '\n' || last == '\r')
    {
      grammar ().setCurrentLine (grammar ().getCurrentLine () + 1);
      grammar ().setCurrentColumn (1);
    }
    return aSB.toString ();
  }

  @NonNull
  public static String printToken (@NonNull final Token t)
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

  @NonNull
  protected static String printLeadingComments (@NonNull final Token t)
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
    if (grammar ().getCurrentColumn () != 1 && grammar ().getCurrentLine () != t.beginLine)
    {
      aSB.append ('\n');
      grammar ().setCurrentLine (grammar ().getCurrentLine () + 1);
      grammar ().setCurrentColumn (1);
    }
    return aSB.toString ();
  }

  @NonNull
  public static String printTrailingComments (@NonNull final Token t)
  {
    if (t.next == null)
      return "";
    return printLeadingComments (t.next);
  }

  @NonNull
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
  public static String replaceBackslash (@NonNull final String str)
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
