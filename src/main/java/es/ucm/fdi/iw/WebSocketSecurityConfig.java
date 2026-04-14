package es.ucm.fdi.iw;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import es.ucm.fdi.iw.model.User;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
                .simpDestMatchers("/admin/**").hasRole(User.Role.ADMIN.toString())
                .simpSubscribeDestMatchers("/admin/**").hasRole(User.Role.ADMIN.toString())
                .simpSubscribeDestMatchers("/topic/cola/**").authenticated()
                .anyMessage().authenticated();
        return messages.build();
    }
}