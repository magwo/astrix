/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.registry;

import java.util.Objects;

import com.avanza.astrix.beans.service.ServiceProperties;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceRegistryExporterClient {
	
	/*
	 * DESIGN NOTE:
	 * 
	 * ServiceRegistryClient (used by consumers to lookup services) and ServiceRegistryExporterClient
	 * (used by servers to register provided services) are split into two interfaces. The main reason
	 * is to allow plugins in Astrix to override the AstrixSettings.APPLICATION_INSTANCE_ID, which can only
	 * be overridden until an instance of ServiceRegistryExporterClient is created for the first time.
	 */
	
	private final AstrixServiceRegistry serviceRegistry;
	private final String subsystem;
	private final String applicationInstanceId;
	private final String zone;
	
	public ServiceRegistryExporterClient(AstrixServiceRegistry serviceRegistry, String subsystem, String applicationInstanceId) {
		this(serviceRegistry, subsystem, applicationInstanceId, subsystem);
	}
	
	public ServiceRegistryExporterClient(AstrixServiceRegistry serviceRegistry, String subsystem, String applicationInstanceId, String zone) {
		this.zone = zone;
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
		this.subsystem = Objects.requireNonNull(subsystem);
		this.applicationInstanceId = Objects.requireNonNull(applicationInstanceId);
	}

	public <T> void register(Class<T> type, ServiceProperties properties, long lease) {
		properties.setProperty(ServiceProperties.SUBSYSTEM, this.subsystem);
		properties.setProperty(ServiceProperties.APPLICATION_INSTANCE_ID, this.applicationInstanceId);
		properties.setProperty(ServiceProperties.SERVICE_ZONE, zone);
		AstrixServiceRegistryEntry entry = new AstrixServiceRegistryEntry();
		entry.setServiceProperties(properties.getProperties());
		entry.setServiceBeanType(type.getName());
		this.serviceRegistry.register(entry, lease);
	}
	
}
