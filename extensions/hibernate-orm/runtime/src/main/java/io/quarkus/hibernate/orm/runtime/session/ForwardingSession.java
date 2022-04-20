package io.quarkus.hibernate.orm.runtime.session;

import org.hibernate.Session;

public class ForwardingSession implements Session {

    protected abstract Session delegate();

    //TBD 
}
