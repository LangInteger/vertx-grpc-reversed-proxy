package com.langinteger.vertx.demo;

import com.langinteger.proto.Request;
import com.langinteger.proto.Response;
import com.langinteger.proto.VertxHelloWorldServiceGrpc;
import io.vertx.core.Future;

public class HelloWorldGrpcService extends VertxHelloWorldServiceGrpc.HelloWorldServiceVertxImplBase {

  public Future<Response> helloWorld(Request request) {
    return Future.succeededFuture(Response.newBuilder()
      .setGreeting(String.format("Hello World! %s", request.getName()))
      .build());
  }

  public Future<Response> helloWorldBroken(Request request) {
    throw new RuntimeException("Bad things happened");
  }
}
