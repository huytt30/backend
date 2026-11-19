package com.da.billiards.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SlugUtilTest {

    @Test
    void removesVietnameseDiacritics() {
        assertThat(SlugUtil.slugify("Cơ bida Đen Đỏ 19oz")).isEqualTo("co-bida-den-do-19oz");
        assertThat(SlugUtil.slugify("  Bàn bida lỗ -- 9 feet!  ")).isEqualTo("ban-bida-lo-9-feet");
    }

    @Test
    void appendsSuffixWhenSlugExists() {
        Set<String> existing = Set.of("phan-bida", "phan-bida-2");
        assertThat(SlugUtil.unique("Phấn bida", existing::contains)).isEqualTo("phan-bida-3");
    }

    @Test
    void fallsBackWhenNothingLeft() {
        assertThat(SlugUtil.unique("!!!", slug -> false)).isEqualTo("item");
    }
}
