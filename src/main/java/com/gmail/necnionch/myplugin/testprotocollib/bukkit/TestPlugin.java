package com.gmail.necnionch.myplugin.testprotocollib.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.ItemEntityReplacer;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener.PreviewListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public final class TestPlugin extends JavaPlugin implements Listener {
    private ItemEntityReplacer replacer;
    private Function<UUID, Boolean> bedrockCheck;

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
                .filter(t -> !ignores.contains(t) && !t.name().equals("CLIENT_TICK_END"))
//                .filter(t -> t.name().contains("ENTITY"))
                .toArray(PacketType[]::new);
    }

    @Override
    public void onEnable() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PreviewListener(this, getPreviewPacketTypes()));
        replacer = new ItemEntityReplacer(this, manager);
        manager.addPacketListener(replacer);
        getServer().getPluginManager().registerEvents(this, this);
        hookToFloodgateOrGeyser();

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
        bedrockCheck = null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (replacer != null) {
            replacer.removePlayerContext(event.getPlayer());
        }
    }

    public void hookToFloodgateOrGeyser() {
        if (getServer().getPluginManager().isPluginEnabled("Geyser-Spigot")) {
            try {
                GeyserApi api = GeyserApi.api();
                bedrockCheck = api::isBedrockPlayer;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (bedrockCheck == null && getServer().getPluginManager().isPluginEnabled("floodgate")) {
            try {
                FloodgateApi api = FloodgateApi.getInstance();
                bedrockCheck = api::isFloodgatePlayer;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (bedrockCheck == null) {
            getLogger().severe("Unable to hook Geyser-Spigot or floodgate");
        }
    }

    public boolean isBedrockPlayer(UUID playerId) {
        return bedrockCheck != null && bedrockCheck.apply(playerId);
    }

}
