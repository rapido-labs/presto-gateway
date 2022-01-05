package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.QueryRouting;
import javax.servlet.http.HttpServletRequest;

public class RoutingGroupSelectorByCardId implements RoutingGroupSelector {
  static  String CARD_ID_HEADER = "X-Metabase-Card-Id";

  private JdbcConnectionManager connectionManager;

  public RoutingGroupSelectorByCardId(JdbcConnectionManager connectionManager) {

    this.connectionManager = connectionManager;
  }

  @Override
  public String findRoutingGroup(HttpServletRequest request) {
    String cardId = request.getHeader(CARD_ID_HEADER);

    try {
      connectionManager.open();
      return QueryRouting.where("query_id = ?", cardId).stream().findFirst()
        .map(q -> QueryRouting.getRoutingGroup((QueryRouting) q))
        .orElse(null);
    } catch (Exception e) {
      return null;
    } finally {
      connectionManager.close();
    }

  }
}
