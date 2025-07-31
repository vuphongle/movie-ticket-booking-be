package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.model.request.LoginRequest;
import vn.edu.iuh.fit.model.request.RegisterRequest;
import vn.edu.iuh.fit.model.response.AuthResponse;
import vn.edu.iuh.fit.model.response.VerifyTokenResponse;
import vn.edu.iuh.fit.service.AuthService;

@Slf4j
@RestController
@RequestMapping("api/public/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.login(request);
            return ResponseEntity.ok(authResponse);
        } catch (DisabledException e) {
            throw new BadRequestException("Tài khoản của bạn chưa được kích hoạt. Vui lòng kiểm tra email của bạn để kích hoạt tài khoản");
        } catch (AuthenticationException e) {
            throw new BadRequestException("Tài khoản hoặc mật khẩu không đúng");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-register-token/{token}")
    public ResponseEntity<?> checkRegisterToken(@PathVariable String token) {
        VerifyTokenResponse response = authService.checkRegisterToken(token);
        return ResponseEntity.ok(response);
    }
}
