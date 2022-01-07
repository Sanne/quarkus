package io.quarkus.mutiny.deployment.test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;

public class CloseableResourceProducer {
	
	@Produces
    @RequestScoped
    @DefaultBean
    public CloseableResource createResource() {
		CloseableResource instance = new CloseableResource();
		instance.openIt();
		return instance;
	}
	
	public void disposeMutinySession(@Disposes CloseableResource rez) {
		rez.closeIt();
	}

}
