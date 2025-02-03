package com.gmail.necnionch.myplugin.testprotocollib.bukkit;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class MyUtil {

    public static WrappedDataWatcher.Serializer vectorSerializer = WrappedDataWatcher.Registry.getVectorSerializer();
    public static EquivalentConverter<Vector3F> vector3FConverter = Vector3F.getConverter();

    public static WrappedDataValue vec3FtoWrappedDataValue(int index, Vector3F value) {
        return new WrappedDataValue(index, vectorSerializer, vector3FConverter.getGeneric(value));
    }

}
