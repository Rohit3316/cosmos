/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Lists;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedVersionedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.jpa.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.constants.ArtifactsAuditStatus;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link SoftwareModuleManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaSoftwareModuleManagement implements SoftwareModuleManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaSoftwareModuleManagement.class);

    private final EntityManager entityManager;

    private final DistributionSetRepository distributionSetRepository;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;

    private final AuditorAware<String> auditorProvider;

    private final ArtifactsManagement artifactsManagement;

    private final QuotaManagement quotaManagement;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    private final EcuModelManagement ecuModelManagement;


    public JpaSoftwareModuleManagement(final EntityManager entityManager,
                                       final DistributionSetRepository distributionSetRepository,
                                       final SoftwareModuleRepository softwareModuleRepository,
                                       final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
                                       final SoftwareModuleTypeRepository softwareModuleTypeRepository, final AuditorAware<String> auditorProvider,
                                       final ArtifactsManagement artifactsManagement, final QuotaManagement quotaManagement,
                                       final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
                                       final EcuModelManagement ecuModelManagement) {
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.softwareModuleRepository = softwareModuleRepository;
        this.softwareModuleMetadataRepository = softwareModuleMetadataRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.auditorProvider = auditorProvider;
        this.artifactsManagement = artifactsManagement;
        this.quotaManagement = quotaManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.ecuModelManagement = ecuModelManagement;

    }

    private static Stream<JpaSoftwareModuleMetadataCreate> createJpaMetadataCreateStream(
            final Collection<SoftwareModuleMetadataCreate> create) {
        return create.stream().map(JpaSoftwareModuleMetadataCreate.class::cast);
    }

    private static Specification<JpaSoftwareModuleMetadata> bySmIdSpec(final long smId) {
        return (root, query, cb) -> cb
                .equal(root.get(JpaSoftwareModuleMetadata_.softwareModule).get(JpaSoftwareModule_.id), smId);
    }

    private static void throwMetadataKeyAlreadyExists(final String metadataKey) {
        throw new EntityAlreadyExistsException("Metadata entry with key '" + metadataKey + "' already exists");
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModule update(final SoftwareModuleUpdate u) {
        final GenericSoftwareModuleUpdate update = (GenericSoftwareModuleUpdate) u;

        final JpaSoftwareModule module = softwareModuleRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, update.getId()));

        update.getDescription().ifPresent(module::setDescription);
        update.getVendor().ifPresent(module::setVendor);

        return softwareModuleRepository.save(module);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModule create(final SoftwareModuleCreate c) {
        final JpaSoftwareModuleCreate create = (JpaSoftwareModuleCreate) c;
        final JpaSoftwareModule sm = softwareModuleRepository.save(create.build());
        if (create.isEncrypted()) {
            // flush sm creation in order to get an Id
            entityManager.flush();
            ArtifactEncryptionService.getInstance().addSoftwareModuleEncryptionSecrets(sm.getId());
        }
        return sm;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModule> create(final Collection<SoftwareModuleCreate> swModules) {
        return swModules.stream().map(this::create).toList();
    }

    @Override
    public Slice<SoftwareModule> findByType(final Pageable pageable, final long typeId) {
        throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

        final List<Specification<JpaSoftwareModule>> specList = Lists.newArrayListWithExpectedSize(2);

        specList.add(SoftwareModuleSpecification.equalType(typeId));
        specList.add(SoftwareModuleSpecification.isDeletedFalse());

        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, pageable, specList);
    }

    private void throwExceptionIfSoftwareModuleTypeDoesNotExist(final Long typeId) {
        if (!softwareModuleTypeRepository.existsById(typeId)) {
            throw new EntityNotFoundException(SoftwareModuleType.class, typeId);
        }
    }

    @Override
    public Optional<SoftwareModule> get(final long id) {
        return softwareModuleRepository.findById(id).map(SoftwareModule.class::cast);
    }

    @Override
    public Optional<SoftwareModule> getByNameAndVersionAndType(final String name, final String version,
                                                               final long typeId) {

        throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

        return softwareModuleRepository.findOneByNameAndVersionAndTypeId(name, version, typeId);
    }


    /**
     * Returns the software module with the given name.
     *
     * @param name the name of the software module
     * @return the software module with the given name, or an empty optional if not found
     */
    @Override
    public Optional<SoftwareModule> getByScomoName(String name) {
        LOG.debug("Getting software module by scomo name: {}", name);
        return softwareModuleRepository.findByName(name)
                .map(SoftwareModule.class::cast);
    }

    @Override
    public Optional<Long> getIdByScomoName(String name) {
        LOG.debug("Getting software module id by scomo name: {}", name);
        return softwareModuleRepository.findByName(name)
                .map(SoftwareModule::getId);
    }

    private boolean isUnassigned(final Long moduleId) {
        return distributionSetRepository.countByModulesId(moduleId) <= 0;
    }

    private void deleteGridFsArtifacts(final JpaSoftwareModule swModule) {

        List<Artifacts> artifactsList = swModule.getArtifactSoftwareModuleAssociations()
                .stream()
                .map(ArtifactSoftwareModuleAssociation::getArtifact)
                .collect(Collectors.toList());

        for (final Artifacts artifacts : artifactsList) {
            if(swModule.isDeleted()){
                artifactsManagement.deleteArtifactsById(artifacts.getId(), ArtifactsAuditStatus.DELETED);
            }
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaSoftwareModule> swModulesToDelete = softwareModuleRepository.findByIdIn(ids);

        if (swModulesToDelete.size() < ids.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, ids,
                    swModulesToDelete.stream().map(SoftwareModule::getId).toList());
        }

        final Set<Long> assignedModuleIds = new HashSet<>();
        swModulesToDelete.forEach(swModule -> {

            // delete binary data of artifacts
            deleteGridFsArtifacts(swModule);

            if (isUnassigned(swModule.getId())) {
                softwareModuleRepository.deleteById(swModule.getId());
            } else {
                assignedModuleIds.add(swModule.getId());
            }
        });

        if (!assignedModuleIds.isEmpty()) {
            String currentUser = null;
            currentUser = auditorProvider.getCurrentAuditor().orElse(null);
            softwareModuleRepository.deleteSoftwareModule(Instant.now().getEpochSecond(), currentUser,
                    assignedModuleIds.toArray(new Long[0]));
        }
    }

    @Override
    public Slice<SoftwareModule> findAll(final Pageable pageable) {
        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(2);
        specList.add(SoftwareModuleSpecification.isDeletedFalse());
        specList.add(SoftwareModuleSpecification.fetchType());

        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, pageable, specList);
    }

    @Override
    public long count() {
        final Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();

        return JpaManagementHelper.countBySpec(softwareModuleRepository, Collections.singletonList(spec));
    }

    @Override
    public Page<SoftwareModule> findByRsql(final Pageable pageable, final String rsqlParam) {
        final List<Specification<JpaSoftwareModule>> specList = Lists.newArrayListWithExpectedSize(2);
        specList.add(RSQLUtility.buildRsqlSpecification(rsqlParam, SoftwareModuleFields.class, virtualPropertyReplacer,
                database));
        specList.add(SoftwareModuleSpecification.isDeletedFalse());

        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleRepository, pageable, specList);
    }

    @Override
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<SoftwareModule> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleRepository.findByIdIn(ids));
    }

    @Override
    public Slice<SoftwareModule> findByTextAndType(final Pageable pageable, final String searchText,
                                                   final Long typeId) {
        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(4);
        specList.add(SoftwareModuleSpecification.isDeletedFalse());

        if (!StringUtils.isEmpty(searchText)) {
            specList.add(buildSmSearchQuerySpec(searchText));
        }

        if (null != typeId) {
            throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);
            specList.add(SoftwareModuleSpecification.equalType(typeId));
        }

        specList.add(SoftwareModuleSpecification.fetchType());

        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, pageable, specList);
    }

    private Specification<JpaSoftwareModule> buildSmSearchQuerySpec(final String searchText) {
        final String[] smFilterNameAndVersionEntries = JpaManagementHelper
                .getFilterNameAndVersionEntries(searchText.trim());
        return SoftwareModuleSpecification.likeNameAndVersion(smFilterNameAndVersionEntries[0],
                smFilterNameAndVersionEntries[1]);
    }

    @Override
    // In the interface org.springframework.data.domain.Pageable.getSort the
    // return value is not guaranteed to be non-null, therefore a null check is
    // necessary otherwise we rely on the implementation but this could change.
    @SuppressWarnings({"squid:S2583", "squid:S2589"})
    public Slice<AssignedSoftwareModule> findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
            final Pageable pageable, final long dsId, final String searchText, final Long smTypeId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Tuple> query = cb.createTupleQuery();
        final Root<JpaSoftwareModule> smRoot = query.from(JpaSoftwareModule.class);

        final ListJoin<JpaSoftwareModule, DistributionSetModule> assignedDsList = smRoot
                .join(JpaSoftwareModule_.assignedTo, JoinType.LEFT);
        final Join<DistributionSetModule, JpaDistributionSet> dsSlist = assignedDsList
                .join(DistributionSetModule_.dsSet, JoinType.LEFT);

        final Expression<Integer> assignedCaseMax = cb.max(
                cb.selectCase()
                        .when(cb.equal(dsSlist.get(JpaDistributionSet_.id), dsId), 1)
                        .otherwise(0)
                        .as(Integer.class)
        );

        query.multiselect(smRoot.alias("sm"), assignedCaseMax.alias("assigned"));

        final Predicate[] specPredicate = specificationsToPredicate(buildSpecificationList(searchText, smTypeId),
                smRoot, query, cb);

        if (specPredicate.length > 0) {
            query.where(specPredicate);
        }

        query.groupBy(smRoot);

        final Sort sort = pageable.getSort();
        final List<Order> orders = new ArrayList<>();
        orders.add(cb.desc(assignedCaseMax));
        if (sort.isEmpty()) {
            orders.add(cb.asc(smRoot.get(AbstractJpaNamedEntity_.name)));
            orders.add(cb.asc(smRoot.get(AbstractJpaNamedVersionedEntity_.version)));
        } else {
            orders.addAll(QueryUtils.toOrders(sort, smRoot, cb));
        }
        query.orderBy(orders);

        final int pageSize = pageable.getPageSize();
        final List<Tuple> smWithAssignedFlagList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageSize).getResultList();
        final boolean hasNext = smWithAssignedFlagList.size() > pageSize;

        final List<AssignedSoftwareModule> resultList = new ArrayList<>();

        smWithAssignedFlagList.forEach(smWithAssignedFlag -> resultList
                .add(new AssignedSoftwareModule(smWithAssignedFlag.get("sm", JpaSoftwareModule.class),
                        smWithAssignedFlag.get("assigned", Number.class).longValue() == 1)));

        return new SliceImpl<>(Collections.unmodifiableList(resultList), pageable, hasNext);
    }

    private List<Specification<JpaSoftwareModule>> buildSpecificationList(final String searchText, final Long typeId) {
        final List<Specification<JpaSoftwareModule>> specList = Lists.newArrayListWithExpectedSize(3);
        if (!StringUtils.isEmpty(searchText)) {
            specList.add(buildSmSearchQuerySpec(searchText));
        }

        if (typeId != null) {
            throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

            specList.add(SoftwareModuleSpecification.equalType(typeId));
        }
        specList.add(SoftwareModuleSpecification.isDeletedFalse());
        return specList;
    }

    private Predicate[] specificationsToPredicate(final List<Specification<JpaSoftwareModule>> specifications,
                                                  final Root<JpaSoftwareModule> root, final CriteriaQuery<?> query, final CriteriaBuilder cb,
                                                  final Predicate... additionalPredicates) {

        return Stream.concat(specifications.stream().map(spec -> spec.toPredicate(root, query, cb)),
                Arrays.stream(additionalPredicates)).toArray(Predicate[]::new);
    }

    @Override
    public long countByTextAndType(final String searchText, final Long typeId) {
        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(3);

        Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        if (!StringUtils.isEmpty(searchText)) {
            specList.add(buildSmSearchQuerySpec(searchText));
        }

        if (null != typeId) {
            throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

            spec = SoftwareModuleSpecification.equalType(typeId);
            specList.add(spec);
        }

        return JpaManagementHelper.countBySpec(softwareModuleRepository, specList);
    }

    @Override
    public Page<SoftwareModule> findByAssignedTo(final Pageable pageable, final long setId) {
        if (!distributionSetRepository.existsById(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return softwareModuleRepository.findByAssignedToId(pageable, setId);
    }

    @Override
    public long countByAssignedTo(final long setId) {
        if (!distributionSetRepository.existsById(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return softwareModuleRepository.countByAssignedToId(setId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata createMetaData(final SoftwareModuleMetadataCreate c) {

        final JpaSoftwareModuleMetadataCreate create = (JpaSoftwareModuleMetadataCreate) c;
        final Long moduleId = create.getSoftwareModuleId();
        assertSoftwareModuleExists(moduleId);
        assertMetaDataQuota(moduleId, 1);

        return saveMetadata(create);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleMetadata> createMetaData(final Collection<SoftwareModuleMetadataCreate> create) {

        if (!create.isEmpty()) {

            // check if all meta data entries refer to the same software module
            final Long moduleId = ((JpaSoftwareModuleMetadataCreate) create.iterator().next()).getSoftwareModuleId();
            if (createJpaMetadataCreateStream(create).allMatch(c -> moduleId.equals(c.getSoftwareModuleId()))) {

                assertSoftwareModuleExists(moduleId);
                assertMetaDataQuota(moduleId, create.size());

                return createJpaMetadataCreateStream(create).map(this::saveMetadata).toList();

            } else {

                // group by software module id to minimize database access
                final Map<Long, List<JpaSoftwareModuleMetadataCreate>> groups = createJpaMetadataCreateStream(create)
                        .collect(Collectors.groupingBy(JpaSoftwareModuleMetadataCreate::getSoftwareModuleId));
                return groups.entrySet().stream().flatMap(e -> {

                    final Long id = e.getKey();
                    final List<JpaSoftwareModuleMetadataCreate> group = e.getValue();

                    assertSoftwareModuleExists(id);
                    assertMetaDataQuota(id, group.size());

                    return group.stream().map(this::saveMetadata);
                }).toList();
            }
        }

        return Collections.emptyList();
    }

    private SoftwareModuleMetadata saveMetadata(final JpaSoftwareModuleMetadataCreate create) {
        assertSoftwareModuleMetadataDoesNotExist(create.getSoftwareModuleId(), create);
        return softwareModuleMetadataRepository.save(create.build());
    }

    private void assertSoftwareModuleMetadataDoesNotExist(final Long moduleId,
                                                          final JpaSoftwareModuleMetadataCreate md) {
        if (softwareModuleMetadataRepository.existsById(new SwMetadataCompositeKey(moduleId, md.getKey()))) {
            throwMetadataKeyAlreadyExists(md.getKey());
        }
    }

    private void assertSoftwareModuleExists(final Long moduleId) {
        JpaManagementHelper.touch(entityManager, softwareModuleRepository, (JpaSoftwareModule) get(moduleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId)));
    }

    /**
     * Asserts the meta data quota for the software module with the given ID.
     *
     * @param moduleId  The software module ID.
     * @param requested Number of meta data entries to be created.
     */
    private void assertMetaDataQuota(final Long moduleId, final int requested) {
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        QuotaHelper.assertAssignmentQuota(moduleId, requested, maxMetaData, SoftwareModuleMetadata.class,
                SoftwareModule.class, softwareModuleMetadataRepository::countBySoftwareModuleId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetaData(final SoftwareModuleMetadataUpdate u) {
        final GenericSoftwareModuleMetadataUpdate update = (GenericSoftwareModuleMetadataUpdate) u;

        // check if exists otherwise throw entity not found exception
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) getMetaDataBySoftwareModuleId(
                update.getSoftwareModuleId(), update.getKey())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class,
                        update.getSoftwareModuleId(), update.getKey()));

        update.getValue().ifPresent(metadata::setValue);
        update.isTargetVisible().ifPresent(metadata::setTargetVisible);

        JpaManagementHelper.touch(entityManager, softwareModuleRepository,
                (JpaSoftwareModule) metadata.getSoftwareModule());
        return softwareModuleMetadataRepository.save(metadata);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long moduleId, final String key) {
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) getMetaDataBySoftwareModuleId(moduleId,
                key).orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class, moduleId, key));

        JpaManagementHelper.touch(entityManager, softwareModuleRepository,
                (JpaSoftwareModule) metadata.getSoftwareModule());
        softwareModuleMetadataRepository.deleteById(metadata.getId());
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long swId) {
        if (!softwareModuleRepository.existsById(swId)) {
            throw new EntityNotFoundException(SoftwareModule.class, swId);
        }
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataByRsql(final Pageable pageable, final long softwareModuleId,
                                                           final String rsqlParam) {
        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        final List<Specification<JpaSoftwareModuleMetadata>> specList = Arrays
                .asList(RSQLUtility.buildRsqlSpecification(rsqlParam, SoftwareModuleMetadataFields.class,
                        virtualPropertyReplacer, database), bySmIdSpec(softwareModuleId));
        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleMetadataRepository, pageable, specList);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleId(final Pageable pageable, final long swId) {
        throwExceptionIfSoftwareModuleDoesNotExist(swId);

        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleMetadataRepository, pageable,
                Collections.singletonList(bySmIdSpec(swId)));
    }

    @Override
    public long countMetaDataBySoftwareModuleId(final long moduleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        return softwareModuleMetadataRepository.countBySoftwareModuleId(moduleId);
    }

    @Override
    public Optional<SoftwareModuleMetadata> getMetaDataBySoftwareModuleId(final long moduleId, final String key) {
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        return softwareModuleMetadataRepository.findById(new SwMetadataCompositeKey(moduleId, key))
                .map(SoftwareModuleMetadata.class::cast);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long moduleId) {
        delete(List.of(moduleId));
    }

    @Override
    public boolean exists(final long id) {
        return softwareModuleRepository.existsById(id);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(final Pageable pageable,
                                                                                       final long moduleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        return JpaManagementHelper.convertPage(softwareModuleMetadataRepository.findBySoftwareModuleIdAndTargetVisible(
                PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT), moduleId, true), pageable);
    }

    @Override
    public void assignEcuModel(Long softwareModuleId, List<Long> ecuModelIds) {
        final SoftwareModule softwareModule = get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        assignEcuModel(softwareModule, ecuModelIds);
    }

    @Override
    public void assignEcuModel(SoftwareModule softwareModule, List<Long> ecuModelIds) {

        JpaSoftwareModule jpaSoftwareModule = (JpaSoftwareModule) softwareModule;

        List<EcuModel> ecuModelsList = new ArrayList<>();
        try {
            ecuModelsList = ecuModelManagement.getEcuModelByIdsAndThrowIfNotFound(ecuModelIds);
        } catch (Exception e) {
            throw new ValidationException("One more ecuModelId/s does not exists");
        }
        Set<EcuModel> eculModelSet = new HashSet<>(ecuModelsList);
        eculModelSet.forEach(jpaSoftwareModule::addSoftwareEcu);
        softwareModuleRepository.save(jpaSoftwareModule);
    }

    @Override
    public void removeAssociatedEcuModels(Long softwareModuleId, List<Long> ecuModelIds) {
        final JpaSoftwareModule softwareModule = (JpaSoftwareModule) get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        removeAssociatedEcuModels(softwareModule, ecuModelIds);
    }

    @Override
    public void removeAssociatedEcuModels(SoftwareModule softwareModule, List<Long> ecuModelIds) {
        final JpaSoftwareModule jpaSoftwareModule = (JpaSoftwareModule) softwareModule;
        validateModuleNotLinkedToDistributionSetOrThrow(jpaSoftwareModule.getId());
        ecuModelManagement.getEcuModelByIdsAndThrowIfNotFound(ecuModelIds);
        Set<EcuModel> eculModelSet = jpaSoftwareModule.getSoftwareEcuModels().stream().filter(e ->
                !ecuModelIds.contains(e.getId())).collect(Collectors.toSet());
        jpaSoftwareModule.setSoftwareEcuModels(eculModelSet);
        LOG.debug("ECU Models association removed successfully from software Module {}", jpaSoftwareModule);
        softwareModuleRepository.save(jpaSoftwareModule);
    }

    @Override
    public List<DistributionSet> getDistributionSetByModuleId(final Long moduleId) {
        List<DistributionSet> distributionSetByModules = distributionSetRepository.getDistributionSetByModulesId(moduleId);
        if (!distributionSetByModules.isEmpty()) {
            return distributionSetByModules;
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the given software module is part of any distribution set,
     * and throws a ValidationException if it is, to prevent unlinking.
     *
     * @param softwareModuleId ID of the software module to check
     * @throws ValidationException if the software module is linked to a distribution set
     */
    private void validateModuleNotLinkedToDistributionSetOrThrow(Long softwareModuleId) {
        List<DistributionSet> distributionSets = getDistributionSetByModuleId(softwareModuleId);
        if (!distributionSets.isEmpty()) {
            throw new ValidationException("Cannot unlink ecu model ID because the software module is associated with a distribution set.");
        }
    }

    /**
     * Retrieves a {@link SoftwareModule} entity by its ID.
     *
     * @param softwareModuleId the ID of the software module entity to retrieve
     * @return an {@link Optional} containing the found {@link SoftwareModule} entity, or {@link Optional#empty()} if not found
     */
    @Override
    public Optional<SoftwareModule> getSoftwareModuleById(long softwareModuleId) {
        return softwareModuleRepository.getSoftwareModuleById(softwareModuleId);
    }

    public void checkSoftwareModuleLinkWithDistributionSet(long softwareModuleId){
        List<DistributionSet> distributionSets = distributionSetRepository.getDistributionSetByModulesId(softwareModuleId);
        if(!distributionSets.isEmpty()){
            throw new ValidationException((String.format("Software Module with id %d cannot be deleted due to its association with distribution set(s)", softwareModuleId)));
        }
    }

    /**
     * Checks if a software module with the given name already exists for the given tenant.
     *
     * @param name   the name of the software module
     * @return true if a software module with the given name exists for the tenant, false otherwise
     */
    @Override
    public boolean existsByName(String name) {
        return softwareModuleRepository.existsByName(name);
    }

}
