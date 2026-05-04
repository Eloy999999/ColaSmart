Feature: Prueba base de datos lista de salas (colas)

Scenario: clientes entran en cola
    # Cliente 1 entra en la cola
    Given driver baseUrl + '/user/newQRuser?token=12345'
    Then waitForText('body', 'Tu turno')
    And waitForText('body', 'Posición en la cola')
    And def posicion1 = script("parseInt(document.querySelectorAll('.fs-2 span')[2].textContent)")

    # Cliente 2 entra en la misma cola (genera usuario diferente)
    Given driver baseUrl + '/user/newQRuser?token=12345'
    Then waitForText('body', 'Tu turno')
    And waitForText('body', 'Posición en la cola')
    And def posicion2 = script("parseInt(document.querySelectorAll('.fs-2 span')[2].textContent)")

    # Verificar que el segundo cliente está detrás del primero
    Then assert posicion2 > posicion1

Scenario: personal atiende cola
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When click(".form-signin button")
    Then waitForUrl(baseUrl + '/panelAdmin')

    Given driver baseUrl + '/seguimientoCola'
    Then waitForText('body', 'Seguimiento de Cola')

    When click("tr[data-cola-id='975']")
    Then waitForText('body', 'Atendiendo ahora')

    # Primera llamada a siguiente
    And def turnoAntes = script("document.querySelector('#modal-turno-actual .badge')?.textContent || 'ninguno'")
    And def esperando1 = script("parseInt(document.getElementById('modal-badge-espera').textContent)")
    When click("#btn-siguiente")
    And delay(1000)
    And def turnoDespues = script("document.querySelector('#modal-turno-actual .badge')?.textContent || 'ninguno'")
    * eval if (esperando1 > 0 && turnoDespues == turnoAntes) karate.fail('Primera llamada: el turno no cambió')

    # Segunda llamada a siguiente
    And def turnoAntes2 = script("document.querySelector('#modal-turno-actual .badge')?.textContent || 'ninguno'")
    And def esperando2 = script("parseInt(document.getElementById('modal-badge-espera').textContent)")
    When click("#btn-siguiente")
    And delay(1000)
    And def turnoDespues2 = script("document.querySelector('#modal-turno-actual .badge')?.textContent || 'ninguno'")
    * eval if (esperando2 > 0 && turnoDespues2 == turnoAntes2) karate.fail('Segunda llamada: el turno no cambió')

    # Tercera llamada a siguiente
    And def turnoAntes3 = script("document.querySelector('#modal-turno-actual .badge')?.textContent || 'ninguno'")
    And def esperando3 = script("parseInt(document.getElementById('modal-badge-espera').textContent)")
    When click("#btn-siguiente")
    And delay(1000)
    And def turnoDespues3 = script("document.querySelector('#modal-turno-actual .badge')?.textContent || 'ninguno'")
    * eval if (esperando3 > 0 && turnoDespues3 == turnoAntes3) karate.fail('Tercera llamada: el turno no cambió')