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
package com.avanza.astrix.serviceunit;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.service.AstrixServiceComponent;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.beans.service.UnsupportedTargetTypeException;

class ServiceRegistryExportedService {
	
	private AstrixBeanKey<?> exportedService;
	private Class<?> asyncService;
	private AstrixServiceComponent serviceComponent;
	
	public ServiceRegistryExportedService(AstrixServiceComponent serviceComponent, AstrixBeanKey<?> exportedServiceBeanKey) {
		if (!serviceComponent.canBindType(exportedServiceBeanKey.getBeanType())) {
			throw new UnsupportedTargetTypeException(serviceComponent.getName(), exportedServiceBeanKey.getBeanType());
		}
		this.serviceComponent = serviceComponent;
		this.exportedService = exportedServiceBeanKey;
		if (serviceComponent.supportsAsyncApis()) {
			this.asyncService = loadInterfaceIfExists(exportedServiceBeanKey.getBeanType().getName() + "Async");
		}
	}

	private Class<?> loadInterfaceIfExists(String interfaceName) {
		try {
			Class<?> c = Class.forName(interfaceName);
			if (c.isInterface()) {
				return c;
			}
		} catch (ClassNotFoundException e) {
			// fall through and return null
		}
		return null;
	}
	
	public boolean exportsAsyncApi() {
		return this.asyncService != null;
	}

	public AstrixServiceProperties exportServiceProperties() {
		AstrixServiceProperties serviceProperties = serviceComponent.createServiceProperties(exportedService.getBeanType());
		serviceProperties.setApi(exportedService.getBeanType());
		serviceProperties.setQualifier(exportedService.getQualifier());
		serviceProperties.setComponent(serviceComponent.getName());
		return serviceProperties;
	}
	
	public AstrixServiceProperties exportAsyncServiceProperties() {
		AstrixServiceProperties serviceProperties = exportServiceProperties();
		serviceProperties.setApi(asyncService);
		return serviceProperties;
	}

}