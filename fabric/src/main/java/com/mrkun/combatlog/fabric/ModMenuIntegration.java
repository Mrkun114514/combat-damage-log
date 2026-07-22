package com.mrkun.combatlog.fabric;

import com.mrkun.combatlog.ui.SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Menu 集成：在 Mod Menu 里为本模提供「设置」齿轮按钮。
 * 仅编译期依赖 modmenu（modCompileOnly），运行期由用户安装的 Mod Menu 提供 API。
 * 用户未安装 Mod Menu 时，fabric.mod.json 里的 "modmenu" 入口点会被 Fabric 忽略，不影响加载。
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory<Screen>) (parent -> new SettingsScreen(parent));
    }
}
