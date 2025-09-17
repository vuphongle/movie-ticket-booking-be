package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.constant.ValidationMessages;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.SelectionStrategy;
import vn.edu.iuh.fit.model.enums.TargetType;
import vn.edu.iuh.fit.model.request.CouponApplyRequest;
import vn.edu.iuh.fit.model.request.CouponPreviewRequest;
import vn.edu.iuh.fit.model.response.CouponApplyResponse;
import vn.edu.iuh.fit.model.response.CouponPreviewResponse;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponPreviewService {
    private final CouponRepository couponRepository;
    private final CouponDetailRepository couponDetailRepository;

    public CouponPreviewResponse previewCoupon(Integer couponId, CouponPreviewRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Kiểm tra coupon có khả dụng không
        String validationMessage = validateCouponAvailability(coupon);
        if (validationMessage != null) {
            return CouponPreviewResponse.builder()
                    .totalDiscount(BigDecimal.ZERO)
                    .detailResults(Collections.emptyList())
                    .gifts(Collections.emptyList())
                    .build();
        }

        // Lấy các details đã enabled và sắp xếp theo priority
        List<CouponDetail> enabledDetails = couponDetailRepository
                .findByCouponIdAndEnabledTrueOrderByLinePriorityAsc(couponId);

        return calculateCouponApplication(enabledDetails, request);
    }

    @Transactional
    public CouponApplyResponse applyCoupon(CouponApplyRequest request) {
        Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Kiểm tra coupon có khả dụng không
        String validationMessage = validateCouponAvailability(coupon);
        if (validationMessage != null) {
            return CouponApplyResponse.builder()
                    .status("failed")
                    .errorMessage(validationMessage)
                    .build();
        }

        // Preview trước để xem kết quả
        CouponPreviewResponse previewResult = previewCoupon(coupon.getId(), request.getCart());
        
        // Tạo idempotent token
        String idempotentToken = generateIdempotentToken(request.getOrderId(), request.getCouponCode());
        
        // Lấy danh sách detail IDs đã được áp dụng
        List<Integer> appliedDetailIds = previewResult.getDetailResults().stream()
                .filter(CouponPreviewResponse.DetailApplicationResult::getApplied)
                .map(CouponPreviewResponse.DetailApplicationResult::getDetailId)
                .collect(Collectors.toList());

        return CouponApplyResponse.builder()
                .status("applied_draft")
                .idempotentToken(idempotentToken)
                .appliedDetailIds(appliedDetailIds)
                .previewResult(previewResult)
                .build();
    }

    private String validateCouponAvailability(Coupon coupon) {
        Date now = new Date();

        if (!coupon.getStatus()) {
            return ValidationMessages.COUPON_NOT_ACTIVE;
        }

        if (now.before(coupon.getStartDate())) {
            return ValidationMessages.COUPON_NOT_STARTED;
        }

        if (now.after(coupon.getEndDate())) {
            return ValidationMessages.COUPON_EXPIRED;
        }

        return null; // Valid
    }

    private CouponPreviewResponse calculateCouponApplication(List<CouponDetail> details, CouponPreviewRequest request) {
        List<CouponPreviewResponse.DetailApplicationResult> detailResults = new ArrayList<>();
        List<CouponPreviewResponse.GiftItem> gifts = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // Tính tổng tiền gốc của order
        BigDecimal originalOrderTotal = calculateOriginalOrderTotal(request);

        for (CouponDetail detail : details) {
            CouponPreviewResponse.DetailApplicationResult result = applyDetailToCart(detail, request, originalOrderTotal);
            detailResults.add(result);

            if (result.getApplied()) {
                totalDiscount = totalDiscount.add(result.getLineDiscount());
                
                // Thêm quà nếu là FREE_PRODUCT
                if (detail.getBenefitType() == BenefitType.FREE_PRODUCT) {
                    gifts.add(CouponPreviewResponse.GiftItem.builder()
                            .serviceId(detail.getGiftServiceId())
                            .serviceName("Service " + detail.getGiftServiceId()) // TODO: Get actual name
                            .quantity(detail.getGiftQuantity())
                            .build());
                }
            }
        }

        return CouponPreviewResponse.builder()
                .totalDiscount(totalDiscount)
                .detailResults(detailResults)
                .gifts(gifts)
                .build();
    }

    private CouponPreviewResponse.DetailApplicationResult applyDetailToCart(
            CouponDetail detail, CouponPreviewRequest request, BigDecimal originalOrderTotal) {
        
        // Kiểm tra điều kiện cơ bản
        if (detail.getDetailUsageLimit() != null && detail.getDetailUsageLimit() <= detail.getDetailUsedCount()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason(ValidationMessages.DETAIL_USAGE_EXCEEDED)
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        // Kiểm tra min order total
        if (detail.getMinOrderTotal() != null && originalOrderTotal.compareTo(detail.getMinOrderTotal()) < 0) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason(ValidationMessages.MIN_ORDER_TOTAL_NOT_MET)
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        // Áp dụng theo target type
        switch (detail.getTargetType()) {
            case ORDER:
                return applyToOrder(detail, request);
            case SEAT_TYPE:
                return applyToSeatType(detail, request);
            case SERVICE:
                return applyToService(detail, request);
            default:
                return CouponPreviewResponse.DetailApplicationResult.builder()
                        .detailId(detail.getId())
                        .applied(false)
                        .reason("Target type không hợp lệ")
                        .lineDiscount(BigDecimal.ZERO)
                        .affectedQuantity(0)
                        .build();
        }
    }

    private CouponPreviewResponse.DetailApplicationResult applyToOrder(CouponDetail detail, CouponPreviewRequest request) {
        // Tính tổng tiền của toàn bộ đơn hàng
        BigDecimal orderTotal = calculateOriginalOrderTotal(request);
        int totalQuantity = request.getTickets().stream()
                .mapToInt(CouponPreviewRequest.TicketItem::getQty)
                .sum() + 
                (request.getServices() != null ? request.getServices().stream()
                        .mapToInt(CouponPreviewRequest.ServiceItem::getQty)
                        .sum() : 0);

        if (detail.getMinQuantity() != null && totalQuantity < detail.getMinQuantity()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason("Không đạt số lượng tối thiểu")
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        BigDecimal discount = calculateDiscount(detail, orderTotal, totalQuantity);
        
        return CouponPreviewResponse.DetailApplicationResult.builder()
                .detailId(detail.getId())
                .applied(true)
                .reason("Áp dụng thành công cho toàn bộ đơn hàng")
                .lineDiscount(discount)
                .affectedQuantity(totalQuantity)
                .build();
    }

    private CouponPreviewResponse.DetailApplicationResult applyToSeatType(CouponDetail detail, CouponPreviewRequest request) {
        List<CouponPreviewRequest.TicketItem> matchingTickets;
        
        if (detail.getTargetRefId() == null) {
            // Áp dụng cho tất cả seat types
            matchingTickets = request.getTickets();
        } else {
            // Áp dụng cho seat type cụ thể
            matchingTickets = request.getTickets().stream()
                    .filter(ticket -> Objects.equals(ticket.getSeatTypeId(), detail.getTargetRefId()))
                    .collect(Collectors.toList());
        }

        if (matchingTickets.isEmpty()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason("Không có ghế phù hợp")
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        // Sắp xếp theo selection strategy
        matchingTickets = sortItemsByStrategy(matchingTickets, detail.getSelectionStrategy());
        
        int totalQty = matchingTickets.stream().mapToInt(CouponPreviewRequest.TicketItem::getQty).sum();
        BigDecimal totalValue = matchingTickets.stream()
                .map(ticket -> ticket.getUnitPrice().multiply(BigDecimal.valueOf(ticket.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (detail.getMinQuantity() != null && totalQty < detail.getMinQuantity()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason("Không đạt số lượng tối thiểu")
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        BigDecimal discount = calculateDiscount(detail, totalValue, totalQty);
        
        return CouponPreviewResponse.DetailApplicationResult.builder()
                .detailId(detail.getId())
                .applied(true)
                .reason("Áp dụng thành công cho loại ghế")
                .lineDiscount(discount)
                .affectedQuantity(totalQty)
                .build();
    }

    private CouponPreviewResponse.DetailApplicationResult applyToService(CouponDetail detail, CouponPreviewRequest request) {
        if (request.getServices() == null || request.getServices().isEmpty()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason("Không có dịch vụ trong đơn hàng")
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        List<CouponPreviewRequest.ServiceItem> matchingServices;
        
        if (detail.getTargetRefId() == null) {
            // Áp dụng cho tất cả services
            matchingServices = request.getServices();
        } else {
            // Áp dụng cho service cụ thể
            matchingServices = request.getServices().stream()
                    .filter(service -> Objects.equals(service.getServiceId(), detail.getTargetRefId()))
                    .collect(Collectors.toList());
        }

        if (matchingServices.isEmpty()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason("Không có dịch vụ phù hợp")
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        int totalQty = matchingServices.stream().mapToInt(CouponPreviewRequest.ServiceItem::getQty).sum();
        BigDecimal totalValue = matchingServices.stream()
                .map(service -> service.getUnitPrice().multiply(BigDecimal.valueOf(service.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (detail.getMinQuantity() != null && totalQty < detail.getMinQuantity()) {
            return CouponPreviewResponse.DetailApplicationResult.builder()
                    .detailId(detail.getId())
                    .applied(false)
                    .reason("Không đạt số lượng tối thiểu")
                    .lineDiscount(BigDecimal.ZERO)
                    .affectedQuantity(0)
                    .build();
        }

        BigDecimal discount = calculateDiscount(detail, totalValue, totalQty);
        
        return CouponPreviewResponse.DetailApplicationResult.builder()
                .detailId(detail.getId())
                .applied(true)
                .reason("Áp dụng thành công cho dịch vụ")
                .lineDiscount(discount)
                .affectedQuantity(totalQty)
                .build();
    }

    private BigDecimal calculateDiscount(CouponDetail detail, BigDecimal baseValue, int quantity) {
        BigDecimal discount = BigDecimal.ZERO;
        
        switch (detail.getBenefitType()) {
            case DISCOUNT_PERCENT:
                discount = baseValue.multiply(detail.getPercent()).divide(new BigDecimal("100"));
                break;
            case DISCOUNT_AMOUNT:
                discount = detail.getAmount();
                break;
            case FREE_PRODUCT:
                // Free product không tính discount tiền mặt
                discount = BigDecimal.ZERO;
                break;
        }

        // Áp dụng line max discount
        if (detail.getLineMaxDiscount() != null && discount.compareTo(detail.getLineMaxDiscount()) > 0) {
            discount = detail.getLineMaxDiscount();
        }

        // Áp dụng limit quantity
        if (detail.getLimitQuantityApplied() != null && quantity > detail.getLimitQuantityApplied()) {
            // Tính tỷ lệ giảm theo limit quantity
            BigDecimal ratio = BigDecimal.valueOf(detail.getLimitQuantityApplied()).divide(BigDecimal.valueOf(quantity));
            discount = discount.multiply(ratio);
        }

        return discount;
    }

    private BigDecimal calculateOriginalOrderTotal(CouponPreviewRequest request) {
        BigDecimal ticketTotal = request.getTickets().stream()
                .map(ticket -> ticket.getUnitPrice().multiply(BigDecimal.valueOf(ticket.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceTotal = BigDecimal.ZERO;
        if (request.getServices() != null) {
            serviceTotal = request.getServices().stream()
                    .map(service -> service.getUnitPrice().multiply(BigDecimal.valueOf(service.getQty())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return ticketTotal.add(serviceTotal);
    }

    private List<CouponPreviewRequest.TicketItem> sortItemsByStrategy(
            List<CouponPreviewRequest.TicketItem> items, SelectionStrategy strategy) {
        switch (strategy) {
            case HIGHEST_PRICE_FIRST:
                return items.stream()
                        .sorted(Comparator.comparing(CouponPreviewRequest.TicketItem::getUnitPrice).reversed())
                        .collect(Collectors.toList());
            case LOWEST_PRICE_FIRST:
                return items.stream()
                        .sorted(Comparator.comparing(CouponPreviewRequest.TicketItem::getUnitPrice))
                        .collect(Collectors.toList());
            case FIFO:
            default:
                return new ArrayList<>(items); // Giữ nguyên thứ tự
        }
    }

    private String generateIdempotentToken(Integer orderId, String couponCode) {
        return "COUPON_" + orderId + "_" + couponCode + "_" + System.currentTimeMillis();
    }
}