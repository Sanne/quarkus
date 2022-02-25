package io.quarkus.hibernate.orm.runtime;

import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

import jakarta.persistence.spi.LoadState;

public final class ProviderUtil implements jakarta.persistence.spi.ProviderUtil {

    private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

    @Override
    public LoadState isLoadedWithoutReference(Object proxy, String property) {
        return PersistenceUtilHelper.isLoadedWithoutReference(proxy, property, cache);
    }

    @Override
    public LoadState isLoadedWithReference(Object proxy, String property) {
        return PersistenceUtilHelper.isLoadedWithReference(proxy, property, cache);
    }

    @Override
    public LoadState isLoaded(Object o) {
        return PersistenceUtilHelper.isLoaded(o);
    }
}
