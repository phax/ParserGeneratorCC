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
package com.helger.pgcc.golden;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;

/**
 * A manifest of the files that one generator run produced, as
 * <code>&lt;sha-256&gt;&nbsp;&lt;relative file name&gt;</code> lines sorted by file name.
 * <p>
 * The manifest is the only specification this project has for what "the generated code did not
 * change" means, so it is checked in and compared byte for byte. See
 * {@link GeneratedOutputGoldenTest} for how to re-bless it after a deliberate change.
 *
 * @author Philip Helger
 */
public final class GoldenManifest
{
  private final List <String> m_aLines;

  private GoldenManifest (@NonNull final List <String> aLines)
  {
    m_aLines = aLines;
  }

  /**
   * Take the version stamp out of a generated file before hashing it.
   * <p>
   * Every generated file carries the generator version in its header, and a checksum of its own
   * content underneath. Hashing those means a version bump rewrites all 560 manifest lines and the
   * diff no longer says which files really changed, which is the whole point of the harness.
   * {@code SelfGenerateFuncTest} has always done this; this did not.
   *
   * @param sContent
   *        The file content. May not be <code>null</code>.
   * @return The content with the version and the checksum blanked out. Never <code>null</code>.
   */
  @NonNull
  private static String _normalize (@NonNull final String sContent)
  {
    return sContent.replaceAll ("Version [0-9][^ ]* \\*/", "Version <version> */")
                   .replaceAll ("OriginalChecksum=[0-9a-f]+", "OriginalChecksum=<checksum>");
  }

  @NonNull
  private static String _sha256 (@NonNull final Path aFile)
  {
    try
    {
      final MessageDigest aDigest = MessageDigest.getInstance ("SHA-256");
      final byte [] aHash = aDigest.digest (_normalize (Files.readString (aFile, StandardCharsets.UTF_8))
                                                       .getBytes (StandardCharsets.UTF_8));
      final StringBuilder aRet = new StringBuilder (aHash.length * 2);
      for (final byte b : aHash)
        aRet.append (Character.forDigit ((b >> 4) & 0xf, 16)).append (Character.forDigit (b & 0xf, 16));
      return aRet.toString ();
    }
    catch (final NoSuchAlgorithmException aEx)
    {
      throw new IllegalStateException ("SHA-256 is required by every JRE", aEx);
    }
    catch (final IOException aEx)
    {
      throw new UncheckedIOException (aEx);
    }
  }

  /**
   * Build a manifest of everything below a directory.
   *
   * @param aDir
   *        The directory to scan. May not be <code>null</code>.
   * @return The manifest. Never <code>null</code>.
   * @throws IOException
   *         On IO error
   */
  @NonNull
  public static GoldenManifest ofDirectory (@NonNull final File aDir) throws IOException
  {
    final Path aRoot = aDir.toPath ();
    final List <String> aLines = new ArrayList <> ();
    try (final Stream <Path> aStream = Files.walk (aRoot))
    {
      aStream.filter (Files::isRegularFile)
             .sorted ()
             .forEach (aFile -> aLines.add (_sha256 (aFile) +
                                            "  " +
                                            aRoot.relativize (aFile).toString ().replace ('\\', '/')));
    }
    return new GoldenManifest (aLines);
  }

  /**
   * Read a manifest from disk.
   *
   * @param aFile
   *        The manifest file. May not be <code>null</code>.
   * @return <code>null</code> if the file does not exist yet.
   * @throws IOException
   *         On IO error
   */
  public static GoldenManifest read (@NonNull final File aFile) throws IOException
  {
    if (!aFile.exists ())
      return null;
    return new GoldenManifest (Files.readAllLines (aFile.toPath (), StandardCharsets.UTF_8));
  }

  public void write (@NonNull final File aFile) throws IOException
  {
    aFile.getParentFile ().mkdirs ();
    Files.write (aFile.toPath (), m_aLines, StandardCharsets.UTF_8);
  }

  public int getFileCount ()
  {
    return m_aLines.size ();
  }

  /**
   * Compare this manifest with an expected one.
   *
   * @param aExpected
   *        The expected manifest. May not be <code>null</code>.
   * @return An empty String if both are equal, else a human readable description of every
   *         difference. Never <code>null</code>.
   */
  @NonNull
  public String getDifferences (@NonNull final GoldenManifest aExpected)
  {
    final StringBuilder aRet = new StringBuilder ();
    final List <String> aExpectedNames = aExpected._getNames ();
    final List <String> aActualNames = _getNames ();

    for (final String sName : aExpectedNames)
      if (!aActualNames.contains (sName))
        aRet.append ("  missing: ").append (sName).append ('\n');

    for (final String sName : aActualNames)
      if (!aExpectedNames.contains (sName))
        aRet.append ("  unexpected: ").append (sName).append ('\n');

    for (final String sLine : m_aLines)
    {
      final String sName = _nameOf (sLine);
      if (aExpectedNames.contains (sName) && !aExpected.m_aLines.contains (sLine))
        aRet.append ("  content changed: ").append (sName).append ('\n');
    }
    return aRet.toString ();
  }

  @NonNull
  private static String _nameOf (@NonNull final String sLine)
  {
    final int nIdx = sLine.indexOf ("  ");
    return nIdx < 0 ? sLine : sLine.substring (nIdx + 2);
  }

  @NonNull
  private List <String> _getNames ()
  {
    final List <String> aRet = new ArrayList <> (m_aLines.size ());
    for (final String sLine : m_aLines)
      aRet.add (_nameOf (sLine));
    return aRet;
  }
}
