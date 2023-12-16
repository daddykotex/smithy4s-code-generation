# Generate Smithy4s code on the fly <!-- omit in toc -->

- [Modules](#modules)
  - [Backend](#backend)
    - [Configuration](#configuration)
  - [Frontend](#frontend)
  - [Developement](#developement)
- [Dockerhub](#dockerhub)
- [Fly.io](#flyio)


## Modules

### Backend

Full stack Scala application with a Scalajs frontend. Depends on [Frontend](#frontend). This application is built on on top of Smithy4s and is deployed as a Docker container on fly.io.

Live at: https://morning-bird-7081.fly.dev/

Deployed on a very small 2vcpu and 512mb vm.

#### Configuration

The backend can be configured.

1. `SMITHY_CLASSPATH_CONFIG`: location of a json file used to load dependencies to include during code gen. The file looks content looks like this:

```json
{
  "entries": {
    "com.disneystreaming.alloy:alloy-core:0.2.8": "/opt/docker/smithy-classpath/alloy-core-0.2.8.jar"
  }
}
```

### Frontend

ScalaJS application shipped in [Backend](#backend).

### Developement

It is recomended that you have 3 long running proccesses to develop this application:

1. backend: `sbt ~backend/reStart`
2. cd into `modules/frontend` and run `npm i && npm run dev`
3. frontend scalajs: `sbt "~frontend/fastLinkJS"`

## Dockerhub

The images are pushed to the [docker hub](https://hub.docker.com/repository/docker/daddykotex/smithy4s-code-generation/general) so you can deploy them on your own infrastructure.

* latest: daddykotex/smithy4s-code-generation:latest
* latest: daddykotex/smithy4s-code-generation:with-dependencies
* daddykotex/smithy4s-code-generation:$SHA
* daddykotex/smithy4s-code-generation:with-dependencies-$SHA

## Fly.io

The images are pushed to the fly.io registry so that it can be deployed quickly from there.

Use the following command to use the right version of flyctl via nix:
`nix-shell -p flyctl -I nixpkgs=https://github.com/NixOS/nixpkgs/archive/555bd32eb477d657e133ad14a5f99ac685bfdd61.tar.gz`
