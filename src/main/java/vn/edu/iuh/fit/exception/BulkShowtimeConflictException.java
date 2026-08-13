package vn.edu.iuh.fit.exception;

import lombok.Getter;
import vn.edu.iuh.fit.model.response.BulkShowtimeResponse;

@Getter
public class BulkShowtimeConflictException extends RuntimeException {
  private final BulkShowtimeResponse conflictDetails;

  public BulkShowtimeConflictException(String message, BulkShowtimeResponse conflictDetails) {
    super(message);
    this.conflictDetails = conflictDetails;
  }
}
