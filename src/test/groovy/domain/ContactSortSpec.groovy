package domain

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Unroll

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@ContextConfiguration(classes = [TestConfiguration])
@TransactionConfiguration(defaultRollback = true)
@Transactional
@Unroll
class ContactSortSpec extends Specification {
    @Autowired
    ContactRepository repository

    @PersistenceContext
    EntityManager entityManager


    def "allow #sortExpression of inherited class"() {
        given:
        entityManager.merge(new Person(firstName: "john", lastName: "doe"))
        entityManager.flush()

        when:
        // Currently throws Exception eg "No property organisationName found for type Contact!"
        def contacts = repository.findAll(new Sort(sortExpression))

        then:
        contacts.size() == 1

        where:
        sortExpression << ["lastName", "organisationName"]
    }
}
