package vn.edu.iuh.fit.model.response;

import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.entity.Showtime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkShowtimeResponse {
  // Kết quả tổng quan
  int totalRequested; // Tổng số ngày được yêu cầu tạo
  int successfullyCreated; // Số suất chiếu đã tạo thành công
  int conflictsDetected; // Số ngày bị conflict
  int skipped; // Số ngày bị bỏ qua

  // Chi tiết các suất chiếu đã tạo
  List<Showtime> createdShowtimes;

  // Chi tiết các ngày bị conflict
  List<ConflictDetail> conflicts;

  // Các ngày bị bỏ qua
  List<LocalDate> skippedDates;

  // Thông báo kết quả
  String message;

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class ConflictDetail {
    LocalDate date;
    String conflictMovie;
    String conflictTimeRange;
    String reason;
  }
}
