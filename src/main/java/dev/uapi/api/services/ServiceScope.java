package dev.uapi.api.services;

/** Defines how long a registered service remains valid. */
public enum ServiceScope {
    /** Lives until its registration handle is closed or the process exits. */
    GLOBAL,

    /** Cleared after the active Minecraft server stops. */
    SERVER,

    /** Cleared when the client disconnects from its current server. */
    CLIENT_CONNECTION
}
