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
| `lexer ().nfa ()` | the NFA/DFA of the lexical state being generated | 17 statics + 11 collections in `NfaState` |
| `lexer ().stringLiterals ()` | the string literal trie of that lexical state | 12 statics in `ExpRStringLiteral` |

The old classes are still the API — `JavaCCGlobals.grammar ()`, `Options.getOutputLanguage ()`,
`JavaCCErrors.warning (…)` — they just delegate. Grammar action code in `JavaCC.jj` and
`JJTree.jjt` did not have to change beyond one import.

## What is still static, and why

| Class | Count | Why |
|---|---|---|
| `PGPrinter` | 2 | the console, legitimately process wide |
| `FilesJava` | 1 | a test hook that makes templates load from the checkout |

That is all of them: **117 mutable statics at the start of this work, 3 now**, and neither of the
remaining classes holds anything about a generator run.

### The `static final` trap

Counting `static` fields that are not `final` misses a whole category, and eleven of these were
found only after the migration was declared finished. A `static final Map` is a *constant reference
to a mutable object*: it reads like `MAJOR_TO_VERSION` and behaves like a global variable.

| Where | What | What it did |
|---|---|---|
| `NfaState` | 4 maps | tokenizer data for `JavaCCInterpreter`, never cleared |
| `ExpRStringLiteral` | 4 maps | the literal tables for the same, never cleared |
| `NodeFilesCpp` | 2 collections | the node types to emit and the headers to include |
| `NodeFilesJava` | 1 set | the node files already written |

The JJTree ones were not theoretical. Running `alpha.jjt` and then `beta.jjt` in one JVM put
`ASTAlfa.h`, `ASTAlfa.cc` and a reference to `Alfa` into *Beta's* output directory, because
`NodeFilesCpp` kept adding to the same set. `StateIsolationTest` could not see it: it generated one
grammar twice and compared, and a leak that adds the same extra node to both runs cancels out. It
takes two *different* grammars to expose, which is what
`testJJTreeDoesNotCarryNodesIntoTheNextRun` now does.

The eight tokenizer data maps moved into `TokenizerDataBuildState`, held by `LexerState`. Its
lifetime is the run rather than the lexical state - it collects one entry per lexical state - so it
sits beside `nfa ()` and `stringLiterals ()` rather than inside them, and it has no reset method
because `Main.reInitAll ()` drops the whole context anyway. The three JJTree collections moved into
`JJTreeState`.

When looking for state, grep for `static final` holding a collection as well as for plain mutable
statics.

`NfaState` and `ExpRStringLiteral` needed one extra idea. Their state is not per run, it is per
*lexical state* — `LexGenJava.start ()` walks the lexical states and rebuilds the NFA and the
string literal trie for each. So `LexerState` holds a `NfaBuildState` and a
`StringLiteralBuildState` with their own `resetForLexicalState ()`, which is what the loop calls.
Note what deliberately survives that reset: the token images, the character counter and the boiler
plate flag are set once per run, before the loop.

`Main.reInitAll ()` is now two lines - drop the context, refill the option defaults.

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
- `PGCCContextTest` shows that the options and error counters are isolated per thread.
- `ConcurrentGenerationTest` is the payoff: two grammars generated **at the same time**, four rounds
  of real interleaving, each byte-identical to what it produces on its own. Verified that it means
  something - turning the context from a `ThreadLocal` into a plain static makes it fail.
