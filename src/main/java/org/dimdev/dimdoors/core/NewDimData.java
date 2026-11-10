package org.dimdev.dimdoors.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.item.ItemStack;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.Point3D;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.saving.IPackable;
import org.dimdev.dimdoors.saving.PackedDimData;
import org.dimdev.dimdoors.saving.PackedLinkData;
import org.dimdev.dimdoors.saving.PackedLinkTail;
import org.dimdev.dimdoors.util.Point4D;
import org.dimdev.dimdoors.watcher.ClientLinkData;
import org.dimdev.dimdoors.watcher.IUpdateWatcher;

/**
 * The links of one dimension: a real one, Limbo or one of the shared pocket dimensions. The pockets themselves (their
 * tree, entrances and dungeons) are in the pockets package.
 */
public abstract class NewDimData implements IPackable<PackedDimData> {

    private static class InnerDimLink extends DimLink {

        public InnerDimLink(Point4D source, DimLink parent, int orientation, DDLock lock) {
            super(source, orientation, lock, parent);
        }

        public InnerDimLink(Point4D source, LinkType linkType, int orientation, DDLock lock) {
            super(source, orientation, lock, linkType);
        }

        public void setDestination(int x, int y, int z, NewDimData dimension) {
            tail.setDestination(new Point4D(x, y, z, dimension.id()));
        }

        public boolean overwrite(InnerDimLink nextParent, int orientation) {
            if (nextParent == null) {
                throw new IllegalArgumentException("nextParent cannot be null.");
            }
            if (this == nextParent) {
                // Ignore this request silently
                return false;
            }
            if (nextParent.point.getDimension() != point.getDimension()) {
                // Ban having children in other dimensions to avoid serialization issues with cross-dimensional tails
                throw new IllegalArgumentException("source and parent.source must have the same dimension.");
            }

            // Release children
            for (DimLink child : children) {
                child.parent = null;
            }
            children.clear();

            // Release parent
            if (parent != null) {
                parent.children.remove(this);
            }

            // Attach to new parent
            parent = nextParent;
            tail = nextParent.tail;
            nextParent.children.add(this);
            this.orientation = orientation;
            return true;
        }

        public void overwrite(LinkType linkType, int orientation) {
            // Release children
            for (DimLink child : children) {
                child.parent = null;
            }
            children.clear();

            // Release parent
            if (parent != null) {
                parent.children.remove(this);
            }

            // Attach to new parent
            parent = null;
            tail = new LinkTail(linkType, null);
            // Set new orientation
            this.orientation = orientation;
        }

        /**
         * only use this on the client to update errything
         *
         * @param lock
         */
        public void setLock(DDLock lock) {
            this.lock = lock;
        }

        /**
         * create a lock from a key. Returns false if this door already has a lock, or if they has already locked a door
         *
         * @param itemStack
         * @return
         */
        public boolean createLock(ItemStack itemStack, int lockKey) {
            if (this.hasLock() || DDLock.hasCreatedLock(itemStack)) {
                return false;
            }
            this.lock = DDLock.generateLockKeyPair(itemStack, lockKey);
            return true;
        }

        public void removeLock(ItemStack itemStack, InnerDimLink link) {
            if (link.doesKeyUnlock(itemStack)) {
                link.lock = null;
            }
        }

    }

    private static final int EXPECTED_LINKS_PER_CHUNK = 2;
    protected static Random random = new Random();

    protected int id;
    protected Map<Point4D, InnerDimLink> linkMapping;
    protected List<InnerDimLink> linkList;
    protected DimensionType type;
    protected boolean modified;
    public IUpdateWatcher<ClientLinkData> linkWatcher;

    // Don't write this field to a file - it should be recreated on startup
    private final Map<ChunkCoordIntPair, List<InnerDimLink>> chunkMapping;

    protected NewDimData(int id, DimensionType type, IUpdateWatcher<ClientLinkData> linkWatcher) {
        this.id = id;
        this.linkMapping = new TreeMap<Point4D, InnerDimLink>(); // Should be stored in oct tree -- temporary solution
        this.linkList = new ArrayList<InnerDimLink>(); // Should be stored in oct tree -- temporary solution
        this.type = type;
        this.linkWatcher = linkWatcher;
        this.chunkMapping = new HashMap<ChunkCoordIntPair, List<InnerDimLink>>();
        this.modified = true;
    }

