package com.fuzs.aquaacrobatics.mixins.early.minecraft.client;

import javax.annotation.Nonnull;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fuzs.aquaacrobatics.client.model.IModelBipedSwimming;
import com.fuzs.aquaacrobatics.config.ConfigHandler;
import com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable;
import com.fuzs.aquaacrobatics.integration.efr.EFRIntegration;
import com.fuzs.aquaacrobatics.util.math.MathHelperNew;

@SuppressWarnings("unused")
@Mixin(ModelBiped.class)
public abstract class ModelBipedMixin extends ModelBase implements IModelBipedSwimming {

    @Shadow
    public ModelRenderer bipedHead;
    @Shadow
    public ModelRenderer bipedHeadwear;
    @Shadow
    public ModelRenderer bipedRightArm;
    @Shadow
    public ModelRenderer bipedLeftArm;
    @Shadow
    public ModelRenderer bipedRightLeg;
    @Shadow
    public ModelRenderer bipedLeftLeg;

    @Unique
    public float swimAnimation;

    // ModelPlayerAPI 会把原方法体重命名为 localRender，需要同时注入两者（参照 1.12 上游修复）
    @Redirect(
        method = { "render", "localRender" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelBiped;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"))
    public void setRotationAngles(ModelBiped modelBiped, float limbSwing, float limbSwingAmount, float ageInTicks,
        float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {

        if (entityIn instanceof IPlayerResizeable) {

            boolean flag = EFRIntegration.getTicksElytraFlying((EntityPlayer) entityIn) > 4;
            boolean flag1 = ((IPlayerResizeable) entityIn).isActuallySwimming();
            if (!flag && this.swimAnimation > 0.0F) {

                if (flag1) {

                    headPitch = this
                        .rotLerpRad(this.swimAnimation, this.bipedHead.rotateAngleX, ((float) -Math.PI / 4F))
                        / 0.017453292F;
                } else {

                    headPitch = this.rotLerpRad(
                        this.swimAnimation,
                        this.bipedHead.rotateAngleX,
                        headPitch * ((float) Math.PI / 180F)) / 0.017453292F;
                }
            }
        }

        modelBiped
            .setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
    }

    // ModelPlayerAPI 会把原方法体重命名为 localSetRotationAngles，需要同时注入两者（参照 1.12 上游修复）
    @Inject(
        method = { "setRotationAngles", "localSetRotationAngles" },
        at = @At(value = "FIELD", target = "net/minecraft/client/model/ModelBiped.onGround:F", ordinal = 0))
    public void setRotationAnglesPre(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scaleFactor, Entity entityIn, CallbackInfo callbackInfo) {

        if (!ConfigHandler.MiscellaneousConfig.eatingAnimation || !(entityIn instanceof EntityLivingBase)) {

            return;
        }

        EntityLivingBase livingEntityIn = (EntityLivingBase) entityIn;
        if (livingEntityIn instanceof EntityPlayer player && player.getHeldItem() != null
            && player.getItemInUseCount() > 0) {

            ItemStack stack = livingEntityIn.getHeldItem();
            if (stack != null && stack.getItemUseAction() == EnumAction.eat
                || stack.getItemUseAction() == EnumAction.drink) {

                float partialTicks = (float) (ageInTicks - Math.floor(ageInTicks));
                float animationCount = player.getItemInUseCount() - partialTicks + 1.0F;
                float useRatio = animationCount / (float) stack.getMaxItemUseDuration();
                float f = 1.0F - (float) Math.pow(useRatio, 27.0D);
                if (useRatio < 0.8F) {

                    f += MathHelper.abs(MathHelper.cos(animationCount / 4.0F * (float) Math.PI) * 0.1F);
                }

                bipedRightArm.rotateAngleX = f * (bipedRightArm.rotateAngleX * 0.5F - ((float) Math.PI * 4.0F / 10.0F));
                bipedRightArm.rotateAngleY = f * (float) Math.PI / 6F * (-1.0F);
                bipedLeftArm.rotateAngleX = f * (bipedLeftArm.rotateAngleX * 0.5F - ((float) Math.PI * 4.0F / 10.0F));
                bipedLeftArm.rotateAngleY = f * (float) Math.PI / 6F * (1.0F);
            }
        }
    }

    @Inject(method = "setRotationAngles", at = @At(value = "TAIL"), cancellable = true)
    public void setRotationAnglesPost(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scaleFactor, Entity entityIn, CallbackInfo callbackInfo) {

        if (this.swimAnimation > 0.0F) {

            float f1 = limbSwing % 26.0F;
            float f2 = this.onGround > 0.0F ? 0.0F : this.swimAnimation;
            float f3 = this.onGround > 0.0F ? 0.0F : this.swimAnimation;
            if (f1 < 14.0F) {

                this.bipedLeftArm.rotateAngleX = this.rotLerpRad(f3, this.bipedLeftArm.rotateAngleX, 0.0F);
                this.bipedRightArm.rotateAngleX = MathHelperNew.lerp(f2, this.bipedRightArm.rotateAngleX, 0.0F);
                this.bipedLeftArm.rotateAngleY = this.rotLerpRad(f3, this.bipedLeftArm.rotateAngleY, (float) Math.PI);
                this.bipedRightArm.rotateAngleY = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleY, (float) Math.PI);
                this.bipedLeftArm.rotateAngleZ = this.rotLerpRad(
                    f3,
                    this.bipedLeftArm.rotateAngleZ,
                    (float) Math.PI + 1.8707964F * this.getArmAngleSq(f1) / this.getArmAngleSq(14.0F));
                this.bipedRightArm.rotateAngleZ = MathHelperNew.lerp(
                    f2,
                    this.bipedRightArm.rotateAngleZ,
                    (float) Math.PI - 1.8707964F * this.getArmAngleSq(f1) / this.getArmAngleSq(14.0F));
            } else if (f1 >= 14.0F && f1 < 22.0F) {

                float f10 = (f1 - 14.0F) / 8.0F;
                this.bipedLeftArm.rotateAngleX = this
                    .rotLerpRad(f3, this.bipedLeftArm.rotateAngleX, ((float) Math.PI / 2F) * f10);
                this.bipedRightArm.rotateAngleX = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleX, ((float) Math.PI / 2F) * f10);
                this.bipedLeftArm.rotateAngleY = this.rotLerpRad(f3, this.bipedLeftArm.rotateAngleY, (float) Math.PI);
                this.bipedRightArm.rotateAngleY = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleY, (float) Math.PI);
                this.bipedLeftArm.rotateAngleZ = this
                    .rotLerpRad(f3, this.bipedLeftArm.rotateAngleZ, 5.012389F - 1.8707964F * f10);
                this.bipedRightArm.rotateAngleZ = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleZ, 1.2707963F + 1.8707964F * f10);
            } else if (f1 >= 22.0F && f1 < 26.0F) {

                float f9 = (f1 - 22.0F) / 4.0F;
                this.bipedLeftArm.rotateAngleX = this.rotLerpRad(
                    f3,
                    this.bipedLeftArm.rotateAngleX,
                    ((float) Math.PI / 2F) - ((float) Math.PI / 2F) * f9);
                this.bipedRightArm.rotateAngleX = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleX, ((float) Math.PI / 2F) - ((float) Math.PI / 2F) * f9);
                this.bipedLeftArm.rotateAngleY = this.rotLerpRad(f3, this.bipedLeftArm.rotateAngleY, (float) Math.PI);
                this.bipedRightArm.rotateAngleY = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleY, (float) Math.PI);
                this.bipedLeftArm.rotateAngleZ = this.rotLerpRad(f3, this.bipedLeftArm.rotateAngleZ, (float) Math.PI);
                this.bipedRightArm.rotateAngleZ = MathHelperNew
                    .lerp(f2, this.bipedRightArm.rotateAngleZ, (float) Math.PI);
            }

            this.bipedLeftLeg.rotateAngleX = MathHelperNew.lerp(
                this.swimAnimation,
                this.bipedLeftLeg.rotateAngleX,
                0.3F * MathHelper.cos(limbSwing * 0.33333334F + (float) Math.PI));
            this.bipedRightLeg.rotateAngleX = MathHelperNew.lerp(
                this.swimAnimation,
                this.bipedRightLeg.rotateAngleX,
                0.3F * MathHelper.cos(limbSwing * 0.33333334F));

            callbackInfo.cancel();
        }
    }

    @Override
    public void setLivingAnimations(@Nonnull EntityLivingBase entitylivingbaseIn, float limbSwing,
        float limbSwingAmount, float partialTickTime) {

        if (entitylivingbaseIn instanceof IPlayerResizeable) {

            this.swimAnimation = ((IPlayerResizeable) entitylivingbaseIn).getSwimAnimation(partialTickTime);
        }
    }

    private float getArmAngleSq(float limbSwing) {

        return -65.0F * limbSwing + limbSwing * limbSwing;
    }

    @Unique
    protected float rotLerpRad(float angleIn, float maxAngleIn, float mulIn) {

        float f = (mulIn - maxAngleIn) % ((float) Math.PI * 2F);
        if (f < -(float) Math.PI) {

            f += ((float) Math.PI * 2F);
        }

        if (f >= (float) Math.PI) {

            f -= ((float) Math.PI * 2F);
        }

        return maxAngleIn + angleIn * f;
    }

    @Override
    public void setSwimAnimation(float swimAnimation) {

        this.swimAnimation = swimAnimation;
    }

}
