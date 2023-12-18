package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.exception.NotEnoughStockException;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.domain.service.OrderService;
import jpabook.jpashop.repository.OrderRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
public class OrderServiceTest {
    @Autowired EntityManager em;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    void 상품주문() throws Exception {
        // Given
        Member member = createMember();

        Book book = createBook("시골 jPA", 10000, 10);

        int orderCount = 3;

        // When
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);


        // Then
        Order getOrder = orderRepository.findOne(orderId);
        System.out.println("getOrder = " + getOrder);
        assertEquals(OrderStatus.ORDER, getOrder.getStatus(), "상품 주문시 상태는 ORDER");
        assertEquals(1, getOrder.getOrderItems().size(), "주문한 상품 종류 수가 정확해야 한다");
        assertEquals(10000 * orderCount, getOrder.getTotalPrice(), "주문 가격은 가격 * 수량이다");
        assertEquals(7, book.getStockQuantity(), "주문 수량만큼 재고감소");
    }

    @Test
    void 주문취소() throws Exception {
        // Given
        Member member = createMember();
        Item item = createBook("시고르", 10000, 10);

        int orderCount = 2;
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        // When
        orderService.cancelOrder(orderId);

        // Then
        Order getOrder = orderRepository.findOne(orderId);

        assertEquals(OrderStatus.CANCEL, getOrder.getStatus(), "주문 취소시 상태는 CANCEL");
        assertEquals(10, item.getStockQuantity(), "주문이 취소된 상품은 그만큼 재고가 증가해야 한다.");
    }

    @Test
    void 상품주문_재고수량_초과() throws Exception {
        // Given
        Member member = createMember();
        Item item = createBook("시골 jPA", 10000, 10);

        // When
        int orderCount = 11;
        try {
            orderService.order(member.getId(), item.getId(), orderCount);
        } catch (NotEnoughStockException e) {
            return;
        }

        // Then
        fail("재고 수량 부족 예외가 발생해야 한다.");
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "경기", "123-123"));
        em.persist(member);
        return member;
    }
}
