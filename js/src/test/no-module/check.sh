#!/usr/bin/env bash
set -eu

# Run from the repository root. These settings apply only to this sbt session.
sbt \
  'set scoptJS / Compile / unmanagedSourceDirectories += file("js/src/test/no-module").getCanonicalFile' \
  'set scoptJS / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.NoModule))' \
  'set scoptJS / Compile / fastLinkJS / scalaJSLinkerOutputDirectory := file("js/target/no-module-smoke").getCanonicalFile' \
  'scoptJS / Compile / fastLinkJS'

node js/src/test/no-module/run.cjs js/target/no-module-smoke/main.js
