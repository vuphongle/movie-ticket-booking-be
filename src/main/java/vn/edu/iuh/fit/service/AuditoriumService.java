package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Auditorium;
import vn.edu.iuh.fit.entity.Cinema;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.repository.AuditoriumRepository;
import vn.edu.iuh.fit.repository.CinemaRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriumService {
    private final AuditoriumRepository auditoriumRepository;

    private final CinemaRepository cinemaRepository;

    public List<Auditorium> getAuditoriumsByCinema(Integer cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy rạp chiếu phim có id = " + cinemaId));

        return auditoriumRepository.findByCinema_Id(cinemaId);
    }
}
