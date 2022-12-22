package net.polvott.apps

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
}

val keycloakAddr = "http://localhost:8081"

fun Application.voff(httpClient: HttpClient = applicationHttpClient) {
    println("###JKB### Starting....")
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
    install(Authentication) {
        oauth("auth-oauth-keycloak") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = "$keycloakAddr/auth/realms/ktor/protocol/openid-connect/auth",
                    accessTokenUrl = "$keycloakAddr/auth/realms/ktor/protocol/openid-connect/token",
                    requestMethod = HttpMethod.Post,
                    clientId = "ktorapp",
                    clientSecret = "123456789",
                    defaultScopes = listOf("roles")
                )
            }
            client = httpClient
        }
    }
    routing {
        authenticate("auth-oauth-keycloak") {
            get("/login") {
                // Redirects to 'authorizeUrl' automatically
            }

            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                call.sessions.set(UserSession(principal?.accessToken.toString()))
                call.respondRedirect("/hello")
            }
        }
        get("/") {
            call.respondHtml {
                body {
                    p {
                        a("/login") { +"Login with Keycloak." }
                    }
                }
            }
        }
        get("/hello") {
            println("###JKB### get /hello ....")
            val userSession: UserSession? = call.sessions.get()
            if (userSession != null) {


                val response: HttpResponse = httpClient.get("$keycloakAddr/auth/realms/ktor/protocol/openid-connect/userinfo") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${userSession.token}")
                    }
                }
                val status: HttpStatusCode = response.status
                println("###JKB### http status code $status")
                if (status == HttpStatusCode.OK)
                {
                    val userInfo: UserInfo = response.body()
                    call.respondText("Hello ${userInfo.preferred_username}!")
                }


            } else {
                call.respondRedirect("/")
            }
        }
    }
}

data class UserSession(val token: String)

@Serializable
data class UserInfo(
    val sub: String,
    val email_verified: String? = null,
    val name: String? = null,
    val preferred_username: String,
    val given_name: String? = null,
    val family_name: String? = null
)
