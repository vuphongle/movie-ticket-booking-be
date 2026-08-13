package vn.edu.iuh.fit.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.TokenConfirm;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.model.dto.UserDto;
import vn.edu.iuh.fit.model.enums.TokenType;
import vn.edu.iuh.fit.model.enums.UserRole;
import vn.edu.iuh.fit.model.mapper.UserMapper;
import vn.edu.iuh.fit.model.request.LoginRequest;
import vn.edu.iuh.fit.model.request.RegisterRequest;
import vn.edu.iuh.fit.model.request.ResetPasswordRequest;
import vn.edu.iuh.fit.model.response.AuthResponse;
import vn.edu.iuh.fit.model.response.VerifyTokenResponse;
import vn.edu.iuh.fit.repository.TokenConfirmRepository;
import vn.edu.iuh.fit.repository.UserRepository;
import vn.edu.iuh.fit.security.JwtUtils;
import vn.edu.iuh.fit.security.PasswordPolicy;
import vn.edu.iuh.fit.utils.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenConfirmRepository tokenConfirmRepository;
  private final UserMapper userMapper;
  private final JwtUtils jwtUtils;
  private final MailService mailService;
  private final PasswordPolicy passwordPolicy;

  public AuthResponse login(LoginRequest request) throws AuthenticationException {
    final String email = normalizeEmail(request.getEmail());

    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(email, request.getPassword());

    Authentication authentication = authenticationManager.authenticate(token);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String tokenJwt = jwtUtils.generateToken(userDetails);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Không tìm thấy user có email = " + email, "USER_NOT_FOUND"));

    UserDto userDto = userMapper.toUserDto(user);

    return AuthResponse.builder()
        .user(userDto)
        .accessToken(tokenJwt)
        .refreshToken(null)
        .isAuthenticated(true)
        .build();
  }

  public void register(RegisterRequest request) {
    final String email = normalizeEmail(request.getEmail());

    userRepository
        .findByEmail(email)
        .ifPresent(
            existing -> {
              if (!existing.getEnabled()) {
                resendVerificationIfNeeded(existing);
                throw new BadRequestException(
                    "Email đã tồn tại nhưng chưa kích hoạt", "ACCOUNT_NOT_ACTIVATED");
              }
              throw new BadRequestException("Email đã tồn tại", "EMAIL_ALREADY_EXISTS");
            });

    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException("Mật khẩu không khớp", "PASSWORD_MISMATCH");
    }

    passwordPolicy.validateOrThrow(request.getPassword());

    if (request.getDob() != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.YEAR, -12);
      Date twelveYearsAgo = calendar.getTime();
      if (request.getDob().after(twelveYearsAgo)) {
        throw new BadRequestException(
            "Bạn phải lớn hơn 12 tuổi để đăng ký tài khoản", "AGE_RESTRICTION");
      }
    } else {
      throw new BadRequestException("Ngày sinh không được để trống", "DOB_REQUIRED");
    }

    User user = new User();
    user.setName(request.getName());
    user.setEmail(email);
    user.setPhone(request.getPhone());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(UserRole.USER);
    user.setAvatar(StringUtils.generateLinkImage(request.getName()));
    user.setEnabled(false);
    user.setDob(request.getDob());
    userRepository.save(user);

    sendVerificationEmail(user);
  }

  @Transactional
  public VerifyTokenResponse checkRegisterToken(String token) {
    VerifyTokenResponse response =
        VerifyTokenResponse.builder()
            .token(token)
            .success(true)
            .message("Xác thực tài khoản thành công")
            .build();

    Optional<TokenConfirm> tokenConfirmOptional =
        tokenConfirmRepository.findByTokenAndType(token, TokenType.EMAIL_VERIFICATION);

    if (tokenConfirmOptional.isPresent()) {
      TokenConfirm tokenConfirm = tokenConfirmOptional.get();

      if (tokenConfirm.getConfirmedDate() != null) {
        response.setSuccess(false);
        response.setMessage("Token xác thực tài khoản đã được xác nhận");
        return response;
      } else if (tokenConfirm.getExpiryDate().before(new Date())) {
        response.setSuccess(false);
        response.setMessage("Token xác thực tài khoản đã hết hạn");
        return response;
      }

      User user = tokenConfirm.getUser();
      user.setEnabled(true);
      userRepository.save(user);

      tokenConfirm.setConfirmedDate(new Date());
      tokenConfirmRepository.save(tokenConfirm);
    } else {
      response.setSuccess(false);
      response.setMessage("Token xác thực tài khoản không hợp lệ");
    }

    return response;
  }

  public void forgotPassword(String emailRaw) {
    final String email = normalizeEmail(emailRaw);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Không tìm thấy user có email = " + email, "USER_NOT_FOUND"));

    if (!user.getEnabled()) {
      resendVerificationIfNeeded(user);
      throw new BadRequestException("Tài khoản chưa được kích hoạt", "ACCOUNT_NOT_ACTIVATED");
    }

    TokenConfirm tokenConfirm = createOrReuseActiveToken(user, TokenType.PASSWORD_RESET);
    sendPasswordResetEmail(user, tokenConfirm);
  }

  @Transactional
  public void changePassword(ResetPasswordRequest request) {
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException(
          "Mật khẩu mới và mật khẩu xác nhận không khớp", "PASSWORD_MISMATCH");
    }

    passwordPolicy.validateOrThrow(request.getNewPassword());

    Optional<TokenConfirm> tokenConfirmOptional =
        tokenConfirmRepository.findByTokenAndType(request.getToken(), TokenType.PASSWORD_RESET);

    if (tokenConfirmOptional.isEmpty()) {
      throw new BadRequestException(
          "Token đặt lại mật khẩu không hợp lệ", "PASSWORD_RESET_TOKEN_INVALID");
    }

    TokenConfirm tokenConfirm = tokenConfirmOptional.get();

    if (tokenConfirm.getConfirmedDate() != null) {
      throw new BadRequestException(
          "Token đặt lại mật khẩu đã được xác nhận", "PASSWORD_RESET_TOKEN_ALREADY_CONFIRMED");
    }

    if (tokenConfirm.getExpiryDate().before(new Date())) {
      throw new BadRequestException(
          "Token đặt lại mật khẩu đã hết hạn", "PASSWORD_RESET_TOKEN_EXPIRED");
    }

    User user = tokenConfirm.getUser();

    // Prevent reusing the same password
    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      throw new BadRequestException(
          "Mật khẩu mới không được trùng mật khẩu hiện tại", "PASSWORD_REUSE");
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    tokenConfirm.setConfirmedDate(new Date());
    tokenConfirmRepository.save(tokenConfirm);
  }

  public VerifyTokenResponse checkForgotPasswordToken(String token) {
    VerifyTokenResponse response =
        VerifyTokenResponse.builder()
            .token(token)
            .success(true)
            .message("Token đặt lại mật khẩu hợp lệ")
            .build();

    Optional<TokenConfirm> tokenConfirmOptional =
        tokenConfirmRepository.findByTokenAndType(token, TokenType.PASSWORD_RESET);

    if (tokenConfirmOptional.isPresent()) {
      TokenConfirm tokenConfirm = tokenConfirmOptional.get();

      if (tokenConfirm.getConfirmedDate() != null) {
        response.setSuccess(false);
        response.setMessage("Token đặt lại mật khẩu đã được xác nhận");
      } else if (tokenConfirm.getExpiryDate().before(new Date())) {
        response.setSuccess(false);
        response.setMessage("Token đặt lại mật khẩu đã hết hạn");
      }
    } else {
      response.setSuccess(false);
      response.setMessage("Token đặt lại mật khẩu không hợp lệ");
    }

    return response;
  }

  private TokenConfirm createOrReuseActiveToken(User user, TokenType type) {
    Date now = new Date();
    Optional<TokenConfirm> existing =
        tokenConfirmRepository.findFirstByUserAndTypeAndConfirmedDateIsNullOrderByExpiryDateDesc(
            user, type);

    if (existing.isPresent() && existing.get().getExpiryDate().after(now)) {
      return existing.get();
    }

    TokenConfirm tokenConfirm = new TokenConfirm();
    tokenConfirm.setToken(UUID.randomUUID().toString());
    tokenConfirm.setUser(user);
    tokenConfirm.setType(type);
    tokenConfirm.setExpiryDate(new Date(System.currentTimeMillis() + 24 * 60L * 60L * 1000L));
    return tokenConfirmRepository.save(tokenConfirm);
  }

  private void resendVerificationIfNeeded(User user) {
    if (user.getEnabled()) return;
    sendVerificationEmail(user);
  }

  private void sendVerificationEmail(User user) {
    TokenConfirm tokenConfirm = createOrReuseActiveToken(user, TokenType.EMAIL_VERIFICATION);
    Map<String, String> data = buildMailData(user, tokenConfirm);
    mailService.sendMailConfirmRegistration(data);
  }

  private void sendPasswordResetEmail(User user, TokenConfirm tokenConfirm) {
    Map<String, String> data = buildMailData(user, tokenConfirm);
    mailService.sendMailResetPassword(data);
  }

  private Map<String, String> buildMailData(User user, TokenConfirm tokenConfirm) {
    Map<String, String> data = new HashMap<>();
    data.put("email", user.getEmail());
    data.put("username", user.getName());
    data.put("token", tokenConfirm.getToken());
    return data;
  }

  private String normalizeEmail(String email) {
    if (email == null) return null;
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
