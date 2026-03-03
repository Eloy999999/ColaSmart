package es.ucm.fdi.iw.model;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToMany;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.NamedQueries;
//import jakarta.persistence.NamedQuery;
//import jakarta.persistence.OneToMany;
//import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A group of users, with an associated chat.
 */
@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
@Table(name = "Colas")
public class Cola {

  public enum Estado { ABIERTO, CERRADO }

  public Estado getEstado() {
    return (abierto != null && abierto) ? Estado.ABIERTO : Estado.CERRADO;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  private String nombre;
  private int capacidad;

  @ManyToOne
    @JoinColumn(name = "encargado_id")
    private User encargado;

  @OneToMany
  private User listaClientes;
  private Time horario;
  private String lugar;
  private String turnoActual;
  private Boolean abierto;

   // Trabajadores asignados (bidireccional)
    @ManyToMany(mappedBy = "colasAsignadas")
    private List<User> trabajadores = new ArrayList<>();

    public void abrir(){
      abierto = true;
    }

    public void cerrar(){
      abierto = false;
    }
}
