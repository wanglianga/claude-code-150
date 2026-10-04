package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    /** ADMIN / PROJECT_MANAGER / WAREHOUSE / CREW_LEADER / FINANCE / BUDGET */
    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "crew_id")
    private Long crewId;

    @Column(length = 20)
    private String phone;
}
