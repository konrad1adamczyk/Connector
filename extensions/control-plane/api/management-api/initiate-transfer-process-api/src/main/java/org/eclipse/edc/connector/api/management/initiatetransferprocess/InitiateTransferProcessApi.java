/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.initiatetransferprocess;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiSchema;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition
@Tag(name = "Initiate Transfer Process")
public interface InitiateTransferProcessApi {

    @Operation(description = "Initiates a data transfer with the given parameters. Please note that successfully invoking this endpoint " +
            "only means that the transfer was initiated. Clients must poll the /{id}/state endpoint to track the state",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = TransferProcessSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transfer was successfully initiated. Returns the transfer process ID and created timestamp",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class)),
                            links = @Link(name = "poll-state", operationId = "getTransferProcessState", parameters = {
                                    @LinkParameter(name = "id", expression = "$response.body#/id")
                            })
                    ),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    JsonObject initiateTransferProcess(String id);

    @Schema(name = "TransferProcess", example = TransferProcessSchema.TRANSFER_PROCESS_EXAMPLE)
    record TransferProcessSchema(
            @Schema(name = TYPE, example = TRANSFER_PROCESS_TYPE)
            String ldType,
            @Schema(name = ID)
            String id,
            TransferProcess.Type type,
            String protocol,
            String counterPartyId,
            String counterPartyAddress,
            String state,
            String contractAgreementId,
            String errorDetail,
            @Deprecated(since = "0.2.0")
            @Schema(deprecated = true)
            Map<String, String> properties,
            Map<String, Object> privateProperties,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses
    ) {
        public static final String TRANSFER_PROCESS_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/TransferProcess",
                    "@id": "process-id",
                    "correlationId": "correlation-id",
                    "type": "PROVIDER",
                    "state": "STARTED",
                    "stateTimestamp": 1688465655,
                    "assetId": "asset-id",
                    "connectorId": "connectorId",
                    "contractId": "contractId",
                    "dataDestination": {
                        "type": "data-destination-type"
                    },
                    "privateProperties": {
                        "private-key": "private-value"
                    },
                    "errorDetail": "eventual-error-detail",
                    "createdAt": 1688465655,
                    "callbackAddresses": [{
                        "transactional": false,
                        "uri": "http://callback/url",
                        "events": ["contract.negotiation", "transfer.process"],
                        "authKey": "auth-key",
                        "authCodeId": "auth-code-id"
                    }]
                }
                """;
    }
}
