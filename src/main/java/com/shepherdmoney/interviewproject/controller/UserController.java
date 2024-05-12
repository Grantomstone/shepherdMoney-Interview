package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;
    private Logger logger= LoggerFactory.getLogger(this.getClass());

    public UserController(UserRepository userRepository, CreditCardRepository creditCardRepository) {
        this.userRepository = userRepository;
        this.creditCardRepository = creditCardRepository;
    }

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response
        if (payload == null || payload.getEmail() == null || payload.getName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);
        }

        User user = new User();
        user.setEmail(payload.getEmail());
        user.setName(payload.getName());

        User storedUser = userRepository.save(user);
        logger.debug("user saved at ID {}", storedUser.getId());

        return ResponseEntity.status(HttpStatus.OK).body(storedUser.getId());
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate
        Optional<User> potentialUser = userRepository.findById(userId);

        if (potentialUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("no user with userID exists");
        }

        User user = potentialUser.get();

        List<CreditCard> creditCardList = user.getCreditCards();
        for (CreditCard card: creditCardList) {
            creditCardRepository.delete(card);
        }

        userRepository.delete(user);

        return ResponseEntity.status(HttpStatus.OK).body("deletion complete");
    }
}
