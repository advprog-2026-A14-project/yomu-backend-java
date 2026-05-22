package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EngineArticleIdMapperTest {
    @Test
    void toEngineArticleId_whenAlreadyUuid_keepsUuid() {
        UUID articleId = UUID.randomUUID();

        String result = EngineArticleIdMapper.toEngineArticleId(articleId.toString());

        assertEquals(articleId.toString(), result);
    }

    @Test
    void toEngineArticleId_whenSlug_generatesStableUuid() {
        String first = EngineArticleIdMapper.toEngineArticleId("art-berita-001");
        String second = EngineArticleIdMapper.toEngineArticleId("art-berita-001");
        String different = EngineArticleIdMapper.toEngineArticleId("art-berita-002");

        assertDoesNotThrow(() -> UUID.fromString(first));
        assertEquals(first, second);
        assertNotEquals(first, different);
    }
}
