package com.jio.digigov.notification.enums;

public enum NetworkType {
    INTRANET,  // RIL Network - uses bearer token only
    INTERNET   // External Network - requires mutual SSL + bearer token
}