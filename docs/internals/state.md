# Generator state

The generator historically kept everything it knew in `static` fields — 117 mutable ones across 15
classes when this work started. That had three consequences: two runs in the same JVM depended on
each other, two runs could not happen at once, and `Main.reInitAll` had to be kept in sync by hand
with a list of `reInit` methods that nothing enforced.

## Where the state lives now

`com.helger.pgcc.context.PGCCContext` is a `ThreadLocal` holder for the state of one run.
`PGCCContext.reset ()` starts a fresh one.

| Slot | Holds | Was |
|---|---|---|
| `errors ()` | parse errors, semantic errors, warnings | 3 statics in `JavaCCErrors` |
| `options ()` | option values, which options came from where, the output language | 4 statics in `Options` |
| `grammar ()` | the whole parsed grammar: productions, tokens, lexical states, counters | 16 statics + 14 static collections in `JavaCCGlobals` |
| `parserBuild ()` | which compilation unit list is being filled, the next free lexical state | 5 statics in `JavaCCParserInternals` |
| `lookahead ()` | the depth of the current lookahead computation and the matches that hit it | `MatchInfo.s_laLimit` and 2 statics in `LookaheadWalk` |
| `semanticize ()` | deferred removals, the recursion path found so far | 3 statics in `Semanticize` |
| `jjtree ()` | the parser being decorated, the package names, the class declaration pieces | 6 statics in `JJTreeGlobals` |
| `jjdoc ()` | input file, output file, output generator | 3 statics in `JJDocGlobals` |
| `lexer ()` | everything the token manager generation works with | 43 statics in `LexGenJava` |

The old classes are still the API — `JavaCCGlobals.grammar ()`, `Options.getOutputLanguage ()`,
`JavaCCErrors.warning (…)` — they just delegate. Grammar action code in `JavaCC.jj` and
`JJTree.jjt` did not have to change beyond one import.

## What is still static, and why

| Class | Count | Why |
|---|---|---|
| `NfaState` | 17 | reset per *lexical state*, not per run |
| `ExpRStringLiteral` | 12 | same |
| `PGPrinter` | 2 | the console, legitimately process wide |
| `FilesJava` | 1 | a test hook that makes templates load from the checkout |

`NfaState` and `ExpRStringLiteral` are last for a reason: their state is reset once *per lexical
state*, not per run — `NfaState.reInitStatic ()` and `ExpRStringLiteral.reInitStatic ()` are called
inside the loop in `LexGenJava.start ()`. `LexerState` holds the run-scoped part already; what is
left is the per-lexical-state DFA construction, which wants to be an object created inside that
loop rather than a slot in the run's context.

## Two statics that disappeared rather than moved

`Semanticize.other` was not state: `hasIgnoreCase` wrote its result into a static field for the
caller to read back. It is now the return value of `findIgnoreCase`. When a static turns out to be a
scratch variable, deleting it beats relocating it.

`JavaCCErrors` kept its three counters even after `ErrorCollector` had superseded them.

## The guard rails

Two tests keep this honest while it moves:

- `StateIsolationTest` generates a grammar, generates something with a different shape, generates
  the first grammar again, and requires the two results to be identical byte for byte — for JavaCC,
  for JJTree, and for Java output following C++ output.
- `PGCCContextTest` shows that what has already migrated is isolated per thread. It deliberately
  does **not** claim that two full generator runs can share a JVM concurrently; the lexer trio has
  to move first.
