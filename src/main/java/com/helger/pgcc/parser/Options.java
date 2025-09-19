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

/* Copyright (c) 2006, Sun Microsystems, Inc.
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
package com.helger.pgcc.parser;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsImmutableObject;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.base.system.SystemHelper;
import com.helger.pgcc.EJDKVersion;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.utils.EOptionType;
import com.helger.pgcc.utils.OptionInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A class with static state that stores all option information.
 */
public class Options
{
  /**
   * Limit subclassing to derived classes.
   */
  protected Options ()
  {}

  /**
   * These are options that are not settable by the user themselves, and that are set indirectly via
   * some configuration of user options
   */
  public static final String NONUSER_OPTION__NAMESPACE_CLOSE = "NAMESPACE_CLOSE";
  public static final String NONUSER_OPTION__HAS_NAMESPACE = "HAS_NAMESPACE";
  public static final String NONUSER_OPTION__NAMESPACE_OPEN = "NAMESPACE_OPEN";
  public static final String NONUSER_OPTION__PARSER_NAME = "PARSER_NAME";

  /**
   * Options that the user can specify from .javacc file
   */

  public static final String USEROPTION__PARSER_SUPER_CLASS = "PARSER_SUPER_CLASS";
  public static final String USEROPTION__JAVA_TEMPLATE_TYPE = "JAVA_TEMPLATE_TYPE";
  public static final String USEROPTION__GENERATE_BOILERPLATE = "GENERATE_BOILERPLATE";
  public static final String USEROPTION__OUTPUT_LANGUAGE = "OUTPUT_LANGUAGE";
  public static final String USEROPTION__PARSER_CODE_GENERATOR = "PARSER_CODE_GENERATOR";
  public static final String USEROPTION__TOKEN_MANAGER_CODE_GENERATOR = "TOKEN_MANAGER_CODE_GENERATOR";
  public static final String USEROPTION__NO_DFA = "NO_DFA";
  public static final String USEROPTION__TOKEN_MANAGER_SUPER_CLASS = "TOKEN_MANAGER_SUPER_CLASS";
  public static final String USEROPTION__LOOKAHEAD = "LOOKAHEAD";
  public static final String USEROPTION__IGNORE_CASE = "IGNORE_CASE";
  public static final String USEROPTION__UNICODE_INPUT = "UNICODE_INPUT";
  public static final String USEROPTION__JAVA_UNICODE_ESCAPE = "JAVA_UNICODE_ESCAPE";
  public static final String USEROPTION__ERROR_REPORTING = "ERROR_REPORTING";
  public static final String USEROPTION__DEBUG_TOKEN_MANAGER = "DEBUG_TOKEN_MANAGER";
  public static final String USEROPTION__DEBUG_LOOKAHEAD = "DEBUG_LOOKAHEAD";
  public static final String USEROPTION__DEBUG_PARSER = "DEBUG_PARSER";
  public static final String USEROPTION__OTHER_AMBIGUITY_CHECK = "OTHER_AMBIGUITY_CHECK";
  public static final String USEROPTION__CHOICE_AMBIGUITY_CHECK = "CHOICE_AMBIGUITY_CHECK";
  public static final String USEROPTION__CACHE_TOKENS = "CACHE_TOKENS";
  public static final String USEROPTION__COMMON_TOKEN_ACTION = "COMMON_TOKEN_ACTION";
  public static final String USEROPTION__FORCE_LA_CHECK = "FORCE_LA_CHECK";
  public static final String USEROPTION__SANITY_CHECK = "SANITY_CHECK";
  public static final String USEROPTION__TOKEN_MANAGER_USES_PARSER = "TOKEN_MANAGER_USES_PARSER";
  public static final String USEROPTION__BUILD_TOKEN_MANAGER = "BUILD_TOKEN_MANAGER";
  public static final String USEROPTION__BUILD_PARSER = "BUILD_PARSER";
  public static final String USEROPTION__USER_CHAR_STREAM = "USER_CHAR_STREAM";
  public static final String USEROPTION__USER_TOKEN_MANAGER = "USER_TOKEN_MANAGER";
  public static final String USEROPTION__JDK_VERSION = "JDK_VERSION";
  public static final String USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC = "SUPPORT_CLASS_VISIBILITY_PUBLIC";
  public static final String USEROPTION__OUTPUT_DIRECTORY = "OUTPUT_DIRECTORY";
  public static final String USEROPTION__KEEP_LINE_COLUMN = "KEEP_LINE_COLUMN";
  public static final String USEROPTION__GRAMMAR_ENCODING = "GRAMMAR_ENCODING";
  public static final String USEROPTION__OUTPUT_ENCODING = "OUTPUT_ENCODING";
  public static final String USEROPTION__TOKEN_FACTORY = "TOKEN_FACTORY";
  public static final String USEROPTION__TOKEN_EXTENDS = "TOKEN_EXTENDS";
  public static final String USEROPTION__DEPTH_LIMIT = "DEPTH_LIMIT";

