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
package com.helger.pgcc.parser;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsImmutableObject;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.base.system.EJavaVersion;
import com.helger.base.system.SystemHelper;
import com.helger.pgcc.JavaVersionHelper;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.context.PGCCContext;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.utils.EOptionType;
import com.helger.pgcc.utils.OptionInfo;

/**
 * A class with static state that stores all option information.
 */
public class Options
{
  /**
   * Not settable in the grammar: the closing braces of the C++ namespace, derived from
   * {@link #USEROPTION__CPP_NAMESPACE} by {@link #processCppNamespaceOption(String)}.
   */
  public static final String NONUSER_OPTION__NAMESPACE_CLOSE = "NAMESPACE_CLOSE";

  /**
   * Not settable in the grammar: whether a C++ namespace was requested at all, derived from
   * {@link #USEROPTION__CPP_NAMESPACE} by {@link #processCppNamespaceOption(String)}.
   */
  public static final String NONUSER_OPTION__HAS_NAMESPACE = "HAS_NAMESPACE";

  /**
   * Not settable in the grammar: the opening declarations of the C++ namespace, derived from
   * {@link #USEROPTION__CPP_NAMESPACE} by {@link #processCppNamespaceOption(String)}.
   */
  public static final String NONUSER_OPTION__NAMESPACE_OPEN = "NAMESPACE_OPEN";

  /**
   * Not settable in the grammar: the parser name, put here by Main so that the templates can
   * substitute it.
   */
  public static final String NONUSER_OPTION__PARSER_NAME = "PARSER_NAME";

  /**
   * Options that the user can specify from .javacc file
   */

  /**
   * The {@value} grammar option, read by ParseGenCpp.
   */
  public static final String USEROPTION__PARSER_SUPER_CLASS = "PARSER_SUPER_CLASS";

  /**
   * The {@value} grammar option, read by {@link #getJavaTemplateType()}.
   */
  public static final String USEROPTION__JAVA_TEMPLATE_TYPE = "JAVA_TEMPLATE_TYPE";

  /**
   * The {@value} grammar option, read by {@link #getJavaCharStreamType()}.
   */
  public static final String USEROPTION__JAVA_CHAR_STREAM_TYPE = "JAVA_CHAR_STREAM_TYPE";

  /**
   * The {@value} grammar option, read by {@link #isGenerateJavaBoilerplateCode()}.
   */
  public static final String USEROPTION__GENERATE_BOILERPLATE = "GENERATE_BOILERPLATE";

  /**
   * The {@value} grammar option. It is turned into an {@link EOutputLanguage} while the option is
   * being set, so nothing reads it back by name.
   */
  public static final String USEROPTION__OUTPUT_LANGUAGE = "OUTPUT_LANGUAGE";

  /**
   * The {@value} grammar option, read by {@link #isNoDfa()}.
   */
  public static final String USEROPTION__NO_DFA = "NO_DFA";

  /**
   * The {@value} grammar option, read by LexGenCpp.
   */
  public static final String USEROPTION__TOKEN_MANAGER_SUPER_CLASS = "TOKEN_MANAGER_SUPER_CLASS";

  /**
   * The {@value} grammar option, read by {@link #getLookahead()}.
   */
  public static final String USEROPTION__LOOKAHEAD = "LOOKAHEAD";

  /**
   * The {@value} grammar option, read by {@link #isIgnoreCase()}.
   */
  public static final String USEROPTION__IGNORE_CASE = "IGNORE_CASE";

  /**
   * The {@value} grammar option, read by {@link #isUnicodeInput()}.
   */
  public static final String USEROPTION__UNICODE_INPUT = "UNICODE_INPUT";

  /**
   * The {@value} grammar option, read by {@link #isJavaUnicodeEscape()}.
   */
  public static final String USEROPTION__JAVA_UNICODE_ESCAPE = "JAVA_UNICODE_ESCAPE";

  /**
   * The {@value} grammar option, read by {@link #isErrorReporting()}.
   */
  public static final String USEROPTION__ERROR_REPORTING = "ERROR_REPORTING";

  /**
   * The {@value} grammar option, read by {@link #isDebugTokenManager()}.
   */
  public static final String USEROPTION__DEBUG_TOKEN_MANAGER = "DEBUG_TOKEN_MANAGER";

  /**
   * The {@value} grammar option, read by {@link #isDebugLookahead()}.
   */
  public static final String USEROPTION__DEBUG_LOOKAHEAD = "DEBUG_LOOKAHEAD";

  /**
   * The {@value} grammar option, read by {@link #isDebugParser()}.
   */
  public static final String USEROPTION__DEBUG_PARSER = "DEBUG_PARSER";

  /**
   * The {@value} grammar option, read by {@link #getOtherAmbiguityCheck()}.
   */
  public static final String USEROPTION__OTHER_AMBIGUITY_CHECK = "OTHER_AMBIGUITY_CHECK";

  /**
   * The {@value} grammar option, read by {@link #getChoiceAmbiguityCheck()}.
   */
  public static final String USEROPTION__CHOICE_AMBIGUITY_CHECK = "CHOICE_AMBIGUITY_CHECK";

