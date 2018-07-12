package com.quangio.vertxsample

import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.Json
import java.util.concurrent.ConcurrentHashMap

class ChatServer <K> {
  private val members = ConcurrentHashMap<K, ServerWebSocket>()

  fun memberJoin(userKey: K, session: ServerWebSocket) {
    members.putIfAbsent(userKey, session)
  }
  suspend fun broadcast(message: Any){
    members.values.forEach {
      it.writeTextMessage(Json.encodePrettily(message))
    }
  }

  fun memberLeft(userKey: K) {
    members.remove(userKey)
  }
}
