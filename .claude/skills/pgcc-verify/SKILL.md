---
name: pgcc-verify
description: Run the full ParserGeneratorCC verification — mvn clean test from the repo root, clean up the working-tree noise the tests leave behind, then check BSD license headers. Use after changing anything under src/, and before reporting that a change works.
---

Run every step. Do not skip the cleanup — `mvn test` dirties tracked files.

## 1. Test

Always from the repository root (tests use relative paths and fail from anywhere else):

```
mvn clean test
```

If a single area changed, a targeted run first is fine (`mvn test -Dtest=<Name>`), but finish with
the full `mvn clean test` before reporting success.

## 2. Clean up test side effects

```
git checkout -- www/doc/
rm -f JavaCCParserTokenManager.java
```

`JJDocMainTest` rewrites the tracked `www/doc/JavaCC.html` and `www/doc/JavaCC.txt`; those are test
noise and must never be committed. `JavaCCParserTokenManager.java` is stray root output.

## 3. License headers

```
mvn license:check
```

New or moved `.java` files need the BSD header from `src/etc/license-template.txt` (Philip Helger
2017-2026 + Google 2011 + Sun Microsystems 2006) — not Apache 2.0. `mvn license:format` applies it.

## 4. Report

State the actual surefire totals (`Tests run / Failures / Errors / Skipped`) and confirm
`git status` is clean apart from the intended changes. If anything failed, show the failure output
rather than summarizing it.
