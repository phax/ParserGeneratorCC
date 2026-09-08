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
package com.helger.pgcc.output;

import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.replaceBackslash;

import java.io.File;
import java.io.IOException;

import com.helger.base.io.nonblocking.NonBlockingBufferedReader;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.parser.Options;

  /**
   * Small helpers shared by the emitters of both languages.
   */
public class OutputHelper
{
  private OutputHelper ()
  {}

  /**
   * Read the version from the comment in the specified file. This method does not try to recover
   * from invalid comment syntax, but rather returns version 0.0 (which will always be taken to mean
   * the file is out of date). Works for Java and CPP.
   *
   * @param sFileName
   *        eg Token.java
   * @return The version as a double, eg 4.1
   * @since 4.1
   */
  public static double getVersionDashStar (final String sFileName)
  {
    final String sCommentHeader = "/* " + getIdString (CPG.APP_NAME, sFileName) + " Version ";
    final File aFile = new File (Options.getOutputDirectory (), replaceBackslash (sFileName));

    if (!aFile.exists ())
    {
      // Has not yet been created, so it must be up to date.
      try
      {
        final String sMajorVersion = PGVersion.VERSION_NUMBER.replaceAll ("[^0-9.]+.*", "");
        return Double.parseDouble (sMajorVersion);
      }
      catch (final NumberFormatException e)
      {
        // Should never happen
        return 0.0;
      }
    }

    try (final NonBlockingBufferedReader aReader = FileHelper.getBufferedReader (aFile, Options.getOutputEncoding ()))
    {
      String sStr;
      double dVersion = 0.0;

      // Although the version comment should be the first line, sometimes the
      // user might have put comments before it.
      while ((sStr = aReader.readLine ()) != null)
      {
        if (sStr.startsWith (sCommentHeader))
        {
          sStr = sStr.substring (sCommentHeader.length ());
          final int nPos = sStr.indexOf (' ');
          if (nPos >= 0)
            sStr = sStr.substring (0, nPos);
          if (sStr.length () > 0)
          {
            try
            {
              dVersion = Double.parseDouble (sStr);
            }
            catch (final NumberFormatException aNfe)
            {
              // Ignore - leave version as 0.0
            }
          }

          break;
        }
      }

      return dVersion;
    }
    catch (final IOException aIoe)
    {
      return 0.0;
    }
  }
}
