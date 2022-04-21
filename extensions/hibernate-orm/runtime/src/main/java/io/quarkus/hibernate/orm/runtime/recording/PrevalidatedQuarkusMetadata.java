package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.spi.AbstractDelegatingMetadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

/**
 * This is a Quarkus custom implementation of Metadata wrapping the original
 * implementation from Hibernate ORM.
 * The goal is to run the {@link MetadataImpl#validate()} method
 * earlier than when it is normally performed, for two main reasons: further reduce
 * the work that is still necessary when performing a runtime boot, and to be
 * able to still use reflection as it's neccessary e.g. to validate enum fields.
 *
 * We also make sure that methods {@link #getSessionFactoryBuilder()} and {@link #buildSessionFactory()}
 * are unavailable, as these would normally trigger an additional validation phase:
 * we can actually boot Quarkus in a simpler way.
 */
public final class PrevalidatedQuarkusMetadata extends AbstractDelegatingMetadata implements MetadataImplementor {

    private final MetadataImpl metadata;

	private PrevalidatedQuarkusMetadata(final MetadataImpl metadata) {
    	super(metadata);
    	this.metadata = metadata; //copy of super's delegate field, but we need the hold on to the narrower `MetadataImpl` type.
    }

    public static PrevalidatedQuarkusMetadata validateAndWrap(final MetadataImpl original) {
        original.validate();
        original.getBootstrapContext().getReflectionManager().reset();
        return new PrevalidatedQuarkusMetadata(original);
    }

    // New helpers on this Quarkus specific metadata; these are useful to boot and manage the recorded state:

    public SessionFactoryOptionsBuilder buildSessionFactoryOptionsBuilder() {
        SessionFactoryOptionsBuilder builder = new SessionFactoryOptionsBuilder(
                metadata.getMetadataBuildingOptions().getServiceRegistry(),
                metadata.getBootstrapContext());
        // This would normally be done by the constructor of SessionFactoryBuilderImpl,
        // but we don't use a builder to create the session factory, for some reason.
        Map<String, SqmFunctionDescriptor> sqlFunctions = metadata.getSqlFunctionMap();
        if (sqlFunctions != null) {
            for (Map.Entry<String, SqmFunctionDescriptor> entry : sqlFunctions.entrySet()) {
                builder.applySqlFunction(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    //Relevant overrides:

    @Override
    public SessionFactoryBuilder getSessionFactoryBuilder() {
        //Ensure we don't boot Hibernate using this, but rather use the #buildSessionFactoryOptionsBuilder above.
        throw new IllegalStateException("This method is not supposed to be used in Quarkus");
    }

    @Override
    public SessionFactory buildSessionFactory() {
        //Ensure we don't boot Hibernate using this, but rather use the #buildSessionFactoryOptionsBuilder above.
        throw new IllegalStateException("This method is not supposed to be used in Quarkus");
    }

    @Override
    public void validate() {
        //Intentional no-op
    }

    //All other methods from MetadataImplementor are delegating to the underlying instance.

}