  /**
   * The {@value} grammar option, read by {@link #isCacheTokens()}.
   */
  public static final String USEROPTION__CACHE_TOKENS = "CACHE_TOKENS";

  /**
   * The {@value} grammar option, read by {@link #isCommonTokenAction()}.
   */
  public static final String USEROPTION__COMMON_TOKEN_ACTION = "COMMON_TOKEN_ACTION";

  /**
   * The {@value} grammar option, read by {@link #isForceLaCheck()}.
   */
  public static final String USEROPTION__FORCE_LA_CHECK = "FORCE_LA_CHECK";

  /**
   * The {@value} grammar option, read by {@link #isSanityCheck()}.
   */
  public static final String USEROPTION__SANITY_CHECK = "SANITY_CHECK";

  /**
   * The {@value} grammar option, read by {@link #isTokenManagerUsesParser()}.
   */
  public static final String USEROPTION__TOKEN_MANAGER_USES_PARSER = "TOKEN_MANAGER_USES_PARSER";

  /**
   * The {@value} grammar option, read by {@link #isBuildTokenManager()}.
   */
  public static final String USEROPTION__BUILD_TOKEN_MANAGER = "BUILD_TOKEN_MANAGER";

  /**
   * The {@value} grammar option, read by {@link #isBuildParser()}.
   */
  public static final String USEROPTION__BUILD_PARSER = "BUILD_PARSER";

  /**
   * The {@value} grammar option, read by {@link #isJavaUserCharStream()}.
   */
  public static final String USEROPTION__USER_CHAR_STREAM = "USER_CHAR_STREAM";

  /**
   * The {@value} grammar option, read by {@link #isUserTokenManager()}.
   */
  public static final String USEROPTION__USER_TOKEN_MANAGER = "USER_TOKEN_MANAGER";

  /**
   * The {@value} grammar option, read by {@link #getJdkVersion()}.
   */
  public static final String USEROPTION__JDK_VERSION = "JDK_VERSION";

  /**
   * The {@value} grammar option, read by {@link #isJavaSupportClassVisibilityPublic()}.
   */
  public static final String USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC = "SUPPORT_CLASS_VISIBILITY_PUBLIC";

  /**
   * The {@value} grammar option, read by {@link #getOutputDirectory()}.
   */
  public static final String USEROPTION__OUTPUT_DIRECTORY = "OUTPUT_DIRECTORY";

  /**
   * The {@value} grammar option, read by {@link #isKeepLineColumn()}.
   */
  public static final String USEROPTION__KEEP_LINE_COLUMN = "KEEP_LINE_COLUMN";

  /**
   * The {@value} grammar option, read by {@link #getGrammarEncoding()}.
   */
  public static final String USEROPTION__GRAMMAR_ENCODING = "GRAMMAR_ENCODING";

  /**
   * The {@value} grammar option, read by {@link #getOutputEncoding()}.
   */
  public static final String USEROPTION__OUTPUT_ENCODING = "OUTPUT_ENCODING";

  /**
   * The {@value} grammar option, read by {@link #getTokenFactory()}.
   */
  public static final String USEROPTION__TOKEN_FACTORY = "TOKEN_FACTORY";

  /**
   * The {@value} grammar option, read by {@link #getTokenExtends()}.
   */
  public static final String USEROPTION__TOKEN_EXTENDS = "TOKEN_EXTENDS";

  /**
   * The {@value} grammar option, read by {@link #getDepthLimit()}.
   */
  public static final String USEROPTION__DEPTH_LIMIT = "DEPTH_LIMIT";

  /**
   * The {@value} grammar option, C++ only. Setting it fills in the three
   * <code>NONUSER_OPTION__NAMESPACE...</code> entries, which is what the templates read.
   */
  public static final String USEROPTION__CPP_NAMESPACE = "NAMESPACE";

  /**
   * The {@value} grammar option, read by FilesCpp.
   */
  public static final String USEROPTION__CPP_TOKEN_INCLUDES = "TOKEN_INCLUDES";

  /**
   * The {@value} grammar option, read by ParseGenCpp.
   */
  public static final String USEROPTION__CPP_PARSER_INCLUDES = "PARSER_INCLUDES";

  /**
   * The {@value} grammar option, read by ParseEngine.
   */
  public static final String USEROPTION__CPP_IGNORE_ACTIONS = "IGNORE_ACTIONS";

  /**
   * The {@value} grammar option, read by LexGenCpp.
   */
  public static final String USEROPTION__CPP_TOKEN_MANAGER_INCLUDES = "TOKEN_MANAGER_INCLUDES";

  /**
   * The {@value} grammar option, C++ only. Accepted and then ignored - nothing reads it. The option
   * that does have an effect is {@link #USEROPTION__TOKEN_MANAGER_SUPER_CLASS}.
   */
  public static final String USEROPTION__CPP_TOKEN_MANAGER_SUPERCLASS = "TOKEN_MANAGER_SUPERCLASS";

  /**
   * The {@value} grammar option, read by ParseEngine.
   */
  public static final String USEROPTION__CPP_STOP_ON_FIRST_ERROR = "STOP_ON_FIRST_ERROR";

  /**
   * The {@value} grammar option, read by {@link #getCppStackLimit()}.
   */
  public static final String USEROPTION__CPP_STACK_LIMIT = "STACK_LIMIT";

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

