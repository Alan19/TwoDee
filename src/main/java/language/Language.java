package language;

import com.google.gson.JsonObject;
import util.GsonHelper;

import java.util.Objects;

public class Language {
    private final String name;
    private final String description;
    private final boolean constellation;
    private final boolean court;
    private final boolean family;
    private final boolean regional;
    private final boolean vulgar;

    public Language(String name) {
        this(name, null, false, false, false, false, false);
    }

    public Language(String name, String description, boolean constellation, boolean court, boolean family,
                    boolean regional, boolean vulgar) {
        this.name = name;
        this.description = description;
        this.constellation = constellation;
        this.court = court;
        this.family = family;
        this.regional = regional;
        this.vulgar = vulgar;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isConstellation() {
        return constellation;
    }

    public boolean isCourt() {
        return court;
    }

    public boolean isFamily() {
        return family;
    }

    public boolean isRegional() {
        return regional;
    }

    public boolean isVulgar() {
        return vulgar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Language language = (Language) o;
        return constellation == language.constellation && court == language.court && family == language.family &&
                regional == language.regional && vulgar == language.vulgar && name.equals(language.name) &&
                Objects.equals(description, language.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, constellation, court, family, regional, vulgar);
    }

    public JsonObject toJson() {
        JsonObject languageObject = new JsonObject();
        languageObject.addProperty("name", this.getName());
        if (this.getDescription() != null && !this.getDescription().isEmpty()) {
            languageObject.addProperty("description", this.getDescription());
        }

        if (this.isConstellation()) {
            languageObject.addProperty("constellation", true);
        }
        if (this.isCourt()) {
            languageObject.addProperty("court", true);
        }
        if (this.isFamily()) {
            languageObject.addProperty("family", true);
        }
        if (this.isRegional()) {
            languageObject.addProperty("regional", true);
        }
        if (this.isVulgar()) {
            languageObject.addProperty("vulgar", true);
        }
        return languageObject;
    }

    public static Language fromJson(JsonObject jsonObject) {
        return new Language(
                GsonHelper.getAsString(jsonObject, "name", ""),
                GsonHelper.getAsString(jsonObject, "description", ""),
                GsonHelper.getAsBoolean(jsonObject, "constellation", false),
                GsonHelper.getAsBoolean(jsonObject, "court", false),
                GsonHelper.getAsBoolean(jsonObject, "family", false),
                GsonHelper.getAsBoolean(jsonObject, "regional", false),
                GsonHelper.getAsBoolean(jsonObject, "vulgar", false)
        );
    }
}
