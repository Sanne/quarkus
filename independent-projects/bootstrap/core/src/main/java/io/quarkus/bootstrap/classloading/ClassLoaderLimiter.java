package io.quarkus.bootstrap.classloading;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassLoaderLimiter implements ClassLoaderEventListener {

    private final ConcurrentMap<String, PerClassLoaderStats> classLoaderStats = new ConcurrentHashMap();
    private static final AtomicInteger ZERO = new AtomicInteger();

    @Override
    public void openResourceStream(String resourceName, String classLoaderName) {
        classLoaderStats
                .computeIfAbsent(classLoaderName, this::newStats)
                .incrementOpenResourceCounter(resourceName);
    }

    @Override
    public void loadClass(String className, String classLoaderName) {
//        if (className.contains("MappingBinder")) {
//            throw new IllegalStateException("MappingBinder accessed");
//        }
    }

    public int resourceOpenCount(String classLoaderName, String resourceNameFull) {
        final PerClassLoaderStats perClassLoaderStats = classLoaderStats.get(classLoaderName);
        if (perClassLoaderStats == null) {
            throw new IllegalArgumentException(
                    "This classloader name has not been observed; valid names: " + classLoaderStats.keySet());
        }
        return perClassLoaderStats.getStatsForResource(resourceNameFull);
    }

    private PerClassLoaderStats newStats(String classloaderName) {
        return new PerClassLoaderStats(classloaderName);
    }

    private static class PerClassLoaderStats {
        private final ConcurrentMap<String, AtomicInteger> openedResourcesCounter = new ConcurrentHashMap();
        private final String classLoaderName; //Useful during debugging

        public PerClassLoaderStats(String classLoaderName) {
            this.classLoaderName = classLoaderName;
        }

        public int getStatsForResource(String resourceNameFull) {
            return openedResourcesCounter.getOrDefault(resourceNameFull, ZERO).get();
        }

        public void incrementOpenResourceCounter(String resourceName) {
            openedResourcesCounter
                    .computeIfAbsent(resourceName, (k) -> new AtomicInteger())
                    .incrementAndGet();
        }
    }
}
