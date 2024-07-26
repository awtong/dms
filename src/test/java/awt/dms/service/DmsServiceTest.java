package awt.dms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DmsServiceTest {
    private static final MockMultipartFile MULTIPART_FILE_MOCK = new MockMultipartFile("file", "originalFileName", "text/plain",
            "Some content".getBytes(UTF_8));

    @Mock
    private GridFsTemplate gridFsTemplateMock;

    @Mock
    private GridFsResource gridFsResourceMock;

    @Mock
    private StreamBridge streamBridgeMock;

    @Mock
    private GridFSFile gridFSFileMock;

    @Mock
    private ObjectId objectIdMock;

    @Mock
    private ObjectMapper objectMapperMock;

    @Mock
    private ObjectNode objectNodeMock;

    private DmsService dmsService;

    @BeforeEach
    void setUp() {
        this.dmsService = new DmsService(this.gridFsTemplateMock, this.streamBridgeMock, this.objectMapperMock);
    }

    @Test
    void given_valid_object_id_when_file_not_found_then_return_empty_optional() {
        when(this.gridFsTemplateMock.findOne(any(Query.class))).thenReturn(null);

        final Optional<GridFsResource> optional = this.dmsService.findOne(this.objectIdMock);

        assertThat(optional).isEqualTo(Optional.empty());
        verifyNoInteractions(this.streamBridgeMock);
    }

    @Test
    void given_valid_object_id_when_file_found_then_return_optional() {
        mockRabbitMessageBuilder();

        when(objectIdMock.toString()).thenReturn("123");
        when(this.gridFSFileMock.getObjectId()).thenReturn(objectIdMock);
        when(this.gridFSFileMock.getFilename()).thenReturn("filename");
        when(this.gridFsTemplateMock.findOne(any(Query.class))).thenReturn(this.gridFSFileMock);
        when(this.gridFsTemplateMock.getResource(this.gridFSFileMock)).thenReturn(this.gridFsResourceMock);

        final Optional<GridFsResource> optional = this.dmsService.findOne(any(ObjectId.class));

        assertThat(optional).isEqualTo(Optional.of(this.gridFsResourceMock));
        verify(this.streamBridgeMock).send(anyString(), any());
    }

    @Test
    void given_upload_feature_enabled_when_uploading_file_then_return_object_id() throws IOException {
        mockRabbitMessageBuilder();

        when(this.gridFsTemplateMock.store(any(InputStream.class),
                eq(MULTIPART_FILE_MOCK.getOriginalFilename()),
                eq(MULTIPART_FILE_MOCK.getContentType()),
                any(Document.class)))
                .thenReturn(this.objectIdMock);

        assertThat(this.dmsService.upload(MULTIPART_FILE_MOCK, Collections.emptyMap())).isEqualTo(this.objectIdMock);
        verify(this.streamBridgeMock).send(anyString(), any());
    }

    private void mockRabbitMessageBuilder() {
        when(this.objectMapperMock.createObjectNode()).thenReturn(this.objectNodeMock);
        when(this.objectNodeMock.put(anyString(), anyString())).thenReturn(this.objectNodeMock);
    }
}
