package net.glowstone.generator.decorators;

import java.util.Random;

import net.glowstone.generator.structures.GlowWitchHut;
import net.glowstone.util.BlockStateDelegate;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class WitchHutDecorator extends BlockDecorator {

    @Override
    public void decorate(World world, Random random, Chunk source) {
        if (random.nextInt(2000) == 0) {
            int x = (source.getX() << 4) + random.nextInt(16);
            int z = (source.getZ() << 4) + random.nextInt(16);
            int y = world.getHighestBlockYAt(x, z);

            final GlowWitchHut witchHut = new GlowWitchHut(random, new Location(world, x, y, z));
            final BlockStateDelegate delegate = new BlockStateDelegate();
            if (witchHut.generate(world, random, delegate)) {
                delegate.updateBlockStates();
            }
        }
    }
}
