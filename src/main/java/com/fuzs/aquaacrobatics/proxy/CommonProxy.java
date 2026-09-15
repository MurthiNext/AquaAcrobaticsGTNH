package com.fuzs.aquaacrobatics.proxy;

import com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import com.fuzs.aquaacrobatics.AquaAcrobatics;
import com.fuzs.aquaacrobatics.biome.BiomeWaterFogColors;
import com.fuzs.aquaacrobatics.config.ConfigHandler;
import com.fuzs.aquaacrobatics.handler.CommonHandler;
import com.fuzs.aquaacrobatics.integration.IntegrationManager;
import com.fuzs.aquaacrobatics.integration.hats.HatsIntegration;
import com.fuzs.aquaacrobatics.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

@EventBusSubscriber
public class CommonProxy {

    private boolean needNetworking() {
        return ConfigHandler.MovementConfig.enableToggleCrawling;
    }

    public void onPreInit(FMLPreInitializationEvent event) {
        IntegrationManager.loadCompat();
        if (needNetworking()) NetworkHandler.registerMessages(AquaAcrobatics.MODID);
        MinecraftForge.EVENT_BUS.register(new CommonHandler());
        MinecraftForge.EVENT_BUS.register(new CrawlHandler());

    }

    public void onInit(FMLInitializationEvent event) {

    }

    public void onMappings() {

    }

    public void onPostInit(FMLPostInitializationEvent event) {

        if (IntegrationManager.isHatsEnabled()) {

            HatsIntegration.register();
        }

        BiomeWaterFogColors.recomputeColors();
        // This code will print a warning if we don't have a color mapping for the biome
        /*
         * for(BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
         * biome.getWaterColorMultiplier();
         * }
         */
    }

    public class CrawlHandler {
        @SubscribeEvent
        public void onPlayerJump(LivingEvent.LivingJumpEvent event) {
            if (!(event.entityLiving instanceof EntityPlayer)) return;
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            if (player instanceof IPlayerResizeable) {
                IPlayerResizeable r = (IPlayerResizeable) player;
                if (r.isForcingCrawling()) {
                    // 跳跃会取消强制趴下状态，减速由输入层限速自行消失
                    r.setForcingCrawling(false);
                }
            }
        }
    }

}
