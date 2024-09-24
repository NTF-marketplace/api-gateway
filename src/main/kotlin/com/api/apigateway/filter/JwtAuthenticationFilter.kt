package com.api.apigateway.filter

import com.api.apigateway.util.JwtProvider
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(private val jwtProvider: JwtProvider) : GatewayFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val token = request.headers.getFirst(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer ")

        return if (token != null && jwtProvider.validateToken(token)) {
            val claims = jwtProvider.getClaimsFromToken(token)
            if (claims != null) {
                val address = claims.getClaim("address") as? String
                if (address != null) {
                    val modifiedRequest = exchange.request.mutate()
                        .header("X-Auth-Address", address)
                        .build()

                    chain.filter(exchange.mutate().request(modifiedRequest).build())
                } else {
                    onError(exchange, "Address claim not found in token")
                }
            } else {
                onError(exchange, "Invalid token claims")
            }
        } else {
            onError(exchange, "Invalid or missing token")
        }
    }

    private fun onError(exchange: ServerWebExchange, err: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        return response.writeWith(Mono.just(response.bufferFactory().wrap(err.toByteArray())))
    }
}