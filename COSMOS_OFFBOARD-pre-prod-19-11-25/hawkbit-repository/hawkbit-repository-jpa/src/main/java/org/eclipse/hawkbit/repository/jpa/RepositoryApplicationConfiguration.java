/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;


import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.Maps;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.sql.DataSource;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.S3Repository;
import org.cosmos.sns.services.ISnsServiceFactory;
import org.cosmos.sns.services.SnsService;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileDeleteSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.api.DeploymentLogUrlProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.feignclient.kafka.KafkaClient;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.repository.ArtifactEncryption;
import org.eclipse.hawkbit.repository.ArtifactEncryptionSecretsStore;
import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.BaseRepositoryTypeProvider;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentLogManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.PropertiesQuotaManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryDefaultConfiguration;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RoleManagement;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.UserManagement;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.builder.ArtifactsBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.builder.TargetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.UserBuilder;
import org.eclipse.hawkbit.repository.builder.UserElementBuilder;
import org.eclipse.hawkbit.repository.builder.VehicleBuilder;
import org.eclipse.hawkbit.repository.builder.VersionBuilder;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemConfiguration;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemProperties;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.aspects.ExceptionMappingAspectHandler;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignChecker;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignScheduler;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoActionCleanup;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoCleanupScheduler;
import org.eclipse.hawkbit.repository.jpa.autocleanup.CleanupTask;
import org.eclipse.hawkbit.repository.jpa.builder.JpaArtifactsBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaUserBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaUserElementBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaVehicleBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaVersionBuilder;
import org.eclipse.hawkbit.repository.jpa.configuration.MultiTenantJpaTransactionManager;
import org.eclipse.hawkbit.repository.jpa.event.JpaEventEntityManager;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitDefaultServiceExecutor;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.repository.jpa.rollout.RolloutScheduler;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.PauseRolloutGroupAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.jpa.rsql.DefaultRsqlVisitorFactory;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlParserValidationOracle;
import org.eclipse.hawkbit.repository.jpa.service.ArtifactsFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.CdnFileUploadService;
import org.eclipse.hawkbit.repository.jpa.service.DdiSignatureService;
import org.eclipse.hawkbit.repository.jpa.service.DeviceRetryService;
import org.eclipse.hawkbit.repository.jpa.service.EspFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalServiceFactory;
import org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService;
import org.eclipse.hawkbit.repository.jpa.service.RedisCacheService;
import org.eclipse.hawkbit.repository.jpa.service.RolloutAsyncService;
import org.eclipse.hawkbit.repository.jpa.service.RspFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.SoftwareModuleTypeServiceImpl;
import org.eclipse.hawkbit.repository.jpa.service.SupportPackageCdnFileUploadService;
import org.eclipse.hawkbit.repository.jpa.utils.SoftwareModuleTypeService;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.repository.rsql.RsqlVisitorFactory;
import org.eclipse.hawkbit.repository.rsql.RsqlVisitorFactoryHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

/**
 * General configuration for hawkBit's Repository.
 */
@EnableJpaRepositories(value = "org.eclipse.hawkbit.repository.jpa", repositoryBaseClass = SimpleJpaWithNoCountRepository.class, repositoryFactoryBeanClass = CustomBaseRepositoryFactoryBean.class)
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
@Configuration
@EnableScheduling
@EnableRetry
@EntityScan("org.eclipse.hawkbit.repository.jpa.model")
@PropertySource("classpath:/hawkbit-jpa-defaults.properties")
@Import({RepositoryDefaultConfiguration.class, DataSourceAutoConfiguration.class,
        SystemManagementCacheKeyGenerator.class,
        SupportPackageFileSystemConfiguration.class})
