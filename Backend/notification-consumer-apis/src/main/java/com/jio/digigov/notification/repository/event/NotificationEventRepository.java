package com.jio.digigov.notification.repository.event;

import com.jio.digigov.notification.entity.event.NotificationEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationEventRepository extends MongoRepository<NotificationEvent, String> {

    Optional<NotificationEvent> findByEventIdAndBusinessId(String eventId, String businessId);
}