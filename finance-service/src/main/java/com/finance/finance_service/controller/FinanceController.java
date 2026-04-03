package com.finance.finance_service.controller;

import com.finance.finance_service.dto.ApiResponse;
import com.finance.finance_service.dto.request.FinanceRequest;
import com.finance.finance_service.dto.request.FinanceUpdateReq;
import com.finance.finance_service.dto.response.FinanceResponse;
import com.finance.finance_service.dto.response.MyStats;
import com.finance.finance_service.model.Category;
import com.finance.finance_service.model.Type;
import com.finance.finance_service.service.FinanceService;
import com.finance.finance_service.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final JwtService jwtService;

    // ----------------------------------------------------------------------------

    private String Usn(HttpServletRequest http) {
        return jwtService.extractUsnFromHeaders(http);
    }

    private String Role(HttpServletRequest http) {
        return jwtService.extractRoleFromHeaders(http);
    }

    // ------------------------------------------------------------------------------

    @GetMapping("/v/getTypes")
    public ResponseEntity<ApiResponse<List<String>>> getTypes(){

        log.info("Request received to fetch types");
        List<String> resp = financeService.getTypes();

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    @GetMapping("/v/getCategories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories(){

        log.info("Request received to fetch categories");
        List<String> resp = financeService.getCategories();

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    // ------ WRITE ---------------------------------------------------------------

    @PostMapping("/v/addRecord")
    public ResponseEntity<ApiResponse<FinanceResponse>> addRecord(
            @RequestBody FinanceRequest req,
            HttpServletRequest http
    ) {

        String usn = Usn(http);
        log.info("Request received to add record for usn: {}", usn);

        FinanceResponse resp = financeService.addRecord(req, usn);

        return ResponseEntity.ok(ApiResponse.success(
                "Successfully added data.",
                resp
        ));

    }

    @PutMapping("/v/updateRecord")
    public ResponseEntity<ApiResponse<FinanceResponse>> updateRecord(
            @RequestBody FinanceUpdateReq req,
            HttpServletRequest http
    ) {

        log.info("Request received to update record with id: {}", req.getFinanceId());

        FinanceResponse resp = financeService.updateRecord(req, Usn(http), Role(http));
        return ResponseEntity.ok(ApiResponse.success(
                "Successfully updated record",
                resp
        ));

    }

    @DeleteMapping("/v/deleteRecord/{financeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(
            @PathVariable Long financeId,
            HttpServletRequest http
    ) {

        log.info("Request received to delete record with id: {}", financeId);

        financeService.deleteRecord(financeId, Usn(http), Role(http));
        return ResponseEntity.ok(ApiResponse.success(
                "Successfully deleted record", null
        ));

    }

    // ------ READ (VIEWER) ---------------------------------------------------------------

    @GetMapping("/v/getMyRecords")
    public ResponseEntity<ApiResponse<List<FinanceResponse>>> getMyRecords(
            HttpServletRequest req
    ) {
        String usn = Usn(req);
        log.info("Request received to fetch all records of {}", usn);

        List<FinanceResponse> resp = financeService.getMyAllRecords(usn);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    @GetMapping("/v/getByType/{type}")
    public ResponseEntity<ApiResponse<List<FinanceResponse>>> getByType(
            @PathVariable Type type,
            HttpServletRequest req
    ) {

        String usn = Usn(req);
        log.info("Request received to fetch all data of {} with type: {}", usn, type);

        List<FinanceResponse> resp = financeService.getByType(type, usn);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    @GetMapping("/v/getByCategory/{category}")
    public ResponseEntity<ApiResponse<List<FinanceResponse>>> getByCategory(
            @PathVariable Category category,
            HttpServletRequest req
    ) {

        String usn = Usn(req);
        log.info("Request received to fetch all data of {} with category: {}", usn, category);

        List<FinanceResponse> resp = financeService.getByCategory(category, usn);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    @GetMapping("/v/getMyMonthlyStats/{month}")
    public ResponseEntity<ApiResponse<MyStats>> getMyMonthlyStats(
            @PathVariable String month,  // format: 2026-04
            HttpServletRequest req
    ){

        String usn = Usn(req);
        log.info("Request received to fetch monthly income for {}", usn);

        MyStats resp = financeService.getMyMonthStats(usn, month);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully Fetched stats for month %s", month),
                resp
        ));

    }

    @GetMapping("/v/getMyYearlyStats/{year}")
    public ResponseEntity<ApiResponse<MyStats>> getMyYearlyStats(
            @PathVariable String year,
            HttpServletRequest req
    ){

        String usn = Usn(req);
        log.info("Request received to fetch monthly income for {}", usn);

        MyStats resp = financeService.getMyYearStats(usn, year);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully Fetched stats for month %s", year),
                resp
        ));

    }

    @GetMapping("/v/getMyTotalStats")
    public ResponseEntity<ApiResponse<MyStats>> getMyTotalStats(
            HttpServletRequest req
    ){

        String usn = Usn(req);
        log.info("Request received to fetch total income for {}", usn);

        MyStats resp = financeService.getMyTotalStats(usn);
        return ResponseEntity.ok(ApiResponse.success(
                "Successfully fetched total stats.",
                resp
        ));

    }

    @GetMapping("/v/getCategoryBreakdown")
    public ResponseEntity<ApiResponse<Map<Category, Double>>> getMyCategoryBreakdown(
            HttpServletRequest req
    ) {
        String usn = Usn(req);
        log.info("Request received for category breakdown for usn: {}", usn);
        Map<Category, Double> resp = financeService.getMyCategoryBreakdown(usn);
        return ResponseEntity.ok(ApiResponse.success("Category breakdown fetched successfully", resp));
    }

    @GetMapping("/v/getMonthlyTrend/{year}")
    public ResponseEntity<ApiResponse<Map<String, MyStats>>> getMyMonthlyTrend(
            @PathVariable String year,
            HttpServletRequest req
    ) {
        String usn = Usn(req);
        log.info("Request received for monthly trend for usn: {}, year: {}", usn, year);
        Map<String, MyStats> resp = financeService.getMyMonthlyTrend(usn, year);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Monthly trend for %s fetched successfully", year), resp
        ));
    }

    // ------ READ (ADMIN, ANALYST) ---------------------------------------------------------------

    @GetMapping("/a/getAll/{isDeleted}")
    public ResponseEntity<ApiResponse<List<FinanceResponse>>> getAllRecords(
            @PathVariable boolean isDeleted
    ) {
        String info = isDeleted ? "Active" : "Deleted";
        log.info("Request received to fetch all {} data", info);

        List<FinanceResponse> resp = financeService.getAll(isDeleted);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));
    }


    @GetMapping("/a/getAllByType/{type}")
    public ResponseEntity<ApiResponse<List<FinanceResponse>>> getAllByType(
            @PathVariable Type type
    ) {

        log.info("Request received to fetch all data with type: {}", type);

        List<FinanceResponse> resp = financeService.getAllByType(type);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    @GetMapping("/a/getAllByCategory/{type}")
    public ResponseEntity<ApiResponse<List<FinanceResponse>>> getAllByCategory(
            @PathVariable Category category
    ) {

        log.info("Request received to fetch all data with category: {}", category);

        List<FinanceResponse> resp = financeService.getAllByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetched records of size %d", resp.size()),
                resp
        ));

    }

    @GetMapping("/a/getMyMonthlyStats/{month}")
    public ResponseEntity<ApiResponse<MyStats>> getAllMonthlyStats(
            @PathVariable String month
    ){

        log.info("Request received to fetch all monthly stats");

        MyStats resp = financeService.getAllMonthStats(month);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully Fetched all stats for month %s", month),
                resp
        ));

    }

    @GetMapping("/a/getMyYearlyStats/{year}")
    public ResponseEntity<ApiResponse<MyStats>> getAllYearlyStats(
            @PathVariable String year
    ){

        log.info("Request received to fetch all yearly stats");

        MyStats resp = financeService.getAllYearStats(year);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully Fetched stats for month %s", year),
                resp
        ));

    }

    @GetMapping("/a/getMyTotalStats")
    public ResponseEntity<ApiResponse<MyStats>> getAllTotalStats(
            HttpServletRequest req
    ){



        log.info("Request received to fetch all totalStats");

        MyStats resp = financeService.getAllTotalStats();
        return ResponseEntity.ok(ApiResponse.success(
                "Successfully fetched total stats.",
                resp
        ));

    }

    @GetMapping("/a/getCategoryBreakdown")
    public ResponseEntity<ApiResponse<Map<Category, Double>>> getGlobalCategoryBreakdown() {

        log.info("Request received for global category breakdown");

        Map<Category, Double> resp = financeService.getGlobalCategoryBreakdown();
        return ResponseEntity.ok(ApiResponse.success(
                "Global category breakdown fetched successfully",
                resp
        ));
    }

    @GetMapping("/a/getTypeBreakdown")
    public ResponseEntity<ApiResponse<Map<Type, Double>>> getGlobalTypeBreakdown() {
        log.info("Request received for global type breakdown");
        Map<Type, Double> resp = financeService.getGlobalTypeBreakdown();
        return ResponseEntity.ok(ApiResponse.success(
                "Global type breakdown fetched successfully",
                resp
        ));
    }

    @GetMapping("/a/getMonthlyTrend/{year}")
    public ResponseEntity<ApiResponse<Map<String, MyStats>>> getGlobalMonthlyTrend(
            @PathVariable String year
    ) {
        log.info("Request received for global monthly trend for year: {}", year);
        Map<String, MyStats> resp = financeService.getGlobalMonthlyTrend(year);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Global monthly trend for %s fetched successfully", year),
                resp
        ));
    }

}
