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
package com.avanza.astrix.integration.tests.domain.api;

import java.io.Serializable;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

public class LunchRestaurant implements Serializable {

	private static final long serialVersionUID = 1L;
	private String name;
	private String foodType;

	public LunchRestaurant(String name, String foodType) {
		this.name = name;
		this.foodType = foodType;
	}
	
	public LunchRestaurant() {
	}

	@SpaceRouting
	@SpaceId(autoGenerate = false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getFoodType() {
		return foodType;
	}
	
	public void setFoodType(String foodType) {
		this.foodType = foodType;
	}

	public static LunchRestaurant template() {
		return new LunchRestaurant();
	}
	
}
