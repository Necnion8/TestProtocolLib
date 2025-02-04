package com.gmail.necnionch.myplugin.testprotocollib.geyserextension.entity;

import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class ExtendArmorStandEntity extends ArmorStandEntity {
    public ExtendArmorStandEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void spawnEntity() {
        Vector3f javaPosition = this.position;
        this.position = this.position.up(this.getYOffset());

        float x = Math.round(this.motion.getX() * 1000d) / 1000f;
        float y = Math.round(this.motion.getY() * 1000d) / 1000f;
        float z = Math.round(this.motion.getZ() * 1000d) / 1000f;
        boolean custom = x == 2.123f && y == 2.456f && z == 2.789f;

        if (custom) {
            this.motion = Vector3f.ZERO;
        }

        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier(custom ? this.definition.identifier() : EntityDefinitions.ARMOR_STAND.identifier());
        addEntityPacket.setRuntimeEntityId(this.geyserId);
        addEntityPacket.setUniqueEntityId(this.geyserId);
        addEntityPacket.setPosition(this.position);
        addEntityPacket.setMotion(this.motion);
        addEntityPacket.setRotation(Vector2f.from(this.pitch, this.yaw));
        addEntityPacket.setHeadRotation(this.headYaw);
        addEntityPacket.setBodyRotation(this.yaw);
        addEntityPacket.getMetadata().putFlags(this.flags);
        this.dirtyMetadata.apply(addEntityPacket.getMetadata());
        this.addAdditionalSpawnData(addEntityPacket);
        this.valid = true;
        this.session.sendUpstreamPacket(addEntityPacket);
//        this.flagsDirty = false;
        this.position = javaPosition;
    }

}
