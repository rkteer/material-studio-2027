package com.materialstudio.domain;

public enum TaskStatus {
    WAITING,
    PROCESSING,
    SUCCEEDED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
