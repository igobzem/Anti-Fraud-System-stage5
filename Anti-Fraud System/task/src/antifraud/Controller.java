package antifraud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class Controller {
    @Autowired
    private UserRepository userRepo;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    TransactionService transactionService;

    @PostMapping("/api/antifraud/transaction")
    public ResponseEntity makePurchase(@RequestBody Map<String, String> map) {
        return transactionService.makePurchase(map);
    }

    @PostMapping("/api/antifraud/suspicious-ip")
    public ResponseEntity addSuspiciousIp(@RequestBody Map<String, Object> map) {
        return transactionService.addSuspiciousIp(map);
    }

    @GetMapping("/api/antifraud/suspicious-ip")
    public ResponseEntity getSuspiciousIp() {
        return transactionService.getSuspiciousIp();
    }

    @DeleteMapping("/api/antifraud/suspicious-ip/{ip}")
    public ResponseEntity deleteSuspiciousIp(@PathVariable String ip) {
        return transactionService.deleteSuspiciousIp(ip);
    }

    @PostMapping("/api/antifraud/stolencard")
    public ResponseEntity addStolenCard(@RequestBody Map<String, Object> map) {
        return transactionService.addStolenCard(map);
    }

    @GetMapping("/api/antifraud/stolencard")
    public ResponseEntity getStolenCards() {
        return transactionService.getStolenCards();
    }

    @DeleteMapping("/api/antifraud/stolencard/{number}")
    public ResponseEntity deleteStolenCard(@PathVariable String number) {
        return transactionService.deleteStolenCard(number);
    }


    @PostMapping("/api/auth/user")
    public ResponseEntity auth(@RequestBody User user) {
        if (user.getName() != null && user.getUsername() != null && user.getPassword() != null) {
            Optional<User> optional = userRepo.findByUsernameIgnoreCase(user.getUsername());
            if (optional.isPresent()) {
                return new ResponseEntity(HttpStatus.CONFLICT);
            }
            user.setPassword(encoder.encode(user.getPassword()));
            List<User> list = userRepo.findAll();
            if (list.isEmpty()) {
                user.setRole(User.Role.ADMINISTRATOR.name());
                user.setLocked(false);
            } else {
                user.setRole(User.Role.MERCHANT.name());
                user.setLocked(true);
            }
            user = userRepo.save(user);
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("username", user.getUsername());
            map.put("role", user.getRole());
            return new ResponseEntity(map, HttpStatus.CREATED);
        }
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/api/auth/list")
    public ResponseEntity list() {
        List<User> list = userRepo.findAll();
        List<Map<String,Object>> result = new ArrayList<>();
        for (User user : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("username", user.getUsername());
            map.put("role", user.getRole());
            result.add(map);
        }
        return new ResponseEntity(result, HttpStatus.OK);
    }
    @DeleteMapping("/api/auth/user/{username}")
    public ResponseEntity deleteUser(@PathVariable String username) {
        Optional<User> optional = userRepo.findByUsernameIgnoreCase(username);
        if (optional.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        userRepo.deleteById(optional.get().getId());
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("status", "Deleted successfully!");
        return new ResponseEntity(map, HttpStatus.OK);
    }

    @PutMapping("/api/auth/role")
    public ResponseEntity changeRole(@RequestBody User user) {
         Optional<User> optional = userRepo.findByUsernameIgnoreCase(user.getUsername());
         if (optional.isEmpty()) {
             return new ResponseEntity(HttpStatus.NOT_FOUND);
         }
         if (!User.Role.SUPPORT.name().equals(user.getRole()) && !User.Role.MERCHANT.equals(user.getRole())) {
             return new ResponseEntity(HttpStatus.BAD_REQUEST);
         }
        if (user.getRole().equals(optional.get().getRole()))  {
            return new ResponseEntity(HttpStatus.CONFLICT);
        }
        User entity = optional.get();
        entity.setRole(user.getRole());
        userRepo.save(entity);
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("name", entity.getName());
        map.put("username", entity.getUsername());
        map.put("role", entity.getRole());
        return new ResponseEntity(map, HttpStatus.OK);
    }

    @PutMapping("/api/auth/access")
    public ResponseEntity access(@RequestBody Map<String, String> map) {
        String username = map.get("username");
        Optional<User> optional = userRepo.findByUsernameIgnoreCase(username);
        if (optional.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        User user = optional.get();
        String operation = map.get("operation");
        if ((!"LOCK".equals(operation) && !"UNLOCK".equals(operation))
                || User.Role.ADMINISTRATOR.name().equals(user)) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        boolean locked = "LOCK".equals(operation);
        user.setLocked(locked);
        userRepo.save(user);
        Map<String, Object> result = new HashMap<>();
        String message = "User "+ username + (locked ? " locked!" : " unlocked!");
        result.put("status", message);
        return new ResponseEntity(result, HttpStatus.OK);
    }
}
