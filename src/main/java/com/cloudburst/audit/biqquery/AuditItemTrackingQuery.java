package com.cloudburst.audit.biqquery;

import com.google.api.services.bigquery.model.GetQueryResultsResponse;
import com.google.api.services.bigquery.model.TableCell;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;

import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.audit.model.ImmutableAuditItem;
import com.cloudburst.bigquery.CellUtils;
import com.cloudburst.bigquery.query.Query;
import com.cloudburst.bigquery.query.SQLBuilder;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AuditItemTrackingQuery implements Query<List<AuditItem>> {

    private String datasetId;

    private String tableId;

    private Map<String,String> queryParams;

    public AuditItemTrackingQuery (String datasetId, String tableId, Map<String,String> queryParams) {
        this.datasetId = datasetId;
        this.tableId = tableId;
        this.queryParams = queryParams;
    }

    @Override
    public String getSQL() {
        SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT * FROM [");
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

    @Override
    public List<AuditItem> buildResult(GetQueryResultsResponse response) {
        List<AuditItem> result = new ArrayList<>();
        List<TableFieldSchema> fields = response.getSchema().getFields();
        List<TableRow> rows = response.getRows();
        for (TableRow row : rows) {
            result.add(buildItem(row,fields));
        }
        return result;
    }

    protected AuditItem buildItem(TableRow row, List<TableFieldSchema> fields) {
        Map<String,String> tracking = new HashMap<>();
        ImmutableAuditItem.Builder builder = ImmutableAuditItem.builder();
        List<TableCell> cells = row.getF();
        for (int i = 0; i < fields.size(); i++) {
            TableFieldSchema field = fields.get(i);
            Object cell = cells.get(i).getV();
            if ( cell == null || cell.getClass().equals(Object.class) ) {
                continue;
            }
            if ( field.getName().startsWith("tracking_") && cell instanceof String ) {
                tracking.put(field.getName().substring(9),(String)cell);
            }
            else{
                setPropertyOnBuilder(builder,cell,field);
            }
        }
        builder.tracking(tracking);
        return builder.build();
    }

    protected void setPropertyOnBuilder(ImmutableAuditItem.Builder builder, Object value, TableFieldSchema field) {
        switch (field.getName()) {
            case "timestamp":
                builder.timestamp(CellUtils.toLongTimestamp(value).longValue());
                break;

            case "millisTaken":
                builder.millisTaken(CellUtils.toLong(value).longValue());
                break;

            default:
                setPropertyUsingReflection(builder, value, field);
                break;
        }
    }

    protected void setPropertyUsingReflection(ImmutableAuditItem.Builder builder, Object value, TableFieldSchema field) {
        Method setter = ReflectionUtils.findMethod(builder.getClass(),field.getName(),value.getClass());
        if ( setter != null ) {
            try {
                setter.invoke(builder,value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("failed to set field: " + field.getName() + " of type " + field.getType(), e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("failed to set field: " + field.getName() + " of type " + field.getType(), e);
            }
        }
        else {
            throw new RuntimeException("Failed to find setter on builder for prop: " + field.getName() + " converted value of type: " + value.getClass().getName() + " --> " + value );
        }
    }
}
