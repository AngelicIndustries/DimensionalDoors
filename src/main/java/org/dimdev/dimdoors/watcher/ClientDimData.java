package org.dimdev.dimdoors.watcher;

import java.io.IOException;

import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.NewDimData;

import io.netty.buffer.ByteBuf;

public class ClientDimData {

    // We'll use public fields since this is just a data container and it's immutable
    public final int ID;
    public final DimensionType type;

    public ClientDimData(int id, DimensionType type) {
        ID = id;
        this.type = type;
    }

    public ClientDimData(NewDimData dimension) {
        ID = dimension.id();
        this.type = dimension.type();
    }

    public void write(ByteBuf output) throws IOException {
        output.writeInt(ID);
        output.writeInt(type.index);
    }

    public static ClientDimData read(ByteBuf input) throws IOException {
        int id = input.readInt();
        int index = input.readInt();
        return new ClientDimData(id, DimensionType.getTypeFromIndex(index));
    }
}
