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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.context.GrammarState;
import com.helger.pgcc.context.PGCCContext;

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
   * @param sFullName
   *        full application name
   * @param sVer
   *        version string
   */
  public static void bannerLine (final String sFullName, final String sVer)
  {
    PGPrinter.info (CPG.APP_NAME +
                    " Version " +
                    PGVersion.VERSION_NUMBER +
                    " (" +
                    sFullName +
                    (StringHelper.isNotEmpty (sVer) ? " Version " + sVer : "") +
                    ")");
  }

  // Some general purpose utilities follow.

  /**
   * Returns the identifying string for the file name, given a toolname used to generate it.
   *
   * @param sToolName
   *        tool name
   * @param sFileName
   *        file name
   * @return ID string
   */
  public static String getIdString (final String sToolName, final String sFileName)
  {
    return getIdString (new CommonsArrayList <> (sToolName), sFileName);
  }

  /**
   * Returns the identifying string for the file name, given a set of tool names that are used to
   * generate it. Total name may not exceed 200 characters.
   *
   * @param aToolNames
   *        tool names
   * @param sFileName
   *        file names
   * @return ID string
   */
  public static String getIdString (final List <String> aToolNames, final String sFileName)
  {
    final String sToolNamePrefix = "Generated by: " +
                                   StringImplode.imploder ().source (aToolNames).separator ('&').build () +
                                   ":";

    if (sToolNamePrefix.length () > 200)
    {
      PGPrinter.error ("Tool names too long.");
      throw new IllegalStateException ("Tool names too long: " + sToolNamePrefix);
    }

    return sToolNamePrefix + " Do not edit this line. " + addUnicodeEscapes (sFileName);
  }

  /**
   * @param sToolName
   *        tool name
   * @param sFileName
   *        file name
   * @return <code>true</code> if tool name passed is one of the tool names returned by
   *         getToolNames(fileName).
   */
  public static boolean isGeneratedBy (final String sToolName, final String sFileName)
  {
    final List <String> v = getToolNames (sFileName);

    for (final String element : v)
      if (sToolName.equals (element))
        return true;

    return false;
  }

  private static List <String> _makeToolNameList (final String sStr)
  {
    final List <String> aRetVal = new ArrayList <> ();

    int nLimit1 = sStr.indexOf ('\n');
    if (nLimit1 == -1)
      nLimit1 = 1000;
    int nLimit2 = sStr.indexOf ('\r');
    if (nLimit2 == -1)
      nLimit2 = 1000;
    final int nLimit = (nLimit1 < nLimit2) ? nLimit1 : nLimit2;

    String sTmp;
    if (nLimit == 1000)
    {
      sTmp = sStr;
    }
    else
    {
      sTmp = sStr.substring (0, nLimit);
    }

    if (sTmp.indexOf (':') == -1)
      return aRetVal;

    sTmp = sTmp.substring (sTmp.indexOf (':') + 1);

    if (sTmp.indexOf (':') == -1)
      return aRetVal;

    sTmp = sTmp.substring (0, sTmp.indexOf (':'));

    int i = 0, j = 0;

    // Note: the tool names are surrounded by blanks in the ID string (see
    // getIdString) and must therefore be trimmed here
    while (j < sTmp.length () && (i = sTmp.indexOf ('&', j)) != -1)
    {
      aRetVal.add (sTmp.substring (j, i).trim ());
      j = i + 1;
    }

    if (j < sTmp.length ())
      aRetVal.add (sTmp.substring (j).trim ());

    return aRetVal;
  }

  /**
   * Returns a List of names of the tools that have been used to generate the given file.
   *
   * @param sFileName
   *        file name
   * @return tool names
   */
  @NonNull
  public static List <String> getToolNames (final String sFileName)
  {
    final Charset aCS = Options.getOutputEncoding ();
    final char [] aBuf = new char [256];
    int nTotal = 0;

    try (final Reader aStream = FileHelper.getBufferedReader (new File (sFileName), aCS))
    {
      if (aStream == null)
      {
        // The file does not exist or cannot be read - it was generated by nobody. FileHelper
        // returns null instead of throwing FileNotFoundException.
        return new ArrayList <> ();
      }

      int nRead;
      while (true)
      {
        nRead = aStream.read (aBuf, nTotal, aBuf.length - nTotal);
        if (nRead == -1)
          break;
        nTotal += nRead;
        if (nTotal == aBuf.length)
          break;
      }

      return _makeToolNameList (new String (aBuf, 0, nTotal));
    }
    catch (final FileNotFoundException e1)
    {}
    catch (final IOException e2)
    {
      if (nTotal > 0)
        return _makeToolNameList (new String (aBuf, 0, nTotal));
    }

    return new ArrayList <> ();
  }

  public static void createOutputDir (final File aOutputDir)
  {
    if (!aOutputDir.exists ())
    {
      JavaCCErrors.warning ("Output directory \"" + aOutputDir + "\" does not exist. Creating the directory.");

      if (!aOutputDir.mkdirs ())
      {
        JavaCCErrors.semantic_error ("Cannot create the output directory : " + aOutputDir);
        return;
      }
    }

    if (!aOutputDir.isDirectory ())
    {
      JavaCCErrors.semantic_error ("\"" + aOutputDir + " is not a valid output directory.");
      return;
    }

    if (!aOutputDir.canWrite ())
    {
      JavaCCErrors.semantic_error ("Cannot write to the output output directory : \"" + aOutputDir + "\"");
    }
  }

  @NonNull
  public static String addEscapes (@NonNull final String sStr)
  {
    final StringBuilder aRetVal = new StringBuilder (sStr.length () * 2);
    for (final char ch : sStr.toCharArray ())
      switch (ch)
      {
        case '\b' -> aRetVal.append ("\\b");
        case '\t' -> aRetVal.append ("\\t");
        case '\n' -> aRetVal.append ("\\n");
        case '\f' -> aRetVal.append ("\\f");
        case '\r' -> aRetVal.append ("\\r");
        case '"' -> aRetVal.append ("\\\"");
        case '\'' -> aRetVal.append ("\\'");
        case '\\' -> aRetVal.append ("\\\\");
        default ->
        {
          if (ch < 0x20 || ch > 0x7e)
          {
            final String s = "0000" + Integer.toString (ch, 16);
            aRetVal.append ("\\u").append (s.substring (s.length () - 4));
          }
          else
            aRetVal.append (ch);
        }
      }
    return aRetVal.toString ();
  }

  public static String addUnicodeEscapes (final String sStr)
  {
    return Options.getOutputLanguage ().addUnicodeEscapes (sStr);
  }

  public static void printTokenSetup (final Token t)
  {
    Token aTt = t;
    while (aTt.specialToken != null)
      aTt = aTt.specialToken;
    grammar ().setCurrentLine (aTt.beginLine);
    grammar ().setCurrentColumn (aTt.beginColumn);
  }

  protected static void printTokenOnly (@NonNull final Token t, @NonNull final PrintWriter aOstr)
  {
    for (; grammar ().getCurrentLine () < t.beginLine; grammar ().incCurrentLine ())
    {
      aOstr.println ();
      grammar ().setCurrentColumn (1);
    }
    for (; grammar ().getCurrentColumn () < t.beginColumn; grammar ().incCurrentColumn ())
    {
      aOstr.print (" ");
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      aOstr.print (addUnicodeEscapes (t.image));
    else
      aOstr.print (t.image);
    grammar ().setCurrentLine (t.endLine);
    grammar ().setCurrentColumn (t.endColumn + 1);
    final char cLast = t.image.charAt (t.image.length () - 1);
    if (cLast == '\n' || cLast == '\r')
    {
      grammar ().incCurrentLine ();
      grammar ().setCurrentColumn (1);
    }
  }

  public static void printToken (@NonNull final Token t, @NonNull final PrintWriter aOstr)
  {
    Token aTt = t.specialToken;
    if (aTt != null)
    {
      while (aTt.specialToken != null)
        aTt = aTt.specialToken;
      while (aTt != null)
      {
        printTokenOnly (aTt, aOstr);
        aTt = aTt.next;
      }
    }
    printTokenOnly (t, aOstr);
  }

  protected static void printTokenList (@NonNull final List <Token> aList, @NonNull final PrintWriter aOstr)
  {
    Token t = null;
    for (final Iterator <Token> aIt = aList.iterator (); aIt.hasNext ();)
    {
      t = aIt.next ();
      printToken (t, aOstr);
    }

    if (t != null)
      printTrailingComments (t);
  }

  protected static void printLeadingComments (@NonNull final Token t, @NonNull final PrintWriter aOstr)
  {
    if (t.specialToken == null)
      return;
    Token aTt = t.specialToken;
    while (aTt.specialToken != null)
      aTt = aTt.specialToken;
    while (aTt != null)
    {
      printTokenOnly (aTt, aOstr);
      aTt = aTt.next;
    }
    if (grammar ().getCurrentColumn () != 1 && grammar ().getCurrentLine () != t.beginLine)
    {
      aOstr.println ();
      grammar ().incCurrentLine ();
      grammar ().setCurrentColumn (1);
    }
  }

  @NonNull
  public static String printTokenOnly (@NonNull final Token t)
  {
    final StringBuilder aSB = new StringBuilder (t.image.length () * 2);
    for (; grammar ().getCurrentLine () < t.beginLine; grammar ().incCurrentLine ())
    {
      aSB.append ('\n');
      grammar ().setCurrentColumn (1);
    }
    for (; grammar ().getCurrentColumn () < t.beginColumn; grammar ().incCurrentColumn ())
    {
      aSB.append (' ');
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      aSB.append (addUnicodeEscapes (t.image));
    else
      aSB.append (t.image);
    grammar ().setCurrentLine (t.endLine);
    grammar ().setCurrentColumn (t.endColumn + 1);
    final char cLast = t.image.charAt (t.image.length () - 1);
    if (cLast == '\n' || cLast == '\r')
    {
      grammar ().incCurrentLine ();
      grammar ().setCurrentColumn (1);
    }
    return aSB.toString ();
  }

  @NonNull
  public static String printToken (@NonNull final Token t)
  {
    final StringBuilder aSB = new StringBuilder ();
    Token aTt = t.specialToken;
    if (aTt != null)
    {
      while (aTt.specialToken != null)
        aTt = aTt.specialToken;
      while (aTt != null)
      {
        aSB.append (printTokenOnly (aTt));
        aTt = aTt.next;
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
    Token aTt = t.specialToken;
    while (aTt.specialToken != null)
      aTt = aTt.specialToken;
    while (aTt != null)
    {
      aSB.append (printTokenOnly (aTt));
      aTt = aTt.next;
    }
    if (grammar ().getCurrentColumn () != 1 && grammar ().getCurrentLine () != t.beginLine)
    {
      aSB.append ('\n');
      grammar ().incCurrentLine ();
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
  /**
   * @return The file extension of the generated source files for the configured output language.
   *         Never <code>null</code>. Public since the emitters moved into their own packages.
   */
  public static String getFileExtension ()
  {
    return Options.getOutputLanguage ().getFileExtension ();
  }

  /**
   * Replaces all backslashes with double backslashes.
   *
   * @param sStr
   *        source string
   * @return result string
   */
  public static String replaceBackslash (@NonNull final String sStr)
  {
    if (sStr.indexOf ('\\') < 0)
    {
      // No backslash found.
      return sStr;
    }

    final StringBuilder aSB = new StringBuilder (sStr.length () * 2);
    for (final char c : sStr.toCharArray ())
      if (c == '\\')
        aSB.append ("\\\\");
      else
        aSB.append (c);
    return aSB.toString ();
  }
}
