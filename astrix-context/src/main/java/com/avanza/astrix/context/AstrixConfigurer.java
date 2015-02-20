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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixApiDescriptor;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanPostProcessor;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.inject.AstrixPlugins;
import com.avanza.astrix.beans.publish.AstrixApiDescriptors;
import com.avanza.astrix.beans.publish.AstrixApiProviderPlugin;
import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.publish.AstrixPublishedBeansAware;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.PropertiesConfigSource;
import com.avanza.astrix.provider.core.AstrixExcludedByProfile;
import com.avanza.astrix.provider.core.AstrixIncludedByProfile;

public class AstrixConfigurer {


	private static final String CLASSPATH_OVERRIDE_SETTINGS = "META-INF/astrix/settings.properties";

	private static final Logger log = LoggerFactory.getLogger(AstrixConfigurer.class);
	
	private AstrixApiDescriptors astrixApiDescriptors;
	private final Collection<AstrixFactoryBean<?>> standaloneFactories = new LinkedList<>();
	private final List<AstrixPlugins.Plugin<?>> plugins = new ArrayList<>();
	private final AstrixSettings settings = new AstrixSettings() {{
		set(SUBSYSTEM_NAME, "default");
	}};
	
	private DynamicConfig customConfig = null;
	private final DynamicConfig wellKnownConfigSources = DynamicConfig.create(settings, PropertiesConfigSource.optionalClasspathPropertiesFile(CLASSPATH_OVERRIDE_SETTINGS));
	private final Set<String> activeProfiles = new HashSet<>();
	private DynamicConfig config;
	
	public AstrixContext configure() {
		config = createDynamicConfig();
		AstrixPlugins astrixPlugins = getPlugins();
		AstrixInjector injector = new AstrixInjector(astrixPlugins);
		injector.bind(DynamicConfig.class, config);
		injector.bind(AstrixContext.class, AstrixContextImpl.class);
		injector.bind(AstrixFactoryBeanRegistry.class, SimpleAstrixFactoryBeanRegistry.class);
		injector.bind(AstrixApiDescriptors.class, new FilteredApiDescriptors(getApiDescriptors(astrixPlugins), activeProfiles));
		injector.registerBeanPostProcessor(new InternalBeanPostProcessor(astrixPlugins, injector.getBean(AstrixBeanFactory.class)));
		AstrixContextImpl context = injector.getBean(AstrixContextImpl.class);
		for (AstrixFactoryBean<?> beanFactory : standaloneFactories) {
			log.debug("Registering standalone factory: bean={}", beanFactory.getBeanKey());
			context.registerBeanFactory(beanFactory);
		}
		return context;
	}
	
	private AstrixPlugins getPlugins() {
		AstrixPlugins result = new AstrixPlugins();
		for (AstrixPlugins.Plugin<?> plugin : plugins) {
			result.registerPlugin(plugin);
		}
		configureVersioning(result);
		return result;
	}

	private DynamicConfig createDynamicConfig() {
		if (customConfig != null) {
			return DynamicConfig.merged(customConfig, wellKnownConfigSources);
		}
		String dynamicConfigFactoryClass = wellKnownConfigSources.getStringProperty(AstrixSettings.DYNAMIC_CONFIG_FACTORY, null).get();
		if (dynamicConfigFactoryClass != null) {
			AstrixDynamicConfigFactory dynamicConfigFactory = initFactory(dynamicConfigFactoryClass);
			DynamicConfig config = dynamicConfigFactory.create();
			return DynamicConfig.merged(config, wellKnownConfigSources);
		}
		return wellKnownConfigSources;
	}

	private AstrixDynamicConfigFactory initFactory(String dynamicConfigFactoryClass) {
		try {
			return (AstrixDynamicConfigFactory) Class.forName(dynamicConfigFactoryClass).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to init AstrixDynamicConfigFactoryClass: " + dynamicConfigFactoryClass, e);
		}
	}
	
	private static class FilteredApiDescriptors implements AstrixApiDescriptors {
		
		private AstrixApiDescriptors apiDescriptors;
		private Set<String> activeProfiles;
		
		public FilteredApiDescriptors(AstrixApiDescriptors apiDescriptors, Set<String> activeProfiles) {
			this.apiDescriptors = apiDescriptors;
			this.activeProfiles = activeProfiles;
		}

		@Override
		public Collection<AstrixApiDescriptor> getAll() {
			List<AstrixApiDescriptor> result = new LinkedList<>();
			for (AstrixApiDescriptor descriptor : apiDescriptors.getAll()) {
				if (isActive(descriptor)) {
					log.debug("Found provider: provider={}", descriptor.getName());
					result.add(descriptor);
				}
			}
			return result;
		}
		
		private boolean isActive(AstrixApiDescriptor descriptor) {
			if (descriptor.isAnnotationPresent(AstrixIncludedByProfile.class)) {
				AstrixIncludedByProfile activatedBy = descriptor.getAnnotation(AstrixIncludedByProfile.class);
				if (!this.activeProfiles.contains(activatedBy.value())) {
					return false;
				}
			}
			if (descriptor.isAnnotationPresent(AstrixExcludedByProfile.class)) {
				AstrixExcludedByProfile deactivatedBy = descriptor.getAnnotation(AstrixExcludedByProfile.class);
				if (this.activeProfiles.contains(deactivatedBy.value())) {
					return false;
				}
			}
			return true;
		}
	}

