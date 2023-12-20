package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.OrderSimpleQueryDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

/*
    연관관계, xToOne
    Order
    Order->Member       : ManyToOne
    Order->Delivery     : OneToOne
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        for(Order order : all) {
            /*
            [Lazy 강제 초기화]
                order.getMember() 까지는 프록시 객체다.
                getter 를 통해 필요 데이터를 다 가져오게 되므로
                레이지 로딩을 하이버네이트 모듈에서 킬 필요가 없다.
             */
            order.getMember().getName();
            order.getDelivery().getAddress();
        }

        return all;
    }

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // ORDER 2개
         // N+1 -> 1 + 회원 N + 배송 N
        return orderRepository.findAllByString(new OrderSearch()).stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    // fetch Join
    /*
    select
        order0_.order_id as order_id1_6_0_,
        member1_.member_id as member_i1_4_1_,
        delivery2_.delivery_id as delivery1_2_2_,
        order0_.delivery_id as delivery4_6_0_,
        order0_.member_id as member_i5_6_0_,
        order0_.order_date as order_da2_6_0_,
        order0_.status as status3_6_0_,
        member1_.city as city2_4_1_,
        member1_.street as street3_4_1_,
        member1_.zipcode as zipcode4_4_1_,
        member1_.name as name5_4_1_,
        delivery2_.city as city2_2_2_,
        delivery2_.street as street3_2_2_,
        delivery2_.zipcode as zipcode4_2_2_,
        delivery2_.status as status5_2_2_
    from
        orders order0_
    inner join
        member member1_
            on order0_.member_id=member1_.member_id
    inner join
        delivery delivery2_
            on order0_.delivery_id=delivery2_.delivery_id
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());
        return result;
    }

    /*
    select 를 직접 지정하기 때문에 select 절이 v3 에 비해 가벼워진 것을 볼 수가 있다.
    select
        order0_.order_id as col_0_0_,
        member1_.name as col_1_0_,
        order0_.order_date as col_2_0_,
        order0_.status as col_3_0_,
        delivery2_.city as col_4_0_,
        delivery2_.street as col_4_1_,
        delivery2_.zipcode as col_4_2_
    from
        orders order0_
    inner join
        member member1_
            on order0_.member_id=member1_.member_id
    inner join
        delivery delivery2_
            on order0_.delivery_id=delivery2_.delivery_id
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderRepository.findOrderDtos();
    }

    // @Data 어노테이션 종합세트
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // Lazy 초기화, 조회하고 없으면 디비 조직
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }
}
