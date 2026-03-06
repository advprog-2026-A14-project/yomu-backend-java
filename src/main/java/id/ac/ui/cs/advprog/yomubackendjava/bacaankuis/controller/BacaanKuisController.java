package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.BacaanKuis;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.BacaanKuisRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/bacaankuis", "/api/v1/bacaankuis"})
@CrossOrigin(origins = "*")
public class BacaanKuisController {
    private final BacaanKuisRepository repository;

    public BacaanKuisController(BacaanKuisRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BacaanKuis>>> getAllKuis() {
        return ResponseEntity.ok(ApiResponse.success("Bacaan kuis fetched", repository.findAll()));
    }
}
