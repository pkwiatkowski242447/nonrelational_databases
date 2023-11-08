package model.repositories;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import mapping_layer.model_docs.ClientDoc;
import mapping_layer.model_docs.MovieDoc;
import mapping_layer.model_docs.ScreeningRoomDoc;
import mapping_layer.model_docs.TicketDoc;
import mapping_layer.model_docs.ticket_types.NormalDoc;
import mapping_layer.model_docs.ticket_types.ReducedDoc;
import mapping_layer.model_docs.ticket_types.TypeOfTicketDoc;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.io.Closeable;
import java.util.List;
import java.util.UUID;

public abstract class MongoRepository<Type> implements Closeable {

    // Database connection required information.
    private final ConnectionString connectionString = new ConnectionString("mongodb://mongonode1:27020,mongonode2:27021,mongonode3:27022");
    private final MongoCredential mongoCredential = MongoCredential.createCredential("admin", "admin", "adminpassword".toCharArray());

    // Collection names

    protected final String clientCollectionName = "clients";
    protected final String screeningRoomCollectionName = "screeningRooms";
    protected final String movieCollectionName = "movies";
    protected final String ticketCollectionName = "tickets";
    protected final String typeOfTicketCollectionName = "typesOfTickets";

    // Collection types

    protected final Class<ClientDoc> clientCollectionType = ClientDoc.class;
    protected final Class<ScreeningRoomDoc> screeningRoomCollectionType = ScreeningRoomDoc.class;
    protected final Class<MovieDoc> movieCollectionType = MovieDoc.class;
    protected final Class<TicketDoc> ticketCollectionType = TicketDoc.class;
    protected final Class<TypeOfTicketDoc> typeOfTicketCollectionType = TypeOfTicketDoc.class;

    // Mapping Java classes to Bson documents.
    private final ClassModel<ClientDoc> clientDocClassModel = ClassModel.builder(ClientDoc.class).build();
    private final ClassModel<MovieDoc> movieDocClassModel = ClassModel.builder(MovieDoc.class).build();
    private final ClassModel<ScreeningRoomDoc> screeningRoomDocClassModel = ClassModel.builder(ScreeningRoomDoc.class).build();
    private final ClassModel<TicketDoc> ticketDocClassModel = ClassModel.builder(TicketDoc.class).build();
    private final ClassModel<TypeOfTicketDoc> typeOfTicketDocClassModel = ClassModel.builder(TypeOfTicketDoc.class).enableDiscriminator(true).build();
    private final ClassModel<NormalDoc> normalDocClassModel = ClassModel.builder(NormalDoc.class).enableDiscriminator(true).build();
    private final ClassModel<ReducedDoc> reducedDocClassModel = ClassModel.builder(ReducedDoc.class).enableDiscriminator(true).build();

    private final PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().register(
            clientDocClassModel, movieDocClassModel, screeningRoomDocClassModel, ticketDocClassModel, typeOfTicketDocClassModel, normalDocClassModel, reducedDocClassModel
    ).build();
    private final CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(
            pojoCodecProvider, PojoCodecProvider.builder().automatic(true).conventions(List.of(Conventions.ANNOTATION_CONVENTION)).build());

    // MongoClient & MongoDatabase variables
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;


    protected void initDatabaseConnection(String databaseName) {
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .credential(mongoCredential)
                .readConcern(ReadConcern.MAJORITY)
                .readPreference(ReadPreference.primary())
                .writeConcern(WriteConcern.MAJORITY)
                .applyConnectionString(connectionString)
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .codecRegistry(CodecRegistries.fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        pojoCodecRegistry
                )).build();

        mongoClient = MongoClients.create(mongoClientSettings);
        mongoDatabase = mongoClient.getDatabase(databaseName);
    }

    // Defining methods that later will be overwritten in other repositories.

    // R - Methods for finding object representation in the DB

    public abstract Type findByUUID(UUID elementUUID);
    public abstract List<Type> findAll();
    public abstract List<Type> findAllActive();
    public abstract List<UUID> findAllUUIDs();

    // U - Methods for updating object representation in the DB

    public abstract void updateAllFields(Type element);

    // D - Methods for deleting object representation from DB

    public abstract void delete(Type element);
    public abstract void delete(UUID elementID);
    public abstract void expire(Type element);

    @Override
    public void close() {
        mongoClient.close();
    }
}