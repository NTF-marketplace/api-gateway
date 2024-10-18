package com.api.apigateway.config

import com.api.apigateway.filter.GlobalResponseFilter
import com.api.apigateway.filter.JwtAuthenticationFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class GatewayConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth_route") { r ->
                r.path("/v1/account/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8083")
            }
            .route("wallet_route") { r ->
                r.path("/v1/wallet/**", "/v1/signin/**")
                    // .filters { f ->
                    //      f.rewritePath("/wallet/(?<remaining>.*)", "/$\\{remaining}")
                    // }
                    .uri("http://localhost:8083")
            }
            .route("api_auth_route") { r ->
                r.path("/v1/api-auth/**")
                    // .filters { f ->
                    //      f.rewritePath("/wallet/(?<remaining>.*)", "/$\\{remaining}")
                    // }
                    .uri("http://localhost:8081")
            }
            .build()
    }
}
