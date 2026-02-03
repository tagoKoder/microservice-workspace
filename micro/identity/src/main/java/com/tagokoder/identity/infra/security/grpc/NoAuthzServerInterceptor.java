package com.tagokoder.identity.infra.security.grpc;

import io.grpc.*;

public class NoAuthzServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        return next.startCall(call, headers);
    }
}
