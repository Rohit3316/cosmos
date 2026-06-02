package services.factory;

import io.qameta.allure.Description;
import org.cosmos.models.sns.SnsPublishable;
import org.cosmos.sns.services.ISnsService;
import org.cosmos.sns.services.SnsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.cosmos.sns.services.SnsServiceType;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link SnsServiceFactory}.
 */
@ExtendWith(MockitoExtension.class)
class SnsServiceFactoryTest {

    @Mock
    private CdnUploadSnsService cdnUploadSnsService;

    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;

    @InjectMocks
    private SnsServiceFactory snsServiceFactory;

    @BeforeEach
    void setUp() {
        when(cdnUploadSnsService.getSnsServiceType()).thenReturn(SnsServiceType.CDN_UPLOAD);
        when(cdnDeleteSnsService.getSnsServiceType()).thenReturn(SnsServiceType.CDN_DELETE);

        List<SnsService<?>> snsServiceList = Arrays.asList(cdnUploadSnsService, cdnDeleteSnsService);
        snsServiceFactory = new SnsServiceFactory(snsServiceList);
        snsServiceFactory.init();
    }

    @Test
    @Description("Test for getting SNS service instance.")
    void givenValidTypeWhenGetInstanceThenReturnService() {
        ISnsService<SnsPublishable> service = snsServiceFactory.getInstance(SnsServiceType.CDN_UPLOAD);
        assertNotNull(service);
        assertEquals(cdnUploadSnsService, service);

        service = snsServiceFactory.getInstance(SnsServiceType.CDN_DELETE);
        assertNotNull(service);
        assertEquals(cdnDeleteSnsService, service);
    }

    @Description("Test for getting SNS service instance with invalid type.")
    @Test
    void givenInvalidValidTypeWhenGetInstanceThenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            snsServiceFactory.getInstance(null)
        );
        assertEquals("Unknown service type: null", exception.getMessage());
    }
}