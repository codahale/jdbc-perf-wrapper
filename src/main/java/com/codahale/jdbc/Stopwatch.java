package com.codahale.jdbc;

import net.jcip.annotations.Immutable;

/**
 * A per-thread timer. Calculates elapsed times between calls to
 * {@link #start()} and {@link #stop()} for a given thread.
 * <pre>
 * Stopwatch.getInstance().start();
 * doSomething();
 * Stopwatch.getInstance().stop();
 * 
 * Stopwatch.getInstance().getElapsedTime();
 * </pre>
 * @author coda
 */
@Immutable
public final class Stopwatch {
	private static class ThreadLocalCounter extends ThreadLocal<Long> {
		public void dec(long n) {
			inc(-n);
		}
		
		public void inc(long n) {
			set(get().longValue() + n);
		}
		
		public boolean isZero() {
			return get() == initialValue();
		}
		
		@Override
		protected Long initialValue() {
			return Long.valueOf(0);
		}
	}
	
	private static final Stopwatch INSTANCE = new Stopwatch();
	private final ThreadLocalCounter nanoseconds;
	private final ThreadLocalCounter nesting;
	private final ThreadLocal<Boolean> called;
	
	/**
	 * Returns the {@link Stopwatch} instance for the current thread.
	 */
	public static Stopwatch getInstance() {
		return INSTANCE;
	}

	private Stopwatch() {
		this.nanoseconds = new ThreadLocalCounter();
		this.nesting = new ThreadLocalCounter();
		this.called = new ThreadLocal<Boolean>() {
			@Override
			protected Boolean initialValue() {
				return Boolean.FALSE;
			}
		};
	}
	
	/**
	 * Resets the elapsed time.
	 */
	public void reset() {
		nanoseconds.remove();
		nesting.remove();
		called.remove();
	}
	
	/**
	 * Returns the elapsed time in milliseconds.
	 */
	public long getElapsedTime() {
		return Double.valueOf(nanoseconds.get().longValue() * 1E-6).longValue();
	}
	
	/**
	 * Starts timing.
	 */
	public void start() {
		if (nesting.isZero()) {
			called.set(Boolean.TRUE);
			nanoseconds.dec(System.nanoTime());
		}

		nesting.inc(1);
	}
	
	/**
	 * Stops timing.
	 */
	public void stop() {
		nesting.dec(1);

		if (nesting.isZero()) {
			nanoseconds.inc(System.nanoTime());
		}
	}
	
	/**
	 * Returns {@code true} if the Stopwatch has been called by the current
	 * thread, {@code false} otherwise.
	 */
	public boolean wasCalled() {
		return called.get();
	}
}
