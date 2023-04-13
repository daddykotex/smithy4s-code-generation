$version: "2"

namespace smithy4s.hello

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
    version: "1.0.0"
    operations: [HealthCheck]
}

@http(method: "GET", uri: "/health", code: 200)
operation HealthCheck {
    output := {
        @required
        message: String
    }
}