  /**
   * The classic char stream that reads the input into an internal buffer that needs to be relocated
   * and expanded for tokens that are larger than the buffer.
   */
  public static final String JAVA_CHAR_STREAM_TYPE_SIMPLE = "simple";

  /**
   * A char stream that keeps the whole input in memory and reads directly from it. Token length is
   * therefore not limited by a buffer size. Not available with JAVA_UNICODE_ESCAPE.
   */
  public static final String JAVA_CHAR_STREAM_TYPE_CHARSEQUENCE = "charsequence";

  /**
   * The default value of the <code>JDK_VERSION</code> option. Moved from 1.5 to 1.8 in v2.0.4, so
   * that generated code uses the Charset based constructors and the diamond operator by default.
   */
  public static final EJavaVersion DEFAULT_JDK_VERSION = EJavaVersion.JDK_1_8;

  private static final Set <String> SUPPORTED_JAVA_TEMPLATE_TYPES = Set.of (JAVA_TEMPLATE_TYPE_CLASSIC,
                                                                            JAVA_TEMPLATE_TYPE_MODERN);

  private static final Set <String> SUPPORTED_JAVA_CHAR_STREAM_TYPES = Set.of (JAVA_CHAR_STREAM_TYPE_SIMPLE,
                                                                               JAVA_CHAR_STREAM_TYPE_CHARSEQUENCE);

  private static final Set <OptionInfo> USER_OPTIONS;

  static
  {
    final TreeSet <OptionInfo> aTemp = new TreeSet <> ();
    aTemp.add (new OptionInfo (USEROPTION__PARSER_SUPER_CLASS, EOptionType.STRING, null));
    aTemp.add (new OptionInfo (USEROPTION__TOKEN_MANAGER_SUPER_CLASS, EOptionType.STRING, null));
    aTemp.add (new OptionInfo (USEROPTION__LOOKAHEAD, EOptionType.INTEGER, Integer.valueOf (1)));

    aTemp.add (new OptionInfo (USEROPTION__CHOICE_AMBIGUITY_CHECK, EOptionType.INTEGER, Integer.valueOf (2)));
    aTemp.add (new OptionInfo (USEROPTION__OTHER_AMBIGUITY_CHECK, EOptionType.INTEGER, Integer.valueOf (1)));
    aTemp.add (new OptionInfo (USEROPTION__NO_DFA, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__DEBUG_PARSER, EOptionType.BOOLEAN, Boolean.FALSE));

    aTemp.add (new OptionInfo (USEROPTION__DEBUG_LOOKAHEAD, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__DEBUG_TOKEN_MANAGER, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__ERROR_REPORTING, EOptionType.BOOLEAN, Boolean.TRUE));
    aTemp.add (new OptionInfo (USEROPTION__JAVA_UNICODE_ESCAPE, EOptionType.BOOLEAN, Boolean.FALSE));

    aTemp.add (new OptionInfo (USEROPTION__UNICODE_INPUT, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__IGNORE_CASE, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__USER_TOKEN_MANAGER, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__USER_CHAR_STREAM, EOptionType.BOOLEAN, Boolean.FALSE));

    aTemp.add (new OptionInfo (USEROPTION__BUILD_PARSER, EOptionType.BOOLEAN, Boolean.TRUE));
    aTemp.add (new OptionInfo (USEROPTION__BUILD_TOKEN_MANAGER, EOptionType.BOOLEAN, Boolean.TRUE));
    aTemp.add (new OptionInfo (USEROPTION__TOKEN_MANAGER_USES_PARSER, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__SANITY_CHECK, EOptionType.BOOLEAN, Boolean.TRUE));

    aTemp.add (new OptionInfo (USEROPTION__FORCE_LA_CHECK, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__COMMON_TOKEN_ACTION, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__CACHE_TOKENS, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__KEEP_LINE_COLUMN, EOptionType.BOOLEAN, Boolean.TRUE));

    aTemp.add (new OptionInfo (USEROPTION__GENERATE_BOILERPLATE, EOptionType.BOOLEAN, Boolean.TRUE));

    aTemp.add (new OptionInfo (USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC, EOptionType.BOOLEAN, Boolean.TRUE));
    aTemp.add (new OptionInfo (USEROPTION__OUTPUT_DIRECTORY, EOptionType.STRING, "."));
    aTemp.add (new OptionInfo (USEROPTION__JDK_VERSION, EOptionType.OTHER, DEFAULT_JDK_VERSION));

    aTemp.add (new OptionInfo (USEROPTION__TOKEN_EXTENDS, EOptionType.STRING, ""));
    aTemp.add (new OptionInfo (USEROPTION__TOKEN_FACTORY, EOptionType.STRING, ""));
    aTemp.add (new OptionInfo (USEROPTION__GRAMMAR_ENCODING, EOptionType.STRING, ""));
    aTemp.add (new OptionInfo (USEROPTION__OUTPUT_ENCODING, EOptionType.STRING, StandardCharsets.UTF_8.name ()));
    PGCCContext.current ().options ().setLanguage (EOutputLanguage.JAVA);
    aTemp.add (new OptionInfo (USEROPTION__OUTPUT_LANGUAGE,
                               EOptionType.STRING,
                               PGCCContext.current ().options ().getLanguage ().getID ()));

    aTemp.add (new OptionInfo (USEROPTION__JAVA_TEMPLATE_TYPE, EOptionType.STRING, JAVA_TEMPLATE_TYPE_CLASSIC));
    aTemp.add (new OptionInfo (USEROPTION__JAVA_CHAR_STREAM_TYPE, EOptionType.STRING, JAVA_CHAR_STREAM_TYPE_SIMPLE));
    aTemp.add (new OptionInfo (USEROPTION__CPP_NAMESPACE, EOptionType.STRING, ""));
    aTemp.add (new OptionInfo (USEROPTION__CPP_TOKEN_INCLUDES, EOptionType.STRING, ""));
    aTemp.add (new OptionInfo (USEROPTION__CPP_PARSER_INCLUDES, EOptionType.STRING, ""));

    aTemp.add (new OptionInfo (USEROPTION__CPP_TOKEN_MANAGER_INCLUDES, EOptionType.STRING, ""));
    aTemp.add (new OptionInfo (USEROPTION__CPP_IGNORE_ACTIONS, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__CPP_STOP_ON_FIRST_ERROR, EOptionType.BOOLEAN, Boolean.FALSE));
    aTemp.add (new OptionInfo (USEROPTION__CPP_TOKEN_MANAGER_SUPERCLASS, EOptionType.STRING, ""));

    aTemp.add (new OptionInfo (USEROPTION__DEPTH_LIMIT, EOptionType.INTEGER, Integer.valueOf (0)));
    aTemp.add (new OptionInfo (USEROPTION__CPP_STACK_LIMIT, EOptionType.STRING, ""));

    USER_OPTIONS = Collections.unmodifiableSet (aTemp);
  }

