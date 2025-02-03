package com.gmail.necnionch.myplugin.testprotocollib.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.*;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.PreviewListener;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.ReplaceItemEntityListener;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

public final class TestPlugin extends JavaPlugin {
    private final Logger log = getLogger();

    private static PacketType[] getPreviewPacketTypes() {
        List<PacketType> ignores = Arrays.asList(
                PacketType.Play.Server.ENTITY,
                PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
                PacketType.Play.Server.SPAWN_ENTITY_WEATHER,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.UPDATE_ENTITY_NBT
        );
        return StreamSupport.stream(PacketType.values().spliterator(), false)
                .filter(t -> !ignores.contains(t))
                .filter(t -> t.name().contains("ENTITY"))
                .toArray(PacketType[]::new);
    }

    @Override
    public void onEnable() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PreviewListener(this, getPreviewPacketTypes()));
        ReplaceItemEntityListener replacer = new ReplaceItemEntityListener(this, manager);
        manager.addPacketListener(replacer);

        getCommand("sendd").setExecutor((sender, command, label, args) -> {
            Player p = sender instanceof Player ? ((Player) sender) : null;

            if (2 <= args.length && "1".equalsIgnoreCase(args[0])) {
                int entityId = Integer.parseInt(args[1]);

                PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                newPacket.getIntegers().write(0, entityId);

                Vector3F value = new Vector3F(45f, 45f, 45f);
                Object convertValue = Vector3F.getConverter().getGeneric(value);
                newPacket.getDataValueCollectionModifier().write(0, Arrays.asList(
                        new WrappedDataValue(18, WrappedDataWatcher.Registry.getVectorSerializer(), convertValue),
                        new WrappedDataValue(19, WrappedDataWatcher.Registry.getVectorSerializer(), convertValue)
                ));
                replacer.sendServerPacket(p, newPacket);

            }

            return true;
        });
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }
}
