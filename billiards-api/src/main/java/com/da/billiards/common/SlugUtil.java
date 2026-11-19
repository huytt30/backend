package com.da.billiards.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtil {

    private SlugUtil() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }

    /**
     * Sinh slug không trùng: nếu "co-bida" đã tồn tại thì thử "co-bida-2", "co-bida-3"...
     */
    public static String unique(String input, Predicate<String> exists) {
        String base = slugify(input);
        if (base.isEmpty()) {
            base = "item";
        }
        String candidate = base;
        int suffix = 2;
        while (exists.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
