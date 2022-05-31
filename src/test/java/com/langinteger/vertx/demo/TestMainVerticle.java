package com.langinteger.vertx.demo;

import com.langinteger.proto.HelloWorldServiceGrpc;
import com.langinteger.proto.Request;
import io.grpc.MethodDescriptor;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcReadStream;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void helloWorldTest(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    Map<String, String> result = new HashMap<>();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    GrpcClient client = GrpcClient.client(vertx);
    client.request(
        SocketAddress.inetSocketAddress(8080, "127.0.0.1"),
        HelloWorldServiceGrpc.getHelloWorldMethod())
      .compose(request -> {
        request.end(Request.newBuilder().setName("Me").build());
        return request.response().compose(GrpcReadStream::last);
      })
      .onSuccess(response -> {
        System.out.println("Succeeded: " + response.getGreeting());
        result.put("result", response.getGreeting());
        countDownLatch.countDown();
      })
      .onFailure(exception -> {
        System.out.println("Failed: " + exception.getMessage());
        countDownLatch.countDown();
      });
    countDownLatch.await();
    if (!"Hello World! Me".equals(result.get("result"))) {
      throw new RuntimeException();
    }
    testContext.completeNow();
  }

  @Test
  void helloWorldBrokenTest(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    Map<String, String> result = new HashMap<>();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    GrpcClient client = GrpcClient.client(vertx);
    client.request(
        SocketAddress.inetSocketAddress(8080, "127.0.0.1"),
        HelloWorldServiceGrpc.getHelloWorldBrokenMethod())
      .compose(request -> {
        request.end(Request.newBuilder().setName("Me").build());
        return request.response().compose(GrpcReadStream::last);
      })
      .onSuccess(response -> {
        System.out.println("Succeeded: " + response.getGreeting());
        result.put("result", response.getGreeting());
        countDownLatch.countDown();
      })
      .onFailure(exception -> {
        System.out.println("Failed: " + exception.getMessage());
        countDownLatch.countDown();
      });
    countDownLatch.await();
    if (!"Invalid gRPC status 2".equals(result.get("result"))) {
      throw new RuntimeException();
    }
    testContext.completeNow();
  }
}
