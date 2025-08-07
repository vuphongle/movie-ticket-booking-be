package vn.edu.iuh.fit.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.TokenConfirm;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.dto.UserDto;
import vn.edu.iuh.fit.model.enums.TokenType;
import vn.edu.iuh.fit.model.enums.UserRole;
import vn.edu.iuh.fit.model.mapper.UserMapper;
import vn.edu.iuh.fit.model.request.LoginRequest;
import vn.edu.iuh.fit.model.request.RegisterRequest;
import vn.edu.iuh.fit.model.response.AuthResponse;
import vn.edu.iuh.fit.model.response.VerifyTokenResponse;
import vn.edu.iuh.fit.repository.TokenConfirmRepository;
import vn.edu.iuh.fit.repository.UserRepository;
import vn.edu.iuh.fit.security.JwtUtils;
import vn.edu.iuh.fit.utils.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenConfirmRepository tokenConfirmRepository;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final MailService mailService;

    public AuthResponse login(LoginRequest request) throws AuthenticationException {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        );

        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Tạo token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String tokenJwt = jwtUtils.generateToken(userDetails);

        // TODO: Tạo refresh token

        // Thông tin trả về cho Client
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có email = " + request.getEmail()));
        UserDto userDto = userMapper.toUserDto(user);

        return AuthResponse.builder()
                .user(userDto)
                .accessToken(tokenJwt)
                .refreshToken(null)
                .isAuthenticated(true)
                .build();
    }

    public void register(RegisterRequest request) {
        // check email exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email đã tồn tại", "EMAIL_ALREADY_EXISTS");
        }

        // check password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu không khớp", "PASSWORD_MISMATCH");
        }

        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$";

        if (!request.getPassword().matches(passwordRegex)) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt", "INVALID_PASSWORD_FORMAT");
        }

        // create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setAvatar(StringUtils.generateLinkImage(request.getName()));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("New user registered: {}", user);

        // Create token confirm
        TokenConfirm tokenConfirm = new TokenConfirm();
        tokenConfirm.setToken(UUID.randomUUID().toString());
        tokenConfirm.setUser(user);
        tokenConfirm.setType(TokenType.EMAIL_VERIFICATION);
        tokenConfirm.setExpiryDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));
        tokenConfirmRepository.save(tokenConfirm);
        log.info("Token confirm created: {}", tokenConfirm);

        // send email
        Map<String, String> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("username", user.getName());
        data.put("token", tokenConfirm.getToken());
        mailService.sendMailConfirmRegistration(data);
        log.info("Email sent to: {}", user.getEmail());
    }

    @Transactional
    public VerifyTokenResponse checkRegisterToken(String token) {
        VerifyTokenResponse response = VerifyTokenResponse.builder()
                .token(token)
                .success(true)
                .message("Xác thực tài khoản thành công")
                .build();

        Optional<TokenConfirm> tokenConfirmOptional = tokenConfirmRepository
                .findByTokenAndType(token, TokenType.EMAIL_VERIFICATION);

        if (tokenConfirmOptional.isPresent()) {
            TokenConfirm tokenConfirm = tokenConfirmOptional.get();

            // Kiểm tra nếu token đã được xác nhận
            if (tokenConfirm.getConfirmedDate() != null) {
                response.setSuccess(false);
                response.setMessage("Token xác thực tài khoản đã được xác nhận");
                return response;
            }
            // Kiểm tra nếu token đã hết hạn
            else if (tokenConfirm.getExpiryDate().before(new Date())) {
                response.setSuccess(false);
                response.setMessage("Token xác thực tài khoản đã hết hạn");
                return response;
            }

            // Xác thực tài khoản
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
}
