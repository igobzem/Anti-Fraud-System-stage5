package antifraud;

import jakarta.persistence.*;

@Entity
@Table(name = "limits")
public class Limit {

    private long id;
    private String number;
    private long limitAllowed;

    private long limitManual;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public long getLimitAllowed() {
        return limitAllowed;
    }

    public void setLimitAllowed(long limitAllowed) {
        this.limitAllowed = limitAllowed;
    }

    public long getLimitManual() {
        return limitManual;
    }

    public void setLimitManual(long limitManual) {
        this.limitManual = limitManual;
    }
}
