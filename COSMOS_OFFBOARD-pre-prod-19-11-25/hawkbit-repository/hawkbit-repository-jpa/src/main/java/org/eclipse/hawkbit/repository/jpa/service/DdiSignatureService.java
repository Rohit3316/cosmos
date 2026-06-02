package org.eclipse.hawkbit.repository.jpa.service;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.ValidationException;
import org.eclipse.hawkbit.repository.exception.EcuIdCertificateNotFoundException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.naming.ldap.LdapName;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.cosmos.models.ddi.DdiSignatureResult;
import org.cosmos.models.ddi.DdiSignatureType;
import org.cosmos.s3.S3Constants;
import org.eclipse.hawkbit.repository.exception.CosmosSignatureException;
import org.eclipse.hawkbit.repository.exception.EcuCertificateException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * Service for generating and managing DDI signatures.
 * <p>
 * This service handles the generation of signatures for deployment descriptors
 * using private keys and certificates stored in AWS S3 and AWS Systems Manager (SSM).
 * It supports both legacy (local key) and KMS-based signing, depending on the issuer.
 * </p>
 */
@Service
@Slf4j
public class DdiSignatureService {

    /**
     * Issuer constant for legacy (local key) signature generation.
     */
    private static final String ISSUER_STLA_OTATEAM_TEST_PKI_DS_SUBCA_G1 = "stla_otateam_test_pki_ds_subca_g1";
    /**
     * Issuer constant for KMS-based signature generation.
     */
    private static final String ISSUER_BETA_ROW_DS_SUBCA_G1 = "BETA_ROW_DS_SUBCA_G1";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final S3Client s3Client;
    private final SsmClient ssmClient;
    private final RedisCacheService redisCacheService;
    /**
     * Randomization window in days for signature cache TTL, configurable via application properties or SSM.
     */
    @Value("${cosmos.server.dd.signature-cache-randomization-window-days:30}")
    private int signatureCacheRandomizationWindowDays;
    @Value("${cosmos.server.dd.ecu-certificates.bucket.name}")
    private String ecuCertificatesBucketName;

    @Autowired
    public DdiSignatureService(RedisCacheService redisCacheService) {
        this.s3Client = S3Client.create();
        this.ssmClient = SsmClient.create();
        this.redisCacheService = redisCacheService;
    }

    public DdiSignatureService(S3Client s3Client, SsmClient ssmClient, RedisCacheService redisCacheService) {
        Assert.isTrue(s3Client != null, S3Constants.S3_CLIENT_ERROR_MSG);
        Assert.isTrue(ssmClient != null, "SSM client must not be null");
        this.s3Client = s3Client;
        this.ssmClient = ssmClient;
        this.redisCacheService = redisCacheService;
    }

    /**
     * Parses a PEM formatted private key content and returns a KeyPair.
     *
     * @param pemContent the PEM formatted private key content
     * @return KeyPair containing the private key
     * @throws CosmosSignatureException if there is an error while parsing the private key
     */
    public static KeyPair getKeyPairFromPrivateKey(String pemContent) {
        String formattedContent = pemContent.replace("\\n", "\n");
        try (PEMParser pemParser = new PEMParser(new StringReader(formattedContent))) {
            Object readObject;
            while ((readObject = pemParser.readObject()) != null) {
                if (readObject instanceof PEMKeyPair) {
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                    KeyPair keyPair = converter.getKeyPair((PEMKeyPair) readObject);
                    if (keyPair.getPrivate().getAlgorithm().equals("EC")) {
                        return keyPair;
                    }
                    return converter.getKeyPair((PEMKeyPair) readObject);
                }
            }
        } catch (Exception e) {
            log.error("Error while parsing Private Key", e);
            throw new CosmosSignatureException("Error while parsing Private Key", e);
        }
        log.error("Error while reading private key from PEM content");
        throw new CosmosSignatureException("Error while reading private key from PEM content");
    }

