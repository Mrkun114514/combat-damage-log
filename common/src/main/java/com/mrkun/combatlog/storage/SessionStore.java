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
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话存储：内存环形缓冲（上限 maxBufferSize）+ 周期落盘的 JSONL 会话文件。
 * 文件位于 {@code <gameDir>/combatlog/sessions/<sessionId>.jsonl}。
 */
public final class SessionStore {
    private final Deque<CombatLogEntry> recent = new ArrayDeque<>();
    private final LogConfig config;
    private final File sessionFile;
    private final BufferedWriter writer;
    private final Gson gson = new Gson();
    private int sinceFlush = 0;

    public SessionStore(LogConfig config) {
        this.config = config;
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
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    /** 当前生效的缓冲上限（读取实时配置，修改设置后无需重启即可生效）。 */
    private int max() {
        return Math.max(100, config.maxBufferSize);
    }

    public synchronized void append(CombatLogEntry e) {
        recent.addLast(e);
        if (recent.size() > max()) recent.removeFirst();
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

    public synchronized void close() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException ignored) {
        }
        sinceFlush = 0;
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

    /**
     * WoW 风格伤害统计（DPS 计量器）：按「伤害来源」（谁打的）与「受伤目标」（谁被打）
     * 两个维度聚合，并保留各自内部的明细拆分（来源→各目标 / 目标→各来源，取前若干）。
     * 仅统计造成伤害 / 受到伤害两类事件。
     */
    public synchronized Meter meter() {
        Map<String, Breakdown> bySource = new LinkedHashMap<>();
        Map<String, Breakdown> byTarget = new LinkedHashMap<>();
        float totalDealt = 0, totalTaken = 0;
        for (CombatLogEntry e : recent) {
            if (!"DAMAGE_DEALT".equals(e.eventType) && !"DAMAGE_TAKEN".equals(e.eventType)) continue;
            if (e.amount <= 0) continue;
            String srcKey = keyOf(e.sourceEntityName, e.sourceEntityType);
            String tgtKey = keyOf(e.targetEntityName, e.targetEntityType);
            Breakdown s = bySource.computeIfAbsent(srcKey, k -> new Breakdown(e.sourceEntityName, e.sourceEntityType));
            Breakdown t = byTarget.computeIfAbsent(tgtKey, k -> new Breakdown(e.targetEntityName, e.targetEntityType));
            s.total += e.amount; s.hits++;
            t.total += e.amount; t.hits++;
            s.addSplit(e.targetEntityName, e.amount);
            t.addSplit(e.sourceEntityName, e.amount);
            if ("DAMAGE_DEALT".equals(e.eventType)) totalDealt += e.amount;
            else totalTaken += e.amount;
        }
        List<Breakdown> sources = topSorted(bySource.values());
        List<Breakdown> targets = topSorted(byTarget.values());
        return new Meter(sources, targets, totalDealt, totalTaken);
    }

    private static String keyOf(String name, String type) {
        return name + "\u0000" + (type == null ? "" : type);
    }

    private static List<Breakdown> topSorted(Collection<Breakdown> values) {
        List<Breakdown> list = new ArrayList<>(values);
        list.sort((a, b) -> Float.compare(b.total, a.total));
        for (Breakdown b : list) b.trimSplit(3);
        return list;
    }

    public static final class Meter {
        public final List<Breakdown> bySource;   // 谁打了伤害（含对各个目标的拆分）
        public final List<Breakdown> byTarget;   // 哪个实体受伤了（含被各个来源打的拆分）
        public final float totalDealt, totalTaken;

        public Meter(List<Breakdown> bySource, List<Breakdown> byTarget, float totalDealt, float totalTaken) {
            this.bySource = bySource;
            this.byTarget = byTarget;
            this.totalDealt = totalDealt;
            this.totalTaken = totalTaken;
        }
    }

    public static final class Breakdown {
        public final String name;     // 实体显示名（来源或目标）
        public final String type;     // 实体注册表 id
        public float total;           // 该维度总伤害
        public int hits;              // 命中次数
        public final List<Contrib> split = new ArrayList<>(); // 对另一维度的明细拆分

        public Breakdown(String name, String type) {
            this.name = name;
            this.type = type;
        }

        void addSplit(String otherName, float amount) {
            for (Contrib c : split) {
                if (c.name.equals(otherName)) {
                    c.amount += amount;
                    return;
                }
            }
            split.add(new Contrib(otherName, amount));
        }

        void trimSplit(int n) {
            split.sort((a, b) -> Float.compare(b.amount, a.amount));
            while (split.size() > n) split.remove(split.size() - 1);
        }
    }

    public static final class Contrib {
        public final String name;
        public float amount;

        public Contrib(String name, float amount) {
            this.name = name;
            this.amount = amount;
        }
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
