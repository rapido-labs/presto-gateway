package com.lyft.data.gateway.ha.router;

import com.google.common.base.Strings;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.QueryRouting;


import javax.servlet.http.HttpServletRequest;

public class RoutingGroupSelectorByCardID implements RoutingGroupSelector {
  String CARD_ID_HEADER = "X-Metabase-Card-Id";

  private JdbcConnectionManager connectionManager;

  public RoutingGroupSelectorByCardID(JdbcConnectionManager connectionManager) {

    this.connectionManager = connectionManager;
  }

  @Override
  public String findRoutingGroup(HttpServletRequest request) {
    String cardID = request.getHeader(CARD_ID_HEADER);

    try {
      connectionManager.open();
      return QueryRouting.where("query_id = ?", cardID).stream().findFirst()
        .map(q -> QueryRouting.getRoutingGroup((QueryRouting) q))
        .orElse(null);
    } catch (Exception e) {
      return null;
    } finally {
      connectionManager.close();
    }

  }
}
