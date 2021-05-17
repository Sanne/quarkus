package io.quarkus.hibernate.orm.config;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ConfigMyEntity {
    @Id
    private long id;

    private String name;

    public ConfigMyEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
