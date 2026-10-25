package org.icc.pecesatierra.features.member.repository;

import org.icc.pecesatierra.features.member.MemberNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberNotesRepository extends JpaRepository<MemberNotes, String> {

    List<MemberNotes> findByMember_IdOrderByCreatedAtDesc(String memberId);
}
