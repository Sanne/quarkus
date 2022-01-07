package io.quarkus.mutiny.deployment.test;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class CloseableResource {
	
	private boolean opened = false;
	private boolean closed = false;
	
	public synchronized void openIt() {
		if (! opened && ! closed) {
			opened = true;
		}
		else {
			throw new IllegalStateException(formatState());
		}
	}
	
	private String formatState() {
		return "opened: " + opened + " closed: " + closed;
	}

	public synchronized void closeIt() {
		if (opened && ! closed) {
			closed = true;
		}
		else {
			throw new IllegalStateException(formatState());
		}
	}
	
	public synchronized void useThisResource() {
		if (opened && ! closed) {
			//all good
		}
		else {
			throw new IllegalStateException(formatState());
		}
	}

}
