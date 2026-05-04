package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;

public interface QuizSyncClient {
    void sync(QuizSyncRequest request);
}
