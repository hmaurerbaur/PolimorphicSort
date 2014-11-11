# Best practice to sort inherited classes #

The following classes describe inheritance between `Contact` and `Person` or between `Contact` and `Organisation`. Goal is to write a PagingAndSortingRepository and use Spring Data sort feature with (eg. `findAll(Sort sort)`)

## Entities ##
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

## Test ##
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
Currently this test fails because neither lastName nor organisationName are properties of contact.
What´s the best practice to make this test work?


## Proposed solution ##

```java
/**
 * Handles sorting in inherited classes original {@link org.springframework.data.jpa.repository.support.QueryDslRepositorySupport} implementation
 * only allows sorting of attributes in base class.
 */
public class PolymorphicQueryDsl extends Querydsl {
    private final EntityManager entityManager;
    private final PathBuilder<?> builder;
    private final QueryDslCustomOrderByHandler orderByHandler;
    private final ClassHierarchyMetaData classHierarchyMetaData;
    private final String propertyPrefix;

    public PolymorphicQueryDsl(EntityManager entityManager, PathBuilder<?> builder, EntityPath<?> propertyPath, QueryDslCustomOrderByHandler orderByHandler,
                               ClassHierarchyMetaData classHierarchyMetadata) {
        super(entityManager, builder);
        this.entityManager = entityManager;
        this.builder = builder;
        this.orderByHandler = orderByHandler;
        this.classHierarchyMetaData = classHierarchyMetadata;
        this.propertyPrefix = toPrefix(propertyPath);
    }

    // MODIFICATION: NEW
    private String toPrefix(EntityPath<?> propertyPath) {
        String path = "";
        if (propertyPath != null) {
            path = propertyPath.getMetadata().getName() + ".";
        }
        return path;
    }

    @Override
    public JPQLQuery applySorting(Sort sort, JPQLQuery query) {
        if (sort == null) {
            return query;
        }

        for (Sort.Order order : sort) {
            query.orderBy(toOrder(order, query));
        }

        return query;
    }

    /**
     * Transforms a plain {@link org.springframework.data.domain.Sort.Order} into a QueryDsl specific {@link com.mysema.query.types.OrderSpecifier}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private OrderSpecifier<?> toOrder(Sort.Order order, JPQLQuery query) {
        Expression<?> expression = createExpressionAndPotentionallyAddLeftJoinForReferencedAssociation(order, query, propertyPrefix + order.getProperty(), builder);

        return new OrderSpecifier(order.isAscending() ? ASC : DESC, expression);
    }

    /**
     * This is a copy of {@link org.springframework.data.jpa.repository.support.Querydsl#createExpressionAndPotentionallyAddLeftJoinForReferencedAssociation(org.springframework.data.domain.Sort.Order, com.mysema.query.jpa.JPQLQuery)}
     * MODIFICATIONS are marked in comments
     * <p/>
     * Potentially adds a left join to the given {@link com.mysema.query.jpa.JPQLQuery} query if the order contains a property path that uses
     * an association and returns the property expression build from the path of the association.
     *
     * @param builder
     * @param order    must not be {@literal null}.
     * @param query    must not be {@literal null}.
     * @param property
     * @return property expression.
     */
    private Expression<?> createExpressionAndPotentionallyAddLeftJoinForReferencedAssociation(@NonNull Sort.Order order, @NonNull JPQLQuery query, String property, PathBuilder<?> builder) {
        if (!property.contains(".")) {
            // MODIFICATION: NEW
            if (orderByHandler != null) {
                Expression<?> expression = orderByHandler.toOrder(order, query, builder);
                if (expression != null) {
                    return expression;
                }
            }
            // MODIFICATION: NEW

            // Apply ignore case in case we have a String and ignore case ordering is requested
            StringPath path = builder.getString(property);
            return order.isIgnoreCase() ? path.lower() : path;
        }

        // MODIFICATION: moved to  attributeSet
        //Set<Attribute<?, ?>> combinedAttributes = new LinkedHashSet<Attribute<?, ?>>();
        //combinedAttributes.addAll(entitytype.getSingularAttributes());
        //combinedAttributes.addAll(entitytype.getPluralAttributes());
        // MODIFICATION: moved to  attributeSet

        for (Attribute<?, ?> attribute : attributeSet(builder.getType())) {
            if (property.startsWith(attribute.getName() + ".")) {

                switch (attribute.getPersistentAttributeType()) {
                    case EMBEDDED:
                        return builder.get(property);
                    default:
                        return createLeftJoinForAttributeInOrderBy(builder, property, attribute, order, query);
                }
            }
        }

        throw new IllegalArgumentException(
                String.format("Could not create property expression for %s", property));
    }

    private Set<Attribute<?, ?>> attributeSet(Class<?> clazz) {
        EntityType<?> entityType = entityManager.getMetamodel().entity(clazz);

        Set<Attribute<?, ?>> combinedAttributes = new LinkedHashSet<>();
        combinedAttributes.addAll(entityType.getSingularAttributes());
        combinedAttributes.addAll(entityType.getPluralAttributes());

        Class<?>[] classes = classHierarchyMetaData.subTypesForClass(clazz);
        if (classes != null && classes.length > 0) {
            for (Class<?> aClass : classes) {
                combinedAttributes.addAll(attributeSet(aClass));
            }
        }

        return combinedAttributes;
    }

    /**
     * Copy from {@link org.springframework.data.jpa.repository.support.Querydsl#createLeftJoinForAttributeInOrderBy(javax.persistence.metamodel.Attribute, org.springframework.data.domain.Sort.Order, com.mysema.query.jpa.JPQLQuery)}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Expression<?> createLeftJoinForAttributeInOrderBy(PathBuilder<?> builder, String property, Attribute<?, ?> attribute, Sort.Order order, JPQLQuery query) {
        String alias = attribute.getName() + "_";
        EntityPathBase<?> associationPathRoot = new EntityPathBase<Object>(attribute.getJavaType(), alias);
        query.leftJoin((EntityPath) builder.get(attribute.getName()), associationPathRoot);
        PathBuilder<Object> attributePathBuilder = new PathBuilder<>(attribute.getJavaType(),
                associationPathRoot.getMetadata());

        String nestedAttributePath = property.substring((attribute.getName()).length() + 1); // exclude "."
        return createExpressionAndPotentionallyAddLeftJoinForReferencedAssociation(order, query, nestedAttributePath, attributePathBuilder);
        //ORIGINAL return order.isIgnoreCase() ? attributePathBuilder.getString(nestedAttributePath).lower() : attributePathBuilder.get(nestedAttributePath);
    }
}
```