package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.QueryRouting;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class RoutingGroupSelectorByCardId implements RoutingGroupSelector {
  static  String CARD_ID_HEADER = "X-Metabase-Card-Id";


  private JdbcConnectionManager connectionManager;

  public RoutingGroupSelectorByCardId(JdbcConnectionManager connectionManager) {

    this.connectionManager = connectionManager;
  }

  @Override
  public String findRoutingGroup(HttpServletRequest request) {
    String cardId = request.getHeader(CARD_ID_HEADER);
    log.info("Card id from header is", cardId);
    try {
      connectionManager.open();
      String routingGroup = QueryRouting.where("query_id = ?", cardId).stream().findFirst()
          .map(q -> QueryRouting.getRoutingGroup((QueryRouting) q))
          .orElse(null);
      log.info("Routing group is", routingGroup);
      return  routingGroup;
    } catch (Exception e) {
      return null;
    } finally {
      connectionManager.close();
    }

  }
}
