package com.finance.finance_service.repository;

import com.finance.finance_service.model.Category;
import com.finance.finance_service.model.Finance;
import com.finance.finance_service.model.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FinanceRepository extends JpaRepository<Finance, Long> {


    List<Finance> findAllByIsDeletedOrderByCreatedAtDesc(boolean isDeleted);

    List<Finance> findAllByUsnOrderByCreatedAtDesc(String usn);

    List<Finance> findAllByUsnAndTypeOrderByCreatedAtDesc(Type type);

    List<Finance> findAllByUsnAndCategoryOrderByCreatedAtDesc(Category category);

    List<Finance> findAllByTypeOrderByCreatedAtDesc(Type type);

    List<Finance> findAllByCategoryOrderByCreatedAtDesc(Category category);

    List<Finance> findByUsnAndTypeAndIsDeleted(String usn, Type type, boolean b);

    List<Finance> findByUsnAndTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc
            (String usn, Type type, boolean b, LocalDateTime start, LocalDateTime end);

    List<Finance> findByTypeAndIsDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
            Type type, boolean isDeleted, LocalDateTime start, LocalDateTime end
    );

    List<Finance> findByTypeAndIsDeleted(Type type, boolean isDeleted);

    List<Finance> findByUsnAndIsDeleted(String usn, boolean isDeleted);

    List<Finance> findByIsDeleted(boolean isDeleted);

    List<Finance> findByUsnAndIsDeletedAndCreatedAtBetween(String usn, boolean isDeleted, LocalDateTime start, LocalDateTime end);

    List<Finance> findByIsDeletedAndCreatedAtBetween(boolean isDeleted, LocalDateTime start, LocalDateTime end);
}
