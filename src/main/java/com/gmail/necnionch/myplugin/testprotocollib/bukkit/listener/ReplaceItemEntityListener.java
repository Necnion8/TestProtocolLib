package com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import com.google.common.collect.Lists;
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
    // FIXME: パケットはプレイヤー毎に処理されるため、リストで管理している値もプレイヤー単位で処理すべし
    private final Map<Integer, Integer> itemOfStandIds = Maps.newHashMap();  // Item id : ArmorStand id
    private final Set<Object> ignorePacketHandles = Sets.newHashSet();

    public ReplaceItemEntityListener(Plugin plugin, ProtocolManager manager) {
        super(plugin, LISTEN_TYPES);
        log = plugin.getLogger();
        this.manager = manager;
    }

    private void runTask(Runnable task) {
        getPlugin().getServer().getScheduler().runTask(getPlugin(), task);
    }

    public void sendServerPacket(Player player, PacketContainer packet) {
        ignorePacketHandles.add(packet.getHandle());  // 送信する ENTITY_METADATA を処理しないように無視マークする
        log.warning("SEND PACKET : " + packet.getType().name());
        manager.sendServerPacket(player, packet);
    }


    public void sendEntityMetadata(Player player, int entityId, List<WrappedDataValue> dataValues) {
        try {
            PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            newPacket.getIntegers().write(0, entityId);
            newPacket.getDataValueCollectionModifier().write(0, dataValues);
            sendServerPacket(player, newPacket);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void sendEntityEquipment(Player player, int entityId, List<Pair<EnumWrappers.ItemSlot, ItemStack>> items) {
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

            // Itemエンティティ？
            if (EntityType.DROPPED_ITEM.equals(entityType)) {
                // 仮想ArmorStandをスポーン
                PacketContainer newPacket = packet.deepClone();
                int newEntityId = (int) (Math.random() * Integer.MAX_VALUE);
                newPacket.getIntegers().write(0, newEntityId);
                UUID newEntityUniqueId = UUID.randomUUID();
                newPacket.getUUIDs().write(0, newEntityUniqueId);
                newPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
                runTask(() -> sendServerPacket(event.getPlayer(), newPacket));

                itemOfStandIds.put(entityId, newEntityId);  // Itemに基づくArmorStandのエンティティIDをマップする

                // ArmorStandをItemエンティティに乗せる
                PacketContainer newPacket2 = manager.createPacket(PacketType.Play.Server.MOUNT);
                newPacket2.getIntegers().write(0, entityId);
                newPacket2.getIntegerArrays().write(0, new int[] { newEntityId });  // passengersパケットを監視しないと他から乗っ取れる可能性あり
                runTask(() -> sendServerPacket(event.getPlayer(), newPacket2));

            }

        } else if (PacketType.Play.Server.ENTITY_METADATA.equals(event.getPacketType())) {
            Integer entityId = packet.getIntegers().read(0);
            List<WrappedDataValue> dataValues = packet.getDataValueCollectionModifier().read(0);
            log.warning("eId: " + entityId + ", dataValues size: " + dataValues.size());

            ItemStack itemStack = null;
            for (WrappedDataValue value : dataValues) {
                log.warning("- idx: " + value.getIndex() + ", v: " + value.getValue() + ", rawValue: " + value.getRawValue() + ", serializer: " + value.getSerializer());

                // 仮想ArmorStandを持つItemかつ、Itemエンティティへのアイテム設定情報があれば itemStack に取り出しておく
                if (itemOfStandIds.containsKey(entityId) && value.getIndex() == 8) {  // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Item
                    itemStack = (ItemStack) value.getValue();
                }
            }

            // 仮想ArmorStandを持つItem？
            if (itemOfStandIds.containsKey(entityId)) {
                // Itemエンティティに設定されようとしているItemStackがあるなら
                if (itemStack != null) {
                    // ArmorStandのスロットにItemStackを装備させる (リログ後の反映に1tick待つ必要があった)
                    ItemStack is = itemStack;
                    runTask(() -> sendEntityEquipment(event.getPlayer(), entityId, Collections.singletonList(
                            new Pair<>(EnumWrappers.ItemSlot.HEAD, is)
                    )));
                }
            }

        } else if (PacketType.Play.Server.ENTITY_DESTROY.equals(event.getPacketType())) {
            List<Integer> entityIds = packet.getIntLists().read(0);

            // Itemエンティティが削除される時に仮想ArmorStandも消滅させる。(マップからも削除)
            List<Integer> removingEntityIds = Lists.newArrayList();
            for (Integer entityId : entityIds) {
                if (itemOfStandIds.containsKey(entityId)) {
                    removingEntityIds.add(itemOfStandIds.remove(entityId));
                }
            }
            removingEntityIds.addAll(entityIds);

//            packet.getIntegerArrays().write(0, removingEntityIds.stream().map(Integer::intValue).toArray(int[]::new));
            packet.getIntLists().write(0, removingEntityIds);  // TODO: getIntegerArrays() に write する必要あり？
        }
    }

}
