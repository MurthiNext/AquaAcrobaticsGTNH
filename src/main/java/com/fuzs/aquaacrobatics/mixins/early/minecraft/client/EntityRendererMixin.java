package com.fuzs.aquaacrobatics.mixins.early.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fuzs.aquaacrobatics.util.math.MathHelperNew;

@SuppressWarnings("unused")
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // 姿态切换时相机高度过渡时长（秒）
    private static final float CAMERA_TRANSITION_TIME = 0.3F;

    @Shadow
    @Final
    private Minecraft mc;

    private float partialTicks;
    private float lastYOffset = Float.NaN;
    private long lastFrameTime = Long.MIN_VALUE;
    private float cameraY = Float.NaN;
    private float transitionFromY;
    private float transitionProgress = 1.0F;

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void orientCamera(float partialTicks, CallbackInfo callbackInfo) {

        // field for passing on partialTicks, workaround as @ModifyVariable is unable to handle method arguments in
        // Mixin <0.8
        this.partialTicks = partialTicks;
    }

    @ModifyVariable(
        method = "orientCamera",
        at = @At(value = "FIELD", target = "net/minecraft/entity/EntityLivingBase.prevPosX:D", ordinal = 0),
        ordinal = 1)
    public float getEyeHeight(float eyeHeight) {
        Entity entity = this.mc.renderViewEntity;
        // Do not apply eye height patch if the camera is not a player
        if (!(entity instanceof EntityPlayer)) {
            return eyeHeight;
        }

        EntityPlayer player = (EntityPlayer) entity;

        // 睡觉时 yOffset = 0.2，原版把 `yOffset - 1.62F`（即 -1.42）烘进这个局部变量，睡觉分支再在它上面 +1.0。
        // 下面返回的平滑差值只表示相机过渡量（稳态为 0），不包含这个偏移；直接替换掉它会让睡觉相机高出 1.42 格。
        // 睡觉期间也不需要站/趴/泳姿之间的过渡，原样返回即可（与原版行为一致）。
        if (player.isPlayerSleeping()) {
            return eyeHeight;
        }

        float yOffset = player.yOffset;
        double interpolatedY = player.prevPosY + (player.posY - player.prevPosY) * (double) this.partialTicks;

        long now = System.nanoTime();
        float deltaTime = this.lastFrameTime == Long.MIN_VALUE ? 0.0F
            : Math.min((now - this.lastFrameTime) / 1.0E9F, 0.25F);
        this.lastFrameTime = now;

        if (Float.isNaN(this.lastYOffset) || Math.abs(interpolatedY - (double) this.cameraY) > 8.0D) {
            // 首次调用或传送等大位移：直接对齐，不做过渡
            this.lastYOffset = yOffset;
            this.cameraY = (float) interpolatedY;
            this.transitionProgress = 1.0F;
        } else if (this.lastYOffset != yOffset) {
            // yOffset 变化（如站立 <-> 游泳）：从上一帧相机位置平滑过渡到新位置
            this.lastYOffset = yOffset;
            this.transitionFromY = this.cameraY;
            this.transitionProgress = 0.0F;
        }

        if (this.transitionProgress < 1.0F) {
            this.transitionProgress = Math.min(
                this.transitionProgress + deltaTime / CAMERA_TRANSITION_TIME,
                1.0F);
            // smoothstep 缓动：首尾速度为 0，做成淡入淡出的过渡
            float progress = this.transitionProgress;
            float eased = progress * progress * (3.0F - 2.0F * progress);
            this.cameraY = MathHelperNew.lerp(eased, this.transitionFromY, (float) interpolatedY);
        } else {
            this.cameraY = (float) interpolatedY;
        }

        return (float) (interpolatedY - (double) this.cameraY);
    }
}
