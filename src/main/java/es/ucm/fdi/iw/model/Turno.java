package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
//Turno conecta usuarios y colas
public class Turno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String turno;
    
    @ManyToOne @JoinColumn(name = "user_id")
    private User cliente;  // Relación con User (rol=CLIENTE)
    
    @ManyToOne @JoinColumn(name = "cola_id")
    private Cola cola;
    
}
