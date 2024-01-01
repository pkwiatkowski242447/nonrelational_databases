package model.repositories.implementations;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ValidationOptions;
import mapping_layer.converters.ClientConverter;
import mapping_layer.model_docs.ClientRow;
import model.Client;
import model.exceptions.repository_exceptions.*;
import model.repositories.interfaces.ClientRepositoryInterface;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientRepository extends MongoRepository implements ClientRepositoryInterface {

    public ClientRepository(String databaseName) throws MongoConfigNotFoundException {
        super.initDatabaseConnection(databaseName);

        // Checking if collection "clients" exists.
        boolean collectionExists = false;
        for (String collectionName : mongoDatabase.listCollectionNames()) {
            if (collectionName.equals(this.clientCollectionName)) {
                collectionExists = true;
                break;
            }
        }

        // If it does not exist - then create it with a specific JSON Schema.
        if (!collectionExists) {
            ValidationOptions validationOptions = new ValidationOptions().validator(
                    Document.parse("""
                            {
                                $jsonSchema: {
                                    "bsonType": "object",
                                    "required": ["_id", "client_name", "client_surname", "client_age", "client_status_active"],
                                    "properties": {
                                        "_id": {
                                            "description": "UUID identifier of a certain clients document.",
                                            "bsonType": "binData",
                                            "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
                                        }
                                        "client_name": {
                                            "description": "String with clients name.",
                                            "bsonType": "string",
                                            "minLength": 1,
                                            "maxLength": 50
                                        }
                                        "client_surname": {
                                            "description": "String with clients surname.",
                                            "bsonType": "string",
                                            "minLength": 2,
                                            "maxLength": 100
                                        }
                                        "client_age": {
                                            "description": "Integer value representing clients age.",
                                            "bsonType": "int",
                                            "minimum": 18,
                                            "maximum": 120
                                        }
                                        "client_status_active": {
                                            "description": "Boolean flag indicating whether client is active (their account does exist [that is not deleted by the user]) or not.",
                                            "bsonType": "bool"
                                        }
                                    }
                                }
                            }
                            """));
            CreateCollectionOptions createCollectionOptions = new CreateCollectionOptions().validationOptions(validationOptions);
            mongoDatabase.createCollection(this.clientCollectionName, createCollectionOptions);
        }
    }

    // C - Methods for creating object representation in the DB

    @Override
    public Client create(String clientName, String clientSurname, int clientAge) {
        Client client;
        try {
            client = new Client(UUID.randomUUID(), clientName, clientSurname, clientAge);
            ClientRow clientRow = ClientConverter.toClientRow(client);
            getClientCollection().insertOne(clientRow);
        } catch (MongoException exception) {
            throw new ClientRepositoryCreateException(exception.getMessage(), exception);
        }
        return client;
    }

    @Override
    public void updateAllFields(Client client) {
        try {
            ClientRow clientRow = ClientConverter.toClientRow(client);
            Bson filter = Filters.eq("_id", clientRow.getClientID());
            ClientRow updatedClientRow = getClientCollection().findOneAndReplace(filter, clientRow);
            if (updatedClientRow == null) {
                throw new ClientDocNotFoundException("Document for given client object could not be updated, since it is not in the database.");
            }
        } catch (MongoException exception) {
            throw new ClientRepositoryUpdateException(exception.getMessage(), exception);
        }
    }

    @Override
    public void delete(Client client) {
        delete(client.getClientID());
    }

    @Override
    public void delete(UUID clientID) {
        try {
            Bson filter = Filters.eq("_id", clientID);
            ClientRow removedClientRow = getClientCollection().findOneAndDelete(filter);
            if (removedClientRow == null) {
                throw new ClientDocNotFoundException("Document for given client object could not be deleted, since it is not in the database.");
            }
        } catch (MongoException exception) {
            throw new ClientRepositoryDeleteException(exception.getMessage(), exception);
        }
    }

    @Override
    public void expire(Client client) {
        client.setClientStatusActive(false);
        updateAllFields(client);
    }

    @Override
    public Client findByUUID(UUID identifier) {
        Client client;
        try {
            ClientRow foundClientRow = findClientDoc(identifier);
            if (foundClientRow != null) {
                client = ClientConverter.toClient(foundClientRow);
            } else {
                throw new ClientDocNotFoundException("Document for client object with given UUID could not be read, since it is not in the database.");
            }
        } catch (MongoException | NullPointerException exception) {
            throw new ClientRepositoryReadException(exception.getMessage(), exception);
        }
        return client;
    }

    @Override
    public List<Client> findAll() {
        List<Client> listOfAllClients;
        try(ClientSession clientSession = mongoClient.startSession()) {
            clientSession.startTransaction();
            Bson clientFilter = Filters.empty();
            listOfAllClients = findClients(clientFilter);
            clientSession.commitTransaction();
        } catch (MongoException exception) {
            throw new ClientRepositoryReadException(exception.getMessage(), exception);
        }
        return listOfAllClients;
    }

    @Override
    public List<Client> findAllActive() {
        List<Client> listOfAllActiveClients;
        try(ClientSession clientSession = mongoClient.startSession()) {
            clientSession.startTransaction();
            Bson clientFilter = Filters.eq("client_status_active", true);
            listOfAllActiveClients = findClients(clientFilter);
            clientSession.commitTransaction();
        } catch (MongoException exception) {
            throw new ClientRepositoryReadException(exception.getMessage(), exception);
        }
        return listOfAllActiveClients;
    }

    @Override
    public List<UUID> findAllUUIDs() {
        List<UUID> listOfClientsUUIDs = new ArrayList<>();
        try(ClientSession clientSession = mongoClient.startSession()) {
            clientSession.startTransaction();
            Bson filter = Filters.empty();
            for (ClientRow clientRow : getClientCollection().find(filter)) {
                listOfClientsUUIDs.add(clientRow.getClientID());
            }
            clientSession.commitTransaction();
        } catch (MongoException exception) {
            throw new ClientRepositoryReadException(exception.getMessage(), exception);
        }
        return listOfClientsUUIDs;
    }

    private List<Client> findClients(Bson clientFilter) {
        List<Client> listOfFoundClients = new ArrayList<>();
        for (ClientRow clientRow : getClientCollection().find(clientFilter)) {
            listOfFoundClients.add(ClientConverter.toClient(clientRow));
        }
        return listOfFoundClients;
    }
}