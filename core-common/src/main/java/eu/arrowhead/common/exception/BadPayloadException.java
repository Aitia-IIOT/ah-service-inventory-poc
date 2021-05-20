/********************************************************************************
 * Copyright (c) 2019 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   AITIA - implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.common.exception;

/**
 * Thrown if a resource receives a HTTP payload which have missing mandatory fields.
 */
@SuppressWarnings("serial")
public class BadPayloadException extends ArrowheadException {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BadPayloadException(final String msg, final int errorCode, final String origin, final Throwable cause) {
	    super(msg, errorCode, origin, cause);
	    this.setExceptionType(ExceptionType.BAD_PAYLOAD);
	}
	
	//-------------------------------------------------------------------------------------------------
	public BadPayloadException(final String msg, final int errorCode, final String origin) {
	    super(msg, errorCode, origin);
	    this.setExceptionType(ExceptionType.BAD_PAYLOAD);
	}
	
	//-------------------------------------------------------------------------------------------------
	public BadPayloadException(final String msg, final int errorCode, final Throwable cause) {
	    super(msg, errorCode, cause);
	    this.setExceptionType(ExceptionType.BAD_PAYLOAD);
	}
	
	//-------------------------------------------------------------------------------------------------
	public BadPayloadException(final String msg, final int errorCode) {
	    super(msg, errorCode);
	    this.setExceptionType(ExceptionType.BAD_PAYLOAD);
	}
	
	//-------------------------------------------------------------------------------------------------
	public BadPayloadException(final String msg, final Throwable cause) {
	    super(msg, cause);
	    this.setExceptionType(ExceptionType.BAD_PAYLOAD);
	}
	
	//-------------------------------------------------------------------------------------------------
	public BadPayloadException(final String msg) {
	    super(msg);
	    this.setExceptionType(ExceptionType.BAD_PAYLOAD);
	}
}