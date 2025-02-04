package com.gmail.necnionch.myplugin.testprotocollib.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.PreviewListener;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.ReplaceItemEntityListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

public final class TestPlugin extends JavaPlugin {
    private final Logger log = getLogger();
    private ReplaceItemEntityListener replacer;

    private static PacketType[] getPreviewPacketTypes() {
        List<PacketType> ignores = Arrays.asList(
                PacketType.Play.Server.ENTITY,
                PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
                PacketType.Play.Server.SPAWN_ENTITY_WEATHER,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.UPDATE_ENTITY_NBT,
                PacketType.Play.Server.MAP_CHUNK
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
        replacer = new ReplaceItemEntityListener(this, manager);
        manager.addPacketListener(replacer);

        getCommand("sendd").setExecutor((sender, command, label, args) -> {
            Player p = sender instanceof Player ? ((Player) sender) : null;

//            if (2 <= args.length && "1".equalsIgnoreCase(args[0])) {
//            }

            return true;
        });
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        if (replacer != null) {
            replacer.clearAll();
        }
    }
}
