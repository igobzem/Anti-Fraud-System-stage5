package antifraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository  extends JpaRepository<Transaction, Long> {
    List<Transaction> findByNumber(String number);

    @Query(value = "SELECT COUNT(DISTINCT region) FROM Transaction t WHERE t.date >= ?1 and t.number = ?2 and t.region != ?3")
    long countRegions(Date date, String number, String region);

    @Query(value = "SELECT COUNT(DISTINCT ip) FROM Transaction t WHERE t.date > ?1 and t.date <= ?2 and t.number = ?3 and t.ip != ?4")
    long countIp(Date dateFrom, Date dateTo, String number, String ip);


}

