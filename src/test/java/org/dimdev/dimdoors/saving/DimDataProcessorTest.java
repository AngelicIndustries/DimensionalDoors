package org.dimdev.dimdoors.saving;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

import org.dimdev.dimdoors.Point3D;
import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.util.ConfigurationProcessingException;
import org.dimdev.dimdoors.util.Point4D;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DimDataProcessorTest {

    private static PackedDimData dimension(boolean stabilized) {
        PackedLinkData link = new PackedLinkData(
            new Point4D(1, 64, 2, 0),
            new Point3D(-1, -1, -1),
            new PackedLinkTail(null, LinkType.NORMAL),
            0,
            new ArrayList<Point3D>(),
            null,
            stabilized);
        return new PackedDimData(
            0,
            0,
            0,
            0,
            0,
            0,
            DimensionType.ROOT,
            false,
            null,
            new Point3D(0, 64, 0),
            new ArrayList<Integer>(),
            Collections.singletonList(link),
            new ArrayList<PackedLinkTail>());
    }

    private static String write(PackedDimData data) throws ConfigurationProcessingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DimDataProcessor().writeToStream(out, data);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static PackedDimData read(String json) throws ConfigurationProcessingException {
        return new DimDataProcessor().readFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void linksKeepTheirStabilization() throws ConfigurationProcessingException {
        assertTrue(read(write(dimension(true))).Links.get(0).stabilized);
        assertFalse(read(write(dimension(false))).Links.get(0).stabilized);
    }

    @Test
    public void linksSavedBeforeStabilizationReadAsUnstable() throws ConfigurationProcessingException {
        JsonObject save = new JsonParser().parse(write(dimension(true)))
            .getAsJsonObject();
        save.getAsJsonArray("Links")
            .get(0)
            .getAsJsonObject()
            .remove("stabilized");
        assertFalse(read(save.toString()).Links.get(0).stabilized);
    }
}
