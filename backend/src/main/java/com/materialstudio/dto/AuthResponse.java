package com.materialstudio.dto;

import java.util.List;

public record AuthResponse(UserView currentUser, List<UserView> users) {
}
