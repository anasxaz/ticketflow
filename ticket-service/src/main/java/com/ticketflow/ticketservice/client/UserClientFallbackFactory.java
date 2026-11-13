package com.ticketflow.ticketservice.client;

import com.ticketflow.ticketservice.dto.UserResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserResponse getUserById(Long id) {
                return handle(cause);
            }

            @Override
            public UserResponse getUserByEmail(String email) {
                return handle(cause);
            }
        };
    }

    private UserResponse handle(Throwable cause) {
        // 4xx (ex. user introuvable) = vraie réponse de user-service → on la laisse passer
        if (cause instanceof FeignException.FeignClientException e) throw e;

        // sinon : panne (service down, timeout, disjoncteur ouvert) → 503 propre
        log.warn("user-service indisponible ({}) → fallback", cause.getClass().getSimpleName());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Service utilisateurs indisponible, réessayez plus tard");
    }
}