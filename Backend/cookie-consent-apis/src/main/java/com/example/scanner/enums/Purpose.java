package com.example.scanner.enums;

public enum Purpose {
    Necessary("Necessary", "Essential cookies required for website functionality"),
    Functional("Functional", "Cookies for enhanced functionality and personalization"),
    Analytics("Analytics", "Cookies to understand how visitors use the website"),
    Advertisement("Advertisement", "Cookies for targeted advertising and marketing"),
    Others("Others", "Other types of cookies");

    private final String displayName;
    private final String description;

    Purpose(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}