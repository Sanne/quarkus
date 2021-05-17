package io.quarkus.hibernate.orm.batch;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BatchOtherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq2")
    public Long id;

    public BatchOtherEntity() {
    }

    @Override
    public String toString() {
        return "OtherEntity#" + id;
    }
}
