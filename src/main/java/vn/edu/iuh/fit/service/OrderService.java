package vn.edu.iuh.fit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.AdditionalServiceType;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.model.request.CouponDetailRequest;
import vn.edu.iuh.fit.model.request.CreateOrderRequest;
import vn.edu.iuh.fit.model.response.ImageResponse;
import vn.edu.iuh.fit.model.response.PaymentResponse;
import vn.edu.iuh.fit.repository.*;
import vn.edu.iuh.fit.security.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final SeatRepository seatRepository;
  private final AdditionalServiceRepository additionalServiceRepository;
  private final ShowtimeRepository showtimeRepository;
  private final SeatReservationRepository seatReservationRepository;
  private final ObjectMapper objectMapper;
  private final VNPayService vnpayService;
  private final ImageService imageService;
  private final QRCodeService qrCodeService;
  private final MailService mailService;
  private final PayOSService payOSService;
  private final OrderTicketItemRepository orderTicketItemRepository;
  private final OrderServiceItemRepository orderServiceItemRepository;
  private final ProductRepository productRepository;
  private final AdditionalServiceItemRepository additionalServiceItemRepository;

  @Autowired private CouponDetailTermsRepository couponDetailTermRepository;

  @Value("${app.backend.host}")
  private String backendHost;

  @Value("${app.backend.expose_port}")
  private String backendExposePort;

  public List<Order> getOrdersByCurrentUser() {
    User currentUser = SecurityUtils.getCurrentUserLogin();
    return orderRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(
        currentUser.getId(), OrderStatus.CONFIRMED);
  }

  @Transactional
  public PaymentResponse createOrder(CreateOrderRequest request, String paymentMethod)
      throws JsonProcessingException {
    log.info("Creating order with request: {}", request);

    // Validation: Phải có ít nhất 1 ticket
    if (request.getTicketItems() == null || request.getTicketItems().isEmpty()) {
      throw new IllegalArgumentException("Đơn hàng phải có ít nhất 1 vé");
    }

    User currentUser = SecurityUtils.getCurrentUserLogin();
    Showtime showtime =
        showtimeRepository
            .findById(request.getShowtimeId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy suất chiếu với id " + request.getShowtimeId()));

    // Tạo order - Ensure discount is never null
    Integer discountValue = 0;
    if (request.getDiscounts() != null && request.getDiscounts().getTotalDiscount() != null) {
      discountValue = request.getDiscounts().getTotalDiscount();
    }

    Order order =
        Order.builder()
            .id(generateOrderId())
            .user(currentUser)
            .showtime(showtime)
            .status(OrderStatus.PENDING)
            .discount(discountValue)
            .ticketItems(new ArrayList<>())
            .serviceItems(new ArrayList<>())
            .requestSnapshot(objectMapper.writeValueAsString(request))
            .build();

    for (CreateOrderRequest.TicketItem ticketItem : request.getTicketItems()) {
      Seat seat =
          seatRepository
              .findById(ticketItem.getSeatId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Không tìm thấy ghế với id " + ticketItem.getSeatId()));
      order.addTicketItem(
          OrderTicketItem.builder().seat(seat).price(ticketItem.getPrice()).build());
    }

    if (request.getServiceItems() != null) {
      for (CreateOrderRequest.ServiceItem serviceItem : request.getServiceItems()) {
        AdditionalService additionalService =
            additionalServiceRepository
                .findById(serviceItem.getAdditionalServiceId())
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "Không tìm thấy dịch vụ với id "
                                + serviceItem.getAdditionalServiceId()));
        order.addServiceItem(
            OrderServiceItem.builder()
                .additionalService(additionalService)
                .quantity(serviceItem.getQuantity())
                .price(serviceItem.getPrice())
                .build());
      }
    }

    Order savedOrder = orderRepository.save(order);

    log.info(
        "Order created with ID: {}, Total Price: {}, Discount: {}",
        savedOrder.getId(),
        savedOrder.getTotalPrice(),
        request.getDiscounts().getTotalDiscount());
    String paymentUrl;
    int expireSeconds = (request.getExpireSeconds() != null) ? request.getExpireSeconds() : 600;

    if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
      String returnUrl = "%s:%s/api/orders/vnpay-payment".formatted(backendHost, backendExposePort);
      paymentUrl =
          vnpayService.createOrder(
              savedOrder.getTotalPrice(),
              String.valueOf(savedOrder.getId()),
              returnUrl,
              expireSeconds);
    } else if ("PAYOS".equalsIgnoreCase(paymentMethod)) {
      String returnUrl = "%s:%s/api/orders/payos-payment".formatted(backendHost, backendExposePort);
      paymentUrl =
          payOSService.createOrder(
              savedOrder.getTotalPrice(),
              String.valueOf(savedOrder.getId()),
              returnUrl,
              expireSeconds);
    } else {
      throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
    }

    return PaymentResponse.builder().url(paymentUrl).build();
  }

  @Transactional
  public void updateOrderStatus(Integer orderId, OrderStatus status) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + orderId));

    // Ensure discount is never null
    if (order.getDiscount() == null) {
      order.setDiscount(0);
    }

    order.setStatus(status);

    // Nếu thanh toán thành công, tạo QR code và đặt ghế
    if (status == OrderStatus.CONFIRMED) {
      String qrCodeContent = String.valueOf(order.getId());
      byte[] qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeContent, 400, 400);
      ImageResponse imageResponse = imageService.uploadQRCodeImage(qrCodeImage);
      order.setQrCodePath(imageResponse.getUrl());

      // Cập nhật trạng thái ghế đã được đặt
      Integer showtimeId = order.getShowtime().getId();
      for (OrderTicketItem ticketItem : order.getTicketItems()) {
        SeatReservation seatReservation =
            seatReservationRepository
                .findBySeat_IdAndShowtime_Id(ticketItem.getSeat().getId(), showtimeId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "Không tìm thấy vé với id " + ticketItem.getSeat().getId()));
        seatReservation.setStatus(SeatReservationStatus.BOOKED);
        seatReservationRepository.save(seatReservation);
      }

      // Cập nhật số lượng đã dùng của coupon
      if (order.getDiscount() > 0 && order.getRequestSnapshot() != null) {
        try {
          ObjectMapper objectMapper = new ObjectMapper();
          CreateOrderRequest originalRequest =
              objectMapper.readValue(order.getRequestSnapshot(), CreateOrderRequest.class);

          log.info("Original request discounts: {}", originalRequest.getDiscounts().getCoupons());

          if (originalRequest.getDiscounts() != null
              && originalRequest.getDiscounts().getCoupons() != null) {
            for (CouponDetailRequest couponRequest : originalRequest.getDiscounts().getCoupons()) {
              if (couponRequest.getDetailId() != null) {
                CouponDetailTerms term =
                    couponDetailTermRepository
                        .findById(couponRequest.getDetailId())
                        .orElseThrow(
                            () ->
                                new ResourceNotFoundException(
                                    "Không tìm thấy điều kiện coupon với id "
                                        + couponRequest.getDetailId()));
                // Tăng số lượng đã dùng
                log.info(
                    "Cập nhật coupon detail id {}: current used count = {}, increment by 1",
                    term.getId(),
                    term.getDetailUsedCount());
                term.setDetailUsedCount(term.getDetailUsedCount() + 1);
                couponDetailTermRepository.save(term);
              }
            }
          }
        } catch (Exception e) {
          log.error("Lỗi khi đọc requestSnapshot để cập nhật coupon usage: {}", e.getMessage());
        }
      }

      // Cập nhật lại kho hàng cho các sản phẩm trong dịch vụ kèm theo
      for (OrderServiceItem serviceItem : order.getServiceItems()) {
        AdditionalService additionalService = serviceItem.getAdditionalService();
        int orderedQty = serviceItem.getQuantity();

        if (additionalService.getType() == AdditionalServiceType.COMBO) {
          List<AdditionalServiceItem> items =
              additionalServiceItemRepository.findByAdditionalServiceId(additionalService.getId());

          for (AdditionalServiceItem item : items) {
            Product product = item.getProduct();
            if (product.getQuantity() != null) {
              int totalUsed = item.getQuantity() * orderedQty;
              int newStock = product.getQuantity() - totalUsed;
              product.setQuantity(Math.max(newStock, 0));
              productRepository.save(product);

              log.info(
                  "Cập nhật kho sản phẩm id {}: trừ {} => new stock = {}",
                  product.getId(),
                  totalUsed,
                  product.getQuantity());
            }
          }
        } else if (additionalService.getType() == AdditionalServiceType.SINGLE) {
          if (additionalService.getProductId() != null) {
            Product product =
                productRepository
                    .findById(additionalService.getProductId())
                    .orElseThrow(
                        () ->
                            new ResourceNotFoundException(
                                "Không tìm thấy product với id "
                                    + additionalService.getProductId()));
            if (product.getQuantity() != null) {
              int newStock = product.getQuantity() - orderedQty;
              product.setQuantity(Math.max(newStock, 0));
              productRepository.save(product);

              log.info(
                  "Cập nhật kho sản phẩm id {} (SINGLE): trừ {} => new stock = {}",
                  product.getId(),
                  orderedQty,
                  product.getQuantity());
            }
          }
        }
      }

      // --- Force load các collection để tránh LazyInitializationException ---
      order.getServiceItems().size();
      order.getTicketItems().size();

      // --- Gửi email vé điện tử ---
      Map<String, Object> mailData = new HashMap<>();
      mailData.put("order", order);
      mailData.put("user", order.getUser());
      // Trích xuất thông tin coupon và quà tặng (nếu có)
      if (order.getRequestSnapshot() != null) {
        try {
          ObjectMapper objectMapper = new ObjectMapper();
          CreateOrderRequest request =
              objectMapper.readValue(order.getRequestSnapshot(), CreateOrderRequest.class);

          if (request.getDiscounts() != null && request.getDiscounts().getCoupons() != null) {
            mailData.put("coupons", request.getDiscounts().getCoupons());
            log.info("Gửi email với thông tin coupon: {}", request.getDiscounts().getCoupons());
          }
        } catch (Exception e) {
          log.error("Lỗi khi đọc thông tin coupon từ requestSnapshot: {}", e.getMessage());
        }
      }

      mailService.sendMailConfirmOrder(mailData, qrCodeImage);
    }
    // Nếu hủy thanh toán, Xóa trạng thái ghế đang held
    else if (status == OrderStatus.CANCELLED) {
      Integer showtimeId = order.getShowtime().getId();
      // Xóa các vé đã đặt
      for (OrderTicketItem ticketItem : order.getTicketItems()) {
        seatReservationRepository
            .findBySeat_IdAndShowtime_Id(ticketItem.getSeat().getId(), showtimeId)
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
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + id));

    if (!order.getUser().getId().equals(currentUser.getId())) {
      throw new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + id);
    }

    return order;
  }

  public List<Order> getAllOrders() {
    return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  public Order getOrderById(Integer id) {
    return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id " + id));
  }
}
