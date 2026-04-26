package io.quarkus.arc.impl;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * A method reactive returned type.
 */
public enum ReactiveType {

    NON_REACTIVE(false, null),

    UNI(true, safeLoadClass("io.smallrye.mutiny.Uni")),

    MULTI(true, safeLoadClass("io.smallrye.mutiny.Multi")),

    STAGE(true, CompletionStage.class);

    private final boolean reactive;
    private final Class<?> type;

    ReactiveType(boolean reactive, Class<?> type) {
        this.reactive = reactive;
        this.type = type;
    }

    boolean isReactive() {
        return reactive;
    }

    public static ReactiveType valueOf(Method method) {
        if (Void.class.equals(method.getReturnType())) {
            return NON_REACTIVE;
        }

        for (ReactiveType reactiveType : ReactiveType.values()) {
            if (Objects.nonNull(reactiveType.type)
                    && reactiveType.type.isAssignableFrom(method.getReturnType())) {
                return reactiveType;
            }
        }

        return NON_REACTIVE;
    }

    // Mutiny is an optional dependency — use Class.forName to avoid NoClassDefFoundError
    // when Mutiny is not on the classpath (e.g. CLI apps, non-reactive applications)
    private static Class<?> safeLoadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