  /**
   * Limit subclassing to derived classes.
   */
  protected Options ()
  {}

  /**
   * A mapping of option names (Strings) to values (Integer, Boolean, String). This table is
   * initialized by the main program. Its contents defines the set of legal options. Its initial
   * values define the default option values, and the option types can be determined from these
   * values too.
   */
  /**
   * The option values of the current run, the map that every accessor here reads from.
   *
   * @return The live map, not a copy. Never <code>null</code>.
   */
  @NonNull
  protected static Map <String, Object> optionValues ()
  {
    return PGCCContext.current ().options ().values ();
  }

  /**
   * Initialize for JavaCC
   */
  public static void init ()
  {
    optionValues ().clear ();
    cmdLineSetting ().clear ();
    inputFileSetting ().clear ();

    for (final OptionInfo t : USER_OPTIONS)
      optionValues ().put (t.name (), t.defaultValue ());

    PGCCContext.current ().options ().setLanguage (EOutputLanguage.JAVA);
  }

  /**
   * The raw value of one option, whatever type it was stored with.
   *
   * @param sOption
   *        The option name. May be <code>null</code>.
   * @return <code>null</code> if the option is unknown.
   */
  @Nullable
  public static Object objectValue (final String sOption)
  {
    return optionValues ().get (sOption);
  }

  /**
   * Convenience method to retrieve integer options.
   *
   * @param sOption
   *        Name of the option to be retrieved. May not be <code>null</code>.
   * @return int value
   */
  public static int intValue (final String sOption)
  {
    return ((Integer) objectValue (sOption)).intValue ();
  }

  /**
   * Convenience method to retrieve boolean options.
   *
   * @param sOption
   *        Name of the option to be retrieved. May not be <code>null</code>.
   * @return boolean value
   */
  public static boolean booleanValue (final String sOption)
  {
    return ((Boolean) objectValue (sOption)).booleanValue ();
  }

  /**
   * Convenience method to retrieve string options.
   *
   * @param sOption
   *        Name of the option to be retrieved. May not be <code>null</code>.
   * @return String value
   */
  @Nullable
  public static String stringValue (final String sOption)
  {
    return (String) objectValue (sOption);
  }

  /**
   * Every option of the current run, for the code that has to hand the whole set to a template.
   *
   * @return A copy of the option map. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static Map <String, Object> getAllOptions ()
  {
    return new HashMap <> (optionValues ());
  }

  /**
   * Keep track of what options were set as a command line argument. We use this to see if the
   * options set from the command line and the ones set in the input files clash in any way.
   */
  @NonNull
  private static Set <String> cmdLineSetting ()
  {
    return PGCCContext.current ().options ().cmdLineSet ();
  }

  /**
   * Keep track of what options were set from the grammar file. We use this to see if the options
   * set from the command line and the ones set in the input files clash in any way.
   */
  @NonNull
  private static Set <String> inputFileSetting ()
  {
    return PGCCContext.current ().options ().inputFileSet ();
  }

