package org.eclipse.hawkbit.security.util;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for Jwt Decoder and validator util
 */
@Feature("Unit Tests - Jwt Decoder and Validator")
@Story("Jwt token decoder and validator")
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Test
    void decoderTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JwtUtil.decoder("", "test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JwtUtil.decoder("test", ""));
        Assertions.assertNotNull(JwtUtil.decoder("http://test", "test"));
    }

    @Test
    void validatorTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JwtUtil.validator(""));
        Assertions.assertNotNull(JwtUtil.validator("test"));
    }
}
