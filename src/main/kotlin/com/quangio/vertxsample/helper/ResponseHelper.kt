package com.quangio.vertxsample.helper

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json

fun HttpServerResponse.endWithJson(obj: Any) {
  this.putHeader("Content-Type", "application/json; charset=utf-8").end(Json.encodePrettily(obj))
}
