package com.mrkun.combatlog.neoforge;

import com.mrkun.combatlog.CombatLog;
import com.mrkun.combatlog.platform.neoforge.CombatLogPlatformImpl;
import com.mrkun.combatlog.platform.CombatLogPlatform;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CombatLog.MOD_ID)
public class CombatLogNeoForge {
    public CombatLogNeoForge() {
        CombatLog.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CombatLogPlatform.init(); // 注册 HUD + 按键处理（仅客户端）
            FMLJavaModLoadingContext.get().getModEventBus()
                    .addListener(RegisterKeyMappingsEvent.class, CombatLogPlatformImpl::registerKeyMappings);
        }
    }
}
