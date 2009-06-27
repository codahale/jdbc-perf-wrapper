package com.codahale.jdbc.tests;

import static com.codahale.jdbc.tests.InstrumenterTest.instrumented;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.codahale.jdbc.Instrumented;
import com.codahale.jdbc.InstrumentingDriver;
import com.google.common.collect.Lists;

@RunWith(Enclosed.class)
public class InstrumentingDriverTest {
	public static class Registering_As_A_JDBC_Driver {
		@Test
		public void itRegistersWithTheDriverManager() throws Exception {
			Class.forName("com.codahale.jdbc.InstrumentingDriver");
			
			final Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
				final Driver driver = drivers.nextElement();
				if (driver instanceof InstrumentingDriver) {
					return;
				}
			}
			
			fail("should have registered InstrumentingDriver but didn't");
		}
		
		@Test
		public void itRegistersAsAServiceProvider() throws Exception {
			final FileInputStream services = new FileInputStream("src/main/resources/META-INF/services/java.sql.Driver");
			final Scanner scanner = new Scanner(services);
			final List<String> lines = new LinkedList<String>();
			while (scanner.hasNextLine()) {
				lines.add(scanner.nextLine());
			}
			scanner.close();
			services.close();

			assertThat(lines, hasItem(InstrumentingDriver.class.getCanonicalName()));
		}
	}
	
	public static class An_Instrumenting_Driver {
		private InstrumentingDriver driver;
		
		@Before
		public void setup() throws Exception {
			this.driver = new InstrumentingDriver();
		}
		
		@Test
		public void itAcceptsURLsWithPerfPrefix() throws Exception {
			assertThat(driver.acceptsURL("jdbc:perf-mysql://blah/blee?bloo"), is(true));
		}
		
		@Test
		public void itDoesNotAcceptsURLsWithoutPerfPrefix() throws Exception {
			assertThat(driver.acceptsURL("jdbc:mysql://blah/blee?bloo"), is(false));
		}
		
		@Test
		public void itIsNotJDBCCompliant() throws Exception {
			assertThat(driver.jdbcCompliant(), is(false));
		}
		
		@Test
		public void itIsVersion01() throws Exception {
			assertThat(driver.getMajorVersion(), is(0));
			assertThat(driver.getMinorVersion(), is(1));
		}
	}
	
	public static class Getting_Connection_Properties {
		private InstrumentingDriver driver;
		
		@Before
		public void setup() throws Exception {
			this.driver = new InstrumentingDriver();
			Class.forName("org.hsqldb.jdbcDriver");
		}
		
		@Test
		public void itPassesTheMethodToTheInstrumentedDriver() throws Exception {
			final Properties info = new Properties();
			info.setProperty("user", "sa");
			info.setProperty("password", "");
			
			final List<String> names = Lists.newLinkedList();
			final List<String> values = Lists.newLinkedList();
			
			for (DriverPropertyInfo property : driver.getPropertyInfo("jdbc:perf-hsqldb:mem:JDBCPerfWrapperTest", info)) {
				names.add(property.name);
				values.add(property.value);
			}
			
			assertThat(names, is((List<String>) Lists.newArrayList("user", "password", "get_column_name", "ifexists", "default_schema", "shutdown")));
			assertThat(values, is((List<String>) Lists.newArrayList("sa", "", "true", null, null, null)));
		}
	}
	
	public static class Getting_A_Connection {
		private InstrumentingDriver driver;
		
		@Before
		public void setup() throws Exception {
			this.driver = new InstrumentingDriver();
			Class.forName("org.hsqldb.jdbcDriver");
		}
		
		@Test
		public void itReturnsAnInstrumentedDriver() throws Exception {
			final Properties info = new Properties();
			info.setProperty("user", "sa");
			info.setProperty("password", "");
			
			final Connection connection = driver.connect("jdbc:perf-hsqldb:mem:JDBCPerfWrapperTest", info);
			assertThat(connection, is(instrumented()));
			assertTrue((((Instrumented) connection).getOriginalClass()).equals(org.hsqldb.jdbc.jdbcConnection.class));
		}
	}
}