  /**
   * Returns a string representation of the specified options of interest. Used when, for example,
   * generating Token.java to record the JavaCC options that were used to generate the file. All of
   * the options must be boolean values.
   *
   * @param aInterestingOptions
   *        the options of interest, eg {Options.USEROPTION__KEEP_LINE_COLUMN,
   *        Options.USEROPTION__CACHE_TOKENS}
   * @return the string representation of the options, eg "KEEP_LINE_COLUMN=true,CACHE_TOKENS=false"
   */
  @NonNull
  public static String getOptionsString (final String [] aInterestingOptions)
  {
    final StringBuilder aSB = new StringBuilder ();

    for (final String key : aInterestingOptions)
    {
      if (aSB.length () > 0)
        aSB.append (',');

      aSB.append (key).append ('=').append (optionValues ().get (key));
    }

    return aSB.toString ();
  }

  /**
   * The class the generated token manager throws when it cannot match. The two languages call it
   * differently, which is why this is not a plain option lookup.
   *
   * @return The class name. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public static String getTokenMgrErrorClass ()
  {
    return switch (PGCCContext.current ().options ().getLanguage ())
    {
      case JAVA -> "TokenMgrException";
      case CPP -> "TokenMgrError";
    };
  }

  /**
   * Determine if a given command line argument might be an option flag. Command line options start
   * with a dash&nbsp;(-).
   *
   * @param sOpt
   *        The command line argument to examine.
   * @return True when the argument looks like an option flag.
   */
  public static boolean isOption (@Nullable final String sOpt)
  {
    return sOpt != null && sOpt.length () > 1 && sOpt.charAt (0) == '-';
  }

  /**
   * Help function to handle cases where the meaning of an option has changed over time. If the user
   * has supplied an option in the old format, it will be converted to the new format.
   *
   * @param sName
   *        The name of the option being checked.
   * @param aValue
   *        The option's value.
   * @return The upgraded value.
   */
  @NonNull
  private static Object _upgradeValue (@NonNull final String sName, @NonNull final Object aValue)
  {
    if (sName.equalsIgnoreCase ("NODE_FACTORY") && aValue.getClass () == Boolean.class)
    {
      return ((Boolean) aValue).booleanValue () ? "*" : "";
    }

    if (sName.equalsIgnoreCase (USEROPTION__JDK_VERSION) &&
        (aValue.getClass () == String.class || aValue.getClass () == Integer.class))
    {
      final EJavaVersion eRet = JavaVersionHelper.getFromStringOrNull (aValue.toString ());
      if (eRet != null)
      {
        // Only values >= JDK 1.5 are accepted per PGCC 1.1.0
        if (eRet.isNewerOrEqualsThan (EJavaVersion.JDK_1_5))
          return eRet;
      }

      // Else: bad option
    }

    return aValue;
  }

  /**
   * Apply one option from the grammar file, warning about anything the command line already set,
   * anything unknown and anything of the wrong type.
   *
   * @param aNameloc
   *        Where the option name stands in the grammar, for the warning. May be <code>null</code>.
   * @param aValueloc
   *        Where the value stands in the grammar, for the warning. May be <code>null</code>.
   * @param sName
   *        The option name, in any casing. May not be <code>null</code>.
   * @param aSrcValue
   *        The value as the grammar parser produced it. May not be <code>null</code>.
   */
  public static void setInputFileOption (@Nullable final IGrammarLocation aNameloc,
                                         @Nullable final IGrammarLocation aValueloc,
                                         @NonNull final String sName,
                                         @NonNull final Object aSrcValue)
  {
    final String sNameUC = sName.toUpperCase (Locale.US);
    if (!optionValues ().containsKey (sNameUC))
    {
      JavaCCErrors.warning (aNameloc, "Bad option name \"" + sName + "\".  Option setting will be ignored.");
      return;
    }

    final Object aExistingValue = optionValues ().get (sNameUC);
    final Object aRealSrc = _upgradeValue (sName, aSrcValue);

    if (aExistingValue != null)
    {
      final Object aObject;
      if (aRealSrc instanceof final List <?> aRealSrcList)
        aObject = aRealSrcList.get (0);
      else
        aObject = aRealSrc;

      final boolean bIsInvalidInteger = aObject instanceof final Integer aInteger && aInteger.intValue () <= 0;
      if (aExistingValue.getClass () != aObject.getClass () || bIsInvalidInteger)
      {
        JavaCCErrors.warning (aValueloc,
                              "Bad option value \"" +
                                         aRealSrc +
                                         "\" for \"" +
                                         sName +
                                         "\". Option setting will be ignored.");
        return;
      }

      if (inputFileSetting ().contains (sNameUC))
      {
        JavaCCErrors.warning (aNameloc, "Duplicate option setting for \"" + sName + "\" will be ignored.");
        return;
      }

      if (cmdLineSetting ().contains (sNameUC))
      {
        if (!aExistingValue.equals (aRealSrc))
        {
          JavaCCErrors.warning (aNameloc, "Command line setting of \"" + sName + "\" modifies option value in file.");
        }
        return;
      }
    }

    optionValues ().put (sNameUC, aRealSrc);
    inputFileSetting ().add (sNameUC);

    // Options that are not fully described by their map entry need extra handling
    _applyIndirectOptionFlags (aValueloc, sNameUC, sName, aRealSrc);
  }

