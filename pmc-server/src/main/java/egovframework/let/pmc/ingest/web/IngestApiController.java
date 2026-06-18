package egovframework.let.pmc.ingest.web;

import egovframework.let.pmc.common.ApiResponse;
import egovframework.let.pmc.ingest.service.IncomingResult;
import egovframework.let.pmc.ingest.service.IngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 점검 결과 수신 REST.
 */
@RestController
@RequestMapping("/api/v1/inspections")
public class IngestApiController {

    private final IngestService ingestService;

    @Autowired
    public IngestApiController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/results")
    public ApiResponse<Map<String, Object>> results(@RequestBody IncomingResult body,
                                                    HttpServletRequest req) {
        return ApiResponse.ok(ingestService.ingest(body, req.getRemoteAddr()));
    }
}
