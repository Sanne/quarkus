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

    private final ThreadLocal<Uni<Void>> currentSessionUni = new ThreadLocal<Uni<Void>>();

    @Inject
    Mutiny.SessionFactory mutinySessionFactory;

    @Produces
    @RequestScoped
    @DefaultBean
    public Uni<Mutiny.Session> createMutinySession() {
        Uni<Void> beingClosed = currentSessionUni.get();
        if (beingClosed == null) {
            return openSession();
        } else {
            //Always make sure previous sessions are closed first
            //to avoid starving the connection pool.
            return beingClosed.chain(this::openSession);
        }
    }

    private Uni<Mutiny.Session> openSession() {
        return Uni.createFrom().item(mutinySessionFactory::openSession);
    }

    public void disposeMutinySession(@Disposes Mutiny.Session reactiveSession) {
        if (reactiveSession != null) {
            final Uni<Void> closeOperation = reactiveSession.close();

            final Cancellable subscribe = closeOperation.subscribe().with(
                    item -> removeTracking(),
                    failure -> failedCloseSession(failure));
            currentSessionUni.set(closeOperation);

        }
    }

    private void failedCloseSession(Throwable failure) {
        currentSessionUni.remove();
    }

    private void removeTracking() {
        currentSessionUni.remove();
    }

}
