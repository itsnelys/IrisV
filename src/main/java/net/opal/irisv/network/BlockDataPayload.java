package net.opal.irisv.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.opal.irisv.Irisv;

public record BlockDataPayload(BlockPos pos, CompoundTag tag) implements CustomPacketPayload {

    // 1. Définition du Type (obligatoire pour playToClient)
    public static final Type<BlockDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "block_data"));
    // 2. Définition du Codec (Explique comment lire/écrire)
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockDataPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBlockPos(payload.pos());
                buffer.writeNbt(payload.tag());
            },
            buffer -> new BlockDataPayload(buffer.readBlockPos(), buffer.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}