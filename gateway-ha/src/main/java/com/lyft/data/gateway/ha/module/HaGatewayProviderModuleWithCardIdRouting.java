package com.lyft.data.gateway.ha.module;

import com.codahale.metrics.Meter;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.handler.QueryIdCachingProxyHandler;
import com.lyft.data.gateway.ha.router.RoutingGroupSelector;
import com.lyft.data.gateway.ha.router.RoutingGroupSelectorByCardId;
import com.lyft.data.proxyserver.ProxyHandler;
import io.dropwizard.setup.Environment;


public class HaGatewayProviderModuleWithCardIdRouting extends HaGatewayProviderModule {

  public HaGatewayProviderModuleWithCardIdRouting(HaGatewayConfiguration configuration,
                                                  Environment environment) {
    super(configuration, environment);
  }

  @Override
  protected ProxyHandler getProxyHandler() {
    Meter requestMeter = getEnvironment()
        .metrics()
        .meter(getConfiguration().getRequestRouter().getName() + ".requests");

    RoutingGroupSelector routingGroupSelector =
        new RoutingGroupSelectorByCardId(getConnectionManager());
    return new QueryIdCachingProxyHandler(
      getQueryHistoryManager(),
      getRoutingManager(),
      routingGroupSelector,
      getApplicationPort(),
      requestMeter, super.queue, configuration().getExternalPresto());
  }
}
