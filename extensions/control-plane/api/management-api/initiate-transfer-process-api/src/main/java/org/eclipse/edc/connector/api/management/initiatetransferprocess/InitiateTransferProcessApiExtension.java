/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.initiatetransferprocess;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.connector.api.management.initiatetransferprocess.transform.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.connector.api.management.initiatetransferprocess.transform.JsonObjectFromTransferStateTransformer;
import org.eclipse.edc.connector.api.management.initiatetransferprocess.transform.JsonObjectToTerminateTransferTransformer;
import org.eclipse.edc.connector.api.management.initiatetransferprocess.transform.JsonObjectToTransferRequestTransformer;
import org.eclipse.edc.connector.api.management.initiatetransferprocess.validation.TerminateTransferValidator;
import org.eclipse.edc.connector.api.management.initiatetransferprocess.validation.TransferRequestValidator;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.connector.api.management.initiatetransferprocess.model.TerminateTransfer.TERMINATE_TRANSFER_TYPE;
import static org.eclipse.edc.connector.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;

@Extension(value = InitiateTransferProcessApiExtension.NAME)
public class InitiateTransferProcessApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Initiate Transfer Process";
    private static final int DEFAULT_PUBLIC_PORT = 8185;
    private static final String PUBLIC_API_CONFIG = "web.http.public";

    private static final String PUBLIC_CONTEXT_ALIAS = "public";
    private static final String PUBLIC_CONTEXT_PATH = "/api/v1/public";

    @Setting
    private static final String CONTROL_PLANE_VALIDATION_ENDPOINT = "edc.dataplane.token.validation.endpoint";

    private static final WebServiceSettings PUBLIC_SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(PUBLIC_API_CONFIG)
            .contextAlias(PUBLIC_CONTEXT_ALIAS)
            .defaultPath(PUBLIC_CONTEXT_PATH)
            .defaultPort(DEFAULT_PUBLIC_PORT)
            .name(NAME)
            .build();

    @Inject
    private WebService webService;

    @Inject
    private WebServer webServer;

    @Inject
    private ManagementApiTypeTransformerRegistry transformerRegistry;

    @Inject
    private TransferProcessService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private WebServiceConfigurer webServiceConfigurer;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(emptyMap());
        transformerRegistry.register(new JsonObjectFromTransferProcessTransformer(builderFactory));
        transformerRegistry.register(new JsonObjectFromTransferStateTransformer(builderFactory));

        transformerRegistry.register(new JsonObjectToTerminateTransferTransformer());
        transformerRegistry.register(new JsonObjectToTransferRequestTransformer());

        validatorRegistry.register(TRANSFER_REQUEST_TYPE, TransferRequestValidator.instance());
        validatorRegistry.register(TERMINATE_TRANSFER_TYPE, TerminateTransferValidator.instance());

        var configuration = webServiceConfigurer.configure(context, webServer, PUBLIC_SETTINGS);

        var newController = new InitiateTransferProcessApiController(context.getMonitor(), service, transformerRegistry, validatorRegistry);
        webService.registerResource(configuration.getContextAlias(), newController);
    }
}
