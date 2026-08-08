package com.gym.plans.unit.config;

import com.gym.common.grpc.security.WorkloadIdentityVerifier;
import com.gym.plans.config.GrpcConfig;
import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrpcConfigUnitTest {

    private final WorkloadIdentityVerifier verifier = GrpcConfig.workloadIdentityVerifier();

    @Test
    void givenIdentifierSanOnGetActiveGym_whenVerify_thenAllows() throws Exception {
        // Given
        ServerCall<?, ?> call = callWithSan("plans.v1.PlansService/GetActiveGym", "ms-gym-identifier");

        // When / Then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenMemberSanOnResolvePurchasablePlan_whenVerify_thenAllows() throws Exception {
        // Given
        ServerCall<?, ?> call =
                callWithSan("plans.v1.PlansService/ResolvePurchasablePlan", "ms-gym-member");

        // When / Then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenMemberSanOnGetActiveGym_whenVerify_thenDenies() throws Exception {
        // Given
        ServerCall<?, ?> call = callWithSan("plans.v1.PlansService/GetActiveGym", "ms-gym-member");

        // When / Then
        assertFalse(verifier.isVerified(call));
    }

    @Test
    void givenIdentifierSanOnResolvePurchasablePlan_whenVerify_thenDenies() throws Exception {
        // Given
        ServerCall<?, ?> call =
                callWithSan("plans.v1.PlansService/ResolvePurchasablePlan", "ms-gym-identifier");

        // When / Then
        assertFalse(verifier.isVerified(call));
    }

    @Test
    void givenMissingSslSession_whenVerify_thenDenies() {
        // Given
        ServerCall<Object, Object> call = mock(ServerCall.class);
        MethodDescriptor<Object, Object> method = MethodDescriptor.newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("plans.v1.PlansService/GetActiveGym")
                .setRequestMarshaller(dummy())
                .setResponseMarshaller(dummy())
                .build();
        doReturn(method).when(call).getMethodDescriptor();
        when(call.getAttributes()).thenReturn(Attributes.EMPTY);

        // When / Then
        assertFalse(verifier.isVerified(call));
    }

    private static ServerCall<?, ?> callWithSan(String fullMethod, String san) throws Exception {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        MethodDescriptor<Object, Object> method = MethodDescriptor.newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethod)
                .setRequestMarshaller(dummy())
                .setResponseMarshaller(dummy())
                .build();
        doReturn(method).when(call).getMethodDescriptor();

        SSLSession session = mock(SSLSession.class);
        X509Certificate cert = mock(X509Certificate.class);
        when(session.getPeerCertificates()).thenReturn(new java.security.cert.Certificate[] {cert});
        Collection<List<?>> sans = List.of(List.of(2, san));
        when(cert.getSubjectAlternativeNames()).thenReturn(sans);
        when(call.getAttributes())
                .thenReturn(Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session).build());
        return call;
    }

    private static MethodDescriptor.Marshaller<Object> dummy() {
        return new MethodDescriptor.Marshaller<>() {
            @Override
            public java.io.InputStream stream(Object value) {
                return java.io.InputStream.nullInputStream();
            }

            @Override
            public Object parse(java.io.InputStream stream) {
                return new Object();
            }
        };
    }
}
