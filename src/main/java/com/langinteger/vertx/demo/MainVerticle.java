package com.langinteger.vertx.demo;

import io.grpc.protobuf.services.ProtoReflectionService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {

    startGrpcService(startPromise);

    // ==========

    startGrpcGatewayService();
  }

  private void startGrpcGatewayService() {
    GrpcServer server = GrpcServer.server(vertx);
    GrpcClient client = GrpcClient.client(vertx);
    server.callHandler(clientReq -> {
      clientReq.pause();
      System.out.println(clientReq.fullMethodName());
      SocketAddress address = SocketAddress.inetSocketAddress(8081, "127.0.0.1");
      client.request(address)
        .onSuccess(proxyRequest -> {
          System.out.println("on grpc proxy success");
          proxyRequest.response()
            .onSuccess(proxyResponse -> {
              System.out.println("proxy response success: " + proxyResponse.status());
              // if (proxyResponse.status() != null) {
              //   clientReq.response().status(proxyResponse.status());
              // }
              proxyResponse.messageHandler(clientReq.response()::writeMessage);
              proxyResponse.errorHandler(error -> {
                // TODO why we cannot get proxy call fail code here?
                System.out.println("proxy call error happened: " + proxyResponse.status());
                clientReq.response().status(error.status);
              });
              proxyResponse.endHandler(voids -> clientReq.response().end());
            })
            .onFailure(exception -> {
              System.out.println("proxy response fail: " + exception.getMessage());
              exception.printStackTrace();
              clientReq.response().status(GrpcStatus.UNKNOWN).end();
            });

          proxyRequest.fullMethodName(clientReq.fullMethodName());
          clientReq.messageHandler(proxyRequest::writeMessage);
          clientReq.endHandler(v -> proxyRequest.end());
          clientReq.resume();
        })
        .onFailure(exception -> {
          System.out.println("on grpc proxy fail");
          clientReq.response().status(GrpcStatus.UNKNOWN).end();
          clientReq.resume();
        });
    });

    vertx.createHttpServer(
        new HttpServerOptions()
          .setHost("0.0.0.0")
          .setPort(8080)
      ).requestHandler(server)
      .listen()
      .onSuccess(res -> System.out.println("grpc gateway service started"))
      .onFailure(exception -> System.out.println("grpc gateway service start fail: " + exception.getMessage()));
  }

  private void startGrpcService(Promise<Void> startPromise) {
    VertxServer server = VertxServerBuilder.forAddress(vertx, "0.0.0.0", 8081)
      .addService(new HelloWorldGrpcService())
      .addService(ProtoReflectionService.newInstance())
      .build();

    server.start(result -> {
      if (result.succeeded()) {
        System.out.println("grpc service start success");
        startPromise.complete();
      } else {
        System.out.println("grpc service start fail: " + result.cause().getMessage());
        result.cause().printStackTrace();
        startPromise.fail(result.cause());
      }
    });
  }
}

