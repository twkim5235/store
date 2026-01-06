package com.example.ddd_start.order.application.service;

import com.example.ddd_start.common.domain.error.ValidationError;
import com.example.ddd_start.common.domain.exception.NoMemberFoundException;
import com.example.ddd_start.common.domain.exception.NoOrderException;
import com.example.ddd_start.common.domain.exception.ValidationErrorException;
import com.example.ddd_start.common.domain.exception.VersionConflictException;
import com.example.ddd_start.coupon.application.model.UserCouponDto;
import com.example.ddd_start.member.domain.Member;
import com.example.ddd_start.member.domain.MemberRepository;
import com.example.ddd_start.order.application.model.ChangeOrderShippingInfoCommand;
import com.example.ddd_start.order.application.model.FindOrderResponse;
import com.example.ddd_start.order.application.model.PlaceOrderCommand;
import com.example.ddd_start.order.application.model.StartShippingCommand;
import com.example.ddd_start.order.application.model.UpdateOrderCommand;
import com.example.ddd_start.order.domain.Order;
import com.example.ddd_start.order.domain.OrderLine;
import com.example.ddd_start.order.domain.OrderLineRepository;
import com.example.ddd_start.order.domain.OrderRepository;
import com.example.ddd_start.order.domain.dto.OrderLineDto;
import com.example.ddd_start.order.domain.service.DiscountCalculationService;
import com.example.ddd_start.order.domain.value.OrderState;
import com.example.ddd_start.order.domain.value.ShippingInfo;
import com.example.ddd_start.product.domain.Product;
import com.example.ddd_start.product.domain.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final MemberRepository memberRepository;
  private final DiscountCalculationService discountCalculationService;
  private final ApplicationEventPublisher eventPublisher;
  private final ProductRepository productRepository;
  private final OrderLineRepository orderLineRepository;

  @Transactional
  public void cancelOrder(Long orderId) {
    Optional<Order> optionalOrder = orderRepository.findById(orderId);
    Order order = optionalOrder.orElseThrow(NoOrderException::new);
    order.cancel();
  }

  @Transactional
  public void changeShippingInfo(ChangeOrderShippingInfoCommand changeOrderShippingInfoCommand)
      throws NoMemberFoundException {
    Optional<Order> optionalOrder = orderRepository.findById(
        changeOrderShippingInfoCommand.getOrderId());
    Order order = optionalOrder.orElseThrow(NoOrderException::new);
    ShippingInfo newShippingInfo = changeOrderShippingInfoCommand.getShippingInfo();
    order.changeShippingInfo(newShippingInfo);

    if (changeOrderShippingInfoCommand.isUseNewShippingAddressAsMemberAddress()) {
      Optional<Member> optionalMember = memberRepository.findById(order.getOrderer().getMemberId());
      Member member = optionalMember.orElseThrow(NoMemberFoundException::new);
      member.changeAddress(newShippingInfo.getAddress());
    }

    order.getOrderEvents().forEach(eventPublisher::publishEvent);
  }

  @Transactional
  public Long placeOrderV2(PlaceOrderCommand command)
      throws ValidationErrorException, NoMemberFoundException {
    List<ValidationError> errors = new ArrayList<>();

    if (command == null) {
      errors.add(ValidationError.of("empty"));
    } else {
      if (command.orderer() == null) {
        errors.add(ValidationError.of("orderer", "empty"));
      }
      if (command.orderLines() == null) {
        errors.add(ValidationError.of("orderLine", "empty"));
      }
      if (command.shippingInfo() == null) {
        errors.add(ValidationError.of("shippingInfo", "empty"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationErrorException(errors);
    }

    List<OrderLineDto> orderLineDtos = command.orderLines();
    List<Long> productIds = orderLineDtos.stream()
        .map(OrderLineDto::productId)
        .toList();
    Map<Long, Product> productMap = productRepository.findAllById(productIds)
        .stream()
        .collect(Collectors.toMap(Product::getId, p -> p));

    List<OrderLine> orderLines = orderLineDtos.stream()
        .map(orderLineDto -> {
          Product product = productMap.get(orderLineDto.productId());
          if (product == null) {
            throw new IllegalArgumentException("Product not found: " + orderLineDto.productId());
          }
          return new OrderLine(product, orderLineDto.price(), orderLineDto.quantity());
        })
        .toList();

    Order order = new Order(
        orderLines,
        command.shippingInfo(),
        command.message(),
        command.orderer(),
        command.paymentInfo()
    );
    calculatePaymentInfo(order, command.coupons());
    Order savedOrder = orderRepository.save(order);

    orderLines.forEach(orderLine -> orderLine.changeOrder(savedOrder));
    orderLineRepository.saveAll(orderLines);

    return savedOrder.getId();
  }

  private void calculatePaymentInfo(Order order, List<UserCouponDto> coupons)
      throws NoMemberFoundException {
    Member member = memberRepository.findById(order.getOrderer().getMemberId())
        .orElseThrow(NoMemberFoundException::new);

    order.calculateAmounts(discountCalculationService, member.getMemberGrade(), coupons);
  }

  @Transactional
  public void startShipping(StartShippingCommand command) {
    Order order = orderRepository.findById(command.getId()).orElseThrow(NoOrderException::new);
    if (!order.matchVersion(command.getVersion())) {
      throw new VersionConflictException();
    }

    order.changeShipped();
  }

  @Transactional(readOnly = true)
  public List<FindOrderResponse> findMyOrder(Long memberId) {
    List<Order> orders = orderRepository.findOrderByMemberId(memberId);

    if (orders.isEmpty()) {
      return List.of();
    }

    List<Long> orderIds = orders.stream().map(Order::getId).toList();
    List<OrderLine> allOrderLines = orderLineRepository.findByOrderIdIn(orderIds);

    Map<Long, List<OrderLine>> orderLineMap = allOrderLines.stream()
        .collect(Collectors.groupingBy(OrderLine::getOrderId));

    return orders.stream()
        .map(o -> new FindOrderResponse(
            o.getId(),
            o.getOrderState(),
            o.getShippingInfo(),
            o.getMessage(),
            o.getTotalAmounts(),
            o.getOrderer().getName(),
            o.getCreatedAt(),
            o.getPaymentInfo(),
            orderLineMap.getOrDefault(o.getId(), List.of())
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FindOrderResponse> findMyOrderByUsername(String username)
      throws NoMemberFoundException {
    Member member = memberRepository.findMemberByUsername(username)
        .orElseThrow(NoMemberFoundException::new);
    return findMyOrder(member.getId());
  }

  @Transactional
  public Long updateOrder(UpdateOrderCommand cmd) {
    Order findOrder = orderRepository.findById(cmd.orderId())
        .orElseThrow(NoOrderException::new);

    if (findOrder.getOrderState() == OrderState.CANCEL) {
      throw new IllegalStateException("이미 취소된 주문입니다.");
    }

    findOrder.changeShippingInfo(cmd.shippingInfo());
    return findOrder.getId();
  }
}
