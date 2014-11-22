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
package tutorial.t2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tutorial.t2.api.LunchRestaurantFinder;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;

public class LibraryLifecycleManagementTest {
	
	private AstrixContext astrix;
	
//	@Test
	public void postConstructAnnotatedMethodsAreInvokedAfterTheBeanHasBeanCreated() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setBasePackage("tutorial.t2");
		astrix = configurer.configure();
		
		LunchRestaurantFinder restaurantFinder = astrix.getBean(LunchRestaurantFinder.class);
		assertTrue(restaurantFinder.isInitialized());
		assertFalse(restaurantFinder.isDestroyed());
		
		astrix.destroy();
		assertTrue(restaurantFinder.isDestroyed());
	}
	

}
