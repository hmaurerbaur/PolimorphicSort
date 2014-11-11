# Best practice to sort inherited classes #

The following classes describe inheritance between `Contact` and `Person` or between `Contact` and `Organisation`. Goal is to write a PagingAndSortingRepository and use Spring Data sort feature with (eg. `findAll(Sort sort)`)

## Contact ##
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "contactType")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Contact extends AbstractPersistable<Long> {
}
```
```java
@Entity
@DiscriminatorValue(Organisation.DISCRIMINATOR_VALUE)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organisation extends Contact {
    public static final String DISCRIMINATOR_VALUE = "ORGANISATION";

    private String organisationName;
}
```

```java
@Entity
@DiscriminatorValue(Person.DISCRIMINATOR_VALUE)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person extends Contact {
    public static final String DISCRIMINATOR_VALUE = "PERSON";

    private String firstName;

    private String lastName;
}
``` 

```groovy
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
```