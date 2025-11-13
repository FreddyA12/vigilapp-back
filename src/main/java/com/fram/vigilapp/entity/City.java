package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "cities")
public class City {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", columnDefinition = "text")
    private String name;

    @Column(name = "province", columnDefinition = "text")
    private String province;
}
