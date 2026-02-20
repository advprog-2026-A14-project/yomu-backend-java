package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity 
@Table(name = "bacaan_kuis")
@Getter @Setter
@NoArgsConstructor 
@AllArgsConstructor
public class BacaanKuis {
    @Id
    private String kuisId;
    private String kuisTitle;
}