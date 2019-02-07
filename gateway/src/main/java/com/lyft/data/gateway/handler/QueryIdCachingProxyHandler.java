package com.lyft.data.gateway.handler;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.lyft.data.gateway.router.DefaultRoutingManager;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.QueryHistoryManager;
import com.lyft.data.gateway.router.RoutingManager;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.wrapper.MultiReadHttpServletRequest;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Callback;

@Slf4j
public class QueryIdCachingProxyHandler extends ProxyHandler {
  public static final String PROXY_TARGET_HEADER = "proxytarget";
  public static final String V1_STATEMENT_PATH = "/v1/statement";
  public static final String V1_QUERY_PATH = "/v1/query";
  public static final String QUERY_HTML_PATH = "/query.html";
  public static final String SCHEDULED_QUERY_HEADER = "X-Presto-Scheduled-Query";
  public static final String USER_HEADER = "X-Presto-User";
  public static final String SOURCE_HEADER = "X-Presto-Source";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final RoutingManager routingManager;
  private final QueryHistoryManager queryHistoryManager;

  private final Meter requestMeter;

  public QueryIdCachingProxyHandler(
      GatewayBackendManager gatewayBackendManager,
      QueryHistoryManager queryHistoryManager,
      Meter requestMeter,
      String cacheDataDir) {
    this.requestMeter = requestMeter;
    this.routingManager = new DefaultRoutingManager(gatewayBackendManager, cacheDataDir);
    this.queryHistoryManager = queryHistoryManager;
  }

  protected RoutingManager getRoutingManager() {
    return this.routingManager;
  }

  @Override
  public void preConnectionHook(HttpServletRequest request, Request proxyRequest) {
    if (request.getMethod().equals(HttpMethod.POST)
        && request.getRequestURI().startsWith(V1_STATEMENT_PATH)) {
      requestMeter.mark();
      try {
        String requestBody = CharStreams.toString(request.getReader());
        log.info(
            "Processing request endpoint: [{}], payload: [{}]",
            request.getRequestURI(),
            requestBody);
      } catch (Exception e) {
        log.warn("Error fetching the request payload", e);
      }
    }
  }

  @Override
  public String rewriteTarget(HttpServletRequest request) {
    /* Here comes the load balancer / gateway */
    String backendAddress;

    String queryId = extractQueryIdIfPresent(request.getRequestURI(), request.getQueryString());

    // Find query id and get url from cache
    if (!Strings.isNullOrEmpty(queryId)) {
      backendAddress = routingManager.findBackendForQueryId(queryId);
    } else {
      String scheduledHeader = request.getHeader(SCHEDULED_QUERY_HEADER);
      if ("true".equalsIgnoreCase(scheduledHeader)) {
        // This falls back on adhoc backends if there are no scheduled backends active.
        backendAddress = routingManager.provideScheduledBackendForThisRequest();
      } else {
        backendAddress = routingManager.provideAdhocBackendForThisRequest();
      }
    }
    String targetLocation =
        backendAddress
            + request.getRequestURI()
            + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
    ((MultiReadHttpServletRequest) request).addHeader(PROXY_TARGET_HEADER, backendAddress);

    String originalLocation =
        request.getRemoteAddr()
            + request.getRequestURI()
            + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

    log.info("Rerouting {} --> {}", originalLocation, targetLocation);
    return targetLocation;
  }

  protected String extractQueryIdIfPresent(String path, String queryParams) {
    if (path == null) {
      return null;
    }
    String queryId = null;
    log.debug("trying to extract query id from path [{}] or queryString [{}]", path, queryParams);
    if (path.startsWith(V1_STATEMENT_PATH) || path.startsWith(V1_QUERY_PATH)) {
      String[] tokens = path.split("/");
      if (tokens.length >= 4) {
        queryId = tokens[3];
      }
    } else if (path.startsWith(QUERY_HTML_PATH)) {
      queryId = queryParams;
    }
    log.debug("query id in url [{}]", queryId);
    return queryId;
  }

  /**
   * Response interceptor default.
   *
   * @param request
   * @param response
   * @param buffer
   * @param offset
   * @param length
   * @param callback
   */
  protected void postConnectionHook(
      HttpServletRequest request,
      HttpServletResponse response,
      byte[] buffer,
      int offset,
      int length,
      Callback callback) {
    try {
      String requestPath = request.getRequestURI();
      if (requestPath.startsWith(V1_STATEMENT_PATH)
          && request.getMethod().equals(HttpMethod.POST)) {
        logHeaders(response);
        String output;
        boolean isGZipEncoding = isGZipEncoding(response);
        if (isGZipEncoding) {
          output = plainTextFromGz(buffer);
        } else {
          output = new String(buffer);
        }
        log.debug(output);

        QueryHistoryManager.QueryDetail queryDetail = getQueryDetailsFromRequest(request);
        log.debug("Proxy destination : {}", queryDetail.getBackendUrl());

        if (response.getStatus() == HttpStatus.OK_200) {

          HashMap<String, String> results = objectMapper.readValue(output, HashMap.class);
          queryDetail.setQueryId(results.get("id"));

          if (!Strings.isNullOrEmpty(queryDetail.getQueryId())) {
            routingManager.setBackendForQueryId(
                queryDetail.getQueryId(), queryDetail.getBackendUrl());
            log.debug(
                "QueryId [{}] mapped with proxy [{}]",
                queryDetail.getQueryId(),
                queryDetail.getBackendUrl());
          } else {
            log.debug("QueryId [{}] could not be cached", queryDetail.getQueryId());
          }
        }
        queryHistoryManager.submitQueryDetail(queryDetail);
      } else {
        log.debug("SKIPPING For {}", requestPath);
      }
    } catch (Exception e) {
      log.error("Error in proxying defaulting to super call", e);
    }
    super.postConnectionHook(request, response, buffer, offset, length, callback);
  }

  private QueryHistoryManager.QueryDetail getQueryDetailsFromRequest(HttpServletRequest request)
      throws IOException {
    QueryHistoryManager.QueryDetail queryDetail = new QueryHistoryManager.QueryDetail();
    queryDetail.setBackendUrl(request.getHeader(PROXY_TARGET_HEADER));
    queryDetail.setCaptureTime(System.currentTimeMillis());
    queryDetail.setUser(request.getHeader(USER_HEADER));
    queryDetail.setSource(request.getHeader(SOURCE_HEADER));
    String queryText = CharStreams.toString(request.getReader());
    queryDetail.setQueryText(
        queryText.length() > 500 ? queryText.substring(0, 500) + "..." : queryText);
    return queryDetail;
  }
}
