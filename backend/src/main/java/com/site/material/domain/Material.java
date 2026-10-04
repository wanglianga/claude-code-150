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

    @Column(nullable = false, length = 128)
    private String name;

    /** 钢筋辅材 / 防水卷材 / 瓷砖 / 线管 / 砂浆 / 五金 / 安全防护用品 */
    @Column(nullable = false, length = 64)
    private String category;

    @Column(length = 64)
    private String spec;

    @Column(nullable = false, length = 16)
    private String unit;

    /** 库存预警下限 */
    private Double safetyStock = 0d;

    private boolean active = true;
}
