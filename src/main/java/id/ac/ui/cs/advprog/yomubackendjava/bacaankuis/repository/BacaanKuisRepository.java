package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.BacaanKuis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacaanKuisRepository extends JpaRepository<BacaanKuis, String> {
}