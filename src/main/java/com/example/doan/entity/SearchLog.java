package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", nullable = false)
    private String queryText;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "result_count")
    private int resultCount;

    @Column(name = "client_ip")
    private String clientIp;
}
