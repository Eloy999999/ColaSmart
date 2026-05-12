package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A group of users, with an associated chat.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Colas")
public class Cola {

  public enum Estado {
    ABIERTO, CERRADO
  }

  public Estado getEstado() {
    return (abierto != null && abierto) ? Estado.ABIERTO : Estado.CERRADO;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  private String nombre;
  private int capacidad;

  @Column(unique = true)
  private String qrToken;

  @ManyToOne
  @JoinColumn(name = "encargado_id")
  private User encargado;

  @ManyToMany
  @JoinTable(
      name = "colas_lista_clientes",
      joinColumns = @JoinColumn(name = "cola_id"),
      inverseJoinColumns = @JoinColumn(name = "lista_clientes_id")
  )
  private List<User> listaClientes; // lista intermedia colas.id - lientes.id

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
  private List<User> trabajadores = new ArrayList<>(); // USER_COLAS

  public boolean isAbierto() {
    return abierto;
  }

  public void abrir() {
    abierto = true;
  }

  public void cerrar() {
    abierto = false;
  }

  // Punteros de la cola
  private int first;  // posicion del primero esperando (o siendo atendido)
  private int last;   // posicion del último añadido
  private int waiting;  // esperando

  public int getFirst() {
    return first;
  }

  public int getLast() {
      return last;
  }

  public int getWaiting() {
      return waiting;
  }

  public static int calcularSiguientePosicion(Cola c) {
      if (c == null) return 1;
      c.waiting++;
      c.setLast(c.getLast() + 1);
      return c.getLast();  // siempre el siguiente al último
  }

  public static void adelantarPuntero(Cola c) {
      if (c == null) return;
      if (c.waiting > 0) c.waiting--;
      c.setFirst(c.getFirst() + 1);  // avanza el puntero al siguiente
  }
  

}