package com.codahale.jdbc.tests;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.codahale.jdbc.Stopwatch;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class StopwatchTest {
	public static long seconds(int seconds) {
		return seconds * 100;
	}
	
	public static Matcher<Long> roughly(final int seconds) {
		return new BaseMatcher<Long>() {
			@Override
			public boolean matches(Object o) {
				if (o instanceof Long) {
					return seconds == Math.round(((Long) o) / 100.0);
				}
				
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("~" + seconds + "s");
			}
		};
	}
	
	@Before
	@After
	public void resetCounter() throws Exception {
		Stopwatch.getInstance().reset();
	}
	
	@Test
	public void itRecordsWhetherOrNotTheStopwatchWasCalled() throws Exception {
		assertThat(Stopwatch.getInstance().wasCalled(), is(false));
		
		Stopwatch.getInstance().start();
		Stopwatch.getInstance().stop();
		
		assertThat(Stopwatch.getInstance().wasCalled(), is(true));
	}
	
	@Test
	public void itRecordsTheElapsedTimeForASingleEvent() throws Exception {
		Stopwatch.getInstance().start();
			Thread.sleep(seconds(2));
		Stopwatch.getInstance().stop();
		
		assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(2)));
		assertThat(Stopwatch.getInstance().wasCalled(), is(true));
	}
	
	@Test
	public void itRecordsTheElapsedTimeForMultipleEvents() throws Exception {
		Stopwatch.getInstance().start();
			Thread.sleep(seconds(2));
		Stopwatch.getInstance().stop();
		Thread.sleep(seconds(2));
		Stopwatch.getInstance().start();
			Thread.sleep(seconds(2));
		Stopwatch.getInstance().stop();
		
		assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(4)));
		assertThat(Stopwatch.getInstance().wasCalled(), is(true));
	}
	
	@Test
	public void itRecordsTheTotalElapsedTimeForNestedEvents() throws Exception {
		Stopwatch.getInstance().start();
			Thread.sleep(seconds(2));
			Stopwatch.getInstance().start();
				Thread.sleep(seconds(2));
			Stopwatch.getInstance().stop();
		Stopwatch.getInstance().stop();
		
		assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(4)));
		assertThat(Stopwatch.getInstance().wasCalled(), is(true));
	}
	
	@Test
	public void itRecordsDifferentTimesForEachThread() throws Exception {
		final ExecutorService executor = Executors.newFixedThreadPool(3);
		
		final Callable<Long> wayLong = new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				Stopwatch.getInstance().start();
					Thread.sleep(seconds(4));
				Stopwatch.getInstance().stop();
				return Stopwatch.getInstance().getElapsedTime();
			}
		};
		
		final Callable<Long> quick = new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				Stopwatch.getInstance().start();
					Thread.sleep(seconds(2));
				Stopwatch.getInstance().stop();
				return Stopwatch.getInstance().getElapsedTime();
			}
		};
		
		final Callable<Long> longer = new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				Stopwatch.getInstance().start();
					Thread.sleep(seconds(3));
				Stopwatch.getInstance().stop();
				return Stopwatch.getInstance().getElapsedTime();
			}
		};
		
		final List<Long> results = Lists.transform(
			// run all the tasks at the same time
			executor.invokeAll(ImmutableList.of(wayLong, quick, longer)),
			// and then collect the results
			new Function<Future<Long>, Long>() {
				@Override
				public Long apply(Future<Long> future) {
					try {
						return future.get();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		);
		
		assertThat(results.get(0), is(roughly(4)));
		assertThat(results.get(1), is(roughly(2)));
		assertThat(results.get(2), is(roughly(3)));
		assertThat(Stopwatch.getInstance().wasCalled(), is(false));
	}
}
