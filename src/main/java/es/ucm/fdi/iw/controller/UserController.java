package es.ucm.fdi.iw.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.model.AtencionLog;
import es.ucm.fdi.iw.model.AtencionLogRepository;
import es.ucm.fdi.iw.model.Cola;
import es.ucm.fdi.iw.model.ColaRepository;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.User.Role;
import es.ucm.fdi.iw.model.UserRepository;
import es.ucm.fdi.iw.model.MessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

/**
 * Controlador encargado de la gestion de usuarios.
 * 
 * Incluye funcionalidades relacionadas con:
 * - perfiles de usuario
 * - gestion de imagenes de perfil
 * - mensajeria interna
 * - creacion automatica de pacientes mediante QR
 * - asignacion de personal a colas
 * - modificacion de pacientes y trabajadores
 */
@Controller()
@RequestMapping("user")
public class UserController {

  private static final Logger log = LogManager.getLogger(UserController.class);

  // Acceso directo al EntityManager para operaciones JPA
  @Autowired
  private EntityManager entityManager;

  // Acceso al almacenamiento local de archivos
  @Autowired
  private LocalData localData;

  // Plantilla para enviar mensajes mediante WebSocket
  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  // Codificador de contraseñas
  @Autowired
  private PasswordEncoder passwordEncoder;

  // Repositorio de usuarios
  @Autowired
  private UserRepository userRepository;

  // Repositorio de colas
  @Autowired
  private ColaRepository colaRepository;

  // Repositorio de historial de atención
  @Autowired
  private AtencionLogRepository atencionLogRepository;

  @Autowired
    private MessageRepository messageRepository;

  /**
   * Inserta automaticamente atributos comunes de sesion en el modelo.
   * 
   * Esto evita repetir el mismo codigo en todos los metodos.
   */
  @ModelAttribute
  public void populateModel(HttpSession session, Model model) {
    for (String name : new String[] { "u", "url", "ws", "topics" }) {
      model.addAttribute(name, session.getAttribute(name));
    }
  }

