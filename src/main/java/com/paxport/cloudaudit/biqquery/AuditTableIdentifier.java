package com.paxport.cloudaudit.biqquery;

import com.paxport.bigquery.TableIdentifier;

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


    public AuditTableIdentifier(){
    }

    public AuditTableIdentifier(String project,String dataset,String table){
        this.projectId = project;
        this.datasetId = dataset;
        this.tableId = table;
    }

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
