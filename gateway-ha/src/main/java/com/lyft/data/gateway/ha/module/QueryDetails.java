package com.lyft.data.gateway.ha.module;

import lombok.Data;

@Data
public class QueryDetails {
    private final String query;
    private final String user;
    private final String source;
    private final String catalog;
}
