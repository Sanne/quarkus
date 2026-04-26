package io.quarkus.arc.runtime.graal;

import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

import jakarta.interceptor.InvocationContext;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.arc.impl.ReactiveType;

/**
 * When Mutiny is absent, replace the interceptor's aroundInvoke with a version that omits
 * the UNI/MULTI branches. This prevents GraalVM's reachability analysis from pulling
 * MutinyActivateRequestContext (and transitively Mutiny + jctools) into the native image.
 */
public class MutinySubstitutions {

    static final class IsMutinyAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("io.smallrye.mutiny.Uni");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}

@TargetClass(className = "io.quarkus.arc.impl.ActivateRequestContextInterceptor", onlyWith = MutinySubstitutions.IsMutinyAbsent.class)
final class Target_ActivateRequestContextInterceptor {

    @Alias
    private CompletionStage<?> invokeStage(InvocationContext ctx) {
        return null;
    }

    @Alias
    private Object invoke(InvocationContext ctx) throws Exception {
        return null;
    }

    @Substitute
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        switch (ReactiveType.valueOf(ctx.getMethod())) {
            case STAGE:
                return invokeStage(ctx);
            default:
                return invoke(ctx);
        }
    }
}
