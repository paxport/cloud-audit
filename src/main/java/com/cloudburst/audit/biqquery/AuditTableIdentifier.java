package com.cloudburst.audit.biqquery;

import com.cloudburst.bigquery.TableIdentifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuditTableIdentifier implements TableIdentifier {

    @Value("${audit.bigquery.projectId}")
    private String projectId;

    @Value("${audit.bigquery.datasetId}")
    private String datasetId;

    @Value("${audit.bigquery.tableId}")
    private String tableId;

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public String getDatasetId() {
        return datasetId;
    }

    @Override
    public String getTableId() {
        return tableId;
    }
}
