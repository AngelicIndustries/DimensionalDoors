package org.dimdev.dimdoors.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.helpers.Compactor;
import org.dimdev.dimdoors.pockets.Pocket;
import org.dimdev.dimdoors.pockets.PocketGrid;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.saving.DDSaveHandler;
import org.dimdev.dimdoors.saving.OldSaveImporter;
import org.dimdev.dimdoors.saving.PackedDimData;
import org.dimdev.dimdoors.util.Point4D;
import org.dimdev.dimdoors.watcher.ClientDimData;
import org.dimdev.dimdoors.watcher.ClientLinkData;
import org.dimdev.dimdoors.watcher.IUpdateWatcher;
import org.dimdev.dimdoors.watcher.UpdateWatcherProxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * This class regulates all the operations involving the storage and
 * manipulation of dimensions. It handles saving dim data and the pockets,
 * registering new pockets in the shared pocket dimensions, and loading
 * both on startup
 */
public class PocketManager {

    private static class InnerDimData extends NewDimData {
        // This class allows us to instantiate NewDimData indirectly without
        // exposing
        // a public constructor from NewDimData. It's meant to stop us from
        // constructing
        // instances of NewDimData going through PocketManager. In turn, that
        // enforces
        // that any link destinations must be real dimensions controlled by
        // PocketManager.

        public InnerDimData(int id, DimensionType type, IUpdateWatcher<ClientLinkData> linkWatcher) {
            super(id, type, linkWatcher);
        }

    }

    public static class ClientLinkWatcher implements IUpdateWatcher<ClientLinkData> {

        @Override
        public void onCreated(ClientLinkData link) {
            Point4D source = link.point;
            NewDimData dimension = getDimensionData(source.getDimension());
            if (dimension != null && dimension.getLink(source.getX(), source.getY(), source.getZ()) == null)
                dimension.createLink(source, LinkType.CLIENT, 0, link.lock);
        }

        @Override
        public void onDeleted(ClientLinkData link) {
            Point4D source = link.point;
            NewDimData dimension = getDimensionData(source.getDimension());
            if (dimension != null && dimension.getLink(source.getX(), source.getY(), source.getZ()) != null)
                dimension.deleteLink(source.getX(), source.getY(), source.getZ());
        }

        @Override
        public void update(ClientLinkData link) {
            Point4D source = link.point;
            NewDimData dimension = getDimensionData(source.getDimension());
            if (dimension != null) {
                DimLink dLink = dimension.getLink(source);
                dLink.lock = link.lock;
            }
        }
    }

    public static class ClientDimWatcher implements IUpdateWatcher<ClientDimData> {

        @Override
        public void onCreated(ClientDimData data) {
            registerClientDimension(data.ID, data.type);
        }

        @Override
        public void onDeleted(ClientDimData data) {
            // Dimension data is never deleted: pockets are cells of the shared dimensions
        }

        @Override
        public void update(ClientDimData message) {
            // TODO Auto-generated method stub
        }
    }

    private static class DimRegistrationCallback implements IDimRegistrationCallback {
        // We use this class to provide Compactor with the ability to send us
        // dim data without
        // having to instantiate a bunch of data containers and without exposing
        // an "unsafe"
        // creation method for anyone to call. Integrity protection for the win!
        // It's like
        // exposing a private constructor ONLY to a very specific trusted class.

        @Override
        public NewDimData registerDimension(int dimensionID, DimensionType type) {
            return registerClientDimension(dimensionID, type);
        }
    }

    private static volatile boolean isLoading = false;
    private static volatile boolean isLoaded = false;
    private static volatile boolean isSaving = false;
    /**
     * Set as true if we are a client that has connected to a dedicated server
     */
    public static volatile boolean isConnected = false;
    private static final UpdateWatcherProxy<ClientLinkData> linkWatcher = new UpdateWatcherProxy<ClientLinkData>();
    private static final UpdateWatcherProxy<ClientDimData> dimWatcher = new UpdateWatcherProxy<ClientDimData>();
    private static ArrayList<NewDimData> rootDimensions = null;

    // HashMap that maps all the dimension IDs registered with DimDoors to their
    // DD data.
    private static HashMap<Integer, InnerDimData> dimensionData = null;

    public static boolean isLoaded() {
        return isLoaded;
    }

