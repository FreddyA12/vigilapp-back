package com.fram.vigilapp.entity;

import com.fram.vigilapp.entity.id.AlertMediaId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@IdClass(AlertMediaId.class)
@Table(name = "alert_media")
public class AlertMedia {
    @Id
    @ManyToOne
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Id
    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;
}
