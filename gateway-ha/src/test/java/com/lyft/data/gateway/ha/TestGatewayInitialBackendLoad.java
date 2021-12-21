package com.lyft.data.gateway.ha;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestGatewayInitialBackendLoad {
  final int routerPort = 20000 + (int) (Math.random() * 1000);

  @BeforeClass(alwaysRun = true)
  public void setup() throws Exception {
    // seed database
    HaGatewayTestUtils.TestConfig testConfig = HaGatewayTestUtils
        .buildGatewayConfigAndSeedDb(routerPort);

    // Start Gateway
    String[] args = {"server", testConfig.getConfigFilePath()};
    HaGatewayLauncher.main(args);
  }

  @Test
  public void shouldReturn2InitialPrestoClusterDetails() throws Exception {
    Request request1 = new Request.Builder()
        .url("http://localhost:" + routerPort + "/entity/GATEWAY_BACKEND")
        .build();
    String responseBody = performRestRequest(request1);
    Assert.assertEquals(responseBody, "[{"
        + "\"name\":\"presto1\",\"proxyTo\":\"http://localhost:21000\",\"active\":true,\"routingGroup\":\"adhoc\",\"externalUrl\":\"http://localhost:21000\""
        + "},{"
        + "\"name\":\"presto2\",\"proxyTo\":\"http://localhost:22000\",\"active\":true,\"routingGroup\":\"scheduled\",\"externalUrl\":\"http://localhost:22000\""
        + "}]");
  }

  private String performRestRequest(Request request) throws IOException {
    OkHttpClient httpClient = new OkHttpClient();
    Response response = httpClient.newCall(request).execute();
    return response.body().string();
  }
}