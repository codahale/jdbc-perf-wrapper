package com.codahale.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JDBC driver which instruments an underlying {@link Driver} to monitor
 * performance.
 * 
 * @author coda
 *
 */
public class InstrumentingDriver implements Driver {
	private static final Pattern URL_MATCHER = Pattern.compile("^jdbc:perf-([a-z0-9]+):", Pattern.CASE_INSENSITIVE);
	static {
		try {
			DriverManager.registerDriver(new InstrumentingDriver());
		} catch (SQLException e) {
			throw new RuntimeException("Can't register driver!", e);
		}
	}

	/**
	 * @throws SQLException
	 *             if something goes wrong
	 */
	public InstrumentingDriver() throws SQLException {
		// Required for Class.forName().newInstance()
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return URL_MATCHER.matcher(url).find();
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		final String originalUrl = getOriginalUrl(url);
		final Driver driver = DriverManager.getDriver(originalUrl);
		return Instrumenter.instrument(Connection.class, driver.connect(originalUrl, info));
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public int getMinorVersion() {
		return 1;
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		final String originalUrl = getOriginalUrl(url);
		final Driver driver = DriverManager.getDriver(originalUrl);
		return driver.getPropertyInfo(originalUrl, info);
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	private String getOriginalDriverName(String url) {
		try {
			final Matcher matcher = URL_MATCHER.matcher(url);
			matcher.find();
			return matcher.group(1);
		} catch (IllegalStateException e) {
			throw new IllegalArgumentException(url + " is not an instrumentable JDBC URL.");
		}
	}

	private String getOriginalUrl(String url) {
		return URL_MATCHER.matcher(url).replaceAll("jdbc:" + getOriginalDriverName(url) + "://");
	}
}
