package awt.dms.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import awt.dms.config.MongoDbContainerConfig;
import awt.dms.config.RabbitContainerConfig;
import jakarta.validation.ConstraintViolationException;
import java.sql.Date;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@Import({MongoDbContainerConfig.class, RabbitContainerConfig.class})
class DmsServiceIntegrationTest {

  private static final MockMultipartFile MULTIPART_FILE = new MockMultipartFile("file",
      "Some content".getBytes(UTF_8));

  @Autowired
  private DmsService dmsService;

  @Nested
  @DisplayName("Uploading against actual MongoDB")
  class Upload {

    @Test
    void when_null_multipart_file_then_throw_constraint_violation_exception() {
      assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
          () -> dmsService.upload(null, Collections.emptyMap()));
    }

    @Test
    void when_null_metadata_then_throw_constraint_violation_exception() {
      assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
          () -> dmsService.upload(MULTIPART_FILE, null));
    }
  }

  @Nested
  @DisplayName("Finding against actual MongoDB")
  class Find {

    @Test
    void when_null_objectid_then_throw_constraint_violation_exception() {
      assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
          () -> dmsService.findOne(null));
    }

    @Test
    void when_not_found_then_return_empty_optional() {
      final Optional<GridFsResource> optional = dmsService.findOne(
          new ObjectId(Date.from(Instant.now())));
      assertThat(optional.isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("Testing delete against actual MongoDB")
  class Delete {

    @Test
    void when_null_objectid_then_throw_constraint_violation_exception() {
      assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
          () -> dmsService.delete(null));
    }
  }
}
