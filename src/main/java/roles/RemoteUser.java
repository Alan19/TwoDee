package roles;

import com.google.gson.reflect.TypeToken;

import javax.annotation.Nullable;
import java.util.List;

public record RemoteUser(
        long id,
        String name,
        String characterPermission,
        String doomPermission,
        String userPermission,
        String tokenPermission,
        @Nullable String externalId,
        String notes,
        boolean active,
        @Nullable Long createdBy
) {
    public static final TypeToken<List<RemoteUser>> LIST_TYPE_TOKEN = new TypeToken<>() {
    };
}