  /**
   * Apply the side effects of those options that are not fully described by their entry in the
   * option map. This is called for options from the grammar file as well as for options from the
   * command line - previously it was only called for the former, so that e.g.
   * <code>-OUTPUT_LANGUAGE=c++</code> on the command line was silently ignored.
   *
   * @param aValueloc
   *        Location of the option value, used for warnings. May be <code>null</code> for command
   *        line options, where no location is available.
   * @param sNameUC
   *        Upper cased option name. Never <code>null</code>.
   * @param sName
   *        Option name as written by the user, used for warnings. Never <code>null</code>.
   * @param aValue
   *        The already upgraded option value. Never <code>null</code>.
   */
  private static void _applyIndirectOptionFlags (@Nullable final IGrammarLocation aValueloc,
                                                 @NonNull final String sNameUC,
                                                 @NonNull final String sName,
                                                 @NonNull final Object aValue)
  {
    if (sNameUC.equalsIgnoreCase (USEROPTION__JAVA_TEMPLATE_TYPE))
    {
      final String sTemplateType = (String) aValue;
      if (!_isValidJavaTemplateType (sTemplateType))
      {
        JavaCCErrors.warning (aValueloc,
                              "Bad option value \"" +
                                         aValue +
                                         "\" for \"" +
                                         sName +
                                         "\".  Option setting will be ignored. Valid options are: " +
                                         StringImplode.imploder ()
                                                      .source (SUPPORTED_JAVA_TEMPLATE_TYPES)
                                                      .separator (", ")
                                                      .build ());
      }
    }
    else
      if (sNameUC.equalsIgnoreCase (USEROPTION__JAVA_CHAR_STREAM_TYPE))
      {
        final String sCharStreamType = (String) aValue;
        if (!_isValidJavaCharStreamType (sCharStreamType))
        {
          JavaCCErrors.warning (aValueloc,
                                "Bad option value \"" +
                                           aValue +
                                           "\" for \"" +
                                           sName +
                                           "\".  Option setting will be ignored. Valid options are: " +
                                           StringImplode.imploder ()
                                                        .source (SUPPORTED_JAVA_CHAR_STREAM_TYPES)
                                                        .separator (", ")
                                                        .build ());
        }
      }
      else
        if (sNameUC.equalsIgnoreCase (USEROPTION__OUTPUT_LANGUAGE))
        {
          final String sOutputLanguage = (String) aValue;
          final EOutputLanguage eOutLanguage = EOutputLanguage.getFromIDCaseInsensitiveOrNull (sOutputLanguage);
          if (eOutLanguage == null)
          {
            JavaCCErrors.warning (aValueloc,
                                  "Bad option value \"" +
                                             aValue +
                                             "\" for \"" +
                                             sName +
                                             "\".  Option setting will be ignored. Valid options are: " +
                                             StringImplode.imploder ()
                                                          .source (EOutputLanguage.values (), EOutputLanguage::getID)
                                                          .separator (", ")
                                                          .build ());
            return;
          }
          PGCCContext.current ().options ().setLanguage (eOutLanguage);
        }
        else
          if (sNameUC.equalsIgnoreCase (USEROPTION__CPP_NAMESPACE))
          {
            processCppNamespaceOption ((String) aValue);
          }
  }

