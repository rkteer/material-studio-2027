package com.materialstudio.mapper;

import com.materialstudio.domain.Template;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TemplateMapper {

    @Select("SELECT * FROM template ORDER BY created_at, title")
    List<Template> findAll();

    @Select("SELECT * FROM template WHERE category = #{category} ORDER BY created_at, title")
    List<Template> findByCategory(@Param("category") String category);

    @Select("""
            SELECT category
            FROM template
            GROUP BY category
            ORDER BY MIN(created_at), category
            """)
    List<String> findCategories();

    @Select("SELECT * FROM template WHERE id = #{id}")
    Template findById(@Param("id") String id);
}
