package io.quarkus.hibernate.orm.xml.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class XmlMappedEntity {

    @Id
    public long id;

    public String name;

    public XmlMappedEntity() {
    }

    public XmlMappedEntity(String name) {
        this.name = name;
    }

}
