package com.cloudburst.audit.biqquery;

import java.util.Map;

public class AuditItemOverviewTrackingQuery extends AuditItemTrackingQuery {
    public AuditItemOverviewTrackingQuery(String datasetId, String tableId, Map<String, String> queryParams) {
        super(datasetId, tableId, queryParams);
    }

    @Override
    protected String selectColumns() {
        return "type,guid,seriesGuid,timestamp,level,module,label,url,hostname";
    }
}
