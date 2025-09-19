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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.helger.annotation.WillCloseWhenClosed;
import com.helger.base.io.nonblocking.NonBlockingBufferedReader;
import com.helger.base.io.stream.NullOutputStream;
import com.helger.base.string.StringHex;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.CPG;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.Options;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;

import jakarta.annotation.Nonnull;

/**
 * This class handles the creation and maintenance of the boiler-plate classes, such as Token.java,
 * JavaCharStream.java etc. It is responsible for:
 * <ul>
 * <li>Writing the JavaCC header lines to the file.</li>
 * <li>Writing the checksum line.</li>
 * <li>Using the checksum to determine if an existing file has been changed by the user (and so
 * should be left alone).</li>
 * <li>Checking any existing file's version (if the file can not be overwritten).</li>
 * <li>Checking any existing file's creation options (if the file can not be overwritten).</li>
 * <li></li>
 * </ul>
 *
 * @author Paul Cager
 */
public class OutputFile implements AutoCloseable
{
  private static final String OPTIONS_PREFIX = CPG.APP_NAME + "Options";
  private static final String MD5_LINE_PART_1 = "/* " + CPG.APP_NAME + " - OriginalChecksum=";
  private static final String MD5_LINE_PART_1Q = "/\\* " + CPG.APP_NAME + " - OriginalChecksum=";
  private static final String MD5_LINE_PART_2 = " (do not edit this line) */";
  private static final String MD5_LINE_PART_2Q = " \\(do not edit this line\\) \\*/";

  private TrapClosePrintWriter m_aPW;
  private DigestOutputStream m_dos;
  private String m_sToolName = CPG.APP_NAME;
  private final File m_aFile;
  private final String m_sCompatibleVersion;
  private final String [] m_aOptions;
  private boolean m_bNeedToWrite = true;

  /**
   * Create a new OutputFile.
   *
   * @param file
   *        the file to write to.
   * @param compatibleVersion
   *        the minimum compatible JavaCC version.
   * @param options
   *        if the file already exists, and cannot be overwritten, this is a list of options (such s
   *        STATIC=false) to check for changes.
   * @throws IOException
   *         on error
   */
  public OutputFile (final File file, final String compatibleVersion, final String [] options) throws IOException
  {
    m_aFile = file;
    m_sCompatibleVersion = compatibleVersion;
    m_aOptions = options;

    if (file.exists ())
    {
      // Generate the checksum of the file, and compare with any value
      // stored in the file.
      try (final NonBlockingBufferedReader br = FileHelper.getBufferedReader (file, Options.getOutputEncoding ()))
      {
        MessageDigest digest;
        try
        {
          digest = MessageDigest.getInstance ("MD5");
        }
        catch (final NoSuchAlgorithmException e)
        {
          throw new IOException ("No MD5 implementation", e);
        }
        try (final DigestOutputStream digestStream = new DigestOutputStream (new NullOutputStream (), digest);
             final OutputStreamWriter osw = new OutputStreamWriter (digestStream, Options.getOutputEncoding ());
             final PrintWriter pw = new PrintWriter (osw))
        {
          String line;
          String existingMD5 = null;
          while ((line = br.readLine ()) != null)
          {
            if (line.startsWith (MD5_LINE_PART_1))
            {
              existingMD5 = line.replaceAll (MD5_LINE_PART_1Q, "").replaceAll (MD5_LINE_PART_2Q, "");
            }
            else
            {
              pw.println (line);
            }
          }

          final String calculatedDigest = StringHex.getHexEncoded (digestStream.getMessageDigest ().digest ());

          if (existingMD5 == null || !existingMD5.equals (calculatedDigest))
          {
            // No checksum in file, or checksum differs.
            m_bNeedToWrite = false;

            if (compatibleVersion != null)
            {
              _checkVersion (file, compatibleVersion);
            }

            if (options != null)
            {
              _checkOptions (file, options);
            }
          }
          else
          {
            // The file has not been altered since JavaCC created it.
            // Rebuild it.
            PGPrinter.info ("File \"" + file.getName () + "\" is being rebuilt.");
            m_bNeedToWrite = true;
          }
        }
      }
    }
    else
    {
      // File does not exist
      PGPrinter.info ("File \"" + file.getName () + "\" does not exist.  Will create one.");
      m_bNeedToWrite = true;
    }
  }

  public OutputFile (final File file) throws IOException
  {
    this (file, null, null);
  }

