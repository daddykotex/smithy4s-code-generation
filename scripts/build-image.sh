#!/bin/bash

set -eo pipefail

export BUNDLE_ASSETS="true"

if [[ "$PUBLISH_OFFICIAL" == "true" ]]; then
    BACKEND_PUBLISH="publish"
else
    BACKEND_PUBLISH="publishLocal"
fi

set -u

# build frontend so it's available to be bundled
(cd modules/frontend; npm i && npm run build)

# build backend w/ default dependencies
publish_backend="backend / Docker / $BACKEND_PUBLISH"
sbt "$publish_backend"

# build backend w/ additional dependencies
tag_override="set backend / dockerTagOverride := Some(\"with-dependencies\")"
smithy_classpath="set backend / smithyClasspath ++= Seq(\"com.disneystreaming.alloy\" % \"alloy-core\" % \"0.2.8\")"
sbt "$tag_override; $smithy_classpath; $publish_backend"