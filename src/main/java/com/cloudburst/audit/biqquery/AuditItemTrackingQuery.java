package com.cloudburst.audit.biqquery;

import com.cloudburst.bigquery.query.SQLBuilder;

import java.util.Map;


public class AuditItemTrackingQuery extends AbstractAuditItemQuery {

    private Map<String,String> queryParams;

    public AuditItemTrackingQuery (String datasetId, String tableId, Map<String,String> queryParams) {
        super(tableId, datasetId);
        this.queryParams = queryParams;
    }

    @Override
    public String getSQL() {
        SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT ");
        sql.append ( selectColumns() );
        sql.append(" FROM [");
        sql.append(datasetId).append(".").append(tableId);
        sql.append("]").newLine();
        sql.append("WHERE ");
        boolean first = true;
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            if (first) {
                first = false;
            }
            else{
                sql.append(" AND ");
            }
            sql.append("tracking.");
            sql.append(param.getKey());
            sql.append(" = ");
            sql.quote(param.getValue());
        }
        sql.newLine();
        sql.append("ORDER BY TIMESTAMP ASC").newLine();
        sql.append("LIMIT 200");
        return sql.toString();
    }

    protected String selectColumns() {
        return "*";
    }

}
