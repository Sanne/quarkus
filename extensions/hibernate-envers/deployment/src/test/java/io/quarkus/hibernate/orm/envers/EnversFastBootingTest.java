package io.quarkus.hibernate.orm.envers;

import io.quarkus.bootstrap.classloading.ClassLoaderLimiter;
import io.quarkus.test.QuarkusUnitTest;
import org.hibernate.Session;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

import javax.inject.Inject;

/**
 * Let's run some checks to verify that the optimisations we have
 * to actually boot efficiently are going to survive other patches.
 */
public class EnversFastBootingTest {

    private static final ClassLoaderLimiter stats = new ClassLoaderLimiter();

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyAuditedEntity.class))
            .withConfigurationResource("application.properties")
            .addClassLoaderEventListener(stats)
            .setAfterAllCustomizer(new Runnable() {
                @Override
                public void run() {
                    Assert.assertTrue(1 == stats.resourceOpenCount("QuarkusUnitTest ClassLoader", "/hibernate.properties"));
                    Assert.assertTrue(0 == stats.resourceOpenCount("Augmentation Class Loader", "/hibernate.properties"));
                    Assert.assertTrue(
                            0 == stats.resourceOpenCount("QuarkusUnitTest ClassLoader", "org/hibernate/jpa/orm_2_1.xsd"));
                    Assert.assertTrue(
                            0 == stats.resourceOpenCount("Augmentation Class Loader", "org/hibernate/jpa/orm_2_1.xsd"));
                    Assert.assertTrue(0 == stats.resourceOpenCount("QuarkusUnitTest ClassLoader", "org/hibernate/boot/jaxb/internal/MappingBinder"));
                    Assert.assertTrue(
                            0 == stats.resourceOpenCount("Augmentation Class Loader", "org/hibernate/boot/jaxb/internal/MappingBinder"));
                    Assert.assertTrue(
                            0 == stats.resourceOpenCount("Augmentation Class Loader2", "org/hibernate/boot/jaxb/internal/MappingBinder"));

                }
            });

    @Inject
    Session session;

    @Test
    public void testInjection() {
        //Check that Hibernate actually started:
        Assert.assertNotNull(session);
    }

}
