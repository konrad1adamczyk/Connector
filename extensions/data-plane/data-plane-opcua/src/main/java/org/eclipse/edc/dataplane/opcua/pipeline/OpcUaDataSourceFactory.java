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
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.dataplane.opcua.config.OpcUaPropertiesFactory;
import org.eclipse.edc.dataplane.opcua.pipeline.validation.OpcUaSourceDataAddressValidationRule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

public class OpcUaDataSourceFactory implements DataSourceFactory {

    private final Monitor monitor;
    private final ValidationRule<DataAddress> validation;
    private final OpcUaPropertiesFactory propertiesFactory;

    public OpcUaDataSourceFactory(Monitor monitor, OpcUaPropertiesFactory propertiesFactory, Clock clock) {
        this.monitor = monitor;
        this.propertiesFactory = propertiesFactory;
        this.validation = new OpcUaSourceDataAddressValidationRule(propertiesFactory);
    }

    @Override
    public boolean canHandle(DataFlowRequest dataRequest) {
        return "OPC_UA".equalsIgnoreCase(dataRequest.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var source = request.getSourceDataAddress();
        return validation.apply(source).map(it -> true);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var validationResult = validate(request);
        if (validationResult.failed()) {
            throw new EdcException(validationResult.getFailureDetail());
        }

        var source = request.getSourceDataAddress();

        var clientProps = propertiesFactory.getClientProperties(source.getProperties())
                .orElseThrow(failure -> new IllegalArgumentException(failure.getFailureDetail()));


        return OpcUaDataSource.Builder.newInstance()
                .clientProperties(clientProps)
                //.clock(clock)
                //.nodeId(nodeId)
                .monitor(monitor)
                .build();
    }
}
