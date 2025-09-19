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
package com.helger.pgcc.parser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.helger.base.exception.InitializationException;
import com.helger.io.file.FileHelper;
import com.helger.pgcc.AbstractJavaCCTestCase;
import com.helger.pgcc.parser.CodeGenerator;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.LexGenJava;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.NfaState;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.Semanticize;
import com.helger.pgcc.parser.StreamProvider;

/**
 * A sea anchor, to ensure that code is not inadvertently broken.
 *
 * @author timp
 * @since 16 Mar 2007
 */
public class NfaStateTest extends AbstractJavaCCTestCase
{
  private final String sParserInputFile = getJJInputDirectory () + "JavaCC.jj";

  @Before
  public void setUp () throws Exception
  {
    Options.init ();
    Main.reInitAll ();
  }

  private void _setupState ()
  {
    try
    {
      final JavaCCParser parser = new JavaCCParser (new StreamProvider (FileHelper.getBufferedReader (new File (sParserInputFile),
                                                                                                      Options.getGrammarEncoding ())));
      parser.javacc_input ();
      JavaCCGlobals.s_fileName = JavaCCGlobals.s_origFileName = sParserInputFile;
      JavaCCGlobals.s_jjtreeGenerated = JavaCCGlobals.isGeneratedBy ("JJTree", sParserInputFile);
      JavaCCGlobals.s_toolNames = JavaCCGlobals.getToolNames (sParserInputFile);
      Semanticize.start ();
      new LexGenJava ().start ();
    }
    catch (final Exception ex)
    {
      throw new InitializationException (ex);
    }
  }

