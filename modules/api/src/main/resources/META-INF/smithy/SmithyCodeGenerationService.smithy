$version: "2"

namespace smithy4s_codegen.api

use alloy#simpleRestJson

@simpleRestJson
service SmithyCodeGenerationService {
    version: "1.0.0"
    operations: [
        HealthCheck
        GetConfiguration
        SmithyValidate
        Smithy4sConvert
    ]
}

@http(method: "GET", uri: "/health", code: 200)
@readonly
operation HealthCheck {
    output := {
        @required
        message: String
    }
}

@http(method: "GET", uri: "/configuration", code: 200)
@readonly
operation GetConfiguration {
    output := {
        @required
        availableDependencies: Dependencies
    }
}

@http(method: "POST", uri: "/smithy/validate", code: 200)
operation SmithyValidate {
    input := {
        @required
        content: String
        @documentation("If omitted, use the server's default.")
        deps: Dependencies
    }
    errors: [InvalidSmithyContent]
}

@error("client")
structure InvalidSmithyContent {
    @required
    errors: ErrorMessages
}

@length(min: 1)
list ErrorMessages {
    member: String
}

@http(method: "POST", uri: "/smithy4s/convert", code: 200)
operation Smithy4sConvert {
    input := {
        @required
        content: String
        @documentation("If omitted, use the server's default.")
        deps: Dependencies
    }
    output := {
        @required
        generated: Smithy4sGeneratedContent
    }
    errors: [InvalidSmithyContent]
}

string Path

string Content

map Smithy4sGeneratedContent {
    key: Path
    value: Content
}

string Dependency

list Dependencies {
    member: Dependency
}
