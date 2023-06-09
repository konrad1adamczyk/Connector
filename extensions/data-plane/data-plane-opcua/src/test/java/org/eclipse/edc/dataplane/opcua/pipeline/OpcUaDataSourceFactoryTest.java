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

package org.eclipse.edc.dataplane.opcua.pipeline;

import org.eclipse.edc.dataplane.opcua.config.OpcUaPropertiesFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpcUaDataSourceFactoryTest {

    private final OpcUaPropertiesFactory propertiesFactory = mock(OpcUaPropertiesFactory.class);

    private OpcUaDataSourceFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new OpcUaDataSourceFactory(mock(Monitor.class), propertiesFactory, mock(Clock.class));
    }

    @Test
    void verifyValidateSuccess() {
        Map<String, String> properties = Map.of("NODE_ID", "dummyNodeID", "ENDPOINT_URL", "dummyEndpointUrl");

        var request = createRequest("opcua", properties);

        when(propertiesFactory.getClientProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        var result = factory.validate(request);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void verifyValidateReturnsFailedResult_ifMissingNodeId() {
        Map<String, String> properties = Map.of("ENDPOINT_URL", "dummyEndpointUrl");

        var request = createRequest("opcua", properties);

        when(propertiesFactory.getClientProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        var result = factory.validate(request);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).contains("NODE_ID");
    }

    @Test
    void verifyValidateReturnsFailedResult_ifMissingEndpointUrlProperty() {
        Map<String, String> properties = Map.of("NODE_ID", "dummyNodeID"); // Providing dummy value
        var request = createRequest("opcua", properties);

        when(propertiesFactory.getClientProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        var result = factory.validate(request);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).contains("ENDPOINT_URL");
    }

    @Test
    void verifyValidateReturnsFailedResult_ifOpcUaPropertiesFactoryFails() {
        var errorMsg = "test-error";
        var request = createRequest("opcua", Map.of("opcua.endpoint.url", "opc.tcp://localhost:4840"));

        when(propertiesFactory.getClientProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.failure(errorMsg));

        var result = factory.validate(request);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).contains(errorMsg);
    }

    @Test
    void verifyCreateSourceThrows_ifMissingEndpointUrlProperty() {
        var request = createRequest("opcua", Map.of());

        when(propertiesFactory.getClientProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> factory.createSource(request));
    }

    @Test
    void verifyCreateSourceThrows_ifOpcUaPropertiesFactoryFails() {
        var errorMsg = "test-error";
        var request = createRequest("opcua", Map.of("opcua.endpoint.url", "opc.tcp://localhost:4840"));

        when(propertiesFactory.getClientProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.failure(errorMsg));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> factory.createSource(request))
                .withMessageContaining(errorMsg);
    }

    private static DataFlowRequest createRequest(String sourceType, Map<String, String> sourceProperties) {
        return DataFlowRequest.Builder.newInstance()
                .id("id")
                .processId("processId")
                .destinationDataAddress(DataAddress.Builder.newInstance().type("notused").build())
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(sourceType)
                        .properties(sourceProperties)
                        .build())
                .build();
    }
}
