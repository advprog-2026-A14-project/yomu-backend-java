package id.ac.ui.cs.advprog.yomubackendjava.outbox.repo;

import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FailedSyncEventRepository extends JpaRepository<FailedSyncEventEntity, Long> {
    List<FailedSyncEventEntity> findTop100ByStatusInOrderByCreatedAtAsc(Collection<SyncEventStatus> status);
}
