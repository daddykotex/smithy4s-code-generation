#!/bin/bash

set -euo pipefail

export BUNDLE_ASSETS="true"

(cd modules/frontend; npm i && npm run build)
sbt "backend / Docker / publish"