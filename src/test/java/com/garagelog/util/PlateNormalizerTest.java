package com.garagelog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlateNormalizerTest {

    @Test
    void testNormalizesWhitespaceAndHyphens() {
        assertEquals("BN21LXR", PlateNormalizer.normalize("bn21 lxr"));
        assertEquals("BN21LXR", PlateNormalizer.normalize(" BN-21 - LXR "));
        assertEquals("W1234A", PlateNormalizer.normalize("w 1234 a"));
        assertEquals("", PlateNormalizer.normalize(null));
        assertEquals("", PlateNormalizer.normalize("   "));
    }
}
