package com.lyft.data.gateway.ha.persistence.dao;

import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Slf4j
@Table("query_routing")
@IdName("query_id")
@Cached
public class QueryRouting extends Model {
  private static final String queryId = "query_id";
  private static final String routingGroup = "routing_group";

  public static String getRoutingGroup(QueryRouting model) {
    return model.getString(routingGroup);
  }

}
