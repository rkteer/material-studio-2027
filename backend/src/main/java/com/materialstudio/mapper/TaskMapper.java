package com.materialstudio.mapper;

import com.materialstudio.domain.CreationTask;
import com.materialstudio.domain.TaskStage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TaskMapper {

    @Insert("""
            INSERT INTO creation_task
                (id, user_id, template_id, status, stage, failed_stage, failure_reason,
                 result_storage_name, mock_delay_ms, mock_fail_stage, created_at, updated_at)
            VALUES
                (#{id}, #{userId}, #{templateId}, #{status}, #{stage}, #{failedStage}, #{failureReason},
                 #{resultStorageName}, #{mockDelayMs}, #{mockFailStage}, #{createdAt}, #{updatedAt})
            """)
    int insert(CreationTask task);

    @Select("SELECT * FROM creation_task WHERE id = #{id}")
    CreationTask findById(@Param("id") String id);

    @Select("""
            SELECT * FROM creation_task
            WHERE id = #{id} AND user_id = #{userId}
            """)
    CreationTask findByIdAndUser(@Param("id") String id, @Param("userId") String userId);

    @Select("""
            SELECT * FROM creation_task
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<CreationTask> findAllByUser(@Param("userId") String userId);

    @Update("""
            UPDATE creation_task
            SET status = 'PROCESSING', stage = 'VALIDATION', updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status = 'WAITING'
            """)
    int claim(@Param("id") String id);

    @Update("""
            UPDATE creation_task
            SET stage = #{stage}, updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status = 'PROCESSING'
            """)
    int updateStage(@Param("id") String id, @Param("stage") TaskStage stage);

    @Update("""
            UPDATE creation_task
            SET status = 'SUCCEEDED',
                stage = 'COMPLETED',
                failed_stage = NULL,
                failure_reason = NULL,
                result_storage_name = #{resultStorageName},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status = 'PROCESSING'
            """)
    int markSucceeded(
            @Param("id") String id,
            @Param("resultStorageName") String resultStorageName
    );

    @Update("""
            UPDATE creation_task
            SET status = 'FAILED',
                failed_stage = #{stage},
                failure_reason = #{reason},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND status IN ('WAITING', 'PROCESSING')
            """)
    int markFailed(
            @Param("id") String id,
            @Param("stage") TaskStage stage,
            @Param("reason") String reason
    );
}
