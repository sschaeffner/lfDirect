package me.sschaeffner.lfd;

/**
 * Interface to a logger.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public interface LfdLogger {
    void debug(String msg);
    void info(String msg);
    void error(String msg);
}
