package domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(Organisation.DISCRIMINATOR_VALUE)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organisation extends Contact {
    public static final String DISCRIMINATOR_VALUE = "ORGANISATION";

    private String organisationName;
}
