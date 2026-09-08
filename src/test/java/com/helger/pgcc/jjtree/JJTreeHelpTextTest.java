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
package com.helger.pgcc.jjtree;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.JavaVersionHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.PGPrinter.IPrinter;
import com.helger.pgcc.PGPrinter.PSPrinter;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.Options;

/**
 * Check the defaults JJTree's help output advertises against the defaults it actually uses.
 * <p>
 * The JavaCC help text is generated from the option table, so it cannot drift. JJTree's is a block
 * of hand written lines, and it had drifted: it promised <code>JDK_VERSION (default "1.5")</code>
 * long after the default became 1.8, and <code>OUTPUT_DIRECTORY (default "")</code> where the value
 * is ".". Nobody would notice, because nothing reads the help text but a person.
 *
 * @author Philip Helger
 */
public final class JJTreeHelpTextTest
{
  /** {@code    OPTION_NAME              (default X)} */
  private static final Pattern DEFAULT_LINE = Pattern.compile ("^\\s+([A-Z][A-Z_0-9]+)\\s+\\(default (.+)\\)$");

  private static final class CollectingPrinter implements IPrinter
  {
    private final List <String> m_aLines = new ArrayList <> ();

    public void println (final String s)
    {
      m_aLines.add (s == null ? "" : s);
    }

    public void flush ()
    {}

    public void close ()
    {}
  }

  private CollectingPrinter m_aPrinter;

  @Before
  public void before ()
  {
    m_aPrinter = new CollectingPrinter ();
    PGPrinter.init (m_aPrinter);
  }

  @After
  public void after ()
  {
    PGPrinter.init (new PSPrinter (System.out, false), new PSPrinter (System.err, false));
  }

  /**
   * Compare one advertised default against the value the option really has.
   *
   * @param sName
   *        Option name. May not be <code>null</code>.
   * @param sClaim
   *        What the help text says, quotes already removed. May not be <code>null</code>.
   * @return <code>null</code> if they agree or the claim cannot be compared, otherwise the
   *         complaint.
   */
  private static String _mismatch (@NonNull final String sName, @NonNull final String sClaim)
  {
    final Object aActual = Options.objectValue (sName);
    if (aActual == null)
    {
      // Not a JJTree option, or one without a default - nothing to compare
      return null;
    }

    if (Options.USEROPTION__JDK_VERSION.equals (sName))
    {
      // Stored as an EJavaVersion, so compare what the option parser would make of the claim
      return JavaVersionHelper.getFromStringOrNull (sClaim) == aActual ? null
                                                                       : sName +
                                                                         ": help says \"" +
                                                                         sClaim +
                                                                         "\", actual is " +
                                                                         aActual;
    }

    final String sActual = String.valueOf (aActual);
    return sActual.equals (sClaim) ? null : sName + ": help says \"" + sClaim + "\", actual is \"" + sActual + "\"";
  }

  @Test
  public void testTheAdvertisedDefaultsAreTheRealOnes ()
  {
    // No arguments prints the help and stops
    new JJTree ().main (new String [0]);
    final List <String> aHelp = new ArrayList <> (m_aPrinter.m_aLines);
    assertTrue ("JJTree printed no help at all", aHelp.size () > 20);

    // The run above reset the context, so the option values are the defaults right now
    Main.reInitAll ();
    JJTreeOptions.init ();

    final List <String> aMismatches = new ArrayList <> ();
    int nChecked = 0;
    for (final String sLine : aHelp)
    {
      final Matcher aMatcher = DEFAULT_LINE.matcher (sLine);
      if (!aMatcher.matches ())
        continue;

      String sClaim = aMatcher.group (2).trim ();
      if (sClaim.length () >= 2 && sClaim.charAt (0) == '"' && sClaim.charAt (sClaim.length () - 1) == '"')
        sClaim = sClaim.substring (1, sClaim.length () - 1);
      else
        if (sClaim.indexOf (' ') >= 0)
        {
          // Prose rather than a value, like "value of OUTPUT_DIRECTORY option"
          continue;
        }

      final String sMismatch = _mismatch (aMatcher.group (1), sClaim);
      if (sMismatch != null)
        aMismatches.add (sMismatch);
      nChecked++;
    }

    assertTrue ("The help text advertises no defaults at all - did its layout change?", nChecked >= 15);
    assertTrue (nChecked + " defaults checked, these disagree with the real value:\n" + String.join ("\n", aMismatches),
                aMismatches.isEmpty ());
  }
}