  @Test
  public void testDumpStateSets ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    NfaState.dumpStateSets (cg);
    final String result = cg.getGeneratedCode ().replaceAll ("\r", "");
    assertEquals ("static final int[] jjnextStates = {0\n};\n\n", result);
  }

  /**
   * Test method for {@link NfaState#dumpStateSets(CodeGenerator)}
   */
  @Test
  public void testDumpStateSetsInitialised ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    _setupState ();
    NfaState.dumpStateSets (cg);
    assertEquals ("static final int[] jjnextStates = {\n" +
                  "   37, 38, 39, 15, 43, 44, 45, 49, 50, 51, 26, 27, 29, 17, 19, 5, \n" +
                  "   6, 7, 59, 62, 63, 7, 64, 67, 77, 10, 9, 11, 15, 14, 13, 15, \n" +
                  "   26, 27, 31, 29, 37, 38, 39, 15, 42, 41, 15, 43, 44, 45, 48, 47, \n" +
                  "   15, 49, 50, 51, 52, 54, 15, 53, 52, 54, 15, 57, 56, 15, 61, 60, \n" +
                  "   7, 62, 63, 7, 66, 65, 7, 69, 68, 70, 72, 71, 73, 76, 75, 15, \n" +
                  "   79, 78, 80, 81, 84, 83, 15, 5, 6, 10, 9, 20, 21, 23, 28, 30, \n" +
                  "   32, 37, 38, 43, 44, 49, 50, 53, 52, 62, 63, \n" +
                  "};",
                  cg.getGeneratedCode ().replaceAll ("\r", "").trim ());
  }

  /**
   * Test method for {@link NfaState#dumpCharAndRangeMoves(CodeGenerator)}
   */
  @Test
  public void testDumpCharAndRangeMoves ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    NfaState.dumpCharAndRangeMoves (cg);
    final String result = cg.getGeneratedCode ().replaceAll ("\r", "");
    assertEquals ("         int i2 = (curChar & 0xff) >> 6;\n" +
                  "         long l2 = 1L << (curChar & 077);\n" +
                  "         do\n" +
                  "         {\n" +
                  "            switch(jjstateSet[--i])\n" +
                  "            {\n" +
                  "               default : break;\n" +
                  "            }\n" +
                  "         } while(i != startsAt);\n\n",
                  result);
  }

  /**
   * Test method for {@link NfaState#dumpCharAndRangeMoves(CodeGenerator)}
   */
  @Test
  public void testDumpCharAndRangeMovesInitialised ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    _setupState ();
    NfaState.dumpCharAndRangeMoves (cg);
    assertEquals (("         int hiByte = (curChar >> 8);\n" +
                   "         int i1 = hiByte >> 6;\n" +
                   "         long l1 = 1L << (hiByte & 077);\n" +
                   "         int i2 = (curChar & 0xff) >> 6;\n" +
                   "         long l2 = 1L << (curChar & 077);\n" +
                   "         do\n" +
                   "         {\n" +
                   "            switch(jjstateSet[--i])\n" +
                   "            {\n" +
                   "               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;\n" +
                   "            }\n" +
                   "         } while(i != startsAt);\n").trim (),
                  cg.getGeneratedCode ().replaceAll ("\r", "").trim ());
  }

  /**
   * Test method for {@link NfaState#dumpNonAsciiMoveMethods(CodeGenerator)}
   */
  @Test
  public void testDumpNonAsciiMoveMethods ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    NfaState.dumpNonAsciiMoveMethods (cg);
    final String result = cg.getGeneratedCode ();
    assertEquals ("", result.trim ());
  }

  /**
   * Test method for {@link NfaState#dumpNonAsciiMoveMethods(CodeGenerator)}
   */
  @Test
  public void testDumpNonAsciiMoveMethodsInitialised ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    _setupState ();
    NfaState.dumpNonAsciiMoveMethods (cg);
    assertEquals ("private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2)\n" +
                  "{\n" +
                  "   switch(hiByte)\n" +
                  "   {\n" +
                  "      case 0:\n" +
                  "         return ((jjbitVec2[i2] & l2) != 0L);\n" +
                  "      default :\n" +
                  "         if ((jjbitVec0[i1] & l1) != 0L)\n" +
                  "            return true;\n" +
                  "         return false;\n" +
                  "   }\n" +
                  "}\n" +
                  "private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2)\n" +
                  "{\n" +
                  "   switch(hiByte)\n" +
                  "   {\n" +
                  "      case 0:\n" +
                  "         return ((jjbitVec4[i2] & l2) != 0L);\n" +
                  "      case 2:\n" +
                  "         return ((jjbitVec5[i2] & l2) != 0L);\n" +
                  "      case 3:\n" +
                  "         return ((jjbitVec6[i2] & l2) != 0L);\n" +
                  "      case 4:\n" +
                  "         return ((jjbitVec7[i2] & l2) != 0L);\n" +
                  "      case 5:\n" +
                  "         return ((jjbitVec8[i2] & l2) != 0L);\n" +
                  "      case 6:\n" +
                  "         return ((jjbitVec9[i2] & l2) != 0L);\n" +
                  "      case 7:\n" +
                  "         return ((jjbitVec10[i2] & l2) != 0L);\n" +
                  "      case 9:\n" +
                  "         return ((jjbitVec11[i2] & l2) != 0L);\n" +
                  "      case 10:\n" +
                  "         return ((jjbitVec12[i2] & l2) != 0L);\n" +
                  "      case 11:\n" +
                  "         return ((jjbitVec13[i2] & l2) != 0L);\n" +
                  "      case 12:\n" +
                  "         return ((jjbitVec14[i2] & l2) != 0L);\n" +
                  "      case 13:\n" +
                  "         return ((jjbitVec15[i2] & l2) != 0L);\n" +
                  "      case 14:\n" +
                  "         return ((jjbitVec16[i2] & l2) != 0L);\n" +
                  "      case 15:\n" +
                  "         return ((jjbitVec17[i2] & l2) != 0L);\n" +
                  "      case 16:\n" +
                  "         return ((jjbitVec18[i2] & l2) != 0L);\n" +
                  "      case 17:\n" +
                  "         return ((jjbitVec19[i2] & l2) != 0L);\n" +
                  "      case 18:\n" +
                  "         return ((jjbitVec20[i2] & l2) != 0L);\n" +
                  "      case 19:\n" +
                  "         return ((jjbitVec21[i2] & l2) != 0L);\n" +
                  "      case 20:\n" +
                  "         return ((jjbitVec0[i2] & l2) != 0L);\n" +
                  "      case 22:\n" +
                  "         return ((jjbitVec22[i2] & l2) != 0L);\n" +
                  "      case 23:\n" +
                  "         return ((jjbitVec23[i2] & l2) != 0L);\n" +
                  "      case 24:\n" +
                  "         return ((jjbitVec24[i2] & l2) != 0L);\n" +
                  "      case 30:\n" +
                  "         return ((jjbitVec25[i2] & l2) != 0L);\n" +
                  "      case 31:\n" +
                  "         return ((jjbitVec26[i2] & l2) != 0L);\n" +
                  "      case 32:\n" +
                  "         return ((jjbitVec27[i2] & l2) != 0L);\n" +
                  "      case 33:\n" +
                  "         return ((jjbitVec28[i2] & l2) != 0L);\n" +
                  "      case 48:\n" +
                  "         return ((jjbitVec29[i2] & l2) != 0L);\n" +
                  "      case 49:\n" +
                  "         return ((jjbitVec30[i2] & l2) != 0L);\n" +
                  "      case 77:\n" +
                  "         return ((jjbitVec31[i2] & l2) != 0L);\n" +
                  "      case 159:\n" +
                  "         return ((jjbitVec32[i2] & l2) != 0L);\n" +
                  "      case 164:\n" +
                  "         return ((jjbitVec33[i2] & l2) != 0L);\n" +
                  "      case 215:\n" +
                  "         return ((jjbitVec34[i2] & l2) != 0L);\n" +
                  "      case 250:\n" +
                  "         return ((jjbitVec35[i2] & l2) != 0L);\n" +
                  "      case 251:\n" +
                  "         return ((jjbitVec36[i2] & l2) != 0L);\n" +
                  "      case 253:\n" +
                  "         return ((jjbitVec37[i2] & l2) != 0L);\n" +
                  "      case 254:\n" +
                  "         return ((jjbitVec38[i2] & l2) != 0L);\n" +
                  "      case 255:\n" +
                  "         return ((jjbitVec39[i2] & l2) != 0L);\n" +
                  "      default :\n" +
                  "         if ((jjbitVec3[i1] & l1) != 0L)\n" +
                  "            return true;\n" +
                  "         return false;\n" +
                  "   }\n" +
                  "}\n" +
                  "private static final boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, long l2)\n" +
                  "{\n" +
                  "   switch(hiByte)\n" +
                  "   {\n" +
                  "      case 0:\n" +
                  "         return ((jjbitVec40[i2] & l2) != 0L);\n" +
                  "      case 2:\n" +
                  "         return ((jjbitVec5[i2] & l2) != 0L);\n" +
                  "      case 3:\n" +
                  "         return ((jjbitVec41[i2] & l2) != 0L);\n" +
                  "      case 4:\n" +
                  "         return ((jjbitVec42[i2] & l2) != 0L);\n" +
                  "      case 5:\n" +
                  "         return ((jjbitVec43[i2] & l2) != 0L);\n" +
                  "      case 6:\n" +
                  "         return ((jjbitVec44[i2] & l2) != 0L);\n" +
                  "      case 7:\n" +
                  "         return ((jjbitVec45[i2] & l2) != 0L);\n" +
                  "      case 9:\n" +
                  "         return ((jjbitVec46[i2] & l2) != 0L);\n" +
                  "      case 10:\n" +
                  "         return ((jjbitVec47[i2] & l2) != 0L);\n" +
                  "      case 11:\n" +
                  "         return ((jjbitVec48[i2] & l2) != 0L);\n" +
                  "      case 12:\n" +
                  "         return ((jjbitVec49[i2] & l2) != 0L);\n" +
                  "      case 13:\n" +
                  "         return ((jjbitVec50[i2] & l2) != 0L);\n" +
                  "      case 14:\n" +
                  "         return ((jjbitVec51[i2] & l2) != 0L);\n" +
                  "      case 15:\n" +
                  "         return ((jjbitVec52[i2] & l2) != 0L);\n" +
                  "      case 16:\n" +
                  "         return ((jjbitVec53[i2] & l2) != 0L);\n" +
                  "      case 17:\n" +
                  "         return ((jjbitVec19[i2] & l2) != 0L);\n" +
                  "      case 18:\n" +
                  "         return ((jjbitVec20[i2] & l2) != 0L);\n" +
                  "      case 19:\n" +
                  "         return ((jjbitVec54[i2] & l2) != 0L);\n" +
                  "      case 20:\n" +
                  "         return ((jjbitVec0[i2] & l2) != 0L);\n" +
                  "      case 22:\n" +
                  "         return ((jjbitVec22[i2] & l2) != 0L);\n" +
                  "      case 23:\n" +
                  "         return ((jjbitVec55[i2] & l2) != 0L);\n" +
                  "      case 24:\n" +
                  "         return ((jjbitVec56[i2] & l2) != 0L);\n" +
                  "      case 30:\n" +
                  "         return ((jjbitVec25[i2] & l2) != 0L);\n" +
                  "      case 31:\n" +
                  "         return ((jjbitVec26[i2] & l2) != 0L);\n" +
                  "      case 32:\n" +
                  "         return ((jjbitVec57[i2] & l2) != 0L);\n" +
                  "      case 33:\n" +
                  "         return ((jjbitVec28[i2] & l2) != 0L);\n" +
                  "      case 48:\n" +
                  "         return ((jjbitVec58[i2] & l2) != 0L);\n" +
                  "      case 49:\n" +
                  "         return ((jjbitVec30[i2] & l2) != 0L);\n" +
                  "      case 77:\n" +
                  "         return ((jjbitVec31[i2] & l2) != 0L);\n" +
                  "      case 159:\n" +
                  "         return ((jjbitVec32[i2] & l2) != 0L);\n" +
                  "      case 164:\n" +
                  "         return ((jjbitVec33[i2] & l2) != 0L);\n" +
                  "      case 215:\n" +
                  "         return ((jjbitVec34[i2] & l2) != 0L);\n" +
                  "      case 250:\n" +
                  "         return ((jjbitVec35[i2] & l2) != 0L);\n" +
                  "      case 251:\n" +
                  "         return ((jjbitVec59[i2] & l2) != 0L);\n" +
                  "      case 253:\n" +
                  "         return ((jjbitVec37[i2] & l2) != 0L);\n" +
                  "      case 254:\n" +
                  "         return ((jjbitVec60[i2] & l2) != 0L);\n" +
                  "      case 255:\n" +
                  "         return ((jjbitVec61[i2] & l2) != 0L);\n" +
                  "      default :\n" +
                  "         if ((jjbitVec3[i1] & l1) != 0L)\n" +
                  "            return true;\n" +
                  "         return false;\n" +
                  "   }\n" +
                  "}",
                  cg.getGeneratedCode ().replaceAll ("\r", "").trim ());
  }

  /**
   * Test method for {@link NfaState#dumpMoveNfa(CodeGenerator)}
   */
  @Test
  public void testDumpMoveNfa ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    try
    {
      NfaState.dumpMoveNfa (cg);
      fail ("Should have bombed");
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      e = null;
    }
    final String result = cg.getGeneratedCode ();
    assertEquals ("", result.trim ());

    if (false)
    {

      assertEquals ("static private final void jjCheckNAdd(int state)\n" +
                    "{\n" +
                    "   if (jjrounds[state] != jjround)\n" +
                    "   {\n" +
                    "      jjstateSet[jjnewStateCnt++] = state;\n" +
                    "      jjrounds[state] = jjround;\n" +
                    "   }\n" +
                    "}\n" +
                    "static private final void jjAddStates(int start, int end)\n" +
                    "{\n" +
                    "   do {\n" +
                    "      jjstateSet[jjnewStateCnt++] = jjnextStates[start];\n" +
                    "   } while (start++ != end);\n" +
                    "}\n" +
                    "static private final void jjCheckNAddTwoStates(int state1, int state2)\n" +
                    "{\n" +
                    "   jjCheckNAdd(state1);\n" +
                    "   jjCheckNAdd(state2);\n" +
                    "}\n" +
                    "static private final void jjCheckNAddStates(int start, int end)\n" +
                    "{\n" +
                    "   do {\n" +
                    "      jjCheckNAdd(jjnextStates[start]);\n" +
                    "   } while (start++ != end);\n" +
                    "}\n" +
                    "static private final void jjCheckNAddStates(int start)\n" +
                    "{\n" +
                    "   jjCheckNAdd(jjnextStates[start]);\n" +
                    "   jjCheckNAdd(jjnextStates[start + 1]);\n" +
                    "}\n" +
                    "",
                    cg.getGeneratedCode ().trim ());
    }
  }

  /**
   * Test method for {@link NfaState#dumpMoveNfa(CodeGenerator)}.
   */
  @Test
  public void testDumpMoveNfaInitialised ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    _setupState ();
    NfaState.dumpMoveNfa (cg);
    assertEquals ("private int jjMoveNfa_4(int startState, int curPos)\n" + "{\n" + "   return curPos;\n" + "}",
                  cg.getGeneratedCode ().replaceAll ("\r", "").trim ());
  }

  /**
   * Test method for {@link NfaState#dumpStatesForStateJava(CodeGenerator)}.
   */
  @Test
  public void testDumpStatesForState ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    NfaState.dumpStatesForStateJava (cg);
    final String result = cg.getGeneratedCode ().replaceAll ("\r", "");
    assertEquals ("protected static final class States {\n" + "  protected static final int[][][] statesForState = null;\n" + "}",
                  result.trim ());
  }

  /**
   * Test method for {@link NfaState#dumpStatesForStateJava(CodeGenerator)}.
   */
  @Test
  public void testDumpStatesForStateInitialised ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    _setupState ();
    NfaState.dumpStatesForStateJava (cg);
    final String result = cg.getGeneratedCode ();
    assertEquals ("protected static final class States {\n" +
                  "  protected static final int[][][] statesForState = {\n" +
                  " {\n" +
                  "   { 0 },\n" +
                  "   { 1 },\n" +
                  "   { 2 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 5 },\n" +
                  "   { 6 },\n" +
                  "   { 7 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 9 },\n" +
                  "   { 10 },\n" +
                  "   { 11 },\n" +
                  "   { 12 },\n" +
                  "   { 13 },\n" +
                  "   { 14 },\n" +
                  "   { 15 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 17 },\n" +
                  "   { 18 },\n" +
                  "   { 19 },\n" +
                  "   { 20 },\n" +
                  "   { 21 },\n" +
                  "   { 22 },\n" +
                  "   { 23 },\n" +
                  "   { 24 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 26 },\n" +
                  "   { 27 },\n" +
                  "   { 28 },\n" +
                  "   { 29 },\n" +
                  "   { 30 },\n" +
                  "   { 31 },\n" +
                  "   { 32 },\n" +
                  "   { 33 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 35 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 37 },\n" +
                  "   { 38 },\n" +
                  "   { 39 },\n" +
                  "   { 40 },\n" +
                  "   { 41 },\n" +
                  "   { 42 },\n" +
                  "   { 43 },\n" +
                  "   { 44 },\n" +
                  "   { 45 },\n" +
                  "   { 46 },\n" +
                  "   { 47 },\n" +
                  "   { 48 },\n" +
                  "   { 49 },\n" +
                  "   { 50 },\n" +
                  "   { 51 },\n" +
                  "   { 52 },\n" +
                  "   { 53 },\n" +
                  "   { 54 },\n" +
                  "   { 55 },\n" +
                  "   { 56 },\n" +
                  "   { 57 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 59 },\n" +
                  "   { 60 },\n" +
                  "   { 61 },\n" +
                  "   { 62 },\n" +
                  "   { 63 },\n" +
                  "   { 64 },\n" +
                  "   { 65 },\n" +
                  "   { 66 },\n" +
                  "   { 67 },\n" +
                  "   { 68 },\n" +
                  "   { 69 },\n" +
                  "   { 70 },\n" +
                  "   { 71 },\n" +
                  "   { 72 },\n" +
                  "   { 73 },\n" +
                  "   { 74 },\n" +
                  "   { 75 },\n" +
                  "   { 76 },\n" +
                  "   { 77 },\n" +
                  "   { 78 },\n" +
                  "   { 79 },\n" +
                  "   { 80 },\n" +
                  "   { 81 },\n" +
                  "   { 82 },\n" +
                  "   { 83 },\n" +
                  "   { 84 },\n" +
                  "},\n" +
                  " {},\n" +
                  " {\n" +
                  "   { 0, 2, },\n" +
                  "   { 1 },\n" +
                  "   { 0, 2, },\n" +
                  "},\n" +
                  " {},\n" +
                  " {},\n" +
                  "\n" +
                  "};\n" +
                  "}",
                  result.replaceAll ("\r", "").trim ());
  }

  /**
   * Test method for {@link NfaState#dumpStatesForKind(CodeGenerator)}.
   */
  @Test
  public void testDumpStatesForKind ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    NfaState.dumpStatesForKind (cg);
    final String result = cg.getGeneratedCode ().replaceAll ("\r", "");
    assertEquals ("protected static final class States {\n" +
                  "  protected static final int[][][] statesForState = null;\n" +
                  "}\n" +
                  "protected static final class Kinds {\n" +
                  "  protected static final int[][] kindForState = null;\n" +
                  "}",
                  result.trim ());
  }

  /**
   * Test method for {@link NfaState#dumpStatesForKind(CodeGenerator)}.
   */
  @Test
  public void testDumpStatesForKindInitialised ()
  {
    final CodeGenerator cg = new CodeGenerator ();
    _setupState ();
    NfaState.dumpStatesForKind (cg);
    final String result = cg.getGeneratedCode ();
    assertEquals ("protected static final class States {\n" +
                  "  protected static final int[][][] statesForState = {\n" +
                  " {\n" +
                  "   { 0 },\n" +
                  "   { 1 },\n" +
                  "   { 2 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 5 },\n" +
                  "   { 6 },\n" +
                  "   { 7 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 9 },\n" +
                  "   { 10 },\n" +
                  "   { 11 },\n" +
                  "   { 12 },\n" +
                  "   { 13 },\n" +
                  "   { 14 },\n" +
                  "   { 15 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 17 },\n" +
                  "   { 18 },\n" +
                  "   { 19 },\n" +
                  "   { 20 },\n" +
                  "   { 21 },\n" +
                  "   { 22 },\n" +
                  "   { 23 },\n" +
                  "   { 24 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 26 },\n" +
                  "   { 27 },\n" +
                  "   { 28 },\n" +
                  "   { 29 },\n" +
                  "   { 30 },\n" +
                  "   { 31 },\n" +
                  "   { 32 },\n" +
                  "   { 33 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 35 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 37 },\n" +
                  "   { 38 },\n" +
                  "   { 39 },\n" +
                  "   { 40 },\n" +
                  "   { 41 },\n" +
                  "   { 42 },\n" +
                  "   { 43 },\n" +
                  "   { 44 },\n" +
                  "   { 45 },\n" +
                  "   { 46 },\n" +
                  "   { 47 },\n" +
                  "   { 48 },\n" +
                  "   { 49 },\n" +
                  "   { 50 },\n" +
                  "   { 51 },\n" +
                  "   { 52 },\n" +
                  "   { 53 },\n" +
                  "   { 54 },\n" +
                  "   { 55 },\n" +
                  "   { 56 },\n" +
                  "   { 57 },\n" +
                  "   { 3, 4, 8, 16, 25, 34, 36, 58, },\n" +
                  "   { 59 },\n" +
                  "   { 60 },\n" +
                  "   { 61 },\n" +
                  "   { 62 },\n" +
                  "   { 63 },\n" +
                  "   { 64 },\n" +
                  "   { 65 },\n" +
                  "   { 66 },\n" +
                  "   { 67 },\n" +
                  "   { 68 },\n" +
                  "   { 69 },\n" +
                  "   { 70 },\n" +
                  "   { 71 },\n" +
                  "   { 72 },\n" +
                  "   { 73 },\n" +
                  "   { 74 },\n" +
                  "   { 75 },\n" +
                  "   { 76 },\n" +
                  "   { 77 },\n" +
                  "   { 78 },\n" +
                  "   { 79 },\n" +
                  "   { 80 },\n" +
                  "   { 81 },\n" +
                  "   { 82 },\n" +
                  "   { 83 },\n" +
                  "   { 84 },\n" +
                  "},\n" +
                  " {},\n" +
                  " {\n" +
                  "   { 0, 2, },\n" +
                  "   { 1 },\n" +
                  "   { 0, 2, },\n" +
                  "},\n" +
                  " {},\n" +
                  " {},\n" +
                  "\n" +
                  "};\n" +
                  "}\n" +
                  "protected static final class Kinds {\n" +
                  "  protected static final int[][] kindForState = {\n" +
                  "{ \n" +
                  "  27, \n" +
                  "  27, \n" +
                  "  27, \n" +
                  "  27, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  100, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  101, \n" +
                  "  152, \n" +
                  "  152, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  89, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, \n" +
                  "  94, },\n" +
                  "{}\n" +
                  ",\n" +
                  "{ \n" +
                  "  30, \n" +
                  "  30, \n" +
                  "  30, },\n" +
                  "{}\n" +
                  ",\n" +
                  "{}\n" +
                  "\n" +
                  "};\n" +
                  "}",
                  result.replaceAll ("\r", "").trim ());
  }
}
