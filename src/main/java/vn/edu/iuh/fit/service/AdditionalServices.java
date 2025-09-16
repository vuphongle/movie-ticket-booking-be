package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.AdditionalService;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertAdditionalServiceRequest;
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

    public List<AdditionalService> getAllAdditionalServices() {
        return additionalServiceRepository.findAll();
    }

    public AdditionalService getAdditionalServiceById(Integer id) {
        return additionalServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ có id = " + id));
    }

    public AdditionalService saveAdditionalService(UpsertAdditionalServiceRequest request) {
        AdditionalService additionalService = AdditionalService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .thumbnail(request.getThumbnail())
                .status(request.getStatus())
                .build();

        return additionalServiceRepository.save(additionalService);
    }

    public AdditionalService updateAdditionalService(Integer id, UpsertAdditionalServiceRequest request) {
        AdditionalService additionalService = getAdditionalServiceById(id);
        
        additionalService.setName(request.getName());
        additionalService.setDescription(request.getDescription());
        additionalService.setPrice(request.getPrice());
        additionalService.setThumbnail(request.getThumbnail());
        additionalService.setStatus(request.getStatus());

        return additionalServiceRepository.save(additionalService);
    }

    public void deleteAdditionalService(Integer id) {
        AdditionalService additionalService = getAdditionalServiceById(id);
        additionalServiceRepository.delete(additionalService);
    }
}
