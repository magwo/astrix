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
package tutorial.t2.provider;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import tutorial.t2.api.LunchRestaurantFinder;

public class LunchRestaurantFinderImpl implements LunchRestaurantFinder {

	private boolean initialized = false;
	private boolean destroyed = false;

	@Override
	public List<String> getAllRestaurants() {
		return AllLunchRestaurants.ALL_RESTAURANTS;
	}
	
	@PostConstruct
	public void init() {
		this.initialized = true;
	}
	
	@PreDestroy
	public void preDestroy() {
		this.destroyed  = true;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public boolean isDestroyed() {
		return this.destroyed;
	}

}
