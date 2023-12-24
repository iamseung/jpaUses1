package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /*
        Entity 전체 노출 하는 버전
        Hibernate 모듈을 기본 설정으로 쓰고 있기 때문에
        Lazy Loading 을 호출해서 프록시가 호출,
        즉, 데이터가 초기화된 데이터들을 Api로 반환하는 것을 목표로 한다.
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        // 강제 Lazy Loading
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            // OrerItem 과 Item 도 초기화
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }

        return all;
    }

    @GetMapping("api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

        return  result;
    }

    /*
    order_id 가 4인 주문 건에 orderItems 가 2개인 경우 order 자체가 2배로 뻥튀기 되는 이슈
    : 1대다 조인이 있으므로 데이터베이스 row가 증가한다.
    distinct 를 추가해서 같은 엔티티가 조회되면 애플리케이션에서 중복을 걸러준다.
    아래의 쿼리에서 order가 컬렉션 페치 조인 때문에 중복 조회되는 것을 방지

    쿼리 한번으로 조회 가능하다는 장점이 있지만 컬렉션 페치 조인을 사용하면 페이징이 불가능하다.
    즉 1대다 조인에서는 페치 조인을 사용하면 안된다.
     */
    @GetMapping("api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

        return  result;
    }

    /*
    [페치 조인과 페이징이 가능한 API 설계]
        ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다.
        따라서 ToOne 관계는 페치조인으로 쿼 리 수를 줄이고 해결하고,
        나머지는 hibernate.default_batch_fetch_size 로 최적화 하자.
     */
    @GetMapping("api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        // XtoOne 관계는 페치 조인으로 바로 가져온다
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        // 1대다 관계는 default_batch_fetch_size 를 지정해서 IN 으로 일괄로 가져와서 조회!
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

        return  result;
    }

    /*
    Query: 루트 1번, 컬렉션 N 번 실행
    ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
    이런 방식을 선택한 이유는 다음과 같다.
    ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다. ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
    row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고, ToMany 관계 는 최적화 하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
     */
    @GetMapping("api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    // Query 한번에 데이터를 조회한다
    @GetMapping("api/v6/orders")
    public List<OrderFlatDto> ordersV6() {
        return orderQueryRepository.findAllByDto_flat();
    }

    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems = new ArrayList<>();

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // Lazy 초기화, 조회하고 없으면 디비 조직
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();

            // orderItem Entity 도 전체 노출을 지양하기 때문에 별도에 Dto 로 !
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {
        private String itemName;    // 상품명
        private int orderPrice;     // 주문 가격
        private int count;          // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
