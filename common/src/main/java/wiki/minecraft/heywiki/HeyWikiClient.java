package wiki.minecraft.heywiki;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.client.ClientChatEvent;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import wiki.minecraft.heywiki.command.*;
import wiki.minecraft.heywiki.resource.PageExcerptCacheManager;
import wiki.minecraft.heywiki.resource.PageNameSuggestionCacheManager;
import wiki.minecraft.heywiki.resource.WikiFamilyConfigManager;
import wiki.minecraft.heywiki.resource.WikiTranslationManager;

import java.util.List;
import java.util.Objects;

import static dev.architectury.event.events.client.ClientCommandRegistrationEvent.literal;


public class HeyWikiClient {
    public static final String MOD_ID = "heywiki";
    public static KeyBinding openWikiKey = new KeyBinding("key.heywiki.open", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_H, // The keycode of the key
            "key.categories.heywiki" // The translation key of the keybinding's category.
    );

    private static void registerCommands(CommandDispatcher<ClientCommandSourceStack> dispatcher, CommandRegistryAccess registryAccess) {
        ImFeelingLuckyCommand.register(dispatcher);
        WhatBiomeCommand.register(dispatcher);
        var whatCommandCommand = WhatCommandCommand.register(dispatcher);
        WhatIsThisCommand.register(dispatcher);
        WhatIsThisItemCommand.register(dispatcher);
        var wikiCommand = WikiCommand.register(dispatcher);

        dispatcher.register(literal("whatis").redirect(wikiCommand));
        dispatcher.register(literal("whatcmd").redirect(whatCommandCommand));
    }

    public static void init() {
        HeyWikiConfig.load();

        KeyMappingRegistry.register(openWikiKey);

        ClientCommandRegistrationEvent.EVENT.register(HeyWikiClient::registerCommands);

        ClientChatEvent.RECEIVED.register(ChatWikiLinks::onClientChatReceived);

        ClientGuiEvent.DEBUG_TEXT_RIGHT.register(CrosshairRaycast::onDebugTextRight);

        ClientTickEvent.CLIENT_POST.register(CrosshairRaycast::onClientTickPost);

        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, new WikiFamilyConfigManager(), Identifier.of("heywiki", "family"));
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, new WikiTranslationManager(), Identifier.of("heywiki", "translation"), List.of(Objects.requireNonNull(Identifier.of("heywiki", "family"))));
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, new PageNameSuggestionCacheManager(), Identifier.of("heywiki", "page_name_suggestions"));
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, new PageExcerptCacheManager(), Identifier.of("heywiki", "page_excerpts"));
    }
}