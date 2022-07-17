package com.lyft.data.gateway.ha.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalPrestoConfiguration {
  private String jdbcUrl;
  private String user;
  private String password;
  private boolean enabled;
  private String routingGroup;
}
