package io.quarkus.hibernate.orm.runtime.recording;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;

public interface RecordingDialectFactory extends DialectFactory {

    Dialect getDialect();

}
