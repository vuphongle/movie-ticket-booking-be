package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.AdditionalService;
import vn.edu.iuh.fit.repository.AdditionalServiceRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdditionalServices {
    private final AdditionalServiceRepository additionalServiceRepository;

    public List<AdditionalService> getAllAdditionalServicesByStatus(Boolean status) {
        return additionalServiceRepository.findByStatus(status);
    }
}
