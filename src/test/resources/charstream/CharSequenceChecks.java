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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

/** Compiled with the generated grammar and streams by CharSequenceCharStreamTest. */
public class CharSequenceChecks
{
  private static String letters (final int nLength)
  {
    final StringBuilder ret = new StringBuilder (nLength);
    for (int i = 0; i < nLength; ++i)
      ret.append ((char) ('a' + i % 26));
    return ret.toString ();
  }

  private static StringReader reader (final String sInput, final int nChunk)
  {
    return new StringReader (sInput)
    {
      @Override
      public int read (final char [] aBuffer, final int nOffset, final int nLength) throws IOException
      {
        return super.read (aBuffer, nOffset, Math.min (nLength, nChunk));
      }
    };
  }

  private static void positions (final CharStream aStream, final int nBeginLine, final int nBeginColumn,
                                 final int nEndLine, final int nEndColumn)
  {
    assertEquals (BufferTestStream.LINE_COLUMN ? nBeginLine : -1, aStream.getBeginLine ());
    assertEquals (BufferTestStream.LINE_COLUMN ? nBeginColumn : -1, aStream.getBeginColumn ());
    assertEquals (BufferTestStream.LINE_COLUMN ? nEndLine : -1, aStream.getEndLine ());
    assertEquals (BufferTestStream.LINE_COLUMN ? nEndColumn : -1, aStream.getEndColumn ());
  }

  private static void eof (final CharStream aStream) throws IOException
  {
    try
    {
      aStream.readChar ();
      fail ("Expected EOF");
    }
    catch (final IOException ex)
    {
      // The char-stream API signals EOF with IOException.
    }
  }

  private static void readToken (final CharStream aStream, final String sExpected) throws IOException
  {
    assertEquals (sExpected.charAt (0), aStream.beginToken ());
    for (int i = 1; i < sExpected.length (); ++i)
      assertEquals ("character " + i, sExpected.charAt (i), aStream.readChar ());
    assertEquals (sExpected, aStream.getImage ());
    assertArrayEquals (sExpected.toCharArray (), aStream.getSuffix (sExpected.length ()));
  }

  @Test
  public void largeTokens () throws IOException
  {
    // No buffer means no buffer size limit - all of these must simply work.
    for (final int nOffset : new int [] { 0, 1, 2048, 2051, 4093 })
      for (final int nLength : new int [] { 4093, 4096, 4097, 8193, 32768 })
        for (final int nChunk : new int [] { 1, 7, 511, 4096 })
        {
          final String sPrefix = letters (nOffset);
          final String sToken = letters (nLength);
          final BufferTestStream aStream = new BufferTestStream (reader (sPrefix + sToken + "!", nChunk));
          if (nOffset > 0)
          {
            readToken (aStream, sPrefix);
            // One character of lexer lookahead beyond the first token
            assertEquals (sToken.charAt (0), aStream.readChar ());
            aStream.backup (1);
          }
          readToken (aStream, sToken);
          positions (aStream, 1, nOffset + 1, 1, nOffset + nLength);
          // Back up the whole token and read it again
          aStream.backup (nLength);
          readToken (aStream, sToken);
          positions (aStream, 1, nOffset + 1, 1, nOffset + nLength);
          readToken (aStream, "!");
          positions (aStream, 1, nOffset + nLength + 1, 1, nOffset + nLength + 1);
          eof (aStream);
        }
  }

  @Test
  public void imageAndSuffix () throws IOException
  {
    final String sPrefix = letters (3000);
    final String sToken = letters (2000);
    final BufferTestStream aStream = new BufferTestStream (reader (sPrefix + sToken + "!", 7));
    readToken (aStream, sPrefix);
    assertEquals ('a', aStream.readChar ());
    aStream.backup (1);
    readToken (aStream, sToken);
    positions (aStream, 1, 3001, 1, 5000);
    aStream.backup (1500);
    for (int i = 500; i < 2000; ++i)
      assertEquals (sToken.charAt (i), aStream.readChar ());
    assertEquals (sToken, aStream.getImage ());
    assertArrayEquals (letters (2000).substring (1990).toCharArray (), aStream.getSuffix (10));
    readToken (aStream, "!");
    eof (aStream);
  }

  @Test
  public void linesAndColumns () throws IOException
  {
    // "\r\n" is a single line terminator, a lonely "\r" is one as well
    final BufferTestStream aStream = new BufferTestStream (reader ("ab\r\ncd\ref\ngh", 3));
    readToken (aStream, "ab");
    positions (aStream, 1, 1, 1, 2);
    readToken (aStream, "\r\ncd");
    positions (aStream, 1, 3, 2, 2);
    readToken (aStream, "\ref");
    positions (aStream, 2, 3, 3, 2);
    readToken (aStream, "\ngh");
    positions (aStream, 3, 3, 4, 2);
    eof (aStream);
  }

