package es.ucm.fdi.iw.model;

//import java.time.LocalDateTime;

import jakarta.persistence.*;
//import jakarta.persistence.Entity;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un turno dentro del sistema.
 * 
 * Un turno conecta:
 * - un usuario (cliente/paciente)
 * - una cola concreta
 * 
 * Cada turno posee un identificador unico que permite
 * llamar y gestionar a los pacientes dentro de la cola.
 */
@Entity
@Data
@NoArgsConstructor
// Turno conecta usuarios y colas
public class Turno {

    /**
     * Identificador unico del turno.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * Codigo o identificador visible del turno.
     * 
     * Se marca como unico para evitar duplicados.
     */
    @Column(unique = true)
    private String turno;

    /**
     * Usuario asociado al turno.
     * 
     * Relacion muchos-a-uno:
     * varios turnos pueden pertenecer al mismo usuario.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User cliente; // Relación con User (rol=CLIENTE)

    /**
     * Cola asociada al turno.
     * 
     * Relacion muchos-a-uno:
     * varios turnos pueden pertenecer a la misma cola.
     */
    @ManyToOne
    @JoinColumn(name = "cola_id")
    private Cola cola;

}
