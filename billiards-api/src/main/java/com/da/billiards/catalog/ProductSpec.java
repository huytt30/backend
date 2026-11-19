package com.da.billiards.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một dòng thông số kỹ thuật, ví dụ "Trọng lượng" - "19oz".
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpec {

    @Column(name = "spec_name", nullable = false, length = 100)
    private String name;

    @Column(name = "spec_value", nullable = false, length = 255)
    private String value;
}
