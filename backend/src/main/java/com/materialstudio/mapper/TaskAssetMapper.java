package com.materialstudio.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TaskAssetMapper {

    @Insert("""
            INSERT INTO task_asset (task_id, asset_id, sort_order)
            VALUES (#{taskId}, #{assetId}, #{sortOrder})
            """)
    int insert(
            @Param("taskId") String taskId,
            @Param("assetId") String assetId,
            @Param("sortOrder") int sortOrder
    );

    @Select("""
            SELECT asset_id
            FROM task_asset
            WHERE task_id = #{taskId}
            ORDER BY sort_order
            """)
    List<String> findAssetIds(@Param("taskId") String taskId);

    @Select("SELECT COUNT(*) FROM task_asset WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") String taskId);
}
