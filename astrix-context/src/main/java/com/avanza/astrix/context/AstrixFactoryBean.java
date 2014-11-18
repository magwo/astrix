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
package com.avanza.astrix.context;

/**
 * An AstrixFactoryBean is a factory for creating an instance of a type that is part of a given api,
 * see {@link AstrixApiProvider}. <p>
 * 
 * Bean is a generic term used for an instance of any api element that Astrix can create. 
 * It might be in the form of a service ('remoting service') or in the form of a library,
 * or any other type that is part of an api that Astrix manages. <p>
 * 
 * Bean is the generic term used for an instance of a given type. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public final class AstrixFactoryBean<T> implements AstrixDecorator {
	
	private final AstrixFactoryBeanPlugin <T> plugin;
	private final Class<T> beanType;
	private final AstrixApiDescriptor apiDescriptor;
	private volatile AstrixBean<T> AstrixBean;
	private final boolean isLibrary;
	
	public AstrixFactoryBean(AstrixFactoryBeanPlugin<T> factoryPlugin, AstrixApiDescriptor apiDescriptor, boolean isLibrary) {
		this.plugin = factoryPlugin;
		this.apiDescriptor = apiDescriptor;
		this.isLibrary = isLibrary;
		this.beanType = factoryPlugin.getBeanType();
	}

	public T create(String optionalQualifier) {
		AstrixBean<T> bean = getAstrixBean(optionalQualifier);
		return bean.getInstance(); 
	}
	
	private AstrixBean<T> getAstrixBean(String optionalQualifier) {
		AstrixBean<T> bean = AstrixBean;
		if (bean != null) {
			return bean;
		}
		synchronized (this) {
			bean = AstrixBean;
			if (bean != null) {
				return bean;
			}
			bean = new AstrixBean<>(this.plugin.create(optionalQualifier));
			this.AstrixBean = bean;
			return bean;
		}
	}
	
	public Class<T> getBeanType() {
		return this.beanType;
	}

	@Override
	public Object getTarget() {
		return plugin;
	}

	public boolean isLibrary() {
		return isLibrary;
	}

	public boolean isVersioned() {
		return apiDescriptor.isVersioned();
	}

	public AstrixApiDescriptor getApiDescriptor() {
		return apiDescriptor;
	}
	
	private static class AstrixBean<T> {
		private final T instance;
		
		public AstrixBean(T instance) {
			this.instance = instance;
		}

		public T getInstance() {
			return instance;
		}
	}
	
}