  public static final String USEROPTION__CPP_NAMESPACE = "NAMESPACE";
  public static final String USEROPTION__CPP_TOKEN_INCLUDES = "TOKEN_INCLUDES";
  public static final String USEROPTION__CPP_PARSER_INCLUDES = "PARSER_INCLUDES";
  public static final String USEROPTION__CPP_IGNORE_ACTIONS = "IGNORE_ACTIONS";
  public static final String USEROPTION__CPP_TOKEN_MANAGER_INCLUDES = "TOKEN_MANAGER_INCLUDES";
  public static final String USEROPTION__CPP_TOKEN_MANAGER_SUPERCLASS = "TOKEN_MANAGER_SUPERCLASS";
  public static final String USEROPTION__CPP_STOP_ON_FIRST_ERROR = "STOP_ON_FIRST_ERROR";
  public static final String USEROPTION__CPP_STACK_LIMIT = "STACK_LIMIT";

  public static final String USEROPTION__CPP_TOKEN_INCLUDE = "TOKEN_INCLUDE";
  public static final String USEROPTION__CPP_PARSER_INCLUDE = "PARSER_INCLUDE";

  private static EOutputLanguage s_language = EOutputLanguage.JAVA;

  /**
   * 2013/07/22 -- GWT Compliant Output -- no external dependencies on GWT, but generated code adds
   * loose coupling to IO, for 6.1 release, this is opt-in, moving forward to 7.0, after thorough
   * testing, this will likely become the default option with classic being deprecated
   */
  public static final String JAVA_TEMPLATE_TYPE_MODERN = "modern";

  /**
   * The old style of Java code generation (tight coupling of code to Java IO classes - not GWT
   * compatible)
   */
  public static final String JAVA_TEMPLATE_TYPE_CLASSIC = "classic";

  private static final Set <OptionInfo> s_userOptions;

