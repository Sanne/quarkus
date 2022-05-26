package io.quarkus.hibernate.reactive.runtime.boot.registry;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.reactive.id.impl.ReactiveIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.service.QuarkusMutableIdentifierGeneratorFactory;

final class QuarkusReactiveIdentifierGeneratorFactoryInitiator
        implements StandardServiceInitiator<MutableIdentifierGeneratorFactory> {

    @Override
    public MutableIdentifierGeneratorFactory initiateService(Map map, ServiceRegistryImplementor serviceRegistryImplementor) {
        return new QuarkusMutableIdentifierGeneratorFactory(new ReactiveIdentifierGeneratorFactory());
    }

    @Override
    public Class<MutableIdentifierGeneratorFactory> getServiceInitiated() {
        return MutableIdentifierGeneratorFactory.class;
    }
}
