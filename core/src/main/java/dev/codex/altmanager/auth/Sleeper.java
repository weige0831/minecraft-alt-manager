package dev.codex.altmanager.auth;

interface Sleeper {
    void sleepSeconds(int seconds) throws InterruptedException;
}
