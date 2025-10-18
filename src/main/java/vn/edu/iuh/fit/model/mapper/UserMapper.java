package vn.edu.iuh.fit.model.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.model.dto.UserDto;

@Component
@RequiredArgsConstructor
public class UserMapper {
  private final ModelMapper modelMapper;

  public UserDto toUserDto(User user) {
    return modelMapper.map(user, UserDto.class);
  }
}
