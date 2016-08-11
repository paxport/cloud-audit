package com.cloudburst.audit.biqquery;

import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.bigquery.BigQueryFactory;
import com.cloudburst.bigquery.TableIdentifier;
import com.cloudburst.bigquery.query.BigQueryJobFactory;
import com.cloudburst.bigquery.query.Query;
import com.cloudburst.bigquery.query.QueryJob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//@RestController
//@RequestMapping("/v1/audit/items")
public abstract class AbstractAuditItemController {

    @Autowired
    protected BigQueryFactory factory;

    @Autowired
    protected BigQueryJobFactory jobFactory;

    @Autowired
    private AuditTableIdentifier auditTableIdentifier;

    protected Set<String> queryableColumns = queryableTrackingColumns();

    /**
     * This could be a security risk so need to restrict access to those columns that
     * are secure random tokens like sessionId etc
     *
     * @return set of column names tracking.<foo> that we are allowed to query on
     */
    protected abstract Set<String> queryableTrackingColumns();

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditItem> findItemsByTrackingProps(@RequestParam Map<String,String> queryParams) {
        Map<String,String> filtered = queryParams.entrySet().stream()
                .filter(e -> queryableColumns.contains(e.getKey()))
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        if ( filtered.isEmpty() ) {
            throw new RuntimeException("No valid tracking params found");
        }
        QueryJob<List<AuditItem>> job = startTrackingQueryJob(filtered);
        return job.waitForResult();
    }

    protected QueryJob<List<AuditItem>> startTrackingQueryJob(Map<String, String> filtered) {
        TableIdentifier id = auditTableIdentifier;
        Query<List<AuditItem>> query = new AuditItemOverviewTrackingQuery(id.getDatasetId(),id.getTableId(),filtered);
        return jobFactory.startQuery(id.getProjectId(),query);
    }


    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = "seriesGuid")
    public List<AuditItem> findItemsBySeriesGuid(@RequestParam String seriesGuid) {
        TableIdentifier id = auditTableIdentifier;
        Query<List<AuditItem>> query = new AuditItemSeriesQuery(id.getDatasetId(),id.getTableId(),seriesGuid);
        return jobFactory.startQuery(id.getProjectId(),query).waitForResult();
    }

    @RequestMapping("overview.html")
    public ModelAndView overview(@RequestParam Map<String, String> queryParams) {
        List<AuditItem> items = this.findItemsByTrackingProps(queryParams);
        Map<String,Object> model = new HashMap<>();
        model.put("items", items);
        return new ModelAndView("overview", model);
    }

    @RequestMapping("details.html")
    public ModelAndView detailsForSeriesGuid(@RequestParam String seriesGuid) {
        List<AuditItem> items = this.findItemsBySeriesGuid(seriesGuid);
        Map<String,Object> model = new HashMap<>();
        model.put("items", items);
        return new ModelAndView("details", model);
    }
}
