# The pipeline

What `com.helger.pgcc.parser.Main.mainProgram` does, in order. Everything below is read off that
method and the classes it calls.

```
 command line ─┐
               ├─→ Options ─→ grammar file ─→ JavaCCParser ─→ GrammarState
 grammar file ─┘                                                   │
                                                                   ├─→ Semanticize
                                                                   │      (checks + numbering)
                                                                   ├─→ ParseGenJava | ParseGenCPP
                                                                   │      (the parser)
                                                                   ├─→ LexGenJava  | LexGenCpp
                                                                   │      (the token manager)
                                                                   └─→ OtherFilesGenJava | …CPP
                                                                          (constants + boilerplate)
```

## 1. Options

`Main` reads every argument except the last as an option, so `-STATIC=false Foo.jj` is the shape.
Options set on the command line win over the grammar's `options { … }` block; both funnel through
`Options.setCmdLineOption` / `setInputFileOption`, which store into the map and then call
`_applyIndirectOptionFlags` for the handful of options whose value is not fully described by that
map entry — `OUTPUT_LANGUAGE`, `JAVA_TEMPLATE_TYPE`, `JAVA_CHAR_STREAM_TYPE`, `NAMESPACE`.

`Options.normalize` runs afterwards from the grammar's action code, and is where cross-option rules
live (`DEBUG_LOOKAHEAD` implies `DEBUG_PARSER`, `JAVA_UNICODE_ESCAPE` overrides
`JAVA_CHAR_STREAM_TYPE=charsequence`).

## 2. Parsing the grammar

`JavaCCParser` is itself generated from `src/main/javacc/JavaCC.jj`. Its action code fills
`GrammarState` through the `JavaCCGlobals` facade and through `JavaCCParserInternals`, which tracks
which of the three "compilation unit" token lists is currently being filled:

| List | Content |
|---|---|
| `cuToInsertionPoint1` | everything before the parser class declaration — package, imports |
| `cuToInsertionPoint2` | the parser class declaration itself |
| `cuFromInsertionPoint2` | the parser class body, which is copied into the generated parser |

The productions land in `bnfProductions` in declaration order, the token productions in
`rexprList`, and the lexical states get their indices from `ParserBuildState`.

## 3. Semantic checks — `Semanticize.start`

A sequence of passes over the parse tree, each one commented in place. In order:

1. Convert `LOOKAHEAD` specifications that are not at a choice point into trivial choices, so that
   their semantic lookahead can be evaluated together with the rest.
2. Populate `productionTable` from `bnfProductions`.
3. Check that every non-terminal used on a right hand side is defined on a left hand side.
4. Check that every target lexical state exists; detect `<EOF>` and `<name>` in token productions;
   check inline private regular expressions. Behaves differently under `USER_TOKEN_MANAGER`.
5. Fill `namedTokensTable` and `orderedNameTokens`, flagging duplicate labels.
6. Merge multiple uses of the same string literal in the same lexical state, assign every regular
   expression its ordinal, and fill `simpleTokensTable`. This is where `findIgnoreCase` reports a
   literal that can never match because a more general `IGNORE_CASE` expression exists.
7. Compute `emptyPossible` for each production, check that `(…)*`, `(…)?` and `(…)+` cannot expand
   to the empty string, and detect left recursion and loops in regular expressions.

Errors and warnings go to `JavaCCErrors`, which counts them in the run's `ErrorCollector`.
Generation continues after an error so that as many problems as possible are reported at once; the
error count decides the exit status.

## 4. Generating the parser

`ParseGenJava.start` emits the parser class: the copied compilation unit, the `jj_input_stream`
field of whichever char stream the options select, the constructors and `ReInit` methods, then one
method per production produced by `ParseEngine`, then the lookahead machinery (`jj_2_*`,
`jj_3_*`, the `jj_la1` bit masks) and the error handling.

`ParseEngine` is where the LL(k) decisions are made. For each choice it asks `LookaheadCalc`
whether one token is enough to decide; if not it emits a syntactic lookahead routine that tries the
alternatives. `LookaheadWalk` computes the first sets, bounded by `LookaheadState.getLimit`, and
collects the matches that hit that bound so the ambiguity report can show them.

## 5. Generating the token manager

`LexGenJava.start` walks the token productions per lexical state and builds, for each state:

- the string literal matcher, in `ExpRStringLiteral` — a trie over the literals compiled into the
  `jjMoveStringLiteralDfa*` methods
- the NFA for everything that is not a plain literal, in `NfaState`, which is then converted to a
  DFA and emitted as the `jjMoveNfa_*` methods and the `jjnextStates` table

`NfaState.reInitStatic` and `ExpRStringLiteral.reInitStatic` are called *per lexical state*, not per
run — that is why those two classes still keep static state while the rest of the generator does
not.

The emitted methods are wrapped by `templates/java/TokenManagerBoilerPlateMethods.template`, which
contains `getNextToken`, `jjFillToken` and the skip/more/special handling.

## 6. The remaining files

`OtherFilesGenJava.start` writes `<Parser>Constants.java` and, unless the grammar opts out with
`GENERATE_BOILERPLATE=false`, the shared classes: `Token`, `ParseException`, `TokenMgrError`, the
`CharStream` interface and the char stream implementation that the options select.

Which char stream that is:

| `JAVA_UNICODE_ESCAPE` | `JAVA_CHAR_STREAM_TYPE` | Generated |
|---|---|---|
| true | (ignored) | `JavaCharStream` on `AbstractCharStream` |
| false | `simple` (default) | `SimpleCharStream` on `AbstractCharStream` |
| false | `charsequence` | `CharSequenceCharStream`, no buffer at all |

`USER_CHAR_STREAM=true` skips all of them and generates only the interface.
