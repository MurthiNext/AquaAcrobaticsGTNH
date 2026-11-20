package com.fuzs.aquaacrobatics.client.handler;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import com.fuzs.aquaacrobatics.config.ConfigHandler;

public class AirMeterHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onRenderGameOverlay(final RenderGameOverlayEvent.Pre evt) {

        if (evt.type != RenderGameOverlayEvent.ElementType.AIR) {

            return;
        }

        // 这个空气条只用于展示本 mod 的「缓慢补氧」（离开水面后空气逐渐恢复）。
        // 功能关闭时原版会立刻把空气补满，再画这条只会在刚出水的那一瞬间闪一下；
        // 更糟的是装了同样实现该功能的 mod（如 NeoStates 的 modernAirRecovery）时，
        // 两边会各画一条，出水恢复期间就是两排氧气条。
        if (!ConfigHandler.MiscellaneousConfig.slowAirReplenish) {

            return;
        }

        EntityPlayer playerIn = (EntityPlayer) this.mc.renderViewEntity;
        assert playerIn != null;
        if (!playerIn.isInsideOfMaterial(Material.water) && playerIn.getAir() < 300) {

            this.mc.mcProfiler.startSection("air");
            GL11.glEnable(GL11.GL_BLEND);
            // GlStateManager.enableBlend();
            int left = evt.resolution.getScaledWidth() / 2 + 91;
            int top = evt.resolution.getScaledHeight() - GuiIngameForge.right_height;
            int air = playerIn.getAir();
            int full = MathHelper.ceiling_double_int((double) (air - 2) * 10.0D / 300.0D);
            int partial = MathHelper.ceiling_double_int((double) air * 10.0D / 300.0D) - full;
            for (int i = 0; i < full + partial; ++i) {

                this.mc.ingameGUI.drawTexturedModalRect(left - i * 8 - 9, top, (i < full ? 16 : 25), 18, 9, 9);
            }

            GuiIngameForge.right_height += 10;
            GL11.glDisable(GL11.GL_BLEND);
            // GlStateManager.disableBlend();
            this.mc.mcProfiler.endSection();
        }
    }

}
