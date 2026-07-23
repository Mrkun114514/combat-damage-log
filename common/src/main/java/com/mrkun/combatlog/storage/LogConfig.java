package com.mrkun.combatlog.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 极简 JSON 配置（不引入额外配置库，common 自实现读写）。
 * 路径：{@code <gameDir>/config/combatlog.json}
 */
public final class LogConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enableHud = true;
    public String openLogKey = "key.combatlog.open";
    public String toggleHudKey = "key.combatlog.togglehud";
    /**
     * HUD 面板左上角的屏幕坐标（GUI 缩放后的像素）。
     * 任一值 < 0 表示「自动」：默认贴右上角。拖动编辑器会把实际坐标写回这里。
     */
    public int hudX = -1;
    public int hudY = -1;
    public int maxBufferSize = 2000;
    public long maxSessionBytes = 5L * 1024 * 1024;
    public String colorScheme = "wow";

    private transient File file;

    public LogConfig() {
    }

    public LogConfig(File file) {
        this.file = file;
    }

    public static LogConfig load() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return new LogConfig();
        }
        File dir = new File(mc.gameDirectory, "config");
        dir.mkdirs();
        File file = new File(dir, "combatlog.json");
        if (file.exists()) {
            try (FileReader r = new FileReader(file, StandardCharsets.UTF_8)) {
                LogConfig loaded = GSON.fromJson(r, LogConfig.class);
                if (loaded != null) {
                    loaded.file = file;
                    return loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LogConfig cfg = new LogConfig(file);
        cfg.save();
        return cfg;
    }

    public void save() {
        if (file == null) return;
        try (FileWriter w = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
