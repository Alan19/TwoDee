package configs;

public class RemoteDataSettings {
    private final String url;
    private final String token;

    public RemoteDataSettings() {
        this("", "");
    }

    public RemoteDataSettings(String url, String token) {
        this.url = url;
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }
}
