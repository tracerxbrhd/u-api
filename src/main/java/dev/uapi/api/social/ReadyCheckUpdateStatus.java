package dev.uapi.api.social;

/** Outcome of a requested state-machine or service operation. */
public enum ReadyCheckUpdateStatus {
    APPLIED,
    NO_CHANGE,
    UNAUTHORIZED,
    UNKNOWN_PARTICIPANT,
    TERMINAL_STATE,
    NOT_FOUND
}
