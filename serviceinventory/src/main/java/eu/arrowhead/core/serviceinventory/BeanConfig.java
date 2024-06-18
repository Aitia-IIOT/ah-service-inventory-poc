/********************************************************************************
 * Copyright (c) 2024 AITIA
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

package eu.arrowhead.core.serviceinventory;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.core.serviceinventory.data.HardcodedScriptConfiguration;
import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.IScriptConfiguration;
import eu.arrowhead.core.serviceinventory.data.InMemoryLabelingStorage;
import eu.arrowhead.core.serviceinventory.thread.LabelingWorker;

@Configuration
public class BeanConfig {
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	public ILabelingStorage createStorage() {
		return new InMemoryLabelingStorage();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Bean
	public IScriptConfiguration createConfiguration() {
		return new HardcodedScriptConfiguration();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Bean(name = CoreCommonConstants.SERVICE_INVENTORY_LABELING_JOB_QUEUE)
	public BlockingQueue<UUID> initLabelingJobQueue() {
		return new LinkedBlockingQueue<>();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LabelingWorker createLabelingWorker(final UUID uuid) {
		return new LabelingWorker(uuid);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Bean
	public Function<UUID,LabelingWorker> labelingWorkerFactory() {
		return uuid -> createLabelingWorker(uuid);
	}
}