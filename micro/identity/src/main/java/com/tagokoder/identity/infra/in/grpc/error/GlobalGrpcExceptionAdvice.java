package com.tagokoder.identity.infra.in.grpc.error;

import com.tagokoder.identity.infra.security.context.AuthCtx;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcAdvice
public class GlobalGrpcExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalGrpcExceptionAdvice.class);

    private static final Metadata.Key<String> CORR_HDR =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgument(IllegalArgumentException ex) {
        // Mensaje controlado (sin detalles internos)
        return withCorr(Status.INVALID_ARGUMENT.withDescription("Invalid request").asRuntimeException());
    }

    @GrpcExceptionHandler(StatusRuntimeException.class)
    public StatusRuntimeException handleStatusRuntime(StatusRuntimeException ex) {
        // Ya viene “sanitizado” (por tus validators o authz)
        return withCorr(ex);
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleUnexpected(Exception ex) {
        String corrId = AuthCtx.CORRELATION_ID.get();

        // Log interno con stacktrace (solo server logs)
        log.error("Unhandled error corr_id={}", corrId, ex);

        // Cliente: genérico
        return withCorr(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
    }

    private StatusRuntimeException withCorr(StatusRuntimeException ex) {
        String corrId = AuthCtx.CORRELATION_ID.get();
        Metadata trailers = new Metadata();
        if (corrId != null && !corrId.isBlank()) {
            trailers.put(CORR_HDR, corrId);
        }
        // Re-empaqueta con trailers
        return Status.fromThrowable(ex).asRuntimeException(trailers);
    }
}
