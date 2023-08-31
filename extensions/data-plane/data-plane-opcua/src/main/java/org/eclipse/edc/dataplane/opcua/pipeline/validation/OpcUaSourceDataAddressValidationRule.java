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

package org.eclipse.edc.dataplane.opcua.pipeline.validation;

import org.eclipse.edc.connector.dataplane.util.validation.CompositeValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.EmptyValueValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.dataplane.opcua.config.OpcUaPropertiesFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Map;

public class OpcUaSourceDataAddressValidationRule implements ValidationRule<DataAddress> {

    private final CompositeValidationRule<Map<String, String>> validationRule;

    public OpcUaSourceDataAddressValidationRule(OpcUaPropertiesFactory propertiesFactory) {
        this.validationRule = new CompositeValidationRule<>(
                List.of(
                        new EmptyValueValidationRule("NODE_ID"),
                        new EmptyValueValidationRule("ENDPOINT_URL"),
                        new ClientPropertiesValidationRule(propertiesFactory)
                )
        );
    }

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        return validationRule.apply(dataAddress.getProperties());
    }

    private static final class ClientPropertiesValidationRule implements ValidationRule<Map<String, String>> {

        private final OpcUaPropertiesFactory propertiesFactory;

        private ClientPropertiesValidationRule(OpcUaPropertiesFactory propertiesFactory) {
            this.propertiesFactory = propertiesFactory;
        }

        @Override
        public Result<Void> apply(Map<String, String> properties) {
            return propertiesFactory.getClientProperties(properties)
                    .compose(p -> Result.success());
        }
    }
}
