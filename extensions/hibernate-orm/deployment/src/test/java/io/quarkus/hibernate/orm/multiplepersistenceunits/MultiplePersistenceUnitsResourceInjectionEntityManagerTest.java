package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.MultiPuUser;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsResourceInjectionEntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MultiPuUser.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @PersistenceContext(unitName = "users")
    EntityManager usersEntityManager;

    @PersistenceContext(unitName = "inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void testUser() {
        MultiPuUser user = new MultiPuUser("gsmet");
        usersEntityManager.persist(user);

        MultiPuUser savedUser = usersEntityManager.find(MultiPuUser.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventoryEntityManager.persist(plane);

        Plane savedPlane = inventoryEntityManager.find(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventoryEntityManager() {
        MultiPuUser user = new MultiPuUser("gsmet");
        assertThatThrownBy(() -> inventoryEntityManager.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }

    @Test
    @Transactional
    public void testAccessBothPersistenceUnits() {
        testUser();
        testPlane();
    }

}
