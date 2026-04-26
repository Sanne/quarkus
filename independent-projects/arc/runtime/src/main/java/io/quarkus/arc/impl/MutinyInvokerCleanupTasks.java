package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.CreationalContext;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Separated from {@link InvokerCleanupTasks} so that Mutiny types are only class-loaded
 * when the invoked method actually returns a Uni or Multi. This keeps Mutiny an optional
 * dependency of ArC — do not merge back into InvokerCleanupTasks.
 *
 * @see InvokerCleanupTasks
 */
public class MutinyInvokerCleanupTasks {

    public static <T> Uni<T> deferRelease(CreationalContext<?> cc, Uni<T> uni) {
        return uni.onTermination().invoke(cc::release);
    }

    public static <T> Multi<T> deferRelease(CreationalContext<?> cc, Multi<T> multi) {
        return multi.onTermination().invoke(cc::release);
    }
}
