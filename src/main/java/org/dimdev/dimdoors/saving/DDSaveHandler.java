package org.dimdev.dimdoors.saving;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraftforge.common.DimensionManager;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.Point3D;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.dungeon.DungeonData;
import org.dimdev.dimdoors.helpers.DungeonHelper;
import org.dimdev.dimdoors.util.DDLogger;
import org.dimdev.dimdoors.util.FileFilters;
import org.dimdev.dimdoors.util.Point4D;

import com.google.common.io.Files;

public class DDSaveHandler {

    public static boolean loadAll() {
        // SenseiKiwi: Loading up our save data is not as simple as just reading files.
        // To properly restore dimensions, we need to make sure we always load
        // a dimension's parent and root before trying to load it. We'll use
        // topological sorting to determine the order in which to recreate the
        // dimension objects such that we respect those dependencies.
        // Links must be loaded after instantiating all the dimensions and must
        // be checked against our dimension blacklist.

        // Don't surround this code with try-catch. Our mod should crash if an error
        // occurs at this level, since it could lead to some nasty problems.

        DDLogger.startTimer("Loading data");

        String basePath = DimensionManager.getCurrentSaveRootDirectory() + "/DimensionalDoors/data/";
        File dataDirectory = new File(basePath);

        // Check if the folder exists. If it doesn't, just return.
        if (!dataDirectory.exists()) {
            return true;
        }

        // Legacy's dimension blacklist and personal pocket mapping (blacklist.txt, personalPockets.txt) are no
        // longer read: pockets are cells of the shared dimensions, saved in pockets.json

        // List any dimension data files and read each dimension
        DimDataProcessor reader = new DimDataProcessor();
        HashMap<Integer, PackedDimData> packedDims = new HashMap<Integer, PackedDimData>();
        FileFilter dataFileFilter = new FileFilters.RegexFileFilter("dim_-?\\d+\\.txt");

        File[] dataFiles = dataDirectory.listFiles(dataFileFilter);
        for (File dataFile : dataFiles) {
            PackedDimData packedDim = readDimension(dataFile, reader);
            if (packedDim == null) {
                throw new IllegalStateException(
                    "The DD data for " + dataFile.getName()
                        .replace(".txt", "")
                        + " at "
                        + dataFile.getPath()
                        + " is corrupted. Please report this on the MCF or on the DD github issues tracker.");
            }
            packedDims.put(packedDim.ID, packedDim);

        }

        List<PackedLinkData> linksToUnpack = new ArrayList<PackedLinkData>();
        // get the grand list of all links to unpack
        for (PackedDimData packedDim : packedDims.values()) {
            linksToUnpack.addAll(packedDim.Links);
        }
        unpackDimData(packedDims);
        unpackLinkData(linksToUnpack);

        return true;
    }

    /**
     * Takes a list of packedDimData and rebuilds the DimData for it. Legacy's pocket dimensions are skipped.
     *
     * @param packedDims
     * @return
     */
    public static boolean unpackDimData(HashMap<Integer, PackedDimData> packedDims) {
        for (PackedDimData packedDim : packedDims.values()) {
            PocketManager.registerPackedDimData(packedDim);
        }
        return true;
    }

    public static boolean unpackLinkData(List<PackedLinkData> linksToUnpack) {
        Point3D fakePoint = new Point3D(-1, -1, -1);
        List<PackedLinkData> unpackedLinks = new ArrayList<PackedLinkData>();
        /**
         * sort through the list, unpacking links that do not have parents.
         */
        // TODO- what we have a loop of links?
        for (PackedLinkData packedLink : linksToUnpack) {
            if (packedLink.parent.equals(fakePoint)) {
                // Links in Legacy's pocket dimensions are lost with them
                NewDimData data = PocketManager.getDimensionData(packedLink.source.getDimension());
                if (data != null) {
                    LinkType linkType = LinkType.getLinkTypeFromIndex(packedLink.tail.linkType);
                    DimLink link = data
                        .createLink(packedLink.source, linkType, packedLink.orientation, packedLink.lock);
                    Point4D destination = packedLink.tail.destination;
                    if (destination != null) {
                        data.pointLinkAt(link, destination);
                    }
                }
                unpackedLinks.add(packedLink);
            }
        }
        linksToUnpack.removeAll(unpackedLinks);

        // unpack remaining children
        while (!linksToUnpack.isEmpty()) {
            for (PackedLinkData packedLink : linksToUnpack) {
                NewDimData data = PocketManager.getDimensionData(packedLink.source.getDimension());
                if (data != null && data.getLink(packedLink.parent) != null) {
                    data.createChildLink(packedLink.source, data.getLink(packedLink.parent), packedLink.lock);
                }
                unpackedLinks.add(packedLink);
            }
            linksToUnpack.removeAll(unpackedLinks);
        }
        return true;
    }

