package com.centit.product.metadata.graphql;

import com.centit.product.metadata.po.MetaColumn;
import com.centit.product.metadata.po.MetaTable;
import com.centit.product.metadata.service.MetaDataService;
import com.centit.support.database.utils.FieldType;
import graphql.Scalars;
import graphql.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A wrapper for the {@link GraphQLSchema.Builder}. In addition to exposing the traditional builder functionality,
 * this class constructs an initial {@link GraphQLSchema} by scanning the given {@link EntityManager} for relevant
 * JPA entities. This happens at construction time.
 *
 * Note: This class should not be accessed outside this library.
 */
public class GraphQLSchemaBuilder extends GraphQLSchema.Builder {

    public static final String PAGINATION_REQUEST_PARAM_NAME = "paginationRequest";
    private static final Logger log = LoggerFactory.getLogger(GraphQLSchemaBuilder.class);

    private final MetaDataService metaDataService;
    private final String databaseId;

    private final Map<Class, GraphQLType> classCache = new HashMap<>();
    private final Map<EmbeddableType<?>, GraphQLObjectType> embeddableCache = new HashMap<>();
    private final Map<EntityType, GraphQLObjectType> entityCache = new HashMap<>();
    private final List<AttributeMapper> attributeMappers = new ArrayList<>();

    /**
     * Initialises the builder with the given {@link EntityManager} from which we immediately start to scan for
     * entities to include in the GraphQL schema.
     * @param metaDataService MetaDataService The manager containing the data models to include in the final GraphQL schema.
     */
    public GraphQLSchemaBuilder(MetaDataService metaDataService, String databaseId) {
        this.metaDataService = metaDataService;
        this.databaseId = databaseId;
        populateStandardAttributeMappers();

        super.query(getQueryType());
    }

    public GraphQLSchemaBuilder(MetaDataService metaDataService, String databaseId, Collection<AttributeMapper> attributeMappers) {
        this.metaDataService = metaDataService;
        this.databaseId = databaseId;
        this.attributeMappers.addAll(attributeMappers);
        populateStandardAttributeMappers();

        super.query(getQueryType());
    }

    private void populateStandardAttributeMappers() {
        attributeMappers.add(createStandardAttributeMapper(UUID.class, JavaScalars.GraphQLUUID));
        attributeMappers.add(createStandardAttributeMapper(Date.class, JavaScalars.GraphQLDate));
        attributeMappers.add(createStandardAttributeMapper(LocalDateTime.class, JavaScalars.GraphQLLocalDateTime));
        attributeMappers.add(createStandardAttributeMapper(Instant.class, JavaScalars.GraphQLInstant));
        attributeMappers.add(createStandardAttributeMapper(LocalDate.class, JavaScalars.GraphQLLocalDate));
    }

    private AttributeMapper createStandardAttributeMapper(final Class<?> assignableClass, final GraphQLType type) {
        return (javaType) -> {
            if (assignableClass.isAssignableFrom(javaType))
                return Optional.of(type);

            return Optional.empty();
        };
    }

    /**
     * @deprecated Use {@link #build()} instead.
     * @return A freshly built {@link GraphQLSchema}
     */
    @Deprecated()
    public GraphQLSchema getGraphQLSchema() {
        return super.build();
    }

