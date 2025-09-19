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
 * This file contains the code for JavaCCParser generated
 * by JavaCCParser itself.
 */

package com.helger.pgcc.parser;

/**
 * Describes the input token stream.
 */
public class Token
{
  /**
   * An integer that describes the kind of this token. This numbering system is
   * determined by JavaCCParser, and a table of these numbers is stored in the
   * file ...Constants.java.
   */
  public int kind;

  /** The line number of the first character of this Token. */
  public int beginLine;
  /** The column number of the first character of this Token. */
  public int beginColumn;
  /** The line number of the last character of this Token. */
  public int endLine;
  /** The column number of the last character of this Token. */
  public int endColumn;

  /**
   * The string image of the token.
   */
  public String image;

  /**
   * A reference to the next regular (non-special) token from the input stream.
   * If this is the last token from the input stream, or if the token manager
   * has not read tokens beyond this one, this field is set to null. This is
   * true only if this token is also a regular token. Otherwise, see below for a
   * description of the contents of this field.
   */
  public Token next;

  /**
   * This field is used to access special tokens that occur prior to this token,
   * but after the immediately preceding regular (non-special) token. If there
   * are no such special tokens, this field is set to null. When there are more
   * than one such special token, this field refers to the last of these special
   * tokens, which in turn refers to the next previous special token through its
   * specialToken field, and so on until the first special token (whose
   * specialToken field is null). The next fields of special tokens refer to
   * other special tokens that immediately follow it (without an intervening
   * regular token). If there is no such token, this field is null.
   */
  public Token specialToken;

  /**
   * No-argument constructor
   */
  public Token ()
  {}

  /**
   * Constructs a new token for the specified Image and Kind.
   *
   * @param kind
   *        token kind
   * @param image
   *        token image
   */
  public Token (final int kind, final String image)
  {
    this.kind = kind;
    this.image = image;
  }

  /**
   * equals
   */
  @Override
  public boolean equals (final Object object)
  {
    if (object == null)
      return false;
    if (this == object)
      return true;
    if (object instanceof String)
      return object.equals (image);
    return false;
  }

  /**
   * hashCode
   */
  @Override
  public int hashCode ()
  {
    return image.hashCode ();
  }

  /**
   * Returns the image.
   */
  @Override
  public String toString ()
  {
    return image;
  }

  /**
   * An optional attribute value of the Token. Tokens which are not used as
   * syntactic sugar will often contain meaningful values that will be used
   * later on by the compiler or interpreter. This attribute value is often
   * different from the image. Any subclass of Token that actually wants to
   * return a non-null value can override this method as appropriate.
   *
   * @return token value
   */
  public Object getValue ()
  {
    return null;
  }

  /**
   * Returns a new Token object, by default. However, if you want, you can
   * create and return subclass objects based on the value of ofKind. Simply add
   * the cases to the switch for all those special cases. For example, if you
   * have a subclass of Token called IDToken that you want to create if ofKind
   * is ID, simply add something like : case MyParserConstants.ID : return new
   * IDToken(ofKind, image); to the following switch statement. Then you can
   * cast matchedToken variable to the appropriate type and use it in your
   * lexical actions.
   *
   * @param ofKind
   *        token kind
   * @param image
   *        token image
   * @return The created token. Never <code>null</code>.
   */
  public static Token newToken (final int ofKind, final String image)
  {
    switch (ofKind)
    {
      case JavaCCParserConstants.RUNSIGNEDSHIFT:
      case JavaCCParserConstants.RSIGNEDSHIFT:
      case JavaCCParserConstants.GT:
        return new GTToken (ofKind, image);
      default:
        return new Token (ofKind, image);
    }
  }

  public static Token newToken (final int ofKind)
  {
    return newToken (ofKind, null);
  }

  /**
   * Greater than Token.
   */
  public static class GTToken extends Token
  {
    int m_realKind = JavaCCParserConstants.GT;

    public GTToken (final int kind, final String image)
    {
      super (kind, image);
    }
  }
}
