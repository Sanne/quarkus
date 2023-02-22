package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.reactive.dialect.ReactivePostgreSQLDialect;

import io.quarkus.hibernate.orm.runtime.recording.RecordingDialectFactory;

public class ReactiveRecordingDialectFactory implements RecordingDialectFactory {

    //Keeping the logging category consistent with ORM core:
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger("SQL dialect");

    private Dialect dialect;

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public Dialect buildDialect(Map<String, Object> configValues, DialectResolutionInfoSource resolutionInfoSource)
            throws HibernateException {
        final Object dialectReference = configValues.get(AvailableSettings.DIALECT);
        dialect = resolveReactiveDialect(dialectReference);
        LOG.usingDialect(dialect);
        return dialect;
    }

    private static Dialect resolveReactiveDialect(Object configValue) {
        if (!(configValue instanceof String)) {
            throw new IllegalArgumentException(
                    "Hibernate Reactive currently expects the dialect to be configured exclusively via a String");
        } else {
            return resolveReactiveDialect((String) configValue);
        }
    }

    private static Dialect resolveReactiveDialect(String configValue) {
        //TODO : we should warn when overriding an explicit choice.
        //How to know this wasn't generated automatically?
        switch (configValue) {
            case "PostgreSQL":
            case "ReactivePostgreSQLDialect":
            case "org.hibernate.reactive.dialect.ReactivePostgreSQLDialect":
            case "org.hibernate.dialect.PostgreSQLDialect":
            case "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect":
                return new ReactivePostgreSQLDialect();
            default:
                return notImplementedYetException(configValue);
        }
    }

    private static Dialect notImplementedYetException(String configValue) {
        throw new IllegalArgumentException("Not supporting dialect '" + configValue + "' yet.");
    }

}
