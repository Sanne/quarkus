package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

public class ScanSubresource {
    @Path("doit")
    @GET
    @Produces("text/plain")
    public String get() {
        return "subresource-doit";
    }
}
