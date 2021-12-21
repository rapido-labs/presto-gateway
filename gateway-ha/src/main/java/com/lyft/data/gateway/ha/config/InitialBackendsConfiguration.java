package com.lyft.data.gateway.ha.config;

import java.util.List;
import lombok.Data;



@Data
public class InitialBackendsConfiguration {
  private List<ProxyBackendConfiguration> backends;
}
