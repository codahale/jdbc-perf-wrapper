package com.codahale.jdbc;

/**
 * A simple interface indicating an object has been instrumented by
 * {@link Instrumenter}.
 * 
 * @author coda
 *
 */
public interface Instrumented {
	public abstract Class<?> getOriginalClass();
}
