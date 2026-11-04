package com.restaurant.service;

public final class PrepTimeEstimator {

    private PrepTimeEstimator() {
    }

    public static int estimate(int quantitySum, int baseMinutes, int perItemMinutes, int maxMinutes) {
        int estimate = baseMinutes + (perItemMinutes * quantitySum);
        return Math.min(maxMinutes, Math.max(baseMinutes, estimate));
    }
}
