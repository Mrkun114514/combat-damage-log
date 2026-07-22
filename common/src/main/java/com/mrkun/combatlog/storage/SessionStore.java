package com.mrkun.combatlog.storage;

import com.mrkun.combatlog.capture.CombatLogEntry;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;

/**
 * 会话存储：内存环形缓冲（上限 maxBufferSize）+ 周期落盘的 JSONL 会话文件。
 * 文件位于 {@code <gameDir>/combatlog/sessions/<sessionId>.jsonl}。
 */
public final class SessionStore {
    private final Deque<CombatLogEntry> recent = new ArrayDeque<>();
    private final int max;
    private final File sessionFile;
    private final BufferedWriter writer;
    private final Gson gson = new Gson();
    private int sinceFlush = 0;

    public SessionStore(int maxBufferSize) {
        this.max = Math.max(100, maxBufferSize);
        File dir = new File(Minecraft.getInstance().gameDirectory, "combatlog/sessions");
        dir.mkdirs();
        String id = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        this.sessionFile = new File(dir, id + ".jsonl");
        try {
            this.writer = Files.newBufferedWriter(sessionFile.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("无法创建战斗日志会话文件: " + sessionFile, e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::flush));
    }

    public synchronized void append(CombatLogEntry e) {
        recent.addLast(e);
        if (recent.size() > max) recent.removeFirst();
        try {
            writer.write(gson.toJson(e));
            writer.newLine();
            if (++sinceFlush >= 25) flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void flush() {
        try {
            writer.flush();
            sinceFlush = 0;
        } catch (IOException ignored) {
        }
    }

    public synchronized void clear() {
        recent.clear();
    }

    public synchronized List<CombatLogEntry> recent() {
        return new ArrayList<>(recent);
    }

    public File sessionFile() {
        return sessionFile;
    }

    public synchronized Stats stats() {
        float dealt = 0, taken = 0;
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        for (CombatLogEntry e : recent) {
            if (e.timestamp < start) start = e.timestamp;
            if (e.timestamp > end) end = e.timestamp;
            if ("DAMAGE_DEALT".equals(e.eventType)) dealt += e.amount;
            else if ("DAMAGE_TAKEN".equals(e.eventType)) taken += e.amount;
        }
        long durSec = Math.max(1, (end - start) / 1000);
        return new Stats(dealt, taken, dealt / durSec, taken / durSec, recent.size());
    }

    public synchronized void export(File out) throws IOException {
        List<CombatLogEntry> all = recent();
        try (BufferedWriter w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (CombatLogEntry e : all) {
                w.write(formatLine(e));
                w.newLine();
            }
        }
    }

    /** WoW 风格的纯文本单行渲染（导出 / 面板显示共用）。 */
    public static String formatLine(CombatLogEntry e) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(e.timestamp));
        if ("DEATH".equals(e.eventType)) {
            return "[" + time + "] " + e.deathMessage;
        }
        String dir = "DAMAGE_DEALT".equals(e.eventType) ? "造成伤害" : "受到伤害";
        String src = e.sourceEntityName.isEmpty() ? e.damageTypeId : e.sourceEntityName + "(" + e.damageTypeId + ")";
        return "[" + time + "] " + dir + " " + e.amount + " 来自 " + src + " -> " + e.targetEntityName;
    }

    public static final class Stats {
        public final float totalDealt, totalTaken, dpsDealt, dpsTaken;
        public final int entries;

        public Stats(float totalDealt, float totalTaken, float dpsDealt, float dpsTaken, int entries) {
            this.totalDealt = totalDealt;
            this.totalTaken = totalTaken;
            this.dpsDealt = dpsDealt;
            this.dpsTaken = dpsTaken;
            this.entries = entries;
        }
    }
}
