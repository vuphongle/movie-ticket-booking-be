package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.ConflictPolicy;
import vn.edu.iuh.fit.model.enums.GraphicsType;
import vn.edu.iuh.fit.model.enums.TranslationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkShowtimeRequest {
  @NotNull(message = "ID phim không được để trống")
  Integer movieId;

  @NotNull(message = "ID phòng chiếu không được để trống")
  Integer auditoriumId;

  @NotNull(message = "Hình thức chiếu không được để trống")
  GraphicsType graphicsType;

  @NotNull(message = "Hình thức dịch không được để trống")
  TranslationType translationType;

  @NotNull(message = "Ngày bắt đầu không được để trống")
  LocalDate dateFrom;

  @NotNull(message = "Ngày kết thúc không được để trống")
  LocalDate dateTo;

  @NotEmpty(message = "Thời gian bắt đầu không được để trống")
  @NotNull(message = "Thời gian bắt đầu không được để trống")
  String startTime;

  @NotEmpty(message = "Thời gian kết thúc không được để trống")
  @NotNull(message = "Thời gian kết thúc không được để trống")
  String endTime;

  // Các ngày trong tuần được chọn (1=Chủ nhật, 2=Thứ 2, ..., 7=Thứ 7)
  List<Integer> daysOfWeek;

  // Chính sách xử lý conflict: FAIL (dừng tất cả), SKIP (bỏ qua ngày trùng)
  ConflictPolicy conflictPolicy = ConflictPolicy.FAIL;
}
