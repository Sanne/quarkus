package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * As long as the Dialect is controlled by the blocking variant
 * of HibernateOrmProcessor, we need to potentially adjust them
 * within the reactive services.
 * Also beyond overriding, we need to ensure that the chosen
 * Dialect is actually one of the reactive dialects; otherwise
 * issues further down become tricky to identify.
 */
public final class ReactiveDialectOverridingJdbcEnvironmentInitiator implements StandardServiceInitiator<JdbcEnvironment> {

    public static final ReactiveDialectOverridingJdbcEnvironmentInitiator INSTANCE = new ReactiveDialectOverridingJdbcEnvironmentInitiator();

    @Override
    public JdbcEnvironment initiateService(
            Map<String, Object> configurationValues,
            ServiceRegistryImplementor registry) {
        //The Hibernate Reactive dialects currently don't have a notion of versions
        //TODO allow version configurations to tune the Dialect
        final DialectFactory dialectFactory = registry.getService(DialectFactory.class);

        return new JdbcEnvironmentImpl(registry, dialectFactory.buildDialect(configurationValues, null));
    }

    @Override
    public Class<JdbcEnvironment> getServiceInitiated() {
        return JdbcEnvironment.class;
    }

}
