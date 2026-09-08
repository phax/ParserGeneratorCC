# Bootstrapping

This project's own parser is generated from `src/main/javacc/JavaCC.jj` and
`src/main/jjtree/JJTree.jjt` — by a **released** version of itself, wrapped in a released
`ph-javacc-maven-plugin`. That is the single most surprising thing about working here.

## The consequence

A change to the code generation does not affect this project's own build. Change a template, run
`mvn clean install`, and `target/generated-sources/` still contains what the *released* generator
produced. The change only reaches this build after:

1. `parser-generator-cc` is released, and
2. `ph-javacc-maven-plugin` is released with a dependency on that version, and
3. this project's `ph-javacc-maven-plugin.version` property is bumped.

Historically the project sat two releases behind, which is a large part of why the char stream bugs
in issues #33 and #46 survived as long as they did: the generator never ran its own fixed code
against its own grammar.

Two things follow, and both have bitten people:

- **Do not assume a codegen change is self-applying.** If you changed a template and the build still
  behaves the old way, that is expected, not a bug.
- **Do not "fix" a build failure by assuming the local generator was used.** It was not.

## The self-hosting loop

The `selfhost` profile closes the gap for testing. It needs the sibling `ph-javacc-maven-plugin`
checkout:

```
# 1. build this project with the released plugin, and install the snapshot
mvn clean install -DskipTests

# 2. rebuild the plugin around that snapshot - the -D override means the sibling
#    checkout is never modified
cd ../ph-javacc-maven-plugin
mvn clean install -DskipTests -Dpgcc.version=<this project's SNAPSHOT version>
cd -

# 3. build this project with its own generator
mvn clean verify -Pselfhost
```

Step 3 is the real signal: the parser generator parsing its own grammar with its own current code.

The default profile deliberately stays on a released plugin, because a release build has to work
with what is on Maven Central. `selfhost` pins `ph-javacc-maven-plugin.version` to the plugin's
current SNAPSHOT.

## What this means for the grammar files

`JavaCC.jj` and `JJTree.jjt` have to stay parseable by the **last released** generator. That freezes
the grammar *syntax* they may use — a new construct cannot be used in the file that defines it until
a release later.

It does not freeze their action code. The Java inside `{ … }` blocks is copied through verbatim, so
it can be refactored freely; the state migration changed it and only needed one added import.
