package io.quarkus.hibernate.orm.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

import jakarta.persistence.spi.PersistenceProviderResolver;

@TargetClass(className = "jakarta.persistence.spi.PersistenceProviderResolverHolder")
public final class Substitute_PersistenceProviderResolverHolder {

    @Alias
    @RecomputeFieldValue(kind = Kind.Reset)
    private static PersistenceProviderResolver singleton;
}
