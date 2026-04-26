package io.quarkus.arc.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;

@Interceptor
@ActivateRequestContext
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class ActivateRequestContextInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        switch (ReactiveType.valueOf(ctx.getMethod())) {
            case UNI:
                return MutinyActivateRequestContext.invokeUni(ctx);
            case MULTI:
                return MutinyActivateRequestContext.invokeMulti(ctx);
            case STAGE:
                return invokeStage(ctx);
            default:
                return invoke(ctx);
        }
    }

    private CompletionStage<?> invokeStage(InvocationContext ctx) {
        ManagedContext requestContext = Arc.requireContainer().requestContext();
        if (requestContext.isActive()) {
            return proceedWithStage(ctx);
        }

        return activate(requestContext)
                .thenCompose(state -> proceedWithStage(ctx).whenComplete((r, t) -> {
                    requestContext.destroy(state);
                    requestContext.deactivate();
                }));
    }

    private static CompletionStage<InjectableContext.ContextState> activate(ManagedContext requestContext) {
        try {
            requestContext.activate();
            return CompletableFuture.completedStage(requestContext.getState());
        } catch (Throwable t) {
            return CompletableFuture.failedStage(t);
        }
    }

    private CompletionStage<?> proceedWithStage(InvocationContext ctx) {
        try {
            return (CompletionStage<?>) ctx.proceed();
        } catch (Throwable t) {
            return CompletableFuture.failedStage(t);
        }
    }

    private Object invoke(InvocationContext ctx) throws Exception {
        ManagedContext requestContext = Arc.requireContainer().requestContext();
        if (requestContext.isActive()) {
            return ctx.proceed();
        }

        try {
            requestContext.activate();
            return ctx.proceed();
        } finally {
            requestContext.terminate();
        }
    }
}
