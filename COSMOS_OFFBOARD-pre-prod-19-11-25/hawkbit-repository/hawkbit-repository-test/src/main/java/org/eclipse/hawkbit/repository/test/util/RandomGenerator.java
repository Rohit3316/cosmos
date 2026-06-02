package org.eclipse.hawkbit.repository.test.util;

import java.security.SecureRandom;
import java.util.Random;

public class RandomGenerator {

    private static final Random rand = new SecureRandom();

    private RandomGenerator(){}

    public static Random getRandom() {
        return rand;
    }
}
