/*
 *  Copyright (c) 2023 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.dataplane.opcua;


import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.dataplane.opcua.config.OpcUaPropertiesFactory;
import org.eclipse.edc.dataplane.opcua.pipeline.OpcUaDataSourceFactory;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

@Extension(value = DataPlaneOpcUaExtension.NAME)
public class DataPlaneOpcUaExtension implements ServiceExtension {

    public static final String NAME = "Data Plane OPC UA";

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var propertiesFactory = new OpcUaPropertiesFactory();

        pipelineService.registerFactory(new OpcUaDataSourceFactory(monitor, propertiesFactory, clock));
    }
}