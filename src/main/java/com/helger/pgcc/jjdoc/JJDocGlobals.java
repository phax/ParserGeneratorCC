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
package com.helger.pgcc.jjdoc;

import com.helger.pgcc.context.PGCCContext;

/**
 * Global variables for JJDoc.
 */
public final class JJDocGlobals
{
  /** Default constructor. */
  public JJDocGlobals ()
  {}

  /**
   * The name {@link com.helger.pgcc.context.JJDocState#getInputFile()} carries when the grammar
   * is read from stdin rather than from a file.
   */
  public static final String STANDARD_INPUT = "standard input";
  /**
   * The name {@link com.helger.pgcc.context.JJDocState#getOutputFile()} carries when the output
   * goes to stdout rather than to a file.
   */
  public static final String STANDARD_OUTPUT = "standard output";

  /**
   * Choose what JJDoc produces.
   *
   * @param aGenerator
   *        The generator to set.
   */
  public static void setGenerator (final IDocGenerator aGenerator)
  {
    PGCCContext.current ().jjdoc ().setGenerator (aGenerator);
  }

  /**
   * The commandline option is either TEXT or not, but the generator might have been set to some
   * other Generator using the setGenerator method.
   *
   * @return the generator configured in options or set by setter.
   */
  public static IDocGenerator getGenerator ()
  {
    if (PGCCContext.current ().jjdoc ().getGenerator () == null)
    {
      if (JJDocOptions.isText ())
      {
        PGCCContext.current ().jjdoc ().setGenerator (new TextGenerator ());
      }
      else
        if (JJDocOptions.isBNF ())
        {
          PGCCContext.current ().jjdoc ().setGenerator (new BNFGenerator ());
        }
        else
          if (JJDocOptions.isXText ())
          {
            PGCCContext.current ().jjdoc ().setGenerator (new XTextGenerator ());
          }
          else
          {
            PGCCContext.current ().jjdoc ().setGenerator (new HTMLGenerator ());
          }
    }
    else
    {
      if (JJDocOptions.isText ())
      {
        if (PGCCContext.current ().jjdoc ().getGenerator () instanceof HTMLGenerator)
        {
          PGCCContext.current ().jjdoc ().setGenerator (new TextGenerator ());
        }
      }
      else
        if (JJDocOptions.isBNF ())
        {
          PGCCContext.current ().jjdoc ().setGenerator (new BNFGenerator ());
        }
        else
          if (JJDocOptions.isXText ())
          {
            PGCCContext.current ().jjdoc ().setGenerator (new XTextGenerator ());
          }
          else
          {
            if (PGCCContext.current ().jjdoc ().getGenerator () instanceof TextGenerator)
            {
              PGCCContext.current ().jjdoc ().setGenerator (new HTMLGenerator ());
            }
          }
    }
    return PGCCContext.current ().jjdoc ().getGenerator ();
  }
}