  @Test
  public void tabColumns () throws IOException
  {
    final BufferTestStream aStream = new BufferTestStream (reader (letters (8193) + "\r\n\tend", 7));
    aStream.setTabSize (4);
    readToken (aStream, letters (8193) + "\r\n\tend");
    positions (aStream, 1, 1, 2, 7);
    eof (aStream);
  }

  @Test
  public void grammarTokens () throws Exception
  {
    for (final int nOffset : new int [] { 0, 2051, 4093 })
    {
      final String sWord = letters (4093);
      final String sString = "\"" + sWord + "\"";
      final String sPrefix = letters (nOffset);
      final BufferParserTokenManager aTokens = new BufferParserTokenManager (
          new BufferTestStream (reader (sPrefix + sString + ";\r\n" + sWord + ";" + sWord, 7)));
      if (nOffset > 0)
        assertEquals (sPrefix, aTokens.getNextToken ().image);
      Token aToken = aTokens.getNextToken ();
      assertEquals (BufferParserConstants.STRING, aToken.kind);
      assertEquals (sString, aToken.image);
      if (BufferTestStream.LINE_COLUMN)
      {
        assertEquals (1, Token.class.getField ("beginLine").getInt (aToken));
        assertEquals (nOffset + 1, Token.class.getField ("beginColumn").getInt (aToken));
        assertEquals (1, Token.class.getField ("endLine").getInt (aToken));
        assertEquals (nOffset + sString.length (), Token.class.getField ("endColumn").getInt (aToken));
      }
      assertEquals (BufferParserConstants.SEMICOLON, aTokens.getNextToken ().kind);
      aToken = aTokens.getNextToken ();
      assertEquals (BufferParserConstants.WORD, aToken.kind);
      assertEquals (sWord, aToken.image);
      if (BufferTestStream.LINE_COLUMN)
      {
        assertEquals (2, Token.class.getField ("beginLine").getInt (aToken));
        assertEquals (1, Token.class.getField ("beginColumn").getInt (aToken));
        assertEquals (2, Token.class.getField ("endLine").getInt (aToken));
        assertEquals (sWord.length (), Token.class.getField ("endColumn").getInt (aToken));
      }
      assertEquals (BufferParserConstants.SEMICOLON, aTokens.getNextToken ().kind);
      assertEquals (sWord, aTokens.getNextToken ().image);
      assertEquals (BufferParserConstants.EOF, aTokens.getNextToken ().kind);
    }
  }

  @Test
  public void reinitialization () throws IOException
  {
    for (final boolean bReadToEOF : new boolean [] { false, true })
    {
      final BufferTestStream aStream = new BufferTestStream (reader (letters (8193), 4096));
      if (bReadToEOF)
      {
        readToken (aStream, letters (8193));
        eof (aStream);
      }
      else
        assertEquals ('a', aStream.beginToken ());
      aStream.reset (reader ("xy\r\nz", 1), 4096);
      readToken (aStream, "xy");
      positions (aStream, 3, 7, 3, 8);
      readToken (aStream, "\r\nz");
      positions (aStream, 3, 9, 4, 1);
      eof (aStream);
      aStream.reset (reader ("", 1), 4096);
      eof (aStream);
      aStream.done ();
      aStream.reset (reader ("q", 1), 4096);
      readToken (aStream, "q");
      positions (aStream, 3, 7, 3, 7);
      eof (aStream);
    }
  }

  @Test
  public void charSequenceInput () throws IOException
  {
    // The whole point - the input can be used as it is
    final String sToken = letters (32768);
    final CharSequenceCharStream aStream = new CharSequenceCharStream (new StringBuilder (sToken).append ('!'));
    readToken (aStream, sToken);
    positions (aStream, 1, 1, 1, sToken.length ());
    readToken (aStream, "!");
    eof (aStream);

    aStream.reInit (sToken);
    readToken (aStream, sToken);
    eof (aStream);
  }

  @Test
  public void emptyInput () throws IOException
  {
    final CharSequenceCharStream aStream = new CharSequenceCharStream ("");
    try
    {
      aStream.beginToken ();
      fail ("Expected EOF");
    }
    catch (final IOException ex)
    {
      // Expected
    }
    positions (aStream, 1, 1, 1, 1);
  }
}
