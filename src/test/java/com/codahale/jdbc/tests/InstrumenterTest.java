package com.codahale.jdbc.tests;

import static com.codahale.jdbc.tests.StopwatchTest.roughly;
import static com.codahale.jdbc.tests.StopwatchTest.seconds;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.codahale.jdbc.Stopwatch;
import com.codahale.jdbc.Instrumented;
import com.codahale.jdbc.Instrumenter;

@RunWith(Enclosed.class)
public class InstrumenterTest {
	private static Matcher<Object> instrumented() {
			return new BaseMatcher<Object>() {
				@Override
				public boolean matches(Object object) {
					for (Class<?> iface : object.getClass().getInterfaces()) {
						if (iface.equals(Instrumented.class)) {
							return true;
						}
					}
					return false;
				}
	
				@Override
				public void describeTo(Description description) {
					description.appendText("an instrumented object");
				}
			};
		}
	
	public static class An_Instrumented_Connection {
		private Statement statement;
		private PreparedStatement preparedStatement;
		private CallableStatement callableStatement;
		private DatabaseMetaData metaData;
		private Blob blob;
		private Connection connection, instrumentedConnection;
		
		@Before
		public void setup() throws Exception {
			this.statement = mock(Statement.class);
			this.preparedStatement = mock(PreparedStatement.class);
			this.callableStatement = mock(CallableStatement.class);
			this.metaData = mock(DatabaseMetaData.class);
			this.blob = mock(Blob.class);
			
			this.connection = mock(Connection.class);
			when(connection.createStatement()).thenReturn(statement);
			when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
			when(connection.prepareCall(anyString())).thenReturn(callableStatement);
			when(connection.getMetaData()).thenReturn(metaData);
			when(connection.createBlob()).thenReturn(blob);
			
			this.instrumentedConnection = Instrumenter.instrument(Connection.class, connection);
		}
		
		@Test
		public void itIsInstrumented() throws Exception {
			assertThat(instrumentedConnection, is(instrumented()));
		}
		
		@Test
		public void itReturnsInstrumentedStatements() throws Exception {
			assertThat(instrumentedConnection.createStatement(), is(instrumented()));
			
			verify(connection).createStatement();
		}
		
		@Test
		public void itReturnsInstrumentedPreparedStatements() throws Exception {
			assertThat(instrumentedConnection.prepareStatement("SELECT * FROM funk WHERE id = ?"), is(instrumented()));
			
			verify(connection).prepareStatement("SELECT * FROM funk WHERE id = ?");
		}
		
		@Test
		public void itReturnsInstrumentedCallableStatements() throws Exception {
			assertThat(instrumentedConnection.prepareCall("SELECT * FROM funk WHERE id = ?"), is(instrumented()));
			
			verify(connection).prepareCall("SELECT * FROM funk WHERE id = ?");
		}
		
		@Test
		public void itReturnsInstrumentedDatabaseMetaDatas() throws Exception {
			assertThat(instrumentedConnection.getMetaData(), is(instrumented()));
			
			verify(connection).getMetaData();
		}
		
		@Test
		public void itProxiesOtherMethodCallsThrough() throws Exception {
			assertThat(instrumentedConnection.createBlob(), is(not(instrumented())));
			
			verify(connection).createBlob();
		}
	}
	
	public static class An_Instrumented_Array {
		private ResultSet resultSet;
		private Array array, instrumentedArray;
		
		@Before
		public void setup() throws Exception {
			this.resultSet = mock(ResultSet.class);
			
			this.array = mock(Array.class);
			when(array.getResultSet(anyLong(), anyInt())).thenReturn(resultSet);
			
			this.instrumentedArray = Instrumenter.instrument(Array.class, array);
		}
		
		@Test
		public void itIsInstrumented() throws Exception {
			assertThat(instrumentedArray, is(instrumented()));
		}
		
		@Test
		public void itReturnsInstrumentedResultSets() throws Exception {
			assertThat(instrumentedArray.getResultSet(20L, 20), is(instrumented()));
			verify(array).getResultSet(20L, 20);
		}
	}
	
	public static class An_Instrumented_Statement {
		private ResultSet resultSet;
		private Statement statement, instrumentedStatement;
		
		@Before
		public void setup() throws Exception {
			Stopwatch.getInstance().reset();
			
			this.resultSet = mock(ResultSet.class);
			
			this.statement = mock(Statement.class);
			when(statement.execute(anyString())).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					Thread.sleep(seconds(2));
					return true;
				}
			});
			when(statement.executeQuery(anyString())).thenAnswer(new Answer<ResultSet>() {
				@Override
				public ResultSet answer(InvocationOnMock invocation) throws Throwable {
					Thread.sleep(seconds(2));
					return resultSet;
				}
			});
			when(statement.executeUpdate(anyString())).thenAnswer(new Answer<Integer>() {
				@Override
				public Integer answer(InvocationOnMock invocation) throws Throwable {
					Thread.sleep(seconds(2));
					return 2;
				}
			});
			when(statement.executeBatch()).thenAnswer(new Answer<Object>() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Thread.sleep(seconds(2));
					return null;
				}
			});
			
			this.instrumentedStatement = Instrumenter.instrument(Statement.class, statement);
		}
		
		@Test
		public void itIsInstrumented() throws Exception {
			assertThat(instrumentedStatement, is(instrumented()));
		}
		
		@Test
		public void itTimesQueryExecution() throws Exception {
			instrumentedStatement.executeQuery("blah");
			
			assertThat(Stopwatch.getInstance().wasCalled(), is(true));
			assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(2)));
			
			verify(statement).executeQuery("blah");
		}
		
		@Test
		public void itTimesStatementExecution() throws Exception {
			instrumentedStatement.execute("blah");
			
			assertThat(Stopwatch.getInstance().wasCalled(), is(true));
			assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(2)));
			
			verify(statement).execute("blah");
		}
		
		@Test
		public void itTimesUpdateExecution() throws Exception {
			instrumentedStatement.executeUpdate("blah");
			
			assertThat(Stopwatch.getInstance().wasCalled(), is(true));
			assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(2)));
			
			verify(statement).executeUpdate("blah");
		}
		
		@Test
		public void itTimesBatchExecution() throws Exception {
			instrumentedStatement.executeBatch();
			
			assertThat(Stopwatch.getInstance().wasCalled(), is(true));
			assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(2)));
			
			verify(statement).executeBatch();
		}
	}
	
	public static class An_Instrumented_ResultSet {
		private ResultSet resultSet, instrumentedResultSet;
		
		@Before
		public void setup() throws Exception {
			this.resultSet = mock(ResultSet.class);
			when(resultSet.next()).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					Thread.sleep(seconds(2));
					return true;
				}
			});
			
			this.instrumentedResultSet = Instrumenter.instrument(ResultSet.class, resultSet);
		}
		
		@Test
		public void itIsInstrumented() throws Exception {
			assertThat(instrumentedResultSet, is(instrumented()));
		}
		
		@Test
		public void itTimesNext() throws Exception {
			instrumentedResultSet.next();
			
			assertThat(Stopwatch.getInstance().wasCalled(), is(true));
			assertThat(Stopwatch.getInstance().getElapsedTime(), is(roughly(2)));
			
			verify(resultSet).next();
		}
	}
}
