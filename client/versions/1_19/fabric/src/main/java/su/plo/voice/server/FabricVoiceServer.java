package su.plo.voice.server;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.server.connection.FabricServerChannelHandler;
import su.plo.voice.server.event.player.PlayerJoinEvent;
import su.plo.voice.server.event.player.PlayerQuitEvent;
import su.plo.voice.server.player.PermissionSupplier;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FabricVoiceServer extends ModVoiceServer implements ModInitializer {

    private final FabricServerChannelHandler handler = new FabricServerChannelHandler(this);

    @Override
    public void onInitialize() {
        eventBus.register(this, handler);

        ServerLifecycleEvents.SERVER_STARTED.register(this::onInitialize);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onShutdown);

        // todo: мб переместить в отдельный файл, но наверное и тут норм.
        //  если надумаю куда-то переместить, то уберу Kappa
        ServerPlayConnectionEvents.JOIN.register((handler, sender, mcServer) ->
                eventBus.call(new PlayerJoinEvent(handler.getPlayer(), handler.getPlayer().getUUID()))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, mcServer) ->
                eventBus.call(new PlayerQuitEvent(handler.getPlayer(), handler.getPlayer().getUUID()))
        );

        S2CPlayChannelEvents.REGISTER.register(handler);
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, handler);
    }

    @Override
    protected void onShutdown(MinecraftServer server) {
        super.onShutdown(server);
        handler.clear();
    }

    @Override
    public int getMinecraftServerPort() {
        return server != null ? server.getPort() : -1;
    }

    @Override
    public @NotNull String getVersion() {
        ModContainer modContainer = FabricLoader.getInstance()
                .getModContainer(modId)
                .orElse(null);
        checkNotNull(modContainer, "modContainer cannot be null");
        return modContainer.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    protected File configFolder() {
        return new File("config/" + modId);
    }

    @Override
    protected File modsFolder() {
        return new File("mods");
    }

    @Override
    protected File addonsFolder() {
        return new File(configFolder(), "addons");
    }

    @Override
    protected PermissionSupplier createPermissionSupplier() {
        return (player, permission) -> {
            if (!(player instanceof ServerPlayer serverPlayer))
                throw new IllegalArgumentException("player is not " + ServerPlayer.class);
            return Permissions.check(serverPlayer, permission);
        };
    }
}
