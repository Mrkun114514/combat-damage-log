package com.mrkun.combatlog.neoforge;

import com.mrkun.combatlog.CombatLog;
import com.mrkun.combatlog.platform.neoforge.CombatLogPlatformImpl;
import com.mrkun.combatlog.platform.CombatLogPlatform;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CombatLog.MOD_ID)
public class CombatLogNeoForge {
    public CombatLogNeoForge() {
        // 注意：严禁在构造函数里调用 CombatLog.init()。
        // CombatLog.init() -> CombatLogService 单例 -> new SessionStore()
        // -> Minecraft.getInstance().gameDirectory，而 mod 构造阶段 Minecraft 实例尚为 null，
        // 会导致启动即 NullPointerException（尤其是专用服务端永远为 null）。
        // 初始化推迟到 FMLClientSetupEvent（此时客户端已就绪，Minecraft 实例可用）。
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().<FMLClientSetupEvent>addListener(event -> {
                CombatLog.init();
                CombatLogPlatform.init(); // 注册 HUD + 按键处理（仅客户端）
            });
            FMLJavaModLoadingContext.get().getModEventBus()
                    .addListener(RegisterKeyMappingsEvent.class, CombatLogPlatformImpl::registerKeyMappings);
        }
    }
}
