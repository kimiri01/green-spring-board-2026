package com.green.spring_board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
// DB테이블 이름을 넣어라.
@Table(name = "boards")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int hits;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedDatetime;

    // 유저 전체를 넣어 연관 관계를 생성
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
