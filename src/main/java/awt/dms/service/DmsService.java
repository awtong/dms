package awt.dms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.validation.constraints.NotNull;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;

@Validated
@Service
public class DmsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmsService.class);

    private static final String UPLOADED_OUTPUT_BINDING_NAME = "file-uploaded-out-0";
    private static final String VIEWED_OUTPUT_BINDING_NAME = "file-viewed-out-0";
    private static final String DELETED_OUTPUT_BINDING_NAME = "file-deleted-out-0";

    private static final String FALLBACK_OBJECT_TYPE = "application/octet-stream";

    private static final String ID_KEY = "_id";

    private final GridFsOperations gridFsOperations;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public DmsService(final GridFsTemplate gridFsTemplate, final StreamBridge streamBridge, final ObjectMapper objectMapper) {
        this.gridFsOperations = gridFsTemplate;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    public ObjectId upload(@NotNull final MultipartFile file, @NotNull final Map<String, String> metadata) throws IOException {
        LOGGER.info("Trying to upload {}", file.getOriginalFilename());
        final ObjectId objectId = this.gridFsOperations.store(file.getInputStream(),
                file.getOriginalFilename(), file.getContentType(), new Document(metadata));

        final ObjectNode message = this.createRabbitMessage(objectId, file.getOriginalFilename(), "userId", file.getContentType());
        this.streamBridge.send(UPLOADED_OUTPUT_BINDING_NAME, message);
        return objectId;
    }

    public Collection<GridFsResource> findAll(@NotNull String userId) {
        LOGGER.info("Trying to find {}", userId);
        final Collection<GridFsResource> resources = new HashSet<>();
        final GridFSFindIterable iterable = this.gridFsOperations.find(Query.query(whereMetaData("user").is(userId)));
        final Collection<ObjectNode> nodes = new HashSet<>();
        iterable.forEach(file -> {
            final GridFsResource resource = this.gridFsOperations.getResource(file);
            resources.add(resource);
            nodes.add(this.createRabbitMessage(file, userId));
        });

        this.streamBridge.send(VIEWED_OUTPUT_BINDING_NAME, nodes);
        return resources;
    }

    public Optional<GridFsResource> findOne(@NotNull final ObjectId objectId) {
        LOGGER.info("Trying to find {}", objectId);
        final GridFSFile file = this.gridFsOperations.findOne(Query.query(Criteria.where(ID_KEY).is(objectId)));
        if (file == null) {
            return Optional.empty();
        }

        final Optional<GridFsResource> resource = Optional.of(this.gridFsOperations.getResource(file));
        final ObjectNode message = createRabbitMessage(file, "userId");

        this.streamBridge.send(VIEWED_OUTPUT_BINDING_NAME, message);
        return resource;
    }

    public void delete(@NotNull final ObjectId objectId) {
        LOGGER.info("Trying to delete {}", objectId);
        this.gridFsOperations.delete(new Query(Criteria.where(ID_KEY).is(objectId)));

        final ObjectNode node = this.objectMapper.createObjectNode()
                .put("objectId", objectId.toString());
        this.streamBridge.send(DELETED_OUTPUT_BINDING_NAME, node);
    }

    private ObjectNode createRabbitMessage(final GridFSFile file, final String userId) {
        return this.createRabbitMessage(file.getObjectId(), file.getFilename(), userId,
                file.getMetadata() != null ? file.getMetadata().getString("_contentType") : FALLBACK_OBJECT_TYPE);
    }

    private ObjectNode createRabbitMessage(final ObjectId objectId, final String filename, final String userId, final String objectType) {
        return this.objectMapper.createObjectNode()
                .put("objectId", Objects.requireNonNull(objectId.toString()))
                .put("displayName", Objects.requireNonNull(filename))
                .put("userId", Objects.requireNonNull(userId))
                .put("objectType", Objects.requireNonNull(objectType));
    }
}
