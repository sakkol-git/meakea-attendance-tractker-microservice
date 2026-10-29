package com.hrflow.notification.repository;

import com.hrflow.notification.entity.NotificationPreference;
import com.hrflow.notification.entity.NotificationPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, NotificationPreferenceId> {
}
