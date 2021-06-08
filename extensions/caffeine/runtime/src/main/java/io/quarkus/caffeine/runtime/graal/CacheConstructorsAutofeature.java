package io.quarkus.caffeine.runtime.graal;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.oracle.svm.core.annotate.AutomaticFeature;

/**
 * This Automatic Feature for GraalVM will register for reflection
 * the most commonly used cache implementations from Caffeine.
 * It's implemented as an explicit @{@link Feature} rather than
 * using the Quarkus builditems because it doesn't need to be
 * dynamically tuned (the list is static), and to take advantage
 * of the reachability information we can infer from @{@link org.graalvm.nativeimage.hosted.Feature.DuringAnalysisAccess}.
 *
 * This allows us to register for reflection these resources only if
 * Caffeine is indeed being used: only if the cache builder is reachable
 * in the application code.
 */
@AutomaticFeature
public class CacheConstructorsAutofeature implements Feature {

    private final AtomicBoolean triggeredLocalCacheFactoryClass = new AtomicBoolean(false);
    private final AtomicBoolean triggeredNodeFactoryClass = new AtomicBoolean(false);
    private static final String SHARED_CLASSNAME_PREFIX = "com.github.benmanes.caffeine.cache.";

    /**
     * To set this, add `-J-Dio.quarkus.caffeine.graalvm.diagnostics=true` to the native-image parameters
     */
    private static final boolean log = Boolean.getBoolean("io.quarkus.caffeine.graalvm.diagnostics");

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> localCacheFactoryClass = access.findClassByName("com.github.benmanes.caffeine.cache.LocalCacheFactory");
        access.registerReachabilityHandler(this::ensureLocalCacheFactorySupported, localCacheFactoryClass);
        Class<?> nodeFactoryClass = access.findClassByName("com.github.benmanes.caffeine.cache.NodeFactory");
        access.registerReachabilityHandler(this::ensureNodeFactorySupported, nodeFactoryClass);
    }

    private void ensureNodeFactorySupported(DuringAnalysisAccess duringAnalysisAccess) {
        final boolean needsEnablingYet = triggeredNodeFactoryClass.compareAndSet(false, true);
        if (needsEnablingYet) {
            if (log) {
                System.out.println(
                        "Quarkus's automatic feature for GraalVM native images: enabling limited support for Caffeine's [com.github.benmanes.caffeine.cache.NodeFactory]");
            }
            registerCaffeineReflections(duringAnalysisAccess, typesfromNodeFactory());
        }
    }

    private void ensureLocalCacheFactorySupported(DuringAnalysisAccess duringAnalysisAccess) {
        final boolean needsEnablingYet = triggeredLocalCacheFactoryClass.compareAndSet(false, true);
        if (needsEnablingYet) {
            if (log) {
                System.out.println(
                        "Quarkus's automatic feature for GraalVM native images: enabling limited support for Caffeine's [com.github.benmanes.caffeine.cache.LocalCacheFactory]");
            }
            registerCaffeineReflections(duringAnalysisAccess, typesfromLocalCacheFactory());
        }
    }

    private void registerCaffeineReflections(DuringAnalysisAccess duringAnalysisAccess, String[] strings) {
        for (String postfix : strings) {
            String className = SHARED_CLASSNAME_PREFIX + postfix;
            registerForReflection(className, duringAnalysisAccess);
        }
    }

    private void registerForReflection(
            String className,
            DuringAnalysisAccess duringAnalysisAccess) {
        final Class<?> aClass = duringAnalysisAccess.findClassByName(className);
        final Constructor<?>[] z = aClass.getDeclaredConstructors();
        RuntimeReflection.register(aClass);
        RuntimeReflection.register(z);
    }

    // N.B. the following lists are not complete, but a selection of the types we expect being most useful.
    // unfortunately registering all of them has been shown to have a very significant impact
    // on executable sizes.
    // For this reason we also keep two separate lists: we might be able to need only one of them.
    // See https://github.com/quarkusio/quarkus/issues/12961

    public static String[] typesfromLocalCacheFactory() {
        return new String[] {
                // Starting with S means strong keys from com.github.benmanes.caffeine.cache.LocalCacheFactory.newBoundedLocalCache :
                // Second char 'I' means weak values: triggered by com.github.benmanes.caffeine.cache.Caffeine.weakValues
                // If 3rd char is 'L' listeners must have been registered: triggered by com.github.benmanes.caffeine.cache.Caffeine.removalListener
                "SILMS",
                "SSA",
                "SSLA",
                "SSLMS",
                "SSMS",
                "SSMSA",
                "SSMSW",
                "SSW",
        };
    }

    public static String[] typesfromNodeFactory() {
        return new String[] {
                // See LocalCacheFactory and NodeFactory for the naming pattern:
                //Starting with P means strong keys from com.github.benmanes.caffeine.cache.NodeFactory.newFactory :
                "PDMS", //PD = strong keys, values are neither strong nor weak
                "PSA", //PS = strong keys && strong values
                "PSMS", //PS = strong keys && strong values
                "PSW", //PS = strong keys && strong values
                "PSWMS", //PS = strong keys && strong values
                "PSWMW", //PS = strong keys && strong values
        };
    }

}
