package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "material")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** 钢筋辅材/防水卷材/瓷砖/线管/砂浆/五金/安全防护用品 */
    @Column(nullable = false, length = 30)
    private String category;

    /** 默认规格 */
    @Column(length = 100)
    private String spec;

    @Column(nullable = false, length = 10)
    private String unit;
}
