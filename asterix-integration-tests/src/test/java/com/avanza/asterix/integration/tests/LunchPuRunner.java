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
package com.avanza.asterix.integration.tests;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.RootLogger;

import com.avanza.asterix.context.AsterixSettings;
import com.avanza.asterix.gs.test.util.PartitionedPu;
import com.avanza.asterix.gs.test.util.PuConfigurers;
import com.avanza.asterix.provider.component.AsterixServiceComponentNames;

public class LunchPuRunner {
	
	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		System.setProperty("com.gs.jini_lus.groups", "lunch-pu");
		AsterixSettings settings = new AsterixSettings();
		settings.setServiceRegistryUri(AsterixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=service-registry");
		PartitionedPu partitionedPu = new PartitionedPu(PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
				.numberOfPrimaries(1)
				.contextProperty("configUrl", settings.getExternalConfigUrl())
				.numberOfBackups(0));
		partitionedPu.run();
	}
	
}