    /**
     * simple method called on startup to register all dims saved in the dim
     * list. Only tries to register pocket dims, though. Also calls load()
     *
     * @return
     */
    public static void load() {
        if (isLoaded) {
            throw new IllegalStateException("Pocket dimensions have already been loaded!");
        }
        if (isLoading) {
            return;
        }
        isLoading = true;

        dimensionData = new HashMap<Integer, InnerDimData>();
        rootDimensions = new ArrayList<NewDimData>();

        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient()) {
            // Shouldnt try to load everything if we are a client
            // This was preventing onPacket from loading properly
            isLoading = false;
            isLoaded = true;
            return;
        }
        // Register Limbo and the shared pocket dimensions
        DDProperties properties = DDProperties.instance();
        createDimensionData(properties.LimboDimensionID);
        for (DimensionType type : SharedPockets.TYPES) createDimensionData(SharedPockets.dimensionOf(type));

        File saveDir = DimensionManager.getCurrentSaveRootDirectory();
        try {
            SharedPockets.load(saveDir != null ? pocketsFile(saveDir) : null, DDSaveHandler::findDungeon);
            if (saveDir != null) resetPockets(saveDir);
        } catch (IOException e) {
            // Don't go on without the pockets: new ones would be built over the old
            throw new RuntimeException("Could not load the Dimensional Doors pockets", e);
        }

        loadInternal();
        fixLostLinks();