@EnableFeignClients(basePackages = "org.eclipse.hawkbit.feignclient")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class RepositoryApplicationConfiguration extends JpaBaseConfiguration {

    protected RepositoryApplicationConfiguration(final DataSource dataSource, final JpaProperties properties,
                                                 final ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider) {
        super(dataSource, properties, jtaTransactionManagerProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public SoftwareModuleTypeService softwareModuleTypeService(SoftwareModuleTypeRepository moduleTypeRepository) {
        return new SoftwareModuleTypeServiceImpl(moduleTypeRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnsAsyncClient snsAsyncClient() {
        return SnsAsyncClient.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public CdnDeleteSnsService cdnDeleteSnsService(SnsAsyncClient snsAsyncClient) {
        return new CdnDeleteSnsService(snsAsyncClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public RolloutAsyncService rolloutAsyncService() {
        return new RolloutAsyncService();
    }

    /**
     * Creates a {@link KafkaMessageService} bean if one is not already defined in the Spring context.
     * <p>
     * This ensures that a default {@code KafkaMessageService} is available for dependency injection.
     *
     * @param kafkaClient the {@link KafkaClient} used by the service to publish messages to the Kafka service
     * @return a new instance of {@link KafkaMessageService}
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaMessageService kafkaMessageService(KafkaClient kafkaClient) {
        return new KafkaMessageService(kafkaClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public S3FileDeleteSnsService s3FileDeleteSnsService(SnsAsyncClient snsAsyncClient) {
        return new S3FileDeleteSnsService(snsAsyncClient);
    }

    /**
     * Creates a new instance of {@link ArtifactsFileRemovalService}.
     *
     * @param artifactFilesystemProperties the properties for the artifact filesystem
     * @param artifactUrlHandlerProperties the properties for the artifact URL handler
     * @param tenantAware                  the tenant aware service
     * @param snsServiceFactory            the SNS service factory
     * @return a new instance of {@link ArtifactsFileRemovalService}
     */
    @Bean
    @ConditionalOnMissingBean
    public ArtifactsFileRemovalService artifactsS3FileDeletionService(final ArtifactFilesystemProperties artifactFilesystemProperties,
                                                                      final ArtifactUrlHandlerProperties artifactUrlHandlerProperties,
                                                                      final TenantAware tenantAware,
                                                                      final SnsServiceFactory snsServiceFactory,
                                                                      final SystemManagement systemManagement,
                                                                      final ArtifactsRepository artifactsRepository) {
        return new ArtifactsFileRemovalService(artifactFilesystemProperties,
                artifactUrlHandlerProperties,
                tenantAware,
                snsServiceFactory,
                systemManagement,
                artifactsRepository);
    }

    /**
     * Creates a new instance of {@link SupportPackageUrlHandlerProperties}.
     *
     * @return a new instance of {@link SupportPackageUrlHandlerProperties}
     */
    @Bean
    @ConditionalOnMissingBean
    public SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties() {
        return new SupportPackageUrlHandlerProperties();
    }

    /**
     * Creates a bean for deployment log URL properties.
     *
     * @return a new instance of {@link DeploymentLogUrlProperties}
     */
    @Bean
    @ConditionalOnMissingBean
    public DeploymentLogUrlProperties deploymentLogUrlProperties() {
        return new DeploymentLogUrlProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public CdnUploadSnsService cdnUploadSnsService(SnsAsyncClient snsAsyncClient) {
        return new CdnUploadSnsService(snsAsyncClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public S3FileTransferSnsService s3FileTransferSnsService(SnsAsyncClient snsAsyncClient) {
        return new S3FileTransferSnsService(snsAsyncClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ISnsServiceFactory snsServiceFactory(List<SnsService<?>> snsServiceList) {
        return new SnsServiceFactory(snsServiceList);
    }


    @Bean
    @ConditionalOnMissingBean
    PauseRolloutGroupAction pauseRolloutGroupAction(final RolloutManagement rolloutManagement,
                                                    final RolloutGroupRepository rolloutGroupRepository, final SystemSecurityContext systemSecurityContext) {
        return new PauseRolloutGroupAction(rolloutManagement, rolloutGroupRepository, systemSecurityContext);
    }

    @Bean
    @ConditionalOnMissingBean
    StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction(
            final RolloutGroupRepository rolloutGroupRepository, final DeploymentManagement deploymentManagement,
            final SystemSecurityContext systemSecurityContext) {
        return new StartNextGroupRolloutGroupSuccessAction(rolloutGroupRepository, deploymentManagement,
                systemSecurityContext);
    }

    @Bean
    @ConditionalOnMissingBean
    ThresholdRolloutGroupErrorCondition thresholdRolloutGroupErrorCondition(final ActionRepository actionRepository) {
        return new ThresholdRolloutGroupErrorCondition(actionRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    ThresholdRolloutGroupSuccessCondition thresholdRolloutGroupSuccessCondition(
            final ActionRepository actionRepository) {
        return new ThresholdRolloutGroupSuccessCondition(actionRepository);
    }

    /**
     * Creates a new instance of {@link SupportPackageCdnFileUploadService}.
     *
     * @param systemManagement                   the system management service
     * @param snsServiceFactory                  the SNS service factory
     * @param tenantAware                        the tenant aware service
     * @param tenantConfigurationManagement      the tenant configuration management service
     * @param supportPackageUrlHandlerProperties the properties for the support package URL handler
     * @param rspRepository                      the RSP repository
     * @param espRepository                      the ESP repository
     * @return a new instance of {@link SupportPackageCdnFileUploadService}
     */
    @Bean
    @ConditionalOnMissingBean
    CdnFileUploadService<BaseSupportPackage> supportPackageCdnFileUploadService(SystemManagement systemManagement,
                                                                                ISnsServiceFactory snsServiceFactory,
                                                                                TenantAware tenantAware,
                                                                                TenantConfigurationManagement tenantConfigurationManagement,
                                                                                SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties,
                                                                                RspRepository rspRepository,
                                                                                EspRepository espRepository) {
        return new SupportPackageCdnFileUploadService(systemManagement, snsServiceFactory, tenantAware, tenantConfigurationManagement,
                supportPackageUrlHandlerProperties, rspRepository, espRepository);
    }

    @Bean
    RolloutGroupEvaluationManager evaluationManager(
            final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupErrorCondition>> errorConditionEvaluators,
            final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupSuccessCondition>> successConditionEvaluators,
            final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupErrorAction>> errorActionEvaluators,
            final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction>> successActionEvaluators) {
        return new RolloutGroupEvaluationManager(errorConditionEvaluators, successConditionEvaluators,
                errorActionEvaluators, successActionEvaluators);
    }

    @Bean
    @ConditionalOnMissingBean
    SystemManagementCacheKeyGenerator systemManagementCacheKeyGenerator() {
        return new SystemManagementCacheKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    AfterTransactionCommitDefaultServiceExecutor afterTransactionCommitDefaultServiceExecutor() {
        return new AfterTransactionCommitDefaultServiceExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    RsqlValidationOracle rsqlValidationOracle() {
        return new RsqlParserValidationOracle();
    }

    @Bean
    @ConditionalOnMissingBean
    QuotaManagement staticQuotaManagement(final HawkbitSecurityProperties securityProperties) {
        return new PropertiesQuotaManagement(securityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutStatusCache rolloutStatusCache(final TenantAware tenantAware) {
        return new RolloutStatusCache(tenantAware);
    }

    @Bean
    @ConditionalOnMissingBean
    ApplicationEventFilter applicationEventFilter(final RepositoryProperties repositoryProperties) {
        return e -> e instanceof TargetPollEvent && !repositoryProperties.isPublishTargetPollEvent();
    }

    /**
     * @param distributionSetTypeManagement to loading the {@link DistributionSetType}
     * @param softwareManagement            for loading {@link DistributionSet#getModules()}
     * @return DistributionSetBuilder bean
     */
    @Bean
    DistributionSetBuilder distributionSetBuilder(final DistributionSetTypeManagement distributionSetTypeManagement,
                                                  final SoftwareModuleManagement softwareManagement, final VersionManagement versionManagement) {
        return new JpaDistributionSetBuilder(distributionSetTypeManagement, softwareManagement, versionManagement);
    }

    @Bean
    TargetBuilder targetBuilder(final TargetTypeManagement targetTypeManagement, final VehicleManagement vehicleManagement) {
        return new JpaTargetBuilder(targetTypeManagement, vehicleManagement);
    }

    @Bean
    UserBuilder userBuilder(final UserManagement userManagement, final SystemManagement systemManagement) {
        return new JpaUserBuilder(userManagement, systemManagement);
    }

    @Bean
    @ConditionalOnMissingBean
    UserElementBuilder userElementBuilder(final UserManagement userManagement, final SystemManagement systemManagement) {
        return new JpaUserElementBuilder(userManagement, systemManagement);
    }


    @Bean
    VersionBuilder versionBuilder(final VersionManagement versionManagement, final SoftwareModuleManagement softwareModuleManagement) {
        return new JpaVersionBuilder(versionManagement, softwareModuleManagement);
    }


    @Bean
    VehicleBuilder vehicleBuilder() {
        return new JpaVehicleBuilder();
    }

    @Bean
    ArtifactsBuilder artifactsBuilder(final ArtifactsManagement artifactsManagement) {
        return new JpaArtifactsBuilder(artifactsManagement);
    }

    /**
     * @param dsTypeManagement for loading
     *                         {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return TargetTypeBuilder bean
     */
    @Bean
    TargetTypeBuilder targetTypeBuilder(final DistributionSetTypeManagement dsTypeManagement) {
        return new JpaTargetTypeBuilder(dsTypeManagement);
    }

    @Bean
    SoftwareModuleMetadataBuilder softwareModuleMetadataBuilder(
            final SoftwareModuleManagement softwareModuleManagement) {
        return new JpaSoftwareModuleMetadataBuilder(softwareModuleManagement);
    }

    /**
     * @param softwareModuleTypeManagement for loading
     *                                     {@link DistributionSetType#getMandatoryModuleTypes()} and
     *                                     {@link DistributionSetType#getOptionalModuleTypes()}
     * @return DistributionSetTypeBuilder bean
     */
    @Bean
    DistributionSetTypeBuilder distributionSetTypeBuilder(
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        return new JpaDistributionSetTypeBuilder(softwareModuleTypeManagement);
    }

    /**
     * @param softwareModuleTypeManagement for loading {@link SoftwareModule#getType()}
     * @return SoftwareModuleBuilder bean
     */
    @Bean
    SoftwareModuleBuilder softwareModuleBuilder(final SoftwareModuleTypeManagement softwareModuleTypeManagement,
                                                final SoftwareModuleFormatManagement softwareModuleFormatManagement,
                                                final SoftwareInstallerTypeManagement softwareInstallerTypeManagement) {
        return new JpaSoftwareModuleBuilder(softwareModuleTypeManagement, softwareModuleFormatManagement, softwareInstallerTypeManagement);
    }

    /**
     * @return RolloutBuilder bean
     */
    @Bean
    RolloutBuilder rolloutBuilder() {
        return new JpaRolloutBuilder();
    }

    /**
     * @param distributionSetManagement for loading
     *                                  {@link TargetFilterQuery#getAutoAssignDistributionSet()}
     * @return TargetFilterQueryBuilder bean
     */
    @Bean
    TargetFilterQueryBuilder targetFilterQueryBuilder(final DistributionSetManagement distributionSetManagement) {
        return new JpaTargetFilterQueryBuilder(distributionSetManagement);
    }

    /**
     * @return the {@link SystemSecurityContext} singleton bean which make it
     * accessible in beans which cannot access the service directly,
     * e.g. JPA entities.
     */
    @Bean
    SystemSecurityContextHolder systemSecurityContextHolder() {
        return SystemSecurityContextHolder.getInstance();
    }

    /**
     * @return the {@link TenantConfigurationManagement} singleton bean which
     * make it accessible in beans which cannot access the service
     * directly, e.g. JPA entities.
     */
    @Bean
    TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
        return TenantConfigurationManagementHolder.getInstance();
    }

    /**
     * @return the {@link SystemManagementHolder} singleton bean which holds the
     * current {@link SystemManagement} service and make it accessible
     * in beans which cannot access the service directly, e.g. JPA
     * entities.
     */
    @Bean
    SystemManagementHolder systemManagementHolder() {
        return SystemManagementHolder.getInstance();
    }

    //Rohit Salunkhe
// @Bean
// SystemManagementHolder systemManagementHolder(
//         SystemManagement systemManagement) {

//     SystemManagementHolder holder =
//             SystemManagementHolder.getInstance();

//     holder.setSystemManagement(systemManagement);

//     System.out.println("Injected holder = " + holder.getSystemManagement());

//     return holder;
// }
//Rohit Salunkhe

    /**
     * @return the {@link TenantAwareHolder} singleton bean which holds the
     * current {@link TenantAware} service and make it accessible in
     * beans which cannot access the service directly, e.g. JPA
     * entities.
     */
    @Bean
    TenantAwareHolder tenantAwareHolder() {
        return TenantAwareHolder.getInstance();
    }

    /**
     * @return the {@link SecurityTokenGeneratorHolder} singleton bean which
     * holds the current {@link SecurityTokenGenerator} service and make
     * it accessible in beans which cannot access the service via
     * injection
     */
    @Bean
    SecurityTokenGeneratorHolder securityTokenGeneratorHolder() {
        return SecurityTokenGeneratorHolder.getInstance();
    }

    /**
     * @return the singleton instance of the {@link EntityInterceptorHolder}
     */
    @Bean
    EntityInterceptorHolder entityInterceptorHolder() {
        return EntityInterceptorHolder.getInstance();
    }

    /**
     * @return the singleton instance of the
     * {@link AfterTransactionCommitExecutorHolder}
     */
    @Bean
    AfterTransactionCommitExecutorHolder afterTransactionCommitExecutorHolder() {
        return AfterTransactionCommitExecutorHolder.getInstance();
    }

    /**
     * Defines the validation processor bean.
     *
     * @return the {@link MethodValidationPostProcessor}
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(Validation.byDefaultProvider().configure()
                .addProperty(BaseHibernateValidatorConfiguration.ALLOW_PARALLEL_METHODS_DEFINE_PARAMETER_CONSTRAINTS,
                        "true")
                .buildValidatorFactory().getValidator());
        return processor;
    }

    /**
     * @return {@link ExceptionMappingAspectHandler} aspect bean
     */
    @Bean
    ExceptionMappingAspectHandler createRepositoryExceptionHandlerAdvice() {
        return new ExceptionMappingAspectHandler();
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter() {
            private final HawkBitEclipseLinkJpaDialect jpaDialect = new HawkBitEclipseLinkJpaDialect();

            @Override
            public EclipseLinkJpaDialect getJpaDialect() {
                return jpaDialect;
            }
        };
    }

    @Override
    protected Map<String, Object> getVendorProperties(DataSource dataSource) {

        final Map<String, Object> properties = Maps.newHashMapWithExpectedSize(7);
        // Turn off dynamic weaving to disable LTW lookup in static weaving mode
        properties.put(PersistenceUnitProperties.WEAVING, "false");
        // needed for reports
        properties.put(PersistenceUnitProperties.ALLOW_NATIVE_SQL_QUERIES, "true");
        // flyway
        properties.put(PersistenceUnitProperties.DDL_GENERATION, "none");
        // Embeed into hawkBit logging
        properties.put(PersistenceUnitProperties.LOGGING_LOGGER, "JavaLogger");
        // Ensure that we flush only at the end of the transaction
        properties.put(PersistenceUnitProperties.PERSISTENCE_CONTEXT_FLUSH_MODE, "COMMIT");
        // Enable batch writing
        properties.put(PersistenceUnitProperties.BATCH_WRITING, "JDBC");
        // Batch size
        properties.put(PersistenceUnitProperties.BATCH_WRITING_SIZE, "500");

        return properties;
    }

    /**
     * {@link MultiTenantJpaTransactionManager} bean.
     *
     * @return a new {@link PlatformTransactionManager}
     * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#transactionManager(ObjectProvider)
     */
    @Override
    @Bean
    public PlatformTransactionManager transactionManager(
            final ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        return new MultiTenantJpaTransactionManager();
    }

    /**
     * {@link JpaSystemManagement} bean.
     *
     * @return a new {@link SystemManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SystemManagement systemManagement(final JpaProperties properties, final SoftwareModuleTypeService moduleTypeService,
                                      final EntityManager entityManager, final TargetRepository targetRepository,
                                      final TargetFilterQueryRepository targetFilterQueryRepository, final DistributionSetRepository distributionSetRepository,
                                      final SoftwareModuleRepository softwareModuleRepository, final TenantMetaDataRepository tenantMetaDataRepository,
                                      final DistributionSetTypeRepository distributionSetTypeRepository, final SoftwareModuleTypeRepository softwareModuleTypeRepository,
                                      final TargetTagRepository targetTagRepository, final TargetTypeRepository targetTypeRepository,
                                      final DistributionSetTagRepository distributionSetTagRepository, final TenantConfigurationRepository tenantConfigurationRepository,
                                      final RolloutRepository rolloutRepository, final TenantAware tenantAware,
                                      final TenantStatsManagement systemStatsManagement, final TenancyCacheManager cacheManager,
                                      final SystemManagementCacheKeyGenerator currentTenantCacheKeyGenerator, final SystemSecurityContext systemSecurityContext,
                                      final PlatformTransactionManager txManager, final RolloutStatusCache rolloutStatusCache,
                                      final TenantConfigurationManagement jpaTenantConfigurationManagement,
                                      final UserRepository userRepository, final UserConfigurationRepository userConfigurationRepository,
                                      final ArtifactModuleLinkRepository artifactModuleLinkRepository, final EspEcuRolloutRepository espEcuRolloutRepository,
                                      final RspRolloutRepository rspRolloutRepository, final SoftwareModuleFormatManagement softwareModuleFormatManagement,
                                      final VehicleRepository vehicleRepository, final VirtualPropertyReplacer virtualPropertyReplacer,
                                      final SoftwareModuleFormatRepository softwareModuleFormatRepository) {
        return new JpaSystemManagement(properties, moduleTypeService, entityManager, targetRepository, targetFilterQueryRepository,
                distributionSetRepository, softwareModuleRepository, tenantMetaDataRepository, distributionSetTypeRepository,
                softwareModuleTypeRepository, targetTagRepository, targetTypeRepository, distributionSetTagRepository,
                tenantConfigurationRepository, rolloutRepository, tenantAware, systemStatsManagement, cacheManager,
                currentTenantCacheKeyGenerator, systemSecurityContext, txManager, rolloutStatusCache,
                jpaTenantConfigurationManagement, userRepository, userConfigurationRepository, artifactModuleLinkRepository,
                espEcuRolloutRepository, rspRolloutRepository, softwareModuleFormatManagement, vehicleRepository, virtualPropertyReplacer, softwareModuleFormatRepository);

    }

    /**
     * {@link JpaDistributionSetManagement} bean.
     *
     * @return a new {@link DistributionSetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    DistributionSetManagement distributionSetManagement(final EntityManager entityManager,
                                                        final DistributionSetRepository distributionSetRepository,
                                                        final ArtifactsManagement artifactsManagement,
                                                        final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
                                                        final DistributionSetTypeManagement distributionSetTypeManagement, final QuotaManagement quotaManagement,
                                                        final DistributionSetMetadataRepository distributionSetMetadataRepository,
                                                        final TargetFilterQueryRepository targetFilterQueryRepository, final ActionRepository actionRepository,
                                                        final EventPublisherHolder eventPublisherHolder, final TenantAware tenantAware,
                                                        final VirtualPropertyReplacer virtualPropertyReplacer,
                                                        final SoftwareModuleRepository softwareModuleRepository,
                                                        final DistributionSetTagRepository distributionSetTagRepository,
                                                        final DistributionSetModuleRepository distributionSetModuleRepository,
                                                        final VersionRepository versionRepository,
                                                        final AfterTransactionCommitExecutor afterCommit, final JpaProperties properties,
                                                        @Lazy final RolloutManagement rolloutManagement) {
        return new JpaDistributionSetManagement(entityManager, distributionSetRepository, artifactsManagement, distributionSetTagManagement,
                systemManagement, distributionSetTypeManagement, quotaManagement, distributionSetMetadataRepository,
                targetFilterQueryRepository, actionRepository, eventPublisherHolder, tenantAware,
                virtualPropertyReplacer, softwareModuleRepository, distributionSetTagRepository,
                distributionSetModuleRepository,
                versionRepository,
                afterCommit,
                properties.getDatabase(),
                rolloutManagement);

    }

    /**
     * {@link JpaDistributionSetManagement} bean.
     *
     * @return a new {@link DistributionSetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DistributionSetTypeManagement distributionSetTypeManagement(
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository, final TargetTypeRepository targetTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties,
            final QuotaManagement quotaManagement) {
        return new JpaDistributionSetTypeManagement(distributionSetTypeRepository, softwareModuleTypeRepository,
                distributionSetRepository, targetTypeRepository, virtualPropertyReplacer, properties.getDatabase(),
                quotaManagement);
    }

    /**
     * {@link JpaTargetTypeManagement} bean.
     *
     * @return a new {@link TargetTypeManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetTypeManagement targetTypeManagement(final TargetTypeRepository targetTypeRepository,
                                              final TargetRepository targetRepository, final DistributionSetTypeRepository distributionSetTypeRepository,
                                              final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties,
                                              final QuotaManagement quotaManagement) {
        return new JpaTargetTypeManagement(targetTypeRepository, targetRepository, distributionSetTypeRepository,
                virtualPropertyReplacer, properties.getDatabase(), quotaManagement);
    }

    /**
     * {@link JpaUserManagement} bean.
     *
     * @return a new {@link TargetTypeManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    UserManagement userManagement(final UserRepository userRepository, final EntityManager entityManager, final UserElementRepository userElementRepository, final TenantMetaDataRepository tenantMetaDataRepository) {
        return new JpaUserManagement(userRepository, tenantMetaDataRepository);
    }


    /**
     * {@link JpaArtifactsManagement} bean.
     *
     * @return a new {@link ArtifactsManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    ArtifactsManagement artifactsManagement(final ArtifactsRepository artifactsRepository,
                                            final ArtifactModuleLinkRepository artifactModuleLinkRepository,
                                            final TenantAware tenantAware, final ArtifactFilesystemProperties artifactFilesystemProperties,
                                            final ArtifactUrlHandlerProperties artifactUrlHandlerProperties, final SoftwareModuleRepository softwareModuleRepository,
                                            final S3Repository s3Repository, final SystemSecurityContext systemSecurityContext,
                                            final SystemManagement systemManagement, final TenantConfigurationManagement tenantConfigurationManagement,
                                            final ISnsServiceFactory snsServiceFactory, @Lazy final RolloutManagement rolloutManagement,
                                            final ActionArtifactRepository actionArtifactRepository,
                                            final KafkaMessageService kafkaMessageService,
                                            final ArtifactsFileRemovalService artifactsFileRemovalService,
                                            final S3MultipartFileUpload s3MultipartFileUpload,
                                            @Lazy final SoftwareModuleManagement softwareModuleManagement, final VersionManagement versionManagement,
                                            final EntityManager entityManager, final VirtualPropertyReplacer virtualPropertyReplacer,
                                            final JpaProperties properties) {
        return new JpaArtifactsManagement(artifactsRepository, artifactModuleLinkRepository, tenantAware,
                artifactFilesystemProperties, artifactUrlHandlerProperties, softwareModuleRepository, systemSecurityContext, systemManagement,
                tenantConfigurationManagement, snsServiceFactory, rolloutManagement, actionArtifactRepository, kafkaMessageService,
                artifactsFileRemovalService, s3Repository, s3MultipartFileUpload, softwareModuleManagement, versionManagement, entityManager,
                virtualPropertyReplacer, properties.getDatabase());
    }

    /**
     * {@link JpaVersionManagement} bean.
     *
     * @return a new {@link VersionManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    VersionManagement versionManagement(final VersionRepository versionRepository, final SoftwareModuleRepository smr) {
        return new JpaVersionManagement(versionRepository, smr);
    }


    /**
     * {@link JpaTenantStatsManagement} bean.
     *
     * @return a new {@link TenantStatsManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TenantStatsManagement tenantStatsManagement() {
        return new JpaTenantStatsManagement();
    }

    /**
     * {@link JpaTenantConfigurationManagement} bean.
     *
     * @return a new {@link TenantConfigurationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TenantConfigurationManagement tenantConfigurationManagement() {
        return new JpaTenantConfigurationManagement();
    }

    /**
     * {@link JpaTargetManagement} bean.
     *
     * @return a new {@link JpaTargetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetManagement targetManagement(final EntityManager entityManager, final QuotaManagement quotaManagement,
                                      final TargetRepository targetRepository, final TargetMetadataRepository targetMetadataRepository,
                                      final RolloutGroupRepository rolloutGroupRepository,
                                      final TargetFilterQueryRepository targetFilterQueryRepository,
                                      final TargetTypeRepository targetTypeRepository, final TargetTagRepository targetTagRepository,
                                      final EventPublisherHolder eventPublisherHolder, final TenantAware tenantAware,
                                      final AfterTransactionCommitExecutor afterCommit, final VirtualPropertyReplacer virtualPropertyReplacer,
                                      final JpaProperties properties, final DistributionSetManagement distributionSetManagement,
                                      final VehicleManagement vehicleManagement,
                                      @Lazy final RolloutManagement rolloutManagement,
                                      final ActionRepository actionRepository,
                                      final TargetInventoryRepository targetInventoryRepository) {
        return new JpaTargetManagement(entityManager, distributionSetManagement, quotaManagement, targetRepository,
                targetTypeRepository, targetMetadataRepository, rolloutGroupRepository, targetFilterQueryRepository,
                targetTagRepository, eventPublisherHolder, tenantAware, afterCommit, virtualPropertyReplacer,
                properties.getDatabase(), vehicleManagement, rolloutManagement, actionRepository, targetInventoryRepository);
    }

    /**
     * {@link JpaTargetFilterQueryManagement} bean.
     *
     * @param targetFilterQueryRepository holding {@link TargetFilterQuery} entities
     * @param targetManagement            managing {@link Target} entities
     * @param virtualPropertyReplacer     for RSQL handling
     * @param distributionSetManagement   for auto assign DS access
     * @param quotaManagement             to access quotas
     * @param properties                  JPA properties
     * @param tenantAware                 the {@link TenantAware} bean holding the tenant information
     * @return a new {@link TargetFilterQueryManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetFilterQueryManagement targetFilterQueryManagement(
            final TargetFilterQueryRepository targetFilterQueryRepository, final TargetManagement targetManagement,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final JpaProperties properties, final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware) {
        return new JpaTargetFilterQueryManagement(targetFilterQueryRepository, targetManagement,
                virtualPropertyReplacer, distributionSetManagement, quotaManagement, properties.getDatabase(),
                tenantConfigurationManagement, systemSecurityContext, tenantAware);
    }

    /**
     * {@link JpaTargetTagManagement} bean.
     *
     * @return a new {@link TargetTagManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetTagManagement targetTagManagement(final TargetTagRepository targetTagRepository,
                                            final TargetRepository targetRepository, final VirtualPropertyReplacer virtualPropertyReplacer,
                                            final JpaProperties properties, final TargetManagement targetManagement,
                                            final EntityManager entityManager) {
        return new JpaTargetTagManagement(targetTagRepository, targetRepository, virtualPropertyReplacer,
                properties.getDatabase(), targetManagement, entityManager);
    }

    /**
     * {@link JpaDistributionSetTagManagement} bean.
     *
     * @return a new {@link JpaDistributionSetTagManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DistributionSetTagManagement distributionSetTagManagement(
            final DistributionSetTagRepository distributionSetTagRepository,
            final DistributionSetRepository distributionSetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties) {
        return new JpaDistributionSetTagManagement(distributionSetTagRepository, distributionSetRepository,
                virtualPropertyReplacer, properties.getDatabase());
    }

    /**
     * {@link JpaSoftwareModuleManagement} bean.
     *
     * @return a new {@link SoftwareModuleManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SoftwareModuleManagement softwareModuleManagement(final EntityManager entityManager,
                                                      final DistributionSetRepository distributionSetRepository,
                                                      final SoftwareModuleRepository softwareModuleRepository,
                                                      final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
                                                      final SoftwareModuleTypeRepository softwareModuleTypeRepository, final AuditorAware<String> auditorProvider,
                                                      final ArtifactsManagement artifactsManagement, final QuotaManagement quotaManagement,
                                                      final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties,
                                                      final EcuModelManagement ecuModelManagement) {
        return new JpaSoftwareModuleManagement(entityManager, distributionSetRepository, softwareModuleRepository,
                softwareModuleMetadataRepository, softwareModuleTypeRepository, auditorProvider, artifactsManagement,
                quotaManagement, virtualPropertyReplacer, properties.getDatabase(), ecuModelManagement);
    }

    /**
     * {@link JpaSoftwareModuleTypeManagement} bean.
     *
     * @return a new {@link SoftwareModuleTypeManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SoftwareModuleTypeManagement softwareModuleTypeManagement(
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository, final JpaProperties properties) {
        return new JpaSoftwareModuleTypeManagement(distributionSetTypeRepository, softwareModuleTypeRepository,
                virtualPropertyReplacer, softwareModuleRepository, properties.getDatabase());
    }

    @Bean
    @ConditionalOnMissingBean
    SoftwareModuleFormatManagement softwareModuleFormatManagement(

            final SoftwareModuleFormatRepository softwareModuleFormatRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository, final JpaProperties properties) {
        return new JpaSoftwareModuleFormatManagement(softwareModuleFormatRepository,
                virtualPropertyReplacer, softwareModuleRepository, properties.getDatabase());
    }


    @Bean
    @ConditionalOnMissingBean
    RolloutHandler rolloutHandler(final TenantAware tenantAware, final RolloutManagement rolloutManagement,
                                  final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
                                  final PlatformTransactionManager txManager) {
        return new JpaRolloutHandler(tenantAware, rolloutManagement, rolloutExecutor, lockRegistry, txManager);
    }

    @Bean
    @ConditionalOnMissingBean
    HandleRolloutSchedulerService processRolloutGroups(final RolloutRepository rolloutRepository,
                                                       final ActionRepository actionRepository, final RolloutGroupRepository rolloutGroupRepository,
                                                       final RolloutTargetGroupRepository rolloutTargetGroupRepository, final TenantConfigurationManagement tenantConfigurationManagement,
                                                       final SupportPackageManagement supportPackageManagement, final SystemSecurityContext systemSecurityContext, final RolloutApprovalStrategy rolloutApprovalStrategy,
                                                       final RolloutGroupManagement rolloutGroupManagement, final RolloutManagement rolloutManagement, final DeploymentManagement deploymentManagement,
                                                       final AfterTransactionCommitExecutor afterCommit, final TenantAware tenantAware, final EventPublisherHolder eventPublisherHolder, final EntityManager entityManager,
                                                       final RolloutGroupEvaluationManager evaluationManager, KafkaMessageService kafkaMessageService, final TargetRepository targetRepository,final SystemManagement systemManagement ) {

        return new HandleRolloutSchedulerService(rolloutRepository, actionRepository, rolloutGroupRepository, rolloutTargetGroupRepository, tenantConfigurationManagement,
                supportPackageManagement, systemSecurityContext, rolloutApprovalStrategy, rolloutGroupManagement, rolloutManagement, deploymentManagement, afterCommit, tenantAware,
                eventPublisherHolder, entityManager, evaluationManager, kafkaMessageService, targetRepository,systemManagement);
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutExecutor rolloutExecutor(final RolloutTargetGroupRepository rolloutTargetGroupRepository,
                                    final EntityManager entityManager, final RolloutRepository rolloutRepository,
                                    final ActionRepository actionRepository, final RolloutGroupRepository rolloutGroupRepository,
                                    final AfterTransactionCommitExecutor afterCommit, final TenantAware tenantAware,
                                    final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement,
                                    final DeploymentManagement deploymentManagement, final TargetManagement targetManagement,
                                    final EventPublisherHolder eventPublisherHolder, final PlatformTransactionManager txManager,
                                    final RolloutApprovalStrategy rolloutApprovalStrategy, final RolloutGroupEvaluationManager evaluationManager,
                                    final RolloutManagement rolloutManagement, final ActionStatusRepository actionStatusRepository,
                                    final SupportPackageManagement supportPackageManagement,
                                    final SystemManagement systemManagement,
                                    final TenantConfigurationManagement tenantConfigurationManagement,
                                    final SystemSecurityContext systemSecurityContext,
                                    final KafkaMessageService kafkaMessageService) {
        return new JpaRolloutExecutor(rolloutTargetGroupRepository, entityManager, rolloutRepository, actionRepository,
                rolloutGroupRepository, afterCommit, tenantAware, rolloutGroupManagement, quotaManagement,
                deploymentManagement, targetManagement, eventPublisherHolder, txManager, rolloutApprovalStrategy,
                evaluationManager, rolloutManagement, actionStatusRepository, supportPackageManagement,
                systemManagement, tenantConfigurationManagement, systemSecurityContext, kafkaMessageService);
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutManagement rolloutManagement(final TargetManagement targetManagement,
                                        @Qualifier("distributionSetManagement") DistributionSetManagement distributionSetManagement,
                                        final VirtualPropertyReplacer virtualPropertyReplacer,
                                        final JpaProperties properties,
                                        final RolloutApprovalStrategy rolloutApprovalStrategy,
                                        final TenantConfigurationManagement tenantConfigurationManagement,
                                        final SystemSecurityContext systemSecurityContext,
                                        final DistributionSetTagRepository distributionSetTagRepository,
                                        @Lazy SupportPackageManagement supportPackageManagement,
                                        final TargetTagManagement targetTagManagement,
                                        final TargetFilterQueryManagement targetFilterQueryManagement,
                                        final RolloutGroupManagement rolloutGroupManagement,
                                        @Lazy SoftwareModuleManagement softwareModuleManagement,
                                        final ArtifactsManagement artifactsManagement,
                                        @Lazy EntityFactory entityFactory,
                                        final VersionManagement versionManagement,
                                        final SystemManagement systemManagement,
                                        final DistributionSetTypeManagement distributionSetTypeManagement,
                                        final TargetRepository targetRepository,
                                        final TargetTargetTagRepository targetTargetTagRepository,
                                        final KafkaMessageService kafkaMessageService,
                                        final DeploymentManagement deploymentManagement) {
        return new JpaRolloutManagement(targetManagement, distributionSetManagement,
                virtualPropertyReplacer, properties.getDatabase(), rolloutApprovalStrategy,
                tenantConfigurationManagement, systemSecurityContext, distributionSetTagRepository, supportPackageManagement, targetTagManagement, targetFilterQueryManagement,
                rolloutGroupManagement, softwareModuleManagement, artifactsManagement,
                entityFactory, versionManagement, systemManagement, distributionSetTypeManagement, targetRepository,
                targetTargetTagRepository, kafkaMessageService, deploymentManagement);
    }


    @Bean
    @ConditionalOnMissingBean
    RoleManagement roleManagement(final RoleRepository roleRepository) {
        return new JpaRoleManagement(roleRepository);
    }


    /**
     * {@link DefaultRolloutApprovalStrategy} bean.
     *
     * @return a new {@link RolloutApprovalStrategy}
     */
    @Bean
    @ConditionalOnMissingBean
    RolloutApprovalStrategy rolloutApprovalStrategy(final UserDetailsService userDetailsService,
                                                    final TenantConfigurationManagement tenantConfigurationManagement,
                                                    final SystemSecurityContext systemSecurityContext) {
        return new DefaultRolloutApprovalStrategy(userDetailsService, tenantConfigurationManagement,
                systemSecurityContext);
    }

    /**
     * {@link JpaRolloutGroupManagement} bean.
     *
     * @return a new {@link RolloutGroupManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    RolloutGroupManagement rolloutGroupManagement(final RolloutGroupRepository rolloutGroupRepository,
                                                  final RolloutRepository rolloutRepository, final ActionRepository actionRepository,
                                                  final TargetRepository targetRepository, final EntityManager entityManager,
                                                  final VirtualPropertyReplacer virtualPropertyReplacer, final RolloutStatusCache rolloutStatusCache,
                                                  final JpaProperties properties, final RolloutTargetGroupRepository rolloutTargetGroupRepository,
                                                  final TargetManagement targetManagement, final QuotaManagement quotaManagement) {
        return new JpaRolloutGroupManagement(rolloutGroupRepository, rolloutRepository, actionRepository,
                targetRepository, entityManager, virtualPropertyReplacer, rolloutStatusCache, properties.getDatabase(), rolloutTargetGroupRepository, targetManagement, quotaManagement);
    }

    /**
     * {@link JpaDeploymentManagement} bean.
     *
     * @return a new {@link DeploymentManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DeploymentManagement deploymentManagement(final EntityManager entityManager,
                                              final ActionRepository actionRepository, final DistributionSetRepository distributionSetRepository,
                                              final DistributionSetManagement distributionSetManagement, final TargetRepository targetRepository,
                                              final ActionStatusRepository actionStatusRepository, final AuditorAware<String> auditorProvider,
                                              final EventPublisherHolder eventPublisherHolder, final AfterTransactionCommitExecutor afterCommit,
                                              final VirtualPropertyReplacer virtualPropertyReplacer, final PlatformTransactionManager txManager,
                                              final TenantConfigurationManagement tenantConfigurationManagement, final QuotaManagement quotaManagement,
                                              final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware,
                                              final JpaProperties properties, final RepositoryProperties repositoryProperties,
                                              final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository,
                                              final RolloutGroupManagement rolloutGroupManagement,
                                              final RolloutRepository rolloutRepository, final TargetManagement targetManagement,
                                              final KafkaMessageService kafkaMessageService) {
        return new JpaDeploymentManagement(entityManager, actionRepository, distributionSetManagement,
                distributionSetRepository, targetRepository, actionStatusRepository, auditorProvider,
                eventPublisherHolder, afterCommit, virtualPropertyReplacer, txManager, tenantConfigurationManagement,
                quotaManagement, systemSecurityContext, tenantAware, properties.getDatabase(), repositoryProperties, actionStatusUserAcceptanceRepository,
                rolloutGroupManagement, rolloutRepository, targetManagement, kafkaMessageService);
    }

    @Bean
    @ConditionalOnMissingBean
    ConfirmationManagement confirmationManagement(final TargetRepository targetRepository,
                                                  final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
                                                  final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository,
                                                  final RepositoryProperties repositoryProperties, final QuotaManagement quotaManagement,
                                                  final EntityFactory entityFactory) {
        return new JpaConfirmationManagement(targetRepository, actionRepository, actionStatusRepository,
                actionStatusUserAcceptanceRepository, repositoryProperties, quotaManagement, entityFactory);
    }

    /**
     * {@link JpaControllerManagement} bean.
     *
     * @return a new {@link ControllerManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    ControllerManagement controllerManagement(final ScheduledExecutorService executorService,
                                              final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository, final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository,
                                              final QuotaManagement quotaManagement, final RepositoryProperties repositoryProperties,
                                              final KafkaMessageService kafkaMessageService) {
        return new JpaControllerManagement(executorService, actionRepository, actionStatusRepository, actionStatusUserAcceptanceRepository, quotaManagement,
                repositoryProperties, kafkaMessageService);
    }

    /**
     * Creates a {@link DeploymentLogManagement} bean for managing deployment logs.
     *
     * @param deploymentLogRepository    the repository for deployment logs
     * @param quotaManagement            quota management service
     * @param tenantAware                tenant context provider
     * @param s3MultipartFileUpload      S3 multipart upload service
     * @param s3Repository               S3 repository service
     * @param deploymentLogUrlProperties properties for deployment log URLs
     * @return a new instance of {@link DeploymentLogManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public DeploymentLogManagement deploymentLogManagement(
            final DeploymentLogRepository deploymentLogRepository,
            final QuotaManagement quotaManagement,
            final TenantAware tenantAware,
            final S3MultipartFileUpload s3MultipartFileUpload,
            final S3Repository s3Repository,
            final DeploymentLogUrlProperties deploymentLogUrlProperties) {
        return new JpaDeploymentLogManagement(tenantAware, deploymentLogRepository,
                quotaManagement, s3MultipartFileUpload, s3Repository, deploymentLogUrlProperties);
    }

    /**
     * {@link JpaEntityFactory} bean.
     *
     * @return a new {@link EntityFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    EntityFactory entityFactory() {
        return new JpaEntityFactory();
    }

    /**
     * {@link EventEntityManagerHolder} bean.
     *
     * @return a new {@link EventEntityManagerHolder}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManagerHolder eventEntityManagerHolder() {
        return EventEntityManagerHolder.getInstance();
    }

    /**
     * {@link EventEntityManager} bean.
     *
     * @param aware         the tenant aware
     * @param entityManager the entitymanager
     * @return a new {@link EventEntityManager}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManager eventEntityManager(final TenantAware aware, final EntityManager entityManager) {
        return new JpaEventEntityManager(aware, entityManager);
    }

    /**
     * {@link AutoAssignChecker} bean.
     *
     * @param targetFilterQueryManagement to get all target filter queries
     * @param targetManagement            to get targets
     * @param deploymentManagement        to assign distribution sets to targets
     * @param transactionManager          to run transactions
     * @return a new {@link AutoAssignChecker}
     */
    @Bean
    @ConditionalOnMissingBean
    AutoAssignExecutor autoAssignExecutor(final TargetFilterQueryManagement targetFilterQueryManagement,
                                          final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
                                          final PlatformTransactionManager transactionManager, final TenantAware tenantAware) {
        return new AutoAssignChecker(targetFilterQueryManagement, targetManagement, deploymentManagement,
                transactionManager, tenantAware);
    }

    /**
     * {@link AutoAssignScheduler} bean.
     * <p>
     * Note: does not activate in test profile, otherwise it is hard to test the
     * auto assign functionality.
     *
     * @param systemManagement      to find all tenants
     * @param systemSecurityContext to run as system
     * @param autoAssignExecutor    to run a check as tenant
     * @param lockRegistry          to lock the tenant for auto assignment
     * @return a new {@link AutoAssignChecker}
     */
    @Bean
    @ConditionalOnMissingBean
    // don't active the auto assign scheduler in test, otherwise it is hard to
    // test
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autoassign.scheduler", name = "enabled", matchIfMissing = true)
    AutoAssignScheduler autoAssignScheduler(final SystemManagement systemManagement,
                                            final SystemSecurityContext systemSecurityContext, final AutoAssignExecutor autoAssignExecutor,
                                            final LockRegistry lockRegistry) {
        return new AutoAssignScheduler(systemManagement, systemSecurityContext, autoAssignExecutor, lockRegistry);
    }

    /**
     * {@link AutoActionCleanup} bean.
     *
     * @param deploymentManagement Deployment management service
     * @param configManagement     Tenant configuration service
     * @return a new {@link AutoActionCleanup} bean
     */
    @Bean
    CleanupTask actionCleanup(final DeploymentManagement deploymentManagement,
                              final TenantConfigurationManagement configManagement) {
        return new AutoActionCleanup(deploymentManagement, configManagement);
    }

    /**
     * {@link AutoCleanupScheduler} bean.
     *
     * @param systemManagement      to find all tenants
     * @param systemSecurityContext to run as system
     * @param lockRegistry          to lock the tenant for auto assignment
     * @param cleanupTasks          a list of cleanup tasks
     * @return a new {@link AutoCleanupScheduler} bean
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autocleanup.scheduler", name = "enabled", matchIfMissing = true)
    AutoCleanupScheduler autoCleanupScheduler(final SystemManagement systemManagement,
                                              final SystemSecurityContext systemSecurityContext, final LockRegistry lockRegistry,
                                              final List<CleanupTask> cleanupTasks) {
        return new AutoCleanupScheduler(systemManagement, systemSecurityContext, lockRegistry, cleanupTasks);
    }

    /**
     * {@link RolloutScheduler} bean.
     * <p>
     * Note: does not activate in test profile, otherwise it is hard to test the
     * rollout handling functionality.
     *
     * @param systemManagement      to find all tenants
     * @param rolloutHandler        to run the rollout handler
     * @param systemSecurityContext to run as system
     * @return a new {@link RolloutScheduler} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.rollout.scheduler", name = "enabled", matchIfMissing = true)
    RolloutScheduler rolloutScheduler(final TenantAware tenantAware, final SystemManagement systemManagement,
                                      final RolloutHandler rolloutHandler, final SystemSecurityContext systemSecurityContext) {
        return new RolloutScheduler(systemManagement, rolloutHandler, systemSecurityContext);
    }

    /**
     * Creates the {@link RsqlVisitorFactory} bean.
     *
     * @return A new {@link RsqlVisitorFactory} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    RsqlVisitorFactory rsqlVisitorFactory() {
        return new DefaultRsqlVisitorFactory();
    }

    /**
     * Obtains the {@link RsqlVisitorFactoryHolder} bean.
     *
     * @return The {@link RsqlVisitorFactoryHolder} singleton.
     */
    @Bean
    RsqlVisitorFactoryHolder rsqlVisitorFactoryHolder() {
        return RsqlVisitorFactoryHolder.getInstance();
    }

    /**
     * {@link JpaDistributionSetInvalidationManagement} bean.
     *
     * @return a new {@link JpaDistributionSetInvalidationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    JpaDistributionSetInvalidationManagement distributionSetInvalidationManagement(
            final DistributionSetManagement distributionSetManagement, final RolloutManagement rolloutManagement,
            final DeploymentManagement deploymentManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final PlatformTransactionManager txManager,
            final RepositoryProperties repositoryProperties, final TenantAware tenantAware,
            final LockRegistry lockRegistry) {
        return new JpaDistributionSetInvalidationManagement(distributionSetManagement, rolloutManagement,
                deploymentManagement, targetFilterQueryManagement, txManager, repositoryProperties, tenantAware,
                lockRegistry);
    }

    /**
     * Our default {@link BaseRepositoryTypeProvider} bean always provides the
     * NoCountBaseRepository
     *
     * @return a {@link BaseRepositoryTypeProvider} bean
     */
    @Bean
    @ConditionalOnMissingBean
    BaseRepositoryTypeProvider baseRepositoryTypeProvider() {
        return new NoCountBaseRepositoryTypeProvider();

    }

    /**
     * Default artifact encryption service bean that internally uses
     * {@link ArtifactEncryption} and {@link ArtifactEncryptionSecretsStore}
     * beans for {@link SoftwareModule} artifacts encryption/decryption
     *
     * @return a {@link ArtifactEncryptionService} bean
     */
    @Bean
    @ConditionalOnMissingBean
    ArtifactEncryptionService artifactEncryptionService() {
        return ArtifactEncryptionService.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean
    VehicleManagement vehicleManagement(final EcuModelManagement ecuModelManagement, final VehicleRepository vehicleRepository, @Lazy final TargetManagement targetManagement, @Lazy final RolloutManagement rolloutManagement, final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties) {
        return new JpaVehicleManagement(ecuModelManagement, vehicleRepository, targetManagement, rolloutManagement, virtualPropertyReplacer, properties.getDatabase());
    }

    /**
     * Creates a {@link PKIManagement} bean that manages Public Key Infrastructure (PKI) related operations.
     *
     * @param signingCertificateConfigurationRepository signing certificate configuration repository
     * @return a new {@link PKIManagement} bean
     */
    @Bean
    @ConditionalOnMissingBean
    PKIManagement pkiManagement(final SigningCertificateConfigurationRepository signingCertificateConfigurationRepository) {
        return new JpaPKIManagement(signingCertificateConfigurationRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    EcuModelManagement ecuModelManagement(final EcuModelRepository ecuModelRepository,
                                          final EcuModelTypeRepository ecuModelTypeRepository, final VehicleRepository vehicleRepository, final SoftwareModuleRepository softwareModuleRepository,final VirtualPropertyReplacer virtualPropertyReplacer,final JpaProperties properties) {
        return new JpaEcuModelModelManagement(ecuModelRepository, ecuModelTypeRepository, vehicleRepository, softwareModuleRepository, virtualPropertyReplacer, properties.getDatabase());
    }

    @Bean
    @ConditionalOnMissingBean
    SoftwareInstallerTypeManagement softwareInstallerTypeManagement(final SoftwareInstallerTypeRepository softwareInstallerTypeRepository) {
        return new JpaSoftwareInstallerTypeManagement(softwareInstallerTypeRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    SupportPackageManagement supportPackageManagement(EspEcuRolloutRepository espEcuRolloutRepository,
                                                      RspRolloutRepository rspRolloutRepository,
                                                      EspRepository espRepository,
                                                      @Lazy RolloutManagement rolloutManagement,
                                                      RspRepository rspRepository,
                                                      TenantAware tenantAware,
                                                      ISnsServiceFactory snsServiceFactory,
                                                      EntityManager entityManager, S3MultipartFileUpload s3MultipartFileUpload,
                                                      S3Repository s3Repository, SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties,
                                                      RolloutTargetGroupRepository rolloutTargetGroupRepository, RolloutGroupRepository rolloutGroupRepository, TargetRepository targetRepository,
                                                      TargetManagement targetManagement, DdiSignatureService ddiSignatureService, PKIManagement pkiManagement, VirtualPropertyReplacer virtualPropertyReplacer, JpaProperties properties) {
        return new JpaSupportPackageManagement(espEcuRolloutRepository,
                rspRolloutRepository, espRepository,
                rolloutManagement, rspRepository,
                tenantAware, snsServiceFactory, entityManager, s3MultipartFileUpload, s3Repository, supportPackageUrlHandlerProperties, rolloutTargetGroupRepository, rolloutGroupRepository, targetRepository,
                targetManagement, ddiSignatureService, pkiManagement, virtualPropertyReplacer, properties.getDatabase());
    }

    @Bean
    @ConditionalOnMissingBean
    public EspFileRemovalService espFileRemovalService(final SupportPackageFileSystemProperties supportPackageFileSystemProperties,
                                                       final SupportPackageUrlHandlerProperties supportUrlHandlerProperties,
                                                       final TenantAware tenantAware,
                                                       final SnsServiceFactory snsServiceFactory,
                                                       final SystemManagement systemManagement,
                                                       final EspRepository espRepository) {
        return new EspFileRemovalService(supportPackageFileSystemProperties, supportUrlHandlerProperties,
                tenantAware, snsServiceFactory,
                systemManagement, espRepository);
    }


    @Bean
    @ConditionalOnMissingBean
    public DeviceRetryService deviceRetryService() {
        return new DeviceRetryService();
    }

    @Bean
    @ConditionalOnMissingBean
    public DdiSignatureService ddiSignatureService(RedisCacheService redisCacheService) {
        return new DdiSignatureService(redisCacheService);
    }

    @Bean
    public Database database(JpaProperties properties) {
        return properties.getDatabase();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheService redisCacheService(final RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RspFileRemovalService rspFileRemovalService(final SupportPackageFileSystemProperties supportPackageFileSystemProperties,
                                                       final SupportPackageUrlHandlerProperties supportUrlHandlerProperties,
                                                       final TenantAware tenantAware,
                                                       final SnsServiceFactory snsServiceFactory,
                                                       final SystemManagement systemManagement,
                                                       final RspRepository rspRepository) {
        return new RspFileRemovalService(supportPackageFileSystemProperties, supportUrlHandlerProperties,
                tenantAware, snsServiceFactory,
                systemManagement, rspRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileRemovalServiceFactory fileRemovalServiceFactory(final List<FileRemovalService> fileRemovalServices) {
        return new FileRemovalServiceFactory(fileRemovalServices);
    }

    @Bean
    @ConditionalOnMissingBean
    public ArtifactFilesystemProperties artifactFilesystemProperties() {
        return new ArtifactFilesystemProperties();
    }


    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}