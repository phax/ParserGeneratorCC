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
// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2005-2006, Kees Jan Koster kjkoster@kjkoster.org
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

import java.io.File;

import com.helger.base.string.StringHelper;
import com.helger.pgcc.EJDKVersion;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;

/**
 * The JJTree-specific options.
 *
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public class JJTreeOptions extends Options
{
  /**
   * Limit subclassing to derived classes.
   */
  protected JJTreeOptions ()
  {}

  /**
   * Initialize the JJTree-specific options.
   */
  public static void init ()
  {
    Options.init ();

    Options.s_optionValues.put ("MULTI", Boolean.FALSE);
    Options.s_optionValues.put ("NODE_DEFAULT_VOID", Boolean.FALSE);
    Options.s_optionValues.put ("NODE_SCOPE_HOOK", Boolean.FALSE);
    Options.s_optionValues.put ("NODE_USES_PARSER", Boolean.FALSE);
    Options.s_optionValues.put ("BUILD_NODE_FILES", Boolean.TRUE);
    Options.s_optionValues.put ("VISITOR", Boolean.FALSE);
    Options.s_optionValues.put ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME", Boolean.FALSE);
    Options.s_optionValues.put ("TRACK_TOKENS", Boolean.FALSE);

    Options.s_optionValues.put ("NODE_PREFIX", "AST");
    Options.s_optionValues.put ("NODE_PACKAGE", "");
    Options.s_optionValues.put ("NODE_EXTENDS", "");
    Options.s_optionValues.put ("NODE_CLASS", "");
    Options.s_optionValues.put ("NODE_FACTORY", "");
    Options.s_optionValues.put ("NODE_INCLUDES", "");
    Options.s_optionValues.put ("OUTPUT_FILE", "");
    Options.s_optionValues.put ("VISITOR_DATA_TYPE", "");
    Options.s_optionValues.put ("VISITOR_RETURN_TYPE", "Object");
    Options.s_optionValues.put ("VISITOR_EXCEPTION", "");

    Options.s_optionValues.put ("JJTREE_OUTPUT_DIRECTORY", "");

    // TODO :: 2013/07/23 -- This appears to be a duplicate from the parent
    // class
    Options.s_optionValues.put (Options.USEROPTION__JDK_VERSION, EJDKVersion.DEFAULT);

    // Also appears to be a duplicate
    Options.s_optionValues.put (Options.USEROPTION__CPP_NAMESPACE, "");

    // Also appears to be a duplicate
    Options.s_optionValues.put (Options.USEROPTION__CPP_IGNORE_ACTIONS, Boolean.FALSE);
  }

  /**
   * Check options for consistency
   */
  public static void validate ()
  {
    if (!isVisitor ())
    {
      if (getVisitorDataType ().length () > 0)
      {
        JavaCCErrors.warning ("VISITOR_DATA_TYPE option will be ignored since VISITOR is false");
      }
      if (getVisitorReturnType ().length () > 0 && !getVisitorReturnType ().equals ("Object"))
      {
        JavaCCErrors.warning ("VISITOR_RETURN_TYPE option will be ignored since VISITOR is false");
      }
      if (getVisitorException ().length () > 0)
      {
        JavaCCErrors.warning ("VISITOR_EXCEPTION option will be ignored since VISITOR is false");
      }
    }
  }

  /**
   * Find the multi value.
   *
   * @return The requested multi value.
   */
  public static boolean isMulti ()
  {
    return booleanValue ("MULTI");
  }

  /**
   * Find the node default void value.
   *
   * @return The requested node default void value.
   */
  public static boolean isNodeDefaultVoid ()
  {
    return booleanValue ("NODE_DEFAULT_VOID");
  }

  /**
   * Find the node scope hook value.
   *
   * @return The requested node scope hook value.
   */
  public static boolean isNodeScopeHook ()
  {
    return booleanValue ("NODE_SCOPE_HOOK");
  }

  /**
   * Find the node factory value.
   *
   * @return The requested node factory value.
   */
  public static String getNodeFactory ()
  {
    return stringValue ("NODE_FACTORY");
  }

  /**
   * Find the node uses parser value.
   *
   * @return The requested node uses parser value.
   */
  public static boolean isNodeUsesParser ()
  {
    return booleanValue ("NODE_USES_PARSER");
  }

  /**
   * Find the build node files value.
   *
   * @return The requested build node files value.
   */
  public static boolean isBuildNodeFiles ()
  {
    return booleanValue ("BUILD_NODE_FILES");
  }

  /**
   * Find the visitor value.
   *
   * @return The requested visitor value.
   */
  public static boolean isVisitor ()
  {
    return booleanValue ("VISITOR");
  }

  /**
   * Find the trackTokens value.
   *
   * @return The requested trackTokens value.
   */
  public static boolean isTrackTokens ()
  {
    return booleanValue ("TRACK_TOKENS");
  }

  /**
   * Find the node prefix value.
   *
   * @return The requested node prefix value.
   */
  public static String getNodePrefix ()
  {
    return stringValue ("NODE_PREFIX");
  }

  /**
   * Find the node super class name.
   *
   * @return The requested node super class
   */
  public static String getNodeExtends ()
  {
    return stringValue ("NODE_EXTENDS");
  }

  /**
   * Find the node class name.
   *
   * @return The requested node class
   */
  public static String getNodeClass ()
  {
    return stringValue ("NODE_CLASS");
  }

  /**
   * Find the node package value.
   *
   * @return The requested node package value.
   */
  public static String getNodePackage ()
  {
    return stringValue ("NODE_PACKAGE");
  }

  /**
   * Find the output file value.
   *
   * @return The requested output file value.
   */
  public static String getOutputFile ()
  {
    return stringValue ("OUTPUT_FILE");
  }

  /**
   * Find the visitor exception value
   *
   * @return The requested visitor exception value.
   */
  public static String getVisitorException ()
  {
    return stringValue ("VISITOR_EXCEPTION");
  }

  /**
   * Find the visitor data type value
   *
   * @return The requested visitor data type value.
   */
  public static String getVisitorDataType ()
  {
    return stringValue ("VISITOR_DATA_TYPE");
  }

  /**
   * Find the visitor return type value
   *
   * @return The requested visitor return type value.
   */
  public static String getVisitorReturnType ()
  {
    return stringValue ("VISITOR_RETURN_TYPE");
  }

  /**
   * Find the output directory to place the generated <code>.jj</code> files
   * into. If none is configured, use the value of
   * <code>getOutputDirectory()</code>.
   *
   * @return The requested JJTree output directory
   */
  public static File getJJTreeOutputDirectory ()
  {
    final String dirName = stringValue ("JJTREE_OUTPUT_DIRECTORY");
    if (StringHelper.isEmpty (dirName))
      return getOutputDirectory ();
    return new File (dirName);
  }
}
