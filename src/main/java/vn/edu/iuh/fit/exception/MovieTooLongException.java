package vn.edu.iuh.fit.exception;

public class MovieTooLongException extends RuntimeException {
  public MovieTooLongException(String message) {
    super(message);
  }
}
