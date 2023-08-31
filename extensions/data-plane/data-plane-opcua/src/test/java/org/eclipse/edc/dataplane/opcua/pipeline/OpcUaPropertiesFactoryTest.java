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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpcUaPropertiesFactoryTest {

    private final OpcUaPropertiesFactory factory = new OpcUaPropertiesFactory();

    @Test
    void verifyGetClientProperties() {
        var properties = Map.of(
                "opcua.endpoint.url", "opc.tcp://localhost:4840",
                "opcua.identity.provider", "value1",
                "opcua.security.policy", "value2"

        );

        var result = factory.getClientProperties(properties);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent())
                .hasSize(3)
                .containsEntry("opcua.endpoint.url", "opc.tcp://localhost:4840")
                .containsEntry("opcua.identity.provider", "value1")
                .containsEntry("opcua.security.policy", "value2");
    }

    @Test
    void verifyFromFailsIfMissingEndpointUrl() {
        var properties = Map.of(
                "opcua.identity.provider", "value1",
                "opcua.security.policy", "value2"
        );

        var result = factory.getClientProperties(properties);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("`opcua.endpoint.url`");
    }
}