	private AstrixApiDescriptors getApiDescriptors(AstrixPlugins astrixPlugins) {
		if (this.astrixApiDescriptors != null) {
			return astrixApiDescriptors;
		}
		String basePackage = config.getStringProperty(AstrixSettings.API_DESCRIPTOR_SCANNER_BASE_PACKAGE, "").get();
		if (basePackage.trim().isEmpty()) {
			return new AstrixApiDescriptorScanner(getAllDescriptorAnnotationsTypes(astrixPlugins), "com.avanza.astrix"); // Always scan com.avanza.astrix package
		}
		return new AstrixApiDescriptorScanner(getAllDescriptorAnnotationsTypes(astrixPlugins), "com.avanza.astrix", basePackage.split(","));
	}
	
	private List<Class<? extends Annotation>> getAllDescriptorAnnotationsTypes(AstrixPlugins astrixPlugins) {
		List<Class<? extends Annotation>> result = new ArrayList<>();
		for (AstrixApiProviderPlugin plugin : astrixPlugins.getPlugins(AstrixApiProviderPlugin.class)) {
			result.add(plugin.getProviderAnnotationType());
		}
		return result;
	}

	public void setBasePackage(String basePackage) {
		 this.settings.set(AstrixSettings.API_DESCRIPTOR_SCANNER_BASE_PACKAGE, basePackage);
	}
	
	public void enableFaultTolerance(boolean enableFaultTolerance) {
		this.settings.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, enableFaultTolerance);
	}
	
	public void enableVersioning(boolean enableVersioning) {
		this.settings.set(AstrixSettings.ENABLE_VERSIONING, enableVersioning);
	}
	
	private void configureVersioning(AstrixPlugins plugins) {
		if (config.getBooleanProperty(AstrixSettings.ENABLE_VERSIONING, true).get()) {
			discoverOnePlugin(plugins, AstrixVersioningPlugin.class);
		} else {
			plugins.registerPlugin(AstrixVersioningPlugin.class, AstrixVersioningPlugin.Default.create());
		}
	}
	
	private static <T> void discoverOnePlugin(AstrixPlugins plugin, Class<T> type) {
		T provider = plugin.getPlugin(type);
		log.debug("Found plugin for {}, using {}", type.getName(), provider.getClass().getName());
	}

	// package private. Used for internal testing only
	void setAstrixApiDescriptors(AstrixApiDescriptors astrixApiDescriptors) {
		this.astrixApiDescriptors = astrixApiDescriptors;
	}
	
	// package private. Used for internal testing only
	<T> void registerPlugin(Class<T> c, T provider) {
		plugins.add(new AstrixPlugins.Plugin<>(c, Arrays.asList(provider)));
	}

	public void set(String settingName, long value) {
		this.settings.set(settingName, value);
	}
	
	public void set(String settingName, boolean value) {
		this.settings.set(settingName, value);
	}
	
	public void set(String settingName, String value) {
		this.settings.set(settingName, value);
	}
	
	public void setSettings(Map<String, String> settings) {
		this.settings.setAll(settings);
	}
	
	public void setConfig(DynamicConfig config) {
		this.customConfig = config;
	}
	
	public void setAstrixSettings(AstrixSettings settings) {
		this.settings.setAll(settings);
	}
	
	/**
	 * Optional property that identifies what subsystem the current context belongs to. Its only
	 * allowed to invoke non-versioned services within the same subsystem. Attempting
	 * to invoke a non-versioned service in another subsystem will throw an IllegalSubsystemException. <p>
	 * 
	 * @param string
	 */
	public void setSubsystem(String subsystem) {
		this.settings.set(AstrixSettings.SUBSYSTEM_NAME, subsystem);
	}

	public void addFactoryBean(AstrixFactoryBean<?> factoryBean) {
		this.standaloneFactories.add(factoryBean);
	}

	void removeSetting(String name) {
		this.settings.remove(name);
	}

	public void activateProfile(String profile) {
		this.activeProfiles.add(profile);
	}
	
	public static  class InternalBeanPostProcessor implements AstrixBeanPostProcessor {
		
		private final AstrixPlugins plugins;
		private final AstrixBeanFactory publishedApis;
		
		public InternalBeanPostProcessor(AstrixPlugins plugins, AstrixBeanFactory publishedApis) {
			this.plugins = plugins;
			this.publishedApis = publishedApis;
		}

		@Override
		public void postProcess(Object bean, AstrixBeans beans) {
			injectAwareDependencies(bean, beans);
		}
		
		private void injectAwareDependencies(Object object, AstrixBeans beans) {
			if (object instanceof AstrixPublishedBeansAware) {
				injectBeanDependencies((AstrixPublishedBeansAware)object);
			}
			if (object instanceof AstrixPluginsAware) {
				AstrixPluginsAware.class.cast(object).setPlugins(plugins);
			}
			if (object instanceof AstrixConfigAware) {
				AstrixConfigAware.class.cast(object).setConfig(beans.getBean(AstrixBeanKey.create(DynamicConfig.class)));
			}
		}
		
		private void injectBeanDependencies(AstrixPublishedBeansAware beanDependenciesAware) {
			beanDependenciesAware.setAstrixBeans(new AstrixPublishedBeans() {
				@Override
				public <T> T getBean(AstrixBeanKey<T> beanKey) {
					return publishedApis.getBean(beanKey);
				}
			});
		}
	}

	

}
