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
package se.avanzabank.asterix.context;

/**
 * An AsterixFactoryBean is a factory for creating an instance of a type that is part of a given api,
 * see {@link AsterixApiProvider}. <p>
 * 
 * Bean is a generic term used for an instance of any api element that asterix can create. 
 * It might be in the form of a service ('remoting service') or in the form of a library,
 * or any other type that is part of an api that asterix manages. <p>
 * 
 * Bean is the generic term used for an instance of a given type. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public final class AsterixFactoryBean<T> implements AsterixDecorator {
	
	private final AsterixFactoryBeanPlugin <T> plugin;
	private final Class<T> beanType;
	
	public AsterixFactoryBean(AsterixFactoryBeanPlugin<T> plugin) {
		this.plugin = plugin;
		this.beanType = plugin.getBeanType();
	}

	public T create(String optionalQualifier) {
		return this.plugin.create(optionalQualifier);
	}
	
	public Class<T> getBeanType() {
		return this.beanType;
	}

	@Override
	public Object getTarget() {
		return plugin;
	}
	
}
