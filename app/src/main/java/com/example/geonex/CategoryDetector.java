package com.example.geonex;

import java.util.HashMap;
import java.util.Map;

public class CategoryDetector {

    private static final Map<String, String> keywordCategoryMap = new HashMap<>();

    static {
        // Grocery keywords
        keywordCategoryMap.put("milk", "Grocery");
        keywordCategoryMap.put("bread", "Grocery");
        keywordCategoryMap.put("egg", "Grocery");
        keywordCategoryMap.put("eggs", "Grocery");
        keywordCategoryMap.put("grocery", "Grocery");
        keywordCategoryMap.put("vegetable", "Grocery");
        keywordCategoryMap.put("vegetables", "Grocery");
        keywordCategoryMap.put("fruit", "Grocery");
        keywordCategoryMap.put("fruits", "Grocery");
        keywordCategoryMap.put("rice", "Grocery");
        keywordCategoryMap.put("wheat", "Grocery");
        keywordCategoryMap.put("flour", "Grocery");
        keywordCategoryMap.put("oil", "Grocery");
        keywordCategoryMap.put("spice", "Grocery");
        keywordCategoryMap.put("spices", "Grocery");
        keywordCategoryMap.put("snack", "Grocery");
        keywordCategoryMap.put("snacks", "Grocery");
        keywordCategoryMap.put("drink", "Grocery");
        keywordCategoryMap.put("drinks", "Grocery");
        keywordCategoryMap.put("water", "Grocery");
        keywordCategoryMap.put("juice", "Grocery");
        keywordCategoryMap.put("soda", "Grocery");
        keywordCategoryMap.put("meat", "Grocery");
        keywordCategoryMap.put("chicken", "Grocery");
        keywordCategoryMap.put("fish", "Grocery");
        keywordCategoryMap.put("pork", "Grocery");
        keywordCategoryMap.put("beef", "Grocery");

        // Medicine keywords
        keywordCategoryMap.put("medicine", "Medicine");
        keywordCategoryMap.put("medicines", "Medicine");
        keywordCategoryMap.put("tablet", "Medicine");
        keywordCategoryMap.put("tablets", "Medicine");
        keywordCategoryMap.put("pill", "Medicine");
        keywordCategoryMap.put("pills", "Medicine");
        keywordCategoryMap.put("doctor", "Medicine");
        keywordCategoryMap.put("pharmacy", "Medicine");
        keywordCategoryMap.put("drug", "Medicine");
        keywordCategoryMap.put("drugs", "Medicine");
        keywordCategoryMap.put("prescription", "Medicine");
        keywordCategoryMap.put("vaccine", "Medicine");
        keywordCategoryMap.put("injection", "Medicine");
        keywordCategoryMap.put("syrup", "Medicine");
        keywordCategoryMap.put("dose", "Medicine");
        keywordCategoryMap.put("dosage", "Medicine");
        keywordCategoryMap.put("health", "Medicine");
        keywordCategoryMap.put("clinic", "Medicine");
        keywordCategoryMap.put("hospital", "Medicine");
        keywordCategoryMap.put("appointment", "Medicine");
        keywordCategoryMap.put("checkup", "Medicine");
        keywordCategoryMap.put("check-up", "Medicine");

        // Bills keywords
        keywordCategoryMap.put("bill", "Bills");
        keywordCategoryMap.put("bills", "Bills");
        keywordCategoryMap.put("payment", "Bills");
        keywordCategoryMap.put("pay", "Bills");
        keywordCategoryMap.put("electricity", "Bills");
        keywordCategoryMap.put("water", "Bills");
        keywordCategoryMap.put("gas", "Bills");
        keywordCategoryMap.put("rent", "Bills");
        keywordCategoryMap.put("internet", "Bills");
        keywordCategoryMap.put("wifi", "Bills");
        keywordCategoryMap.put("broadband", "Bills");
        keywordCategoryMap.put("phone", "Bills");
        keywordCategoryMap.put("mobile", "Bills");
        keywordCategoryMap.put("credit card", "Bills");
        keywordCategoryMap.put("loan", "Bills");
        keywordCategoryMap.put("emi", "Bills");
        keywordCategoryMap.put("tax", "Bills");
        keywordCategoryMap.put("insurance", "Bills");
        keywordCategoryMap.put("subscription", "Bills");
        keywordCategoryMap.put("recharge", "Bills");

        // Shopping keywords
        keywordCategoryMap.put("shop", "Shopping");
        keywordCategoryMap.put("shopping", "Shopping");
        keywordCategoryMap.put("buy", "Shopping");
        keywordCategoryMap.put("purchase", "Shopping");
        keywordCategoryMap.put("mall", "Shopping");
        keywordCategoryMap.put("store", "Shopping");
        keywordCategoryMap.put("clothes", "Shopping");
        keywordCategoryMap.put("dress", "Shopping");
        keywordCategoryMap.put("dresses", "Shopping");
        keywordCategoryMap.put("shirt", "Shopping");
        keywordCategoryMap.put("shirts", "Shopping");
        keywordCategoryMap.put("pant", "Shopping");
        keywordCategoryMap.put("pants", "Shopping");
        keywordCategoryMap.put("jeans", "Shopping");
        keywordCategoryMap.put("shoe", "Shopping");
        keywordCategoryMap.put("shoes", "Shopping");
        keywordCategoryMap.put("footwear", "Shopping");
        keywordCategoryMap.put("accessory", "Shopping");
        keywordCategoryMap.put("accessories", "Shopping");
        keywordCategoryMap.put("bag", "Shopping");
        keywordCategoryMap.put("watch", "Shopping");
        keywordCategoryMap.put("gift", "Shopping");
        keywordCategoryMap.put("present", "Shopping");
        keywordCategoryMap.put("electronics", "Shopping");
        keywordCategoryMap.put("gadget", "Shopping");
        keywordCategoryMap.put("mobile", "Shopping");
        keywordCategoryMap.put("phone", "Shopping");
        keywordCategoryMap.put("laptop", "Shopping");

        // Work keywords
        keywordCategoryMap.put("work", "Work");
        keywordCategoryMap.put("office", "Work");
        keywordCategoryMap.put("meeting", "Work");
        keywordCategoryMap.put("project", "Work");
        keywordCategoryMap.put("task", "Work");
        keywordCategoryMap.put("deadline", "Work");
        keywordCategoryMap.put("presentation", "Work");
        keywordCategoryMap.put("report", "Work");
        keywordCategoryMap.put("email", "Work");
        keywordCategoryMap.put("call", "Work");
        keywordCategoryMap.put("conference", "Work");
        keywordCategoryMap.put("interview", "Work");
        keywordCategoryMap.put("client", "Work");
        keywordCategoryMap.put("boss", "Work");
        keywordCategoryMap.put("colleague", "Work");
        keywordCategoryMap.put("team", "Work");
        keywordCategoryMap.put("meeting", "Work");
        keywordCategoryMap.put("appointment", "Work");
        keywordCategoryMap.put("schedule", "Work");
        keywordCategoryMap.put("deadline", "Work");
        keywordCategoryMap.put("submission", "Work");

        // Hospital keywords
        keywordCategoryMap.put("hospital", "Hospital");
        keywordCategoryMap.put("clinic", "Hospital");
        keywordCategoryMap.put("doctor", "Hospital");
        keywordCategoryMap.put("nurse", "Hospital");
        keywordCategoryMap.put("patient", "Hospital");
        keywordCategoryMap.put("ward", "Hospital");
        keywordCategoryMap.put("emergency", "Hospital");
        keywordCategoryMap.put("surgery", "Hospital");
        keywordCategoryMap.put("operation", "Hospital");
        keywordCategoryMap.put("admit", "Hospital");
        keywordCategoryMap.put("admission", "Hospital");
        keywordCategoryMap.put("discharge", "Hospital");
        keywordCategoryMap.put("icu", "Hospital");

        // Gym keywords
        keywordCategoryMap.put("gym", "Gym");
        keywordCategoryMap.put("workout", "Gym");
        keywordCategoryMap.put("exercise", "Gym");
        keywordCategoryMap.put("fitness", "Gym");
        keywordCategoryMap.put("trainer", "Gym");
        keywordCategoryMap.put("training", "Gym");
        keywordCategoryMap.put("muscle", "Gym");
        keywordCategoryMap.put("protein", "Gym");
        keywordCategoryMap.put("supplement", "Gym");
        keywordCategoryMap.put("yoga", "Gym");
        keywordCategoryMap.put("meditation", "Gym");
        keywordCategoryMap.put("cardio", "Gym");
        keywordCategoryMap.put("weights", "Gym");
        keywordCategoryMap.put("dumbbell", "Gym");

        // Restaurant keywords
        keywordCategoryMap.put("restaurant", "Restaurant");
        keywordCategoryMap.put("food", "Restaurant");
        keywordCategoryMap.put("dinner", "Restaurant");
        keywordCategoryMap.put("lunch", "Restaurant");
        keywordCategoryMap.put("breakfast", "Restaurant");
        keywordCategoryMap.put("meal", "Restaurant");
        keywordCategoryMap.put("eat", "Restaurant");
        keywordCategoryMap.put("cafe", "Restaurant");
        keywordCategoryMap.put("coffee", "Restaurant");
        keywordCategoryMap.put("tea", "Restaurant");
        keywordCategoryMap.put("snack", "Restaurant");
        keywordCategoryMap.put("pizza", "Restaurant");
        keywordCategoryMap.put("burger", "Restaurant");
        keywordCategoryMap.put("sandwich", "Restaurant");
        keywordCategoryMap.put("sushi", "Restaurant");
        keywordCategoryMap.put("chinese", "Restaurant");
        keywordCategoryMap.put("indian", "Restaurant");
        keywordCategoryMap.put("italian", "Restaurant");
        keywordCategoryMap.put("mexican", "Restaurant");
        keywordCategoryMap.put("thai", "Restaurant");

        // Petrol keywords
        keywordCategoryMap.put("petrol", "Petrol");
        keywordCategoryMap.put("gas", "Petrol");
        keywordCategoryMap.put("fuel", "Petrol");
        keywordCategoryMap.put("diesel", "Petrol");
        keywordCategoryMap.put("petrol pump", "Petrol");
        keywordCategoryMap.put("gas station", "Petrol");
        keywordCategoryMap.put("refuel", "Petrol");
        keywordCategoryMap.put("fill", "Petrol");
        keywordCategoryMap.put("car fuel", "Petrol");
        keywordCategoryMap.put("vehicle", "Petrol");

        // School keywords
        keywordCategoryMap.put("school", "School");
        keywordCategoryMap.put("college", "School");
        keywordCategoryMap.put("university", "School");
        keywordCategoryMap.put("class", "School");
        keywordCategoryMap.put("teacher", "School");
        keywordCategoryMap.put("student", "School");
        keywordCategoryMap.put("exam", "School");
        keywordCategoryMap.put("test", "School");
        keywordCategoryMap.put("assignment", "School");
        keywordCategoryMap.put("homework", "School");
        keywordCategoryMap.put("project", "School");
        keywordCategoryMap.put("grade", "School");
        keywordCategoryMap.put("semester", "School");
        keywordCategoryMap.put("lecture", "School");
        keywordCategoryMap.put("course", "School");

        // Pet Care keywords
        keywordCategoryMap.put("pet", "Pet Care");
        keywordCategoryMap.put("dog", "Pet Care");
        keywordCategoryMap.put("cat", "Pet Care");
        keywordCategoryMap.put("fish", "Pet Care");
        keywordCategoryMap.put("bird", "Pet Care");
        keywordCategoryMap.put("veterinary", "Pet Care");
        keywordCategoryMap.put("vet", "Pet Care");
        keywordCategoryMap.put("pet food", "Pet Care");
        keywordCategoryMap.put("dog food", "Pet Care");
        keywordCategoryMap.put("cat food", "Pet Care");
        keywordCategoryMap.put("grooming", "Pet Care");
        keywordCategoryMap.put("pet shop", "Pet Care");
        keywordCategoryMap.put("pet store", "Pet Care");

        // Movie keywords
        keywordCategoryMap.put("movie", "Movie");
        keywordCategoryMap.put("film", "Movie");
        keywordCategoryMap.put("cinema", "Movie");
        keywordCategoryMap.put("theater", "Movie");
        keywordCategoryMap.put("theatre", "Movie");
        keywordCategoryMap.put("watch", "Movie");
        keywordCategoryMap.put("ticket", "Movie");
        keywordCategoryMap.put("show", "Movie");
        keywordCategoryMap.put("premiere", "Movie");
        keywordCategoryMap.put("release", "Movie");
        keywordCategoryMap.put("bollywood", "Movie");
        keywordCategoryMap.put("hollywood", "Movie");
        keywordCategoryMap.put("tollywood", "Movie");
        keywordCategoryMap.put("kollywood", "Movie");
    }

