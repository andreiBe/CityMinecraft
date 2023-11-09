package org.patonki.util;

import org.apache.logging.log4j.Logger;

public class ProgressLogger {
    private final int max;
    private final Logger logger;
    private final String message;
    private int progress = 0;
    private int lastLogged = 0;

    private long firstLoggedTime = -1;

    private final int diff;

    public ProgressLogger(int max, Logger logger, String message, int diff) {
        this.max = max;
        this.logger = logger;
        this.message = message;
        this.diff = diff;
    }
    private void log() {
        if (lastLogged == 0 || this.progress - this.lastLogged > this.max / this.diff || this.progress >= this.max) {
            StringBuilder toLog = new StringBuilder(message + ": " + Math.round(this.progress / (double)this.max * 100) + "%");
            if (firstLoggedTime != -1) {
                long timeTaken = System.currentTimeMillis() - firstLoggedTime;
                long millisecondsPerProgress = timeTaken / this.progress;
                int remaining = this.max - this.progress;
                long expectedRemainingTime = millisecondsPerProgress * remaining;

                int seconds = (int) (expectedRemainingTime / 1000) % 60 ;
                int minutes = (int) ((expectedRemainingTime / (1000*60)) % 60);
                int hours   = (int) ((expectedRemainingTime / (1000*60*60)) % 24);

                toLog.append(String.format(" Expected remaining time: %02d hours %02d minutes %02d seconds", hours, minutes, seconds));
            }
            this.logger.info(toLog);
            this.lastLogged = this.progress;
            if (firstLoggedTime == -1) firstLoggedTime = System.currentTimeMillis();
        }
    }
    public void increment() {
        this.add(1);
    }
    public void add(int amount) {
        this.progress += amount;
        log();
    }
}
