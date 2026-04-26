package io.quarkus.arc.impl;

import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Separated from {@link ActivateRequestContextInterceptor} so that Mutiny types are only
 * class-loaded when the intercepted method actually returns a Uni or Multi. This keeps
 * Mutiny an optional dependency of ArC — do not merge back into the interceptor.
 *
 * @see ActivateRequestContextInterceptor
 */
class MutinyActivateRequestContext {

    static Multi<?> invokeMulti(InvocationContext ctx) {
        return Multi.createFrom().deferred(() -> {
            ManagedContext requestContext = Arc.requireContainer().requestContext();
            if (requestContext.isActive()) {
                return proceedWithMulti(ctx);
            }

            return Multi.createFrom().deferred(() -> {
                requestContext.activate();
                InjectableContext.ContextState state = requestContext.getState();
                return proceedWithMulti(ctx)
                        .onTermination().invoke(() -> {
                            requestContext.destroy(state);
                            requestContext.deactivate();
                        });
            });
        });
    }

    static Uni<?> invokeUni(InvocationContext ctx) {
        return Uni.createFrom().deferred(() -> {
            ManagedContext requestContext = Arc.requireContainer().requestContext();
            if (requestContext.isActive()) {
                return proceedWithUni(ctx);
            }

            return Uni.createFrom().deferred(() -> {
                requestContext.activate();
                InjectableContext.ContextState state = requestContext.getState();
                return proceedWithUni(ctx)
                        .eventually(() -> {
                            requestContext.destroy(state);
                            requestContext.deactivate();
                        });
            });
        });
    }

    private static Multi<?> proceedWithMulti(InvocationContext ctx) {
        try {
            return (Multi<?>) ctx.proceed();
        } catch (Throwable t) {
            return Multi.createFrom().failure(t);
        }
    }

    private static Uni<?> proceedWithUni(InvocationContext ctx) {
        try {
            return (Uni<?>) ctx.proceed();
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }
}