        isLoaded = true;
        isLoading = false;
    }

    private static File pocketsFile(File saveDir) {
        return new File(saveDir, "DimensionalDoors/data/pockets.json");
    }

    /** Deletes the region files of the pockets reset dungeons marked, before their dimension loads. */
    private static void resetPockets(File saveDir) throws IOException {
        int count = SharedPockets.applyResets((type, cellX, cellZ) -> {
            File region = new File(
                saveDir,
                "DimensionalDoors/" + SharedPockets.folderOf(type)
                    + "/region/"
                    + PocketGrid.regionFileName(cellX, cellZ));
            if (region.exists() && !region.delete()) throw new IOException("Could not delete " + region);
        })
            .size();
        if (count > 0) System.out.println("Reset " + count + " dungeons");
    }

    /**
     * Whether a link destination is gone: in a pocket that was reset, or in a dimension that no longer exists (as
     * Legacy's own pocket dimensions, which aren't imported).
     */
    public static boolean isLostDestination(Point4D destination) {
        int dimension = destination.getDimension();
        if (SharedPockets.isShared(dimension)) return SharedPockets.pocketAt(destination) == null;
        return !DimensionManager.isDimensionRegistered(dimension);
    }

    /**
     * Re-makes a link whose destination is lost, as Legacy did for deleted pockets: pocket and dungeon links make a new
     * pocket on their next use, and other links in pockets become exits. Returns false for a link that can't be used
     * anymore.
     */
    public static boolean fixLostLink(DimLink link) {
        NewDimData start = getDimensionData(
            link.source()
                .getDimension());
        switch (link.linkType()) {
            case DUNGEON:
            case POCKET:
            case PERSONAL:
                start.createLink(link.source(), link.linkType(), link.orientation(), null);
                return true;
            default:
                if (!start.isPocketDimension()) return false;
                // This could be the only way out of a pocket
                Pocket pocket = SharedPockets.pocketAt(link.source());
                boolean dungeon = pocket != null && pocket.type() == DimensionType.DUNGEON;
                start.createLink(
                    link.source(),
                    dungeon ? LinkType.DUNGEON_EXIT : LinkType.SAFE_EXIT,
                    link.orientation(),
                    null);
                return true;
        }
    }

    /**
     * After loading: drops the links of reset pockets, and fixes or drops links into them and into Legacy's pocket
     * dimensions, before their cells are used again.
     */
    private static void fixLostLinks() {
        int fixed = 0;
        for (InnerDimData dimension : dimensionData.values()) {
            for (DimLink link : dimension.getAllLinks()) {
                if (dimension.isPocketDimension() && SharedPockets.pocketAt(link.source()) == null) {
                    dimension.deleteLink(link);
                    fixed++;
                } else if (link.hasDestination() && isLostDestination(link.destination())) {
                    if (!fixLostLink(link)) dimension.deleteLink(link);
                    fixed++;
                }
            }
        }
        if (fixed > 0) System.out.println("Fixed " + fixed + " links into pockets that are gone");
    }

    public static boolean registerPackedDimData(PackedDimData packedData) {
        // Legacy's pocket dimensions (not their own roots) aren't imported: their pockets are lost
        if (packedData.ID != packedData.RootID) {
            return false;
        }
        createDimensionData(packedData.ID);
        return true;
    }

    /**
     * loads the dim data from the saved hashMap. Also handles compatibility
     * with old saves, see OldSaveHandler
     */
    private static void loadInternal() {
        File saveDir = DimensionManager.getCurrentSaveRootDirectory();
        if (saveDir != null) {
            // Try to import data from old DD versions
            // TODO - remove this code in a few versions
            File oldSaveData = new File(saveDir + "/DimensionalDoorsData");
            if (oldSaveData.exists()) {
                try {
                    System.out.println("Importing old DD save data...");
                    OldSaveImporter.importOldSave(oldSaveData);

                    oldSaveData.renameTo(new File(oldSaveData.getAbsolutePath() + "_IMPORTED"));

                    System.out.println("Import Succesful!");
                } catch (Exception e) {
                    // TODO handle fail cases
                    System.out.println("Import failed!");
                    e.printStackTrace();
                }
                return;
            }

            // Load save data
            System.out.println("Loading Dimensional Doors save data...");
            if (DDSaveHandler.loadAll()) {
                System.out.println("Loaded successfully!");
            }
        }
    }

    public static void save(boolean checkModified) {
        if (!isLoaded) {
            return;
        }
        // Check this last to make sure we set the flag shortly after.
        if (isSaving) {
            return;
        }
        isSaving = true;

        try {
            DDSaveHandler.saveAll(dimensionData.values(), checkModified);
            File saveDir = DimensionManager.getCurrentSaveRootDirectory();
            if (saveDir != null) SharedPockets.save(pocketsFile(saveDir), checkModified);
        } catch (Exception e) {
            // Wrap the exception in a RuntimeException so functions that call
            // PocketManager.save() don't need to catch it. We want MC to
            // crash if something really bad happens rather than ignoring it!
            throw new RuntimeException(e);
        } finally {
            isSaving = false;
        }
    }

    public static WorldServer loadDimension(int id) {
        if (!DimensionManager.isDimensionRegistered(id)) {
            return null;
        }

        WorldServer world = DimensionManager.getWorld(id);
        if (world == null) {
            DimensionManager.initDimension(id);
            world = DimensionManager.getWorld(id);
        } else if (world.provider == null) {
            DimensionManager.initDimension(id);
            world = DimensionManager.getWorld(id);
        }
        return world;
    }

    public static NewDimData registerDimension(World world) {
        return createDimensionData(world.provider.dimensionId);
    }

    /**
     * Registers a new pocket entered through a link's source, in the next free cell of its type's shared dimension. It
     * hangs from the pocket the source is in, or from the source's real dimension. Pockets made inside personal pockets
     * are personal too, as in Legacy, but only a player's own personal pocket has an owner.
     */
    public static Pocket registerPocket(Point4D source, DimensionType type, String owner) {
        if (type == DimensionType.PERSONAL && owner == null) {
            throw new IllegalArgumentException("A personal pocket must be attached to a player");
        }
        Pocket parent = SharedPockets.pocketAt(source);
        if (parent != null && parent.type() == DimensionType.PERSONAL) {
            type = DimensionType.PERSONAL;
        }
        Pocket pocket = SharedPockets.registry(type)
            .newPocket(parent, SharedPockets.rootDimensionOf(source));
        if (owner != null) {
            pocket.setOwner(owner);
        }
        return pocket;
    }

    public static Pocket registerPocket(Point4D source, DimensionType type) {
        return registerPocket(source, type, null);
    }

    /** Registers a dimension's data with DD (not with Forge). */
    private static NewDimData registerDimension(int dimensionID, DimensionType type) {
        if (dimensionData.containsKey(dimensionID)) {
            throw new IllegalArgumentException(
                "Cannot register a dimension with ID = " + dimensionID + " because it has already been registered.");
        }
        InnerDimData dimension = new InnerDimData(dimensionID, type, linkWatcher);
        dimensionData.put(dimensionID, dimension);
        if (type == DimensionType.ROOT) {
            rootDimensions.add(dimension);
        }
        getDimwatcher().onCreated(new ClientDimData(dimension));

        return dimension;
    }

    @SideOnly(Side.CLIENT)
    private static NewDimData registerClientDimension(int dimensionID, DimensionType type) {
        // No need to raise events here since this code should only run on the client side
        NewDimData dimension = dimensionData.get(dimensionID);
        return dimension != null ? dimension : registerDimension(dimensionID, type);
    }

    public static NewDimData getDimensionData(int dimensionID) {
        return PocketManager.dimensionData.get(dimensionID);
    }

    public static NewDimData getDimensionData(World dimension) {
        return PocketManager.dimensionData.get(dimension.provider.dimensionId);
    }

    public static NewDimData createDimensionData(World world) {
        return createDimensionData(world.provider.dimensionId);
    }

    public static NewDimData createDimensionDataDangerously(int dimensionID) {
        // Same as createDimensionData(int), but public. Meant to discourage
        // anyone from
        // using it unless absolutely needed! We'll probably phase this out
        // eventually.
        return createDimensionData(dimensionID);
    }

    protected static NewDimData createDimensionData(int dimensionID) {
        // Retrieve the data for a dimension. If we don't have a record for that
        // dimension,
        // assume it's a non-pocket dimension that hasn't been initialized with
        // us before
        // and create a NewDimData instance for it.
        NewDimData dimension = PocketManager.dimensionData.get(dimensionID);

        // if we do not have a record of it, then it must be a root or a shared pocket dimension
        if (dimension == null) {
            DimensionType shared = SharedPockets.typeOf(dimensionID);
            dimension = registerDimension(dimensionID, shared != null ? shared : DimensionType.ROOT);
        }
        return dimension;
    }

    public static Iterable<? extends NewDimData> getDimensions() {
        return dimensionData.values();
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<NewDimData> getRootDimensions() {
        return (ArrayList<NewDimData>) rootDimensions.clone();
    }

    public static void tryUnload() {
        if (isConnected) unload();
        isLoading = false;
        isLoaded = false;
    }

    public static void unload() {
        System.out.println("Unloading Pocket Dimensions...");
        if (!isLoaded) {
            throw new IllegalStateException("Pocket dimensions have already been unloaded!");
        }

        SharedPockets.unload();
        dimensionData = null;
        rootDimensions = null;
        isLoaded = false;
        isConnected = false;
    }

    public static DimLink getLink(int x, int y, int z, World world) {
        return getLink(x, y, z, world.provider.dimensionId);
    }

    public static DimLink getLink(Point4D point) {
        return getLink(point.getX(), point.getY(), point.getZ(), point.getDimension());
    }

    public static DimLink getLink(int x, int y, int z, int dimensionID) {
        if (!isLoaded()) return null;

        NewDimData dimension = dimensionData.get(dimensionID);
        if (dimension != null) {
            return dimension.getLink(x, y, z);
        }
        return null;
    }

    public static void registerDimWatcher(IUpdateWatcher<ClientDimData> watcher) {
        getDimwatcher().registerReceiver(watcher);
    }

    public static boolean unregisterDimWatcher(IUpdateWatcher<ClientDimData> watcher) {
        return getDimwatcher().unregisterReceiver(watcher);
    }

    public static void registerLinkWatcher(IUpdateWatcher<ClientLinkData> watcher) {
        linkWatcher.registerReceiver(watcher);
    }

    public static boolean unregisterLinkWatcher(IUpdateWatcher<ClientLinkData> watcher) {
        return linkWatcher.unregisterReceiver(watcher);
    }

    public static void writePacket(ByteBuf output) throws IOException {
        // Write a very compact description of our dimensions and links to be
        // sent to a client
        Compactor.write(dimensionData.values(), output);
    }

    public static boolean isRegisteredInternally(int dimensionID) {
        return dimensionData.containsKey(dimensionID);
    }

    public static void readPacket(ByteBuf input) throws IOException {
        // TODO- figure out why this is getting called so frequently
        if (isLoaded) {
            return;
        }
        if (isLoading) {
            throw new IllegalStateException("Pocket dimensions are already loading!");
        }
        // Load compacted client-side dimension data
        load();
        Compactor.readDimensions(input, new DimRegistrationCallback());

        isLoaded = true;
        isLoading = false;
        isConnected = true;
    }

    public static UpdateWatcherProxy<ClientDimData> getDimwatcher() {
        return dimWatcher;
    }

    public static UpdateWatcherProxy<ClientLinkData> getLinkWatcher() {
        return linkWatcher;
    }

    /** The personal pocket a player owns, or null. */
    public static Pocket getPersonalPocket(String owner) {
        return SharedPockets.personalPocketOf(owner);
    }
}
