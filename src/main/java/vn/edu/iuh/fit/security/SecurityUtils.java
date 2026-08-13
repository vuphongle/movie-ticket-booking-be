package vn.edu.iuh.fit.security;

import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.iuh.fit.entity.User;

@Slf4j
public class SecurityUtils {
  // Lấy thông tin user hiện tại
  public static Optional<User> getCurrentUserLoginOptional() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    if (securityContext == null) {
      return Optional.empty();
    }
    Authentication authentication = securityContext.getAuthentication();
    if (authentication == null) {
      return Optional.empty();
    }

    log.debug("Lấy thông tin user từ SecurityContext");
    log.debug("Authentication: {}", authentication.getPrincipal());

    if (authentication.getPrincipal() instanceof UserDetails userDetails
        && userDetails instanceof CustomUserDetails customUserDetails) {
      return Optional.ofNullable(customUserDetails.getUser());
    }

    return Optional.empty();
  }

  public static User getCurrentUserLogin() {
    return getCurrentUserLoginOptional()
        .orElseThrow(
            () -> new AuthenticationCredentialsNotFoundException("No authenticated user found"));
  }

  // Kiểm tra xem user đã đăng nhập chưa
  public static boolean isAuthenticated() {
    return getCurrentUserLoginOptional().filter(Objects::nonNull).isPresent();
  }
}
