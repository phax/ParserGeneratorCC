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

public class OutputHelper
{
  private OutputHelper ()
  {}

  /**
   * Read the version from the comment in the specified file. This method does
   * not try to recover from invalid comment syntax, but rather returns version
   * 0.0 (which will always be taken to mean the file is out of date). Works for
   * Java and CPP.
   *
   * @param fileName
   *        eg Token.java
   * @return The version as a double, eg 4.1
   * @since 4.1
   */
  public static double getVersionDashStar (final String fileName)
  {
    final String commentHeader = "/* " + getIdString (CPG.APP_NAME, fileName) + " Version ";
    final File file = new File (Options.getOutputDirectory (), replaceBackslash (fileName));

    if (!file.exists ())
    {
      // Has not yet been created, so it must be up to date.
      try
      {
        final String majorVersion = PGVersion.VERSION_NUMBER.replaceAll ("[^0-9.]+.*", "");
        return Double.parseDouble (majorVersion);
      }
      catch (final NumberFormatException e)
      {
        return 0.0; // Should never happen
      }
    }

    try (final NonBlockingBufferedReader reader = FileHelper.getBufferedReader (file, Options.getOutputEncoding ()))
    {
      String str;
      double version = 0.0;

      // Although the version comment should be the first line, sometimes the
      // user might have put comments before it.
      while ((str = reader.readLine ()) != null)
      {
        if (str.startsWith (commentHeader))
        {
          str = str.substring (commentHeader.length ());
          final int pos = str.indexOf (' ');
          if (pos >= 0)
            str = str.substring (0, pos);
          if (str.length () > 0)
          {
            try
            {
              version = Double.parseDouble (str);
            }
            catch (final NumberFormatException nfe)
            {
              // Ignore - leave version as 0.0
            }
          }

          break;
        }
      }

      return version;
    }
    catch (final IOException ioe)
    {
      return 0.0;
    }
  }
}
