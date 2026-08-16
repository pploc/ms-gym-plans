package com.gym.plans.unit.config;

import com.gym.common.grpc.interceptor.AuthServerInterceptor;
import com.gym.common.grpc.interceptor.ExceptionInterceptor;
import com.gym.common.grpc.interceptor.LoggingInterceptor;
import com.gym.common.grpc.interceptor.MetricsInterceptor;
import com.gym.common.grpc.interceptor.TracingInterceptor;
import com.gym.common.grpc.interceptor.ValidationInterceptor;
import com.gym.common.grpc.security.WorkloadIdentityVerifier;
import com.gym.plans.adapter.in.grpc.PlansGrpcHandler;
import com.gym.plans.config.GrpcConfig;
import com.gym.plans.config.KongIdentityServerInterceptor;
import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrpcConfigUnitTest {

    private static final String GET_ACTIVE_GYM = "plans.v1.PlansService/GetActiveGym";
    private static final String VALIDATE_CHECK_IN_GYM = "plans.v1.PlansService/ValidateCheckInGym";
    private static final String RESOLVE_PURCHASABLE_PLAN = "plans.v1.PlansService/ResolvePurchasablePlan";

    private final GrpcConfig config = new GrpcConfig(
            mock(PlansGrpcHandler.class),
            mock(AuthServerInterceptor.class),
            mock(KongIdentityServerInterceptor.class),
            mock(ExceptionInterceptor.class),
            mock(LoggingInterceptor.class),
            mock(TracingInterceptor.class),
            mock(MetricsInterceptor.class),
            mock(ValidationInterceptor.class));

    private final WorkloadIdentityVerifier verifier = GrpcConfig.workloadIdentityVerifier();

    @Test
    void givenIdentifierDnsSanOnGetActiveGym_whenVerifyWorkloadIdentity_thenAllows() throws Exception {
        // Given
        ServerCall<?, ?> call = callWithSans(GET_ACTIVE_GYM, List.of(List.of(2, "ms-gym-identifier")));

        // When / Then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenIdentifierSpiffeSanOnGetActiveGym_whenVerifyWorkloadIdentity_thenAllows() throws Exception {
        // Given
        ServerCall<?, ?> call = callWithSans(
                GET_ACTIVE_GYM, List.of(List.of(6, "spiffe://gym.cluster.local/ns/default/sa/ms-gym-identifier")));

        // When / Then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenCheckinDnsSanOnValidateCheckInGym_whenVerifyWorkloadIdentity_thenAllows() throws Exception {
        // given
        ServerCall<?, ?> call = callWithSans(VALIDATE_CHECK_IN_GYM, List.of(List.of(2, "ms-gym-checkin")));

        // when / then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenCheckinSpiffeSanOnValidateCheckInGym_whenVerifyWorkloadIdentity_thenAllows() throws Exception {
        // given
        ServerCall<?, ?> call = callWithSans(
                VALIDATE_CHECK_IN_GYM, List.of(List.of(6, "spiffe://gym.cluster.local/ns/gym-system/sa/ms-gym-checkin")));

        // when / then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenMemberDnsSanOnResolvePurchasablePlan_whenVerifyWorkloadIdentity_thenAllows() throws Exception {
        // Given
        ServerCall<?, ?> call = callWithSans(RESOLVE_PURCHASABLE_PLAN, List.of(List.of(2, "ms-gym-member")));

        // When / Then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenMemberSpiffeSanOnResolvePurchasablePlan_whenVerifyWorkloadIdentity_thenAllows() throws Exception {
        // Given
        ServerCall<?, ?> call = callWithSans(
                RESOLVE_PURCHASABLE_PLAN, List.of(List.of(6, "spiffe://gym.cluster.local/ns/gym-system/sa/ms-gym-member")));

        // When / Then
        assertTrue(verifier.isVerified(call));
    }

    @Test
    void givenSwappedOrKongSanOnWorkloadRpc_whenVerifyWorkloadIdentity_thenDenies() throws Exception {
        // Given
        ServerCall<?, ?> memberOnIdentifierMethod = callWithSans(GET_ACTIVE_GYM, List.of(List.of(2, "ms-gym-member")));
        ServerCall<?, ?> identifierOnMemberMethod =
                callWithSans(RESOLVE_PURCHASABLE_PLAN, List.of(List.of(2, "ms-gym-identifier")));
        ServerCall<?, ?> kongOnWorkloadMethod = callWithSans(GET_ACTIVE_GYM, List.of(List.of(2, "kong")));
        ServerCall<?, ?> identifierOnCheckinMethod =
                callWithSans(VALIDATE_CHECK_IN_GYM, List.of(List.of(2, "ms-gym-identifier")));
        ServerCall<?, ?> checkinOnIdentifierMethod =
                callWithSans(GET_ACTIVE_GYM, List.of(List.of(2, "ms-gym-checkin")));

        // When / Then
        assertFalse(verifier.isVerified(memberOnIdentifierMethod));
        assertFalse(verifier.isVerified(identifierOnMemberMethod));
        assertFalse(verifier.isVerified(kongOnWorkloadMethod));
        assertFalse(verifier.isVerified(identifierOnCheckinMethod));
        assertFalse(verifier.isVerified(checkinOnIdentifierMethod));
    }

    @Test
    void givenMissingTlsCertificateOrSan_whenVerifyWorkloadIdentity_thenDenies() throws Exception {
        // Given
        ServerCall<Object, Object> missingTls = callWithMethod(GET_ACTIVE_GYM);
        when(missingTls.getAttributes()).thenReturn(Attributes.EMPTY);
        ServerCall<?, ?> missingCertificate = callWithCertificates(GET_ACTIVE_GYM, new Certificate[0]);
        ServerCall<?, ?> missingSan = callWithSans(GET_ACTIVE_GYM, null);

        // When / Then
        assertFalse(verifier.isVerified(missingTls));
        assertFalse(verifier.isVerified(missingCertificate));
        assertFalse(verifier.isVerified(missingSan));
    }

    @Test
    void givenUnknownMethodOrUnsupportedSanType_whenVerifyWorkloadIdentity_thenDenies() throws Exception {
        // Given
        ServerCall<?, ?> unknownMethod = callWithSans("plans.v1.PlansService/Unknown", List.of(List.of(2, "ms-gym-identifier")));
        ServerCall<?, ?> unsupportedSanType = callWithSans(GET_ACTIVE_GYM, List.of(List.of(7, "ms-gym-identifier")));

        // When / Then
        assertFalse(verifier.isVerified(unknownMethod));
        assertFalse(verifier.isVerified(unsupportedSanType));
    }

    @Test
    void givenPlaintextWithoutExplicitTestOptIn_whenBuildGrpcServer_thenRejects() throws Exception {
        // Given
        setField("tlsEnabled", false);
        setField("allowPlaintext", false);

        // When
        IllegalStateException exception = assertThrows(IllegalStateException.class, this::serverBuilder);

        // Then
        assertTrue(exception.getMessage().contains("explicit test configuration"));
    }

    @Test
    void givenTlsWithoutCertificateMaterial_whenBuildGrpcServer_thenRejects() throws Exception {
        // Given
        setField("tlsEnabled", true);
        setField("certificateChain", "");
        setField("privateKey", "");
        setField("clientCa", "");

        // When
        IllegalStateException exception = assertThrows(IllegalStateException.class, this::serverBuilder);

        // Then
        assertTrue(exception.getMessage().contains("certificate-chain, private-key, and client-ca"));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = GrpcConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(config, value);
    }

    private void serverBuilder() throws Exception {
        Method method = GrpcConfig.class.getDeclaredMethod("serverBuilder");
        method.setAccessible(true);
        try {
            method.invoke(config);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw exception;
        }
    }

    private static ServerCall<?, ?> callWithSans(String fullMethod, Collection<List<?>> sans) throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectAlternativeNames()).thenReturn(sans);
        return callWithCertificates(fullMethod, new Certificate[] {certificate});
    }

    private static ServerCall<?, ?> callWithCertificates(String fullMethod, Certificate[] certificates) {
        ServerCall<Object, Object> call = callWithMethod(fullMethod);
        SSLSession session = mock(SSLSession.class);
        try {
            when(session.getPeerCertificates()).thenReturn(certificates);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
        when(call.getAttributes())
                .thenReturn(Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session).build());
        return call;
    }

    private static ServerCall<Object, Object> callWithMethod(String fullMethod) {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        doReturn(method(fullMethod)).when(call).getMethodDescriptor();
        return call;
    }

    private static MethodDescriptor<Object, Object> method(String fullMethod) {
        return MethodDescriptor.newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethod)
                .setRequestMarshaller(dummy())
                .setResponseMarshaller(dummy())
                .build();
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
