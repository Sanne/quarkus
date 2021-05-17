package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.SinglePuDefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.MultiPuUser;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SinglePuDefaultEntity.class)
                    .addClass(MultiPuUser.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    Session defaultSession;

    @Inject
    @PersistenceUnit("users")
    Session usersSession;

    @Inject
    @PersistenceUnit("inventory")
    Session inventorySession;

    @Test
    @Transactional
    public void testDefault() {
        SinglePuDefaultEntity defaultEntity = new SinglePuDefaultEntity("default");
        defaultSession.persist(defaultEntity);

        SinglePuDefaultEntity savedDefaultEntity = defaultSession.get(SinglePuDefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Test
    @Transactional
    public void testUser() {
        MultiPuUser user = new MultiPuUser("gsmet");
        usersSession.persist(user);

        MultiPuUser savedUser = usersSession.get(MultiPuUser.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventorySession.persist(plane);

        Plane savedPlane = inventorySession.get(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventorySession() {
        MultiPuUser user = new MultiPuUser("gsmet");
        assertThatThrownBy(() -> inventorySession.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }
}
