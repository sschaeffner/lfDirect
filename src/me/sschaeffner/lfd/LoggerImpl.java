package me.sschaeffner.lfd;

/**
 * A simple Logger Implementation writing to System.out.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public class LoggerImpl implements LfdLogger {
    private Loglevel level;

    public LoggerImpl(Loglevel level) {
        this.level = level;
    }


    @Override
    public void debug(String msg) {
        if (level == Loglevel.DEBUG)
            System.out.println("[Lfd][Debug] " + msg);
    }

    @Override
    public void info(String msg) {
        if (level == Loglevel.DEBUG || level == Loglevel.INFO)
            System.out.println("[Lfd][Info] " + msg);

    }

    @Override
    public void error(String msg) {
        System.out.println("[Lfd][Error] " + msg);
    }

    public void setLevel(Loglevel level) {
        this.level = level;
    }

    public enum Loglevel {
        DEBUG,
        INFO,
        ERROR
    }
}
