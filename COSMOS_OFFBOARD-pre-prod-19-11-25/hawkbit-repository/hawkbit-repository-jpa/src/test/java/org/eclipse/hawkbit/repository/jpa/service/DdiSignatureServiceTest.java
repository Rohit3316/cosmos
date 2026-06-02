package org.eclipse.hawkbit.repository.jpa.service;

import com.nimbusds.jose.util.Base64URL;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cosmos.models.ddi.DdiSignatureResult;
import org.cosmos.models.ddi.DdiSignatureType;
import org.eclipse.hawkbit.repository.exception.CosmosSignatureException;
import org.eclipse.hawkbit.repository.exception.EcuCertificateException;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Description;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import jakarta.validation.ValidationException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;

/**
 * Unit tests for the {@link DdiSignatureService} class.
 * <p>
 * Tests signature generation, certificate retrieval, SSM parameter handling,
 * and utility methods for certificate and key processing.
 */
@ExtendWith(MockitoExtension.class)
class DdiSignatureServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private SsmClient ssmClient;

    @Mock
    private SigningCertificateConfiguration signingCertificateConfiguration;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private DdiSignatureService signatureService;

    @Mock
    private KmsClient kmsClient;

    private static final String TEST_DD_PRIVATE_KEY_01 = "-----BEGIN EC PARAMETERS-----\nBggqhkjOPQMBBw==\n-----END EC PARAMETERS-----\n-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIKFxdPyn6QZ1DFeIJe8q1LKNPXG0trhuNBb22hIamKYWoAoGCCqGSM49AwEHoUQDQgAE0F2C8bWyvM6f4L+toDvMwJis1Ck3BK61qJd17e4QfgvsDrubhTIcffzv/CiMDoYr5MDaqb30nhngvxOOCCJpyw==\n-----END EC PRIVATE KEY-----";
    private static final String TEST_DD_SERVER_CERTIFICATE_01 = "-----BEGIN CERTIFICATE-----\nMIICOjCCAeCgAwIBAgIJAMNGCrznxnr+MAoGCCqGSM49BAMCMHYxCzAJBgNVBAYTAklUMQ4wDAYDVQQIDAVJdGFseTETMBEGA1UECgwKU3RlbGxhbnRpczEWMBQGA1UECwwNU1RMQSBPVEEgVGVhbTEqMCgGA1UEAwwhc3RsYV9vdGF0ZWFtX3Rlc3RfcGtpX2RzX3N1YmNhX2cxMB4XDTI0MDYwNzE0MDIzMFoXDTI2MDYwNzE0MDIzMFowZzELMAkGA1UEBhMCSVQxDjAMBgNVBAgMBUl0YWx5MRMwEQYDVQQKDApTdGVsbGFudGlzMRYwFAYDVQQLDA1TVExBIE9UQSBUZWFtMRswGQYDVQQDDBJjb3Ntb3MuZGV2LnNpZ24uZGQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATQXYLxtbK8zp/gv62gO8zAmKzUKTcErrWol3Xt7hB+C+wOu5uFMhx9/O/8KIwOhivkwNqpvfSeGeC/E44IImnLo2YwZDAdBgNVHQ4EFgQUJWpm4EdN9tQT4OfleX24XWOZkkMwHwYDVR0jBBgwFoAUPzCY+y38Wo+Oi7zkz126H0GZj8cwEgYDVR0TAQH/BAgwBgEB/wIBAzAOBgNVHQ8BAf8EBAMCAYYwCgYIKoZIzj0EAwIDSAAwRQIgbkZT35kQbdfCXICv0RdyTXa9aelTQZE35Svt2Kob48sCIQDf6QuuStYQlh0wu4tY5ihWFfVs78cp3o+ck4ylvWubpw==\n-----END CERTIFICATE-----";

    @BeforeEach
    void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @Description("Test for successful signature generation with valid parameters.")
    void givenValidCertificatesAndKeysWhenSignatureGeneratedThenSuccess() {
        String encodedObject = "dummyDescriptorObject";

        when(signingCertificateConfiguration.getDdPrivateKeyPath()).thenReturn("/test/dd-key");
        when(signingCertificateConfiguration.getDdCertificatePath()).thenReturn("test-bucket/test_cert.pem");
        when(signingCertificateConfiguration.getIntermediateCACertificatePath()).thenReturn("test-bucket/intermediate_ca.pem");
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("stla_otateam_test_pki_ds_subca_g1");
        JpaRollout rollout = mock(JpaRollout.class);

        when(ssmClient.getParameter(any(GetParameterRequest.class)))
                .thenReturn(GetParameterResponse.builder().parameter(p -> p.value(TEST_DD_PRIVATE_KEY_01)).build());

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> {
                    InputStream certStream = new ByteArrayInputStream(TEST_DD_SERVER_CERTIFICATE_01.getBytes(StandardCharsets.UTF_8));
                    return new ResponseInputStream<>(
                            GetObjectResponse.builder().build(),
                            new BufferedInputStream(certStream)
                    );
                });

        String signature = signatureService.generateSignature(encodedObject, DdiSignatureType.DD, signingCertificateConfiguration, rollout).getSignature();
        assertNotNull(signature);
        assertEquals(3, signature.split("\\.").length);

        // Decode JWT payload and verify claims
        String payload = new String(java.util.Base64.getUrlDecoder().decode(signature.split("\\.")[1]), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"sha256\":\"" + encodedObject + "\""));
        assertTrue(payload.contains("\"certificate expiry\""));
        assertTrue(payload.contains("\"exp\""));
        assertTrue(payload.contains("\"nbf\""));
    }

    @Test
    @Description("Test certificate retrieval from S3 with a valid certificate.")
    void givenValidS3PathWhenGetCertificateFromS3ThenSuccess() throws Exception {
        InputStream certStream = new ByteArrayInputStream(TEST_DD_SERVER_CERTIFICATE_01.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> certResponse = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new FilterInputStream(certStream) {
                }
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(certResponse);

        X509Certificate cert = signatureService.getCertificateFromS3("test-bucket", "test-key");
        assertNotNull(cert);
        assertEquals("X.509", cert.getType());
    }

    @Test
    @Description("Test private key retrieval from SSM with an invalid key format.")
    void givenInvalidPrivateKeyPathWhenGetParameterFromSSMThenException() {
        JpaRollout rollout = mock(JpaRollout.class);

        assertThrows(CosmosSignatureException.class, () ->
                signatureService.generateSignature("test", DdiSignatureType.DD, signingCertificateConfiguration, rollout));
    }

    @Test
    @Description("Test CN extraction from a valid DN string.")
    void givenValidDnWhenExtractCNFromDNThenValidCN() {
        String dn = "CN=TestIssuer, OU=Dev, O=Company, C=IN";
        String cn = DdiSignatureService.extractCNFromDN(dn);
        assertEquals("TestIssuer", cn);
    }

    @Test
    @Description("Test CN extraction from a DN string without CN.")
    void givenWithCNWhenExtractCNFromDNThenNoCN() {
        String dn = "O=Company, C=IN";
        String cn = DdiSignatureService.extractCNFromDN(dn);
        assertNull(cn);
    }

    @Test
    @Description("Test CN extraction from an empty DN string.")
    void givenEmptyDNExtractCNFromDNThenEmptyCN() {
        String dn = "";
        String cn = DdiSignatureService.extractCNFromDN(dn);
        assertNull(cn);
    }

    @Test
    @Description("Test CN extraction from a DN string with an invalid format.")
    void givenInvalidDNWhenExtractCNFromDNThenThrowError() {
        String dn = "not-a-valid-dn";
        assertThrows(ValidationException.class, () -> DdiSignatureService.extractCNFromDN(dn));
    }

    @Test
    @Description("Test private keypair retrieval from PEM with a valid key.")
    void givenValidKeyWhenGetPrivateKeyFromPEMThenValidKeyPair() {
        KeyPair keyPair = DdiSignatureService.getKeyPairFromPrivateKey(TEST_DD_PRIVATE_KEY_01);
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    @Description("Test generateAndCacheSignatures calls generateSignature for each configuration.")
    void givenConfigsWhenGenerateAndCacheSignaturesThenAllProcessed() {
        List<SigningCertificateConfiguration> configs = List.of(signingCertificateConfiguration, signingCertificateConfiguration,
                signingCertificateConfiguration, signingCertificateConfiguration);

        DdiSignatureService spyService = spy(signatureService);

        doReturn("sig").when(spyService)
                .generateAndCacheSignature(anyLong(), anyString(), any(), any(), any());

        spyService.generateAndCacheSignatures(1L, "obj", DdiSignatureType.ESP, configs, null);

        verify(spyService, times(4))
                .generateAndCacheSignature(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @Description("Test generateAndCacheSignatures processes all configs and skips on error configuration.")
    void givenConfigsWhenOneFailsThenOthersProcessed() {
        SigningCertificateConfiguration config1 = mock(SigningCertificateConfiguration.class);
        SigningCertificateConfiguration config2 = mock(SigningCertificateConfiguration.class);
        SigningCertificateConfiguration config3 = mock(SigningCertificateConfiguration.class);

        DdiSignatureService spyService = spy(signatureService);

        doReturn("sig1").when(spyService).generateAndCacheSignature(anyLong(), anyString(), any(), eq(config1), any());
        doThrow(new RuntimeException("fail")).when(spyService).generateAndCacheSignature(anyLong(), anyString(), any(), eq(config2), any());
        doReturn("sig3").when(spyService).generateAndCacheSignature(anyLong(), anyString(), any(), eq(config3), any());

        List<SigningCertificateConfiguration> configs = List.of(config1, config2, config3);
        JpaRollout rollout = mock(JpaRollout.class);

        spyService.generateAndCacheSignatures(1L, "obj", DdiSignatureType.DD, configs, rollout);

        verify(spyService, times(1)).generateAndCacheSignature(anyLong(), anyString(), any(), eq(config1), any());
        verify(spyService, times(1)).generateAndCacheSignature(anyLong(), anyString(), any(), eq(config2), any());
        verify(spyService, times(1)).generateAndCacheSignature(anyLong(), anyString(), any(), eq(config3), any());
    }

    @Test
    @Description("Test getCertificateIssuer returns correct CN from certificate issuer DN.")
    void givenValidCertificateWhenGetCertificateIssuerThenReturnsCN() {
        // Mock certificate with issuer DN containing CN
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getIssuerX500Principal()).thenReturn(
                new javax.security.auth.x500.X500Principal("CN=IssuerCN, O=Company, C=US")
        );
        DdiSignatureService spyService = spy(signatureService);
        doReturn(cert).when(spyService).getCertificateFromS3(anyString(), anyString());

        String cn = spyService.getCertificateIssuer("bucket", "key");
        assertEquals("IssuerCN", cn);
    }

    @Test
    @Description("Test getKeyPairFromPrivateKey throws CosmosSignatureException for invalid PEM.")
    void givenInvalidPEMWhenGetKeyPairFromPrivateKeyThenThrows() {
        assertThrows(CosmosSignatureException.class, () -> {
            DdiSignatureService.getKeyPairFromPrivateKey("invalid-pem");
        });
    }

    @Test
    @Description("Test generateAndCacheSignature returns caches if exists.")
    void givenCachedSignature_whenGenerateAndCacheSignature_thenReturnsCached() {
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("issuer");
        when(redisCacheService.get(anyString(), eq(String.class))).thenReturn(Optional.of("cached-signature"));
        JpaRollout rollout = mock(JpaRollout.class);

        String result = signatureService.generateAndCacheSignature(1L, "obj", DdiSignatureType.DD, signingCertificateConfiguration, rollout);

        assertEquals("cached-signature", result);
        verify(redisCacheService).get(contains("issuer_DD_1"), eq(String.class));
    }

    @Test
    @Description("Test generateAndCacheSignature generates and caches new signature if not cached")
    void givenNoCachedSignature_whenGenerateAndCacheSignature_thenGeneratesAndCaches() {
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("issuer");
        when(redisCacheService.get(anyString(), eq(String.class))).thenReturn(Optional.empty());
        DdiSignatureResult result = DdiSignatureResult.builder()
                .signature("new-signature")
                .signingCertificateExpirationDate(new Date(System.currentTimeMillis() + 10000))
                .build();
        DdiSignatureService spyService = spy(signatureService);
        doReturn(result).when(spyService).generateSignature(anyString(), any(), any(), any());

        String signature = spyService.generateAndCacheSignature(1L, "obj", DdiSignatureType.DD, signingCertificateConfiguration, null);

        assertEquals("new-signature", signature);
        verify(redisCacheService).put(anyString(), eq("new-signature"), anyLong());
    }

    @Test
    @Description("Test generateAndCacheSignature generates signature if cache get throws exception")
    void givenCacheGetThrows_whenGenerateAndCacheSignature_thenGenerates() {
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("issuer");
        when(redisCacheService.get(anyString(), eq(String.class))).thenThrow(new RuntimeException("cache error"));
        DdiSignatureResult result = DdiSignatureResult.builder()
                .signature("sig")
                .signingCertificateExpirationDate(new Date(System.currentTimeMillis() + 10000))
                .build();
        DdiSignatureService spyService = spy(signatureService);
        doReturn(result).when(spyService).generateSignature(anyString(), any(), any(), any());

        String signature = spyService.generateAndCacheSignature(1L, "obj", DdiSignatureType.DD, signingCertificateConfiguration, null);

        assertEquals("sig", signature);
    }

    @Test
    @Description("verifySignatureWithEcuCertificate: combined negative scenarios")
    void verifySignatureWithEcuCertificateCombinedNegativeScenarios() throws Exception {
        // Missing parameters
        assertThrows(ValidationException.class, () -> signatureService.verifySignatureWithEcuCertificate(null, "sig", "p".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ValidationException.class, () -> signatureService.verifySignatureWithEcuCertificate("ecu-1", null, "p".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ValidationException.class, () -> signatureService.verifySignatureWithEcuCertificate("ecu-1", "sig", null));
        assertThrows(ValidationException.class, () -> signatureService.verifySignatureWithEcuCertificate(" ", "sig", "p".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ValidationException.class, () -> signatureService.verifySignatureWithEcuCertificate("ecu-1", " ", "p".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ValidationException.class, () -> signatureService.verifySignatureWithEcuCertificate("ecu-1", "sig", new byte[0]));

        // Missing ECU ID certificate
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("NoSuchKey: The specified key does not exist"));
        assertThrows(org.eclipse.hawkbit.repository.exception.EcuIdCertificateNotFoundException.class,
                () -> signatureService.verifySignatureWithEcuCertificate("ecu-123", "AAA=", "payload".getBytes(StandardCharsets.UTF_8)));

        // Certificate present but signature does not verify
        KeyPair kpCert = generateEcKeyPair();
        String pemCert = generateSelfSignedCertPem(kpCert);
        KeyPair kpOther = generateEcKeyPair();
        byte[] payload = "dummy-payload".getBytes(StandardCharsets.UTF_8);
        String badSignature = base64EcdsaSign(kpOther.getPrivate(), payload);

        mockS3ReturningPem(pemCert);
        boolean verified = signatureService.verifySignatureWithEcuCertificate("ecu-999", badSignature, payload);
        assertEquals(false, verified);
    }

    @Test
    @Description("verifySignatureWithEcuCertificate: certificate present and signature verifies true")
    void givenCertPresentWhenVerifySignatureSucceedsThenReturnsTrue() throws Exception {
        KeyPair kp = generateEcKeyPair();
        String pemCert = generateSelfSignedCertPem(kp);

        byte[] payload = "dummy-payload".getBytes(StandardCharsets.UTF_8);
        String signature = base64EcdsaSign(kp.getPrivate(), payload);

        mockS3ReturningPem(pemCert);

        boolean verified = signatureService.verifySignatureWithEcuCertificate("ecu-123", signature, payload);
        assertEquals(true, verified);
    }

    private void mockS3ReturningPem(String pemContent) {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        new ByteArrayInputStream(pemContent.getBytes(StandardCharsets.UTF_8))
                ));
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    private static String base64EcdsaSign(PrivateKey privateKey, byte[] payload) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(privateKey);
        sig.update(payload);
        byte[] derSignature = sig.sign();
        return java.util.Base64.getEncoder().encodeToString(derSignature);
    }

    private static String generateSelfSignedCertPem(KeyPair keyPair) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        java.math.BigInteger serial = java.math.BigInteger.valueOf(System.currentTimeMillis());
        java.util.Date notBefore = new java.util.Date(System.currentTimeMillis() - 1000L);
        java.util.Date notAfter = new java.util.Date(System.currentTimeMillis() + 86400000L);
        org.bouncycastle.asn1.x500.X500Name subject = new org.bouncycastle.asn1.x500.X500Name("CN=TestECU");

        org.bouncycastle.cert.X509v3CertificateBuilder certBuilder =
                new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                        subject,
                        serial,
                        notBefore,
                        notAfter,
                        subject,
                        keyPair.getPublic()
                );

        org.bouncycastle.operator.ContentSigner signer =
                new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withECDSA")
                        .setProvider("BC")
                        .build(keyPair.getPrivate());

        org.bouncycastle.cert.X509CertificateHolder holder = certBuilder.build(signer);
        java.security.cert.X509Certificate cert =
                new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                        .setProvider("BC")
                        .getCertificate(holder);

        String base64 = java.util.Base64.getEncoder().encodeToString(cert.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        sb.append("-----END CERTIFICATE-----\n");
        return sb.toString();
    }

    @Test
    @Description("Test generateAndCacheSignature returns signature even if cache put throws exception")
    void givenCachePutThrows_whenGenerateAndCacheSignature_thenReturnsSignature() {
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("issuer");
        when(redisCacheService.get(anyString(), eq(String.class))).thenReturn(Optional.empty());
        doThrow(new RuntimeException("cache put error")).when(redisCacheService).put(anyString(), anyString(), anyLong());
        DdiSignatureResult result = DdiSignatureResult.builder()
                .signature("sig")
                .signingCertificateExpirationDate(new Date(System.currentTimeMillis() + 10000))
                .build();
        DdiSignatureService spyService = spy(signatureService);
        doReturn(result).when(spyService).generateSignature(anyString(), any(), any(), any());

        String signature = spyService.generateAndCacheSignature(1L, "obj", DdiSignatureType.DD, signingCertificateConfiguration, null);

        assertEquals("sig", signature);
    }

    @Test
    @Description("Should return issuer from cache when serial number is present in cache.")
    void givenSerialNoInCache_whenGetOrCacheEcuIssuer_thenReturnsCachedIssuer() {
        String serialNo = "123";
        String issuer = "issuerCN";
        when(redisCacheService.get(eq(serialNo), eq(String.class))).thenReturn(Optional.of(issuer));

        String result = signatureService.getOrCacheEcuIssuer(serialNo, "bucket", "key");

        assertEquals(issuer, result);
        verify(redisCacheService).get(serialNo, String.class);
        verify(redisCacheService, never()).put(anyString(), anyString());
    }

    @Test
    @Description("Should throw exception when serial number is not in cache and S3 retrieval fails.")
    void givenSerialNoNotInCacheAndS3Fails_whenGetOrCacheEcuIssuer_thenThrowsException() {
        String serialNo = "123";

        when(redisCacheService.get(eq(serialNo), eq(String.class))).thenReturn(null);
        doThrow(new ValidationException("Error occurred while retrieving signing certificate from S3")).when(s3Client).getObject(any(GetObjectRequest.class));

        assertThrows(Exception.class,
                () -> signatureService.getOrCacheEcuIssuer(serialNo, "bucket", "key"), "Error occurred while retrieving signing certificate from S3");
    }

    @Test
    @Description("Should retrieve issuer from S3, cache it, and verify cached value matches the value put in cache when serial number is not present in cache.")
    void givenSerialNoNotInCache_whenGetOrCacheEcuIssuer_thenRetrievesAndCachesIssuer() {
        String serialNo = "123";

        // Before put, return Optional.empty() (simulate cache miss)
        when(redisCacheService.get(eq(serialNo), eq(String.class))).thenReturn(Optional.empty());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(TEST_DD_SERVER_CERTIFICATE_01.getBytes(StandardCharsets.UTF_8))
        ));

        // Capture the value put in cache
        final String[] cachedValue = new String[1];
        doAnswer(invocation -> {
            cachedValue[0] = invocation.getArgument(1, String.class);
            // After put, return the cached value
            when(redisCacheService.get(eq(serialNo), eq(String.class))).thenReturn(Optional.ofNullable(cachedValue[0]));
            return null;
        }).when(redisCacheService).put(eq(serialNo), anyString(), anyLong());

        DdiSignatureService spyService = spy(signatureService);

        String result = spyService.getOrCacheEcuIssuer(serialNo, "bucket", "key");

        assertNotNull(result);
        verify(redisCacheService).get(serialNo, String.class);

        // Now get should return the value that was put
        Optional<String> cached = redisCacheService.get(serialNo, String.class);
        assertNotNull(cached);
        assertEquals(cachedValue[0], cached.orElse(null));
    }

    @Test
    @Description("Test for successful signature generation with valid parameters.")
    void givenValidCertificatesAndKeysWhenSignatureGeneratedThenSucessKMS() {
        String encodedObject = "dummyDescriptorObject";

        when(signingCertificateConfiguration.getDdPrivateKeyPath()).thenReturn("/test/dd-key");
        when(signingCertificateConfiguration.getDdCertificatePath()).thenReturn("test-bucket/test_cert.pem");
        when(signingCertificateConfiguration.getIntermediateCACertificatePath()).thenReturn("test-bucket/intermediate_ca.pem");
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("BETA_ROW_DS_SUBCA_G1");

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> {
                    InputStream certStream = new ByteArrayInputStream(TEST_DD_SERVER_CERTIFICATE_01.getBytes(StandardCharsets.UTF_8));
                    return new ResponseInputStream<>(
                            GetObjectResponse.builder().build(),
                            new BufferedInputStream(certStream)
                    );
                });

        // Spy the service and mock signWithKms to avoid actual KMS call
        DdiSignatureService spyService = spy(signatureService);
        doReturn(Base64URL.encode(new byte[64]).toString()).when(spyService).signWithKms(any(byte[].class), anyString());
        JpaRollout rollout = mock(JpaRollout.class);

        String signature = spyService.generateSignature(encodedObject, DdiSignatureType.DD, signingCertificateConfiguration, rollout).getSignature();
        assertNotNull(signature);
        assertEquals(3, signature.split("\\.").length);
    }

    @Test
    @Description("Test for failure signature generation with invalid parameters at KMS.")
    void givenValidKeysWhenSignatureGeneratedThenFailureForSignAtKMS() {
        // Arrange
        byte[] signingInput = new byte[]{1, 2, 3};
        String keyId = "invalid-key-id";
        DdiSignatureService spyService = spy(signatureService);
        // Act & Assert
        assertThrows(Exception.class, () -> spyService.signWithKms(signingInput, keyId));
    }

    @Test
    @Description("TTL is zero if expiry date is in the past (via generateAndCacheSignature)")
    void givenExpiryInPastWhenGenerateAndCacheSignatureThenTTLZero() {
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("issuer");
        when(redisCacheService.get(anyString(), eq(String.class))).thenReturn(Optional.empty());
        // Use Instant for date calculation
        Date expired = Date.from(java.time.Instant.now().minus(java.time.Duration.ofSeconds(10)));
        DdiSignatureResult result = DdiSignatureResult.builder()
                .signature("sig")
                .signingCertificateExpirationDate(expired)
                .build();
        DdiSignatureService spyService = spy(signatureService);
        doReturn(result).when(spyService).generateSignature(anyString(), any(), any(), any());

        spyService.generateAndCacheSignature(1L, "obj", DdiSignatureType.DD, signingCertificateConfiguration, null);

        // TTL should be zero, so cache put should be called with 0
        verify(redisCacheService).put(anyString(), eq("sig"), eq(0L));
    }

    @Test
    @Description("TTL is positive if expiry date is in the future (via KafkaMessageUtilTest)")
    void givenExpiryInFutureWhenGenerateAndCacheSignatureThenTTLGtZero() {
        when(signingCertificateConfiguration.getEcuIdIssuer()).thenReturn("issuer");
        when(redisCacheService.get(anyString(), eq(String.class))).thenReturn(Optional.empty());
        // Use Instant for date calculation
        Date future = Date.from(Instant.now().plus(Duration.ofDays(1))); // 1 day in future
        DdiSignatureResult result = DdiSignatureResult.builder()
                .signature("sig")
                .signingCertificateExpirationDate(future)
                .build();
        DdiSignatureService spyService = spy(signatureService);
        doReturn(result).when(spyService).generateSignature(anyString(), any(), any(), any());

        spyService.generateAndCacheSignature(1L, "obj", DdiSignatureType.DD, signingCertificateConfiguration, null);

        // TTL should be > 0
        verify(redisCacheService).put(anyString(), eq("sig"), org.mockito.AdditionalMatchers.gt(0L));
    }


    @Test
    @Description("Test putInCacheWithTTL with invalid parameters does not throw exception")
    void givenExceptionInPutInCacheWithTTL_whenCalled_thenLogsError() throws Exception {
        RedisCacheService cache = mock(RedisCacheService.class);
        doThrow(new RuntimeException("cache error")).when(cache).put(anyString(), anyString(), anyLong());
        DdiSignatureService service = new DdiSignatureService(cache);
        var method = DdiSignatureService.class.getDeclaredMethod("putInCacheWithTTL", String.class, String.class, long.class);
        method.setAccessible(true);
        method.invoke(service, "key", "val", 1L);
        // No exception should be thrown
    }

    private String loadSampleCertificateInBase64DER() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("testCertificateChain/leaf.pem");
        String pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String base64Cert = pemContent
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");

        return base64Cert;
    }
}
