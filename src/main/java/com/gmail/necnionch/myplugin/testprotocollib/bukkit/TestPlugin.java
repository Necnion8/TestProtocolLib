package com.gmail.necnionch.myplugin.testprotocollib.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.PreviewListener;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.ReplaceItemEntityListener;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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
        manager.addPacketListener(new ReplaceItemEntityListener(this, manager));

        getCommand("sendd").setExecutor((sender, command, label, args) -> {
            Player p = sender instanceof Player ? ((Player) sender) : null;

            if (2 <= args.length && "1".equalsIgnoreCase(args[0])) {
                int entityId = Integer.parseInt(args[1]);
                ItemStack itemStack = new ItemStack(Material.APPLE);

                try {
                    PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
                    newPacket.getIntegers().write(0, entityId);
                    List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = Lists.newArrayList();
                    pairs.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, itemStack));
                    newPacket.getSlotStackPairLists().write(0, pairs);

                    log.warning("SEND ENTITY EQUIPMENT");
                    manager.sendServerPacket(p, newPacket);

                } catch (Throwable e) {
                    e.printStackTrace();
                }


            }

            return true;
        });
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }
}
