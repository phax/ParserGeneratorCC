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
package com.helger.pgcc.output;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Various constants relating to possible values for certain options
 */

public enum EOutputLanguage implements IHasID <String>
{
  /** Generate Java. */
  JAVA ("java")
  {
    @Override
    public String getTypeLong ()
    {
      return "long";
    }

    @Override
    public String getLongValueSuffix ()
    {
      return "L";
    }

    @Override
    public String getTypeBoolean ()
    {
      return "boolean";
    }

    @Override
    public String getFileExtension ()
    {
      return ".java";
    }

    @Override
    public String addUnicodeEscapes (@NonNull final String sStr)
    {
      final StringBuilder aRetVal = new StringBuilder (sStr.length () * 2);
      for (final char ch : sStr.toCharArray ())
        if (ch < 0x20 || ch > 0x7e)
        {
          final String s = "0000" + Integer.toString (ch, 16);
          aRetVal.append ("\\u").append (s.substring (s.length () - 4));
        }
        else
          aRetVal.append (ch);
      return aRetVal.toString ();
    }

    @Override
    public String getAnnotation (final String sAnnotation)
    {
      return "@" + sAnnotation;
    }

    @Override
    public String getMethodModifiers (final String sModifiers)
    {
      return sModifiers.isEmpty () ? "" : sModifiers + " ";
    }

    @Override
    public String getDebugStreamPrintLine (final String sMessage)
    {
      return "debugStream.println(\"" + sMessage + "\");";
    }

    @Override
    public String getModifier (final String sModifier)
    {
      return sModifier;
    }

    @Override
    public String getStaticArrayDeclaration (final String sType, final String sName)
    {
      return "static final " + sType + "[] " + sName + " = ";
    }

    @Override
    public String getClassStart (final String sModifier,
                                 final String sName,
                                 final String [] aSuperClasses,
                                 final String [] aSuperInterfaces)
    {
      final StringBuilder aSB = new StringBuilder ();
      if (sModifier != null)
        aSB.append (getModifier (sModifier));
      aSB.append ("class ").append (sName);
      if (aSuperClasses.length == 1 && aSuperClasses[0] != null)
        aSB.append (" extends ").append (aSuperClasses[0]);
      if (aSuperInterfaces.length != 0)
        aSB.append (" implements ");
      _appendCommaSeparated (aSB, aSuperInterfaces);
      aSB.append (" {\n");
      return aSB.toString ();
    }

    @Override
    public String getMemberAccess ()
    {
      return ".";
    }

    @Override
    public String getLoopStart (final int nLabelIndex)
    {
      return "label_" + nLabelIndex + ":\n" + "while (true) {";
    }

    @Override
    public String getLoopBreak (final int nLabelIndex)
    {
      return "\nbreak label_" + nLabelIndex + ";";
    }

    @Override
    public String getLoopEnd (final int nLabelIndex)
    {
      // A labelled break needs nothing after the loop
      return "";
    }

    @Override
    public String getMissingReturnStatement ()
    {
      return "    throw new IllegalStateException (\"Missing return statement in function\");";
    }

    @Override
    public String getThrowsClause ()
    {
      return " throws ParseException";
    }

    @Override
    public List <String> getTraceEnterLines (final String sProductionName)
    {
      return List.of ("    trace_call(\"" + sProductionName + "\");");
    }

    @Override
    public List <String> getTraceExitLines (final String sProductionName)
    {
      return List.of ("    } finally {", "      trace_return(\"" + sProductionName + "\");", "    }");
    }

    @Override
    public String getLookaheadEntryDeclaration (final String sInternalName)
    {
      return "  private boolean jj_2" + sInternalName + "(int xla)";
    }

    @Override
    public String getLookaheadScanDeclaration (final String sInternalName)
    {
      return "  private boolean jj_3" + sInternalName + "()";
    }
  },
  /** Generate C++. This backend is frozen but supported. */
  CPP ("c++")
  {
    @Override
    public String getTypeLong ()
    {
      return "unsigned long long";
    }

    @Override
    public String getLongValueSuffix ()
    {
      return "ULL";
    }

    @Override
    public String getTypeBoolean ()
    {
      return "bool";
    }

    @Override
    public String getFileExtension ()
    {
      return ".cc";
    }

    @Override
    public String addUnicodeEscapes (final String sStr)
    {
      // C++ source is written as it is
      return sStr;
    }

    @Override
    public String getAnnotation (final String sAnnotation)
    {
      // C++ has no annotations, so it becomes a comment
      return "/*" + sAnnotation + "*/";
    }

    @Override
    public String getMethodModifiers (final String sModifiers)
    {
      // C++ puts none on a definition: the access comes from the section label in the header file,
      // and a member function is non virtual unless its declaration says otherwise
      return "";
    }

    @Override
    public String getDebugStreamPrintLine (final String sMessage)
    {
      // printf does not end the line by itself
      return "fprintf(debugStream, \"" + sMessage + "\\n\");";
    }

    @Override
    public String getModifier (final String sModifier)
    {
      // Only the access modifiers exist, and they are a label rather than a prefix
      final String sLower = sModifier.trim ().toLowerCase (Locale.US);
      if (sLower.equals ("public") || sLower.equals ("protected") || sLower.equals ("private"))
        return sLower + ": ";
      return "";
    }

    @Override
    public String getStaticArrayDeclaration (final String sType, final String sName)
    {
      return "static const " + sType + " " + sName + "[] = ";
    }

    @Override
    public String getClassStart (final String sModifier,
                                 final String sName,
                                 final String [] aSuperClasses,
                                 final String [] aSuperInterfaces)
    {
      final StringBuilder aSB = new StringBuilder ();
      aSB.append ("class ").append (sName);
      if (aSuperClasses.length > 0 || aSuperInterfaces.length > 0)
        aSB.append (" : ");
      _appendCommaSeparated (aSB, aSuperClasses);
      _appendCommaSeparated (aSB, aSuperInterfaces);
      aSB.append (" {\npublic:\n");
      return aSB.toString ();
    }
    @Override
    public String getMemberAccess ()
    {
      return "->";
    }

    @Override
    public String getLoopStart (final int nLabelIndex)
    {
      return "while (!hasError) {";
    }

    @Override
    public String getLoopBreak (final int nLabelIndex)
    {
      return "\ngoto end_label_" + nLabelIndex + ";";
    }

    @Override
    public String getLoopEnd (final int nLabelIndex)
    {
      // The goto needs somewhere to land
      return "\nend_label_" + nLabelIndex + ": ;";
    }

    @Override
    public String getMissingReturnStatement ()
    {
      return "    throw \"Missing return statement in function\";";
    }

    @Override
    public String getThrowsClause ()
    {
      // C++ reports through its error handler instead
      return "";
    }

    @Override
    public List <String> getTraceEnterLines (final String sProductionName)
    {
      // No finally in C++, so a pair of scope guards does the entry and the exit
      return List.of ("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\"" +
                      sProductionName +
                      "\"); });",
                      "    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\"" +
                                  sProductionName +
                                  "\"); });");
    }

    @Override
    public List <String> getTraceExitLines (final String sProductionName)
    {
      return List.of ("    } catch(...) { }");
    }

    @Override
    public String getLookaheadEntryDeclaration (final String sInternalName)
    {
      return " inline bool jj_2" + sInternalName + "(int xla)";
    }

    @Override
    public String getLookaheadScanDeclaration (final String sInternalName)
    {
      return " inline bool jj_3" + sInternalName + "()";
    }
  };

