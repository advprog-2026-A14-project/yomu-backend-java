package id.ac.ui.cs.advprog.yomubackendjava.config;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.Comment;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.CommentReaction;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentReactionRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Database seeder for development, local, and seed profiles.
 * Populates all tables in FK dependency order so foreign key
 * constraints are never violated during insertion.
 *
 * Seeding order: users → articles → failed_sync_events → quizzes
 *                → comments → user_attempts → comment_reactions
 *
 * NEVER runs in production — guarded by @Profile.
 */
@Component
@Order(1)
@Profile({"dev", "local", "seed"})
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    // Fixed UUIDs for users — must match Rust backend seeding for cross-backend consistency
    private static final UUID ALICE   = UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final UUID BOB     = UUID.fromString("a2222222-2222-2222-2222-222222222222");
    private static final UUID CHARLIE = UUID.fromString("b3333333-3333-3333-3333-333333333333");
    private static final UUID DIANA   = UUID.fromString("b4444444-4444-4444-4444-444444444444");
    private static final UUID ERIC    = UUID.fromString("b5555555-5555-5555-5555-555555555555");
    private static final UUID FIONA   = UUID.fromString("b6666666-6666-6666-6666-666666666666");
    private static final UUID GEORGE  = UUID.fromString("b7777777-7777-7777-7777-777777777777");
    private static final UUID HANNAH  = UUID.fromString("b8888888-8888-8888-8888-888888888888");

    // Fixed String IDs for articles referenced by quizzes and forum comments
    private static final String ART_ID_1 = "art-001";
    private static final String ART_ID_2 = "art-002";
    private static final String ART_ID_3 = "art-003";
    private static final String ART_ID_4 = "art-004";
    private static final String ART_ID_5 = "art-005";
    private static final String ART_ID_6 = "art-006";

    // BCrypt placeholder hashes for seeded users
    private static final String HASH_ALICE   = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String HASH_BOB     = "$2a$10$VxDGJK5Rl9rLMqFvY1Gq5Oz3qPZ4vXy6w8tHjK2mN3bB5cD7eF9g";
    private static final String HASH_CHARLIE = "$2a$10$R8pQn2MkL5jH7wX3zC6vN9bV0dF1gY4aB8cD2eG6hI0jK2mO4qS";
    private static final String HASH_DIANA   = "$2a$10$T5sU8vW1xY4zA7bC0dE3fG6hI9jK2lM5nO8pQ1rS3uV6wX9yZ0a";
    private static final String HASH_ERIC    = "$2a$10$B3cD5eF7gH9iJ1kL3mN5oP7qR9sT1uV3wX5yZ7aB9cD1eF3gH5i";
    private static final String HASH_FIONA   = "$2a$10$J7kL9mN1oP3qR5sT7uV9wX1yZ3aB5cD7eF9gH1iJ3kL5mN7oP9q";
    private static final String HASH_GEORGE  = "$2a$10$R1sT3uV5wX7yZ9aB1cD3eF5gH7iJ9kL1mN3oP5qR7sT9uV1wX3y";
    private static final String HASH_HANNAH  = "$2a$10$Z5aB7cD9eF1gH3iJ5kL7mN9oP1qR3sT5uV7wX9yZ1aB3cD5eF7g";

    // -----------------------------------------------------------------------
    // Repository dependencies — all via constructor injection
    // -----------------------------------------------------------------------
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final FailedSyncEventRepository failedSyncEventRepository;
    private final QuizRepository quizRepository;
    private final CommentRepository commentRepository;
    private final UserAttemptRepository userAttemptRepository;
    private final CommentReactionRepository commentReactionRepository;

    private static final String CATEGORY_LANGUAGE = "language";

    public DatabaseSeeder(
            UserRepository userRepository,
            ArticleRepository articleRepository,
            FailedSyncEventRepository failedSyncEventRepository,
            QuizRepository quizRepository,
            CommentRepository commentRepository,
            UserAttemptRepository userAttemptRepository,
            CommentReactionRepository commentReactionRepository) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.failedSyncEventRepository = failedSyncEventRepository;
        this.quizRepository = quizRepository;
        this.commentRepository = commentRepository;
        this.userAttemptRepository = userAttemptRepository;
        this.commentReactionRepository = commentReactionRepository;
    }

    // -----------------------------------------------------------------------
    // Entry point — transactional so partial failure rolls back everything
    // -----------------------------------------------------------------------
    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== DatabaseSeeder starting (profile: dev/local/seed) ===");

        // Phase 1: Users (no FKs)
        seedUsers();

        // Phase 2: Articles (no FKs)
        seedArticles();

        // Phase 3: FailedSyncEvents (no FKs)
        seedFailedSyncEvents();

        // Phase 4: Quizzes (FK → articles.id)
        seedQuizzes();

        // Phase 5: Comments (FK → users.user_id, self-referential FK → comments.id)
        seedComments();

        // Phase 6: UserAttempts (FK → users.user_id, FK → quizzes.id)
        seedUserAttempts();

        // Phase 7: CommentReactions (FK → comments.id, FK → users.user_id)
        seedCommentReactions();

        log.info("=== DatabaseSeeder completed successfully ===");
    }

    // =======================================================================
    // PHASE 1 — USERS
    // =======================================================================
    /**
     * Seeds 8 users with FIXED UUIDs that must match the Rust backend.
     * UUID is set via setUserId() before save, bypassing @UuidGenerator.
     * Timestamps (created_at, updated_at) are populated by @PrePersist.
     */
    private void seedUsers() {
        log.info("  [1/7] Seeding 8 users...");

        // Admin users
        userRepository.save(buildUser(ALICE,   "alice_admin", "Alice Admin",   Role.ADMIN,   "alice@yomu.id",   "+6281111111111", HASH_ALICE));
        userRepository.save(buildUser(BOB,     "bob_admin",   "Bob Admin",     Role.ADMIN,   "bob@yomu.id",     "+6282222222222", HASH_BOB));

        // Student (PELAJAR) users
        userRepository.save(buildUser(CHARLIE, "charlie",     "Charlie Student", Role.PELAJAR, "charlie@yomu.id", "+6283333333333", HASH_CHARLIE));
        userRepository.save(buildUser(DIANA,   "diana",       "Diana Student",   Role.PELAJAR, "diana@yomu.id",   "+6284444444444", HASH_DIANA));
        userRepository.save(buildUser(ERIC,    "eric",        "Eric Student",    Role.PELAJAR, "eric@yomu.id",    "+6285555555555", HASH_ERIC));
        userRepository.save(buildUser(FIONA,   "fiona",       "Fiona Student",   Role.PELAJAR, "fiona@yomu.id",   "+6286666666666", HASH_FIONA));
        userRepository.save(buildUser(GEORGE,  "george",      "George Student",  Role.PELAJAR, "george@yomu.id",  "+6287777777777", HASH_GEORGE));
        userRepository.save(buildUser(HANNAH,  "hannah",      "Hannah Student",  Role.PELAJAR, "hannah@yomu.id",  "+6288888888888", HASH_HANNAH));

        log.info("    ✅ 8 users seeded (2 ADMIN, 6 PELAJAR)");
    }

    private UserEntity buildUser(UUID userId, String username, String displayName, Role role, String email, String phone, String hash) {
        UserEntity u = new UserEntity();
        // Set UUID manually — @UuidGenerator won't override an already-set PK
        u.setUserId(userId);
        u.setUsername(username);
        u.setDisplayName(displayName);
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setPasswordHash(hash);
        u.setRole(role);
        // createdAt / updatedAt will be set by @PrePersist
        return u;
    }

    // =======================================================================
    // PHASE 2 — ARTICLES
    // =======================================================================
    /**
     * Seeds 6 language-learning articles with manual String IDs.
     * No FKs to other tables — safe to insert independently.
     */
    private void seedArticles() {
        log.info("  [2/7] Seeding 6 articles...");

        articleRepository.save(buildArticle(ART_ID_1, "Introduction to Bahasa Indonesia",
                "Bahasa Indonesia is the official language of Indonesia. It is a standardized register of Malay, "
                        + "an Austronesian language. The language has a relatively simple grammar: no tenses, no gender, "
                        + "and no plural forms for nouns. Plurality is expressed by reduplication (e.g., anak = child, "
                        + "anak-anak = children). The word order is typically Subject-Verb-Object. "
                        + "Bahasa Indonesia uses the Latin alphabet and has a phonetic spelling system, "
                        + "meaning words are pronounced as they are written.",
                CATEGORY_LANGUAGE));

        articleRepository.save(buildArticle(ART_ID_2, "English Grammar Fundamentals",
                "English grammar is the set of structural rules that govern the composition of clauses, phrases, "
                        + "and words in the English language. Key aspects include parts of speech (nouns, verbs, "
                        + "adjectives, adverbs, pronouns, prepositions, conjunctions, interjections), tenses "
                        + "(present, past, future with simple, continuous, perfect, and perfect continuous aspects), "
                        + "articles (a, an, the), and sentence structure. English has a Subject-Verb-Object word order "
                        + "and uses auxiliary verbs extensively for questions and negation.",
                CATEGORY_LANGUAGE));

        articleRepository.save(buildArticle(ART_ID_3, "Japanese Hiragana Guide",
                "Hiragana is one of the three writing systems used in Japanese, alongside Katakana and Kanji. "
                        + "It consists of 46 basic characters, each representing a distinct sound (mora). "
                        + "Hiragana is a phonetic script, meaning each character corresponds to a specific syllable. "
                        + "The characters are organized in a gojuon table (五十音, 'fifty sounds'). "
                        + "Hiragana is used for native Japanese words, grammatical particles, and verb/ adjective endings. "
                        + "Learning hiragana is the essential first step for any Japanese language learner.",
                CATEGORY_LANGUAGE));

        articleRepository.save(buildArticle(ART_ID_4, "French Pronunciation Tips",
                "French pronunciation can be challenging for English speakers. Key features include nasal vowels "
                        + "(an, en, in, on, un), silent final consonants (except c, r, f, l), and the uvular 'r' sound "
                        + "produced at the back of the throat. French has liaison and elision rules where words connect "
                        + "in speech. The language uses diacritical marks including the acute accent (é), grave accent (è), "
                        + "circumflex (ê), cedilla (ç), and diaeresis (ë). Mastering French pronunciation requires "
                        + "consistent practice with native audio materials.",
                CATEGORY_LANGUAGE));

        articleRepository.save(buildArticle(ART_ID_5, "German Articles Explained",
                "German has three grammatical genders: masculine, feminine, and neuter. Each noun belongs to one "
                        + "of these genders, and the definite article changes accordingly: der (masculine), die (feminine), "
                        + "das (neuter). In the nominative case, 'der Mann' (the man), 'die Frau' (the woman), "
                        + "'das Kind' (the child). All plural nouns use 'die' regardless of gender. "
                        + "Articles also change based on the case system: nominative, accusative, dative, and genitive. "
                        + "Memorizing the article together with each noun is essential for German learners.",
                CATEGORY_LANGUAGE));

        articleRepository.save(buildArticle(ART_ID_6, "Spanish Verb Conjugation",
                "Spanish verbs are conjugated to reflect person, number, tense, mood, and aspect. "
                        + "There are three verb categories based on infinitive endings: -ar, -er, and -ir. "
                        + "In the present tense, a regular -ar verb like 'hablar' (to speak) conjugates as: "
                        + "hablo, hablas, habla, hablamos, habláis, hablan. Spanish has a rich system of tenses "
                        + "including present, preterite, imperfect, future, conditional, and various compound tenses "
                        + "using the auxiliary verb 'haber'. Mastering conjugation patterns is the key to Spanish fluency.",
                CATEGORY_LANGUAGE));

        log.info("    ✅ 6 articles seeded");
    }

    private Article buildArticle(String id, String title, String content, String category) {
        Article a = new Article();
        a.setId(id); // manual String ID
        a.setTitle(title);
        a.setContent(content);
        a.setCategory(category);
        return a;
    }

    // =======================================================================
    // PHASE 3 — FAILED SYNC EVENTS
    // =======================================================================
    /**
     * Seeds 4 outbox sync events with varied statuses for testing
     * the retry/scheduler logic. PK (event_id) is IDENTITY — auto-generated.
     */
    private void seedFailedSyncEvents() {
        log.info("  [3/7] Seeding 4 failed_sync_events...");

        // 1. PENDING — just queued, never attempted yet
        FailedSyncEventEntity e1 = new FailedSyncEventEntity();
        e1.setEventType(SyncEventType.USER_SYNC);
        e1.setPayloadJson("{\"user_id\":\"b3333333-3333-3333-3333-333333333333\",\"username\":\"charlie\",\"event\":\"USER_SYNC\"}");
        e1.setStatus(SyncEventStatus.PENDING);
        e1.setRetryCount(0);
        failedSyncEventRepository.save(e1);

        // 2. FAILED — one retry, connection timeout
        FailedSyncEventEntity e2 = new FailedSyncEventEntity();
        e2.setEventType(SyncEventType.USER_SYNC);
        e2.setPayloadJson("{\"user_id\":\"b4444444-4444-4444-4444-444444444444\",\"username\":\"diana\",\"event\":\"USER_SYNC\"}");
        e2.setStatus(SyncEventStatus.FAILED);
        e2.setRetryCount(1);
        e2.setLastError("Connection timeout: unable to reach Rust backend at 172.18.0.3:8081 after 5000ms");
        e2.setNextRetryAt(Instant.now().plusSeconds(600)); // retry in 10 minutes
        failedSyncEventRepository.save(e2);

        // 3. FAILED — two retries, HTTP error
        FailedSyncEventEntity e3 = new FailedSyncEventEntity();
        e3.setEventType(SyncEventType.USER_SYNC);
        e3.setPayloadJson("{\"user_id\":\"b5555555-5555-5555-5555-555555555555\",\"username\":\"eric\",\"event\":\"USER_SYNC\"}");
        e3.setStatus(SyncEventStatus.FAILED);
        e3.setRetryCount(2);
        e3.setLastError("HTTP 503 Service Unavailable: {\"error\":\"database connection pool exhausted\"}");
        e3.setNextRetryAt(Instant.now().plusSeconds(300)); // retry in 5 minutes
        failedSyncEventRepository.save(e3);

        // 4. DONE — successfully synced
        FailedSyncEventEntity e4 = new FailedSyncEventEntity();
        e4.setEventType(SyncEventType.USER_SYNC);
        e4.setPayloadJson("{\"user_id\":\"b6666666-6666-6666-6666-666666666666\",\"username\":\"fiona\",\"event\":\"USER_SYNC\"}");
        e4.setStatus(SyncEventStatus.DONE);
        e4.setRetryCount(1);
        failedSyncEventRepository.save(e4);

        log.info("    ✅ 4 failed_sync_events seeded (1 PENDING, 2 FAILED, 1 DONE)");
    }

    // =======================================================================
    // PHASE 4 — QUIZZES
    // =======================================================================
    /**
     * Seeds 12 quizzes (2 per article) with manual String IDs.
     * FK: article_id references articles.id (String type).
     * Each quiz has question, options as JSON array string, and a single-letter answer.
     */
    private void seedQuizzes() {
        log.info("  [4/7] Seeding 12 quizzes...");

        // --- art-001: Introduction to Bahasa Indonesia ---
        quizRepository.save(buildQuiz("quiz-001", ART_ID_1,
                "How is plurality expressed in Bahasa Indonesia?",
                "[\"By adding -s\",\"By reduplication\",\"By adding -es\",\"By adding numbers\"]", "B"));
        quizRepository.save(buildQuiz("quiz-002", ART_ID_1,
                "What is the typical word order in Bahasa Indonesia?",
                "[\"Subject-Object-Verb\",\"Verb-Subject-Object\",\"Subject-Verb-Object\",\"Object-Verb-Subject\"]", "C"));

        // --- art-002: English Grammar Fundamentals ---
        quizRepository.save(buildQuiz("quiz-003", ART_ID_2,
                "How many parts of speech are there in English?",
                "[\"6\",\"7\",\"8\",\"9\"]", "C"));
        quizRepository.save(buildQuiz("quiz-004", ART_ID_2,
                "Which article is used before a vowel sound?",
                "[\"a\",\"an\",\"the\",\"none\"]", "B"));

        // --- art-003: Japanese Hiragana Guide ---
        quizRepository.save(buildQuiz("quiz-005", ART_ID_3,
                "How many basic Hiragana characters are there?",
                "[\"36\",\"46\",\"56\",\"26\"]", "B"));
        quizRepository.save(buildQuiz("quiz-006", ART_ID_3,
                "What is the Hiragana character table called?",
                "[\"Katakana\",\"Kanji\",\"Gojuon\",\"Romaji\"]", "C"));

        // --- art-004: French Pronunciation Tips ---
        quizRepository.save(buildQuiz("quiz-007", ART_ID_4,
                "What are French nasal vowels?",
                "[\"a, e, i, o, u\",\"an, en, in, on, un\",\"ai, ei, oi, ui, au\",\"ou, eu, au, eau, ai\"]", "B"));
        quizRepository.save(buildQuiz("quiz-008", ART_ID_4,
                "Which final consonants are typically pronounced in French?",
                "[\"c, r, f, l\",\"p, t, k, b\",\"m, n, s, z\",\"d, g, v, j\"]", "A"));

        // --- art-005: German Articles Explained ---
        quizRepository.save(buildQuiz("quiz-009", ART_ID_5,
                "What is the definite article for neuter nouns in German nominative case?",
                "[\"der\",\"die\",\"das\",\"dem\"]", "C"));
        quizRepository.save(buildQuiz("quiz-010", ART_ID_5,
                "What article do ALL plural nouns use in German nominative?",
                "[\"der\",\"die\",\"das\",\"den\"]", "B"));

        // --- art-006: Spanish Verb Conjugation ---
        quizRepository.save(buildQuiz("quiz-011", ART_ID_6,
                "What is the 'yo' form of the regular -ar verb 'hablar' in present tense?",
                "[\"hablas\",\"habla\",\"hablo\",\"hablamos\"]", "C"));
        quizRepository.save(buildQuiz("quiz-012", ART_ID_6,
                "What auxiliary verb is used for compound tenses in Spanish?",
                "[\"ser\",\"estar\",\"tener\",\"haber\"]", "D"));

        log.info("    ✅ 12 quizzes seeded");
    }

    private Quiz buildQuiz(String id, String articleId, String question, String options, String answer) {
        Quiz q = new Quiz();
        q.setId(id); // manual String ID
        q.setArticleId(articleId);
        q.setQuestion(question);
        q.setOptions(options);
        q.setAnswer(answer);
        return q;
    }

    // =======================================================================
    // PHASE 5 — COMMENTS
    // =======================================================================
    /**
     * Seeds 10 comments: 7 top-level + 3 nested replies.
     * FK: user_id → users.user_id, article_id → articles.id (String),
     *     parent_comment_id → comments.id (self-referential, nullable).
     *
     * Strategy: save top-level comments first, capture their generated UUIDs,
     * then create reply comments referencing those UUIDs via parentComment.
     */
    private void seedComments() {
        log.info("  [5/7] Seeding 10 comments...");

        // --- Top-level comments (7) — save and capture IDs for replies ---

        // Comment 1: Alice on art-001 (Indonesian)
        Comment c1 = new Comment();
        c1.setArticleId(ART_ID_1);
        c1.setUserId(ALICE);
        c1.setContent("Bahasa Indonesia is such a beautiful language! The grammar is simpler than English in many ways. "
                + "I especially love how phonetic the spelling is — you can pronounce any word just by reading it.");
        c1 = commentRepository.save(c1);

        // Comment 2: Charlie on art-001 (Indonesian)
        Comment c2 = new Comment();
        c2.setArticleId(ART_ID_1);
        c2.setUserId(CHARLIE);
        c2.setContent("I agree! I'm a native speaker and this article explains it well. "
                + "The reduplication for plurals is actually quite intuitive once you get used to it.");
        c2 = commentRepository.save(c2);

        // Comment 3: Diana on art-002 (English)
        Comment c3 = new Comment();
        c3.setArticleId(ART_ID_2);
        c3.setUserId(DIANA);
        c3.setContent("English grammar can be tricky. The tenses are especially difficult for me! "
                + "Present perfect vs past simple still confuses me sometimes. Any tips?");
        c3 = commentRepository.save(c3);

        // Comment 4: Bob on art-003 (Japanese)
        Comment c4 = new Comment();
        c4.setArticleId(ART_ID_3);
        c4.setUserId(BOB);
        c4.setContent("Hiragana is the foundation of Japanese. Great guide! "
                + "I recommend practicing writing each character 50 times until muscle memory kicks in.");
        c4 = commentRepository.save(c4);

        // Comment 5: Eric on art-003 (Japanese)
        Comment c5 = new Comment();
        c5.setArticleId(ART_ID_3);
        c5.setUserId(ERIC);
        c5.setContent("I've been studying Japanese for 3 months. Hiragana was the first thing I learned. "
                + "It took me about two weeks to memorize all 46 characters. Now I'm working on katakana!");
        c5 = commentRepository.save(c5);

        // Comment 6: Fiona on art-004 (French)
        Comment c6 = new Comment();
        c6.setArticleId(ART_ID_4);
        c6.setUserId(FIONA);
        c6.setContent("French pronunciation is beautiful but the nasal sounds are challenging. "
                + "I still can't properly distinguish between 'un' and 'in'. Does anyone have good resources?");
        c6 = commentRepository.save(c6);

        // Comment 7: George on art-005 (German)
        Comment c7 = new Comment();
        c7.setArticleId(ART_ID_5);
        c7.setUserId(GEORGE);
        c7.setContent("Der, die, das... German articles are my nightmare! "
                + "I wish there was a logical pattern to follow. At least plural is always 'die' in nominative!");
        c7 = commentRepository.save(c7);

        // --- Nested replies (3) — reference saved parent comments ---

        // Comment 8: Hannah replying to Charlie (c2) on art-001
        Comment c8 = new Comment();
        c8.setArticleId(ART_ID_1);
        c8.setUserId(HANNAH);
        c8.setParentComment(c2); // self-referential FK → comments.id
        c8.setContent("That's great! I'm learning Indonesian and finding it very approachable. "
                + "The lack of tenses is such a relief compared to English!");
        commentRepository.save(c8);

        // Comment 9: Eric replying to Bob (c4) on art-003
        Comment c9 = new Comment();
        c9.setArticleId(ART_ID_3);
        c9.setUserId(ERIC);
        c9.setParentComment(c4); // self-referential FK → comments.id
        c9.setContent("Thanks for sharing! Do you have tips for learning katakana too? "
                + "I heard it's harder because the characters look more angular.");
        commentRepository.save(c9);

        // Comment 10: Charlie replying to Fiona (c6) on art-004
        Comment c10 = new Comment();
        c10.setArticleId(ART_ID_4);
        c10.setUserId(CHARLIE);
        c10.setParentComment(c6); // self-referential FK → comments.id
        c10.setContent("The French 'r' sound is especially hard. Practice makes perfect! "
                + "Try gargling water to understand the uvular vibration — it actually helps!");
        commentRepository.save(c10);

        log.info("    ✅ 10 comments seeded (7 top-level, 3 replies)");

        // Save comment IDs in instance fields so later phases can reference them.
        // These are the generated UUIDs from JPA after save().
        this.savedComment1 = c1.getId();
        this.savedComment4 = c4.getId();
        this.savedComment6 = c6.getId();
    }

    // Generated UUIDs captured during comment seeding — used by comment_reactions phase
    private UUID savedComment1;
    private UUID savedComment4;
    private UUID savedComment6;

    // =======================================================================
    // PHASE 6 — USER ATTEMPTS
    // =======================================================================
    /**
     * Seeds 16 quiz attempts by various users on various quizzes.
     * FK: user_id → users.user_id, kuis_id → quizzes.id.
     * PK (id) is BIGINT IDENTITY — auto-generated.
     * completed_at is set manually (LocalDateTime) since there's no @PrePersist.
     */
    private void seedUserAttempts() {
        log.info("  [6/7] Seeding 16 user_attempts...");

        LocalDateTime base = LocalDateTime.now().minusDays(7);

        // Charlie attempted 4 quizzes
        userAttemptRepository.save(buildAttempt(CHARLIE, "quiz-001", base.plusHours(2)));
        userAttemptRepository.save(buildAttempt(CHARLIE, "quiz-002", base.plusHours(3)));
        userAttemptRepository.save(buildAttempt(CHARLIE, "quiz-005", base.plusDays(1).plusHours(1)));
        userAttemptRepository.save(buildAttempt(CHARLIE, "quiz-006", base.plusDays(1).plusHours(2)));

        // Diana attempted 3 quizzes
        userAttemptRepository.save(buildAttempt(DIANA, "quiz-003", base.plusHours(5)));
        userAttemptRepository.save(buildAttempt(DIANA, "quiz-004", base.plusHours(6)));
        userAttemptRepository.save(buildAttempt(DIANA, "quiz-007", base.plusDays(2).plusHours(1)));

        // Eric attempted 3 quizzes
        userAttemptRepository.save(buildAttempt(ERIC, "quiz-005", base.plusDays(1).plusHours(4)));
        userAttemptRepository.save(buildAttempt(ERIC, "quiz-006", base.plusDays(1).plusHours(5)));
        userAttemptRepository.save(buildAttempt(ERIC, "quiz-009", base.plusDays(3).plusHours(1)));

        // Fiona attempted 3 quizzes
        userAttemptRepository.save(buildAttempt(FIONA, "quiz-007", base.plusDays(2).plusHours(3)));
        userAttemptRepository.save(buildAttempt(FIONA, "quiz-008", base.plusDays(2).plusHours(4)));
        userAttemptRepository.save(buildAttempt(FIONA, "quiz-011", base.plusDays(4).plusHours(1)));

        // George attempted 2 quizzes
        userAttemptRepository.save(buildAttempt(GEORGE, "quiz-009", base.plusDays(3).plusHours(2)));
        userAttemptRepository.save(buildAttempt(GEORGE, "quiz-010", base.plusDays(3).plusHours(3)));

        // Hannah attempted 1 quiz
        userAttemptRepository.save(buildAttempt(HANNAH, "quiz-012", base.plusDays(5).plusHours(1)));

        log.info("    ✅ 16 user_attempts seeded");
    }

    private UserAttempt buildAttempt(UUID userId, String kuisId, LocalDateTime completedAt) {
        UserAttempt ua = new UserAttempt();
        ua.setUserId(userId);
        ua.setKuisId(kuisId);
        ua.setCompletedAt(completedAt);
        return ua;
    }

    // =======================================================================
    // PHASE 7 — COMMENT REACTIONS
    // =======================================================================
    /**
     * Seeds 8 UPVOTE reactions on popular comments.
     * FK: comment_id → comments.id, user_id → users.user_id.
     * Unique constraint: (comment_id, user_id, reaction_type) — no duplicate UPVOTEs.
     * PK (id) is UUID — auto-generated by @GeneratedValue.
     */
    private void seedCommentReactions() {
        log.info("  [7/7] Seeding 8 comment_reactions...");

        // UPVOTEs on comment 1 (Alice's Indonesian comment) — by Diana, Eric, Fiona
        commentReactionRepository.save(buildReaction(savedComment1, DIANA));
        commentReactionRepository.save(buildReaction(savedComment1, ERIC));
        commentReactionRepository.save(buildReaction(savedComment1, FIONA));

        // UPVOTEs on comment 4 (Bob's Hiragana comment) — by Charlie, George
        commentReactionRepository.save(buildReaction(savedComment4, CHARLIE));
        commentReactionRepository.save(buildReaction(savedComment4, GEORGE));

        // UPVOTEs on comment 6 (Fiona's French comment) — by Alice, Bob, Hannah
        commentReactionRepository.save(buildReaction(savedComment6, ALICE));
        commentReactionRepository.save(buildReaction(savedComment6, BOB));
        commentReactionRepository.save(buildReaction(savedComment6, HANNAH));

        log.info("    ✅ 8 comment_reactions seeded");
    }

    private CommentReaction buildReaction(UUID commentId, UUID userId) {
        CommentReaction cr = new CommentReaction();
        cr.setCommentId(commentId);
        cr.setUserId(userId);
        cr.setReactionType(ReactionType.UPVOTE);
        return cr;
    }
}
