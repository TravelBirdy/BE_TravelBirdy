package com.travelbird.common.util;

/**
 * MySQL LIKE 특수문자(\, %, _)를 이스케이프한다. QueryDSL {@code .contains()}는 자동
 * 이스케이프하지 않으므로 {@code .like(pattern, '\\')}와 함께 쓴다.
 */
public final class LikePatterns {

    private LikePatterns() {
    }

    public static String contains(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
