package vn.edu.iuh.fit.exception;

public class BadRequestException extends RuntimeException {
    private String code;

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
