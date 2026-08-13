package vn.edu.iuh.fit.model.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponApplyResponse {
  Integer couponDetailId;
  String status; // "applied_draft", "failed"
  String idempotentToken;
  List<Integer> appliedDetailIds;
  CouponPreviewResponse previewResult;
  String errorMessage;
}
