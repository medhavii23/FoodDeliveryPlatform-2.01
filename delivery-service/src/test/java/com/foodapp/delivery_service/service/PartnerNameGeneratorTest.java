package com.foodapp.delivery_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerNameGeneratorTest {

    private PartnerNameGenerator partnerNameGenerator;

    @BeforeEach
    void setUp() {
        partnerNameGenerator = new PartnerNameGenerator();
    }

    @RepeatedTest(20)
    void randomPartnerName_returnsNameOrNull() {
        String name = partnerNameGenerator.randomPartnerName();
        if (name != null) {
            assertThat(name).contains(" ");
            assertThat(name.split(" ")).hasSize(2);
        }
    }

    @Test
    void randomPartnerName_eventuallyReturnsNonNull() {
        boolean foundNonNull = false;
        for (int i = 0; i < 30; i++) {
            if (partnerNameGenerator.randomPartnerName() != null) {
                foundNonNull = true;
                break;
            }
        }
        assertThat(foundNonNull).isTrue();
    }
}
