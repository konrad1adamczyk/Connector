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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.stack.client.UaStackClient;
import org.eclipse.milo.opcua.stack.client.transport.UaTransport;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.time.Clock;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

class OpcUaDataSource implements DataSource, Closeable {

    private String nodeId;
    private Monitor monitor;
    private Clock clock;
    private OpcUaClient opcUaClient;

    OpcUaDataSource() {
    }

    @Override
    public void close() {
        if (opcUaClient != null) {
            opcUaClient.disconnect();
        }
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        return success(openDataStream()
                .map(OpcUaPart::new));
    }

    @NotNull
    private Stream<byte[]> openDataStream() {
        // use your OPC UA client to fetch data from the server
        return null;
    }

    public static class Builder {

        private Properties clientProperties;
        private String nodeId;
        private final OpcUaDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            dataSource.nodeId = nodeId;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public Builder clock(Clock clock) {
            dataSource.clock = clock;
            return this;
        }

        public Builder clientProperties(Properties clientProperties) {
            this.clientProperties = clientProperties;
            return this;
        }

        public OpcUaDataSource build() {
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(clientProperties, "clientProperties");
            Objects.requireNonNull(dataSource.clock, "clock");

            OpcUaClientConfig config = OpcUaClientConfig.builder().setApplicationName(LocalizedText.english("My OPC UA Client")).build();
            UaStackClient stackClient = new UaStackClient(config, (Function<UaStackClient, UaTransport>) clientProperties);
            dataSource.opcUaClient = new OpcUaClient(config, stackClient);

            return dataSource;
        }

        private Builder() {
            dataSource = new OpcUaDataSource();
        }
    }

    private class OpcUaPart implements Part {

        private final byte[] data;

        private OpcUaPart(byte[] data) {
            this.data = data;
        }

        @Override
        public String name() {
            return nodeId;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(data);
        }
    }
}