  /**
   * Excepcion utilizada cuando un usuario intenta acceder
   * o modificar un perfil que no le pertenece sin ser administrador.
   */
  @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "No eres administrador, y éste no es tu perfil") // 403
  public static class NoEsTuPerfilException extends RuntimeException {
  }

  /**
   * Codifica una contraseña utilizando el encoder configurado en Spring Security.
   * 
   * Cada codificacion genera un hash distinto debido al uso de salt aleatorio.
   * 
   * @param rawPassword contraseña sin cifrar
   * @return contraseña cifrada (typically a 60-character string)
   *         for example, a possible encoding of "test" is
   *         {bcrypt}$2y$12$XCKz0zjXAP6hsFyVc8MucOzx6ER6IsC1qo5zQbclxhddR1t6SfrHm
   */
  public String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  /**
   * Genera un token aleatorio codificado en Base64 URL-safe.
   * 
   * Utilizado para identificadores temporales o tokens de acceso.
   *
   * @param byteLength longitud del array aleatorio en bytes
   * @return token aleatorio codificado
   */
  public static String generateRandomBase64Token(int byteLength) {
    SecureRandom secureRandom = new SecureRandom();
    byte[] token = new byte[byteLength];
    secureRandom.nextBytes(token);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token); // base64 encoding
  }

  /**
   * Crea automaticamente un paciente temporal al acceder mediante QR.
   * 
   * El usuario:
   * - se crea automaticamente en la BD
   * - se añade a la cola asociada al QR
   * - recibe una posicion en la cola
   * - se notifica mediante WebSocket a los clientes suscritos
   * 
   * Finalmente se redirige a la vista del turno del paciente.
   *
   * @param token identificador QR asociado a una cola
   * @param session sesion HTTP actual
   */
  @GetMapping("/newQRuser")
  public String mostararVerTurno(@RequestParam("token") String token, HttpSession session, Model model) throws IOException {

    // Crear nuevo paciente temporal
    User u = new User();
    u.setUsername(generarUsernameUnico());
    u.setPassword(passwordEncoder.encode("a"));
    u.setRoles(Role.PACIENTE.name());
    u.setEnabled(true);
    userRepository.save(u);

    // Buscar cola asociada al token QR
    Cola cola = colaRepository.findByQrToken(token)
        .orElseThrow(() -> new RuntimeException("Cola no encontrada"));

    // Asignar posicion en la cola
    u.setPosicion(Cola.calcularSiguientePosicion(cola));

    cola.getListaClientes().add(u);
    colaRepository.save(cola);

    // Notificar actualizacion a clientes WebSocket
    log.info("Enviando WebSocket a /topic/cola/{}/actualizar", cola.getId());

    messagingTemplate.convertAndSend(
        "/topic/cola/" + cola.getId() + "/actualizar",
        "{\"colaId\":" + cola.getId() + ", \"tipo\":\"NUEVO_USUARIO\"}");
    
    log.info("WebSocket enviado correctamente");

    // Guardar usuario y cola en sesion
    session.setAttribute("userId", u.getId());
    session.setAttribute("colaId", cola.getId());

    AtencionLog logAtencion = new AtencionLog();
    logAtencion.setUserId(u.getId());
    logAtencion.setUsername(u.getUsername());
    logAtencion.setHoraEntradaCola(LocalDateTime.now());
    logAtencion.setColaId(cola.getId());
    logAtencion.setCola(cola.getNombre());

    atencionLogRepository.save(logAtencion);
    // Redirigir al panel de turnos
    return "redirect:/tuTurno"; // pasar id de cola e id de nuevo user paciente
  }

  /**
   * Muestra la vista principal de un perfil de usuario.
   *
   * @param id ID del usuario
   * @param model modelo de la vista
   */
  @GetMapping("{id:\\d+}")
  public String index(@PathVariable long id, Model model, HttpSession session) {
    User target = entityManager.find(User.class, id);
    model.addAttribute("user", target);
    return "user";
  }

  /**
   * Crea o modifica un usuario.
   * 
   * Los administradores pueden crear nuevos usuarios usando ID -1.
   * Los usuarios normales solo pueden modificar su propio perfil.
   * 
   * Tambien valida que las contraseñas coincidan antes de guardarlas.
   *
   * @param response respuesta HTTP
   * @param id ID del usuario
   * @param edited datos modificados
   * @param pass2 confirmacion de contraseña
   * @param model modelo de la vista
   * @param session sesion HTTP
   */
  @PostMapping("/{id}")
  @Transactional
  public String postUser(
      HttpServletResponse response,
      @PathVariable long id,
      @ModelAttribute User edited,
      @RequestParam(required = false) String pass2,
      Model model, HttpSession session) throws IOException {

    User requester = (User) session.getAttribute("u");
    User target = null;

    // Crear nuevo usuario si el ID es -1 y el usuario es admin
    if (id == -1 && requester.hasRole(Role.ADMIN)) {

      // create new user with random password
      target = new User();

      target.setPassword(encodePassword(generateRandomBase64Token(12)));
      target.setEnabled(true);

      entityManager.persist(target);
      entityManager.flush(); // forces DB to add user & assign valid id

      id = target.getId(); // retrieve assigned id from DB
    }

    // retrieve requested user
    target = entityManager.find(User.class, id);
    model.addAttribute("user", target);

    // Verificar permisos
    if (requester.getId() != target.getId() &&
        !requester.hasRole(Role.ADMIN)) {
      throw new NoEsTuPerfilException();
    }

    // Validar y actualizar contraseña
    if (edited.getPassword() != null) {
      if (!edited.getPassword().equals(pass2)) {
        log.warn("Passwords do not match - returning to user form");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("user", target);
        return "user";
      } else {
        // save encoded version of password
        target.setPassword(encodePassword(edited.getPassword()));
      }
    }

    // Actualizar datos basicos
    target.setUsername(edited.getUsername());
    target.setFirstName(edited.getFirstName());
    target.setLastName(edited.getLastName());

    // Actualizar usuario en sesion si modifica su propio perfil
    if (requester.getId() == target.getId()) {
      session.setAttribute("u", target);
    }

    return "user";
  }

  /**
   * Devuelve la imagen de perfil por defecto.
   *
   * @return InputStream de la imagen por defecto
   */
  private static InputStream defaultPic() {
    return new BufferedInputStream(Objects.requireNonNull(
        UserController.class.getClassLoader().getResourceAsStream(
            "static/img/default-pic.jpg")));
  }

  /**
   * Devuelve la imagen de perfil de un usuario.
   * 
   * Si el usuario no tiene imagen personalizada,
   * devuelve la imagen por defecto.
   *
   * @param id ID del usuario
   * @return stream con la imagen
   */
  @GetMapping("{id}/pic")
  public StreamingResponseBody getPic(@PathVariable long id) throws IOException {
    File f = localData.getFile("user", "" + id + ".jpg");
    InputStream in = new BufferedInputStream(f.exists() ? new FileInputStream(f) : UserController.defaultPic());
    return os -> FileCopyUtils.copy(in, os);
  }

  /**
   * Sube y guarda la imagen de perfil de un usuario.
   * 
   * Solo el propio usuario o un administrador pueden modificarla.
   *
   * @param photo imagen subida
   * @param id ID del usuario
   * @param response respuesta HTTP
   * @param session sesion actual
   * @param model modelo de la vista
   */
  @PostMapping("{id}/pic")
  @ResponseBody
  public String setPic(@RequestParam("photo") MultipartFile photo, @PathVariable long id,
      HttpServletResponse response, HttpSession session, Model model) throws IOException {

    User target = entityManager.find(User.class, id);
    model.addAttribute("user", target);

    // check permissions
    User requester = (User) session.getAttribute("u");
    if (requester.getId() != target.getId() &&
        !requester.hasRole(Role.ADMIN)) {
      throw new NoEsTuPerfilException();
    }

    log.info("Updating photo for user {}", id);
    File f = localData.getFile("user", "" + id + ".jpg");
    if (photo.isEmpty()) {
      log.info("failed to upload photo: emtpy file?");
    } else {
      try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {
        byte[] bytes = photo.getBytes();
        stream.write(bytes);
        log.info("Uploaded photo for {} into {}!", id, f.getAbsolutePath());
      } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log.warn("Error uploading " + id + " ", e);
      }
    }
    return "{\"status\":\"photo uploaded correctly\"}";
  }

  /**
   * Muestra la pagina de error personalizada.
   *
   * @param model modelo de la vista
   * @param session sesion HTTP
   * @param request peticion HTTP
   */
  @GetMapping("error")
  public String error(Model model, HttpSession session, HttpServletRequest request) {
    model.addAttribute("sess", session);
    model.addAttribute("req", request);
    return "error";
  }

  /**
   * Devuelve en formato JSON todos los mensajes recibidos
   * por el usuario autenticado.
   *
   * @param session sesion HTTP
   * @return lista de mensajes serializados
   */
  @GetMapping(path = "received", produces = "application/json")
  @Transactional // para no recibir resultados inconsistentes
  @ResponseBody // para indicar que no devuelve vista, sino un objeto (jsonizado)
  public List<Message.Transfer> retrieveMessages(HttpSession session) {
    long userId = ((User) session.getAttribute("u")).getId();
    User u = entityManager.find(User.class, userId);
    log.info("Generating message list for user {} ({} messages)",
        u.getUsername(), u.getReceived().size());
    return u.getReceived().stream().map(Transferable::toTransfer).collect(Collectors.toList());
  }

  /**
   * Devuelve el numero de mensajes no leidos del usuario actual.
   *
   * @param session sesion HTTP
   * @return JSON con el numero de mensajes no leidos
   */
  @GetMapping(path = "unread", produces = "application/json")
  @ResponseBody
  public String checkUnread(HttpSession session) {
    long userId = ((User) session.getAttribute("u")).getId();
    long unread = entityManager.createNamedQuery("Message.countUnread", Long.class)
        .setParameter("userId", userId)
        .getSingleResult();
    session.setAttribute("unread", unread);
    return "{\"unread\": " + unread + "}";
  }

  /**
   * Envia un mensaje privado a otro usuario.
   * 
   * El mensaje:
   * - se guarda en BD
   * - se serializa a JSON
   * - se envia mediante WebSocket al destinatario
   *
   * @param id ID del usuario destinatario
   * @param o JSON con el contenido del mensaje
   * @param model modelo de la vista
   * @param session sesion HTTP
   */
  @PostMapping("/{id}/msg")
  @ResponseBody
  @Transactional
  public String postMsg(@PathVariable long id,
      @RequestBody JsonNode o, Model model, HttpSession session)
      throws JsonProcessingException {

    String text = o.get("message").asText();
    User u = entityManager.find(User.class, id);
    User sender = entityManager.find(
        User.class, ((User) session.getAttribute("u")).getId());
    model.addAttribute("user", u);

    // construye mensaje, lo guarda en BD
    Message m = new Message();
    m.setRecipient(u);
    m.setSender(sender);
    m.setDateSent(LocalDateTime.now());
    m.setText(text);
    entityManager.persist(m);
    entityManager.flush(); // to get Id before commit

    ObjectMapper mapper = new ObjectMapper();
    /*
     * // construye json: método manual
     * ObjectNode rootNode = mapper.createObjectNode();
     * rootNode.put("from", sender.getUsername());
     * rootNode.put("to", u.getUsername());
     * rootNode.put("text", text);
     * rootNode.put("id", m.getId());
     * String json = mapper.writeValueAsString(rootNode);
     */
    // persiste objeto a json usando Jackson
    String json = mapper.writeValueAsString(m.toTransfer());

    log.info("Sending a message to {} with contents '{}'", id, json);

    // Enviar mensaje por WebSocket
    messagingTemplate.convertAndSend("/user/" + u.getUsername() + "/queue/updates", json);
    return "{\"result\": \"message sent.\"}";
  }

  /**
   * Muestra la vista de gestion de personal.
   *
   * @param model modelo de la vista
   */
  @GetMapping("manejar_personal")
  public String mostrarManejarPersonal(Model model) {

    List<String> roles = Arrays.asList("ORGANIZADOR", "ADMIN");
    model.addAttribute("roles", roles);

    return "manejar_personal";
  }

  /**
   * Muestra el formulario de modificacion de personal.
   *
   * @param id ID del trabajador
   * @param model modelo de la vista
   */
  @GetMapping("/modificar_personal/{id}")
  public String mostrarModificarPersonal(@PathVariable Long id, Model model) {
    User personal = userRepository.findById(id).orElse(null);
    model.addAttribute("personal", personal);

    List<String> roles = Arrays.asList("ADMIN", "ORGANIZADOR");
    model.addAttribute("roles", roles);

    return "modificar_personal";
  }

  /**
   * Muestra la vista para asignar o desasignar colas a un trabajador.
   *
   * @param id ID del trabajador
   * @param model modelo de la vista
   */
  @GetMapping("/modificar_colas_personal/{id}")
  public String mostrarModificarColasPersonal(@PathVariable Long id, Model model) {

      User personal = userRepository.findById(id).orElseThrow();
      model.addAttribute("personal", personal);

      List<Cola> colas = colaRepository.findAll();
      model.addAttribute("colas", colas);

      // Colas donde el trabajador está asignado
      List<Cola> colasDelTrabajador = colas.stream()
          .filter(c -> c.getTrabajadores().stream()
              .anyMatch(t -> Objects.equals(t.getId(), personal.getId())))
          .toList();

      model.addAttribute("colasDelTrabajador", colasDelTrabajador);

      // Mapa para saber si cada cola está asignada o no
      Map<Long, Boolean> colaAsignada = colas.stream()
          .collect(Collectors.toMap(
              Cola::getId,
              c -> c.getTrabajadores().stream()
                  .anyMatch(t -> Objects.equals(t.getId(), personal.getId()))
          ));

      model.addAttribute("colaAsignada", colaAsignada);

      return "modificar_colas_personal";
  }

  /**
   * Asigna una cola a un trabajador.
   *
   * @param trabajadorId ID del trabajador
   * @param colaId ID de la cola
   */
  @PostMapping("/asignarColaTrabajador")
  public String asignarCola(@RequestParam Long trabajadorId, @RequestParam Long colaId) {

      User trabajador = userRepository.findById(trabajadorId).orElseThrow();
      Cola cola = colaRepository.findById(colaId).orElseThrow();

      if (!trabajador.getColasAsignadas().contains(cola)) {
          trabajador.getColasAsignadas().add(cola);
          userRepository.save(trabajador);
      }

      return "redirect:/user/modificar_colas_personal/" + trabajadorId;
  }

  /**
   * Elimina la asignacion de una cola a un trabajador.
   *
   * @param trabajadorId ID del trabajador
   * @param colaId ID de la cola
   */
  @PostMapping("/desasignarColaTrabajador")
  public String desasignarCola(@RequestParam Long trabajadorId,
                              @RequestParam Long colaId) {

      User trabajador = userRepository.findById(trabajadorId).orElseThrow();
      Cola cola = colaRepository.findById(colaId).orElseThrow();

      trabajador.getColasAsignadas().remove(cola);
      userRepository.save(trabajador);

      return "redirect:/user/modificar_colas_personal/" + trabajadorId;
  }

