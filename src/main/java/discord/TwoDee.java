package discord;

import configs.Settings;
import doom.DoomHandler;
import language.LanguageLogic;
import listeners.DoomPoolAutocomplete;
import listeners.LanguageAutocompleteListener;
import listeners.PoolAutocompleteListener;
import logic.AwardContextMenu;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.internals.observer.VelenObserver;
import pw.mihou.velen.internals.observer.modes.ObserverMode;
import roles.PlayerHandler;
import slashcommands.SlashCommandRegister;

import java.io.File;

public class TwoDee {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwoDee.class);

    public static void main(String[] args) {

        DoomHandler.setupRemoteDoom();
        String token = Settings.getDiscordSettings().getToken();

        LanguageLogic languageLogic = LanguageLogic.of(new File("resources/languages.json"))
                .onFailure(error -> LOGGER.error("Failed to load language file", error))
                .getOrElse(() -> LanguageLogic.of(
                        graph -> LOGGER.warn("Failed to handle update. Errored loading language file"))
                );

        final Velen velen = SlashCommandRegister.setupVelen(languageLogic);
        new DiscordApiBuilder()
                .setToken(token)
                .setAllIntentsExcept(Intent.GUILD_PRESENCES)
                .setUserCacheEnabled(true)
                .addListener(velen)
                .login()
                .thenCompose(PlayerHandler.getInstance()::uploadPlayersToRemote)
                .thenAccept(api -> {
                    LOGGER.info("You can invite the bot by using the following url: {}&scope=bot%20applications.commands", api.createBotInvite());
                    velen.registerAllSlashCommands(api);
                    //Send startup message
                    Settings.getDiscordSettings().getAnnouncementChannels().forEach(id -> {
                        var channel = api.getTextChannelById(id);
                        if (channel.isPresent()) {
                            channel.get().sendMessage(Settings.getQuotes().getRandomStartupQuote());
                        } else {
                            LOGGER.error("Failed to find channel for ID: {}", id);
                        }
                    });
                    AwardContextMenu.setupContextMenu(api);
                    api.addListener(new LanguageAutocompleteListener(languageLogic));
                    api.addListener(new PoolAutocompleteListener());
                    api.addListener(new DoomPoolAutocomplete());
                    api.addUserContextMenuCommandListener(new AwardContextMenu());
                    new VelenObserver(api, ObserverMode.MASTER).observeAllServers(velen, api);
                    velen.index(true, api).join();
                })
                // Log any exceptions that happened
                .exceptionally(ExceptionLogger.get());
    }
}
