# Generate Smithy4s code on the fly

## Modules

### Backend

Full stack Scala application with a Scalajs frontend. Depends on [Frontend](#frontend). This application is built on on top of Smithy4s and is deployed as a Docker container on fly.io.

Live at: https://morning-bird-7081.fly.dev/

Deployed on a very small 2vcpu and 512mb vm.

### Frontend

ScalaJS application shipped in [Backend](#backend).

### Developement

It is recomended that you have 3 long running proccesses to develop this application:

1. backend: `sbt ~backend/reStart`
2. frontend scalajs: `sbt "~frontend/fastLinkJS"`
3. cd into `modules/frontend` and run `npm i && npm run dev`

You could drop 3 if you've built it once and your backend is running serving the generated frontend assets.


## Fly.io

Use the following command to use the right version of flyctl via nix:
`nix-shell -p flyctl -I nixpkgs=https://github.com/NixOS/nixpkgs/archive/555bd32eb477d657e133ad14a5f99ac685bfdd61.tar.gz`