    /**
     * Extracts the Common Name (CN) from a Distinguished Name (DN).
     *
     * @param dn the Distinguished Name string
     * @return the Common Name (CN) if found, otherwise null
     * @throws ValidationException if extraction fails
     */
    public static String extractCNFromDN(String dn) {
        try {
            return new LdapName(dn).getRdns().stream()
                    .filter(rdn -> "CN".equalsIgnoreCase(rdn.getType()))
                    .map(rdn -> rdn.getValue().toString())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            throw new ValidationException("Error occurred while extracting CN from DN: " + dn, e);
        }
    }

    /**
     * Generates a signature for the given encodedObject using the specified type and signing certificate configuration,
     * if it is not present in the cache.
     * The signature is also cached based on the ECU ID issuer, type, and file ID.
     *
     * @param fileId                          - Unique identifier of the support package file
     * @param encodedObject                   - The encoded support package to be signed.
     * @param type                            - The type of signature to be generated (ESP, RSP).
     * @param signingCertificateConfiguration - Configuration containing the signing certificate and private key paths.
     * @return The generated signature as a String.
     * <p>
     * Note: this method is Cacheable, which means that the generated signature will be cached in redis for future use.
     */
    public String generateAndCacheSignature(Long fileId, String encodedObject, DdiSignatureType type, SigningCertificateConfiguration signingCertificateConfiguration, Rollout rollout) {

        String cacheKey = signingCertificateConfiguration.getEcuIdIssuer() + "_" + type + "_" + fileId;

        //Try to get the cached signature first
        String cachedSignature = getCachedValue(cacheKey);
        if (cachedSignature != null) {
            log.debug("Returning cached signature for SupportPackage: {} with ECU Issuer: {}", fileId, signingCertificateConfiguration.getEcuIdIssuer());
            return cachedSignature;
        }

        //Not found in cache, generate a new signature
        DdiSignatureResult generatedSignatureResult = generateSignature(encodedObject, type, signingCertificateConfiguration, rollout);

        //update in cache with the generated signature and signature expiry as it's TTL
        putInCacheWithTTL(cacheKey, generatedSignatureResult.getSignature(), computeTTLWithRandomization(generatedSignatureResult.getSigningCertificateExpirationDate()));

        log.debug("Returning new signature for SupportPackage: {} with ECU Issuer: {}", fileId, signingCertificateConfiguration.getEcuIdIssuer());
        return generatedSignatureResult.getSignature();
    }

    /**
     * Generates and caches signatures for the given encodedObject using all the available signing certificate configurations in the system.
     *
     * @param fileId                           - Unique identifier of the support package file
     * @param encodedObject                    - The encoded support package to be signed.
     * @param type                             - The type of signature to be generated (ESP, RSP).
     * @param signingCertificateConfigurations - List of signing certificate configurations containing the signing certificate and private key paths.
     */
    public void generateAndCacheSignatures(Long fileId, String encodedObject, DdiSignatureType type, List<SigningCertificateConfiguration> signingCertificateConfigurations, Rollout rollout) {

        signingCertificateConfigurations.forEach(config -> {
            try {
                log.debug("Generating signature for SupportPackage: {} with ECU Issuer: {}", fileId, config.getEcuIdIssuer());

                generateAndCacheSignature(fileId, encodedObject, type, config, rollout);
            } catch (Exception e) {
                log.error("Error generating signature for SupportPackage: {} with ECU Issuer: {}. Skipping. Error: {}", fileId, config.getEcuIdIssuer(), e.getMessage(), e);
            }
        });
    }

