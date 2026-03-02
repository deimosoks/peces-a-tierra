package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, String>, JpaSpecificationExecutor<Member> {

    long count();

    @Query("SELECT m FROM Member m WHERE FUNCTION('DATE_PART', 'month', m.birthdate) = :currentMonth")
    List<Member> findMembersWithBirthdayInMonth(@Param("currentMonth") int currentMonth);

    boolean existsMemberByCategoryId(MemberCategory memberCategory);

    boolean existsMemberBySubcategoryId(MemberSubCategory memberSubCategory);

    boolean existsMemberByTypeId(MemberType memberType);

    boolean existsMemberByBranch(Branch branch);

    long countByBranch(Branch branch);

    boolean existsByCc(String cc);

    @Query("SELECT m FROM Member m WHERE FUNCTION('DATE_PART', 'month', m.birthdate) = :currentMonth AND m.branch = :branch")
    List<Member> findMembersWithBirthdayInMonthAndBranch(@Param("currentMonth") int currentMonth,
                                                         @Param("branch") Branch branch);

    @Modifying
    @Query(value = """
            UPDATE members m
            SET
                category_id = r.category_id,
                subcategory_id = r.sub_category_id
            FROM (
                SELECT DISTINCT ON (m.id)
                    m.id,
                    cr.category_id,
                    cr.sub_category_id,
                    cr.priority
                FROM members m
                JOIN category_rules cr
                    ON cr.active = true
                    AND (cr.gender IS NULL OR cr.gender = m.gender)
            
                    AND (
                        cr.min_age IS NULL OR
                        AGE(CURRENT_DATE, m.birthdate) >= (cr.min_age || ' years')::interval
                    )
            
                    AND (
                        cr.max_age IS NULL OR
                        AGE(CURRENT_DATE, m.birthdate) <= (cr.max_age || ' years')::interval
                    )
            
                WHERE m.birthdate IS NOT NULL
                  AND m.category_locked = false   
            
                ORDER BY m.id, cr.priority
            ) r
            WHERE m.id = r.id
              AND m.category_locked = false      
            """, nativeQuery = true)
    int applyCategoryRules();

}
