# ParserGeneratorCC

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger/parser-generator-cc/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger/parser-generator-cc/)
[![javadoc](https://javadoc.io/badge2/com.helger/parser-generator-cc/javadoc.svg)](https://javadoc.io/doc/com.helger/parser-generator-cc)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

Fork of JavaCC 7.0.3 because the original code base has poor quality and PRs were not really merged.
The overall goal is to maintain compatibility to JavaCC but
* The code itself should be better maintainable
* The code itself should conform to best-practices
* Because this is NOT JavaCC the class names are similar, but the base package name changed from `net.javacc` to `com.helger.pgcc`
* The created code requires at least Java 1.5  

See https://github.com/phax/ph-javacc-maven-plugin/ for a Maven plugin that uses this CC.

This fork is not really actively maintained, except some severe problems arise.
I mainly use it to power my [ph-css](https://github.com/phax/ph-css) CSS parser.

## Maven usage

Add the following to your pom.xml to use this artifact (replacing `x.y.z` with the actual version number):

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>parser-generator-cc</artifactId>
  <version>x.y.z</version>
</dependency>
```

## Documentation

* [`docs/internals/`](docs/internals/) - how the generator works: the pipeline, the generator state, the template language and the bootstrap loop
* [`www/doc/`](www/doc/) - the JavaCC language reference

## Option `JAVA_CHAR_STREAM_TYPE`

Controls which `CharStream` implementation is generated for Java:

* `simple` (default) - `SimpleCharStream` on top of `AbstractCharStream`, reading the input into an
  internal circular buffer that is relocated and doubled for tokens larger than the buffer.
* `charsequence` - `CharSequenceCharStream`, keeping the whole input in memory and reading directly
  from it. There is no buffer, so token length is never limited by a buffer size and no buffer
  relocation ever takes place. Line and column numbers are calculated on demand instead of being
  recorded for every single character.

The generated parser gets an additional `CharSequence` based constructor and `ReInit` method with
`charsequence`. Restrictions:

* Not available in combination with `JAVA_UNICODE_ESCAPE=true` - `JavaCharStream` is used instead
  and a warning is emitted.
* The input is read completely up front, so an `IOException` of the underlying `Reader` surfaces as
  an `IllegalStateException` in the constructor and the input may not exceed `Integer.MAX_VALUE`
  characters.
* `adjustBeginLineColumn` is not available because line and column numbers are not recorded per
  character.

# News and noteworthy

v3.0.0 - 2026-06-09
* The remaining `instanceof` tests that were followed by a cast use pattern matching now, in `ParseEngine`, `Semanticize`, `ExpRCharacterList`, `ExpRChoice`, `ExpRStringLiteral`, `JJDoc`, `LexGenCpp` and `Options`.
  `LexGenCpp` had three casts of `lexer ().getCurRE ()` in one condition where the Java backend already had one binding, so the two backends read alike again.
  This also removes a latent `ClassCastException` in `Options.setInputFileOption`, which tested `aObject instanceof Integer` but cast `aRealSrc`; only `&&` short circuiting kept it unreachable, because the grammar can produce a `List<String>` but never a `List<Integer>`.
  The action blocks in `JavaCC.jj` and `ConditionParser.jj` keep their casts on purpose - the Java grammar the generator parses them with has no production for `instanceof Type name`
* The reworked classes declare their members in one consistent order: nested types, then the fields, then the constructor, then the instance methods, with the static factory methods last.
  `PGCCContext.current ()`, `PGCCContext.reset ()` and `ProcessState.getInstance ()` moved to the end of their class, matching `LookaheadState.current ()`, and `TokenizerData`'s three nested types moved above the fields whose types they are.
  Pure reordering; no API and no behaviour change
* Generating a large lexer is around 3x faster.
  The epsilon closure walked the whole state list twice per call purely to record which states a pass had visited, which is quadratic in the token count; the passes are numbered now and the walks are gone.
  On a synthetic 1280 token grammar that loop ran 690 million times and was 70% of the run, and generation drops from 1344 ms to 413 ms.
  Generated output is unchanged
* Every public and protected member of the generator carries javadoc now, and `mvn javadoc:javadoc` reports no warnings.
  Writing it up turned up 16 members with no caller left and two option constants that were never registered, all removed.
  `TOKEN_MANAGER_SUPERCLASS` is documented as accepted and ignored - the option that works is `TOKEN_MANAGER_SUPER_CLASS`
* Fixed the generated C++ token manager not compiling for a grammar with `'` or `\` in a string literal.
  The switch over the current character got `case ''':` and `case '\':`, because only the Java backend escaped those two characters in a case label
* Fixed two token manager debug messages in the generated C++ running into the following line, and one being indented outside the `if` that guards it.
  `DEBUG_TOKEN_MANAGER` only
* **Breaking API change** Replaced the internal `EJDKVersion` enum with `EJavaVersion` from ph-commons, so that `JDK_VERSION` values above 14 are supported.
  `Options.getJdkVersion ()` returns `com.helger.base.system.EJavaVersion` now
* **Potentially breaking** The default value of `JDK_VERSION` moved from `1.5` to `1.8`, so that generated code uses the `Charset` based constructors and the diamond operator unless configured otherwise
* **Breaking API change** `LexGenJava`, `ParseGenJava`, `LexGenCpp` and `ParseGenCPP` moved from `com.helger.pgcc.parser` to `com.helger.pgcc.output.java` and `com.helger.pgcc.output.cpp`, so that the package says which target language a class writes.
  `com.helger.pgcc.parser` no longer contains anything that writes a file
* **Breaking API change** The generator state moved from static fields into `com.helger.pgcc.context.PGCCContext`, one instance per run and per thread.
  The old classes remain as facades, so `Options`, `JavaCCErrors` and `JavaCCGlobals` are used exactly as before, but `JavaCCGlobals` exposes the grammar through `grammar ()` instead of public static fields.
  Two generator runs can now happen at the same time in one JVM
* Added `JavaCCLauncher`, `JJTreeLauncher` and `JJDocLauncher` as properly named command line entry points; the lower case `javacc`, `jjtree` and `jjdoc` classes remain as deprecated aliases
* Fixed generated files never being rebuilt when regenerating into a directory that already contains them.
  The checksum that decides this was computed before the writer had been flushed, so for anything but a very large file it was the checksum of nothing and never matched.
  Hand edited files are still protected, as intended
* Fixed `JavaCCGlobals.getToolNames` throwing a `NullPointerException` instead of returning an empty list when the file does not exist
* Fixed `JDK_VERSION` values above 14 silently falling back to the default
* **Breaking** Removed the undocumented and untested `TOKEN_MANAGER_CODE_GENERATOR` and `PARSER_CODE_GENERATOR` options together with the table driven token manager behind them
* Removed the dead `test/` (upstream Ant build) and `docs/` directories, and trimmed `www/doc/` to the reference pages
* Fixed `OUTPUT_LANGUAGE` (and every other option with an indirect effect) being ignored when set on the command line instead of in the grammar file
* Fixed the JJDoc `-BNF` output dropping every terminal from the productions, so that `<NUMBER> ( <PLUS> <NUMBER> )* <EOF>` was written as `( )* <EOF>`
* Fixed JJTree carrying node types from one run into the next when several grammars are processed in the same JVM, so that the second grammar's output contained the first grammar's node classes.
  The C++ node files were affected most visibly
* Fixed `JavaCCInterpreter` failing on the first character of any input for grammars with more than one character class token.
  The composite state the tokenizer starts in had no entry in the `TokenizerData`, so the NFA was skipped entirely.
  Only the interpreter is affected; generated code never used this path
* Fixed `JavaCCInterpreter` losing the characters a `MORE` production consumed, so that a token assembled across a lexical state switch reported only its last piece - a string literal came out as its closing quote
* **Breaking API change** No `static` non final field is left in the code base.
  The last per run collections - `ASTNodeDescriptor`'s node tables and `JJTreeGlobals.TOOL_LIST` - moved into `PGCCContext`, and the two genuinely process wide settings moved to the new `com.helger.pgcc.context.ProcessState`.
  `PGPrinter.init` and `FilesJava.setReadFromClassPath` are unchanged
* Fixed a `NullPointerException` when setting `PARSER_SUPER_CLASS` or `TOKEN_MANAGER_SUPER_CLASS` on the command line.
  They are the only two options with a `null` default, so there was no existing value to take the expected type from.
  Setting them in the grammar file always worked
* `PARSER_SUPER_CLASS` and `TOKEN_MANAGER_SUPER_CLASS` now warn when set with a Java target.
  Both are read by the C++ backend only and were silently ignored otherwise
* Fixed the `jjtree` help output advertising `JDK_VERSION (default "1.5")` and `OUTPUT_DIRECTORY (default "")`, neither of which was the actual default
* Removed a leftover debug line that made `jjtree` print `opt:java` on every run
* Local variables and parameters throughout the code base now use the project's Hungarian notation.
  The public fields of `Token` keep their names - generated parsers and grammar action code read `token.kind` and `t.image`
* **Breaking API change** `JavaCCErrors.parse_error`, `semantic_error` and `warning` take a `com.helger.pgcc.parser.IGrammarLocation` instead of an `Object`.
  The new interface extends `com.helger.base.location.ILocation` and is implemented by `NormalProduction`, `TokenProduction`, `Expansion`, `ICCCharacter` and both `Token` classes - the six types the old `instanceof` cascade tested for.
  `Options.setInputFileOption` takes it too
* **Breaking API change** The grammar model spells its position the ph-commons way: `getLineNumber ()` / `getColumnNumber ()` and `setLineNumber ()` / `setColumnNumber ()` on `NormalProduction`, `TokenProduction`, `Expansion`, `ICCCharacter` and both `Token` classes. `IGrammarLocation` is now `ILocation` plus the resource id
* **Breaking API change** Renamed the two abstract classes that did not say so: `NormalProduction` is `AbstractNormalProduction` and `JavaCCParserInternals` is `AbstractJavaCCParserInternals`
* **Breaking API change** `Nfa` and the two carriers inside `TokenizerData` are records.
  `TokenizerData.NfaState.m_aCharacters` and friends are accessors now, so they read `characters ()`
* JJDoc's HTML output is HTML5 instead of HTML 3.2.
  Lower case tags, `<meta charset>`, `id` anchors instead of `<a name>`, and a small default stylesheet in place of the `ALIGN` and `VALIGN` attributes - a stylesheet given with the `CSS` option is linked after it and still wins.
  Token productions with nothing to show no longer leave an empty table row behind
* The generated `CharStream` and `AbstractCharStream` are fully documented.
  `javadoc` reported 28 warnings on them and now reports none, so a project that runs `javadoc` over its generated parser no longer inherits them.
  The parameter `newCol` of `adjustBeginLineColumn` is `nNewCol`
* Generation of grammars whose tokens are built from character classes is about three times faster.
  The NFA construction was resolving the `ThreadLocal` that holds the run's state once per element rather than once per call - in one case for every state, for every state
* **Breaking API change** Method names use camel case throughout: `JavaCCErrors.parse_error` is `parseError`, `semantic_error` is `semanticError`, `FilesJava.gen_Token` is `genToken` and so on for 27 names.
  The `jj_` and `trace_` methods of *generated* parsers keep their names - grammar action code calls them
* **Breaking API change** `CodeGenerator` is `AbstractCodeGenerator` and is abstract - nothing outside the tests ever instantiated it.
  The two C++ only methods it carried, `genStringLiteralArrayCPP` and `genStringLiteralInCPP`, moved into `LexGenCpp` where the only caller is
* **Breaking API change** The C++ generators no longer extend the Java ones.
  `LexGenCpp` and `LexGenJava` share the new `com.helger.pgcc.output.AbstractLexGenJavaLike`, and `ParseGenCpp` extends `AbstractCodeGenerator` directly - it inherited nothing from `ParseGenJava` at all
* **Breaking API change** Removed `JavaCCErrors.reInit ()`, deprecated since the error counters moved into `PGCCContext`

v2.0.3 - 2026-09-08
* Added the new option `JAVA_CHAR_STREAM_TYPE` that allows to generate a `CharSequenceCharStream` that needs no internal buffer at all ([issue #21](https://github.com/tulipcc/ParserGeneratorCC/issues/21))
* Fixed the tool names in the `Generated by:` header not being trimmed, so that a parser generated by JavaCC from a JJTree generated grammar did not reset the JJTree state in its `ReInit` methods ([issue #45](https://github.com/tulipcc/ParserGeneratorCC/issues/45))

v2.0.2 - 2026-09-08
* Removed OSGI bundling
* Fixed `JavaCharStream` reusing the inherited `maxNextCharInd` field for its raw input buffer, leading to `ArrayIndexOutOfBoundsException` or silently altered characters with `JAVA_UNICODE_ESCAPE=true` - thanks to @wilx
* Fixed `AbstractCharStream.expandBuff` not updating `maxNextCharInd` when relocating token data, leading to NUL gaps in `SimpleCharStream` for nonzero token starts - thanks to @wilx

v2.0.1 - 2025-11-16
* Updated to ph-commons 12.1.0
* Using JSpecify annotations

v2.0.0 - 2025-09-19
* Using Java 17 as the baseline
* Updated to ph-commons 12

v1.1.4 - 2022-01-10
* Updated to ph-commons 10
* Java non-modern style no longer creates constructors without a Charset. See [issue #29](https://github.com/phax/ParserGeneratorCC/issues/29) - thanks @sfuhrm
* Improved the code of the `jj_ntk_f` method to be more efficient

v1.1.3 - 2020-05-13
* Allow `final` when catching exceptions - [issue #24](https://github.com/phax/ParserGeneratorCC/issues/24)
* Changed Automatic-Module-Name to `com.helger.pgcc`
* Fixed internal buffer corruption in generated code - [issue #26](https://github.com/phax/ParserGeneratorCC/issues/26)

v1.1.2 - 2019-05-02
* JavaCC grammar can parse Java 7 language features ([JavaCC PR 71](https://github.com/javacc/javacc/pull/71))
* Backported a CPP output fix from JavaCC
* Fixed an error in `JavaCharStream.template` if `KEEP_LINE_COLUMN` is turned off

v1.1.1 - 2019-01-28
* Fixed an error in Java modern template `SimpleCharStream` - thanks to @nbauma109

v1.1.0 - 2018-10-25
* Focus is on improving the quality and consistency of the template files - this may cause interoperability problem because names change etc.
* This version assumes that the created code uses at least Java 1.5 - JDK versions below 1.5 are no longer supported!
* The option `STATIC` was removed - it caused too many variations in the templates which decreases maintainability. STATIC is now always `false`!
* Added new option `OUTPUT_ENCODING` to define the character set for the output files
* If JDK version is at least 1.6 than `java.nio.Charset encoding` is used and not `String encoding` in generated Streams.
* The Java templates were modified so that the interface methods start with a lowercase character (`beginToken` instead of `BeginToken`) - the created code may need to be adopted!
* A masking issue for `'` and `\` characters was resolved (issue #20) 

v1.0.2 - 2018-01-08
* Fixed an error in jump-patching that was originally only considered if legacy exception handling was enabled - now ParserGeneratorCC can create itself using itself!

v1.0.1 - 2018-01-05
* Initial release with expected output compatibility to JavaCC 7.0.3
* The JavaCC option `GENERATE_STRING_BUILDER` was removed - it was never evaluated
* The JavaCC option `LEGACY_EXCEPTION_HANDLING` was removed - that was too much 1990 ;) - see issue #7. This implies that the class `TokenMgrError` is no longer available!
* The JavaCC option `GENERATE_CHAINED_EXCEPTION` was replaced with deduction from the Java version (&ge; 1.4)
* The JavaCC option `GENERATE_GENERICS` was replaced with deduction from the Java version (&ge; 1.5)
* The JavaCC option `GENERATE_ANNOTATIONS` was replaced with deduction from the Java version (&ge; 1.5)
* The main applications previously located in the root packaged were moved to package `com.helger.pgcc.main`
* Minor improvements for Java output if JDK level is &ge; 1.7
* https://github.com/phax/ph-javacc-maven-plugin/ starting from v4 will use this package instead of JavaCC.
* No pure `RuntimeException` or `Error` classes are thrown inside the code

v1.0.0 - 2018-01-05 - had a regression and was therefore never released binary

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
