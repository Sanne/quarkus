package io.quarkus.hibernate.orm.runtime.service;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.GenerationType;

import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.Assigned;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.internal.AutoGenerationTypeStrategy;
import org.hibernate.id.factory.internal.IdentityGenerationTypeStrategy;
import org.hibernate.id.factory.internal.SequenceGenerationTypeStrategy;
import org.hibernate.id.factory.internal.TableGenerationTypeStrategy;
import org.hibernate.id.factory.internal.UUIDGenerationTypeStrategy;
import org.hibernate.id.factory.spi.GenerationTypeStrategy;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Quarkus custom implementation of Hibernate ORM's org.hibernate.id.factory.internal.StandardIdentifierGeneratorFactory
 * differences with the original:
 * 1# it does not attempt to use a BeanContainer to create instances; Hibernate ORM introduced this feature in HHH-14688
 * 2# No need to handle AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS as Quarkus users shouldn't disable it (on by default)
 */
final class QuarkusSimplifiedIdentifierGeneratorFactory
        implements IdentifierGeneratorFactory {

    private static final CoreMessageLogger LOG = CoreLogging.messageLogger(QuarkusSimplifiedIdentifierGeneratorFactory.class);

    private final ServiceRegistry serviceRegistry;
    private final ConcurrentHashMap<String, Class> generatorStrategyToClassNameMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<GenerationType, GenerationTypeStrategy> generatorTypeStrategyMap = new ConcurrentHashMap<>();

    private Dialect dialect;

    public QuarkusSimplifiedIdentifierGeneratorFactory(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        generatorTypeStrategyMap.put(GenerationType.AUTO, AutoGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.SEQUENCE, SequenceGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.TABLE, TableGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.IDENTITY, IdentityGenerationTypeStrategy.INSTANCE);
        try {
            //This needs a try block to be compatible with both JPA 3.0 and JPA 3.1
            //FIXME remove as we switch JPA baseline
            generatorTypeStrategyMap.put(GenerationType.valueOf("UUID"), UUIDGenerationTypeStrategy.INSTANCE);
        } catch (IllegalArgumentException ex) {
            // Ignore
        }

        register("uuid2", UUIDGenerator.class);
        // can be done with UuidGenerator + strategy
        register("guid", GUIDGenerator.class);
        register("uuid", UUIDHexGenerator.class); // "deprecated" for new use
        register("uuid.hex", UUIDHexGenerator.class); // uuid.hex is deprecated
        register("assigned", Assigned.class);
        register("identity", IdentityGenerator.class);
        register("select", SelectGenerator.class);
        register("sequence", SequenceStyleGenerator.class);
        register("increment", IncrementGenerator.class);
        register("foreign", ForeignGenerator.class);
        register("enhanced-sequence", SequenceStyleGenerator.class);
        register("enhanced-table", TableGenerator.class);
    }

    //Same-as-upstream TODO make private?
    public void register(String strategy, Class generatorClass) {
        LOG.debugf("Registering IdentifierGenerator strategy [%s] -> [%s]", strategy, generatorClass.getName());
        final Class previous = generatorStrategyToClassNameMap.put(strategy, generatorClass);
        if (previous != null) {
            LOG.debugf("    - overriding [%s]", previous.getName());
        }
    }

    @Override //Same-as-upstream
    public Dialect getDialect() {
        if (dialect == null) {
            dialect = serviceRegistry.getService(JdbcEnvironment.class).getDialect();
        }
        return dialect;
    }

    @Override //Same-as-upstream
    public IdentifierGenerator createIdentifierGenerator(
            GenerationType generationType,
            String generatedValueGeneratorName,
            String generatorName,
            JavaType<?> javaType,
            Properties config,
            GeneratorDefinitionResolver definitionResolver) {
        final GenerationTypeStrategy strategy = generatorTypeStrategyMap.get(generationType);
        if (strategy != null) {
            return strategy.createIdentifierGenerator(
                    generationType,
                    generatorName,
                    javaType,
                    config,
                    definitionResolver,
                    serviceRegistry);
        }
        throw new NotYetImplementedFor6Exception(getClass());
    }

    @Override //Same-as-upstream
    public Class getIdentifierGeneratorClass(String strategy) {
        if ("hilo".equals(strategy)) {
            throw new UnsupportedOperationException("Support for 'hilo' generator has been removed");
        }
        String resolvedStrategy = "native".equals(strategy) ? getDialect().getNativeIdentifierGeneratorStrategy() : strategy;

        Class generatorClass = generatorStrategyToClassNameMap.get(resolvedStrategy);
        try {
            if (generatorClass == null) {
                final ClassLoaderService cls = serviceRegistry.getService(ClassLoaderService.class);
                generatorClass = cls.classForName(resolvedStrategy);
            }
        } catch (ClassLoadingException e) {
            throw new MappingException("Could not interpret id generator strategy [" + strategy + "]");
        }
        return generatorClass;
    }

    @Override
    public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config) {
        try {
            final Class<? extends IdentifierGenerator> clazz = getIdentifierGeneratorClass(strategy);
            final IdentifierGenerator identifierGenerator = clazz.getDeclaredConstructor().newInstance();
            identifierGenerator.configure(type, config, serviceRegistry);
            return identifierGenerator;
        } catch (Exception e) {
            final String entityName = config.getProperty(IdentifierGenerator.ENTITY_NAME);
            throw new MappingException("Could not instantiate id generator [entity-name=" + entityName + "]", e);
        }
    }

}
