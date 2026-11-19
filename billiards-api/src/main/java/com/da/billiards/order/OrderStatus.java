package com.da.billiards.order;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Luồng trạng thái hợp lệ: Chờ xác nhận → Đã xác nhận → Đang giao → Hoàn thành.
     * Chỉ được hủy khi đơn chưa giao.
     */
    public Set<OrderStatus> nextStatuses() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPING, CANCELLED);
            case SHIPPING -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return nextStatuses().contains(target);
    }
}
