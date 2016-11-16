package com.paxport.cloudaudit.biqquery;

import com.paxport.cloudaudit.model.AuditItem;
import com.paxport.cloudaudit.model.AuditItemType;
import com.paxport.cloudaudit.servlet.DefaultAuditFilter;
import com.paxport.bigquery.BigQueryFactory;
import com.paxport.bigquery.TableIdentifier;
import com.paxport.bigquery.query.BigQueryJobFactory;
import com.paxport.bigquery.query.Query;
import com.paxport.bigquery.query.QueryJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

//@RestController
//@RequestMapping("/v1/audit/items")
public abstract class AbstractAuditItemController {

    private final static Logger logger = LoggerFactory.getLogger(AbstractAuditItemController.class);

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
    protected Set<String> queryableTrackingColumns(){
        Set<String> result = new HashSet<>();
        result.add(DefaultAuditFilter.TRACING_ID);
        result.add(DefaultAuditFilter.LOGICAL_SESSION_ID);
        result.add(DefaultAuditFilter.REQUEST_ID);
        return result;
    }

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
        model.put("overview",generateOverview(items,queryParams));
        return new ModelAndView("auditoverview", model);
    }

    /**
     * Generate Overview
     * @param items
     * @return
     */
    protected Map<String,Object> generateOverview(List<AuditItem> items,Map<String, String> queryParams) {
        Map<String,Object> overview = new HashMap<>();
        overview.putAll(queryParams);
        if ( items.size() > 1 ) {
            AuditItem first = items.get(0);
            AuditItem last = items.stream()
                    .filter(i -> i.getType() == AuditItemType.RESPONSE)
                    .reduce((f, s) -> s).get();

            long envelopeMillis = last.getTimestamp() - first.getTimestamp();
            overview.put("envelopeMillis",envelopeMillis);

            long responseCount = items.stream()
                    .filter(i -> i.getType() == AuditItemType.RESPONSE).count();
            overview.put("responseCount",responseCount);

            long totalTimeRecorded = items.stream()
                    .filter(i -> i.getType() == AuditItemType.RESPONSE)
                    .mapToLong( i -> i.getMillisTaken() ).sum();

            long contentMillis = totalTimeRecorded - last.getMillisTaken();
            overview.put("contentMillis", contentMillis);
        }
        logger.info("Overview --> " + overview);
        return overview;
    }

    @RequestMapping("details.html")
    public ModelAndView detailsForSeriesGuid(@RequestParam String seriesGuid) {
        List<AuditItem> items = this.findItemsBySeriesGuid(seriesGuid);
        Map<String,Object> model = new HashMap<>();
        model.put("items", items);
        return new ModelAndView("auditdetails", model);
    }

    public String script(String scriptname,final HttpServletResponse response) throws IOException {
        InputStream in = this.getClass().getResourceAsStream("/scripts/" + scriptname + ".js");
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(in))) {
            response.setContentType("text/javascript");
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    @RequestMapping(value = "pretty-elements.js")
    public String prettyElements(final HttpServletResponse response) throws IOException {
        return script("pretty-elements",response);
    }

    @RequestMapping(value = "vkbeautify.js")
    public String vkbeautify(final HttpServletResponse response) throws IOException {
        return script("vkbeautify",response);
    }
}
