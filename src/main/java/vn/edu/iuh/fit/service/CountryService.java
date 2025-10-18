package vn.edu.iuh.fit.service;

import com.github.slugify.Slugify;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Country;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertCountryRequest;
import vn.edu.iuh.fit.repository.CountryRepository;
import vn.edu.iuh.fit.repository.MovieRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountryService {
  private final CountryRepository countryRepository;
  private final MovieRepository movieRepository;
  private final Slugify slugify;

  public List<Country> getAllCountries() {
    return countryRepository.findAll(Sort.by("id").descending());
  }

  public Country saveCountry(UpsertCountryRequest request) {
    // check country name is exist
    if (countryRepository.findByName(request.getName()).isPresent()) {
      throw new BadRequestException("Quốc gia đã tồn tại");
    }

    Country country =
        Country.builder().name(request.getName()).slug(slugify.slugify(request.getName())).build();

    return countryRepository.save(country);
  }

  public Country updateCountry(Integer id, UpsertCountryRequest request) {
    Country existingCountry =
        countryRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy Quốc gia có id = " + id));

    // Kiểm tra tên Quốc gia đã tồn tại hay chưa. Nếu đã tồn tại và không phải là Quốc gia cần
    // update thì throw exception
    if (countryRepository.findByName(request.getName()).isPresent()
        && !Objects.equals(existingCountry.getName(), request.getName())) {
      throw new BadRequestException("Quốc gia đã tồn tại");
    }

    existingCountry.setName(request.getName());
    existingCountry.setSlug(slugify.slugify(request.getName()));
    return countryRepository.save(existingCountry);
  }

  public void deleteCountry(Integer id) {
    Country existingCountry =
        countryRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy Quốc gia có id = " + id));

    // Đếm số phim có thể loại này
    long count = movieRepository.countByCountry_Id(existingCountry.getId());
    if (count > 0) {
      throw new BadRequestException(
          "Không thể xóa quốc gia này vì có " + count + " phim thuộc quốc gia này");
    }

    countryRepository.deleteById(id);
  }
}
