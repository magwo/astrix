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
package com.avanza.asterix.service.registry.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class AsterixServiceProxy<T> {
	
	private ServiceFactory<T> serviceFactory;
	private InvocationHandler state;
	
	public static <T> T create(Class<T> api) {
		return (T) Proxy.newProxyInstance(AsterixServiceProxy.class.getClassLoader(), new Class[]{api}, new StatefulInvocationHandler(new Disconnected<>()));
	}
	
	
	static class StatefulInvocationHandler implements InvocationHandler {

		private volatile InvocationHandler state;
		
		public StatefulInvocationHandler(InvocationHandler state) {
			this.state = state;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return this.state.invoke(proxy, method, args);
		}
		
	}
	
	static class Connected<T> implements InvocationHandler {
		private T instance;
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return method.invoke(instance, args);
		}
	}
	
	static class Disconnected<T> implements InvocationHandler {
		private T instance;
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new RuntimeException("Service unavailable"); // TODO: correct error message
		}
	}
	
	
	static class ServiceFactory<T> {
		
	}
	

}