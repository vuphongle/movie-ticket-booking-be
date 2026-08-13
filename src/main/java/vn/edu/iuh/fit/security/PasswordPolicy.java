package vn.edu.iuh.fit.security;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.exception.BadRequestException;

@Component
public class PasswordPolicy {
  private static final Pattern PWD =
      Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$");

  public void validateOrThrow(String rawPassword) {
    if (rawPassword == null || !PWD.matcher(rawPassword).matches()) {
      throw new BadRequestException(
          "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt",
          "INVALID_PASSWORD_FORMAT");
    }
  }
}
