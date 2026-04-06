package com.fix.myfix.item;

import com.fix.myfix.event.ModGameplayEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FlintKnifeItem extends SwordItem {
    public FlintKnifeItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return ModGameplayEvents.isFiberPlant(state) ? getTier().getSpeed() : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide && ModGameplayEvents.isFiberPlant(state)) {
            stack.hurtAndBreak(1, entity, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            return true;
        }

        return super.mineBlock(stack, level, state, pos, entity);
    }
}
