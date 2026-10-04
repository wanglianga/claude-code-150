package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 工序，如钢筋绑扎、防水施工、墙地砖铺贴、线管预埋 */
@Getter
@Setter
@Entity
@Table(name = "work_process")
public class WorkProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;
}
