# Generate Smithy4s code on the fly

## Modules

### Backend

Full stack Scala application with a Scalajs frontend. Depends on [Frontend](#frontend). This application is built on on top of Smithy4s and is deployed as a Docker container on fly.io.

Live at: https://morning-bird-7081.fly.dev/

### Frontend

ScalaJS application shipped in [Backend](#backend).

### Developement

It is recomended that you have 3 long running proccesses to develop this application:

1. backend: `sbt ~backend/reStart`
2. frontend scalajs: `sbt "~frontend/fastLinkJS"`
3. cd into `modules/frontend` and run `npm i && npm run dev`

You could drop 3 if you've built it once and your backend is running serving the generated frontend assets.
