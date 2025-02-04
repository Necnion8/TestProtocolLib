package com.gmail.necnionch.myplugin.testprotocollib.geyserextension;

import com.gmail.necnionch.myplugin.testprotocollib.geyserextension.entity.ExtendArmorStandEntity;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TestExtension implements Extension {

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        EntityDefinition<ExtendArmorStandEntity> myStand = EntityDefinition.inherited(ExtendArmorStandEntity::new, EntityDefinitions.ARMOR_STAND)
//                .identifier("minecraft:armor_stand")
                .type(EntityType.ARMOR_STAND)
                .identifier("testprotocollibext:my_armor_stand")
                .build(false);

        Registries.ENTITY_DEFINITIONS.get().put(myStand.entityType(), myStand);
        Registries.JAVA_ENTITY_IDENTIFIERS.get().put("minecraft:" + myStand.entityType().name().toLowerCase(Locale.ROOT), myStand);
        if (myStand.registeredProperties() != null) {
            Registries.BEDROCK_ENTITY_PROPERTIES.get().add(myStand.registeredProperties().toNbtMap(myStand.identifier()));
        }

        registerIdentifier(myStand.identifier());


        logger().error("HELLO!");
    }

    public void registerIdentifier(String id) {
        NbtMap registry = Registries.BEDROCK_ENTITY_IDENTIFIERS.get();
        List<NbtMap> idList = new ArrayList<>(registry.getList("idlist", NbtType.COMPOUND));
        idList.add(NbtMap.builder()
                .putString("id", id)
                .putString("bid", "")
                .putBoolean("hasspawnegg", false)
                .putInt("rid", idList.size() + 1)
                .putBoolean("summonable", false).build()
        );

        Registries.BEDROCK_ENTITY_IDENTIFIERS.set(NbtMap.builder()
                .putList("idlist", NbtType.COMPOUND, idList).build()
        );
    }

}
