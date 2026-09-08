---
name: pgcc-verify
description: Run the full ParserGeneratorCC verification — mvn clean test from the repo root, then check BSD license headers, then confirm the working tree is clean. Use after changing anything under src/, and before reporting that a change works.
---

Run every step.

## 1. Test

Always from the repository root (tests use relative paths and fail from anywhere else):

```
mvn clean test
```

If a single area changed, a targeted run first is fine (`mvn test -Dtest=<Name>`), but finish with
the full `mvn clean test` before reporting success.

## 2. License headers

```
mvn license:check
```

New or moved `.java` files need the BSD header from `src/etc/license-template.txt` (Philip Helger
2017-2026 + Google 2011 + Sun Microsystems 2006) — not Apache 2.0. `mvn license:format` applies it.

## 3. Working tree

```
git status --porcelain
```

A test run must leave the working tree untouched. If a tracked file changed or a new file appeared
in the repository root, that is a bug in the test, not something to clean up by hand — fix the test
to write below `target/`.

## 4. Report

State the actual surefire totals (`Tests run / Failures / Errors / Skipped`) and confirm
`git status` is clean apart from the intended changes. If anything failed, show the failure output
rather than summarizing it.
