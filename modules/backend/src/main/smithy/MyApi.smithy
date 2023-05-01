$version: "2"

namespace smithy4s_codegen.api

use alloy#simpleRestJson

@simpleRestJson
service SmithyCodeGenerationService {
    version: "1.0.0"
    operations: [
        HealthCheck
        SmithyValidate
        Smithy4sConvert
    ]
}

@http(method: "GET", uri: "/health", code: 200)
operation HealthCheck {
    output := {
        @required
        message: String
    }
}

@http(method: "POST", uri: "/smithy/validate", code: 200)
operation SmithyValidate {
    input := {
        @required
        content: String
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
    }
    output := {
        @required
        message: String
    }
}
