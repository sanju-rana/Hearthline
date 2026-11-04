package com.restaurant.service;

import com.restaurant.exception.BadRequestException;

public final class PasswordRules {

    private PasswordRules() {
    }

    public static void check(String password) {
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (password.length() < 8 || password.length() > 72 || !hasLetter || !hasDigit) {
            throw new BadRequestException("Password must be 8 to 72 characters and include a letter and a number.");
        }
    }
}
