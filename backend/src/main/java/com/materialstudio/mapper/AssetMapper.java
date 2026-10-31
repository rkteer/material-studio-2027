package com.materialstudio.mapper;

import com.materialstudio.domain.Asset;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AssetMapper {

    @Insert("""
            INSERT INTO asset
                (id, user_id, storage_name, original_name, mime_type, size_bytes, width, height, created_at)
            VALUES
                (#{id}, #{userId}, #{storageName}, #{originalName}, #{mimeType},
                 #{sizeBytes}, #{width}, #{height}, #{createdAt})
            """)
    int insert(Asset asset);

    @Select("SELECT * FROM asset WHERE id = #{id} AND user_id = #{userId}")
    Asset findByIdAndUser(@Param("id") String id, @Param("userId") String userId);

    @Delete("""
            DELETE FROM asset
            WHERE id = #{id}
              AND user_id = #{userId}
              AND NOT EXISTS (SELECT 1 FROM task_asset WHERE asset_id = #{id})
            """)
    int deleteIfUnused(@Param("id") String id, @Param("userId") String userId);
}