    public DimLink findNearestRift(World world, int range, int x, int y, int z) {
        // Sanity check...
        if (world.provider.dimensionId != id) {
            throw new IllegalArgumentException(
                "Attempted to search for links in a World instance for a different dimension!");
        }

        // Note: Only detect rifts at a distance > 0, so we ignore the rift
        // at the center of the search space.
        DimLink link;
        DimLink nearest = null;

        int i, j, k;
        int distance;
        int minDistance = Integer.MAX_VALUE;
        DDProperties properties = DDProperties.instance();

        for (i = -range; i <= range; i++) {
            for (j = -range; j <= range; j++) {
                for (k = -range; k <= range; k++) {
                    distance = getAbsoluteSum(i, j, k);
                    if (distance > 0 && distance < minDistance
                        && world.getBlock(x + i, y + j, z + k) == DimDoors.blockRift) {
                        link = getLink(x + i, y + j, z + k);
                        if (link != null) {
                            nearest = link;
                            minDistance = distance;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    public ArrayList<DimLink> findRiftsInRange(World world, int range, int x, int y, int z) {
        // Sanity check...
        if (world.provider.dimensionId != id) {
            throw new IllegalArgumentException(
                "Attempted to search for links in a World instance for a different dimension!");
        }

        // Note: Only detect rifts at a distance > 0, so we ignore the rift
        // at the center of the search space.
        int i, j, k;
        int distance;
        DimLink link;
        DDProperties properties = DDProperties.instance();
        ArrayList<DimLink> links = new ArrayList<DimLink>();

        for (i = -range; i <= range; i++) {
            for (j = -range; j <= range; j++) {
                for (k = -range; k <= range; k++) {
                    distance = getAbsoluteSum(i, j, k);
                    if (distance > 0 && world.getBlock(x + i, y + j, z + k) == DimDoors.blockRift) {
                        link = getLink(x + i, y + j, z + k);
                        if (link != null) {
                            links.add(link);
                        }
                    }
                }
            }
        }

        return links;
    }

    private static int getAbsoluteSum(int i, int j, int k) {
        return Math.abs(i) + Math.abs(j) + Math.abs(k);
    }

    public DimLink createLink(int x, int y, int z, LinkType linkType, int orientation) {
        return createLink(new Point4D(x, y, z, id), linkType, orientation, null);
    }

    public DimLink createLink(Point4D source, LinkType linkType, int orientation, DDLock locked) {
        // Return an existing link if there is one to avoid creating multiple links starting at the same point.
        InnerDimLink link = linkMapping.get(source);
        if (link == null) {
            link = new InnerDimLink(source, linkType, orientation, locked);
            linkMapping.put(source, link);
            linkList.add(link);

            // If this code is running on the server side, add this link to chunkMapping.
            if (linkType != LinkType.CLIENT) {
                ChunkCoordIntPair chunk = link.getChunkCoordinates();
                List<InnerDimLink> chunkLinks = chunkMapping.get(chunk);
                if (chunkLinks == null) {
                    chunkLinks = new ArrayList<InnerDimLink>(EXPECTED_LINKS_PER_CHUNK);
                    chunkMapping.put(chunk, chunkLinks);
                }
                chunkLinks.add(link);
            }
        } else {
            link.overwrite(linkType, orientation);
        }
        modified = true;

        // Link created!
        if (linkType != LinkType.CLIENT) {
            linkWatcher.onCreated(new ClientLinkData(link));
        }
        return link;
    }

    public DimLink createChildLink(int x, int y, int z, DimLink parent) {
        return createChildLink(new Point4D(x, y, z, id), parent, null);
    }

    public DimLink createChildLink(Point4D source, DimLink parent, DDLock locked) {
        // To avoid having multiple links at a single point, if we find an existing link then we overwrite
        // its destination data instead of creating a new instance.

        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null.");
        }
        InnerDimLink link = linkMapping.get(source);
        if (link == null) {
            link = new InnerDimLink(source, parent, parent.orientation, locked);
            linkMapping.put(source, link);
            linkList.add(link);

            // If this code is running on the server side, add this link to chunkMapping.
            // Granted, the client side code should never create child links anyway...
            if (link.linkType() != LinkType.CLIENT) {
                ChunkCoordIntPair chunk = link.getChunkCoordinates();
                List<InnerDimLink> chunkLinks = chunkMapping.get(chunk);
                if (chunkLinks == null) {
                    chunkLinks = new ArrayList<InnerDimLink>(EXPECTED_LINKS_PER_CHUNK);
                    chunkMapping.put(chunk, chunkLinks);
                }
                chunkLinks.add(link);
            }

            // Link created!
            linkWatcher.onCreated(new ClientLinkData(link));
        } else {
            if (link.overwrite((InnerDimLink) parent, parent.orientation)) {
                // Link created!
                linkWatcher.onCreated(new ClientLinkData(link));
            }
        }
        modified = true;
        return link;
    }

    public boolean deleteLink(DimLink link) {
        if (link.source()
            .getDimension() != id) {
            throw new IllegalArgumentException("Attempted to delete a link from another dimension.");
        }
        InnerDimLink target = linkMapping.remove(link.source());
        if (target != null) {
            linkList.remove(target);

            // If this code is running on the server side, remove this link to chunkMapping.
            if (link.linkType() != LinkType.CLIENT) {
                ChunkCoordIntPair chunk = target.getChunkCoordinates();
                List<InnerDimLink> chunkLinks = chunkMapping.get(chunk);
                if (chunkLinks != null) {
                    chunkLinks.remove(target);
                }
            }

            // Raise deletion event
            if (linkWatcher != null) linkWatcher.onDeleted(new ClientLinkData(link));
            target.clear();
            modified = true;
        }
        return (target != null);
    }

    public boolean deleteLink(int x, int y, int z) {
        return this.deleteLink(this.getLink(x, y, z));
    }

    public boolean deleteLink(Point4D location) {
        return this.deleteLink(this.getLink(location));
    }

    public DimLink getLink(int x, int y, int z) {
        Point4D location = new Point4D(x, y, z, id);
        return linkMapping.get(location);
    }

    public DimLink getLink(Point3D location) {
        return linkMapping.get(new Point4D(location.getX(), location.getY(), location.getZ(), this.id));
    }

    public DimLink getLink(Point4D location) {
        if (location.getDimension() != id) return null;

        return linkMapping.get(location);
    }

    public ArrayList<DimLink> getAllLinks() {
        ArrayList<DimLink> results = new ArrayList<DimLink>(linkMapping.size());
        results.addAll(linkMapping.values());
        return results;
    }

    /** Whether this is one of the shared pocket dimensions. */
    public boolean isPocketDimension() {
        return SharedPockets.isShared(id);
    }

    /** ROOT for a real dimension, otherwise the type of pockets this shared dimension holds. */
    public DimensionType type() {
        return this.type;
    }

    public int id() {
        return id;
    }

    public int linkCount() {
        return linkList.size();
    }

    public Iterable<? extends DimLink> links() {
        return linkList;
    }

    public void setLinkDestination(DimLink incoming, int x, int y, int z) {
        InnerDimLink link = (InnerDimLink) incoming;
        link.setDestination(x, y, z, this);
        this.modified = true;
    }

    /** Points one of this dimension's links at a destination, which needn't have data of its own. */
    public void pointLinkAt(DimLink link, Point4D destination) {
        link.tail.setDestination(destination);
        this.modified = true;
    }

    public void stabilize(DimLink link) {
        link.stabilized = true;
        modified = true;
    }

    public void lock(DimLink link, boolean locked) {
        InnerDimLink innerLink = (InnerDimLink) link;
        innerLink.lock.setLockState(locked);
        modified = true;
    }

    public void setLock(DimLink link, DDLock lock) {
        InnerDimLink innerLink = (InnerDimLink) link;
        innerLink.setLock(lock);
        modified = true;
    }

    public void createLock(DimLink link, ItemStack item, int lockKey) {
        InnerDimLink innerLink = (InnerDimLink) link;
        innerLink.createLock(item, lockKey);
        modified = true;
    }

    public void removeLock(DimLink link, ItemStack item) {
        InnerDimLink innerLink = (InnerDimLink) link;
        innerLink.removeLock(item, innerLink);
        modified = true;
    }

    public DimLink getRandomLink() {
        if (linkMapping.isEmpty()) {
            throw new IllegalStateException("There are no links to select from in this dimension.");
        }
        if (linkList.size() > 1) {
            return linkList.get(random.nextInt(linkList.size()));
        }
        return linkList.get(0);
    }

    public Iterable<? extends DimLink> getChunkLinks(int chunkX, int chunkZ) {
        List<InnerDimLink> chunkLinks = chunkMapping.get(new ChunkCoordIntPair(chunkX, chunkZ));
        if (chunkLinks != null) {
            return chunkLinks;
        }
        return new ArrayList<InnerDimLink>(0);
    }

    public boolean isModified() {
        return modified;
    }

    public void clearModified() {
        this.modified = false;
    }

    public PackedDimData pack() {
        ArrayList<PackedLinkData> Links = new ArrayList<PackedLinkData>();
        for (DimLink link : this.links()) {
            ArrayList<Point3D> children = new ArrayList<Point3D>();
            Point3D parentPoint = new Point3D(-1, -1, -1);
            if (link.parent != null) {
                parentPoint = link.parent.point.toPoint3D();
            }

            for (DimLink childLink : link.children) {
                children.add(
                    childLink.source()
                        .toPoint3D());
            }
            PackedLinkTail tail = new PackedLinkTail(link.tail.getDestination(), link.tail.getLinkType());
            Links.add(
                new PackedLinkData(
                    link.point,
                    parentPoint,
                    tail,
                    link.orientation,
                    children,
                    link.lock,
                    link.stabilized));
        }
        // The pocket tree fields of Legacy's format are left at a root dimension's values: pockets save on their own
        return new PackedDimData(
            this.id,
            0,
            0,
            this.id,
            this.id,
            0,
            type,
            false,
            null,
            new Point3D(0, 0, 0),
            new ArrayList<Integer>(),
            Links,
            new ArrayList<PackedLinkTail>());
    }

    @Override
    public String name() {
        return String.valueOf(id);
    }

    @Override
    public String toString() {
        return "DimID= " + this.id;
    }
}
