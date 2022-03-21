package linguistics;

public class Language {
    private final String name;
    private String description;
    private boolean constellation;
    private boolean court;
    private boolean regional;
    private boolean vulgar;
    private boolean family;

    public Language(String name) {
        this.name = name;
        this.description = "";
        this.constellation = false;
        this.court = false;
        this.regional = false;
        this.vulgar = false;
        this.family = false;
    }

    public String getName() {
        return name;
    }

    public boolean isConstellation() {
        return constellation;
    }

    public Language setConstellation() {
        this.constellation = true;
        return this;
    }

    public boolean isCourt() {
        return court;
    }

    public Language setCourt() {
        this.court = true;
        return this;
    }

    public boolean isRegional() {
        return regional;
    }

    public Language setRegional() {
        this.regional = true;
        return this;
    }

    public boolean isVulgar() {
        return vulgar;
    }

    public Language setVulgar() {
        this.vulgar = true;
        return this;
    }

    public boolean isFamily() {
        return family;
    }

    public Language setFamily() {
        this.family = true;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Language setDescription(String description) {
        this.description = description;
        return this;
    }
}
