package com.materialstudio.mapper;

import com.materialstudio.domain.AppUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AppUserMapper {

    @Select("SELECT * FROM app_user ORDER BY created_at, id")
    List<AppUser> findAll();

    @Select("SELECT * FROM app_user WHERE id = #{id}")
    AppUser findById(@Param("id") String id);
}
