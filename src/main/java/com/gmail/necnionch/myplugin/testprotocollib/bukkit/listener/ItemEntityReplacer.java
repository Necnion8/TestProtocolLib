package com.gmail.necnionch.myplugin.testprotocollib.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.gmail.necnionch.myplugin.testprotocollib.bukkit.MyUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class ItemEntityReplacer extends PacketAdapter {
    
    private static final PacketType[] LISTEN_TYPES = new PacketType[] {
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_DESTROY,
            PacketType.Play.Server.COLLECT,
    };
    
    private final ProtocolManager manager;
    //
    private final Map<Player, PlayerContext> contexts = Maps.newHashMap();
    private final Set<Object> ignorePacketHandles = Sets.newHashSet();

    public ItemEntityReplacer(Plugin plugin, ProtocolManager manager) {
        super(plugin, LISTEN_TYPES);
        this.manager = manager;
    }

    private void runTask(Runnable task) {
        getPlugin().getServer().getScheduler().runTask(getPlugin(), task);
    }

    private void runTask(Runnable task, long delay) {
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), task, delay);
    }

    private void sendServerPacket(Player player, PacketContainer packet) {
        ignorePacketHandles.add(packet.getHandle());  // 送信したパケットを自信が再処理しないように無視マークする
        manager.sendServerPacket(player, packet);
    }


    public void clearAll() {
        for (PlayerContext context : contexts.values()) {
            context.clear();
        }
        contexts.clear();
        ignorePacketHandles.clear();
    }

    public PlayerContext getPlayerContext(Player player, boolean create) {
        if (create) {
            return contexts.computeIfAbsent(player, PlayerContext::new);
        }
        return contexts.get(player);
    }

    public PlayerContext removePlayerContext(Player player) {
        PlayerContext context = contexts.remove(player);
        if (context != null) {
            context.clear();
        }
        return context;
    }


    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        // 自信が送信したパケットを再処理するのを防ぐ
        if (ignorePacketHandles.remove(packet.getHandle())) {
            return;
        }

        PlayerContext context = getPlayerContext(event.getPlayer(), true);

        if (PacketType.Play.Server.SPAWN_ENTITY.equals(event.getPacketType())) {
            context.processOnSpawnEntityPacket(packet);

        } else if (PacketType.Play.Server.ENTITY_METADATA.equals(event.getPacketType())) {
            context.processOnEntityMetadataPacket(packet);

        } else if (PacketType.Play.Server.ENTITY_DESTROY.equals(event.getPacketType())) {
            context.processOnEntityDestroyPacket(packet);

        } else if (PacketType.Play.Server.COLLECT.equals(event.getPacketType())) {
            context.processOnCollectPacket(packet);

        }
    }


    public final class PlayerContext {

        private final Player player;
        private final Map<Integer, Integer> itemOfStandIds = Maps.newHashMap();  // Item entity id -> ArmorStand entity id

        public PlayerContext(Player player) {
            this.player = player;
        }

        public void sendServerPacket(PacketContainer packet) {
            ItemEntityReplacer.this.sendServerPacket(player, packet);
        }

        public void sendEntityMetadata(int entityId, List<WrappedDataValue> dataValues) {
            try {
                PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                newPacket.getIntegers().write(0, entityId);
                newPacket.getDataValueCollectionModifier().write(0, dataValues);
                sendServerPacket(newPacket);

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public void sendEntityEquipment(int entityId, List<Pair<EnumWrappers.ItemSlot, ItemStack>> items) {
            try {
                PacketContainer newPacket = manager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
                newPacket.getIntegers().write(0, entityId);
                newPacket.getSlotStackPairLists().write(0, items);
                sendServerPacket(newPacket);

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public void clear() {
            if (itemOfStandIds.isEmpty()) {
                return;
            }

            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntLists().write(0, Lists.newArrayList(itemOfStandIds.values()));
            itemOfStandIds.clear();
            sendServerPacket(packet);
        }


        public void processOnSpawnEntityPacket(PacketContainer packet) {
            EntityType entityType = packet.getEntityTypeModifier().read(0);

            if (!EntityType.DROPPED_ITEM.equals(entityType)) {
                return;
            }

            Integer itemEntityId = packet.getIntegers().read(0);

            // ArmorStandをスポーンさせる
            PacketContainer newPacket = packet.deepClone();
            int standEntityId = (int) (Math.random() * Integer.MAX_VALUE);  // これで大丈夫？
            newPacket.getIntegers().write(0, standEntityId);
            UUID newEntityUniqueId = UUID.randomUUID();
            newPacket.getUUIDs().write(0, newEntityUniqueId);
            newPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
            newPacket.getIntegers().write(1, (int) (2.123 * 8000d));  // hook to geyser extension
            newPacket.getIntegers().write(2, (int) (2.456 * 8000d));  // hook to geyser extension
            newPacket.getIntegers().write(3, (int) (2.789 * 8000d));  // hook to geyser extension
            sendServerPacket(newPacket);

            itemOfStandIds.put(itemEntityId, standEntityId);  // Itemに基づくArmorStandのエンティティIDをマップする

            // ArmorStandをItemエンティティに乗せる
            PacketContainer newPacket2 = manager.createPacket(PacketType.Play.Server.MOUNT);
            newPacket2.getIntegers().write(0, itemEntityId);
            newPacket2.getIntegerArrays().write(0, new int[] { standEntityId });  // passengersパケットを監視しないと他から乗っ取れる可能性あり
            runTask(() -> sendServerPacket(newPacket2));  // Itemエンティティがスポーンした後に実行
        }

        public void processOnEntityMetadataPacket(PacketContainer packet) {
            Integer entityId = packet.getIntegers().read(0);

            if (!itemOfStandIds.containsKey(entityId)) {
                return;
            }
            // METADATAが設定されたのがItemエンティティなら
            Integer standEntityId = itemOfStandIds.get(entityId);

            for (WrappedDataValue dataValue : packet.getDataValueCollectionModifier().read(0)) {
                if (dataValue.getIndex() == 8) {  // ItemエンティティのItemStackが更新されるなら  // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Item
                    ItemStack itemStack = (ItemStack) dataValue.getValue();
                    // ArmorStandにアイテムを装備させる
                    sendEntityEquipment(standEntityId, Collections.singletonList(
                            new Pair<>(EnumWrappers.ItemSlot.MAINHAND, itemStack)
                    ));
                    // ArmorStandの腕の角度を設定する
                    sendEntityMetadata(standEntityId, Collections.singletonList(
                            MyUtil.vec3FtoWrappedDataValue(19, new Vector3F(-90, 0, 0))
                    ));
                    break;
                }
            }
        }

        public void processOnEntityDestroyPacket(PacketContainer packet) {
            List<Integer> entityIds = packet.getIntLists().read(0);

            // Itemエンティティが削除される時に仮想ArmorStandのidも加えて消滅させる。(マップからも削除)
            List<Integer> removingEntityIds = Lists.newArrayList();
            for (Integer entityId : entityIds) {
                if (itemOfStandIds.containsKey(entityId)) {
                    removingEntityIds.add(itemOfStandIds.remove(entityId));
                }
            }
            removingEntityIds.addAll(entityIds);
            packet.getIntLists().write(0, removingEntityIds);
        }

        public void processOnCollectPacket(PacketContainer packet) {
            Integer collected = packet.getIntegers().read(0);

            if (!itemOfStandIds.containsKey(collected)) {
                return;
            }
            // Itemエンティティが拾われたなら
            Integer standEntityId = itemOfStandIds.get(collected);

            PacketContainer newPacket = packet.deepClone();
            newPacket.getIntegers().write(0, standEntityId);
            sendServerPacket(newPacket);
        }

    }

}
