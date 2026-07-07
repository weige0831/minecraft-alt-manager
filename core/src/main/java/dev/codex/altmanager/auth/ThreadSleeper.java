package dev.codex.altmanager.auth;

final class ThreadSleeper implements Sleeper {
    @Override
    public void sleepSeconds(int seconds) throws InterruptedException {
        Thread.sleep(Math.max(1, seconds) * 1000L);
    }
}
