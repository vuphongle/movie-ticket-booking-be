package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.BaseTicketPrice;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertBaseTicketPriceRequest;
import vn.edu.iuh.fit.repository.BaseTicketPriceRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseTicketPriceService {
    private final BaseTicketPriceRepository baseTicketPriceRepository;

    public List<BaseTicketPrice> getAllBaseTicketPrices() {
        return baseTicketPriceRepository.findAll();
    }

    public BaseTicketPrice getBaseTicketPriceById(Integer id) {
        return baseTicketPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá vé với id: " + id));
    }

    public BaseTicketPrice saveBaseTicketPrice(UpsertBaseTicketPriceRequest request) {
        log.info("Creating base ticket price: {}", request);
        // Check if the base ticket price already exists
        Optional<BaseTicketPrice> existingBaseTicketPrice = baseTicketPriceRepository.findBySeatTypeAndGraphicsTypeAndAuditoriumTypeAndScreeningTimeTypeAndDayType(
                request.getSeatType(),
                request.getGraphicsType(),
                request.getAuditoriumType(),
                request.getScreeningTimeType(),
                request.getDayType()
        );
        if (existingBaseTicketPrice.isPresent()) {
            throw new BadRequestException("Giá vé đã tồn tại");
        }

        BaseTicketPrice baseTicketPrice = BaseTicketPrice.builder()
                .seatType(request.getSeatType())
                .graphicsType(request.getGraphicsType())
                .auditoriumType(request.getAuditoriumType())
                .screeningTimeType(request.getScreeningTimeType())
                .price(request.getPrice())
                .dayType(request.getDayType())
                .build();
        return baseTicketPriceRepository.save(baseTicketPrice);
    }

    public BaseTicketPrice updateBaseTicketPrice(Integer id, UpsertBaseTicketPriceRequest request) {
        BaseTicketPrice baseTicketPrice = baseTicketPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá vé với id: " + id));

        Optional<BaseTicketPrice> existingBaseTicketPrice = baseTicketPriceRepository.findBySeatTypeAndGraphicsTypeAndAuditoriumTypeAndScreeningTimeTypeAndDayType(
                request.getSeatType(),
                request.getGraphicsType(),
                request.getAuditoriumType(),
                request.getScreeningTimeType(),
                request.getDayType()
        );
        if (existingBaseTicketPrice.isPresent() && !existingBaseTicketPrice.get().getId().equals(id)) {
            throw new BadRequestException("Giá vé đã tồn tại");
        }

        baseTicketPrice.setSeatType(request.getSeatType());
        baseTicketPrice.setGraphicsType(request.getGraphicsType());
        baseTicketPrice.setAuditoriumType(request.getAuditoriumType());
        baseTicketPrice.setScreeningTimeType(request.getScreeningTimeType());
        baseTicketPrice.setPrice(request.getPrice());
        baseTicketPrice.setDayType(request.getDayType());

        return baseTicketPriceRepository.save(baseTicketPrice);
    }

    public void deleteBaseTicketPrice(Integer id) {
        BaseTicketPrice baseTicketPrice = baseTicketPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá vé với id: " + id));
        baseTicketPriceRepository.delete(baseTicketPrice);
    }
}
