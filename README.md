# ParserGeneratorCC

[![javadoc](https://javadoc.io/badge2/com.helger/parser-generator-cc/javadoc.svg)](https://javadoc.io/doc/com.helger/parser-generator-cc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger/parser-generator-cc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger/parser-generator-cc) 

Fork of JavaCC 7.0.3 because the original code base has poor quality and PRs were not really merged.
The overall goal is to maintain compatibility to JavaCC but
* The code itself should be better maintainable
* The code itself should conform to best-practices
* Because this is NOT JavaCC the class names are similar, but the base package name changed from `net.javacc` to `com.helger.pgcc`
* The created code requires Java 1.5  

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

# News and noteworthy

* v2.0.0 - 2025-09-19
    * Using Java 17 as the baseline
    * Updated to ph-commons 17
* v1.1.4 - 2022-01-10
    * Updated to ph-commons 10
    * Java non-modern style no longer creates constructors without a Charset. See [issue #29](https://github.com/phax/ParserGeneratorCC/issues/29) - thanks @sfuhrm
    * Improved the code of the `jj_ntk_f` method to be more efficient
* v1.1.3 - 2020-05-13
    * Allow `final` when catching exceptions - [issue #24](https://github.com/phax/ParserGeneratorCC/issues/24)
    * Changed Automatic-Module-Name to `com.helger.pgcc`
    * Fixed internal buffer corruption in generated code - [issue #26](https://github.com/phax/ParserGeneratorCC/issues/26)
* v1.1.2 - 2019-05-02
    * JavaCC grammar can parse Java 7 language features ([JavaCC PR 71](https://github.com/javacc/javacc/pull/71))
    * Backported a CPP output fix from JavaCC
    * Fixed an error in `JavaCharStream.template` if `KEEP_LINE_COLUMN` is turned off
* v1.1.1 - 2019-01-28
    * Fixed an error in Java modern template `SimpleCharStream` - thanks to @nbauma109
* v1.1.0 - 2018-10-25
    * Focus is on improving the quality and consistency of the template files - this may cause interoperability problem because names change etc.
    * This version assumes that the created code uses at least Java 1.5 - JDK versions below 1.5 are no longer supported!
    * The option `STATIC` was removed - it caused too many variations in the templates which decreases maintainability. STATIC is now always `false`!
    * Added new option `OUTPUT_ENCODING` to define the character set for the output files
    * If JDK version is at least 1.6 than `java.nio.Charset encoding` is used and not `String encoding` in generated Streams.
    * The Java templates were modified so that the interface methods start with a lowercase character (`beginToken` instead of `BeginToken`) - the created code may need to be adopted!
    * A masking issue for `'` and `\` characters was resolved (issue #20) 
* v1.0.2 - 2018-01-08
    * Fixed an error in jump-patching that was originally only considered if legacy exception handling was enabled - now ParserGeneratorCC can create itself using itself!
* v1.0.1 - 2018-01-05
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
* v1.0.0 - 2018-01-05 - had a regression and was therefore never released binary

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.