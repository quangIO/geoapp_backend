package com.quangio.vertxsample

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.quangio.vertxsample.domain.LocationData
import com.quangio.vertxsample.helper.websocket.TransportPayload
import com.quangio.vertxsample.service.UserService
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory

val server = ChatServer<String>()
class MainVerticle : CoroutineVerticle() {
  private val sessionStore by lazy { LocalSessionStore.create(vertx) }
  private val userService by lazy { UserService(sessionStore) }

  private val logger = LoggerFactory.getLogger(this::class.java)

  /* In case you need SQL support
  private val data: KotlinEntityDataStore<Persistable> by lazy {
    val config = JsonObject()
      .put("jdbcUrl", "jdbc:hsqldb:mem:test?shutdown=true")
      .put("driverClassName", "org.hsqldb.jdbcDriver")
      .put("maximumPoolSize", 10)
    val source = HikariCPDataSourceProvider().getDataSource(config)
      SchemaModifier(source, Models.DEFAULT).createTables(TableCreationMode.DROP_CREATE)
    KotlinEntityDataStore<Persistable>(KotlinConfiguration(Models.DEFAULT, source))
  }
  */


  override fun start(startFuture: Future<Void>?) {
    val router = createRouter()

    val port = System.getenv("PORT")?.toInt() ?: 8080


    Json.mapper.registerModule(KotlinModule())

    vertx.createHttpServer().apply {
      requestHandler(router::accept)
      websocketHandler(webSocketHandler)
    }.listen(port) {
      if (it.failed()) {
        logger.error(it.cause().message)
      } else {
        logger.info("Server started at port $port")
      }
    }
  }


  private val webSocketHandler = Handler<ServerWebSocket> { ws ->
    /* If you want to do some authentication logic
    val user = try {
      userService.getFromSession(ws.query())
    } catch (e: Exception) {
      logger.warn(MarkerFactory.getMarker("InvalidSession"), "${ws.query()} is not a valid session id")
      ws.reject()
      return@Handler
    }
    */

    // good for prototyping but not for production
    val userKey = ws.query()
    if (userKey.isNullOrBlank()){
      ws.close(1, "query should not be null")
      return@Handler
    }
    System.out.print(userKey)
    server.memberJoin(userKey, ws)
    ws.textMessageHandler { req -> // On received request req,
      launch(vertx.dispatcher()) { // (scaling to thousands of CCU is easy)
        val data = Json.decodeValue(req, LocationData::class.java) // parse string into LocationData object
        server.broadcast(data) // do this
      }
    }.closeHandler {
      server.memberLeft(userKey)
    }
  }

  private fun createRouter() = Router.router(vertx).apply {
    /* In case you need authentication and stuff
    route().handler(CookieHandler.create())
    route().handler(SessionHandler.create(sessionStore))
    */
    route().handler(BodyHandler.create())
    route().handler(CorsHandler.create("*")) // if you host server and client on different servers
  }
}

fun main(args: Array<String>) {
  val vertx = Vertx.vertx()
  vertx.deployVerticle(MainVerticle())
}
