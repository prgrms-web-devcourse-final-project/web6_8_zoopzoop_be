package org.tuna.zoopzoop.backend.domain.datasource.entity;

public enum Category {
    POLITICS("정치"),
    ECONOMY("경제"),
    SOCIETY("사회"),
    IT("IT"),
    SCIENCE("과학"),
    CULTURE("문화"),
    SPORTS("스포츠"),
    ENVIRONMENT("환경"),
    HISTORY("역사"),
    WORLD("세계");

    private final String name;

    Category (String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isBlank() {
        return this.name == null || this.name.isBlank();
    }

    public String toUpperCase() {
        return this.name.toUpperCase();
    }
}
