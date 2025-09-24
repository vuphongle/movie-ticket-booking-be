package vn.edu.iuh.fit.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.model.request.CreateOrderRequest;
import vn.edu.iuh.fit.model.response.ImageResponse;
import vn.edu.iuh.fit.model.response.PaymentResponse;
import vn.edu.iuh.fit.repository.*;
import vn.edu.iuh.fit.security.SecurityUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final SeatRepository seatRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final CouponRepository couponRepository;
    private final VNPayService vnpayService;
    private final ImageService imageService;
    private final QRCodeService qrCodeService;
    private final MailService mailService;
    private final PayOSService payOSService;

    @Value("${app.backend.host}")
    private String backendHost;

    @Value("${app.backend.expose_port}")
    private String backendExposePort;

    public List<Order> getOrdersByCurrentUser() {
        User currentUser = SecurityUtils.getCurrentUserLogin();
        return orderRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(currentUser.getId(), OrderStatus.CONFIRMED);
    }

    @Transactional
    public PaymentResponse createOrder(CreateOrderRequest request, String paymentMethod) {
        log.info("Creating order with request: {}", request);

        User currentUser = SecurityUtils.getCurrentUserLogin();
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu với id " + request.getShowtimeId()));

//        // Kiểm tra mã giảm giá
//        Coupon coupon = null;
//        if (request.getCouponCode() != null) {
//            coupon = couponRepository.findByCode(request.getCouponCode())
//                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá " + request.getCouponCode()));
//            coupon.setUsed(coupon.getUsed() + 1);
//            couponRepository.save(coupon);
//        }

        // Tạo order
        Order order = Order.builder()
                .id(generateOrderId())
                .user(currentUser)
                .showtime(showtime)
                .status(OrderStatus.PENDING)
                .discount(null)
                .ticketItems(new ArrayList<>())
                .serviceItems(new ArrayList<>())
                .build();

        for (CreateOrderRequest.TicketItem ticketItem : request.getTicketItems()) {
            Seat seat = seatRepository.findById(ticketItem.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghế với id " + ticketItem.getSeatId()));
            order.addTicketItem(OrderTicketItem.builder().seat(seat).price(ticketItem.getPrice()).build());
        }

        if (request.getServiceItems() != null) {
            for (CreateOrderRequest.ServiceItem serviceItem : request.getServiceItems()) {
                AdditionalService additionalService = additionalServiceRepository.findById(serviceItem.getAdditionalServiceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ với id " + serviceItem.getAdditionalServiceId()));
                order.addServiceItem(OrderServiceItem.builder()
                        .additionalService(additionalService)
                        .quantity(serviceItem.getQuantity())
                        .price(serviceItem.getPrice())
                        .build());
            }
        }

        Order savedOrder = orderRepository.save(order);

        String paymentUrl;
        int expireSeconds = (request.getExpireSeconds() != null) ? request.getExpireSeconds() : 600;
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            String returnUrl = "%s:%s/api/orders/vnpay-payment".formatted(backendHost, backendExposePort);
            paymentUrl = vnpayService.createOrder(savedOrder.getTotalPrice(), String.valueOf(savedOrder.getId()), returnUrl, expireSeconds);
        } else if ("PAYOS".equalsIgnoreCase(paymentMethod)) {
            String returnUrl = "%s:%s/api/orders/payos-payment".formatted(backendHost, backendExposePort);
            paymentUrl = payOSService.createOrder(savedOrder.getTotalPrice(), String.valueOf(savedOrder.getId()), returnUrl);
        } else {
            throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
        }

        return PaymentResponse.builder().url(paymentUrl).build();
    }


    @Transactional
    public void updateOrderStatus(Integer orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + orderId));

        order.setStatus(status);

        // Nếu thanh toán thành công, tạo QR code và đặt ghế
        if (status == OrderStatus.CONFIRMED) {
            String qrCodeContent = String.valueOf(order.getId());
            byte[] qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeContent, 200, 200);
            ImageResponse imageResponse = imageService.uploadQRCodeImage(qrCodeImage);
            order.setQrCodePath(imageResponse.getUrl());

            Integer showtimeId = order.getShowtime().getId();
            for (OrderTicketItem ticketItem : order.getTicketItems()) {
                SeatReservation seatReservation = seatReservationRepository
                        .findBySeat_IdAndShowtime_Id(ticketItem.getSeat().getId(), showtimeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé với id " + ticketItem.getSeat().getId()));
                seatReservation.setStatus(SeatReservationStatus.BOOKED);
                seatReservationRepository.save(seatReservation);
            }

            // --- Force load các collection để tránh LazyInitializationException ---
            order.getServiceItems().size();
            order.getTicketItems().size();

            // --- Gửi email vé điện tử ---
            Map<String, Object> mailData = new HashMap<>();
            mailData.put("order", order);
            mailData.put("user", order.getUser());
            mailService.sendMailConfirmOrder(mailData, qrCodeImage);
        }
        // Nếu hủy thanh toán, Xóa trạng thái ghế đang held
        else if (status == OrderStatus.CANCELLED) {
            Integer showtimeId = order.getShowtime().getId();
            for (OrderTicketItem ticketItem : order.getTicketItems()) {
                seatReservationRepository.findBySeat_IdAndShowtime_Id(ticketItem.getSeat().getId(), showtimeId)
                        .ifPresent(seatReservationRepository::delete);
            }
            // Không tạo QR code khi hủy
            order.setQrCodePath(null);
        }

        orderRepository.save(order);
    }

    // Generate order id has 8 digits
    private Integer generateOrderId() {
        Random random = new Random();
        return random.nextInt(90000000) + 10000000;
    }

    public Order GetOrderByIdByCustomer(Integer id) {
        User currentUser = SecurityUtils.getCurrentUserLogin();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + id));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + id);
        }

        return order;
    }
}