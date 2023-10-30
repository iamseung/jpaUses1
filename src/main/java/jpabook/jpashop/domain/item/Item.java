package jpabook.jpashop.domain.item;

import lombok.Getter;
import lombok.Setter;
import jpabook.jpashop.domain.Category;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // 싱글 테이블 전략
@DiscriminatorColumn(name = "dtype") // 구분자
@Getter @Setter
public abstract class Item {
    @Id
    @GeneratedValue
    @Column(name = "item_id") // 테이블 명의 아이디 값을 주로 선언
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<Category>();
}
