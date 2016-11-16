package com.paxport.cloudaudit.biqquery;

import java.util.Map;

public class AuditItemOverviewTrackingQuery extends AuditItemTrackingQuery {
    public AuditItemOverviewTrackingQuery(String datasetId, String tableId, Map<String, String> queryParams) {
        super(datasetId, tableId, queryParams);
    }

    @Override
    protected String selectColumns() {
        return "type,guid,timestamp,level,module,label,url,hostname,seriesGuid,millisTaken";
    }
}