  private final String m_sID;

  private EOutputLanguage (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * {@return the native data type for "long" values}
   */
  @NonNull
  @Nonempty
  public abstract String getTypeLong ();

  /**
   * The suffix a long literal carries in this language.
   *
   * @return The value suffix to be used for long values.
   * @see #getTypeLong()
   */
  @NonNull
  @Nonempty
  protected abstract String getLongValueSuffix ();

  /**
   * A long constant in hexadecimal, with whatever suffix this language wants.
   *
   * @param n
   *        The value.
   * @return The literal. Never <code>null</code>.
   */
  @NonNull
  public String getLongHex (final long n)
  {
    return "0x" + Long.toHexString (n) + getLongValueSuffix ();
  }

  /**
   * A long constant in decimal, with whatever suffix this language wants.
   *
   * @param n
   *        The value.
   * @return The literal. Never <code>null</code>.
   */
  @NonNull
  public String getLongPlain (final long n)
  {
    return "0x" + Long.toString (n) + getLongValueSuffix ();
  }

  /**
   * {@return the native data type for "boolean" values}
   */
  @NonNull
  @Nonempty
  public abstract String getTypeBoolean ();

  /**
   * {@return the file extension for a generated source file in this language, dot included. Never
   *         <code>null</code>.}
   */
  @NonNull
  @Nonempty
  public abstract String getFileExtension ();

  /**
   * Escape everything that is not printable ASCII, the way a string literal in this language wants
   * it.
   * <p>
   * Note that {@code com.helger.pgcc.jjtree.TokenUtils.addUnicodeEscapes} deliberately does
   * something else: it leaves tab, newline, carriage return and form feed alone, because it escapes
   * into source text being copied through rather than into a string literal.
   *
   * @param sStr
   *        The string to escape. May not be <code>null</code>.
   * @return The escaped string. Never <code>null</code>.
   */
  @NonNull
  public abstract String addUnicodeEscapes (@NonNull String sStr);

  /**
   * How this language spells an annotation. Java has them; C++ makes do with a comment.
   *
   * @param sAnnotation
   *        The annotation name, without the marker. May not be <code>null</code>.
   * @return How this language writes that annotation. Never <code>null</code>.
   */
  @NonNull
  public abstract String getAnnotation (@NonNull String sAnnotation);

  /**
   * How this language spells a member modifier. In Java it is a prefix on the member; in C++ an
   * access modifier is a section label and everything else has no equivalent.
   *
   * @param sModifier
   *        The modifier as written in the grammar. May not be <code>null</code>.
   * @return How this language writes it, empty if it has no equivalent. Never <code>null</code>.
   */
  @NonNull
  public abstract String getModifier (@NonNull String sModifier);

  /**
   * The modifiers a method definition carries in this language, ready to be prefixed to the return
   * type.
   *
   * @param sModifiers
   *        The Java modifiers, e.g. <code>private static final</code>. May be empty but not
   *        <code>null</code>.
   * @return The modifiers including a trailing space, or an empty String for a language that puts
   *         none on a definition. Never <code>null</code>.
   */
  @NonNull
  public abstract String getMethodModifiers (@NonNull String sModifiers);

  /**
   * The statement that writes one fixed line to the token manager debug stream.
   *
   * @param sMessage
   *        The message, already escaped for a string literal in this language. May not be
   *        <code>null</code>.
   * @return The complete statement including the trailing semicolon. Never <code>null</code>.
   */
  @NonNull
  public abstract String getDebugStreamPrintLine (@NonNull String sMessage);

  /**
   * How this language declares an array constant.
   *
   * @param sType
   *        The element type. May not be <code>null</code>.
   * @param sName
   *        The variable name. May not be <code>null</code>.
   * @return The declaration of a static array constant up to and including the "=", without a
   *         newline. Never <code>null</code>.
   */
  @NonNull
  public abstract String getStaticArrayDeclaration (@NonNull String sType, @NonNull String sName);

  /**
   * How this language opens a class declaration.
   *
   * @param sModifier
   *        The access modifier, or <code>null</code> for none.
   * @param sName
   *        The class name. May not be <code>null</code>.
   * @param aSuperClasses
   *        The classes it extends. May not be <code>null</code>.
   * @param aSuperInterfaces
   *        The interfaces it implements. May not be <code>null</code>.
   * @return The class header, up to and including the opening brace and its newline. Never
   *         <code>null</code>.
   */
  @NonNull
  public abstract String getClassStart (@Nullable String sModifier,
                                        @NonNull String sName,
                                        @NonNull String [] aSuperClasses,
                                        @NonNull String [] aSuperInterfaces);

  private static void _appendCommaSeparated (@NonNull final StringBuilder aSB, @NonNull final String [] aStrings)
  {
    for (int i = 0; i < aStrings.length; i++)
    {
      if (i > 0)
        aSB.append (", ");
      aSB.append (aStrings[i]);
    }
  }

  /**
   * {@return how a member of a pointer or reference is reached - "." in Java, "-&gt;" in C++. Never
   *         <code>null</code>.}
   */
  @NonNull
  @Nonempty
  public abstract String getMemberAccess ();

  /**
   * The head of a loop that the generated code may want to leave from the inside.
   *
   * @param nLabelIndex
   *        The unique number of this loop within the production.
   * @return The code that opens the loop, without the indentation change. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  public abstract String getLoopStart (int nLabelIndex);

  /**
   * How the generated code leaves a loop from the inside.
   *
   * @param nLabelIndex
   *        The number used by the matching {@link #getLoopStart(int)}.
   * @return The statement that leaves that loop. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  public abstract String getLoopBreak (int nLabelIndex);

  /**
   * What a loop needs after its closing brace, which in C++ is where a break lands.
   *
   * @param nLabelIndex
   *        The number used by the matching {@link #getLoopStart(int)}.
   * @return What has to follow the closing brace of the loop - a landing label in C++, nothing in
   *         Java. Never <code>null</code>, but maybe empty.
   */
  @NonNull
  public abstract String getLoopEnd (int nLabelIndex);

  /**
   * {@return the statement that a non-void production ends with when control can fall off the end -
   *         the compiler requires it in Java, and C++ needs its own spelling. Never
   *         <code>null</code>.}
   */
  @NonNull
  @Nonempty
  public abstract String getMissingReturnStatement ();

  /**
   * What follows a production's parameter list to declare that it can fail.
   *
   * @return " throws ParseException" in Java, nothing in C++, which reports through its error
   *         handler. Never <code>null</code>, but maybe empty.
   */
  @NonNull
  public abstract String getThrowsClause ();

  /**
   * The tracing that {@code DEBUG_PARSER} adds when a production is entered. Java prints on entry
   * and prints again from a finally block; C++ has no finally, so it declares scope guards that do
   * both.
   *
   * @param sProductionName
   *        The name of the production, already escaped. May not be <code>null</code>.
   * @return The lines to emit, in order. Never <code>null</code>.
   */
  @NonNull
  public abstract List <String> getTraceEnterLines (@NonNull String sProductionName);

  /**
   * The counterpart of {@link #getTraceEnterLines(String)} that closes the production's try block.
   *
   * @param sProductionName
   *        The name of the production, already escaped. May not be <code>null</code>.
   * @return The lines to emit, in order. Never <code>null</code>.
   */
  @NonNull
  public abstract List <String> getTraceExitLines (@NonNull String sProductionName);

  /**
   * The header of the routine that the parser calls to try a syntactic lookahead.
   *
   * @param sInternalName
   *        The generated suffix of the lookahead routine. May not be <code>null</code>.
   * @return The declaration of the {@code jj_2} entry point, which takes the lookahead limit. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public abstract String getLookaheadEntryDeclaration (@NonNull String sInternalName);

  /**
   * The header of the routine that walks one expansion during a syntactic lookahead.
   *
   * @param sInternalName
   *        The generated suffix of the lookahead routine. May not be <code>null</code>.
   * @return The declaration of the {@code jj_3} routine that does the actual scan. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public abstract String getLookaheadScanDeclaration (@NonNull String sInternalName);

  /**
   * {@return <code>true</code> if this is the Java backend}
   */
  public boolean isJava ()
  {
    return this == JAVA;
  }

  /**
   * How this language asks for the larger of two values.
   *
   * @param sA
   *        First expression. May not be <code>null</code>.
   * @param sB
   *        Second expression. May not be <code>null</code>.
   * @return An expression for the larger of the two, as this language spells it. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public String getMax (@NonNull final String sA, @NonNull final String sB)
  {
    return (this == JAVA ? "Math.max(" : "MAX(") + sA + ", " + sB + ")";
  }

  /**
   * How this language asks for the smaller of two values.
   *
   * @param sA
   *        First expression. May not be <code>null</code>.
   * @param sB
   *        Second expression. May not be <code>null</code>.
   * @return An expression for the smaller of the two, as this language spells it. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public String getMin (@NonNull final String sA, @NonNull final String sB)
  {
    return (this == JAVA ? "Math.min(" : "MIN(") + sA + ", " + sB + ")";
  }

  /**
   * {@return <code>true</code> if this language keeps its constants in a file of its own, so that
   * switchToStaticsFile does something}
   */
  public boolean hasStaticsFile ()
  {
    return this == CPP;
  }

  /**
   * {@return <code>true</code> if this language declares in a header file separate from the
   * definitions, so that switchToIncludeFile does something}
   */
  public boolean hasIncludeFile ()
  {
    return this == CPP;
  }

  /**
   * Look a language up by the name a grammar uses in its OUTPUT_LANGUAGE option.
   *
   * @param sID
   *        The name, in any casing. May be <code>null</code>.
   * @return <code>null</code> if no language has that name.
   */
  @Nullable
  public static EOutputLanguage getFromIDCaseInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EOutputLanguage.class, sID);
  }
}
