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
 * Entidad que representa una cola de espera.
 * Gestiona clientes, trabajadores, turnos y estado de apertura.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Colas")
public class Cola {

  /** Estado posible de la cola. */
  public enum Estado {
    ABIERTO, CERRADO
  }

  /** Devuelve el estado actual en funcion del campo "abierto". */
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

  /**
   * Clientes en la cola. Tabla intermedia: colas_lista_clientes.
   */
  @ManyToMany
  @JoinTable(
      name = "colas_lista_clientes",
      joinColumns = @JoinColumn(name = "cola_id"),
      inverseJoinColumns = @JoinColumn(name = "lista_clientes_id")
  )
  private List<User> listaClientes;

  private LocalDateTime horarioInicio;
  private LocalDateTime horarioFin;
  private String lugar;
  private String turnoActual;
  private Boolean abierto = false;
  private int tiempo; // tiempo estimado por turno en minutos

  private LocalTime inicioTurnoActual; // hora en que empezo el turno actual

  // Datos del ultimo turno atendido
  private String ultimoTurno;
  private LocalTime inicioUltimoTurno;
  private LocalTime finUltimoTurno;

  /**
   * Trabajadores asignados a esta cola.
   * Lado inverso de la relacion; el propietario es User.colasAsignadas.
   */
  @ManyToMany(mappedBy = "colasAsignadas")
  private List<User> trabajadores = new ArrayList<>();

  public boolean isAbierto() { return abierto; }
  public void abrir()        { abierto = true; }
  public void cerrar()       { abierto = false; }

  private int first;    // posicion del cliente siendo atendido
  private int last;     // posicion del ultimo cliente añadido
  private int waiting;  // clientes en espera

  /**
   * Asigna posicion al siguiente cliente que se une a la cola
   * e incrementa los contadores correspondientes.
   *
   * @param c  cola a la que se une el cliente
   * @return   posicion asignada
   */
  public static int calcularSiguientePosicion(Cola c) {
    if (c == null) return 1;
    c.waiting++;
    c.setLast(c.getLast() + 1);
    return c.getLast();
  }

  /**
   * Avanza el puntero "first" al siguiente en espera
   * y reduce el contador de espera.
   *
   * @param c  cola cuyo puntero se adelanta
   */
  public static void adelantarPuntero(Cola c) {
    if (c == null) return;
    if (c.waiting > 0) c.waiting--;
    c.setFirst(c.getFirst() + 1);
  }
}