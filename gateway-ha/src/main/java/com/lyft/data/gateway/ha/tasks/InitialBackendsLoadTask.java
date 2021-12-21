package com.lyft.data.gateway.ha.tasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Singleton
@AllArgsConstructor(onConstructor_ = @Inject)
public class InitialBackendsLoadTask {

  private final HaGatewayConfiguration configuration;
  private final GatewayBackendManager gatewayBackendManager;

  public void add() {
    try {
      List<ProxyBackendConfiguration> proxyConfigs =
          configuration.getInitialBackends().getBackends();

      log.info("Loading {} initial proxy configurations", proxyConfigs.size());

      for (ProxyBackendConfiguration proxyConfig : proxyConfigs) {
        gatewayBackendManager.updateBackend(proxyConfig);
      }
    } catch (RuntimeException e) {
      log.error("Could not load initial proxy configuration ", e);
    }
  }
}
