package io.quarkus.hibernate.orm.metadatabuildercontributor;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MetadataBuilderContributorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(AnotherEntity.class)
                    .addClass(CustomMetadataBuilderContributor.class)
                    .addAsResource("application-metadata-builder-contributor.properties", "application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        AnotherEntity entity = new AnotherEntity();
        entity.setName("some_name");
        entityManager.persist(entity);

        assertThat(entityManager.createQuery("select addHardcodedSuffix(e.name) from AnotherEntity e", String.class)
                .getSingleResult())
                        .isEqualTo("some_name_some_suffix");
    }

}