    /**
     * Generate signature for the encodedObject using private key from PEM file.
     *
     * @param encodedObject                   encoded deployment descriptor
     * @param type                            signature type (DD, ESP, RSP)
     * @param signingCertificateConfiguration signing certificate configuration containing paths to private key and certificates
     * @return signature result containing the signature and certificate expiration date
     * @throws CosmosSignatureException if signature generation fails
     *                                  <p>
     *                                  Note: This method does not cache the signature.
     */
    public DdiSignatureResult generateSignature(String encodedObject, DdiSignatureType type, SigningCertificateConfiguration signingCertificateConfiguration, Rollout rollout) {
        String signature;
        Date serverCertificateExpiry;

        try {
            // Load server and intermediateCA certificates from PEM files
            X509Certificate serverCertificate = getCertificate(type, signingCertificateConfiguration);
            X509Certificate intermediateCACert = getCertificate(DdiSignatureType.INTERMEDIATE_CA, signingCertificateConfiguration);

            // Prepare JWT with claims set
            JWTClaimsSet claimsSet = buildJwtClaimsSet(encodedObject, serverCertificate, rollout);

            // Create a list of base64-encoded certificates (x5c parameter)
            // TODO: Using com.nimbusds.jose.util.Base64 library which needs to be addressed later.
            List<com.nimbusds.jose.util.Base64> certChain = new ArrayList<>();
            certChain.add(com.nimbusds.jose.util.Base64.encode(serverCertificate.getEncoded()));
            certChain.add(com.nimbusds.jose.util.Base64.encode(intermediateCACert.getEncoded()));

            // Prepare JWT with header parameters
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .x509CertChain(certChain)
                    .customParam("ecuIdIssuer", signingCertificateConfiguration.getEcuIdIssuer())
                    .customParam("Headers-  cert expiry", serverCertificate.getNotAfter())
                    .build();

            // Use new method to generate signature token based on issuer
            signature = generateSignatureToken(
                    signingCertificateConfiguration.getEcuIdIssuer(),
                    header,
                    claimsSet,
                    type,
                    signingCertificateConfiguration
            );
            serverCertificateExpiry = serverCertificate.getNotAfter();
        } catch (Exception e) {
            log.error("Error while generating signature: {}", e.getMessage());
            throw new CosmosSignatureException("Error while generating signature", e);
        }

        return DdiSignatureResult.builder()
                .signature(signature)
                .signingCertificateExpirationDate(serverCertificateExpiry)
                .build();
    }

