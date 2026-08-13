package vn.edu.iuh.fit.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.iuh.fit.exception.*;

public class SlotValidationServiceTest {

  private SlotValidationService slotValidationService;

  @BeforeEach
  void setUp() {
    slotValidationService = new SlotValidationService();
  }

  @Test
  void testValidSlotShowtime() {
    // Valid: Ca 1 (08:00-10:30) với phim 120 phút
    assertDoesNotThrow(
        () -> {
          slotValidationService.validateSlotBasedShowtime("08:00", "10:30", 120);
        });
  }

  @Test
  void testInvalidStartTime() {
    // Invalid: 08:15 không phải giờ bắt đầu ca hợp lệ
    InvalidSlotException exception =
        assertThrows(
            InvalidSlotException.class,
            () -> {
              slotValidationService.validateSlotBasedShowtime("08:15", "10:30", 120);
            });
    assertTrue(exception.getMessage().contains("không khớp với ca chiếu hợp lệ"));
  }

  @Test
  void testMovieTooLong() {
    // Movie 350 phút cần 3 ca (350+20=370min / 150min = 2.47 -> 3 ca)
    MovieTooLongException exception =
        assertThrows(
            MovieTooLongException.class,
            () -> {
              slotValidationService.validateSlotBasedShowtime("08:00", "15:30", 350);
            });
    assertTrue(exception.getMessage().contains("quá dài"));
  }

  @Test
  void testSpanOverflow() {
    // Ca 6 (20:30) với phim 280 phút cần 2 ca -> overflow
    SpanOverflowException exception =
        assertThrows(
            SpanOverflowException.class,
            () -> {
              slotValidationService.validateSlotBasedShowtime("20:30", "23:00", 280);
            });
    assertTrue(exception.getMessage().contains("vượt quá khung giờ"));
  }

  @Test
  void testTimeOverlap() {
    // Test overlap detection
    assertTrue(slotValidationService.isTimeOverlap("08:00", "10:30", "10:00", "12:30"));
    assertFalse(slotValidationService.isTimeOverlap("08:00", "10:30", "10:30", "13:00"));
  }

  @Test
  void testOccupiedSlotsDescription() {
    // Single slot
    assertEquals("Ca 1", slotValidationService.getOccupiedSlotsDescription("08:00", "10:30"));

    // Multiple slots
    assertEquals("Ca 1 đến 2", slotValidationService.getOccupiedSlotsDescription("08:00", "13:00"));
  }
}
