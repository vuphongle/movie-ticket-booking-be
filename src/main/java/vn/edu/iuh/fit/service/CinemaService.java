package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Cinema;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertCinemaRequest;
import vn.edu.iuh.fit.repository.CinemaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaService {
    private final CinemaRepository cinemaRepository;

    public List<String> getAllCinemaNames() {
        return cinemaRepository.findAllCinemaNames();
    }

    public List<String> getAllCities() {
        List<String> addresses = cinemaRepository.findAllAddresses();
        return addresses.stream()
                .map(addr -> {
                    String[] parts = addr.split(",");
                    return parts[parts.length - 1].trim();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Cinema> getAllCinemas() {
        return cinemaRepository.findAll(Sort.by("id").descending());
    }

    public Cinema getCinemaById(Integer id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy rạp chiếu phim với id: " + id));
    }

    public Cinema updateCinema(Integer id, UpsertCinemaRequest request) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy rạp chiếu phim với id: " + id));

        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        cinema.setMapLocation(request.getMapLocation());
        return cinemaRepository.save(cinema);
    }
}
