# HTTP Request Logging

The application records HTTP requests to aid debugging and monitoring. Logs are written using the `httptrace` logger to two points in the request lifecycle:

1. **Request received** – emitted by `IncomingRequestLoggingFilter` as soon as the server accepts the request.
2. **Request completed** – emitted when response processing finishes via `LogHttpTraceRepository`.

Both logs contain the HTTP method, path, headers and client address. Completion logs additionally include response status and request duration in milliseconds. Slow requests over one second are logged with an extra `[slow_request]` marker.
