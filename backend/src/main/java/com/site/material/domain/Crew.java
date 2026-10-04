package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crew")
public class Crew {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    /** 工种：钢筋/防水/镶贴/水电/架子安全等 */
    @Column(length = 30)
    private String trade;

    @Column(name = "leader_name", length = 50)
    private String leaderName;

    @Column(length = 20)
    private String phone;
}
