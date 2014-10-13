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
package se.avanzabank.asterix.service.registry.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixExportedServiceInfo;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceBuilderHolder;
import se.avanzabank.asterix.context.AsterixServiceComponent;
import se.avanzabank.asterix.context.AsterixServiceComponents;
import se.avanzabank.asterix.context.AsterixServiceRegistryPlugin;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistry;

@MetaInfServices(AsterixServiceRegistryPlugin.class)
public class AsterixServiceRegistryPluginImpl implements AsterixServiceRegistryPlugin, AsterixPluginsAware {
	
	private AsterixPlugins plugins;
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<AsterixExportedServiceInfo> publishedServices) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceRegistryExporterWorker.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_asterixServiceBusExporterWorker", beanDefinition);
		
		Set<AsterixServiceComponent> usedServiceComponents = new HashSet<>();
		for (final AsterixExportedServiceInfo exportedService : publishedServices) {
			usedServiceComponents.add(getComponent(exportedService.getComponentName()));
			
			beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceBuilderHolder.class);
			beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues() {{
				addIndexedArgumentValue(0, new RuntimeBeanReference("_asterixServiceBuilderBean-" + exportedService.getComponentName()));
				addIndexedArgumentValue(1, exportedService.getComponentName());
				addIndexedArgumentValue(2, exportedService.getProvidedService());
			}});
			beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
			registry.registerBeanDefinition("_asterixServiceBuilderHolder-" + exportedService.getProvidingBeanName() + "-" + exportedService.getProvidedService().getName(), beanDefinition);
		}
		for (AsterixServiceComponent serviceComponent : usedServiceComponents) {
			beanDefinition = new AnnotatedGenericBeanDefinition(serviceComponent.getServiceBuilder());
			registry.registerBeanDefinition("_asterixServiceBuilderBean-" + serviceComponent.getName(), beanDefinition);
		}
	}
	
	private AsterixServiceComponent getComponent(String componentName) {
		return plugins.getPlugin(AsterixServiceComponents.class).getComponent(componentName);
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
	@Override
	public Collection<? extends Class<?>> getConsumedBeanTypes() {
		return Arrays.asList(AsterixServiceRegistry.class);
	}
	
}
