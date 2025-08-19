package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.model.request.UpdatePasswordRequest;
import vn.edu.iuh.fit.model.request.UpdateProfileUserRequest;
import vn.edu.iuh.fit.repository.UserRepository;
import vn.edu.iuh.fit.security.PasswordPolicy;
import vn.edu.iuh.fit.security.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public void updatePassword(UpdatePasswordRequest request) {
        User user = SecurityUtils.getCurrentUserLogin();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không đúng", "INVALID_OLD_PASSWORD");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp", "PASSWORDS_NOT_MATCH");
        }

        passwordPolicy.validateOrThrow(request.getNewPassword());

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ", "NEW_PASSWORD_SAME_AS_OLD");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User updateProfile(UpdateProfileUserRequest request) {
        User user = SecurityUtils.getCurrentUserLogin();
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new BadRequestException("Tên không được để trống", "NAME_CANNOT_BE_EMPTY");
        }

        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            throw new BadRequestException("Số điện thoại không được để trống", "PHONE_CANNOT_BE_EMPTY");
        }

        if (request.getDob() == null) {
            throw new BadRequestException("Ngày sinh không được để trống", "DOB_CANNOT_BE_EMPTY");
        }

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setDob(request.getDob());

        userRepository.save(user);

        return user;
    }
}
