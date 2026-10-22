package roles;

import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

public record RemoteCharacter(
        long id,
        String name,
        String sheet,
        long ownerId,
        int plotPoints,
        Map<String, String> skills
) {
    public static final TypeToken<List<RemoteCharacter>> LIST_TYPE_TOKEN = new TypeToken<>() {
    };
}
