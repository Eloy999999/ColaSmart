package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
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
  private List<User> listaClientes;
  private LocalDateTime horarioInicio;
  private LocalDateTime horarioFin;
  private String lugar;
  private String turnoActual;
  private Boolean abierto = false;
  private int tiempo;

  // Turno actual: hora en que empezó
  private LocalTime inicioTurnoActual;

  // Último turno atendido
  private String ultimoTurno;
  private LocalTime inicioUltimoTurno;
  private LocalTime finUltimoTurno;

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