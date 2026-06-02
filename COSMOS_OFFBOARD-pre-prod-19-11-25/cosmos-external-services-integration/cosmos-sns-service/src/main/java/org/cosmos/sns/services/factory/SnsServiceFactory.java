package org.cosmos.sns.services.factory;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sns.SnsPublishable;
import org.cosmos.sns.services.ISnsService;
import org.cosmos.sns.services.ISnsServiceFactory;
import org.cosmos.sns.services.SnsService;
import org.cosmos.sns.services.SnsServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating SNS services.
 */
@Component
@Slf4j
public final class SnsServiceFactory implements ISnsServiceFactory {

    @Autowired
    private List<SnsService<?>> snsServiceList;
    private static final Map<SnsServiceType, SnsService<?>> serviceMap = new EnumMap<>(SnsServiceType.class);

    private SnsServiceFactory() {}

    public SnsServiceFactory(List<SnsService<?>> snsServiceList) {
        this.snsServiceList = snsServiceList;
    }

    @PostConstruct
    public void init() {
        snsServiceList.forEach(snsService -> serviceMap.put(snsService.getSnsServiceType(), snsService));
    }

    @Override
    public <T extends SnsPublishable> ISnsService<T> getInstance(SnsServiceType type) {
        SnsService<?> service = serviceMap.get(type);
        if (service == null) {
            log.error("Unknown service type: {}", type);
            throw new IllegalArgumentException("Unknown service type: " + type);
        }
        return (ISnsService<T>) service;
    }
}