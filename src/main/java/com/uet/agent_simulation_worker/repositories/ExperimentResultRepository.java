package com.uet.agent_simulation_worker.repositories;

import com.uet.agent_simulation_worker.models.ExperimentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

public interface ExperimentResultRepository extends JpaRepository<ExperimentResult, BigInteger> {
    List<ExperimentResult> findByExperimentId(BigInteger experimentId);

    @Query(
        value = """
            SELECT er FROM ExperimentResult er
            JOIN Experiment e ON er.experimentId = e.id
            WHERE e.userId = :user_id
            AND (:experiment_id IS NULL OR e.id = :experiment_id)
            AND (:model_id IS NULL OR e.modelId = :model_id)
            AND (:project_id IS NULL OR e.projectId = :project_id)
        """
    )
    List<ExperimentResult> find(@Param("user_id") BigInteger userId,
        @Param("experiment_id") BigInteger experimentId,
        @Param("model_id") BigInteger modelId,
        @Param("project_id") BigInteger projectId
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM ExperimentResult er WHERE er.experimentId = :experiment_id")
    void deleteByExperimentId(@Param("experiment_id") BigInteger experimentId);

    @Transactional
    @Modifying
    @Query("""
        DELETE FROM ExperimentResult er
        WHERE er.experimentId = :experiment_id
        AND er.number = :experiment_result_number
        AND er.nodeId = :node_id
    """)
    void deleteByExperimentIdAndExperimentResultNumber(
        @Param("experiment_id") BigInteger experimentId,
        @Param("experiment_result_number") Integer experimentResultNumber,
        @Param("node_id") Integer nodeId
    );
}
