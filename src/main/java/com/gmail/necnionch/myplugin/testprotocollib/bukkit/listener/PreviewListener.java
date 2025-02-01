package com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public class PreviewListener extends PacketAdapter {

    private final Logger log;

    public PreviewListener(Plugin plugin, PacketType... types) {
        super(plugin, types);
        log = plugin.getLogger();
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        log.info("[->] " + event.getPacketType().name());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        log.info("[<-] " + event.getPacketType().name());
    }

}
