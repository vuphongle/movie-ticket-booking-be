package vn.edu.iuh.fit.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.dto.UserDto;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {
    UserDto user;
    String accessToken;
    String refreshToken;

    @JsonProperty("isAuthenticated")
    Boolean isAuthenticated;
}
