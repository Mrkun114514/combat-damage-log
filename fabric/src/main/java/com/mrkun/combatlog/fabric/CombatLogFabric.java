package com.mrkun.combatlog.fabric;

import com.mrkun.combatlog.platform.CombatLogPlatform;
import net.fabricmc.api.ClientModInitializer;

public class CombatLogFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CombatLogPlatform.init();
    }
}
