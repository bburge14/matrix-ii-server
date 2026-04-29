package com.rs.bot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Audit log for diagnostic runs (::auditmethods, etc). Separate file from
 * BotLog so users can tail it independently:
 *   tail -f data/logs/audit.log
 */
public final class AuditLog {

    private static final String PATH = "data/logs/audit.log";
    private static BufferedWriter writer;
    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private AuditLog() {}

    private static synchronized void open() {
        if (writer != null) return;
        try {
            File f = new File(PATH);
            f.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(f, true));
            writer.write("# === audit session " + TS.format(new Date()) + " ===");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("[AuditLog] couldn't open " + PATH + ": " + e);
        }
    }

    public static synchronized void log(String message) {
        open();
        String line = "[" + TS.format(new Date()) + "] " + message;
        System.out.println("[AUDIT] " + line);
        if (writer != null) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException ignored) {}
        }
    }
}
