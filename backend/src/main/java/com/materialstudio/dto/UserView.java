package com.materialstudio.dto;

import com.materialstudio.domain.AppUser;

public record UserView(String id, String displayName, String email, String avatarColor) {
    public static UserView from(AppUser user) {
        return new UserView(user.getId(), user.getDisplayName(), user.getEmail(), user.getAvatarColor());
    }
}
