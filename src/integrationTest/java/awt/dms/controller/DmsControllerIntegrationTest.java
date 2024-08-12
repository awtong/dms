package awt.dms.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import awt.dms.service.DmsService;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DmsController.class)
class DmsControllerIntegrationTest {

  private static final String ROOT_PATH = "/v1/documents";
  private static final ObjectId OBJECT_ID = new ObjectId(Date.from(Instant.now()));

  // filename must match @RequestParam name
  private static final MockMultipartFile MOCK_MULTIPART_FILE = new MockMultipartFile("file",
      "Some content".getBytes(UTF_8));

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DmsService dmsService;

  @Nested
  @DisplayName("Finding a document")
  class Finding {

    private static final String FIND_DOCUMENT_PATH = ROOT_PATH + "/{objectId}";

    @Test
    void when_not_logged_in_return_401() throws Exception {
      mockMvc.perform(get(FIND_DOCUMENT_PATH, OBJECT_ID))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void when_object_id_not_found_return_404() throws Exception {
      when(dmsService.findOne(OBJECT_ID)).thenReturn(Optional.empty());

      mockMvc.perform(get(FIND_DOCUMENT_PATH, OBJECT_ID)
              .with(oauth2Login()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Deleting a document")
  class Deleting {

    private static final String DELETE_DOCUMENT_PATH = ROOT_PATH + "/{objectId}";

    @Test
    void when_delete_successful_return_204() throws Exception {
      doNothing().when(dmsService).delete(OBJECT_ID);
      mockMvc.perform(delete(DELETE_DOCUMENT_PATH, OBJECT_ID)
              .with(csrf())
              .with(oauth2Login()))
          .andExpect(status().isNoContent());
    }

    @Test
    void when_not_logged_in_return_401() throws Exception {
      mockMvc.perform(delete(DELETE_DOCUMENT_PATH, OBJECT_ID)
              .with(csrf()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("Uploading a document")
  class Uploading {

    private static final String UPLODA_PATH = ROOT_PATH;

    @Test
    void when_upload_successful_occurs_return_201() throws Exception {
      when(dmsService.upload(MOCK_MULTIPART_FILE, Collections.emptyMap())).thenReturn(OBJECT_ID);

      mockMvc.perform(multipart(UPLODA_PATH)
              .file(MOCK_MULTIPART_FILE)
              .with(csrf())
              .with(oauth2Login()))
          .andExpect(header().string("Location", endsWith(OBJECT_ID.toString())))
          .andExpect(status().isCreated());
    }

    @Test
    void when_no_file_sent_return_400() throws Exception {
      mockMvc.perform(multipart(UPLODA_PATH)
              .with(csrf())
              .with(oauth2Login()))
          .andExpect(status().isBadRequest());
    }

    @Test
    void when_not_logged_in_return_401() throws Exception {
      mockMvc.perform(multipart(UPLODA_PATH)
              .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void when_io_exception_occurs_return_500() throws Exception {
      when(dmsService.upload(MOCK_MULTIPART_FILE, Collections.emptyMap())).thenThrow(
          IOException.class);

      mockMvc.perform(multipart(UPLODA_PATH)
              .file(MOCK_MULTIPART_FILE)
              .with(csrf())
              .with(oauth2Login()))
          .andExpect(status().isInternalServerError());
    }
  }
}
