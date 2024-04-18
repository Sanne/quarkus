package io.quarkus.hibernate.orm.deployment.integration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.stream.Stream;

/**
 * This is meant to be passed exclusively to the ByteBuddy based entity Enhancer of Hibernate ORM.
 *
 * High couple warning: we rely on the knowledge that ByteBuddy will exclusively invoke method
 * {@link ClassLoader#getResourceAsStream(String)} on the passed classloader; to verify that
 * this assumption doesn't break in the future we throw exceptions on all other operations.
 *
 * Why do we do this?
 * We need the Enhancer to source such resources from the thread context classloader which is set during the
 * enhancement process, but we can't capture this yet as it hasn't been created at the point in which we
 * need to pass a ClassLoader to the Hibernate ORM context.
 * On the other hand, deferring the creation of the ORM enhancer implies needing to create a new enhancer
 * instance for each processed entity, which very significantly slows down the process.
 */
final class DeferredClassLoader extends ClassLoader {

    public static final ClassLoader INSTANCE = new DeferredClassLoader();

    protected DeferredClassLoader() {
        super();
    }

    @Override
    public String getName() {
        return "Defferred Enhancement Classloader";
    }

    @Override
    public Class<?> loadClass(String name) {
        dont();
        return null;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        dont();
        return null;
    }

    @Override
    protected Object getClassLoadingLock(String className) {
        dont();
        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        dont();
        return null;
    }

    @Override
    protected Class<?> findClass(String moduleName, String name) {
        dont();
        return null;
    }

    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        dont();
        return null;
    }

    @Override
    public URL getResource(String name) {
        dont();
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) {
        dont();
        return null;
    }

    @Override
    public Stream<URL> resources(String name) {
        dont();
        return null;
    }

    @Override
    protected URL findResource(String name) {
        dont();
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        dont();
        return null;
    }

    /**
     * The only implemented method: delegate to the currently set context classloader.
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    @Override
    protected Package definePackage(
            String name,
            String specTitle,
            String specVersion,
            String specVendor,
            String implTitle,
            String implVersion,
            String implVendor,
            URL sealBase) {
        dont();
        return null;
    }

    @Override
    protected Package getPackage(String name) {
        dont();
        return null;
    }

    @Override
    protected Package[] getPackages() {
        dont();
        return null;
    }

    @Override
    protected String findLibrary(String libname) {
        dont();
        return null;
    }

    private void dont() {
        throw new UnsupportedOperationException("This classloader only supports the #getResourceAsStream() operation");
    }

}
