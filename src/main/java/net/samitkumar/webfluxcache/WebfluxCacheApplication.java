package net.samitkumar.webfluxcache;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
@RequiredArgsConstructor
@EnableCaching
@EnableScheduling
@Slf4j
public class WebfluxCacheApplication {
	private final CacheManager cacheManager;
	private final TokenService tokenService;
	public static void main(String[] args) {
		SpringApplication.run(WebfluxCacheApplication.class, args);
	}

	@Scheduled(fixedRate = 60000)
	public void evictAllcachesAtIntervals() {
		cacheManager.getCacheNames().stream()
				.forEach(cacheName -> {
					log.info("CacheEvict Name:{} ", cacheName);
					//cacheManager.getCache(cacheName).evictIfPresent(cacheName);
					cacheManager.getCache(cacheName).clear();
				});
	}

	@Bean
	public RouterFunction router() {
		return RouterFunctions
				.route(GET("/db"), request -> ok().body(tokenService.fetchToken(), Token.class));
	}

}

@Configuration
@Slf4j
class Config {

	@Bean
	public WebClient tokenWebClient() {
		return WebClient.builder().baseUrl("http://localhost:8111").build();
	}
}

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class Token {
	private String idToken;
	private String accessToken;
	private String authType;
}

@Service
@Slf4j
@AllArgsConstructor
class TokenService {

	private final WebClient tokenWebClient;

	@Cacheable(cacheNames = "token")
	public Mono<Token> fetchToken() {
		log.info("fetchToken flow");
		return getToken()
				.cache(Duration.ofMinutes(1l))
				/*.cacheInvalidateIf(e -> Objects.isNull(e))
				.cacheInvalidateWhen(e -> )*/;
	}


	private Mono<Token> getToken() {
		log.info("getToken flow");
		return tokenWebClient
				.get()
				.uri("/token")
				.retrieve()
				.bodyToMono(Token.class)
				.doOnSuccess(r -> log.info("WebClient Response: SUCCESS"))
				.doOnError(e -> log.error("WebClient Response: ERROR {} ", e.getMessage()));
	}
}