    GraphQLObjectType getQueryType() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("QueryType_MD").description("All encompassing schema for this database metadata environment");
        queryType.fields(metaDataService.listAllMetaTables(databaseId).stream().map(this::getQueryFieldDefinition).collect(Collectors.toList()));
        queryType.fields(metaDataService.listAllMetaTables(databaseId).stream().map(this::getQueryFieldPageableDefinition).collect(Collectors.toList()));
        return queryType.build();
    }

    GraphQLFieldDefinition getQueryFieldDefinition(MetaTable entityType) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(FieldType.mapPropName(entityType.getTableName()))
                .description(entityType.getTableLabelName())
                .type(new GraphQLList(getObjectType(entityType)))
                .dataFetcher(new MetadataDataFetcher(metaDataService, entityType))
                .argument(entityType.getColumns().stream().flatMap(this::getArgument).collect(Collectors.toList()))
                .build();
    }



    private GraphQLFieldDefinition getQueryFieldPageableDefinition(MetaTable entityType) {
        String entityName = FieldType.mapPropName(entityType.getTableName());
        GraphQLObjectType pageType = GraphQLObjectType.newObject()
                .name(entityName + "Connection")
                .description("'Connection' response wrapper object for " + entityName + ".  When pagination or aggregation is requested, this object will be returned with metadata about the query.")
                .field(GraphQLFieldDefinition.newFieldDefinition().name("pageNo").description("Total index of current page.").type(Scalars.GraphQLLong).build())
                .field(GraphQLFieldDefinition.newFieldDefinition().name("pageSize").description("Total max number of one page.").type(Scalars.GraphQLLong).build())
                .field(GraphQLFieldDefinition.newFieldDefinition().name("totalRows").description("Total number of results on the database for this query.").type(Scalars.GraphQLLong).build())
                .field(GraphQLFieldDefinition.newFieldDefinition().name("objList").description("The actual object results").type(new GraphQLList(getObjectType(entityType))).build())
                .build();

        return GraphQLFieldDefinition.newFieldDefinition()
                .name(entityName + "Connection")
                .description("'Connection' request wrapper object for " + entityName + ".  Use this object in a query to request things like pagination or aggregation in an argument.  Use the 'content' field to request actual fields ")
                .type(pageType)
                .dataFetcher(new MetadataDataFetcher(metaDataService, entityType))
                .argument(paginationArgument)
                .build();
    }

    private Stream<GraphQLArgument> getArgument(MetaColumn attribute) {
        return getAttributeType(attribute)
                .filter(type -> type instanceof GraphQLInputType)
                .filter(type -> attribute.getPersistentAttributeType() != Attribute.PersistentAttributeType.EMBEDDED ||
                        (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED && type instanceof GraphQLScalarType))
                .map(type -> {
                    String name = attribute.getName();

                    return GraphQLArgument.newArgument()
                            .name(name)
                            .type((GraphQLInputType) type)
                            .build();
                });
    }

    GraphQLObjectType getObjectType(MetaTable entityType) {
        if (entityCache.containsKey(entityType))
            return entityCache.get(entityType);

        GraphQLObjectType answer = GraphQLObjectType.newObject()
                .name(entityType.getName())
                .description(getSchemaDocumentation(entityType.getJavaType()))
                .fields(entityType.getColumns().stream().filter(this::isNotIgnored).flatMap(this::getObjectField).collect(Collectors.toList()))
                .fields(entityType.getMdRelations().stream().filter(this::isNotIgnored).flatMap(this::getObjectField).collect(Collectors.toList()))
                .build();

        entityCache.put(entityType, answer);

        return answer;
    }


    private Stream<GraphQLFieldDefinition> getObjectField(MetaColumn attribute) {
        return getAttributeType(attribute)
                .filter(type -> type instanceof GraphQLOutputType)
                .map(type -> {
                    List<GraphQLArgument> arguments = new ArrayList<>();
                    arguments.add(GraphQLArgument.newArgument().name("orderBy").type(orderByDirectionEnum).build());            // Always add the orderBy argument

                    // Get the fields that can be queried on (i.e. Simple Types, no Sub-Objects)
                    if (attribute instanceof SingularAttribute
                            && attribute.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC) {
                        ManagedType foreignType = (ManagedType) ((SingularAttribute) attribute).getType();

                        Stream<Attribute> attributes = findBasicAttributes(foreignType.getAttributes());

                        attributes.forEach(it -> {
                            arguments.add(GraphQLArgument.newArgument()
                                    .name(it.getName())
                                    .type((GraphQLInputType) getAttributeType(it).findFirst().get())
                                    .build());
                        });
                    }

                    String name = attribute.getName();


                    return GraphQLFieldDefinition.newFieldDefinition()
                            .name(name)
                            .description(getSchemaDocumentation(attribute.getJavaMember()))
                            .type((GraphQLOutputType) type)
                            .argument(arguments)
                            .build();
                });
    }

    private Stream<Attribute> findBasicAttributes(Collection<Attribute> attributes) {
        return attributes.stream().filter(this::isNotIgnored).filter(it -> it.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC);
    }

    private GraphQLType getBasicAttributeType(Class javaType) {
        // First check our 'standard' and 'customized' Attribute Mappers.  Use them if possible
        Optional<AttributeMapper> customMapper = attributeMappers.stream()
                .filter(it -> it.getBasicAttributeType(javaType).isPresent())
                .findFirst();

        if (customMapper.isPresent())
            return customMapper.get().getBasicAttributeType(javaType).get();
        else if (String.class.isAssignableFrom(javaType))
            return Scalars.GraphQLString;
        else if (Integer.class.isAssignableFrom(javaType) || int.class.isAssignableFrom(javaType))
            return Scalars.GraphQLInt;
        else if (Short.class.isAssignableFrom(javaType) || short.class.isAssignableFrom(javaType))
            return Scalars.GraphQLShort;
        else if (Float.class.isAssignableFrom(javaType) || float.class.isAssignableFrom(javaType)
                || Double.class.isAssignableFrom(javaType) || double.class.isAssignableFrom(javaType))
            return Scalars.GraphQLFloat;
        else if (Long.class.isAssignableFrom(javaType) || long.class.isAssignableFrom(javaType))
            return Scalars.GraphQLLong;
        else if (Boolean.class.isAssignableFrom(javaType) || boolean.class.isAssignableFrom(javaType))
            return Scalars.GraphQLBoolean;
        else if (javaType.isEnum()) {
            return getTypeFromJavaType(javaType);
        } else if (BigDecimal.class.isAssignableFrom(javaType)) {
            return Scalars.GraphQLBigDecimal;
        }

        throw new UnsupportedOperationException(
                "Class could not be mapped to GraphQL: '" + javaType.getClass().getTypeName() + "'");
    }

    private Stream<GraphQLType> getAttributeType(MetaColumn attribute) {
        if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
            try {
                return Stream.of(getBasicAttributeType(attribute.getJavaType()));
            } catch (UnsupportedOperationException e) {
                //fall through to the exception below
                //which is more useful because it also contains the declaring member
            }
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_MANY || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_MANY) {
            EntityType foreignType = (EntityType) ((PluralAttribute) attribute).getElementType();
            return Stream.of(new GraphQLList(new GraphQLTypeReference(foreignType.getName())));
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE) {
            EntityType foreignType = (EntityType) ((SingularAttribute) attribute).getType();
            return Stream.of(new GraphQLTypeReference(foreignType.getName()));
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {
            Type foreignType = ((PluralAttribute) attribute).getElementType();
            return Stream.of(new GraphQLList(getTypeFromJavaType(foreignType.getJavaType())));
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
            EmbeddableType<?> embeddableType = (EmbeddableType<?>) ((SingularAttribute<?,?>) attribute).getType();
            return Stream.of(new GraphQLTypeReference(embeddableType.getJavaType().getSimpleName()));
        }

        final String declaringType = attribute.getDeclaringType().getJavaType().getName(); // fully qualified name of the entity class
        final String declaringMember = attribute.getJavaMember().getName(); // field name in the entity class

        throw new UnsupportedOperationException(
                "Attribute could not be mapped to GraphQL: field '" + declaringMember + "' of entity class '" + declaringType + "'");
    }

    private boolean isValidInput(Attribute attribute) {
        return attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC ||
                attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ELEMENT_COLLECTION ||
                attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED;
    }



    private GraphQLType getTypeFromJavaType(Class clazz) {
        if (clazz.isEnum()) {
            if (classCache.containsKey(clazz))
                return classCache.get(clazz);

            GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(clazz.getSimpleName());
            int ordinal = 0;
            for (Enum enumValue : ((Class<Enum>) clazz).getEnumConstants())
                enumBuilder.value(enumValue.name(), ordinal++);

            GraphQLType answer = enumBuilder.build();
            setIdentityCoercing(answer);

            classCache.put(clazz, answer);

            return answer;
        }

        return getBasicAttributeType(clazz);
    }

    /**
     * A bit of a hack, since JPA will deserialize our Enum's for us...we don't want GraphQL doing it.
     *
     * @param type
     */
    private void setIdentityCoercing(GraphQLType type) {
        try {
            Field coercing = type.getClass().getDeclaredField("coercing");
            coercing.setAccessible(true);
            coercing.set(type, new IdentityCoercing());
        } catch (Exception e) {
            log.error("Unable to set coercing for " + type, e);
        }
    }

    private static final GraphQLArgument paginationArgument =
            GraphQLArgument.newArgument()
                    .name(PAGINATION_REQUEST_PARAM_NAME)
                    .type(GraphQLInputObjectType.newInputObject()
                            .name("PaginationObject")
                            .description("Query object for Pagination Requests, specifying the requested page, and that page's size.\n\nNOTE: 'page' parameter is 1-indexed, NOT 0-indexed.\n\nExample: paginationRequest { page: 1, size: 20 }")
                            .field(GraphQLInputObjectField.newInputObjectField().name("pageNo").description("Which page should be returned, starting with 1 (1-indexed)").type(Scalars.GraphQLInt).build())
                            .field(GraphQLInputObjectField.newInputObjectField().name("pageSize").description("How many results should this page contain").type(Scalars.GraphQLInt).build())
                            .build()
                    ).build();

    private static final GraphQLEnumType orderByDirectionEnum =
            GraphQLEnumType.newEnum()
                    .name("OrderByDirection")
                    .description("Describes the direction (Ascending / Descending) to sort a field.")
                    .value("ASC", 0, "Ascending")
                    .value("DESC", 1, "Descending")
                    .build();


}
