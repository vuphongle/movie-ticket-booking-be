package vn.edu.iuh.fit.model.request;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotEmpty(message = "Tên không được để trống")
    String name;

    @NotEmpty(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email;

    @NotEmpty(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "(0[0-9]{9})", message = "Số điện thoại không đúng định dạng")
    String phone;

    @NotEmpty(message = "Mật khẩu không được để trống")
    String password;

    @NotEmpty(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;

    Date dob;
}
