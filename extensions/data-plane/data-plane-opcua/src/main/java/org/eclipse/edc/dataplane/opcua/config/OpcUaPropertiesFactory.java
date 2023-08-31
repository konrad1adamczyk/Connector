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

package org.eclipse.edc.dataplane.opcua.config;

import org.eclipse.edc.spi.result.Result;

import java.util.Map;
import java.util.Properties;

public class OpcUaPropertiesFactory {

    private static final String OPCUA_ENDPOINT_URL = "opcua.endpoint.url";
    private static final String OPCUA_SECURITY_POLICY = "opcua.security.policy";
    private static final String OPCUA_IDENTITY_PROVIDER = "opcua.identity.provider";

    public Result<Properties> getClientProperties(Map<String, String> properties) {
        var props = new Properties();

        String endpointUrl = properties.get(OPCUA_ENDPOINT_URL);
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            return Result.failure(String.format("Missing `%s` config", OPCUA_ENDPOINT_URL));
        }
        props.put(OPCUA_ENDPOINT_URL, endpointUrl);

        String securityPolicy = properties.get(OPCUA_SECURITY_POLICY);
        if (securityPolicy == null || securityPolicy.isEmpty()) {
            return Result.failure(String.format("Missing `%s` config", OPCUA_SECURITY_POLICY));
        }
        props.put(OPCUA_SECURITY_POLICY, securityPolicy);

        String identityProvider = properties.get(OPCUA_IDENTITY_PROVIDER);
        if (identityProvider == null || identityProvider.isEmpty()) {
            return Result.failure(String.format("Missing `%s` config", OPCUA_IDENTITY_PROVIDER));
        }
        props.put(OPCUA_IDENTITY_PROVIDER, identityProvider);

        return Result.success(props);
    }
}
