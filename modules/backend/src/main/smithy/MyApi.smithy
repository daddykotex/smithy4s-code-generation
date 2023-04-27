$version: "2"

namespace smithy4s.hello

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
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
