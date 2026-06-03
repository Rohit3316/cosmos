package com.stellantis.cosmos.sqs.app;

import org.cosmos.s3.ChecksumCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChecksumCalculatorTest {

    private final ChecksumCalculator checksumCalculator = new ChecksumCalculator();

    @Test
    @Description("Given a valid Base64-encoded MD5 hash, when converted to a hexadecimal string, then the correct hexadecimal value should be returned.")
    void givenValidBase64EncodedMD5HashWhenConvertedToHexStringThenCorrectHexadecimalValueReturned() {
        // Arrange
        String base64Md5 = "1B2M2Y8AsgTpgAmY7PhCfg=="; // Base64 for MD5 hash of an empty string

        // Act
        String hexResult = checksumCalculator.convertBase64ToHex(base64Md5);

        // Assert
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hexResult, "Hexadecimal MD5 hash should match expected value.");
    }

    @Test
    @Description("Given an invalid Base64 string, when converted to a hexadecimal string, then an IllegalArgumentException should be thrown.")
    void givenInvalidBase64StringWhenConvertedToHexStringThenIllegalArgumentExceptionThrown() {
        // Arrange
        String invalidBase64 = "InvalidBase64";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                checksumCalculator.convertBase64ToHex(invalidBase64), "Invalid Base64 input should throw IllegalArgumentException.");
    }

    @Test
    @Description("Given a null input, when converted to a hexadecimal string, then a NullPointerException should be thrown.")
    void givenNullInputWhenConvertedToHexStringThenNullPointerExceptionThrown() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            checksumCalculator.convertBase64ToHex(null);
        }, "Null input should throw NullPointerException.");
    }

}