  static
  {
    final TreeSet <OptionInfo> temp = new TreeSet <> ();
    temp.add (new OptionInfo (USEROPTION__PARSER_SUPER_CLASS, EOptionType.STRING, null));
    temp.add (new OptionInfo (USEROPTION__TOKEN_MANAGER_SUPER_CLASS, EOptionType.STRING, null));
    temp.add (new OptionInfo (USEROPTION__LOOKAHEAD, EOptionType.INTEGER, Integer.valueOf (1)));

    temp.add (new OptionInfo (USEROPTION__CHOICE_AMBIGUITY_CHECK, EOptionType.INTEGER, Integer.valueOf (2)));
    temp.add (new OptionInfo (USEROPTION__OTHER_AMBIGUITY_CHECK, EOptionType.INTEGER, Integer.valueOf (1)));
    temp.add (new OptionInfo (USEROPTION__PARSER_CODE_GENERATOR, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__TOKEN_MANAGER_CODE_GENERATOR, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__NO_DFA, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__DEBUG_PARSER, EOptionType.BOOLEAN, Boolean.FALSE));

    temp.add (new OptionInfo (USEROPTION__DEBUG_LOOKAHEAD, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__DEBUG_TOKEN_MANAGER, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__ERROR_REPORTING, EOptionType.BOOLEAN, Boolean.TRUE));
    temp.add (new OptionInfo (USEROPTION__JAVA_UNICODE_ESCAPE, EOptionType.BOOLEAN, Boolean.FALSE));

    temp.add (new OptionInfo (USEROPTION__UNICODE_INPUT, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__IGNORE_CASE, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__USER_TOKEN_MANAGER, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__USER_CHAR_STREAM, EOptionType.BOOLEAN, Boolean.FALSE));

    temp.add (new OptionInfo (USEROPTION__BUILD_PARSER, EOptionType.BOOLEAN, Boolean.TRUE));
    temp.add (new OptionInfo (USEROPTION__BUILD_TOKEN_MANAGER, EOptionType.BOOLEAN, Boolean.TRUE));
    temp.add (new OptionInfo (USEROPTION__TOKEN_MANAGER_USES_PARSER, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__SANITY_CHECK, EOptionType.BOOLEAN, Boolean.TRUE));

    temp.add (new OptionInfo (USEROPTION__FORCE_LA_CHECK, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__COMMON_TOKEN_ACTION, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__CACHE_TOKENS, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__KEEP_LINE_COLUMN, EOptionType.BOOLEAN, Boolean.TRUE));

    temp.add (new OptionInfo (USEROPTION__GENERATE_BOILERPLATE, EOptionType.BOOLEAN, Boolean.TRUE));

    temp.add (new OptionInfo (USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC, EOptionType.BOOLEAN, Boolean.TRUE));
    temp.add (new OptionInfo (USEROPTION__OUTPUT_DIRECTORY, EOptionType.STRING, "."));
    temp.add (new OptionInfo (USEROPTION__JDK_VERSION, EOptionType.OTHER, EJDKVersion.DEFAULT));

    temp.add (new OptionInfo (USEROPTION__TOKEN_EXTENDS, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__TOKEN_FACTORY, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__GRAMMAR_ENCODING, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__OUTPUT_ENCODING, EOptionType.STRING, StandardCharsets.UTF_8.name ()));
    s_language = EOutputLanguage.JAVA;
    temp.add (new OptionInfo (USEROPTION__OUTPUT_LANGUAGE, EOptionType.STRING, s_language.getID ()));

    temp.add (new OptionInfo (USEROPTION__JAVA_TEMPLATE_TYPE, EOptionType.STRING, JAVA_TEMPLATE_TYPE_CLASSIC));
    temp.add (new OptionInfo (USEROPTION__CPP_NAMESPACE, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__CPP_TOKEN_INCLUDES, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__CPP_PARSER_INCLUDES, EOptionType.STRING, ""));

    temp.add (new OptionInfo (USEROPTION__CPP_TOKEN_MANAGER_INCLUDES, EOptionType.STRING, ""));
    temp.add (new OptionInfo (USEROPTION__CPP_IGNORE_ACTIONS, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__CPP_STOP_ON_FIRST_ERROR, EOptionType.BOOLEAN, Boolean.FALSE));
    temp.add (new OptionInfo (USEROPTION__CPP_TOKEN_MANAGER_SUPERCLASS, EOptionType.STRING, ""));

    temp.add (new OptionInfo (USEROPTION__DEPTH_LIMIT, EOptionType.INTEGER, Integer.valueOf (0)));
    temp.add (new OptionInfo (USEROPTION__CPP_STACK_LIMIT, EOptionType.STRING, ""));

    s_userOptions = Collections.unmodifiableSet (temp);
  }

  /**
   * A mapping of option names (Strings) to values (Integer, Boolean, String). This table is
   * initialized by the main program. Its contents defines the set of legal options. Its initial
   * values define the default option values, and the option types can be determined from these
   * values too.
   */
  protected static final Map <String, Object> s_optionValues = new HashMap <> ();

  /**
   * Initialize for JavaCC
   */
  public static void init ()
  {
    s_optionValues.clear ();
    s_cmdLineSetting.clear ();
    s_inputFileSetting.clear ();

    for (final OptionInfo t : s_userOptions)
      s_optionValues.put (t.getName (), t.getDefault ());

    s_language = EOutputLanguage.JAVA;
  }

  @Nullable
  public static Object objectValue (final String option)
  {
    return s_optionValues.get (option);
  }

  /**
   * Convenience method to retrieve integer options.
   *
   * @param option
   *        Name of the option to be retrieved. May not be <code>null</code>.
   * @return int value
   */
  public static int intValue (final String option)
  {
    return ((Integer) objectValue (option)).intValue ();
  }

  /**
   * Convenience method to retrieve boolean options.
   *
   * @param option
   *        Name of the option to be retrieved. May not be <code>null</code>.
   * @return boolean value
   */
  public static boolean booleanValue (final String option)
  {
    return ((Boolean) objectValue (option)).booleanValue ();
  }

