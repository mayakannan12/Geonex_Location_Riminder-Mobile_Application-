package com.example.geonex;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InputValidator {

    private final List<ValidationError> errors = new ArrayList<>();

    // Validation patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{10,13}$");

    private static final Pattern ALPHANUMERIC_PATTERN =
            Pattern.compile("^[a-zA-Z0-9 ]*$");

    public static class ValidationError {
        public final String field;
        public final String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }

    /**
     * Validate reminder title
     */
    public InputValidator validateTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            errors.add(new ValidationError("title", "Title is required"));
        } else if (title.length() < 3) {
            errors.add(new ValidationError("title", "Title must be at least 3 characters"));
        } else if (title.length() > 100) {
            errors.add(new ValidationError("title", "Title must be less than 100 characters"));
        } else if (!ALPHANUMERIC_PATTERN.matcher(title).matches()) {
            errors.add(new ValidationError("title", "Title can only contain letters, numbers and spaces"));
        }
        return this;
    }

    /**
     * Validate location name
     */
    public InputValidator validateLocation(String location) {
        if (TextUtils.isEmpty(location)) {
            errors.add(new ValidationError("location", "Location is required"));
        } else if (location.length() > 200) {
            errors.add(new ValidationError("location", "Location name too long"));
        }
        return this;
    }

    /**
     * Validate coordinates
     */
    public InputValidator validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            errors.add(new ValidationError("latitude", "Invalid latitude"));
        }
        if (longitude < -180 || longitude > 180) {
            errors.add(new ValidationError("longitude", "Invalid longitude"));
        }
        return this;
    }

    /**
     * Validate radius
     */
    public InputValidator validateRadius(float radius) {
        if (radius < 100) {
            errors.add(new ValidationError("radius", "Radius must be at least 100 meters"));
        } else if (radius > 5000) {
            errors.add(new ValidationError("radius", "Radius cannot exceed 5000 meters"));
        }
        return this;
    }

    /**
     * Validate email
     */
    public InputValidator validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            errors.add(new ValidationError("email", "Email is required"));
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(new ValidationError("email", "Invalid email format"));
        }
        return this;
    }

    /**
     * Validate phone number
     */
    public InputValidator validatePhone(String phone) {
        if (!TextUtils.isEmpty(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(new ValidationError("phone", "Invalid phone number format"));
        }
        return this;
    }

    /**
     * Check if validation passed
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Get first error message
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0).message;
    }

    /**
     * Get all errors
     */
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Clear errors
     */
    public void clear() {
        errors.clear();
    }
}