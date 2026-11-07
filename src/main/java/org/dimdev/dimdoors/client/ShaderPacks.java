package org.dimdev.dimdoors.client;

import java.lang.reflect.Method;

import cpw.mods.fml.common.Loader;

/** Whether a shader pack draws the world, through the Iris API that Angelica includes. False without Angelica. */
public final class ShaderPacks {

    private static final String UNKNOWN = "?";
    private static Method packName;

    private static Object api;
    private static Method inUse;
    private static boolean looked;

    private ShaderPacks() {}

    public static boolean inUse() {
        if (!looked) {
            looked = true;
            if (Loader.isModLoaded("angelica")) {
                try {
                    Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    api = irisApi.getMethod("getInstance")
                        .invoke(null);
                    inUse = irisApi.getMethod("isShaderPackInUse");
                } catch (ReflectiveOperationException | LinkageError e) {
                    api = null;
                }
            }
        }
        if (api == null) return false;
        try {
            return (Boolean) inUse.invoke(api);
        } catch (ReflectiveOperationException e) {
            api = null;
            return false;
        }
    }

    /** The name of the pack in use, as Angelica lists it. */
    public static String name() {
        try {
            if (packName == null) packName = Class.forName("net.coderbot.iris.Iris")
                .getMethod("getCurrentPackName");
            Object name = packName.invoke(null);
            return name != null ? name.toString() : UNKNOWN;
        } catch (ReflectiveOperationException | LinkageError e) {
            return UNKNOWN;
        }
    }
}
