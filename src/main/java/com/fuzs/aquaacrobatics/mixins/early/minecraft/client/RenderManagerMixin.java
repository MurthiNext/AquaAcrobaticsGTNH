package com.fuzs.aquaacrobatics.mixins.early.minecraft.client;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(RenderManager.class)
public abstract class RenderManagerMixin {

    /**
     * 原版 F3+B 用渲染插值位置直接当箱底（玩家 posY 是眼睛高度），Hodgepodge 的修正又硬编码了 -1.62，
     * 与本 mod 游泳时的 yOffset=0.28 冲突，导致调试箱比玩家低约一格。
     * 这里直接使用实体真实的 boundingBox 叠加上插值位移绘制，保证调试箱与碰撞箱完全一致。
     */
    @Inject(method = "renderDebugBoundingBox", at = @At("HEAD"), cancellable = true)
    private void renderAccurateDebugBoundingBox(Entity entity, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo callbackInfo) {

        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);

        AxisAlignedBB box = entity.boundingBox
            .getOffsetBoundingBox(x - entity.posX, y - entity.posY, z - entity.posZ);
        RenderGlobal.drawOutlinedBoundingBox(box, 0xFFFFFF);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        callbackInfo.cancel();
    }

}
