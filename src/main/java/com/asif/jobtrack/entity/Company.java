package com.asif.jobtrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 120)
    private String name;

    @Column(length = 500)
    private String website;

    @Column(length = 120)
    private String location;

    @Column(name = "created_at", nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void setCreatedAt(){
        this.createdAt = LocalDateTime.now();
    }
}
