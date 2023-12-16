#!/bin/bash

set -eo pipefail

export BUNDLE_ASSETS="true"

if [[ "$PUBLISH_OFFICIAL" == "true" ]]; then
    BACKEND_PUBLISH="publish"
else
    BACKEND_PUBLISH="publishLocal"
fi

set -u

(cd modules/frontend; npm i && npm run build)
sbt "backend / Docker / $BACKEND_PUBLISH; backendDependencies / Docker / $BACKEND_PUBLISH"