package io.quarkus.bootstrap.classloading;

public interface ClassLoaderEventListener {

    default void enumeratingResourceURLs(String resourceName, String classLoaderName) {
    }

    default void gettingURLFromResource(String resourceName, String classLoaderName) {
    }

    default void openResourceStream(String resourceName, String classLoaderName) {
    }

    default void loadClass(String className, String classLoaderName) {
    }

}
