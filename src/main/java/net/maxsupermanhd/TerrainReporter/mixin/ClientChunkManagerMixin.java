package net.maxsupermanhd.TerrainReporter.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import net.maxsupermanhd.TerrainReporter.TerrainReporter;
import net.maxsupermanhd.TerrainReporter.TerrainReporterConfig;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;

@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {
    @Inject(at = @At("TAIL"), method = "loadChunkFromPacket(IILnet/minecraft/world/biome/source/BiomeArray;Lnet/minecraft/network/PacketByteBuf;Lnet/minecraft/nbt/NbtCompound;Ljava/util/BitSet;)Lnet/minecraft/world/chunk/WorldChunk;")
    private void loadChunkFromPacket(int x, int z, BiomeArray biomes, PacketByteBuf buf, NbtCompound nbt, BitSet bitSet, CallbackInfoReturnable<WorldChunk> cir) throws InterruptedException {
        TerrainReporterConfig config = AutoConfig.getConfigHolder(TerrainReporterConfig.class).getConfig();
        if(config.enabled) {
            TerrainReporter.submitChunkTrigger(x, z);
        }
    }
}