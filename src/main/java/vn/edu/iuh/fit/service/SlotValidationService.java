package vn.edu.iuh.fit.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.exception.*;

@Slf4j
@Service
public class SlotValidationService {

  // 6 ca chiếu cố định theo spec FE
  private static final List<String> SLOT_START_TIMES =
      Arrays.asList("08:00", "10:30", "13:00", "15:30", "18:00", "20:30");

  private static final List<String> SLOT_END_TIMES =
      Arrays.asList("10:30", "13:00", "15:30", "18:00", "20:30", "23:00");

  private static final int DEFAULT_BUFFER_TIME = 20; // minutes
  private static final int SLOT_DURATION = 150; // minutes
  private static final int MAX_SLOT_SPANS = 2;

  /** Validate slot-based showtime creation */
  public void validateSlotBasedShowtime(String startTime, String endTime, int movieRuntimeMin) {
    log.info(
        "Validating slot-based showtime: start={}, end={}, runtime={}",
        startTime,
        endTime,
        movieRuntimeMin);

    // 1. Validate startTime matches slot boundaries
    int slotIndexStart = getSlotIndexByStartTime(startTime);
    if (slotIndexStart == -1) {
      throw new InvalidSlotException(
          "Giờ bắt đầu không khớp với ca chiếu hợp lệ. Vui lòng chọn: "
              + String.join(", ", SLOT_START_TIMES));
    }

    // 2. Calculate effective runtime and spans
    int effectiveRuntime = movieRuntimeMin + DEFAULT_BUFFER_TIME;
    int spans = (int) Math.ceil((double) effectiveRuntime / SLOT_DURATION);

    log.info(
        "Movie runtime: {}min, effective: {}min, spans: {}",
        movieRuntimeMin,
        effectiveRuntime,
        spans);

    // 3. Check if movie is too long
    if (spans > MAX_SLOT_SPANS) {
      throw new MovieTooLongException(
          "Phim quá dài, cần "
              + spans
              + " ca liên tiếp. Hệ thống chỉ hỗ trợ tối đa "
              + MAX_SLOT_SPANS
              + " ca.");
    }

    // 4. Check span overflow
    int endSlotIndex = slotIndexStart + spans - 1;
    if (endSlotIndex >= SLOT_END_TIMES.size()) {
      throw new SpanOverflowException(
          "Suất chiếu vượt quá khung giờ cuối ngày. Vui lòng chọn ca sớm hơn.");
    }

    // 5. Validate endTime matches expected slot boundary
    String expectedEndTime = SLOT_END_TIMES.get(endSlotIndex);
    if (!endTime.equals(expectedEndTime)) {
      throw new InvalidSlotException(
          "Giờ kết thúc không khớp với ca chiếu. Mong đợi: "
              + expectedEndTime
              + ", nhận được: "
              + endTime);
    }

    log.info(
        "Slot validation passed: slot {} to {} (spans={})",
        slotIndexStart + 1,
        endSlotIndex + 1,
        spans);
  }

  /** Get slot index by start time (0-based) */
  private int getSlotIndexByStartTime(String startTime) {
    return SLOT_START_TIMES.indexOf(startTime);
  }

  /** Check if two time ranges overlap for conflict detection */
  public boolean isTimeOverlap(String start1, String end1, String start2, String end2) {
    LocalTime s1 = LocalTime.parse(start1, DateTimeFormatter.ofPattern("HH:mm"));
    LocalTime e1 = LocalTime.parse(end1, DateTimeFormatter.ofPattern("HH:mm"));
    LocalTime s2 = LocalTime.parse(start2, DateTimeFormatter.ofPattern("HH:mm"));
    LocalTime e2 = LocalTime.parse(end2, DateTimeFormatter.ofPattern("HH:mm"));

    // Check if ranges overlap: (start1 < end2) && (start2 < end1)
    return s1.isBefore(e2) && s2.isBefore(e1);
  }

  /** Get occupied slots for logging/debugging */
  public String getOccupiedSlotsDescription(String startTime, String endTime) {
    int startSlot = getSlotIndexByStartTime(startTime) + 1;
    int endSlotIndex = SLOT_END_TIMES.indexOf(endTime);
    int endSlot = endSlotIndex + 1;

    if (startSlot == endSlot) {
      return "Ca " + startSlot;
    } else {
      return "Ca " + startSlot + " đến " + endSlot;
    }
  }
}
