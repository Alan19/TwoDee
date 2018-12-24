package doom;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class DoomHandler {
    private String message;

    public DoomHandler(String message) {
        this.message = message;
    }

    //Generates an embed of the new doom value
    public EmbedBuilder newDoom() {
        String[] args = message.split(" ");
        DoomWriter doomWriter = new DoomWriter();
        if (args.length == 1) {
            return new DoomWriter().generateDoomEmbed();
        }
        if (args.length != 3) {
            return new EmbedBuilder()
                    .setTitle("Invalid command!");
        } else {
            String commandType = args[1];
            int doomVal = Integer.parseInt(args[2]);

            switch (commandType) {
                case "add":
                    return doomWriter.addDoom(doomVal);

                case "sub":
                    return doomWriter.addDoom(doomVal * -1);

                case "set":
                    return doomWriter.setDoom(doomVal);

                default:
                    return new EmbedBuilder()
                            .setTitle("Invalid command!");
            }
        }
    }
}
