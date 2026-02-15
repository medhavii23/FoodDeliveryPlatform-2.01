package com.foodapp.delivery_service.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PartnerNameGenerator {

    private static final List<String> FIRST_NAMES = List.of(
            "Arjun", "Meera", "Karthik", "Ananya", "Vikram", "Priya",
            "Rahul", "Sneha", "Aditi", "Naveen", "Divya", "Sanjay",
            "Neha", "Rohan", "Pooja", "Suresh", "Kiran", "Ishita");

    private static final List<String> LAST_NAMES = List.of(
            "Kumar", "Nair", "Iyer", "Reddy", "Sharma", "Singh",
            "Patel", "Gupta", "Menon", "Joshi", "Bose", "Das",
            "Jain", "Verma", "Rao", "Chatterjee", "Chirayil");

    /**
     * Returns a random partner name ~90% of the time, null ~10% (simulates no partner available).
     */
    public String randomPartnerName() {
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
            return null;
        }
        String first = FIRST_NAMES.get(ThreadLocalRandom.current().nextInt(FIRST_NAMES.size()));
        String last = LAST_NAMES.get(ThreadLocalRandom.current().nextInt(LAST_NAMES.size()));
        return first + " " + last;
    }
}
