package antifraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StolenCardRepository  extends JpaRepository<StolenCard, Long> {
    Optional<StolenCard> findByNumber(String number);
}