  /**
   * Process a single command-line option. The option is parsed and stored in the optionValues map.
   *
   * @param sArg
   *        argument string to set. May not be <code>null</code>.
   */
  public static void setCmdLineOption (@NonNull final String sArg)
  {
    final String sRealArg;
    if (sArg.charAt (0) == '-')
      sRealArg = sArg.substring (1);
    else
      sRealArg = sArg;

    final int nIndex;
    {
      // Look for the first ":" or "=", which will separate the option name
      // from its value (if any).
      final int nIndex1 = sRealArg.indexOf ('=');
      final int nIndex2 = sRealArg.indexOf (':');
      if (nIndex1 < 0)
        nIndex = nIndex2;
      else
        if (nIndex2 < 0)
          nIndex = nIndex1;
        else
          nIndex = Math.min (nIndex1, nIndex2);
    }

    String sNameUC;
    Object aVal;
    if (nIndex < 0)
    {
      // No separator char (like in "DO_THIS_AND_THAT")
      sNameUC = sRealArg.toUpperCase (Locale.US);
      if (optionValues ().containsKey (sNameUC))
      {
        aVal = Boolean.TRUE;
      }
      else
        if (sNameUC.length () > 2 &&
            sNameUC.charAt (0) == 'N' &&
            sNameUC.charAt (1) == 'O' &&
            optionValues ().containsKey (sNameUC.substring (2)))
        {
          aVal = Boolean.FALSE;
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
      sNameUC = sRealArg.substring (0, nIndex).toUpperCase (Locale.US);
      final String sRealValue = sRealArg.substring (nIndex + 1);
      if (sRealValue.equalsIgnoreCase ("TRUE"))
      {
        // Boolean
        aVal = Boolean.TRUE;
      }
      else
        if (sRealValue.equalsIgnoreCase ("FALSE"))
        {
          // Boolean
          aVal = Boolean.FALSE;
        }
        else
        {
          try
          {
            // Integer?
            final int i = Integer.parseInt (sRealValue);
            if (i <= 0)
            {
              PGPrinter.warn ("Warning: Bad option value in \"" + sArg + "\" will be ignored.");
              return;
            }
            aVal = Integer.valueOf (i);
          }
          catch (final NumberFormatException e)
          {
            // String
            aVal = sRealValue;
            if (sRealValue.length () > 2)
            {
              // Check if quoted
              // i.e., there is space for two '"'s in value
              if (sRealValue.charAt (0) == '"' && sRealValue.charAt (sRealValue.length () - 1) == '"')
              {
                // remove the two '"'s.
                aVal = sRealValue.substring (1, sRealValue.length () - 1);
              }
            }
          }
        }
    }

    if (!optionValues ().containsKey (sNameUC))
    {
      PGPrinter.warn ("Warning: Bad option \"" + sArg + "\" will be ignored.");
      return;
    }

    aVal = _upgradeValue (sNameUC, aVal);

    // PARSER_SUPER_CLASS and TOKEN_MANAGER_SUPER_CLASS default to null, so there is no existing
    // value to take the expected type from. setInputFileOption has always guarded against that;
    // this path did not, and setting either of them on the command line threw a
    // NullPointerException instead of generating anything
    final Object aValOrig = optionValues ().get (sNameUC);
    if (aValOrig != null && aVal.getClass () != aValOrig.getClass ())
    {
      PGPrinter.warn ("Warning: Bad option value in \"" + sArg + "\" will be ignored.");
      return;
    }
    if (cmdLineSetting ().contains (sNameUC))
    {
      PGPrinter.warn ("Warning: Duplicate option setting \"" + sArg + "\" will be ignored.");
      return;
    }

    optionValues ().put (sNameUC, aVal);
    cmdLineSetting ().add (sNameUC);

    // Options that are not fully described by their map entry need extra handling
    _applyIndirectOptionFlags (null, sNameUC, sNameUC, aVal);
  }

  /**
   * Settle the options that depend on one another once all of them have been read: turn on
   * DEBUG_PARSER for DEBUG_LOOKAHEAD, reject the char stream combinations that cannot work, and
   * complain about C++ only options in a Java run.
   */
  public static void normalize ()
  {
    if (isDebugLookahead () && !isDebugParser ())
    {
      if (cmdLineSetting ().contains (USEROPTION__DEBUG_PARSER) ||
          inputFileSetting ().contains (USEROPTION__DEBUG_PARSER))
      {
        JavaCCErrors.warning ("True setting of option DEBUG_LOOKAHEAD overrides " +
                              "false setting of option DEBUG_PARSER.");
      }
      optionValues ().put (USEROPTION__DEBUG_PARSER, Boolean.TRUE);
    }

    if (JAVA_CHAR_STREAM_TYPE_CHARSEQUENCE.equalsIgnoreCase (getJavaCharStreamType ()) && isJavaUnicodeEscape ())
    {
      JavaCCErrors.warning ("True setting of option " +
                            USEROPTION__JAVA_UNICODE_ESCAPE +
                            " overrides the \"" +
                            JAVA_CHAR_STREAM_TYPE_CHARSEQUENCE +
                            "\" setting of option " +
                            USEROPTION__JAVA_CHAR_STREAM_TYPE +
                            ".");
    }

    // Both of these are read by the C++ backend only. For Java the parser class declaration comes
    // from the grammar's own PARSER_BEGIN block, so a super class there is written by hand, and the
    // token manager code that would have used the other one has been commented out upstream for as
    // long as this fork exists. Setting either with Java output did nothing at all and said nothing
    if (getOutputLanguage () != EOutputLanguage.CPP)
      for (final String sCppOnly : new String [] { USEROPTION__PARSER_SUPER_CLASS,
                                                   USEROPTION__TOKEN_MANAGER_SUPER_CLASS })
        if (objectValue (sCppOnly) != null)
          JavaCCErrors.warning ("Option " +
                                sCppOnly +
                                " only has an effect with OUTPUT_LANGUAGE=c++ and is ignored here.");
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

  /**
   * Whether to skip the string literal DFA and match every token through the NFA instead.
   *
   * @return The NO_DFA option.
   */
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
  public static EJavaVersion getJdkVersion ()
  {
    return (EJavaVersion) objectValue (USEROPTION__JDK_VERSION);
  }

  /**
   * Whether to write the supporting Java classes - Token, ParseException, the char streams - next
   * to the parser, or to leave them to an existing runtime.
   *
   * @return The GENERATE_BOILERPLATE option.
   */
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
  @NonNull
  public static Charset getGrammarEncoding ()
  {
    final String sValue = stringValue (USEROPTION__GRAMMAR_ENCODING);
    if (StringHelper.isNotEmpty (sValue))
      try
      {
        return Charset.forName (sValue);
      }
      catch (final UnsupportedCharsetException aEx)
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
  @NonNull
  public static Charset getOutputEncoding ()
  {
    final String sValue = stringValue (USEROPTION__OUTPUT_ENCODING);
    if (StringHelper.isNotEmpty (sValue))
      try
      {
        return Charset.forName (sValue);
      }
      catch (final UnsupportedCharsetException aEx)
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

  private static boolean _isValidJavaTemplateType (@Nullable final String sType)
  {
    return sType == null ? false : SUPPORTED_JAVA_TEMPLATE_TYPES.contains (sType.toLowerCase (Locale.US));
  }

  /**
   * The language to generate. Unlike the other options this is kept as an enum rather than as a map
   * entry, because almost everything in the generator branches on it.
   *
   * @return The output language. Never <code>null</code>.
   */
  @NonNull
  public static EOutputLanguage getOutputLanguage ()
  {
    return PGCCContext.current ().options ().getLanguage ();
  }

  /**
   * Which set of Java templates to generate from, {@link #JAVA_TEMPLATE_TYPE_CLASSIC} or
   * {@link #JAVA_TEMPLATE_TYPE_MODERN}.
   *
   * @return The JAVA_TEMPLATE_TYPE option.
   */
  public static String getJavaTemplateType ()
  {
    return stringValue (USEROPTION__JAVA_TEMPLATE_TYPE);
  }

  private static boolean _isValidJavaCharStreamType (@Nullable final String sType)
  {
    return sType == null ? false : SUPPORTED_JAVA_CHAR_STREAM_TYPES.contains (sType.toLowerCase (Locale.US));
  }

  /**
   * Which Java char stream to generate, {@link #JAVA_CHAR_STREAM_TYPE_SIMPLE} or
   * {@link #JAVA_CHAR_STREAM_TYPE_CHARSEQUENCE}.
   *
   * @return The JAVA_CHAR_STREAM_TYPE option.
   */
  public static String getJavaCharStreamType ()
  {
    return stringValue (USEROPTION__JAVA_CHAR_STREAM_TYPE);
  }

  /**
   * Which of the two Java char stream implementations to generate. The CharSequence variant cannot
   * be combined with <code>JAVA_UNICODE_ESCAPE</code>, so that combination silently falls back.
   *
   * @return <code>true</code> if the <code>CharSequenceCharStream</code> should be generated
   *         instead of the <code>SimpleCharStream</code>.
   */
  public static boolean isCharSequenceCharStream ()
  {
    return JAVA_CHAR_STREAM_TYPE_CHARSEQUENCE.equalsIgnoreCase (getJavaCharStreamType ()) && !isJavaUnicodeEscape ();
  }

  /**
   * Set one option to a String value, without any of the checking
   * {@link #setInputFileOption(IGrammarLocation, IGrammarLocation, String, Object)} does.
   *
   * @param sOptionName
   *        The option name. May not be <code>null</code>.
   * @param sOptionValue
   *        The value. May be <code>null</code>.
   */
  public static void setStringOption (@NonNull final String sOptionName, final String sOptionValue)
  {
    optionValues ().put (sOptionName, sOptionValue);
    if (sOptionName.equalsIgnoreCase (USEROPTION__CPP_NAMESPACE))
    {
      processCppNamespaceOption (sOptionValue);
    }
  }

  /**
   * Split a C++ namespace like <code>a::b</code> into the opening and closing text the templates
   * need, and store both under their NONUSER_OPTION names.
   *
   * @param sOptionValue
   *        The namespace, :: separated. May not be <code>null</code>.
   */
  public static void processCppNamespaceOption (final String sOptionValue)
  {
    final String sNs = sOptionValue;
    if (sNs.length () > 0)
    {
      // We also need to split it.
      final StringTokenizer aSt = new StringTokenizer (sNs, "::");
      final StringBuilder aExpanded_ns = new StringBuilder ().append (aSt.nextToken ()).append (" {");
      final StringBuilder aNs_close = new StringBuilder ("}");
      while (aSt.hasMoreTokens ())
      {
        aExpanded_ns.append ("\nnamespace ").append (aSt.nextToken ()).append (" {");
        aNs_close.append ("\n}");
      }
      optionValues ().put (NONUSER_OPTION__NAMESPACE_OPEN, aExpanded_ns.toString ());
      optionValues ().put (NONUSER_OPTION__HAS_NAMESPACE, Boolean.TRUE);
      optionValues ().put (NONUSER_OPTION__NAMESPACE_CLOSE, aNs_close.toString ());
    }
  }

  /**
   * Whether the generated token manager needs a reference back to the parser.
   *
   * @return The TOKEN_MANAGER_USES_PARSER option.
   */
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

  /**
   * Whether the generated parser guards against runaway recursion.
   *
   * @return <code>true</code> if DEPTH_LIMIT is greater than zero.
   */
  public static boolean hasDepthLimit ()
  {
    return getDepthLimit () > 0;
  }

  /**
   * Get defined parser stack usage limit.
   *
   * @return The requested stack usage limit.
   */
  public static String getCppStackLimit ()
  {
    final String sLimit = stringValue (USEROPTION__CPP_STACK_LIMIT);
    if (sLimit.equals ("0"))
      return "";
    return sLimit;
  }

  /**
   * Whether the generated C++ parser guards against running out of stack.
   *
   * @return <code>true</code> if STACK_LIMIT is set.
   */
  public static boolean hasCppStackLimit ()
  {
    return StringHelper.isNotEmpty (getCppStackLimit ());
  }

  /**
   * Gets all the user options (in order)
   *
   * @return all user options
   */
  @NonNull
  @ReturnsImmutableObject
  public static Set <OptionInfo> getUserOptions ()
  {
    return USER_OPTIONS;
  }
}
