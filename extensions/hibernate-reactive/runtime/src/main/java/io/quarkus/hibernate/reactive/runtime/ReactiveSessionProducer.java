package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

@ApplicationScoped
public class ReactiveSessionProducer {

    private final ThreadLocal<Uni<Void>> currentSessionUni = new ThreadLocal<>();
    private final ThreadLocal<Mutiny.Session> currentlyOpenSession = new ThreadLocal<>();

    @Inject
    Mutiny.SessionFactory mutinySessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    public Uni<Mutiny.Session> createMutinySessionUni() {
        Uni<Void> beingClosed = currentSessionUni.get();
        if (beingClosed == null) {
            return sessionOpeningUni();
        } else {
            //Always make sure previous sessions are closed first
            //to avoid starving the connection pool.
            return beingClosed.chain(this::sessionOpeningUni);
        }
    }

    private Uni<Mutiny.Session> sessionOpeningUni() {
        return Uni.createFrom().item(this::actuallyOpenSession);
    }

    private Mutiny.Session actuallyOpenSession() {
        final Mutiny.Session session = mutinySessionFactory.openSession();
        currentlyOpenSession.set(session);
        return session;
    }

    public void disposeMutinySession(@Disposes Uni<Mutiny.Session> reactiveSessionUni) {
        if (reactiveSessionUni != null) {
            currentlyOpenSession.remove();//track current session as no longer open
            final Uni<Void> closeOperation = reactiveSessionUni.chain(s -> s.close());

            final Cancellable subscribe = closeOperation.subscribe().with(
                    item -> removeTracking(),
                    failure -> failedCloseSession(failure));
            currentSessionUni.set(closeOperation);

        }
    }

    private void failedCloseSession(Throwable failure) {
        //TODO log failure
        removeTracking();
    }

    private void removeTracking() {
        currentSessionUni.remove();
    }

    /**
     * @return the current {@link Mutiny.Session}, if and only if there is one
     *         open currently; otherwise null is returned.
     */
    public Mutiny.Session getCurrentSessionIfOpen() {
        return currentlyOpenSession.get();
    }

}
