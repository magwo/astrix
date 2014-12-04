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
package com.avanza.astrix.core;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	private final String exceptionType;
	private final String serviceExceptionMessage;
	private final String correlationId;

	/**
	 * 
	 * @param msg - 
	 * @param serviceExceptionMessage - the message from the exception thrown 
	 * @param serviceExceptionType - the type of the thrown exception
	 * @param correlationId - a correlation id to identify the exception in server side logs.
	 */
	public ServiceException(String msg, String serviceExceptionMessage, String serviceExceptionType, String correlationId) {
		super(msg);
		this.serviceExceptionMessage = serviceExceptionMessage;
		this.exceptionType = serviceExceptionType;
		this.correlationId = correlationId;
	}
	
	public String getExceptionType() {
		return exceptionType;
	}
	
	public String getServiceExceptionMessage() {
		return serviceExceptionMessage;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}
}
