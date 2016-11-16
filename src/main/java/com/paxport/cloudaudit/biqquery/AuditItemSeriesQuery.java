package com.paxport.cloudaudit.biqquery;

import com.paxport.bigquery.query.SQLBuilder;


public class AuditItemSeriesQuery extends AbstractAuditItemQuery {

    private String seriesGuid;

    public AuditItemSeriesQuery(String datasetId, String tableId, String seriesGuid) {
        super(tableId, datasetId);
        this.seriesGuid = seriesGuid;
    }

    @Override
    public String getSQL() {
        SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT ");
        sql.append ( selectColumns() );
        sql.append(" FROM [");
        sql.append(datasetId).append(".").append(tableId);
        sql.append("]").newLine();
        sql.append("WHERE seriesGuid = ");
        sql.quote(seriesGuid);
        sql.newLine();
        sql.append("ORDER BY TIMESTAMP ASC").newLine();
        sql.append("LIMIT 200");
        return sql.toString();
    }

    protected String selectColumns() {
        return "*";
    }

}
