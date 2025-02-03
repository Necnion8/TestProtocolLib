package com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

public class ReplaceItemEntityListener extends PacketAdapter {
    
    private static final PacketType[] LISTEN_TYPES = new PacketType[] {
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_DESTROY,
    };
    
    private final Logger log;
    private final ProtocolManager manager;
    //
    private final Map<Integer, UUID> replacedEntityUniqueIds = Maps.newHashMap();  // 置換したエンティティID (とUUID)
    private final Set<Object> ignorePacketHandles = Sets.newHashSet();

    public ReplaceItemEntityListener(Plugin plugin, ProtocolManager manager) {
        super(plugin, LISTEN_TYPES);
        log = plugin.getLogger();
        this.manager = manager;
    }

    private void runTask(Runnable task) {
        getPlugin().getServer().getScheduler().runTask(getPlugin(), task);
    }

    private void sendServerPacket(Player player, PacketContainer packet) {
        ignorePacketHandles.add(packet.getHandle());  // 送信する ENTITY_METADATA を処理しないように無視マークする
        log.warning("SEND PACKET : " + packet.getType().name());
        manager.sendServerPacket(player, packet);
    }


    private void sendEntityMetadata(Player player, int entityId, List<WrappedDataValue> dataValues) {
        try {
            PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            newPacket.getIntegers().write(0, entityId);
            newPacket.getDataValueCollectionModifier().write(0, dataValues);
            sendServerPacket(player, newPacket);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void sendEntityEquipment(Player player, int entityId, List<Pair<EnumWrappers.ItemSlot, ItemStack>> items) {
        try {
            PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            newPacket.getIntegers().write(0, entityId);
            newPacket.getSlotStackPairLists().write(0, items);
            sendServerPacket(player, newPacket);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        // 無視マークされたパケット
        if (ignorePacketHandles.remove(packet.getHandle())) {
            log.severe("IGNORED PACKET : " + event.getPacketType().name());
            return;
        }

        if (PacketType.Play.Server.SPAWN_ENTITY.equals(event.getPacketType())) {
            Integer entityId = packet.getIntegers().read(0);
            UUID entityUniqueId = packet.getUUIDs().read(0);
            EntityType entityType = packet.getEntityTypeModifier().read(0);
            Double x = packet.getDoubles().read(0);
            Double y = packet.getDoubles().read(1);
            Double z = packet.getDoubles().read(2);
            log.warning("eId: " + entityId + ", uuid: " + entityUniqueId + ", eType: " + entityType + ", x: " + x + ", y: " + y + ", z: " + z);

            // アイテムエンティティ？
            if (EntityType.DROPPED_ITEM.equals(entityType)) {
                replacedEntityUniqueIds.put(entityId, entityUniqueId);
                // エンティティタイプを防具立てに上書きする
                packet.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);

                // SPAWN_ENTITY を送信した後に、防具立てのデータを設定する
                runTask(() -> sendEntityMetadata(event.getPlayer(), entityId, Arrays.asList(
                        new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20),  // 0x20 = invisible; https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Entity
                        new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), true)  // true = no gravity
                )));
            }

        } else if (PacketType.Play.Server.ENTITY_METADATA.equals(event.getPacketType())) {
            Integer entityId = packet.getIntegers().read(0);
            List<WrappedDataValue> dataValues = packet.getDataValueCollectionModifier().read(0);
            log.warning("eId: " + entityId + ", dataValues size: " + dataValues.size());

            ItemStack itemStack = null;
            for (WrappedDataValue value : dataValues) {
                log.warning("- idx: " + value.getIndex() + ", v: " + value.getValue() + ", rawValue: " + value.getRawValue() + ", serializer: " + value.getSerializer());

                // 置換したItemかつ、Itemエンティティへのアイテム設定情報があれば itemStack に取り出しておく
                if (replacedEntityUniqueIds.containsKey(entityId) && value.getIndex() == 8) {  // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Item
                    itemStack = (ItemStack) value.getValue();
                }
            }

            // 置換したItem？
            if (replacedEntityUniqueIds.containsKey(entityId)) {
                // 本来送信されるItemエンティティのデータをキャンセルする (クライアントにはArmorStandと見えてるため)
                event.setCancelled(true);

                // Itemエンティティに設定されようとしているItemStackがあるなら
                if (itemStack != null) {
                    // ArmorStandのスロットにItemStackを装備させる (リログ後の反映に1tick待つ必要があった)
                    ItemStack is = itemStack;
                    runTask(() -> sendEntityEquipment(event.getPlayer(), entityId, Collections.singletonList(
                            new Pair<>(EnumWrappers.ItemSlot.MAINHAND, is)
                    )));
                }
            }

        } else if (PacketType.Play.Server.ENTITY_DESTROY.equals(event.getPacketType())) {
            List<Integer> entityIds = packet.getIntLists().read(0);
            // エンティティが削除される時、置換したエンティティのIDをリストからも削除する
            entityIds.forEach(replacedEntityUniqueIds.keySet()::remove);

        }
    }
    
    
}
