package io.quarkus.hibernate.orm.runtime.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Replaces the ClassLoaderService in Hibernate ORM with one which works in native mode.
 * Also an opportunity to apply some classloading optimisations which are specific to
 * Hibernate ORM and only safe in the context of the Quarkus architecture.
 */
public class FlatClassLoaderService implements ClassLoaderService, SessionFactoryObserver {

    private static final CoreMessageLogger log = CoreLogging.messageLogger(FlatClassLoaderService.class);
    public static final ClassLoaderService INSTANCE = new FlatClassLoaderService();

    //Small optimisation of bootstrap times: Hibernate ORM is prone to attempt loading packages and classes
    //multiple times, and complex models might trigger pathological cycles.
    //This is particularly cumbersome for non-existing package definitions it might attempt to verify,
    //for example to scan for package-level annotations and to match entities with their generated,
    //but optional, metamodel classes.
    //  *A model of 1.000 entities might generate millions of CNF exceptions.*
    //N.B. it's static: this allows for better reuse, but do ensure it's cleared on e.g. LiveReload events;
    //we accomplish this by registering this service as a SessionFactoryObserver: see #sessionFactoryCreated.
    private static final ConcurrentHashMap<String, String> negativePackageCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> negativeClassCache = new ConcurrentHashMap<>();

    private FlatClassLoaderService() {
        // use #INSTANCE when you need one
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> classForName(String className) {
        if (negativeClassCache.containsKey(className)) {
            throw new ClassLoadingException("Unable to load class [" + className + "]");
        }
        try {
            return (Class<T>) Class.forName(className, false, getClassLoader());
        } catch (Exception | LinkageError e) {
            negativeClassCache.put(className, "");
            throw new ClassLoadingException("Unable to load class [" + className + "]", e);
        }
    }

    @Override
    public URL locateResource(String name) {
        URL resource = getClassLoader().getResource(name);
        if (resource == null) {
            log.debugf(
                    "Loading of resource '%s' failed. Maybe that's ok, maybe you forgot to include this resource in the binary image? -H:IncludeResources=",
                    name);
        } else {
            log.tracef("Successfully loaded resource '%s'", name);
        }
        return resource;
    }

    @Override
    public InputStream locateResourceStream(String name) {
        InputStream resourceAsStream = getClassLoader().getResourceAsStream(name);
        if (resourceAsStream == null) {
            log.debugf(
                    "Loading of resource '%s' failed. Maybe that's ok, maybe you forgot to include this resource in the binary image? -H:IncludeResources=",
                    name);
        } else {
            log.tracef("Successfully loaded resource '%s'", name);
        }
        return resourceAsStream;
    }

    @Override
    public List<URL> locateResources(String name) {
        log.debugf(
                "locateResources (plural form) was invoked for resource '%s'. Is there a real need for this plural form?",
                name);
        try {
            Enumeration<URL> resources = getClassLoader().getResources(name);
            List<URL> resource = new ArrayList<>();
            while (resources.hasMoreElements()) {
                resource.add(resources.nextElement());
            }
            return resource;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
        ServiceLoader<S> serviceLoader = ServiceLoader.load(serviceContract, getClassLoader());
        final LinkedHashSet<S> services = new LinkedHashSet<S>();
        for (S service : serviceLoader) {
            services.add(service);
        }
        return services;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> T generateProxy(InvocationHandler handler, Class... interfaces) {
        throw new AssertionFailure("Not implemented! generateProxy(InvocationHandler handler, Class... interfaces)");
    }

    @Override
    public Package packageForNameOrNull(String packageName) {
        if (negativePackageCache.get(packageName) != null) {
            return null;
        }
        try {
            Class<?> aClass = Class.forName(packageName + ".package-info", false, getClassLoader());
            return aClass.getPackage();
        } catch (ClassNotFoundException e) {
            log.packageNotFound(packageName);
            negativePackageCache.put(packageName, "");
            return null;
        } catch (LinkageError e) {
            log.warn("LinkageError while attempting to load Package named " + packageName, e);
            return null;
        }
    }

    @Override
    public <T> T workWithClassLoader(Work<T> work) {
        ClassLoader systemClassLoader = getClassLoader();
        return work.doWork(systemClassLoader);
    }

    @Override
    public void stop() {
        sessionFactoryCreated(null);
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            return FlatClassLoaderService.class.getClassLoader();
        }
        return cl;
    }

    @Override
    public void sessionFactoryCreated(SessionFactory factory) {
        //Wipe these caches after bootstrap: this is important to not break
        //live-reload (new classes might need to show up) and also to save memory.
        //We won't be needing these caches after having started, as classloading events become very rare.
        negativePackageCache.clear();
        negativeClassCache.clear();
    }

}
