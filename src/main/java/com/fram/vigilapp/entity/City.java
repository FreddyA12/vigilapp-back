package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private Integer id;

    @Column(name = "name", columnDefinition = "text")
    private String name;

    @Column(name = "province", columnDefinition = "text")
    private String province;
}
