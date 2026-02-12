package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    boolean existsMemberByTypeId(MemberType memberType);

}
