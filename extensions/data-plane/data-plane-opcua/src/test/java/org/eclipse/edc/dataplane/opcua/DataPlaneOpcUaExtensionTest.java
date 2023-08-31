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

import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.dataplane.opcua.pipeline.OpcUaDataSourceFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneOpcUaExtensionTest {

    private PipelineService pipelineServiceMock;

    private DataPlaneOpcUaExtension extension;
    private ServiceExtensionContext context;

    @Test
    void verifyRegisterOpcUaSource() {
        extension.initialize(context);

        verify(pipelineServiceMock).registerFactory(any(OpcUaDataSourceFactory.class));
    }

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        pipelineServiceMock = mock(PipelineService.class);
        context.registerService(PipelineService.class, pipelineServiceMock);

        this.context = context;
        extension = factory.constructInstance(DataPlaneOpcUaExtension.class);
    }
}
