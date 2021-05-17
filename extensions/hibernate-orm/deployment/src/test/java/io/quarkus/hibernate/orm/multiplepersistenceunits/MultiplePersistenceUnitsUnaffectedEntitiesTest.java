package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Formatter;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.SinglePuDefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.MultiPuUser;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsUnaffectedEntitiesTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SinglePuDefaultEntity.class)
                    .addClass(MultiPuUser.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-unaffected-entities.properties",
                            "application.properties"))
            // Expect a warning on startup
            .setLogRecordPredicate(
                    record -> record.getMessage().contains("Could not find a suitable persistence unit for model classes"))
            .assertLogRecords(records -> assertThat(records)
                    .hasSize(1)
                    .element(0).satisfies(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains(SinglePuDefaultEntity.class.getName())
                                .contains(MultiPuUser.class.getName());
                    }));

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventoryEntityManager.persist(plane);

        Plane savedPlane = inventoryEntityManager.find(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }
}
