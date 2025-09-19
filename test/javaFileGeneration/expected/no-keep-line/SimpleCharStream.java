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
/**
 * An implementation of interface CharStream, where the stream is assumed to
 * contain only ASCII characters (without unicode processing).
 */

public class SimpleCharStream
{
  static int bufsize;
  static int available;
  static int tokenBegin;
/** Position in buffer. */
  static public int bufpos = -1;

  static protected java.io.Reader inputStream;

  static protected char[] buffer;
  static protected int maxNextCharInd = 0;
  static protected int inBuf = 0;
  static protected int tabSize = 8;

  static protected void setTabSize(int i) { tabSize = i; }
  static protected int getTabSize(int i) { return tabSize; }


  static protected void ExpandBuff(boolean wrapAround)
  {
    char[] newbuffer = new char[bufsize + 2048];

    try
    {
      if (wrapAround)
      {
        System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
        System.arraycopy(buffer, 0, newbuffer, bufsize - tokenBegin, bufpos);
        buffer = newbuffer;

        maxNextCharInd = (bufpos += (bufsize - tokenBegin));
      }
      else
      {
        System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
        buffer = newbuffer;

        maxNextCharInd = (bufpos -= tokenBegin);
      }
    }
    catch (Throwable t)
    {
      throw new Error(t.getMessage());
    }


    bufsize += 2048;
    available = bufsize;
    tokenBegin = 0;
  }

  static protected void FillBuff() throws java.io.IOException
  {
    if (maxNextCharInd == available)
    {
      if (available == bufsize)
      {
        if (tokenBegin > 2048)
        {
          bufpos = maxNextCharInd = 0;
          available = tokenBegin;
        }
        else if (tokenBegin < 0)
          bufpos = maxNextCharInd = 0;
        else
          ExpandBuff(false);
      }
      else if (available > tokenBegin)
        available = bufsize;
      else if ((tokenBegin - available) < 2048)
        ExpandBuff(true);
      else
        available = tokenBegin;
    }

    int i;
    try {
      if ((i = inputStream.read(buffer, maxNextCharInd, available - maxNextCharInd)) == -1)
      {
        inputStream.close();
        throw new java.io.IOException();
      }
      else
        maxNextCharInd += i;
      return;
    }
    catch(java.io.IOException e) {
      --bufpos;
      backup(0);
      if (tokenBegin == -1)
        tokenBegin = bufpos;
      throw e;
    }
  }

/** Start. */
  static public char BeginToken() throws java.io.IOException
  {
    tokenBegin = -1;
    char c = readChar();
    tokenBegin = bufpos;

    return c;
  }

/** Read a character. */
  static public char readChar() throws java.io.IOException
  {
    if (inBuf > 0)
    {
      --inBuf;

      if (++bufpos == bufsize)
        bufpos = 0;

      return buffer[bufpos];
    }

    if (++bufpos >= maxNextCharInd)
      FillBuff();

    char c = buffer[bufpos];

    return c;
  }

  /** Get token end column number. */
  static public int getEndColumn() {
    return -1;
  }

  /** Get token end line number. */
  static public int getEndLine() {
    return -1;
  }

  /** Get token beginning column number. */
  static public int getBeginColumn() {
    return -1;
  }

  /** Get token beginning line number. */
  static public int getBeginLine() {
    return -1;
  }

/** Backup a number of characters. */
  static public void backup(int amount) {

    inBuf += amount;
    if ((bufpos -= amount) < 0)
      bufpos += bufsize;
  }

  /** Constructor. */
  public SimpleCharStream(java.io.Reader dstream, int startline,
  int startcolumn, int buffersize)
  {
    if (inputStream != null)
      throw new Error("\n   ERROR: Second call to the constructor of a static SimpleCharStream.\n" +
      "       You must either use ReInit() or set the JavaCC option STATIC to false\n" +
      "       during the generation of this class.");
    inputStream = dstream;

    available = bufsize = buffersize;
    buffer = new char[buffersize];
  }

  /** Constructor. */
  public SimpleCharStream(java.io.Reader dstream, int startline,
                          int startcolumn)
  {
    this(dstream, startline, startcolumn, 4096);
  }