    /**
     * Detect category based on title text
     * @param title The reminder title
     * @return Detected category or "Other" if no match
     */
    public static String detectCategory(String title) {
        if (title == null || title.isEmpty()) {
            return "Other";
        }

        String lowerTitle = title.toLowerCase();

        // Check for exact matches first (whole words)
        String[] words = lowerTitle.split("\\s+");
        for (String word : words) {
            String category = keywordCategoryMap.get(word);
            if (category != null) {
                return category;
            }
        }

        // Check for partial matches (if no exact word match)
        for (Map.Entry<String, String> entry : keywordCategoryMap.entrySet()) {
            if (lowerTitle.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "Other";
    }

    /**
     * Get confidence level of detection
     * @param title The reminder title
     * @return Confidence level (HIGH, MEDIUM, LOW, NONE)
     */
    public static ConfidenceLevel getConfidenceLevel(String title) {
        if (title == null || title.isEmpty()) {
            return ConfidenceLevel.NONE;
        }

        String lowerTitle = title.toLowerCase();
        int matchCount = 0;

        for (String keyword : keywordCategoryMap.keySet()) {
            if (lowerTitle.contains(keyword)) {
                matchCount++;
            }
        }

        if (matchCount >= 3) {
            return ConfidenceLevel.HIGH;
        } else if (matchCount == 2) {
            return ConfidenceLevel.MEDIUM;
        } else if (matchCount == 1) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.NONE;
        }
    }

    public enum ConfidenceLevel {
        HIGH, MEDIUM, LOW, NONE
    }

    /**
     * Get all available categories
     */
    public static String[] getAllCategories() {
        return new String[]{
                "Grocery", "Medicine", "Bills", "Shopping",
                "Hospital", "Office", "Gym", "Restaurant",
                "Petrol", "School", "Pet Care", "Movie", "Other"
        };
    }
}