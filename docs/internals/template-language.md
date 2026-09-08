# The template language

Every file the generator emits that is not built line by line in Java comes from a `.template` file
under `src/main/resources/templates/`, rendered by `com.helger.pgcc.utils.OutputFileGenerator`.

The language is tiny and was previously documented only by its implementation.
`OutputFileGeneratorLanguageTest` now pins down every construct below; if this page and that test
ever disagree, the test is right.

## Substitution

| Written | Means |
|---|---|
| `${NAME}` | the option value, or nothing if the option is unset or empty |
| `${NAME:-fallback}` | the option value, or `fallback` if it is unset **or empty** |
| `${NAME?then:else}` | `then` if the condition `NAME` is true, else `else` |

The value of an option comes from the map passed to the `OutputFileGenerator` constructor, which is
`Options.getAllOptions ()` plus the derived `AT_LEAST_JDK6`, `AT_LEAST_JDK7` and `BEFORE_JDK7`
flags. Braces nest, so `${A?${B}:c}` works. An unbalanced `${` is an error, and so is a character
in a variable name that is not a Java identifier part.

Note that an empty value counts as unset for `:-`, which is why `${PARSER_NAME:-Parser}` falls back
rather than emitting nothing.

## Conditionals

Directives occupy a whole line and are matched by their leading `#` after trimming:

```
#if CONDITION
  …
#elif OTHER_CONDITION
  …
#else
  …
#fi
```

`#elif` and `#else` are optional and `#if` blocks nest. A missing `#fi` is an error
(`Missing "#fi"`), and so is anything other than `#fi` where the block should end.

## Conditions

Conditions are parsed by a second generated parser, `ConditionParser`, from
`src/main/javacc/ConditionParser.jj`. It supports:

| Operator | Meaning |
|---|---|
| `NAME` | the truth of an option, see below |
| `true` / `false` | literals |
| `!A` | negation |
| `A && B` | conjunction |
| <code>A &#124;&#124; B</code> | disjunction |
| `(A)` | grouping |

An option is true when it is `Boolean.TRUE`, or when it is a `String` that is non-empty and, after
trimming, is neither `false` nor `no` (case insensitive). Anything else, including an option that is
not in the map at all, is false. That is why `#if NODE_PREFIX` behaves as "a prefix was configured",
and why a condition that fails to parse silently evaluates to false rather than failing the build.

## Line endings

`OutputFileGenerator.setNewLineMode` decides what the emitted file uses. Templates themselves are
read as UTF-8 regardless of the platform.

## A worked example

`templates/java/stream/CharSequenceCharStream.template` starts:

```
#if SUPPORT_CLASS_VISIBILITY_PUBLIC
public
#fi
class CharSequenceCharStream
```

so `SUPPORT_CLASS_VISIBILITY_PUBLIC=false` produces a package private class, and everything else
about the file is identical. The same template uses `#if KEEP_LINE_COLUMN` to drop the entire line
and column machinery when the grammar does not need it, and `#if AT_LEAST_JDK6` to choose between
the `Charset` and the `String` encoding constructors.

## Gotchas

- `.template` files are excluded from the license header check, so they carry no BSD header.
- They are **not** Java. A `#if` inside a method body still has to leave valid Java behind on both
  branches, and nothing checks that but the compiler in
  `JavaTemplateValidityFuncTest` / `CharStreamBufferTest`.
- The classic and modern variants under `java/stream/` and `java/stream/modern/` are near duplicates
  and drift apart easily. See [the stream-template-change
  skill](../../.claude/skills/stream-template-change/SKILL.md).
