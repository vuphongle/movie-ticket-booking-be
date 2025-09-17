package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponPreviewResponse {
    BigDecimal totalDiscount;
    List<DetailApplicationResult> detailResults;
    List<GiftItem> gifts;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DetailApplicationResult {
        Integer detailId;
        Boolean applied;
        String reason; // lý do áp/không áp
        BigDecimal lineDiscount;
        Integer affectedQuantity;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GiftItem {
        Integer serviceId;
        String serviceName;
        Integer quantity;
    }
}