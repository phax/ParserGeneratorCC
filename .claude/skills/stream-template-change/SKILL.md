---
name: stream-template-change
description: Checklist for changing anything under src/main/resources/templates/ — keeping the classic/modern/C++ variants in parity, testing the generated code rather than the template text, and covering the option matrix. Use when editing char stream, token manager, parser or JJTree templates.
---

Templates are the emitted output, not compiled code — a change is only proven by generating,
compiling and running the result.

## 1. Find every variant that must change together

`src/main/resources/templates/` carries parallel copies that drift apart:

| Variant | Path |
|---|---|
| Classic Java | `stream/java/`, `*.template` at the templates root |
| Modern Java (`Provider`-based) | `stream/java/modern/` |
| C++ | `stream/cpp/` |
| GWT | `gwt/` |
| JJTree | `jjtree/` |

The classic and modern Java stream templates are near-identical — a fix in one almost always
belongs in the other. The C++ templates descend from the same JavaCC ancestor, so when the Java
side looks wrong, diff it against `stream/cpp/` before deciding what correct looks like; a
behaviour present in C++ but missing in Java is usually a porting regression.

Shared state lives in `stream/java/AbstractCharStream.template`. Fields there are used by *both*
`SimpleCharStream` and `JavaCharStream` for different purposes — check every usage before reusing
or resetting an inherited field.

## 2. Test the generated code

Follow the pattern in `src/test/java/com/helger/pgcc/output/java/JavaTemplateValidityFuncTest.java`
and `src/test/java/com/helger/pgcc/issues/Issue33Test.java`:

1. `FilesJava.setReadFromClassPath (false)` — otherwise the templates are read from an older
   `parser-generator-cc` jar on the classpath instead of this checkout. Restore it in a `finally`
   or `@AfterClass`; it is a static global.
2. `Main.mainProgram ("-JDK_VERSION=1.8", "-JAVA_TEMPLATE_TYPE=...", ..., "-OUTPUT_DIRECTORY=...", grammar)`
   returns `ESuccess`.
3. Compile the output with `ToolProvider.getSystemJavaCompiler ()`.
4. Load and run it through a `URLClassLoader`.

Never assert on template text.

## 3. Cover the option matrix

Stream template behaviour is preprocessor-driven. A change is under-tested unless it covers:

- `JAVA_TEMPLATE_TYPE` = `classic` and `modern`
- `JAVA_UNICODE_ESCAPE` = `true` (`JavaCharStream`) and `false` (`SimpleCharStream`)
- `KEEP_LINE_COLUMN` = `true` and `false` (this drives the `#if KEEP_LINE_COLUMN` blocks)

Buffer-related changes additionally need input long enough to force buffer growth and circular
wrap-around — the default buffer is 4096 chars, so bugs only appear past that.

## 4. Finish

Run `/pgcc-verify`, and add a bullet to the `v2.0.2 - work in progress` entry in `README.md`.
