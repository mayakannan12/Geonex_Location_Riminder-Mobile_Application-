package com.example.geonex;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Premium Feature Data Model
 * Used for onboarding feature slides
 * Immutable + Safe + Serializable
 */
public class FeatureData implements Serializable {

    private static final long serialVersionUID = 1L;

    @DrawableRes
    private final int iconResId;

    @StringRes
    private final int titleResId;

    @StringRes
    private final int descriptionResId;

    private final int[] bulletPoints;

    private final int backgroundColor;

    /**
     * Constructor
     */
    public FeatureData(@DrawableRes int iconResId,
                       @StringRes int titleResId,
                       @StringRes int descriptionResId,
                       int[] bulletPoints,
                       int backgroundColor) {

        // ===== VALIDATION =====
        if (iconResId == 0) {
            throw new IllegalArgumentException("Icon resource ID cannot be 0");
        }

        if (titleResId == 0) {
            throw new IllegalArgumentException("Title resource ID cannot be 0");
        }

        if (descriptionResId == 0) {
            throw new IllegalArgumentException("Description resource ID cannot be 0");
        }

        if (bulletPoints == null || bulletPoints.length == 0) {
            throw new IllegalArgumentException("Bullet points cannot be null or empty");
        }

        if (bulletPoints.length > 3) {
            throw new IllegalArgumentException("Maximum 3 bullet points allowed");
        }

        for (int i = 0; i < bulletPoints.length; i++) {
            if (bulletPoints[i] == 0) {
                throw new IllegalArgumentException(
                        "Invalid bullet point at index " + i
                );
            }
        }

        // ===== ASSIGNMENT =====
        this.iconResId = iconResId;
        this.titleResId = titleResId;
        this.descriptionResId = descriptionResId;
        this.bulletPoints = Arrays.copyOf(bulletPoints, bulletPoints.length);
        this.backgroundColor = backgroundColor;
    }

    // ===== GETTERS =====

    public int getIconResId() {
        return iconResId;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getDescriptionResId() {
        return descriptionResId;
    }

    public int[] getBulletPoints() {
        return Arrays.copyOf(bulletPoints, bulletPoints.length);
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    // ===== OPTIONAL: DEBUG SUPPORT =====
    @Override
    public String toString() {
        return "FeatureData{" +
                "iconResId=" + iconResId +
                ", titleResId=" + titleResId +
                ", descriptionResId=" + descriptionResId +
                ", bulletPoints=" + Arrays.toString(bulletPoints) +
                ", backgroundColor=" + backgroundColor +
                '}';
    }
}