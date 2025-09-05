package vn.edu.iuh.fit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.repository.CineRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CineService {
    private final CineRepository cineRepository;

    @Autowired
    public CineService(CineRepository cinemaRepository) {
        this.cineRepository = cinemaRepository;
    }

    public List<String> getAllCinemaNames() {
        return cineRepository.findAllCinemaNames();
    }

    public List<String> getAllCities() {
        List<String> addresses = cineRepository.findAllAddresses();
        return addresses.stream()
                .map(addr -> {
                    String[] parts = addr.split(",");
                    return parts[parts.length - 1].trim();
                })
                .distinct()
                .collect(Collectors.toList());
    }
}
