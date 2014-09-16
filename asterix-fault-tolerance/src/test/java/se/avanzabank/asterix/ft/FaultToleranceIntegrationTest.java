/*
 * Copyright 2014-2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.avanzabank.asterix.ft;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.ft.service.SimpleService;
import se.avanzabank.asterix.ft.service.SimpleServiceException;
import se.avanzabank.asterix.ft.service.SimpleServiceImpl;

import com.google.common.base.Throwables;

public class FaultToleranceIntegrationTest {

	private static final long SLEEP_FOR_TIMEOUT = 1100l;
	private Class<SimpleService> api = SimpleService.class;
	private SimpleService provider = new SimpleServiceImpl();
	private SimpleService testService;

	@Before
	public void createService() {
		testService = HystrixAdapter.create(api , provider, randomString(), settingsRandomCommandKey());
	}
	
	@Test
	public void createWithDefaultSettings() throws Exception {
		SimpleService create = HystrixAdapter.create(api , provider, randomString());
		create.echo("");
	}
	
	@Test
	public void callFtService() {
		assertThat(testService.echo("foo"), is(equalTo("foo")));
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void timeoutThrowsServiceUnavailableException() {
		testService.sleep(SLEEP_FOR_TIMEOUT);
	}
	
	@Test(expected=SimpleServiceException.class)
	public void serviceExceptionIsThrown() throws Exception {
		testService.throwException(SimpleServiceException.class);
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void circuitBreakerOpens() throws Exception {
		// Hystrix needs a service to be invoked at least 20 times in a rolling window of one second for the circuit breaker to open
		for (int i = 0; i < 21; i++) {
			callServiceThrowServiceUnavailable(testService);
		}
		Thread.sleep(1200);
		testService.echo("foo");
	}
	
	@Test
	public void circuitBreakerDoesNotOpenOnServiceExceptions() throws Exception {
		// Hystrix needs a service to be invoked at least 20 times in a rolling window of one second for the circuit breaker to open
		for (int i = 0; i < 21; i++) {
			try {
				testService.throwException(SimpleServiceException.class);
			} catch (SimpleServiceException e) {
				// Ignore
			}
		}
		Thread.sleep(1200);
		try {
			testService.echo("foo");
		} catch (ServiceUnavailableException e) {
			fail("Should not throw ServiceUnavailableException - circuit breaker should be closed");
		}
	}
	
	private void callServiceThrowServiceUnavailable(SimpleService serviceWithFt) {
		try {
			serviceWithFt.throwException(ServiceUnavailableException.class);
		} catch (ServiceUnavailableException e) {
		}
	}
	
	@Test
	public void rejectsWhenPoolIsFull() throws Exception {
		HystrixCommandSettings settings = settingsRandomCommandKey();
		settings.setCoreSize(3);
		settings.setQueueSizeRejectionThreshold(3);
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString(), settings);
		// For some reason calls are not rejected unless we "warm up" with this call first. Why?
		serviceWithFt.echo("");
		ExecutorService pool = Executors.newCachedThreadPool();
		Collection<R> runners = new ArrayList<R>();
		for (int i = 0; i < 10; i++) {
			R runner = new R(serviceWithFt);
			runners.add(runner);
			pool.execute(runner);
		}
		pool.shutdown();
		pool.awaitTermination(2000, TimeUnit.MILLISECONDS);
		int numRejectionErrors = 0;
		for (R runner : runners) {
			Exception e = runner.getException();
			if (e == null) {
				continue;
			}
			numRejectionErrors++;
		}
		assertThat(numRejectionErrors, is(4));
	}
	
	private static class R implements Runnable {

		private SimpleService service;
		private volatile Exception exception;

		public R(SimpleService service) {
			this.service = service;
		}
		
		@Override
		public void run() {
			try {
				service.sleep(200);
			} catch (Exception e) {
				this.exception  = e;
			}
		}

		public Exception getException() {
			return exception;
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceException() throws Exception {
		try {
			testService.throwException(SimpleServiceException.class);
			fail("Expected SimpleServiceException");
		} catch (SimpleServiceException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(getClass().getName() + ".callerStackIsAddedToException"));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceUnavailableException() throws Exception {
		try {
			testService.throwException(ServiceUnavailableException.class);
			fail("Expected SimpleServiceException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(getClass().getName() + ".callerStackIsAddedToException"));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnTimeout() throws Exception {
		try {
			testService.sleep(SLEEP_FOR_TIMEOUT);
			fail("Expected ServiceUnavailableException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(getClass().getName() + ".callerStackIsAddedToException"));
		}
	}
	
	private HystrixCommandSettings settingsRandomCommandKey() {
		HystrixCommandSettings settings = new HystrixCommandSettings();
		settings.setCommandKey(randomString());
		return settings;
	}
	
	
	private String randomString() {
		return "" + Math.random();
	}
	
	
}
