/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.initiatetransferprocess;

import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/transferprocesses")
public class InitiateTransferProcessApiController implements InitiateTransferProcessApi {

    private final Monitor monitor;
    private final TransferProcessService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validatorRegistry;


    public InitiateTransferProcessApiController(Monitor monitor, TransferProcessService service,
                                                TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry
    ) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validatorRegistry = validatorRegistry;

    }

    @GET
    @Path("initiateTransferProcess/{id}")
    @Override
    public JsonObject initiateTransferProcess(@PathParam("id") String id) {


        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(TransferProcess.class, id);
        }

        var transferProcess = transformerRegistry.transform(definition, JsonObject.class)
                .onFailure(f -> monitor.warning(f.getFailureDetail()))
                .orElseThrow(failure -> new ObjectNotFoundException(TransferProcess.class, id));



        var transferRequest = transformerRegistry.transform(transferProcess, TransferRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var createdTransfer = service.initiateTransfer(transferRequest)
                .onSuccess(d -> monitor.debug(format("Transfer Process created %s", d.getId())))
                .orElseThrow(it -> mapToException(it, TransferProcess.class));

        var responseDto = IdResponse.Builder.newInstance()
                .id(createdTransfer.getId())
                .createdAt(createdTransfer.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));


    }

}