  /** Constructor. */
  public SimpleCharStream(java.io.Reader dstream)
  {
    this(dstream, 1, 1, 4096);
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader dstream, int startline,
  int startcolumn, int buffersize)
  {
    inputStream = dstream;

    if (buffer == null || buffersize != buffer.length)
    {
      available = bufsize = buffersize;
      buffer = new char[buffersize];
    }
    tokenBegin = inBuf = maxNextCharInd = 0;
    bufpos = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader dstream, int startline,
                     int startcolumn)
  {
    ReInit(dstream, startline, startcolumn, 4096);
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader dstream)
  {
    ReInit(dstream, 1, 1, 4096);
  }
  /** Constructor. */
  public SimpleCharStream(java.io.InputStream dstream, String encoding, int startline,
  int startcolumn, int buffersize) throws java.io.UnsupportedEncodingException
  {
    this(encoding == null ? new java.io.InputStreamReader(dstream) : new java.io.InputStreamReader(dstream, encoding), startline, startcolumn, buffersize);
  }

  /** Constructor. */
  public SimpleCharStream(java.io.InputStream dstream, int startline,
  int startcolumn, int buffersize)
  {
    this(new java.io.InputStreamReader(dstream), startline, startcolumn, buffersize);
  }

  /** Constructor. */
  public SimpleCharStream(java.io.InputStream dstream, String encoding, int startline,
                          int startcolumn) throws java.io.UnsupportedEncodingException
  {
    this(dstream, encoding, startline, startcolumn, 4096);
  }

  /** Constructor. */
  public SimpleCharStream(java.io.InputStream dstream, int startline,
                          int startcolumn)
  {
    this(dstream, startline, startcolumn, 4096);
  }

  /** Constructor. */
  public SimpleCharStream(java.io.InputStream dstream, String encoding) throws java.io.UnsupportedEncodingException
  {
    this(dstream, encoding, 1, 1, 4096);
  }

  /** Constructor. */
  public SimpleCharStream(java.io.InputStream dstream)
  {
    this(dstream, 1, 1, 4096);
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream dstream, String encoding, int startline,
                          int startcolumn, int buffersize) throws java.io.UnsupportedEncodingException
  {
    ReInit(encoding == null ? new java.io.InputStreamReader(dstream) : new java.io.InputStreamReader(dstream, encoding), startline, startcolumn, buffersize);
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream dstream, int startline,
                          int startcolumn, int buffersize)
  {
    ReInit(new java.io.InputStreamReader(dstream), startline, startcolumn, buffersize);
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream dstream, String encoding) throws java.io.UnsupportedEncodingException
  {
    ReInit(dstream, encoding, 1, 1, 4096);
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream dstream)
  {
    ReInit(dstream, 1, 1, 4096);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream dstream, String encoding, int startline,
                     int startcolumn) throws java.io.UnsupportedEncodingException
  {
    ReInit(dstream, encoding, startline, startcolumn, 4096);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream dstream, int startline,
                     int startcolumn)
  {
    ReInit(dstream, startline, startcolumn, 4096);
  }
  /** Get token literal value. */
  static public String GetImage()
  {
    if (bufpos >= tokenBegin)
      return new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
    else
      return new String(buffer, tokenBegin, bufsize - tokenBegin) +
                            new String(buffer, 0, bufpos + 1);
  }

  /** Get the suffix. */
  static public char[] GetSuffix(int len)
  {
    char[] ret = new char[len];

    if ((bufpos + 1) >= len)
      System.arraycopy(buffer, bufpos - len + 1, ret, 0, len);
    else
    {
      System.arraycopy(buffer, bufsize - (len - bufpos - 1), ret, 0,
                                                        len - bufpos - 1);
      System.arraycopy(buffer, 0, ret, len - bufpos - 1, bufpos + 1);
    }

    return ret;
  }

  /** Reset buffer when finished. */
  static public void Done()
  {
    buffer = null;
  }
}
/* JavaCC - OriginalChecksum=ef34bc2196a4ac3032f8fd45964ca3ae (do not edit this line) */
