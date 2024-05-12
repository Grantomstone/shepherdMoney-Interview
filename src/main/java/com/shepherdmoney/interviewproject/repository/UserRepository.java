package com.shepherdmoney.interviewproject.repository;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.h2.command.ddl.CreateUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Crud Repository to store User classes
 */
@Repository("UserRepo")
public interface UserRepository extends JpaRepository<User, Integer> {



}
