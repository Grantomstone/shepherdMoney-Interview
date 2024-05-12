package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public CreditCardController(CreditCardRepository creditCardRepository, UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        if (payload == null || payload.getCardNumber() == null || payload.getCardIssuanceBank() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);
        }

        Optional<User> user = userRepository.findById(payload.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-2);
        }

        Optional<User> expectedUser = userRepository.findById(payload.getUserId());
        if (expectedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-3);
        }

        CreditCard newCard = new CreditCard();
        newCard.setIssuanceBank(payload.getCardIssuanceBank());
        newCard.setNumber(payload.getCardNumber());
        newCard.setOwner(expectedUser.get());
        //newCard.setBalanceHistory(new TreeMap<>());
        expectedUser.get().getCreditCards().add(newCard);

        logger.debug("credit card to be saved is {}", newCard);
        CreditCard storedCard = creditCardRepository.save(newCard);

        return ResponseEntity.status(HttpStatus.OK).body(storedCard.getId());

    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        List<CreditCardView> response = new ArrayList<>();
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        List<CreditCard> creditCardList = creditCardRepository.findAllByOwner_Id(userId);
        for (CreditCard card : creditCardList) {
            CreditCardView view = new CreditCardView(card.getIssuanceBank(), card.getNumber());
            response.add(view);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        Optional<CreditCard> potentialCard = creditCardRepository.findByNumber(creditCardNumber);
        if (potentialCard.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);
        }

        User owner = potentialCard.get().getOwner();

        return ResponseEntity.status(HttpStatus.OK).body(owner.getId());

    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        if (payload == null || payload.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("request ill-formed");
        }

        StringBuilder responseBody = new StringBuilder();

        for (UpdateBalancePayload transaction : payload) {
            Optional<CreditCard> potentialCard = creditCardRepository.findByNumber(transaction.getCreditCardNumber());

            if (potentialCard.isEmpty()) {
                responseBody.append(transaction.getCreditCardNumber());
                responseBody.append(" is not valid. Rejecting\n");
                continue;
            }

            CreditCard creditCard = potentialCard.get();
            Double transactionBalance = transaction.getBalanceAmount();
            LocalDate transactionDate = transaction.getBalanceDate();
            LocalDate currentDate = LocalDate.now();

            BalanceHistory payloadHistory = new BalanceHistory();
            payloadHistory.setCard(creditCard);
            payloadHistory.setDate(transactionDate);

            List<BalanceHistory> cardBalanceHistory = creditCard.getBalanceHistory();
            cardBalanceHistory.sort(Comparator.comparing(BalanceHistory::getDate).reversed());

            if ( cardBalanceHistory.isEmpty() ) {
                // add all dates from input till today with the same balance
                for (LocalDate date = transactionDate; !date.isAfter(currentDate); date = date.plusDays(1)) {
                    BalanceHistory copy = new BalanceHistory(date, transactionBalance, creditCard);
                    logger.debug("balanceHistory being added: {}", copy);
                    cardBalanceHistory.add(0, copy);
                }
            }
            else {
                // find balance just before date of the transaction
                int closestIndex = findClosestSmallerIndex(cardBalanceHistory, transactionDate);
                if (closestIndex == 0) {
                    // if at the front of the list, put it at the front
                    for (LocalDate date = transactionDate; !date.isAfter(currentDate); date = date.plusDays(1)) {
                        BalanceHistory copy = new BalanceHistory(date, transactionBalance, creditCard);
                        logger.debug("balanceHistory being added: {}", copy);
                        cardBalanceHistory.add(0, copy);
                    }

                } else if (closestIndex == -1) {
                    // if at the end, propagate entire balance
                    double oldestBalance = cardBalanceHistory.get(cardBalanceHistory.size() - 1).getBalance();
                    double propagationValue = transactionBalance - oldestBalance;
                    logger.debug("index: {}, old amount: {}, added amount: {}, propagation: {}", closestIndex, oldestBalance, transactionBalance, propagationValue);
                    for (BalanceHistory copy : cardBalanceHistory) {
                        copy.setBalance(copy.getBalance() + propagationValue);
                    }
                    payloadHistory.setBalance(transactionBalance);
                    cardBalanceHistory.add(payloadHistory);

                } else {
                    // if in the middle, find the diff in balance and propagate that
                    double oldestBalance = cardBalanceHistory.get(closestIndex).getBalance();
                    double propagationValue = transactionBalance - oldestBalance;
                    logger.debug("index: {}, old amount: {}, added amount: {}, propagation: {}", closestIndex, oldestBalance, transactionBalance, propagationValue);
                    for (int i = 0; i < closestIndex; i++) {
                        BalanceHistory copy = cardBalanceHistory.get(i);
                        copy.setBalance(copy.getBalance() + propagationValue);
                    }
                    payloadHistory.setBalance(transactionBalance);
                    cardBalanceHistory.add(closestIndex + 1, payloadHistory);
                }
            }
            creditCardRepository.save(creditCard);
        }

        if (responseBody.isEmpty()) {
            responseBody.append("All entries successfully added");
        } else {
            responseBody.append("Partial entries successfully added");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody.toString());
    }

    private int findClosestSmallerIndex(List<BalanceHistory> objects, LocalDate targetDate) {
        int closestIndex = -1; // Initialize with -1 to indicate no match found
        LocalDate closestDate = null; // Initialize with null to indicate no match found

        for (int i = 0; i < objects.size(); i++) {
            LocalDate currentDate = objects.get(i).getDate();
            if (currentDate.isBefore(targetDate)) {
                logger.debug("date before: {}, date after: {}", currentDate, closestDate);
                if (closestDate == null || currentDate.isAfter(closestDate)) {
                    closestDate = currentDate;
                    closestIndex = i;
                }
            }
        }

        return closestIndex;
    }

}
