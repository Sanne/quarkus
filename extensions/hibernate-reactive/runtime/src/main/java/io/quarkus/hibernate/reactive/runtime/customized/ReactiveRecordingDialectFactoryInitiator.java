package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class ReactiveRecordingDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {
    public static final ReactiveRecordingDialectFactoryInitiator INSTANCE = new ReactiveRecordingDialectFactoryInitiator();

    @Override
    public Class<DialectFactory> getServiceInitiated() {
        return DialectFactory.class;
    }

    @Override
    public DialectFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new ReactiveRecordingDialectFactory();
    }
}
