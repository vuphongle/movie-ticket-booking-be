package vn.edu.iuh.fit.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.constant.ConstantValue;
import vn.edu.iuh.fit.entity.Image;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.CreateUserRequest;
import vn.edu.iuh.fit.model.request.UpdatePasswordRequest;
import vn.edu.iuh.fit.model.request.UpdateProfileUserRequest;
import vn.edu.iuh.fit.model.request.UpdateUserRequest;
import vn.edu.iuh.fit.repository.ImageRepository;
import vn.edu.iuh.fit.repository.UserRepository;
import vn.edu.iuh.fit.security.PasswordPolicy;
import vn.edu.iuh.fit.security.SecurityUtils;
import vn.edu.iuh.fit.utils.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final ImageRepository imageRepository;
  private final ImageService imageService;

  public void updatePassword(UpdatePasswordRequest request) {
    User user = SecurityUtils.getCurrentUserLogin();

    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
      throw new BadRequestException("Mật khẩu cũ không đúng", "INVALID_OLD_PASSWORD");
    }

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException(
          "Mật khẩu mới và xác nhận mật khẩu không khớp", "PASSWORDS_NOT_MATCH");
    }

    passwordPolicy.validateOrThrow(request.getNewPassword());

    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      throw new BadRequestException(
          "Mật khẩu mới không được trùng với mật khẩu cũ", "NEW_PASSWORD_SAME_AS_OLD");
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

    if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
      user.setAvatar(request.getAvatar());
    }

    userRepository.save(user);

    return user;
  }

  public List<User> getAllUsers() {
    return userRepository.findAll(Sort.by("createdAt").descending());
  }

  public User getUserById(Integer id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));
  }

  public User createUser(CreateUserRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new BadRequestException("Email đã tồn tại");
    }

    User user =
        User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .avatar(StringUtils.generateLinkImage(request.getName()))
            .role(request.getRole())
            .enabled(true)
            .build();
    return userRepository.save(user);
  }

  public User updateUser(Integer id, UpdateUserRequest request) {
    User existingUser =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

    existingUser.setName(request.getName());
    existingUser.setPhone(request.getPhone());
    existingUser.setRole(request.getRole());
    existingUser.setAvatar(request.getAvatar());
    existingUser.setEnabled(request.getEnabled());
    return userRepository.save(existingUser);
  }

  @Transactional
  public void deleteUser(Integer id) {
    User existingUser =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

    // delete all images of user in database and file server
    List<Image> images = imageRepository.findByUser_Id(id);
    images.forEach(
        image -> {
          imageService.deleteImage(image.getId());
          imageRepository.delete(image);
        });

    userRepository.delete(existingUser);
  }

  public String resetPassword(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

    user.setPassword(passwordEncoder.encode(ConstantValue.DEFAULT_PASSWORD));
    userRepository.save(user);
    return ConstantValue.DEFAULT_PASSWORD;
  }
}