  /**
   * Convenience method to retrieve string options.
   *
   * @param option
   *        Name of the option to be retrieved. May not be <code>null</code>.
   * @return String value
   */
  @Nullable
  public static String stringValue (final String option)
  {
    return (String) objectValue (option);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Map <String, Object> getAllOptions ()
  {
    return new HashMap <> (s_optionValues);
  }

  /**
   * Keep track of what options were set as a command line argument. We use this to see if the
   * options set from the command line and the ones set in the input files clash in any way.
   */
  private static final Set <String> s_cmdLineSetting = new HashSet <> ();

  /**
   * Keep track of what options were set from the grammar file. We use this to see if the options
   * set from the command line and the ones set in the input files clash in any way.
   */
  private static final Set <String> s_inputFileSetting = new HashSet <> ();

  /**
   * Returns a string representation of the specified options of interest. Used when, for example,
   * generating Token.java to record the JavaCC options that were used to generate the file. All of
   * the options must be boolean values.
   *
   * @param interestingOptions
   *        the options of interest, eg {Options.USEROPTION__STATIC,
   *        Options.USEROPTION__CACHE_TOKENS}
   * @return the string representation of the options, eg "STATIC=true,CACHE_TOKENS=false"
   */
  @Nonnull
  public static String getOptionsString (final String [] interestingOptions)
  {
    final StringBuilder sb = new StringBuilder ();

    for (final String key : interestingOptions)
    {
      if (sb.length () > 0)
        sb.append (',');

      sb.append (key).append ('=').append (s_optionValues.get (key));
    }

    return sb.toString ();
  }

  @Nonnull
  @Nonempty
  public static String getTokenMgrErrorClass ()
  {
    switch (s_language)
    {
      case JAVA:
        return "TokenMgrException";
      case CPP:
        return "TokenMgrError";
      default:
        throw new UnsupportedOutputLanguageException (s_language);
    }
  }

  /**
   * Determine if a given command line argument might be an option flag. Command line options start
   * with a dash&nbsp;(-).
   *
   * @param opt
   *        The command line argument to examine.
   * @return True when the argument looks like an option flag.
   */
  public static boolean isOption (final String opt)
  {
    return opt != null && opt.length () > 1 && opt.charAt (0) == '-';
  }

  /**
   * Help function to handle cases where the meaning of an option has changed over time. If the user
   * has supplied an option in the old format, it will be converted to the new format.
   *
   * @param name
   *        The name of the option being checked.
   * @param value
   *        The option's value.
   * @return The upgraded value.
   */
  @Nonnull
  private static Object _upgradeValue (@Nonnull final String name, @Nonnull final Object value)
  {
    if (name.equalsIgnoreCase ("NODE_FACTORY") && value.getClass () == Boolean.class)
    {
      return ((Boolean) value).booleanValue () ? "*" : "";
    }

    if (name.equalsIgnoreCase (USEROPTION__JDK_VERSION) &&
        (value.getClass () == String.class || value.getClass () == Integer.class))
    {
      final EJDKVersion ret = EJDKVersion.getFromStringOrNull (value.toString ());
      if (ret != null)
      {
        // Only values >= JDK 1.5 are accepted per PGCC 1.1.0
        if (ret.isNewerOrEqualsThan (EJDKVersion.JDK_1_5))
          return ret;
      }

      // Else: bad option
    }

    return value;
  }

  public static void setInputFileOption (final Object nameloc,
                                         final Object valueloc,
                                         @Nonnull final String name,
                                         @Nonnull final Object aSrcValue)
  {
    final String sNameUC = name.toUpperCase (Locale.US);
    if (!s_optionValues.containsKey (sNameUC))
    {
      JavaCCErrors.warning (nameloc, "Bad option name \"" + name + "\".  Option setting will be ignored.");
      return;
    }
    final Object aExistingValue = s_optionValues.get (sNameUC);

    final Object aRealSrc = _upgradeValue (name, aSrcValue);

    if (aExistingValue != null)
    {
      Object aObject = null;
      if (aRealSrc instanceof List)
      {
        aObject = ((List <?>) aRealSrc).get (0);
      }
      else
      {
        aObject = aRealSrc;
      }
      final boolean bIsInvalidInteger = aObject instanceof Integer && ((Integer) aRealSrc).intValue () <= 0;
      if (aExistingValue.getClass () != aObject.getClass () || bIsInvalidInteger)
      {
        JavaCCErrors.warning (valueloc,
                              "Bad option value \"" +
                                        aRealSrc +
                                        "\" for \"" +
                                        name +
                                        "\".  Option setting will be ignored.");
        return;
      }

      if (s_inputFileSetting.contains (sNameUC))
      {
        JavaCCErrors.warning (nameloc, "Duplicate option setting for \"" + name + "\" will be ignored.");
        return;
      }

      if (s_cmdLineSetting.contains (sNameUC))
      {
        if (!aExistingValue.equals (aRealSrc))
        {
          JavaCCErrors.warning (nameloc, "Command line setting of \"" + name + "\" modifies option value in file.");
        }
        return;
      }
    }

    s_optionValues.put (sNameUC, aRealSrc);
    s_inputFileSetting.add (sNameUC);

    // Special case logic block here for setting indirect flags

    if (sNameUC.equalsIgnoreCase (USEROPTION__JAVA_TEMPLATE_TYPE))
    {
      final String templateType = (String) aRealSrc;
      if (!_isValidJavaTemplateType (templateType))
      {
        JavaCCErrors.warning (valueloc,
                              "Bad option value \"" +
                                        aRealSrc +
                                        "\" for \"" +
                                        name +
                                        "\".  Option setting will be ignored. Valid options are: " +
                                        StringImplode.imploder ()
                                                     .source (s_aSupportedJavaTemplateTypes)
                                                     .separator (", ")
                                                     .build ());
      }
    }
    else
      if (sNameUC.equalsIgnoreCase (USEROPTION__OUTPUT_LANGUAGE))
      {
        final String outputLanguage = (String) aRealSrc;
        final EOutputLanguage eOutLanguage = EOutputLanguage.getFromIDCaseInsensitiveOrNull (outputLanguage);
        if (eOutLanguage == null)
        {
          JavaCCErrors.warning (valueloc,
                                "Bad option value \"" +
                                          aRealSrc +
                                          "\" for \"" +
                                          name +
                                          "\".  Option setting will be ignored. Valid options are: " +
                                          StringImplode.imploder ()
                                                       .source (EOutputLanguage.values (), EOutputLanguage::getID)
                                                       .separator (", ")
                                                       .build ());
          return;
        }
        s_language = eOutLanguage;
      }
      else
        if (sNameUC.equalsIgnoreCase (USEROPTION__CPP_NAMESPACE))
        {
          processCPPNamespaceOption ((String) aRealSrc);
        }
  }

  /**
   * Process a single command-line option. The option is parsed and stored in the optionValues map.
   *
   * @param sArg
   *        argument string to set. May not be <code>null</code>.
   */
  public static void setCmdLineOption (@Nonnull final String sArg)
  {
    final String sRealArg;
    if (sArg.charAt (0) == '-')
      sRealArg = sArg.substring (1);
    else
      sRealArg = sArg;

    final int index;
    {
      // Look for the first ":" or "=", which will separate the option name
      // from its value (if any).
      final int index1 = sRealArg.indexOf ('=');
      final int index2 = sRealArg.indexOf (':');
      if (index1 < 0)
        index = index2;
      else
        if (index2 < 0)
          index = index1;
        else
          index = Math.min (index1, index2);
    }

    String sNameUC;
    Object val;
    if (index < 0)
    {
      // No separator char (like in "DO_THIS_AND_THAT")
      sNameUC = sRealArg.toUpperCase (Locale.US);
      if (s_optionValues.containsKey (sNameUC))
      {
        val = Boolean.TRUE;
      }
      else
        if (sNameUC.length () > 2 &&
            sNameUC.charAt (0) == 'N' &&
            sNameUC.charAt (1) == 'O' &&
            s_optionValues.containsKey (sNameUC.substring (2)))
        {
          val = Boolean.FALSE;
          sNameUC = sNameUC.substring (2);
        }
        else
        {
          PGPrinter.warn ("Warning: Bad option \"" + sArg + "\" will be ignored.");
          return;
        }
    }
    else
    {
      // We have name and value as in "X=Y" or "X:Y"
      sNameUC = sRealArg.substring (0, index).toUpperCase (Locale.US);
      final String sRealValue = sRealArg.substring (index + 1);
      if (sRealValue.equalsIgnoreCase ("TRUE"))
      {
        // Boolean
        val = Boolean.TRUE;
      }
      else
        if (sRealValue.equalsIgnoreCase ("FALSE"))
        {
          // Boolean
          val = Boolean.FALSE;
        }
        else
        {
          try
          {
            // INteger?
            final int i = Integer.parseInt (sRealValue);
            if (i <= 0)
            {
              PGPrinter.warn ("Warning: Bad option value in \"" + sArg + "\" will be ignored.");
              return;
            }
            val = Integer.valueOf (i);
          }
          catch (final NumberFormatException e)
          {
            // String
            val = sRealValue;
            if (sRealValue.length () > 2)
            {
              // Check if quoted
              // i.e., there is space for two '"'s in value
              if (sRealValue.charAt (0) == '"' && sRealValue.charAt (sRealValue.length () - 1) == '"')
              {
                // remove the two '"'s.
                val = sRealValue.substring (1, sRealValue.length () - 1);
              }
            }
          }
        }
    }

    if (!s_optionValues.containsKey (sNameUC))
    {
      PGPrinter.warn ("Warning: Bad option \"" + sArg + "\" will be ignored.");
      return;
    }

    val = _upgradeValue (sNameUC, val);

    final Object valOrig = s_optionValues.get (sNameUC);
    if (val.getClass () != valOrig.getClass ())
    {
      PGPrinter.warn ("Warning: Bad option value in \"" + sArg + "\" will be ignored.");
      return;
    }
    if (s_cmdLineSetting.contains (sNameUC))
    {
      PGPrinter.warn ("Warning: Duplicate option setting \"" + sArg + "\" will be ignored.");
      return;
    }

    s_optionValues.put (sNameUC, val);
    s_cmdLineSetting.add (sNameUC);
    if (sNameUC.equalsIgnoreCase (USEROPTION__CPP_NAMESPACE))
    {
      processCPPNamespaceOption ((String) val);
    }
  }

  public static void normalize ()
  {
    if (isDebugLookahead () && !isDebugParser ())
    {
      if (s_cmdLineSetting.contains (USEROPTION__DEBUG_PARSER) ||
          s_inputFileSetting.contains (USEROPTION__DEBUG_PARSER))
      {
        JavaCCErrors.warning ("True setting of option DEBUG_LOOKAHEAD overrides " +
                              "false setting of option DEBUG_PARSER.");
      }
      s_optionValues.put (USEROPTION__DEBUG_PARSER, Boolean.TRUE);
    }
  }

  /**
   * Find the lookahead setting.
   *
   * @return The requested lookahead value.
   */
  public static int getLookahead ()
  {
    return intValue (USEROPTION__LOOKAHEAD);
  }

  /**
   * Find the choice ambiguity check value.
   *
   * @return The requested choice ambiguity check value.
   */
  public static int getChoiceAmbiguityCheck ()
  {
    return intValue (USEROPTION__CHOICE_AMBIGUITY_CHECK);
  }

  /**
   * Find the other ambiguity check value.
   *
   * @return The requested other ambiguity check value.
   */
  public static int getOtherAmbiguityCheck ()
  {
    return intValue (USEROPTION__OTHER_AMBIGUITY_CHECK);
  }

  @Nullable
  public static String getParserCodeGenerator ()
  {
    final String retVal = stringValue (USEROPTION__PARSER_CODE_GENERATOR);
    return StringHelper.isEmpty (retVal) ? null : retVal;
  }

  @Nullable
  public static String getTokenManagerCodeGenerator ()
  {
    final String retVal = stringValue (USEROPTION__TOKEN_MANAGER_CODE_GENERATOR);
    return StringHelper.isEmpty (retVal) ? null : retVal;
  }

  public static boolean isNoDfa ()
  {
    return booleanValue (USEROPTION__NO_DFA);
  }

  /**
   * Find the debug parser value.
   *
   * @return The requested debug parser value.
   */
  public static boolean isDebugParser ()
  {
    return booleanValue (USEROPTION__DEBUG_PARSER);
  }

  /**
   * Find the debug lookahead value.
   *
   * @return The requested debug lookahead value.
   */
  public static boolean isDebugLookahead ()
  {
    return booleanValue (USEROPTION__DEBUG_LOOKAHEAD);
  }

  /**
   * Find the debug tokenmanager value.
   *
   * @return The requested debug tokenmanager value.
   */
  public static boolean isDebugTokenManager ()
  {
    return booleanValue (USEROPTION__DEBUG_TOKEN_MANAGER);
  }

  /**
   * Find the error reporting value.
   *
   * @return The requested error reporting value.
   */
  public static boolean isErrorReporting ()
  {
    return booleanValue (USEROPTION__ERROR_REPORTING);
  }

  /**
   * Find the Java unicode escape value.
   *
   * @return The requested Java unicode escape value.
   */
  public static boolean isJavaUnicodeEscape ()
  {
    return booleanValue (USEROPTION__JAVA_UNICODE_ESCAPE);
  }

  /**
   * Find the unicode input value.
   *
   * @return The requested unicode input value.
   */
  public static boolean isUnicodeInput ()
  {
    return booleanValue (USEROPTION__UNICODE_INPUT);
  }

  /**
   * Find the ignore case value.
   *
   * @return The requested ignore case value.
   */
  public static boolean isIgnoreCase ()
  {
    return booleanValue (USEROPTION__IGNORE_CASE);
  }

  /**
   * Find the user tokenmanager value.
   *
   * @return The requested user tokenmanager value.
   */
  public static boolean isUserTokenManager ()
  {
    return booleanValue (USEROPTION__USER_TOKEN_MANAGER);
  }

  /**
   * Find the user charstream value.
   *
   * @return The requested user charstream value.
   */
  public static boolean isJavaUserCharStream ()
  {
    return booleanValue (USEROPTION__USER_CHAR_STREAM);
  }

  /**
   * Find the build parser value.
   *
   * @return The requested build parser value.
   */
  public static boolean isBuildParser ()
  {
    return booleanValue (USEROPTION__BUILD_PARSER);
  }

  /**
   * Find the build token manager value.
   *
   * @return The requested build token manager value.
   */
  public static boolean isBuildTokenManager ()
  {
    return booleanValue (USEROPTION__BUILD_TOKEN_MANAGER);
  }

  /**
   * Find the token manager uses parser value.
   *
   * @return The requested token manager uses parser value;
   */
  public static boolean isTokenManagerUsesParser ()
  {
    return booleanValue (USEROPTION__TOKEN_MANAGER_USES_PARSER);
  }

  /**
   * Find the sanity check value.
   *
   * @return The requested sanity check value.
   */
  public static boolean isSanityCheck ()
  {
    return booleanValue (USEROPTION__SANITY_CHECK);
  }

  /**
   * Find the force lookahead check value.
   *
   * @return The requested force lookahead value.
   */
  public static boolean isForceLaCheck ()
  {
    return booleanValue (USEROPTION__FORCE_LA_CHECK);
  }

  /**
   * Find the common token action value.
   *
   * @return The requested common token action value.
   */

  public static boolean isCommonTokenAction ()
  {
    return booleanValue (USEROPTION__COMMON_TOKEN_ACTION);
  }

  /**
   * Find the cache tokens value.
   *
   * @return The requested cache tokens value.
   */
  public static boolean isCacheTokens ()
  {
    return booleanValue (USEROPTION__CACHE_TOKENS);
  }

  /**
   * Find the keep line column value.
   *
   * @return The requested keep line column value.
   */
  public static boolean isKeepLineColumn ()
  {
    return booleanValue (USEROPTION__KEEP_LINE_COLUMN);
  }

  /**
   * Find the JDK version.
   *
   * @return The requested jdk version.
   */
  public static EJDKVersion getJdkVersion ()
  {
    return (EJDKVersion) objectValue (USEROPTION__JDK_VERSION);
  }

  public static boolean isGenerateJavaBoilerplateCode ()
  {
    return booleanValue (USEROPTION__GENERATE_BOILERPLATE);
  }

  /**
   * Should the generated code class visibility public?
   *
   * @return <code>true</code> for public visibility
   */
  public static boolean isJavaSupportClassVisibilityPublic ()
  {
    return booleanValue (USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC);
  }

  /**
   * Return the Token's superclass.
   *
   * @return The required base class for Token.
   */
  public static String getTokenExtends ()
  {
    return stringValue (USEROPTION__TOKEN_EXTENDS);
  }

  // public static String getBoilerplatePackage()
  // {
  // return stringValue(BOILERPLATE_PACKAGE);
  // }

  /**
   * Return the Token's factory class.
   *
   * @return The required factory class for Token.
   */
  public static String getTokenFactory ()
  {
    return stringValue (USEROPTION__TOKEN_FACTORY);
  }

  /**
   * Return the file encoding for reading grammars; this will return the file.encoding system
   * property if no value was explicitly set
   *
   * @return The file encoding (e.g., UTF-8, ISO_8859-1, MacRoman)
   */
  @Nonnull
  public static Charset getGrammarEncoding ()
  {
    final String sValue = stringValue (USEROPTION__GRAMMAR_ENCODING);
    if (StringHelper.isNotEmpty (sValue))
      try
      {
        return Charset.forName (sValue);
      }
      catch (final UnsupportedCharsetException ex)
      {
        // Fall through
        JavaCCErrors.warning ("The grammar encoding value '" + sValue + "' is invalid. Falling back to default.");
      }
    return SystemHelper.getSystemCharset ();
  }

  /**
   * Return the file encoding for reading grammars; this will return the UTF-8 if no value was
   * explicitly set
   *
   * @return The output encoding
   */
  @Nonnull
  public static Charset getOutputEncoding ()
  {
    final String sValue = stringValue (USEROPTION__OUTPUT_ENCODING);
    if (StringHelper.isNotEmpty (sValue))
      try
      {
        return Charset.forName (sValue);
      }
      catch (final UnsupportedCharsetException ex)
      {
        // Fall through
        JavaCCErrors.warning ("The output encoding value '" + sValue + "' is invalid. Falling back to default.");
      }
    return StandardCharsets.UTF_8;
  }

  /**
   * Find the output directory.
   *
   * @return The requested output directory.
   */
  public static File getOutputDirectory ()
  {
    return new File (stringValue (USEROPTION__OUTPUT_DIRECTORY));
  }

  private static final Set <String> s_aSupportedJavaTemplateTypes = new HashSet <> ();
  static
  {
    s_aSupportedJavaTemplateTypes.add (JAVA_TEMPLATE_TYPE_CLASSIC);
    s_aSupportedJavaTemplateTypes.add (JAVA_TEMPLATE_TYPE_MODERN);
  }

  private static boolean _isValidJavaTemplateType (@Nullable final String sType)
  {
    return sType == null ? false : s_aSupportedJavaTemplateTypes.contains (sType.toLowerCase (Locale.US));
  }

  @Nonnull
  public static EOutputLanguage getOutputLanguage ()
  {
    return s_language;
  }

  public static String getJavaTemplateType ()
  {
    return stringValue (USEROPTION__JAVA_TEMPLATE_TYPE);
  }

  public static void setStringOption (final String optionName, final String optionValue)
  {
    s_optionValues.put (optionName, optionValue);
    if (optionName.equalsIgnoreCase (USEROPTION__CPP_NAMESPACE))
    {
      processCPPNamespaceOption (optionValue);
    }
  }

  public static void processCPPNamespaceOption (final String optionValue)
  {
    final String ns = optionValue;
    if (ns.length () > 0)
    {
      // We also need to split it.
      final StringTokenizer st = new StringTokenizer (ns, "::");
      String expanded_ns = st.nextToken () + " {";
      String ns_close = "}";
      while (st.hasMoreTokens ())
      {
        expanded_ns = expanded_ns + "\nnamespace " + st.nextToken () + " {";
        ns_close = ns_close + "\n}";
      }
      s_optionValues.put (NONUSER_OPTION__NAMESPACE_OPEN, expanded_ns);
      s_optionValues.put (NONUSER_OPTION__HAS_NAMESPACE, Boolean.TRUE);
      s_optionValues.put (NONUSER_OPTION__NAMESPACE_CLOSE, ns_close);
    }
  }

  public static boolean isTokenManagerRequiresParserAccess ()
  {
    return isTokenManagerUsesParser ();
  }

  /**
   * Get defined parser recursion depth limit.
   *
   * @return The requested recursion limit.
   */
  public static int getDepthLimit ()
  {
    return intValue (USEROPTION__DEPTH_LIMIT);
  }

  public static boolean hasDepthLimit ()
  {
    return getDepthLimit () > 0;
  }

  /**
   * Get defined parser stack usage limit.
   *
   * @return The requested stack usage limit.
   */
  public static String getCPPStackLimit ()
  {
    final String limit = stringValue (USEROPTION__CPP_STACK_LIMIT);
    if (limit.equals ("0"))
      return "";
    return limit;
  }

  public static boolean hasCPPStackLimit ()
  {
    return StringHelper.isNotEmpty (getCPPStackLimit ());
  }

  /**
   * Gets all the user options (in order)
   *
   * @return all user options
   */
  @Nonnull
  @ReturnsImmutableObject
  public static Set <OptionInfo> getUserOptions ()
  {
    return s_userOptions;
  }
}
