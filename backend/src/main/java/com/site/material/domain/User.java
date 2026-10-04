package com.site.material.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    /** ROLE_PM / ROLE_KEEPER / ROLE_BUDGET / ROLE_FINANCE / ROLE_FOREMAN */
    @Column(nullable = false)
    private String role;

    /** 班组长账号绑定的班组 id */
    private Long crewId;
}
