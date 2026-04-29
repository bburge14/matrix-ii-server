package com.rs.bot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Bot-only log writer. Routes diagnostic output to data/logs/bots.log
 * AND System.out so users can `tail -f data/logs/bots.log` for live
 * bot-only tracing without filtering through all server output.
 *
 * Use BotLog.log("subject", "message") - timestamps and the subject
 * tag (usually the bot name) are added automatically.
 */
public final class BotLog {

    private static final String PATH = "data/logs/bots.log";
    private static BufferedWriter writer;
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss");

    private BotLog() {}

    private static synchronized void open() {
        if (writer != null) return;
        try {
            File f = new File(PATH);
            f.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(f, true));
        } catch (IOException e) {
            System.err.println("[BotLog] couldn't open " + PATH + ": " + e);
        }
    }

    public static synchronized void log(String subject, String message) {
        open();
        String line = "[" + TS.format(new Date()) + "] " + subject + " | " + message;
        System.out.println(line);
        if (writer != null) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException ignored) {}
        }
    }
}