  /**
   * Output a warning if the file was created with an incompatible version of JavaCC.
   *
   * @param fileName
   * @param versionId
   */
  private void _checkVersion (final File file, final String versionId)
  {
    final String firstLine = "/* " + JavaCCGlobals.getIdString (m_sToolName, file.getName ()) + " Version ";

    try (final NonBlockingBufferedReader reader = FileHelper.getBufferedReader (file, Options.getOutputEncoding ()))
    {
      String line;
      while ((line = reader.readLine ()) != null)
      {
        if (line.startsWith (firstLine))
        {
          final String version = line.replaceFirst (".*Version ", "").replace (" */", "");
          if (!version.equals (versionId))
          {
            JavaCCErrors.warning (file.getName () +
                                  ": File is obsolete.  Please rename or delete this file so" +
                                  " that a new one can be generated for you.");
            JavaCCErrors.warning (file.getName () + " file   version: " + version + " javacc version: " + versionId);
          }
          return;
        }
      }
      // If no version line is found, do not output the warning.
    }
    catch (final FileNotFoundException e1)
    {
      // This should never happen
      JavaCCErrors.semantic_error ("Could not open file " + file.getName () + " for writing.");
      throw new UncheckedIOException (e1);
    }
    catch (final IOException e2)
    {}
  }

  /**
   * Read the options line from the file and compare to the options currently in use. Output a
   * warning if they are different.
   *
   * @param fileName
   * @param options
   */
  private void _checkOptions (final File file, final String [] options)
  {
    try (final NonBlockingBufferedReader reader = FileHelper.getBufferedReader (file, Options.getOutputEncoding ()))
    {
      String line;
      while ((line = reader.readLine ()) != null)
      {
        if (line.startsWith ("/* " + OPTIONS_PREFIX + ":"))
        {
          final String currentOptions = Options.getOptionsString (options);
          if (line.indexOf (currentOptions) == -1)
          {
            JavaCCErrors.warning (file.getName () +
                                  ": Generated using incompatible options. Please rename or delete this file so" +
                                  " that a new one can be generated for you.");
          }
          return;
        }
      }
    }
    catch (final FileNotFoundException e1)
    {
      // This should never happen
      JavaCCErrors.semantic_error ("Could not open file " + file.getName () + " for writing.");
      throw new UncheckedIOException (e1);
    }
    catch (final IOException e2)
    {}

    // Not found so cannot check
  }

  /**
   * Return a PrintWriter object that may be used to write to this file. Any necessary header
   * information is written by this method.
   *
   * @return the {@link PrintWriter} to use.
   * @throws IOException
   *         on IO error
   */
  @WillCloseWhenClosed
  public PrintWriter getPrintWriter () throws IOException
  {
    if (m_aPW == null)
    {
      MessageDigest digest = EMessageDigestAlgorithm.MD5.createMessageDigest ();
      try
      {
        digest = MessageDigest.getInstance ("MD5");
      }
      catch (final NoSuchAlgorithmException e)
      {
        throw new IOException ("No MD5 implementation", e);
      }
      m_dos = new DigestOutputStream (FileHelper.getBufferedOutputStream (m_aFile), digest);
      m_aPW = new TrapClosePrintWriter (m_dos, Options.getOutputEncoding ());

      // Write the headers....
      final String version = m_sCompatibleVersion == null ? PGVersion.VERSION_NUMBER : m_sCompatibleVersion;
      m_aPW.println ("/* " +
                     JavaCCGlobals.getIdString (m_sToolName, m_aFile.getName ()) +
                     " Version " +
                     version +
                     " */");
      if (m_aOptions != null)
      {
        m_aPW.println ("/* " + OPTIONS_PREFIX + ":" + Options.getOptionsString (m_aOptions) + " */");
      }
    }

    return m_aPW;
  }

  /**
   * Close the OutputFile, writing any necessary trailer information (such as a checksum).
   */
  public void close ()
  {
    // Write the trailer (checksum).
    // Possibly rename the .java.tmp to .java??
    if (m_aPW != null)
    {
      m_aPW.println (MD5_LINE_PART_1 + _getMD5sum () + MD5_LINE_PART_2);
      m_aPW.closePrintWriter ();
      // file.renameTo(dest)
    }
  }

  private String _getMD5sum ()
  {
    m_aPW.flush ();
    final byte [] digest = m_dos.getMessageDigest ().digest ();
    return StringHex.getHexEncoded (digest);
  }

  private final class TrapClosePrintWriter extends PrintWriter
  {
    public TrapClosePrintWriter (final OutputStream os, @Nonnull final Charset aCS)
    {
      super (new OutputStreamWriter (os, aCS));
    }

    void closePrintWriter ()
    {
      super.close ();
    }

    @Override
    public void close ()
    {
      OutputFile.this.close ();
    }
  }

  /**
   * @return the toolName
   */
  public String getToolName ()
  {
    return m_sToolName;
  }

  /**
   * @param toolName
   *        the toolName to set
   */
  public void setToolName (final String toolName)
  {
    m_sToolName = toolName;
  }

  public String getPath ()
  {
    return m_aFile.getAbsolutePath ();
  }

  public boolean needToWrite ()
  {
    return m_bNeedToWrite;
  }
}
