package com.quangio.vertxsample.helper.websocket

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

data class TransportPayload<out T>(val data: T, val path: Any) {
  fun encodePrettily(): String = Json.encodePrettily(this)
}