    /**
     * Verifies a signature using the ECU's certificate fetched from S3 based on the provided ECU serial number.
     *
     * @param ecuIdSerialNumber ECU ID serial number
     * @param base64Signature   Base64-encoded signature to verify
     * @param payload           Payload bytes against which the signature was created
     * @return true if verification succeeds; false if certificate found but verification fails
     * @throws ValidationException                 if input parameters are missing/invalid
     * @throws EcuIdCertificateNotFoundException   if the ECU ID certificate is not present in S3
     */
    public boolean verifySignatureWithEcuCertificate(String ecuIdSerialNumber, String base64Signature, byte[] payload) {
        if (ecuIdSerialNumber == null || ecuIdSerialNumber.isBlank()) {
            throw new ValidationException("Missing parameter: ECU ID Serial Number");
        }
        if (base64Signature == null || base64Signature.isBlank()) {
            throw new ValidationException("Missing parameter: Signature");
        }
        if (payload == null || payload.length == 0) {
            throw new ValidationException("Missing parameter: Payload");
        }

        String key = ecuIdSerialNumber + "/" + ecuIdSerialNumber + ".pem";

        try {
            // Fetch ECU certificate PEM from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(ecuCertificatesBucketName).key(key).build();
            try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

                // Many ECUs store a chain; take the first certificate as the leaf for verification
                List<Certificate> certificates = (List<Certificate>) certFactory.generateCertificates(new BufferedInputStream(inputStream));
                if (certificates == null || certificates.isEmpty()) {
                    throw new EcuIdCertificateNotFoundException("ECU_ID_CERTIFICATE_NOT_FOUND");
                }
                X509Certificate ecuCert = (X509Certificate) certificates.get(0);

                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initVerify(ecuCert.getPublicKey());
                signature.update(payload);
                byte[] sigBytes = java.util.Base64.getDecoder().decode(base64Signature);
                return signature.verify(sigBytes);
            }
        } catch (EcuIdCertificateNotFoundException e) {
            throw e;
        } catch (S3Exception s3e) {
            String errorCode = s3e.awsErrorDetails() != null ? s3e.awsErrorDetails().errorCode() : null;
            int statusCode = s3e.statusCode();
            if (statusCode == 404 || "NoSuchKey".equalsIgnoreCase(errorCode)) {
                throw new EcuIdCertificateNotFoundException("ECU_ID_CERTIFICATE_NOT_FOUND", s3e);
            }
            throw new ValidationException("S3 error during ECU signature verification", s3e);
        } catch (CertificateException ce) {
            throw new ValidationException("Invalid ECU certificate content", ce);
        } catch (Exception e) {
            // Some test/mocked environments may throw a generic RuntimeException with message indicating missing key
            String msg = e.getMessage();
            if (msg != null && msg.contains("NoSuchKey")) {
                throw new EcuIdCertificateNotFoundException("ECU_ID_CERTIFICATE_NOT_FOUND", e);
            }
            throw new ValidationException("Error during ECU signature verification", e);
        }
    }

    /**
     * Builds the JWT claims set with the provided encoded object, server certificate, and rollout information.
     *
     * @param encodedObject
     * @param serverCertificate
     * @param rollout
     * @return
     */
    private JWTClaimsSet buildJwtClaimsSet(String encodedObject, X509Certificate serverCertificate, Rollout rollout) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .claim("sha256", encodedObject)
                .expirationTime(serverCertificate.getNotAfter())
                .claim("certificate expiry", serverCertificate.getNotAfter());
        if (rollout != null && rollout.getActualStartDate() != null) {
            builder.notBeforeTime(new Date(rollout.getActualStartDate() * 1000));
        }
        return builder.build();
    }

    /**
     * Signs the JWT using AWS KMS and returns the signature in Base64URL format.
     * This method prepares the KMS sign request, calls AWS KMS, and converts the DER signature to JOSE format.
     * It logs the start, success, and any errors encountered during the signing process.
     *
     * @param signingInput the input bytes to sign
     * @param keyId        the KMS key ID
     * @return the Base64URL-encoded signature
     * @throws CosmosSignatureException if signing fails
     */
    public String signWithKms(byte[] signingInput, String keyId) {
        log.debug("Starting KMS signing operation for keyId: {}", keyId);
        try (KmsClient kmsClient = KmsClient.create()) {
            SignRequest signRequest = SignRequest.builder()
                    .keyId(keyId)
                    .message(SdkBytes.fromByteArray(signingInput))
                    .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
                    .build();

            log.debug("Sending sign request to AWS KMS for keyId: {}", keyId);
            SignResponse signResponse = kmsClient.sign(signRequest);
            byte[] derSignature = signResponse.signature().asByteArray();

            log.debug("Received DER signature from KMS for keyId: {}. Converting to JOSE format.", keyId);
            byte[] joseSignature = ECDSA.transcodeSignatureToConcat(derSignature, 64);

            log.debug("KMS signing operation successful for keyId: {}", keyId);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(joseSignature);
        } catch (Exception e) {
            log.error("Error while signing with KMS for keyId: {}: {}", keyId, e.getMessage(), e);
            throw new CosmosSignatureException("Error while signing with KMS", e);
        }
    }

    /**
     * Generates a JWT signature token using KMS or the old implementation based on the issuer name.
     *
     * @param issuerName                      the issuer name to determine which signing method to use
     * @param header                          the JWS header
     * @param claimsSet                       the JWT claims set
     * @param type                            the signature type
     * @param signingCertificateConfiguration the signing certificate configuration
     * @return the signed JWT string
     * @throws CosmosSignatureException if signature generation fails
     */
    private String generateSignatureToken(
            String issuerName,
            JWSHeader header,
            JWTClaimsSet claimsSet,
            DdiSignatureType type,
            SigningCertificateConfiguration signingCertificateConfiguration
    ) {
        log.debug("Generating signature token for issuer: {}", issuerName);
        try {
            if (ISSUER_BETA_ROW_DS_SUBCA_G1.equals(issuerName)) {
                log.debug("Using KMS-based signature generation for issuer: {}", issuerName);
                SignedJWT unsignedJWT = new SignedJWT(header, claimsSet);
                byte[] signingInput = unsignedJWT.getSigningInput();
                String keyId = getKMSPrivateKeyIdByType(type, signingCertificateConfiguration);
                log.debug("Calling signWithKms for keyId: {}", keyId);
                String signatureBase64Url = signWithKms(signingInput, keyId);
                String token = String.join(".",
                        unsignedJWT.getHeader().toBase64URL().toString(),
                        unsignedJWT.getPayload().toBase64URL().toString(),
                        signatureBase64Url);
                log.info("Successfully generated KMS-based signature token for issuer: {}", issuerName);
                return token;
            } else if (ISSUER_STLA_OTATEAM_TEST_PKI_DS_SUBCA_G1.equals(issuerName)) {
                log.debug("Using legacy (local key) signature generation for issuer: {}", issuerName);
                KeyPair keypair = getKeyPairFromPrivateKey(getPrivateKey(type, signingCertificateConfiguration));
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keypair.getPrivate().getEncoded());
                KeyFactory kf = KeyFactory.getInstance("EC");
                ECPrivateKey ecPrivateKey = (ECPrivateKey) kf.generatePrivate(privateKeySpec);
                JWSSigner signer = new ECDSASigner(ecPrivateKey, Curve.P_256);

                SignedJWT signedJWT = new SignedJWT(header, claimsSet);
                signedJWT.sign(signer);
                String token = signedJWT.serialize();
                log.info("Successfully generated legacy signature token for issuer: {}", issuerName);
                return token;
            } else {
                log.error("Unknown issuer for signature generation: {}", issuerName);
                throw new CosmosSignatureException("Unknown issuer for signature generation: " + issuerName);
            }
        } catch (Exception e) {
            log.error("Error while generating signature token for issuer: {}: {}", issuerName, e.getMessage(), e);
            throw new CosmosSignatureException("Error while generating signature token", e);
        }
    }

    /**
     * Retrieves the X.509 certificate from S3 using the bucket name and key.
     *
     * @param bucketName the name of the S3 bucket
     * @param key        the key of the certificate in the S3 bucket
     * @return the X.509 certificate
     * @throws ValidationException if retrieval or parsing fails
     */
    public X509Certificate getCertificateFromS3(String bucketName, String key) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(bufferedInputStream);
        } catch (Exception e) {
            throw new ValidationException("Error occurred while retrieving signing certificate from S3", e);
        }
    }

    /**
     * Retrieves the issuer of the certificate from S3.
     *
     * @param bucketName the name of the S3 bucket
     * @param key        the key of the certificate in the S3 bucket
     * @return the Common Name (CN) of the certificate issuer
     */
    public String getCertificateIssuer(String bucketName, String key) {

        X509Certificate certificate = getCertificateFromS3(bucketName, key);
        return extractCNFromDN(certificate.getIssuerX500Principal().getName());
    }

    /**
     * Retrieves the private key based on the signature type from the signing certificate configuration.
     *
     * @param signatureType                   the type of signature (DD, ESP, RSP)
     * @param signingCertificateConfiguration the signing certificate configuration
     * @return the private key as a String
     */
    private String getPrivateKey(DdiSignatureType signatureType, SigningCertificateConfiguration signingCertificateConfiguration) {
        String privateKeyPath = getKMSPrivateKeyIdByType(signatureType, signingCertificateConfiguration);
        return getParameterFromSSM(privateKeyPath);
    }

    /**
     * Retrieves the X.509 certificate based on the signature type from the signing certificate configuration.
     *
     * @param signatureType                   the type of signature (DD, ESP, RSP)
     * @param signingCertificateConfiguration the signing certificate configuration
     * @return the X.509 certificate
     * @throws ValidationException if the certificate path is invalid or retrieval fails
     */
    public X509Certificate getCertificate(DdiSignatureType signatureType, SigningCertificateConfiguration signingCertificateConfiguration) {

        String certificatePath = getCertificatePathByType(signatureType, signingCertificateConfiguration);

        // Certificate path is supposed to be in the format: "bucketName/key" both of them.
        if (certificatePath == null || certificatePath.trim().isEmpty()) {
            log.error("Certificate path cannot be null or empty");
            throw new ValidationException("Certificate path cannot be null or empty");
        }
        int separatorIndex = certificatePath.indexOf('/');
        if (separatorIndex == -1) {
            log.error("Invalid certificate path format: {}", certificatePath);
            throw new ValidationException("Invalid certificate path format: " + certificatePath);
        }

        String bucketName = certificatePath.substring(0, separatorIndex).trim();
        String key = certificatePath.substring(separatorIndex + 1).trim();

        if (bucketName.isEmpty()) {
            log.error("Bucket name in certificate path cannot be empty");
            throw new ValidationException("Bucket name in certificate path cannot be empty");
        }
        if (key.isEmpty()) {
            log.error("Key in certificate path cannot be empty");
            throw new ValidationException("Key in certificate path cannot be empty");
        }

        return getCertificateFromS3(bucketName, key);
    }

    /**
     * Computes the Time-To-Live (TTL) for the cache entry based on the date provided.
     * The TTL is randomized between the expiry date and (expiry date - randomizationWindowDays).
     * The randomization window (in days) is configurable via application properties or SSM (default: 30).
     *
     * @param expiryDate The date until which TTL to be calculated.
     * @return The TTL in seconds.
     */
    private long computeTTLWithRandomization(Date expiryDate) {
        // Number of days to randomize the TTL window
        int randomizationWindowDays = signatureCacheRandomizationWindowDays;

        // Current time
        Instant nowInstant = Instant.now();
        // Expiry time as Instant
        Instant expiryInstant = Instant.ofEpochMilli(expiryDate.getTime());
        // Earliest allowed cache expiry (expiry minus randomization window)
        Instant earliestAllowedExpiry = expiryInstant.minus(Duration.ofDays(randomizationWindowDays));

        // If expiry is already in the past, TTL is zero
        if (expiryInstant.isBefore(nowInstant)) {
            return 0;
        }
        // If the earliest allowed expiry is in the past, use now as the lower bound
        if (earliestAllowedExpiry.isBefore(nowInstant)) {
            earliestAllowedExpiry = nowInstant;
        }

        // Calculate min and max seconds for the random TTL window
        long minTtlSeconds = Duration.between(nowInstant, earliestAllowedExpiry).getSeconds();
        long maxTtlSeconds = Duration.between(nowInstant, expiryInstant).getSeconds();

        // If the window is 0 or negative, just return the maxTtlSeconds (no randomization)
        if (maxTtlSeconds <= minTtlSeconds) {
            return Math.max(0, maxTtlSeconds);
        }

        // Randomize TTL between minTtlSeconds and maxTtlSeconds (inclusive)
        return ThreadLocalRandom.current().nextLong(minTtlSeconds, maxTtlSeconds + 1);
    }


    /**
     * Retrieves a parameter from AWS Systems Manager (SSM) Parameter Store.
     *
     * @param parameterKey the key of the parameter to retrieve
     * @return the value of the parameter
     * @throws ValidationException if there is an error fetching the parameter
     */
    private String getParameterFromSSM(String parameterKey) {
        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(parameterKey)
                    .withDecryption(true)
                    .build();
            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            return parameterResponse.parameter().value();
        } catch (Exception e) {
            throw new ValidationException("Error occurred while fetching signature keys from SSM: " + parameterKey, e);
        }
    }

    /**
     * Retrieves the cached value for the given cache key.
     *
     * @param cacheKey - The key under which the value is cached.
     * @return The cached value if present, otherwise null.
     */
    private String getCachedValue(String cacheKey) {
        try {
            Optional<String> cachedValue = redisCacheService.get(cacheKey, String.class);
            return cachedValue.orElse(null);
        } catch (Exception e) {
            log.error("Error while fetching cached value from cache for key: {}. Error: {}", cacheKey, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Puts the desired value in the cache with a specified TTL (Time To Live).
     *
     * @param cacheKey   - The key under which the value will be cached.
     * @param cacheValue - The value to be cached.
     * @param ttl        - The time to live for the cached value in seconds.
     */
    private void putInCacheWithTTL(String cacheKey, String cacheValue, long ttl) {
        try {
            redisCacheService.put(cacheKey, cacheValue, ttl);
            log.debug("Cached successfully for key: {}. TTL: {} seconds", cacheKey, ttl);
        } catch (Exception e) {
            log.error("Error while caching for key: {}. Error: {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Puts the desired value in the cache without a TTL.
     *
     * @param cacheKey   The key under which the value will be cached.
     * @param cacheValue The value to be cached.
     */
    private void putInCache(String cacheKey, String cacheValue) {
        try {
            redisCacheService.put(cacheKey, cacheValue);
            log.debug("Cached successfully for key: {} with no TTL", cacheKey);
        } catch (Exception e) {
            log.error("Error while caching for key: {}. Error: {}", cacheKey, e.getMessage(), e);
        }
    }


    /**
     * Retrieves the ECU issuer from cache or S3 and caches it if not present.
     *
     * @param ecuSerialNo       The ECU serial number.
     * @param certificateBucket The S3 bucket containing the certificate.
     * @param certificateKey    The S3 key for the certificate.
     * @return The issuer CN.
     */
    public String getOrCacheEcuIssuer(String ecuSerialNo, String certificateBucket, String certificateKey) {
        String cachedIssuer = getCachedValue(ecuSerialNo);

        if (Objects.nonNull(cachedIssuer)) {
            return cachedIssuer;
        }

        String issuer = getCertificateIssuer(certificateBucket, certificateKey);
        putInCache(ecuSerialNo, issuer);
        return issuer;
    }

    /**
     * Gets the certificate path for the given signature type from the configuration.
     *
     * @param signatureType                   The signature type.
     * @param signingCertificateConfiguration The configuration.
     * @return The certificate path.
     */
    private String getCertificatePathByType(DdiSignatureType signatureType, SigningCertificateConfiguration signingCertificateConfiguration) {
        return switch (signatureType) {
            case DD -> signingCertificateConfiguration.getDdCertificatePath();
            case ESP -> signingCertificateConfiguration.getEspCertificatePath();
            case RSP -> signingCertificateConfiguration.getRspCertificatePath();
            case INTERMEDIATE_CA -> signingCertificateConfiguration.getIntermediateCACertificatePath();
        };
    }

    /**
     * Gets the KMS private key ID for the given signature type from the configuration.
     *
     * @param signatureType                   The signature type.
     * @param signingCertificateConfiguration The configuration.
     * @return The KMS private key ID.
     */
    private String getKMSPrivateKeyIdByType(DdiSignatureType signatureType, SigningCertificateConfiguration signingCertificateConfiguration) {
        return switch (signatureType) {
            case DD -> signingCertificateConfiguration.getDdPrivateKeyPath();
            case ESP -> signingCertificateConfiguration.getEspPrivateKeyPath();
            case RSP -> signingCertificateConfiguration.getRspPrivateKeyPath();
            case INTERMEDIATE_CA -> null;
        };
    }
}
