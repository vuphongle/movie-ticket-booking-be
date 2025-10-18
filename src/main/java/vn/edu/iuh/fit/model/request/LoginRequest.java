package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
  @NotEmpty(message = "Email không được để trống")
  String email;

  @NotEmpty(message = "Mật khẩu không được để trống")
  String password;
}
