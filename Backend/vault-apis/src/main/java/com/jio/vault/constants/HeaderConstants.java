package com.jio.vault.constants;

import java.util.Map;

public final class HeaderConstants {
    private HeaderConstants() {}

    public static final String TENANT_ID = "tenant-id";
    public static final String BUSINESS_ID = "business-id";
    public static final String TXN = "txn";
    public static final String DATA_CATEGORY_TYPE = "data-category-type";
    public static final String DATA_CATEGORY_VALUE = "data-category-value";
    public static final String UUID = "reference-id";


    public static final Map<String, String> REQUIRED_HEADERS = Map.of(
            TENANT_ID, "Missing/empty TenantId",
            BUSINESS_ID, "Missing/empty BusinessId",
            TXN, "Missing/empty Txn"
    );
}

