package com.finance.finance_service.service;

import com.finance.finance_service.client.UserServiceClient;
import com.finance.finance_service.dto.request.FinanceRequest;
import com.finance.finance_service.dto.request.FinanceUpdateReq;
import com.finance.finance_service.dto.response.FinanceResponse;
import com.finance.finance_service.dto.response.MyStats;
import com.finance.finance_service.dto.response.UserResponse;
import com.finance.finance_service.exception.ExternalServiceException;
import com.finance.finance_service.exception.ResourceNotFoundException;
import com.finance.finance_service.exception.UnauthorizedException;
import com.finance.finance_service.mapper.FinanceMapper;
import com.finance.finance_service.model.Category;
import com.finance.finance_service.model.Finance;
import com.finance.finance_service.model.Type;
import com.finance.finance_service.repository.FinanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceRepository financeRepository;
    private final UserServiceClient userServiceClient;

    // ------- PRIVATE HELPERS ---------------------------------------------------------

    private boolean isAuthorize(String requesterUsn, String requesterRole, String usn) {

        log.info("Checking authorization");

        if (requesterRole.equals("ADMIN")) return true;

        return usn.equals(requesterUsn);

    }

    private Map<Finance, String> createMap(List<Finance> list){
        List<String> usns = list.stream()
                .map(Finance::getUsn)
                .collect(Collectors.toList());

        List<UserResponse> resp = userServiceClient.getUsers(usns);

        Map<String, String> usnToName = resp.stream()
                .collect(Collectors.toMap(UserResponse::getUsn, UserResponse::getName));

        return list.stream()
                .collect(Collectors.toMap(
                        finance -> finance,
                        finance -> usnToName.getOrDefault(finance.getUsn(), "Unknown")
                ));
    }

    private Map<String, MyStats> buildMonthlyTrend(List<Finance> records) {

        Map<String, List<Finance>> byMonth = records.stream()
                .collect(Collectors.groupingBy(f ->
                        f.getCreatedAt().getYear() + "-"
                                + String.format("%02d", f.getCreatedAt().getMonthValue())
                ));

        return byMonth.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            double income = entry.getValue().stream()
                                    .filter(f -> f.getType() == Type.INCOME)
                                    .mapToDouble(Finance::getAmount)
                                    .sum();
                            double expense = entry.getValue().stream()
                                    .filter(f -> f.getType() == Type.EXPENSE)
                                    .mapToDouble(Finance::getAmount)
                                    .sum();
                            return MyStats.builder()
                                    .totalIncome(income)
                                    .totalExpense(expense)
                                    .netBalance(income - expense)
                                    .build();
                        }
                ));
    }


    // ------- PUBLIC HELPERS ---------------------------------------------------------

    public FinanceResponse addRecord(FinanceRequest req, String usn) {

        log.info("Attempting to add record in db.");

        UserResponse user = userServiceClient.getUser(usn);
        log.info("{}", user);
        if (user == null) {
            log.info("Failed to validate user, cant add data in finance record.");
            throw new ExternalServiceException(
                    String.format("Failed to validate user with usn: %s", usn)
            );
        }

        try {
            Finance saved = financeRepository.save(
                    Finance.builder()
                            .usn(usn)
                            .amount(req.getAmount())
                            .type(req.getType())
                            .category(req.getCategory())
                            .note(req.getNote())
                            .build()
            );

            return FinanceMapper.toResponse(saved, user.getName());

        } catch (Exception e) {
            log.error("Error while saving finance record", e);
            throw new RuntimeException("Failed to add record", e);
        }
    }

    public FinanceResponse updateRecord(FinanceUpdateReq req, String usn, String role) {

        log.info("Attempting to update record with financeId: {}", req.getFinanceId());

        if (!isAuthorize(usn, role, req.getUsn())) {
            log.info("You are Unauthorized to do this operation");
            throw new UnauthorizedException("You are not authorized to update other's record");
        }

        Finance finance = financeRepository.findById(req.getFinanceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Unable to find record with Id: %d", req.getFinanceId())
                ));

        try{
            finance.setAmount(req.getAmount());
            finance.setType(req.getType());
            finance.setCategory(req.getCategory());
            finance.setNote(req.getNote());

            Finance updated = financeRepository.save(finance);
            UserResponse user = userServiceClient.getUser(req.getUsn());
            return FinanceMapper.toResponse(updated, user.getName());
        }catch(Exception e){
            throw new RuntimeException("Failed to update record");
        }

    }

    public void deleteRecord(Long financeId, String usn, String role) {

        log.info("Attempting to delete record with financeId: {}", financeId);

        try{

            Finance finance = financeRepository.findById(financeId).orElseThrow(
                    () -> new ResourceNotFoundException(
                            String.format("Finance with ID %d not found", financeId))
            );

            if(!isAuthorize(usn, role, finance.getUsn())){
                throw new UnauthorizedException("You are not authorized to update other's record");
            }

            finance.delete();
            financeRepository.save(finance);

            log.info("Deletion successful.");

        }catch(Exception e){
            throw new RuntimeException("Failed to delete record");
        }

    }

    // ------- READ (VIEWER) -----------------------------------------------------------

    public List<FinanceResponse> getMyAllRecords(String usn) {

        log.info("Attempting to fetch all my records");

        List<Finance> finances = financeRepository.findAllByUsnAndIsDeletedFalseOrderByCreatedAtDesc(usn);
        UserResponse user = userServiceClient.getUser(usn);

        return FinanceMapper.toResponseList(finances, user.getName());

    }

    public List<FinanceResponse> getByType(Type type, String usn) {

        log.info("Attempting to fetch all my data with type: {}", type);

        List<Finance> finances = financeRepository.findAllByUsnAndTypeAndIsDeletedFalseOrderByCreatedAtDesc(usn, type);
        UserResponse userResponse = userServiceClient.getUser(usn);

        return FinanceMapper.toResponseList(finances, userResponse.getName());

    }

    public List<FinanceResponse> getByCategory(Category category, String usn) {

        UserResponse user = userServiceClient.getUser(usn);
        log.info("Request received to fetch all my data of {} with category: {}",
                user.getName(), category);

        List<Finance> finances = financeRepository
                .findAllByUsnAndCategoryAndIsDeletedFalseOrderByCreatedAtDesc(usn, category);

        return FinanceMapper.toResponseList(finances, user.getName());

    }

    public MyStats getMyMonthStats(String usn, String month) {

        log.info("Attempting to fetch stats for {} of {}", usn, month);

        UserResponse user = userServiceClient.getUser(usn);

        YearMonth yearMonth = YearMonth.parse(month); // expects "2025-01" format
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Finance> incomeList = financeRepository
                .findByUsnAndTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc
                        (usn, Type.INCOME, false, start, end);

        double totalIncome = incomeList.stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        List<Finance> expenseList = financeRepository
                .findByUsnAndTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc
                        (usn, Type.EXPENSE, false, start, end);

        double totalExpense = expenseList.stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        double netBalance = totalIncome - totalExpense;

        return MyStats.builder()
                .usn(usn)
                .name(user.getName())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .build();

    }

    public MyStats getMyYearStats(String usn, String year) {

        log.info("Attempting to fetch stats for {} of year {}", usn, year);

        UserResponse user = userServiceClient.getUser(usn);

        int yr = Integer.parseInt(year); // expects "2025" format
        LocalDateTime start = LocalDateTime.of
                (yr, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of
                (yr, 12, 31, 23, 59, 59);

        List<Finance> incomeList = financeRepository
                .findByUsnAndTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc
                        (usn, Type.INCOME, false, start, end);

        double totalIncome = incomeList.stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        List<Finance> expenseList = financeRepository
                .findByUsnAndTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc
                        (usn, Type.EXPENSE, false, start, end);

        double totalExpense = expenseList.stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        double netBalance = totalIncome - totalExpense;

        return MyStats.builder()
                .usn(usn)
                .name(user.getName())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .build();

    }

    public MyStats getMyTotalStats(String usn) {

        log.info("Attempting to fetch total income for {}", usn);

        UserResponse user = userServiceClient.getUser(usn);

        List<Finance> incomeList = financeRepository
                .findByUsnAndTypeAndIsDeleted(usn, Type.INCOME, false);

        double totalIncome = incomeList.stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        List<Finance> expenseList = financeRepository
                .findByUsnAndTypeAndIsDeleted(usn, Type.EXPENSE, false);

        double totalExpense = expenseList.stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        double netBalance = totalIncome - totalExpense;

        return MyStats.builder()
                .usn(usn)
                .name(user.getName())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .build();

    }

    public Map<Category, Double> getMyCategoryBreakdown(String usn) {

        log.info("Attempting to fetch category breakdown for usn: {}", usn);

        return financeRepository.findByUsnAndIsDeleted(usn, false)
                .stream()
                .collect(Collectors.groupingBy(
                        Finance::getCategory,
                        Collectors.summingDouble(Finance::getAmount)
                ));
    }

    public Map<String, MyStats> getMyMonthlyTrend(String usn, String year) {

        log.info("Attempting to fetch monthly trend for usn: {}, year: {}", usn, year);

        int yr = Integer.parseInt(year);
        LocalDateTime start = LocalDateTime.of(yr, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(yr, 12, 31, 23, 59, 59);

        List<Finance> records = financeRepository
                .findByUsnAndIsDeletedAndCreatedAtBetween(usn, false, start, end);

        return buildMonthlyTrend(records);
    }


    // ------- READ (ADMIN, ANALYST) -----------------------------------------------------------

    public List<FinanceResponse> getAll(boolean isDeleted) {

        log.info("Attempting to fetch all records");

        List<Finance> list = financeRepository.findAllByIsDeletedOrderByCreatedAtDesc(isDeleted);

        Map<Finance, String> map = createMap(list);

        return FinanceMapper.toResponseList(map);

    }

    public List<FinanceResponse> getAllByType(Type type) {

        log.info("Attempting to fetch all the data with type: {}", type);

        List<Finance> list = financeRepository.findAllByTypeAndIsDeletedFalseOrderByCreatedAtDesc(type);

        Map<Finance, String> map = createMap(list);
        return FinanceMapper.toResponseList(map);

    }

    public List<FinanceResponse> getAllByCategory(Category category) {

        log.info("Attempting to category all the data with type: {}", category);

        List<Finance> list = financeRepository.findAllByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(category);

        Map<Finance, String> map = createMap(list);
        return FinanceMapper.toResponseList(map);

    }

    public MyStats getAllMonthStats(String month) {

        log.info("Attempting to fetch all users stats for month: {}", month);

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        double totalIncome = financeRepository
                .findByTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(Type.INCOME, false, start, end)
                .stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        double totalExpense = financeRepository
                .findByTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(Type.EXPENSE, false, start, end)
                .stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        return MyStats.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(totalIncome - totalExpense)
                .build();

    }

    public MyStats getAllYearStats(String year) {

        log.info("Attempting to fetch all users stats for year: {}", year);

        int yr = Integer.parseInt(year);
        LocalDateTime start = LocalDateTime.of(yr, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(yr, 12, 31, 23, 59, 59);

        double totalIncome = financeRepository
                .findByTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(Type.INCOME, false, start, end)
                .stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        double totalExpense = financeRepository
                .findByTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(Type.EXPENSE, false, start, end)
                .stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        return MyStats.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(totalIncome - totalExpense)
                .build();

    }

    public MyStats getAllTotalStats() {

        log.info("Attempting to fetch total stats across all users");

        double totalIncome = financeRepository
                .findByTypeAndIsDeleted(Type.INCOME, false)
                .stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        double totalExpense = financeRepository
                .findByTypeAndIsDeleted(Type.EXPENSE, false)
                .stream()
                .mapToDouble(Finance::getAmount)
                .sum();

        return MyStats.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(totalIncome - totalExpense)
                .build();

    }

    public Map<Category, Double> getGlobalCategoryBreakdown() {

        log.info("Attempting to fetch global category breakdown");

        return financeRepository.findByIsDeleted(false)
                .stream()
                .collect(Collectors.groupingBy(
                        Finance::getCategory,
                        Collectors.summingDouble(Finance::getAmount)
                ));
    }

    public Map<Type, Double> getGlobalTypeBreakdown() {

        log.info("Attempting to fetch global type breakdown");

        return financeRepository.findByIsDeleted(false)
                .stream()
                .collect(Collectors.groupingBy(
                        Finance::getType,
                        Collectors.summingDouble(Finance::getAmount)
                ));
    }

    public Map<String, MyStats> getGlobalMonthlyTrend(String year) {

        log.info("Attempting to fetch global monthly trend for year: {}", year);

        int yr = Integer.parseInt(year);
        LocalDateTime start = LocalDateTime.of(yr, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(yr, 12, 31, 23, 59, 59);

        List<Finance> records = financeRepository
                .findByIsDeletedAndCreatedAtBetween(false, start, end);

        return buildMonthlyTrend(records);
    }


    // --------------------------------------------------------------------------------------

    public List<String> getTypes() {

        log.info("Attempting to fetch all the types from enum");
        return Arrays.stream(Type.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public List<String> getCategories() {

        log.info("Attempting to fetch all the categories from enum");
        return Arrays.stream(Category.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

}
