/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.dispatcher;

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspHttpRemoteMessageDispatcherImplTest {

    private final EdcHttpClient httpClient = mock(EdcHttpClient.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final DspHttpDispatcherDelegate<TestMessage, String> delegate = mock();
    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final Duration timeout = Duration.of(5, SECONDS);

    private DspHttpRemoteMessageDispatcher dispatcher;
    private TokenDecorator tokenDecoratorMock;

    @BeforeEach
    void setUp() {
        tokenDecoratorMock = mock(TokenDecorator.class);
        when(tokenDecoratorMock.decorate(any())).thenAnswer(a -> a.getArgument(0));
        dispatcher = new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService, tokenDecoratorMock, policyEngine);
        when(delegate.getMessageType()).thenReturn(TestMessage.class);
    }

    @Test
    void protocol_returnDsp() {
        assertThat(dispatcher.protocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    @Test
    void send_sendRequestViaHttpClient() {
        var responseBody = "response";
        Function<Response, String> responseFunction = response -> responseBody;
        var authToken = "token";

        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.parseResponse()).thenReturn(responseFunction);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(responseBody));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(authToken).build()));

        dispatcher.registerDelegate(delegate);

        var message = new TestMessage();
        var result = dispatcher.send(String.class, message);

        assertThat(result).succeedsWithin(timeout).isEqualTo(responseBody);

        verify(delegate).buildRequest(message);
        verify(identityService).obtainClientCredentials(argThat(tr -> tr.getAudience().equals(message.getCounterPartyAddress())));
        verify(httpClient).executeAsync(argThat(r -> authToken.equals(r.headers().get("Authorization"))), any(), eq(responseFunction));
    }

    @Test
    void send_ensureTokenDecoratorScope() {
        var responseBody = "response";
        Function<Response, String> responseFunction = response -> responseBody;
        var authToken = "token";

        when(tokenDecoratorMock.decorate(any())).thenAnswer(a -> a.getArgument(0, TokenParameters.Builder.class).scope("test-scope"));
        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.parseResponse()).thenReturn(responseFunction);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(responseBody));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(authToken).build()));

        dispatcher.registerDelegate(delegate);

        var message = new TestMessage();
        var result = dispatcher.send(String.class, message);

        assertThat(result).succeedsWithin(timeout).isEqualTo(responseBody);

        verify(delegate).buildRequest(message);
        verify(identityService).obtainClientCredentials(argThat(tr -> tr.getAudience().equals(message.getCounterPartyAddress())));
        verify(httpClient).executeAsync(argThat(r -> authToken.equals(r.headers().get("Authorization"))), any(), eq(responseFunction));
        verify(identityService).obtainClientCredentials(argThat(tp -> tp.getScope().equals("test-scope")));
    }

    @Test
    void send_noDelegateFound_throwException() {
        assertThat(dispatcher.send(String.class, new TestMessage())).failsWithin(timeout)
                .withThrowableThat().withCauseInstanceOf(EdcException.class).withMessageContaining("found");

        verifyNoInteractions(httpClient);
    }

    @Test
    void send_failedToObtainToken_throwException() {
        dispatcher.registerDelegate(delegate);
        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));

        assertThat(dispatcher.send(String.class, new TestMessage())).failsWithin(timeout)
                .withThrowableThat().withCauseInstanceOf(EdcException.class).withMessageContaining("credentials");

        verifyNoInteractions(httpClient);
    }

    @Test
    void send_shouldNotEvaluatePolicy_whenItIsNotRegistered() {
        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.parseResponse()).thenReturn(response -> null);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(null));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("any").build()));
        dispatcher.registerDelegate(delegate);

        var result = dispatcher.send(String.class, new TestMessage());

        assertThat(result).succeedsWithin(timeout);
        verifyNoInteractions(policyEngine);
    }

    @Test
    void send_shouldEvaluatePolicy() {
        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.parseResponse()).thenReturn(response -> null);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(null));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("any").build()));
        dispatcher.registerDelegate(delegate);
        var policy = Policy.Builder.newInstance().build();
        dispatcher.registerPolicyScope(TestMessage.class, "test.message", m -> policy);

        var result = dispatcher.send(String.class, new TestMessage());

        assertThat(result).succeedsWithin(timeout);
        verify(policyEngine).evaluate(any(), eq(policy), isNull(), argThat(map -> map.containsKey(TokenParameters.Builder.class)));
    }

    static class TestMessage implements RemoteMessage {
        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getCounterPartyAddress() {
            return "http://connector";
        }
    }
}
