package net.glowstone.generator.objects.trees;

import java.util.Random;

import net.glowstone.util.BlockStateDelegate;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

public class AcaciaTree extends GenericTree {

    public AcaciaTree(Random random, BlockStateDelegate delegate) {
        super(random, delegate);
        int height = random.nextInt(3) + random.nextInt(3) + 5;
        setHeight(height);
    }

    protected final int getDirectionalModX(int n) {
        switch (n) {
            case 0:
                return BlockFace.SOUTH.getModX();
            case 1:
                return BlockFace.WEST.getModX();
            case 2:
                return BlockFace.NORTH.getModX();
            case 3:
                return BlockFace.EAST.getModX();
            default:
                return 0;
        }
    }

    protected final int getDirectionalModZ(int n) {
        switch (n) {
            case 0:
                return BlockFace.SOUTH.getModZ();
            case 1:
                return BlockFace.WEST.getModZ();
            case 2:
                return BlockFace.NORTH.getModZ();
            case 3:
                return BlockFace.EAST.getModZ();
            default:
                return 0;
        }
    }

    @Override
    public boolean canPlaceOn(World world, int x, int y, int z) {
        final BlockState state = delegate.getBlockState(world, x, y, z);
        if (state.getType() != Material.GRASS
                && state.getType() != Material.DIRT) {
            return false;
        }
        return true;
    }

    @Override
    public boolean generate(World world, int sourceX, int sourceY, int sourceZ) {

        // check height range
        if (!canHeightFitAt(sourceY)) {
            return false;
        }

        // check below block
        if (!canPlaceOn(world, sourceX, sourceY - 1, sourceZ)) {
            return false;
        }

        // check for sufficient space around
        if (!canPlaceAt(world, sourceX, sourceY, sourceZ)) {
            return false;
        }

        int n = random.nextInt(4);
        int twistHeight = height - 1 - random.nextInt(4);
        int twistCount = random.nextInt(3) + 1;
        int centerX = sourceX, centerZ = sourceZ;
        int trunkTopY = 0;

        // generates the trunk
        for (int y = 0; y < height; y++) {

            // trunk twists
            if (twistCount > 0 && y >= twistHeight) {
                centerX += getDirectionalModX(n);
                centerZ += getDirectionalModZ(n);
                twistCount--;
            }

            final Material material = delegate.getBlockState(world, centerX, sourceY + y, centerZ).getType();
            if (material == Material.AIR || material == Material.LEAVES) {
                trunkTopY = sourceY + y;
                delegate.setTypeAndRawData(world, centerX, sourceY + y, centerZ, Material.LOG_2, 0);
            }
        }

        // generates leaves
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) < 3 || Math.abs(z) < 3) {
                    setLeaveAt(world, centerX + x, trunkTopY, centerZ + z);
                }
                if (Math.abs(x) < 2 && Math.abs(z) < 2) {
                    setLeaveAt(world, centerX + x, trunkTopY + 1, centerZ + z);
                }
                if ((Math.abs(x) == 2 && Math.abs(z) == 0) || (Math.abs(x) == 0 && Math.abs(z) == 2)) {
                    setLeaveAt(world, centerX + x, trunkTopY + 1, centerZ + z);
                }
            }
        }

        // try to choose a different direction for second branching and canopy
        int m = random.nextInt(4);
        if (m != n) {
            centerX = sourceX; centerZ = sourceZ;
            int branchHeight = twistHeight - 1 - random.nextInt(2);
            twistCount = random.nextInt(3) + 1;
            trunkTopY = 0;

            // generates the trunk
            for (int y = branchHeight + 1; y < height; y++) {
                if (twistCount > 0) {
                    centerX += getDirectionalModX(m);
                    centerZ += getDirectionalModZ(m);
                    final Material material = delegate.getBlockState(world, centerX, sourceY + y, centerZ).getType();
                    if (material == Material.AIR || material == Material.LEAVES) {
                        trunkTopY = sourceY + y;
                        delegate.setTypeAndRawData(world, centerX, sourceY + y, centerZ, Material.LOG_2, 0);
                    }
                    twistCount--;
                }
            }

            // generates the leaves
            if (trunkTopY > 0) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        if ((Math.abs(x) < 2) || (Math.abs(z) < 2)) {
                            setLeaveAt(world, centerX + x, trunkTopY, centerZ + z);
                        }
                    }
                }
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        setLeaveAt(world, centerX + x, trunkTopY + 1, centerZ + z);
                    }
                }
            }
        }

        // block below trunk is always dirt
        delegate.setTypeAndRawData(world, sourceX, sourceY - 1, sourceZ, Material.DIRT, 0);

        return true;
    }

    private void setLeaveAt(World world, int x, int y, int z) {
        if (delegate.getBlockState(world, x, y, z).getType() == Material.AIR) {
            delegate.setTypeAndRawData(world, x, y, z, Material.LEAVES_2, 0);
        }
    }
}
