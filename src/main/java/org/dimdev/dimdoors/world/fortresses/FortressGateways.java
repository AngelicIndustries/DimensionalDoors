package org.dimdev.dimdoors.world.fortresses;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureNetherBridgePieces;

import org.dimdev.dimdoors.config.DDProperties;

/**
 * Turns a blaze spawner room of some Nether fortresses into a rift gateway. The choice is made when a fortress is
 * planned, from its own layout, so any generator that builds vanilla fortress pieces gets gateways too.
 */
public final class FortressGateways {

    public static final int MAX_GATEWAY_GENERATION_CHANCE = 100;

    private FortressGateways() {}

    /** A blaze spawner room that may be built as a gateway instead. */
    public interface GatewayRoom {

        boolean dimdoors$isGateway();

        void dimdoors$setGateway(boolean gateway);
    }

    public static void pickRoom(List<?> components, int chunkX, int chunkZ) {
        DDProperties properties = DDProperties.instance();
        if (properties == null || properties.FortressGatewayGenerationChance <= 0) {
            return;
        }
        // The fortress layout already depends on the world seed, so seeding from it keeps the choice the same
        // every time the fortress is planned, even where no world is passed in.
        long seed = chunkX * 341873128712L + chunkZ * 132897987541L;
        List<StructureComponent> rooms = new ArrayList<StructureComponent>();
        for (Object entry : components) {
            StructureComponent component = (StructureComponent) entry;
            StructureBoundingBox bounds = component.getBoundingBox();
            seed = seed * 31 + ((bounds.minX * 31L + bounds.minY) * 31L + bounds.minZ);
            if (component instanceof StructureNetherBridgePieces.Throne) {
                rooms.add(component);
            }
        }
        if (rooms.isEmpty()) {
            return;
        }
        Random random = new Random(seed);
        if (random.nextInt(MAX_GATEWAY_GENERATION_CHANCE) < properties.FortressGatewayGenerationChance) {
            ((GatewayRoom) rooms.get(random.nextInt(rooms.size()))).dimdoors$setGateway(true);
        }
    }
}
