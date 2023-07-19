package antifraud;

import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class TransactionService {
    Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final String regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
    private final Pattern pattern = Pattern.compile(regex);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final String NONE = "none";
    enum Result {ALLOWED, MANUAL_PROCESSING, PROHIBITED};

    @Autowired
    private SuspiciousIpRepository ipRepo;

    @Autowired
    private StolenCardRepository cardRepo;
    @Autowired
    private TransactionRepository transactionRepo;

    public ResponseEntity makePurchase(Map<String, String> map) {
        System.out.println();
        Map<String, Object> result = new HashMap<>();
        Transaction transaction = new Transaction();
        if (!validateData(transaction, map)) {
            result.put("amount", transaction.getAmount());
            result.put("info", NONE);
            return new ResponseEntity(result , HttpStatus.BAD_REQUEST);
        }
        Result resultValue =  checkAmount(transaction);
        resultValue = checkCard(transaction, resultValue);
        resultValue = checkIp(transaction, resultValue);
        resultValue = checkTransaction(transaction, resultValue);
        transaction.setResult(resultValue.name());
        result.put("result", transaction.getResult());
        result.put("info", transaction.getInfo());
        transactionRepo.save(transaction);
        return new ResponseEntity(result, HttpStatus.OK);
    }

    private Result checkTransaction(Transaction transaction, Result result) {
        long cnt = transactionRepo.countRegions(lastHour(transaction.getDate()), transaction.getNumber(),
                transaction.getRegion());
        Result newResult;
        if ( cnt >= 2) {
            if (cnt == 2) {
                newResult = Result.MANUAL_PROCESSING;
            } else {
                newResult = Result.PROHIBITED;
            }
            if (newResult.ordinal() >= result.ordinal()) {
                if (newResult.ordinal() > result.ordinal()) {
                    transaction.setInfo("region-correlation");
                } else {
                    transaction.setInfo(transaction.getInfo() + ", region-correlation");
                }
                result = newResult;
            }
        }
        cnt = transactionRepo.countIp(lastHour(transaction.getDate()), transaction.getDate(), transaction.getNumber(), transaction.getIp());
        if ( cnt >= 2) {
            if (cnt == 2) {
                newResult = Result.MANUAL_PROCESSING;
            } else {
                newResult = Result.PROHIBITED;
            }
            if (newResult.ordinal() >= result.ordinal()) {
                if (newResult.ordinal() > result.ordinal()) {
                    transaction.setInfo("ip-correlation");
                } else {
                    transaction.setInfo(transaction.getInfo() + ", ip-correlation");
                }
                result = newResult;
            }
        }
        return result;
    }
    private boolean validateData(Transaction transaction, Map<String, String> map) {
        String str = map.get("amount");
        Long amount;
        try {
            amount = Long.parseLong(str);
        } catch (NumberFormatException e) {
            amount = 0L;
        }
        transaction.setAmount(amount);
        if (amount <=0) {
            return false;
        }
        String ip = map.get("ip");
        if (ip == null) {
            return false;
        }
        transaction.setIp(ip);
        String number = map.get("number");
        if (number == null) {
            return false;
        }
        transaction.setNumber(number);
        String region = map.get("region");
        if (region == null) {
            return false;
        }
        transaction.setRegion(region);
        String dateStr = map.get("date");
        if (dateStr == null) {
            return false;
        }
        try {
            Date date = dateFormat.parse(dateStr);
            transaction.setDate(date);
        } catch (ParseException e) {
            return false;
        }
        transaction.setInfo(NONE);
        return true;
    }
    private Result checkIp(Transaction transaction, Result result) {
        if (ipRepo.findByIp(transaction.getIp()).isPresent()) {
            if (result != Result.PROHIBITED) {
                transaction.setInfo("ip");
                result = Result.PROHIBITED;
            } else {
                transaction.setInfo(transaction.getInfo() + ", ip");
            }
        }
        return result;
    }

    private Result checkCard(Transaction transaction, Result result) {
        if (cardRepo.findByNumber(transaction.getNumber()).isPresent()) {
            if (result != Result.PROHIBITED) {
                transaction.setInfo("card-number");
                result = Result.PROHIBITED;
            } else {
                transaction.setInfo(transaction.getInfo() + ", card-number");
            }
        }
        return result;
    }

    private void setResult(Transaction transaction, Result result) {
        if (result == Result.PROHIBITED) {
            transaction.setResult(Result.PROHIBITED.name());
        } else if (result == Result.MANUAL_PROCESSING &&
                !Result.PROHIBITED.name().equals(transaction.getResult())) {
            transaction.setResult(Result.MANUAL_PROCESSING.name());
        }
    }
    private Result checkAmount(Transaction transaction) {
        if (transaction.getAmount() <= 200) {
            return Result.ALLOWED;
        }
        transaction.setInfo("amount");
        if (transaction.getAmount() <= 1500) {
            return Result.MANUAL_PROCESSING;
        }
        return Result.PROHIBITED;
    }

    private Date lastHour(Date date) {
       return addHours(date, -1);
    }
    private Date addHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        logger.info("========="+calendar.getTime());
        return calendar.getTime();
    }

    public ResponseEntity addSuspiciousIp(Map<String, Object> map) {
        String ip = map.get("ip").toString();
        if (ip != null && pattern.matcher(ip).find()) {
            Optional<SuspiciousIp> optional = ipRepo.findByIp(ip);
            if (optional.isPresent()) {
                return new ResponseEntity(HttpStatus.CONFLICT);
            }
            SuspiciousIp suspiciousIp = new SuspiciousIp(ip);
            suspiciousIp = ipRepo.save(suspiciousIp);
            map.put("id", suspiciousIp.getId());
            return new ResponseEntity(map, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity getSuspiciousIp() {
        List<SuspiciousIp> list = ipRepo.findAll();
        List<Map<String,Object>> result = new ArrayList<>();
        for (SuspiciousIp ip : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ip.getId());
            map.put("ip", ip.getIp());
            result.add(map);
        }
        return new ResponseEntity(result, HttpStatus.OK);   }

    public ResponseEntity deleteSuspiciousIp(String ip) {
        if (ip != null && pattern.matcher(ip).find()) {
            Optional<SuspiciousIp> optional = ipRepo.findByIp(ip);
            if (optional.isEmpty()) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
            ipRepo.delete(optional.get());
            Map<String, Object> map = new HashMap<>();
            map.put("status", "IP " + ip + " successfully removed!");
            return new ResponseEntity(map, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity addStolenCard(Map<String, Object> map) {
        String number = map.get("number").toString();
        if (number != null && LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(number)) {
            Optional<StolenCard> optional = cardRepo.findByNumber(number);
            if (optional.isPresent()) {
                return new ResponseEntity(HttpStatus.CONFLICT);
            }
            StolenCard stolenCard = new StolenCard(number);
            stolenCard = cardRepo.save(stolenCard);
            map.put("id", stolenCard.getId());
            return new ResponseEntity(map, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity getStolenCards() {
        List<StolenCard> list = cardRepo.findAll();
        List<Map<String,Object>> result = new ArrayList<>();
        for (StolenCard card : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", card.getId());
            map.put("number", card.getNumber());
            result.add(map);
        }
        return new ResponseEntity(result, HttpStatus.OK);
    }

    public ResponseEntity deleteStolenCard(String number) {
        if (number != null && LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(number)) {
            Optional<StolenCard> optional = cardRepo.findByNumber(number);
            if (optional.isEmpty()) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
            cardRepo.delete(optional.get());
            Map<String, Object> map = new HashMap<>();
            map.put("status", "Card " + number + " successfully removed!");
            return new ResponseEntity(map, HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }
}
