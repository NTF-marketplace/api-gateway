package com.api.apigateway.util

import com.api.apigateway.properties.JwtProperties
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.security.interfaces.RSAPublicKey
import java.util.Date

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) {

    fun validateToken(token: String): Boolean {
        try {
            val rsaKey = loadRsaKeyFromFile()
            val publicKey: RSAPublicKey = rsaKey.toRSAPublicKey()

            val signedJWT = SignedJWT.parse(token)

            val verifier = RSASSAVerifier(publicKey)

            if (!signedJWT.verify(verifier)) {
                return false
            }

            val claims = signedJWT.jwtClaimsSet
            return Date().before(claims.expirationTime)

        } catch (e: Exception) {
            println("Error during token validation: ${e.message}")
            return false
        }
    }

    fun getClaimsFromToken(token: String): JWTClaimsSet? {
        try {
            val rsaKey = loadRsaKeyFromFile()
            val publicKey: RSAPublicKey = rsaKey.toRSAPublicKey()

            val signedJWT = SignedJWT.parse(token)
            val verifier = RSASSAVerifier(publicKey)

            if (signedJWT.verify(verifier)) {
                return signedJWT.jwtClaimsSet
            }
        } catch (e: Exception) {
            println("Error during token decoding: ${e.message}")
        }
        return null
    }

    fun loadRsaKeyFromFile(): RSAKey {
        val rsaKeyPath = jwtProperties.rsaKeyPath
        val json = String(Files.readAllBytes(Paths.get(rsaKeyPath)))
        return RSAKey.parse(json)
    }
}
