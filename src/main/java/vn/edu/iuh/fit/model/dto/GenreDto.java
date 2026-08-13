package vn.edu.iuh.fit.model.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenreDto {
  private Integer id;
  private String name;
  private String slug;
  private Date createdAt;
  private Date updatedAt;
}
