package com.api.apigateway.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class GlobalResponseFilter : GlobalFilter, Ordered {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val originalResponse = exchange.response
        val decoratedResponse = object : ServerHttpResponseDecorator(originalResponse) {
            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                if (body is Flux<*>) {
                    val flux = body as Flux<DataBuffer>
                    return super.writeWith(flux.buffer().map { dataBuffers ->
                        val joinedBuffers = exchange.response.bufferFactory().join(dataBuffers)
                        val content = joinedBuffers.toString(Charsets.UTF_8)
                        wrapResponse(content, originalResponse)
                    })
                }
                return super.writeWith(body)
            }
        }
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
    }

    private fun wrapResponse(content: String, originalResponse: ServerHttpResponse): DataBuffer {
        val statusCode = HttpStatus.valueOf(originalResponse.statusCode?.value() ?: HttpStatus.OK.value())
        val responseEntity = try {
            val jsonNode = objectMapper.readTree(content)
            ResponseEntity(ResponseWrapper(jsonNode), statusCode)
        } catch (e: Exception) {
            ResponseEntity(ResponseWrapper(content), statusCode)
        }
        val wrappedJson = objectMapper.writeValueAsString(responseEntity.body)
        val wrapped = originalResponse.bufferFactory().wrap(wrappedJson.toByteArray())
        originalResponse.headers.contentLength = wrapped.readableByteCount().toLong()
        originalResponse.statusCode = responseEntity.statusCode
        return wrapped
    }

    override fun getOrder(): Int = -2

    data class ResponseWrapper<T>(
        val data: T? = null,
        val error: String? = null
    )
}