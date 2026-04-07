package com.fix.myfix.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import net.minecraft.world.level.block.Block;

@WailaPlugin
public class HarderBeginningsJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CharcoalPitJadeProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(ClayKilnJadeProvider.INSTANCE, Block.class);
    }
}
