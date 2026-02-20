package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {

    long count();

    @Query("SELECT m FROM Member m WHERE FUNCTION('DATE_PART', 'month', m.birthdate) = :currentMonth")
    List<Member> findMembersWithBirthdayInMonth(@Param("currentMonth") int currentMonth);

    boolean existsMemberByCategoryId(MemberCategory memberCategory);

    boolean existsMemberBySubcategoryId(MemberSubCategory memberSubCategory);

    boolean existsMemberByTypeId(MemberType memberType);

    boolean existsMemberByBranch(Branch branch);

    long countByBranch(Branch branch);

    @Query("SELECT m FROM Member m WHERE FUNCTION('DATE_PART', 'month', m.birthdate) = :currentMonth AND m.branch = :branch")
    List<Member> findMembersWithBirthdayInMonthAndBranch(@Param("currentMonth") int currentMonth,
                                                         @Param("branch") Branch branch);

    @Modifying
    @Query(value = """
                UPDATE members
                SET category_id = :newCategoryId,
                    subcategory_id = :subcategoryId
                WHERE birthdate IS NOT NULL
                AND category_id = :currentCategoryId
                AND birthdate BETWEEN 
                    CURRENT_DATE - (:maxAge * INTERVAL '1 year')
                AND
                    CURRENT_DATE - (:minAge * INTERVAL '1 year')
            """, nativeQuery = true)
    int updateByBirthdateRange(
            @Param("currentCategoryId") String currentCategoryId,
            @Param("newCategoryId") String newCategoryId,
            @Param("subcategoryId") String subcategoryId,
            @Param("minAge") int minAge,
            @Param("maxAge") int maxAge
    );

    @Modifying
    @Query(value = """
                UPDATE members
                SET category_id = :newCategoryId,
                    subcategory_id = NULL
                WHERE birthdate IS NOT NULL
                AND category_id = :currentCategoryId
                AND birthdate < CURRENT_DATE - (:age * INTERVAL '1 year')
            """, nativeQuery = true)
    int updateOlderThan(
            @Param("currentCategoryId") String currentCategoryId,
            @Param("newCategoryId") String newCategoryId,
            @Param("age") int age
    );
}