/* 
  @GetMapping("/modificar_paciente/{id}")
  public String mostrarModificarPaciente(@PathVariable Long id, Model model) {
    User personal = userRepository.findById(id).orElse(null);
    model.addAttribute("paciente", personal);

    List<Cola> colas = colaRepository.findAll();
    model.addAttribute("colas", colas);

    Cola colaDelPaciente = colaRepository.findAll().stream()
        .filter(c -> c.getListaClientes().contains(personal))
        .findFirst()
        .orElse(null);
    model.addAttribute("colaDelPaciente", colaDelPaciente);
    return "modificar_paciente";
  }
    */

/* 
  @PostMapping("/editarPaciente/{id}")
  public String actualizarPaciente(@PathVariable Long id,
      @ModelAttribute("personal") User personal,
      @RequestParam("colaId") Long colaId) {

    // Cargar usuario existente desde la BD
    User usuarioExistente = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

    //usuarioExistente.setPosicion(personal.getPosicion());

    // Cambiar de cola si se seleccionó una que no estaba
    for (Cola c : colaRepository.findAll()) {
      if (c.getListaClientes().contains(usuarioExistente) && c.getId() != colaId) {
        c.getListaClientes().remove(usuarioExistente);
        // setear last a 1 menos, quitar 1 esperando y quitar una pos a los que estaban detras del user
        c.setLast(c.getLast() - 1);
        c.setWaiting(c.getWaiting() -1);

        colaRepository.save(c);

        List<User> clientes = c.getListaClientes();

        int posicionActual = usuarioExistente.getPosicion();

        // Mover hacia arriba a todos los que están detrás
        if (posicionActual <= c.getLast()){
          for (User u : clientes) {
            
            if (u.getPosicion() > posicionActual) {

              u.setPosicion(u.getPosicion() - 1);
              userRepository.save(u);
            }

          }
        }

        // Poner user en la nueva cola último
        Cola colaFinal = colaRepository.findById(colaId)
          .orElseThrow(() -> new RuntimeException("Cola no encontrada"));
        
        colaFinal.setLast(colaFinal.getLast() + 1);
        colaFinal.setWaiting(colaFinal.getWaiting() + 1);
        usuarioExistente.setPosicion(colaFinal.getLast());
        colaFinal.getListaClientes().add(usuarioExistente);

        colaRepository.save(colaFinal);
        userRepository.save(usuarioExistente);
      }    
    }

    return "redirect:/panelAdmin?modal=usuarios";
  }
    */

  
  @PostMapping("/editarPersonal/{id}")
  public String actualizarPersonal(@PathVariable Long id,
      @ModelAttribute("personal") User personal,
      @RequestParam(required = false) List<Long> colasId,
      @RequestParam("rol") String rol) {

    // Cargar usuario existente desde la BD
    User usuarioExistente = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

    usuarioExistente.setFirstName(personal.getFirstName());
    usuarioExistente.setLastName(personal.getLastName());
    usuarioExistente.setUsername(personal.getUsername());

    if (personal.getPassword() != null) {
        // Guardar contraseña cifrada en lugar de la contraseña sin cifrar
        usuarioExistente.setPassword(encodePassword(personal.getPassword()));
    }

    usuarioExistente.setRoles(rol);
    // 1. quitarlo de todas las colas actuales
    for (Cola c : colaRepository.findAll()) {
      c.getListaClientes().remove(usuarioExistente);
      colaRepository.save(c);
    }

    // 2. añadirlo a todas las colas seleccionadas
    if (colasId != null) {
      for (Long colaId : colasId) {
        Cola cola = colaRepository.findById(colaId)
            .orElseThrow(() -> new RuntimeException("Cola no encontrada: " + colaId));

        cola.getListaClientes().add(usuarioExistente);
        colaRepository.save(cola);
      }
    }

    // Guardar el usuario actualizado
    userRepository.save(usuarioExistente);

    return "redirect:/panelAdmin?modal=personal";
  }

  /**
   * Mueve a un usuario a la primera posición de la cola en la que está
   */
  @PostMapping("/ponerPrimero/{id}")
  public String ponerPrimeroCola(@PathVariable Long id) {

      // Usuario a mover
      User usuario = userRepository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

      // Buscar la cola donde está el usuario
      Cola colaUsuario = null;

      for (Cola c : colaRepository.findAll()) {
          if (c.getListaClientes().contains(usuario)) {
              colaUsuario = c;
              break;
          }
      }

      if (colaUsuario == null) {
          throw new RuntimeException("El usuario no pertenece a ninguna cola");
      }

      List<User> clientes = colaUsuario.getListaClientes();

      int posicionActual = usuario.getPosicion();

      // Mover hacia abajo a todos los que están delante
      if (posicionActual > colaUsuario.getFirst()){
        for (User u : clientes) {
          
          if (u.getId() != usuario.getId() &&
            u.getPosicion() >= colaUsuario.getFirst() && u.getPosicion() < posicionActual) {

            u.setPosicion(u.getPosicion() + 1);
            userRepository.save(u);
          }

        }

        // Poner el usuario primero
        usuario.setPosicion(colaUsuario.getFirst());

        userRepository.save(usuario);
          
      }
    /* 
    messagingTemplate.convertAndSend(
    "/topic/admin/actualizar",
    "reload"
    );
    */

    return "redirect:/panelAdmin?modal=usuarios";
  }

  // ------ metodos auxiliares ------//

  /**
   * Genera un username aleatorio unico de 5 caracteres.
   * 
   * El metodo comprueba en BD que el username no exista previamente.
   *
   * @return username unico
   */
  private String generarUsernameUnico() { // generar user con 3 caracteres aleatorios no repetido en la BD
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    Random r = new Random();
    String username;

    StringBuilder sb = new StringBuilder(); //
    for (int i = 0; i < 5; i++) {
      sb.append(chars.charAt(r.nextInt(chars.length())));
    }
    username = sb.toString();

    // Repetir mientras exista en BD
    while (userRepository.existsByUsername(username)) {
      sb = new StringBuilder();
      for (int i = 0; i < 5; i++) {
        sb.append(chars.charAt(r.nextInt(chars.length())));
      }
      username = sb.toString();
    }
    return username;
  }

  // Usuario sale de cola
  @PostMapping("/salir_de_cola/{id}")
  public String salirDeCola(@PathVariable Long id,
          @RequestParam(required = false, defaultValue = "/panelAdmin?modal=usuarios") String redirect) {

      User usuario = userRepository.findById(id).orElse(null);

      if (usuario != null) {

          List<Cola> colas = colaRepository.findAll();

          Integer posicionUsuario = usuario.getPosicion();

          // Lo elimina de su cola
          for (Cola cola : colas) {
              if (cola.getListaClientes() != null &&
                      cola.getListaClientes().contains(usuario)) {

                  cola.getListaClientes().remove(usuario);

                  if (posicionUsuario < cola.getFirst() -1){ // En el registro de los usuarios ya atendidos
                      for (User u : cola.getListaClientes()) {
                          if (u.getPosicion() < posicionUsuario) {
                              u.setPosicion(u.getPosicion() + 1);
                              userRepository.save(u);
                          }
                      }
                  }else{ // en cola o siendo atendido
                      if (posicionUsuario == cola.getFirst() - 1 && cola.getWaiting() == 0) { // si estaba siendo atendido y no hay nadie más detrás
                          cola.setFirst(cola.getFirst() - 1);
                          cola.setLast(cola.getLast() - 1);
                          cola.setUltimoTurno(String.valueOf(Integer.parseInt(cola.getUltimoTurno()) - 1));
                      }else{// en otro caso
                          /*
                          if (posicionPaciente == cola.getFirst() || posicionPaciente == cola.getFirst() -1) { // si era primero o estaba siendo atendido ya, la posicion del nuevo primero es la siguiente
                              cola.setFirst(cola.getFirst() + 1);
                          }
                          */
                          for (User u : cola.getListaClientes()) {
                              if (u.getPosicion() > posicionUsuario) {
                                  u.setPosicion(u.getPosicion() - 1);
                                  userRepository.save(u);
                              }
                          }
                          cola.setWaiting(cola.getWaiting() - 1);
                          cola.setLast(cola.getLast() - 1);
                      }
                      colaRepository.save(cola);
                  }

                  // Incluido WebSocket para notificar a los usuarios en tiempo real en la vista tuTurno de cuándo otro usuario abandona la cola
                      messagingTemplate.convertAndSend(
                          "/topic/cola/" + cola.getId() + "/actualizar",
                          "{\"colaId\":" + cola.getId() + ", \"tipo\":\"ABANDONO\"}");
              }
          }

          userRepository.delete(usuario);
      }

      return "redirect:" + redirect;
  }

}
