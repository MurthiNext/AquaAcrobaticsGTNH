package com.fuzs.aquaacrobatics.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.fuzs.aquaacrobatics.config.ConfigHandler;
import com.fuzs.aquaacrobatics.integration.IntegrationManager;
import com.fuzs.aquaacrobatics.integration.ae2.AE2Integration;
import com.fuzs.aquaacrobatics.util.BlockPos;

/**
 * Allows items to float like post-1.13.
 */
@Mixin(EntityItem.class)
public abstract class EntityItemMixin extends Entity {

    public EntityItemMixin(World p_i1582_1_) {
        super(p_i1582_1_);
    }

    private void applyFloatMotion() {
        if (this.motionY < (double) 0.06F) {
            this.motionY += (double) 5.0E-4F;
        }
        this.motionX *= 0.99F;
        this.motionZ *= 0.99F;
    }

    private boolean aqua$shouldBeBuoyant() {
        if (!ConfigHandler.MiscellaneousConfig.floatingItems) return false;
        if (IntegrationManager.isAE2Enabled() && AE2Integration.isGrowingCrystal((EntityItem) (Object) this))
            return false;
        return true;
    }

    @Redirect(
        method = "onUpdate",
        at = @At(value = "FIELD", target = "net/minecraft/entity/item/EntityItem.motionY:D", ordinal = 0),
        expect = 1,
        require = 0)
    private double applyFloatMotionIfInWater(EntityItem entityItem) {
        if (!aqua$shouldBeBuoyant()) {
            // Not buoyant: hand the current velocity back so vanilla's own `this.motionY -= 0.04` applies normal
            // gravity. Returning the gravity constant here replaced the value being subtracted from, which made every
            // item lose 0.08 per tick (double gravity, dropped items landed at roughly half the vanilla distance).
            return entityItem.motionY;
        }
        double eyePosition = this.posY + (double) this.getEyeHeight();
        BlockPos eyeBlockPos = new BlockPos(this.posX, eyePosition, this.posZ);
        Block state = this.worldObj.getBlock(eyeBlockPos.getX(), eyeBlockPos.getY(), eyeBlockPos.getZ());
        int metadata = this.worldObj.getBlockMetadata(eyeBlockPos.getX(), eyeBlockPos.getY(), eyeBlockPos.getZ());
        if (state.getMaterial() == Material.water && state instanceof BlockLiquid) {
            float thresholdHeight = eyeBlockPos.getY() + getBlockLiquidHeight(state, metadata) + (1f / 9f);
            if (eyePosition < thresholdHeight) {
                applyFloatMotion();
                return 0.03999999910593033D;
            }
        }
        return entityItem.motionY;
    }

    private static float getBlockLiquidHeight(Block block, int level) {
        return block instanceof BlockLiquid ? BlockLiquid.getLiquidHeightPercent(level) : 1.0F;
    }
}
