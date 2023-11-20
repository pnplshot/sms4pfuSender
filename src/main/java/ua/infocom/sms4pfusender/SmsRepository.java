package ua.infocom.sms4pfusender;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsRepository extends JpaRepository<Sms, Long> {
    List<Sms> findByStatusIsNull();

}