package org.dimdev.dimdoors.helpers;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.IDimRegistrationCallback;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.util.Point4D;
import org.dimdev.dimdoors.watcher.ClientLinkData;

import io.netty.buffer.ByteBuf;

public class Compactor {

    @SuppressWarnings("unused") // ?
    private static class DimComparator implements Comparator<NewDimData> {

        @Override
        public int compare(NewDimData a, NewDimData b) {
            return a.id() - b.id();
        }
    }

    public static void write(Collection<? extends NewDimData> values, ByteBuf output) throws IOException {
        // SenseiKiwi: Just encode the data straight up for now. I'll implement fancier compression later.
        output.writeInt(values.size());
        for (NewDimData dimension : values) {
            output.writeInt(dimension.id());
            output.writeInt(dimension.type().index);
            output.writeInt(dimension.linkCount());
            for (DimLink link : dimension.links()) {
                (new ClientLinkData(link)).write(output);
            }
        }

        /*
         * // To compress the dimension IDs, we'll sort them by ID
         * // and write the _difference_ between their ID numbers.
         * NewDimData[] dimensions = new NewDimData[values.size()];
         * dimensions = values.toArray(dimensions);
         * Arrays.sort(dimensions, new DimComparator());
         */
    }

    public static void readDimensions(ByteBuf input, IDimRegistrationCallback callback) throws IOException {
        // Read in the dimensions one by one
        int dimCount = input.readInt();
        for (int k = 0; k < dimCount; k++) {
            int id = input.readInt();
            DimensionType type = DimensionType.getTypeFromIndex(input.readInt());
            NewDimData dimension = callback.registerDimension(id, type);
            int linkCount = input.readInt();
            for (int h = 0; h < linkCount; h++) {
                ClientLinkData link = ClientLinkData.read(input);
                Point4D source = link.point;
                dimension.createLink(source.getX(), source.getY(), source.getZ(), LinkType.CLIENT, 0);
            }
        }
    }
}
