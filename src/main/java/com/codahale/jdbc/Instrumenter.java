package com.codahale.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * A dynamic wrapper which instruments {@link Connection}s, {@link Array}s,
 * {@link CallableStatement}s, {@link DatabaseMetaData}s,
 * {@link PreparedStatement}s, {@link ResultSet}s, and {@link Statement}s to
 * log the time spent performing database queries in {@link Stopwatch}.
 * 
 * @author coda
 *
 */
public class Instrumenter implements InvocationHandler {
	private static final Class<?>[] ARRAY_INTERFACES = new Class[] { Array.class, Instrumented.class };
	private static final Class<?>[] CALLABLE_STATEMENT_INTERFACES = new Class[] { CallableStatement.class, Instrumented.class };
	private static final Class<?>[] CONNECTION_INTERFACES = new Class[] { Connection.class, Instrumented.class };
	private static final Class<?>[] DATABASE_META_DATA_INTERFACES = new Class[] { DatabaseMetaData.class, Instrumented.class };
	private static final Class<?>[] PREPARED_STATEMENT_INTERFACES = new Class[] { PreparedStatement.class, Instrumented.class };
	private static final Class<?>[] RESULT_SET_INTERFACES = new Class[] { ResultSet.class, Instrumented.class };
	private static final Class<?>[] STATEMENT_INTERFACES = new Class[] { Statement.class, Instrumented.class };
	private static final Set<String> TIMED_METHODS = new HashSet<String>(5);
	{{
		TIMED_METHODS.add("execute");
		TIMED_METHODS.add("executeQuery");
		TIMED_METHODS.add("executeUpdate");
		TIMED_METHODS.add("executeBatch");
		TIMED_METHODS.add("next");
	}}

	@SuppressWarnings("unchecked")
	public static <T> T instrument(Class<? extends T> klass, Object object) {
		if (object == null) {
			return null;
		}
		
		final Instrumenter wrapper = new Instrumenter(object);
		final ClassLoader classLoader = object.getClass().getClassLoader();

		if (Array.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, ARRAY_INTERFACES, wrapper);
		} else if (CallableStatement.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, CALLABLE_STATEMENT_INTERFACES, wrapper);
		} else if (Connection.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, CONNECTION_INTERFACES, wrapper);
		} else if (DatabaseMetaData.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, DATABASE_META_DATA_INTERFACES, wrapper);
		} else if (PreparedStatement.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, PREPARED_STATEMENT_INTERFACES, wrapper);
		} else if (ResultSet.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, RESULT_SET_INTERFACES, wrapper);
		} else if (Statement.class.isAssignableFrom(klass)) {
			return (T) Proxy.newProxyInstance(classLoader, STATEMENT_INTERFACES, wrapper);
		} else {
			return (T) object;
		}
	}

	private final Object object;

	private Instrumenter(Object object) {
		this.object = object;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final boolean isTimed = TIMED_METHODS.contains(method.getName());

		if (isTimed) {
			Stopwatch.getInstance().start();
		}

		try {
			return instrument(method.getReturnType(), method.invoke(object, args));
		} finally {
			if (isTimed) {
				Stopwatch.getInstance().stop();
			}
		}
	}
}
