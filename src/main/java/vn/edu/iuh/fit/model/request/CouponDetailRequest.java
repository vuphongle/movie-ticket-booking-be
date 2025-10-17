package vn.edu.iuh.fit.model.request;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CouponDetailRequest {
    private Integer detailId;
    private String code;
    private Integer discount;
    private String type;
    private List<GiftItem> gifts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiftItem {
        private Integer serviceId;
        private String serviceName;
        private Integer quantity;
        private String thumbnail;
    }
}
