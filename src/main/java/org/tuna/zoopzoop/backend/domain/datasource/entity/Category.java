package org.tuna.zoopzoop.backend.domain.datasource.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

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

    // JSON 문자열을 Category enum으로 변환
    @JsonCreator
    public static Category from(String input) {
        if (input == null || input.isBlank()) return null;
        if (input.equalsIgnoreCase("IT")) return IT; // IT 예외

        // 1) 한글 이름 매칭
        for (Category c : values()) {
            if (c.getName().equalsIgnoreCase(input)) return c;
        }
        // 2) 영문 코드 fallback (e.g., "SPORTS")
        try {
            return Category.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("유효하지 않은 카테고리 값입니다: " + input);
        }
    }
}
