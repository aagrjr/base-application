package br.com.base.application.repository;

import br.com.base.application.domain.Customer;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, ObjectId> {

    boolean existsByName(String documentNumber);

}