    private static PackedDimData readDimension(File dataFile, DimDataProcessor reader) {
        try {
            return reader.readFromFile(dataFile);
        } catch (Exception e) {
            System.err.println("Could not read dimension data from: " + dataFile.getAbsolutePath());
            System.err.println("The following error occurred:");
            printException(e, false);
            return null;
        }
    }

    public static boolean saveAll(Iterable<? extends IPackable<PackedDimData>> dimensions, boolean checkModified)
        throws IOException {
        // Create the data directory for our dimensions
        // Don't catch exceptions here. If we can't create this folder,
        // the mod should crash to let the user know early on.

        // Get the save directory path
        File saveDirectory = new File(DimDoors.instance.getCurrentSavePath() + "/DimensionalDoors/data/");
        String savePath = saveDirectory.getAbsolutePath();
        String baseSavePath = savePath + "/dim_";
        File backupDirectory = new File(savePath + "/backup");
        String baseBackupPath = backupDirectory.getAbsolutePath() + "/dim_";

        if (!saveDirectory.exists()) {
            // Create the save directory
            Files.createParentDirs(saveDirectory);
            saveDirectory.mkdir();
        }
        if (!backupDirectory.exists()) {
            // Create the backup directory
            backupDirectory.mkdir();
        }

        // Write the dimension save data
        boolean succeeded = true;
        DimDataProcessor writer = new DimDataProcessor();
        for (IPackable<PackedDimData> dimension : dimensions) {
            // Check if the dimension should be saved
            if (!checkModified || dimension.isModified()) {
                if (writeDimension(dimension, writer, baseSavePath, baseBackupPath)) {
                    dimension.clearModified();
                } else {
                    succeeded = false;
                }
            }
        }

        return succeeded;
    }

    private static boolean writeDimension(IPackable<PackedDimData> dimension, DimDataProcessor writer, String basePath,
        String backupPath) {
        try {
            File saveFile = new File(basePath + dimension.name() + ".txt");

            // If the save file already exists, back it up.
            if (saveFile.exists()) {
                Files.move(saveFile, new File(backupPath + dimension.name() + ".txt"));
            }

            writer.writeToFile(saveFile, dimension.pack());
            return true;
        } catch (Exception e) {
            System.err
                .println("Could not save data for dimension #" + dimension.name() + ". The following error occurred:");
            printException(e, true);
            return false;
        }
    }

    private static void printException(Exception e, boolean verbose) {
        if (e.getCause() == null) {
            if (verbose) {
                e.printStackTrace();
            } else {
                System.err.println(e.getMessage());
            }
        } else {
            System.out.println(e.getMessage());
            System.err.println("Caused by an underlying error:");
            if (verbose) {
                e.getCause()
                    .printStackTrace();
            } else {
                System.err.println(
                    e.getCause()
                        .getMessage());
            }
        }
    }

    // TODO - make this more robust
    public static DungeonData unpackDungeonData(PackedDungeonData packedDungeon) {
        return findDungeon(packedDungeon.SchematicName);
    }

    /** A registered dungeon by its schematic name, or null. */
    public static DungeonData findDungeon(String schematicName) {
        for (DungeonData data : DungeonHelper.instance()
            .getRegisteredDungeons()) {
            if (data.schematicName()
                .equals(schematicName)) {
                return data;
            }
        }
        return null;
    }
}
