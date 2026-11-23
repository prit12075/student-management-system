package com.university.sms.repository;

import com.university.sms.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByPinnedDescPublishedDateDesc();
    List<Notice> findByTargetAudienceInOrderByPinnedDescPublishedDateDesc(List<String> audiences);
}

