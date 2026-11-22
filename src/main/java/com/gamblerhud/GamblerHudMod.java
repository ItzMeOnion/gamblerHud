package com.gamblerhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class GamblerHudMod implements ClientModInitializer {

    /** Set when a new menu key is captured, so the key press does not also toggle the menu. */
    public static boolean ignoreKeyUntilRelease = false;
    private static boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        Config.load();
        Tracker.load();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) PaymentParser.handle(message.getString());
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("gamblerhud", "overlay"),
                (graphics, tickCounter) -> Hud.render(graphics));

        ClientTickEvents.END_CLIENT_TICK.register(GamblerHudMod::tick);
    }

    private static void tick(Minecraft mc) {
        Auction.tick();

        boolean down = InputConstants.isKeyDown(mc.getWindow(), Config.get().keyCode);
        boolean rising = down && !wasDown;
        wasDown = down;

        if (ignoreKeyUntilRelease) {
            if (!down) ignoreKeyUntilRelease = false;
            return;
        }
        if (!rising) return;

        if (mc.screen == null) {
            mc.setScreen(new MenuScreen());
        } else if (mc.screen instanceof MenuScreen menu && !menu.isTyping()) {
            mc.setScreen(null);
        }
    }
}
