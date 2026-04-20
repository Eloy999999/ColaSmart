package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

  @ManyToOne
  @JoinColumn(name = "encargado_id")
  private User encargado;

  @OneToMany
  private List<User> listaClientes; // Tabla intermedia que relaciona users.id con colas.id
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

  public boolean isAbierto() {
    return abierto;
  }

  public void abrir() {
    abierto = true;
  }

  public void cerrar() {
    abierto = false;
  }

  public static int calcularSiguientePosicion(Cola c) {
    if (c == null || c.getListaClientes() == null || c.getListaClientes().isEmpty()) {
      return 0;
    }

    int max = -1;

    for (User u : c.getListaClientes()) {
      if (u.getPosicion() > max) {
        max = u.getPosicion();
      }
    }

    return max + 1;
  }

  public static void adelantarPacientesDetrasUno(Cola c, int pos) {
    if (c == null || c.getListaClientes() == null || c.getListaClientes().isEmpty()) {
      return;
    }

    for (User u : c.getListaClientes()) {
      if (u.getPosicion() > pos) {
        u.setPosicion(u.getPosicion() - 1);
      }
    }
  }
}