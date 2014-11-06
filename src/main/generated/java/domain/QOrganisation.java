package domain;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QOrganisation is a Querydsl query type for Organisation
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QOrganisation extends EntityPathBase<Organisation> {

    private static final long serialVersionUID = 1784449508L;

    public static final QOrganisation organisation = new QOrganisation("organisation");

    public final QContact _super = new QContact(this);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath organisationName = createString("organisationName");

    public QOrganisation(String variable) {
        super(Organisation.class, forVariable(variable));
    }

    public QOrganisation(Path<? extends Organisation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrganisation(PathMetadata<?> metadata) {
        super(Organisation.class, metadata);
    }

}

