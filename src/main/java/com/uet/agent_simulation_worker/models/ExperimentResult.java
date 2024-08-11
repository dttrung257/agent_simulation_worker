package com.uet.agent_simulation_worker.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Table(name = "experiment_results")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ExperimentResult extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT")
    private BigInteger id;

    @Column(nullable = false)
    private String image_url;

    @Column(name = "experiment_id", nullable = false, insertable = false, updatable = false, columnDefinition = "BIGINT")
    private BigInteger experimentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", referencedColumnName = "id")
    @JsonIgnore
    private Experiment experiment;
}
