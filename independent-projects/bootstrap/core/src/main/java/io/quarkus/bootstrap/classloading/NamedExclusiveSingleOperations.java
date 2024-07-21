package io.quarkus.bootstrap.classloading;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Named unique execution helper. This was created as an alternative strategy to the need for
 * concurrent classloaders to use the getClassLoadingLock(name) scheme of the standard classloaders,
 * which will end up storing all resource names in a hashmap indefinitely.
 * For applications which don't need much heap otherwise, this ends up consuming a significant portion
 * of memory, arguably wasted since after the load of a certain class such objects will not be needed
 * ever again.
 * What we actually need is a way to ensure that, for a given String value, a certain critical section
 * operation is not executed concurrently; this is a recurrent pattern which involves a precondition
 * such as, in the case of ClassLoaders, the class not having being loaded already.
 * We can exploit the particular case to store coordination objects in a concurrent map only for the lifespan
 * of execution of the critical section.
 * The JDK ClassLoader will guard against this case via synchronization, but we propose here a cooperative
 * coordination among threads.
 *
 * @param <T> the type of the returned object
 * @param <Z> the type of the additional paramter to the criticalSectionProducer
 */
final class NamedExclusiveSingleOperations<T, Z> {

    private final ConcurrentHashMap<String, CompletableFuture<T>> namedOperations = new ConcurrentHashMap();

    /**
     * @param name                    the exlusive name: two concurrent operations using the same String value will be enforced to cooperate;
     *                                threads invoking this process with different names should not.
     * @param preExecutionCheck       IFF this function returns a non-null value, such value will be returned and the critical section producer will not be invoked. N.B. this function might be invoked multiple times.
     * @param criticalSectionProducer This is the producer of the value, which shall be invoked if the previous function returned null and will be protected from concurrent invocations.
     * @param usefulParameter         An additional parameter to be passed to the criticalSectionProducer.
     * @return an object of type <T>
     */
    public T exclusiveExecution(final String name, final Function<String, T> preExecutionCheck, final BiFunction<String, Z, T> criticalSectionProducer, final Z usefulParameter) {
        final T preconditionCheckOutput = preExecutionCheck.apply(name);
        if (preconditionCheckOutput != null) {
            return preconditionCheckOutput;
        }
        final CompletableFuture<T> computedValueHolder = new CompletableFuture<>();
        final CompletableFuture<T> existingComputed = namedOperations.putIfAbsent(name, computedValueHolder);
        try {
            if (existingComputed == null) {
                //appears that the current thred is responsible to create this - unless it was already done in the meantime:
                final T preconditionValueCheckAgain = preExecutionCheck.apply(name);
                if (preconditionValueCheckAgain != null) {
                    return preconditionValueCheckAgain;
                }
                //Now we own the exclusive lock and we know it wasn't created in the meantime, proceed with creation:
                try {
                    final T produced = criticalSectionProducer.apply(name, usefulParameter);
                    computedValueHolder.complete(produced);
                    return produced;
                } catch (RuntimeException e) {
                    computedValueHolder.completeExceptionally(e);
                    throw e; //is there a better way to re-throw?
                }
            } else {
                //some other thread is computing it:
                try {
                    //5 seconds should be enough to load any class.. last famous words?
                    return existingComputed.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    //should we have a fallback strategy?
                    throw new RuntimeException(e);
                }
            }
        } finally {
            //This is the significant difference in comparison to java.lang.ClassLoader :
            //coordination objects are cleaned up as soon as they are no longer necessary.
            namedOperations.remove(name);
        }
    }

}
