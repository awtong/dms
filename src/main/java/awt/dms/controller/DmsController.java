package awt.dms.controller;

import awt.dms.service.DmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@ApiResponses(value = {
    @ApiResponse(responseCode = "401", description = "Not logged in."),
    @ApiResponse(responseCode = "403", description = "Do not have permissions.")
})
@RestController
public class DmsController {

  private static final String INTERNAL_SERVER_ERROR = "500";

  private final DmsService dmsService;

  public DmsController(final DmsService dmsService) {
    this.dmsService = dmsService;
  }

  @Operation(summary = "Uploads file. Size limit is 5MB")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "File uploaded successfully.", headers = {
          @Header(name = "location", description = "URL to uploaded file.", required = true)
      }),
      @ApiResponse(responseCode = "400", description = "No file provided."),
      @ApiResponse(responseCode = INTERNAL_SERVER_ERROR, description = "Error uploading file.")
  })
  @PostMapping(value = "/v1/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> uploadFile(@NotNull @RequestPart("file") final MultipartFile file,
      @RequestPart final Optional<Map<String, String>> metadata)
      throws IOException {
    // XXX check user access
    // XXX virus scan
    final ObjectId objectId = this.dmsService.upload(file, metadata.orElse(Collections.emptyMap()));
    return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(objectId)
            .toUri())
        .build();
  }

  @Operation(summary = "Retrieves file.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "File retrieved successfully."),
      @ApiResponse(responseCode = "404", description = "File doesn't exist."),
      @ApiResponse(responseCode = INTERNAL_SERVER_ERROR, description = "Error retrieving file.")
  })
  @GetMapping(value = "/v1/documents/{objectId}")
  public void getFile(@PathVariable("objectId") final ObjectId objectId,
      final HttpServletResponse httpServletResponse)
      throws IOException {
    // XXX check user access
    final Optional<GridFsResource> optional = this.dmsService.findOne(objectId);
    if (optional.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    final GridFsResource resource = optional.get();
    httpServletResponse.setContentType(resource.getContentType());
    httpServletResponse.setContentLengthLong(resource.contentLength());
    httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
        .filename(resource.getFilename())
        .build()
        .toString()
    );

    resource.getInputStream().transferTo(httpServletResponse.getOutputStream());
  }

  @Operation(summary = "Retrieves files.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Files retrieved successfully."),
      @ApiResponse(responseCode = INTERNAL_SERVER_ERROR, description = "Error retrieving file.")
  })
  @GetMapping(value = "/v1/documents")
  public ResponseEntity<Collection<FileItem>> getFiles(@RequestParam("user") final String user) {
    // XXX check user access
    final Collection<GridFsResource> resources = this.dmsService.findAll(user);
    final Collection<FileItem> files = resources.stream().map(
        resource -> new FileItem(Objects.requireNonNull(resource.getFileId()).toString(),
            resource.getContentType(), resource.getFilename())
    ).collect(Collectors.toCollection(HashSet::new));

    return ResponseEntity.ok(files);
  }

  @Operation(summary = "Deletes file.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "File deleted successfully."),
      @ApiResponse(responseCode = INTERNAL_SERVER_ERROR, description = "Error deleting file.")
  })
  @DeleteMapping(value = "/v1/documents/{objectId}")
  public ResponseEntity<Void> deleteFile(@PathVariable("objectId") final ObjectId objectId) {
    // XXX check user access
    this.dmsService.delete(objectId);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<String> handleIOException() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  public record FileItem(String fileId, String contentType, String fileName) {

  }
}
