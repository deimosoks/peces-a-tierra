package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberNotesRepository extends JpaRepository<MemberNotes, String> {
    List<MemberNotes> findByMember(Member member);
}
