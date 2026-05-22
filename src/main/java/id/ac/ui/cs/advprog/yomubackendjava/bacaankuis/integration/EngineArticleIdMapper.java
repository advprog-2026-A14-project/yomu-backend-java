package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class EngineArticleIdMapper {
    private static final String ARTICLE_ID_NAMESPACE = "yomu-article:";

    private EngineArticleIdMapper() {
    }

    public static String toEngineArticleId(String articleId) {
        if (articleId == null || articleId.isBlank()) {
            return articleId;
        }

        String normalized = articleId.trim();
        try {
            return UUID.fromString(normalized).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes((ARTICLE_ID_NAMESPACE + normalized).getBytes(StandardCharsets.UTF_8))
                    .toString();
        }
    }
}
