package com.lumina.twinstar.algorithm.model;

/**
 * Planning time limits. Soft (0.5 s): stop improving and return the best so far, with no notice.
 * Hard (1 s): abort and report a timeout.
 */
public record Deadline(long startNanos, long softNanos, long hardNanos) {
    public static final long DEFAULT_SOFT = 500_000_000L;
    public static final long DEFAULT_HARD = 1_000_000_000L;

    /** For tests and tools: never runs out. */
    public static final Deadline NONE = new Deadline(0, Long.MAX_VALUE, Long.MAX_VALUE);

    public static Deadline startingNow() {
        return new Deadline(System.nanoTime(), DEFAULT_SOFT, DEFAULT_HARD);
    }

    public boolean softPassed() {
        return softNanos != Long.MAX_VALUE && System.nanoTime() - startNanos > softNanos;
    }

    public void checkHard() {
        if (hardNanos != Long.MAX_VALUE && System.nanoTime() - startNanos > hardNanos)
            throw new PlanTimeoutException();
    }

    public static final class PlanTimeoutException extends RuntimeException {
        public PlanTimeoutException() {
            super("crafting calculation timed out", null, false, false);
        }
    }
}
