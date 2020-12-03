package io.quarkus.hibernate.orm;

import javax.inject.Inject;

import org.hibernate.Session;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

import io.quarkus.bootstrap.classloading.ClassLoaderStats;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Let's run some checks to verify that the optimisations we have
 * to actually boot efficiently are going to survive other patches.
 */
public class JPAFastBootingTest {

    private static final ClassLoaderStats stats = new ClassLoaderStats();

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class))
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
