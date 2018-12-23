package listeners;

import org.javacord.api.DiscordApi;

/**
 * Makes sure all event listeners have a method to start listening
 */
public interface EventListener {
    void startListening();
}
