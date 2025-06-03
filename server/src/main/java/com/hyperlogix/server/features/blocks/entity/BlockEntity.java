package com.hyperlogix.server.features.blocks.entity;
import com.hyperlogix.server.domain.Point;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "roadblocks")
@Data
@AllArgsConstructor
@NoArgsConstructor 
public class BlockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @ElementCollection
    @CollectionTable(name = "roadblock_nodes", 
                    joinColumns = @JoinColumn(name = "roadblock_id"))
    @OrderColumn(name = "node_order")    
    private List<Point> nodes;
}