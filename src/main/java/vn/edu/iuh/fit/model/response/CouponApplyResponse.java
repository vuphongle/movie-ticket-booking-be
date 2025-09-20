package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponApplyResponse {
    String status; // "applied_draft", "failed"
    String idempotentToken;
    List<Integer> appliedDetailIds;
    CouponPreviewResponse previewResult;
    String errorMessage;
}