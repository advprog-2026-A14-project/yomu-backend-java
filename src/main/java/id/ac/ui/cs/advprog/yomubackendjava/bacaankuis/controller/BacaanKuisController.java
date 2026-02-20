package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.BacaanKuis;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.BacaanKuisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bacaankuis")
@CrossOrigin(origins = "*") 
public class BacaanKuisController {

    @Autowired
    private BacaanKuisRepository repository;

    @GetMapping
    public List<BacaanKuis> getAllKuis() {
        return repository.findAll(); 
    }
}