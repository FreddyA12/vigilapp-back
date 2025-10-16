package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "email", unique = true, nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(name = "first_name", columnDefinition = "text")
    private String firstName;

    @Column(name = "last_name", columnDefinition = "text")
    private String lastName;

    @Column(name = "phone", columnDefinition = "text")
    private String phone;

    @Column(name = "document_type", columnDefinition = "text")
    private String documentType; // NATIONAL_ID | PASSPORT

    @Column(name = "document_number", columnDefinition = "text")
    private String documentNumber;

    @Column(name = "role", columnDefinition = "text")
    private String role; // USER | MOD | ADMIN

    @Column(name = "status", columnDefinition = "text")
    private String status; // ACTIVE | BLOCKED | PENDING

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;
}
