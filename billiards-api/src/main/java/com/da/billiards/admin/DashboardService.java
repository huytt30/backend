package com.da.billiards.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.catalog.ProductRepository;
import com.da.billiards.catalog.ProductVariantRepository;
import com.da.billiards.order.Order;
import com.da.billiards.order.OrderRepository;
import com.da.billiards.order.OrderStatus;
import com.da.billiards.user.Role;
import com.da.billiards.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int REVENUE_DAYS = 7;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    public DashboardResponse overview() {
        LocalDate today = LocalDate.now();

        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : orderRepository.countGroupByStatus()) {
            byStatus.put((OrderStatus) row[0], (Long) row[1]);
        }

        return new DashboardResponse(
                orderRepository.sumTotalByStatus(OrderStatus.COMPLETED),
                orderRepository.countByCreatedAtGreaterThanEqual(today.atStartOfDay()),
                byStatus.get(OrderStatus.PENDING),
                userRepository.countByRole(Role.CUSTOMER),
                productRepository.countByActiveTrue(),
                byStatus,
                revenueByDay(today),
                variantRepository.findLowStock(LOW_STOCK_THRESHOLD).stream()
                        .map(v -> new LowStockItem(v.getId(), v.getProduct().getId(), v.getProduct().getName(),
                                v.getName(), v.getStock(), v.getProduct().getThumbnail()))
                        .toList());
    }

    /** Doanh thu tính theo đơn hoàn thành; số đơn tính mọi đơn được đặt trong ngày. */
    private List<DailyRevenue> revenueByDay(LocalDate today) {
        LocalDate firstDay = today.minusDays(REVENUE_DAYS - 1L);
        Map<LocalDate, Long> totals = new LinkedHashMap<>();
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < REVENUE_DAYS; i++) {
            totals.put(firstDay.plusDays(i), 0L);
            counts.put(firstDay.plusDays(i), 0L);
        }
        LocalDateTime from = firstDay.atStartOfDay();
        for (Order order : orderRepository.findByStatusAndCreatedAtGreaterThanEqual(OrderStatus.COMPLETED, from)) {
            totals.merge(order.getCreatedAt().toLocalDate(), order.getTotal(), Long::sum);
        }
        for (LocalDateTime createdAt : orderRepository.findCreatedAtSince(from)) {
            counts.merge(createdAt.toLocalDate(), 1L, Long::sum);
        }
        List<DailyRevenue> result = new ArrayList<>();
        totals.forEach((date, revenue) -> result.add(new DailyRevenue(date, revenue, counts.get(date))));
        return result;
    }

    public record DashboardResponse(long totalRevenue, long ordersToday, long pendingOrders, long totalCustomers,
                                    long totalProducts, Map<OrderStatus, Long> ordersByStatus,
                                    List<DailyRevenue> revenueByDay, List<LowStockItem> lowStock) {
    }

    public record DailyRevenue(LocalDate date, long revenue, long orders) {
    }

    public record LowStockItem(Long variantId, Long productId, String productName, String variantName, int stock,
                               String thumbnail) {
    }
}
