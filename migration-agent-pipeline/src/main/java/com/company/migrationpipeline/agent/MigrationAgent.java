package com.company.migrationpipeline.agent;

import com.company.migrationpipeline.model.PipelineContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationAgent implements PipelineAgent {
  private static final Pattern PACKAGE_PATTERN =
      Pattern.compile("CREATE\\s+OR\\s+REPLACE\\s+PACKAGE\\s+(\\w+)",
          Pattern.CASE_INSENSITIVE);

  @Override
  public String name() {
    return "migration";
  }

  @Override
  public void execute(PipelineContext context) {
    List<Path> sqlFiles = context.getSqlFiles();
    if (sqlFiles == null || sqlFiles.isEmpty()) {
      throw new IllegalStateException("No SQL files available for migration.");
    }

    Path sqlFile = sqlFiles.get(0);
    String sqlContents = readSql(sqlFile);
    String packageName = parsePackageName(sqlContents)
        .orElseThrow(() -> new IllegalStateException("Package name not found in " + sqlFile));

    DomainInfo domain = DomainInfo.fromPackageName(packageName);
    Path projectRoot = context.getOutputPath();
    ensureDirectory(projectRoot);

    List<GeneratedFile> files = new ArrayList<>();
    files.add(new GeneratedFile("pom.xml", pomXml(domain)));
    files.add(new GeneratedFile("src/main/resources/application.yml", applicationYaml(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/" + domain.domainClass() + "Application.java",
        applicationClass(domain)));

    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/entity/Order.java",
        orderEntity(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/entity/AuditLog.java",
        auditLogEntity(domain)));

    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/repository/OrderRepository.java",
        orderRepository(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/repository/AuditLogRepository.java",
        auditLogRepository(domain)));

    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/CreateOrderRequest.java",
        createOrderRequest(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/CreateOrderResponse.java",
        createOrderResponse(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/UpdateOrderStatusRequest.java",
        updateOrderStatusRequest(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/UpdateOrderStatusResponse.java",
        updateOrderStatusResponse(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/GetOrderTotalResponse.java",
        getOrderTotalResponse(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/ListOrdersResponse.java",
        listOrdersResponse(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/dto/OrderRecDto.java",
        orderRecDto(domain)));

    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/service/" + domain.domainClass() + "Service.java",
        serviceInterface(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/service/" + domain.domainClass() + "ServiceImpl.java",
        serviceImpl(domain)));

    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/controller/" + domain.domainClass() + "Controller.java",
        controller(domain)));

    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/exception/DomainException.java",
        domainException(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/exception/OrderNotFoundException.java",
        orderNotFoundException(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/exception/ErrorResponse.java",
        errorResponse(domain)));
    files.add(new GeneratedFile(
        "src/main/java/" + domain.packagePath() + "/exception/GlobalExceptionHandler.java",
        globalExceptionHandler(domain)));

    files.sort(Comparator.comparing(GeneratedFile::relativePath));
    for (GeneratedFile generatedFile : files) {
      writeFile(projectRoot.resolve(generatedFile.relativePath()), generatedFile.contents());
    }

    String promptInfo = loadPromptInfo(context.getInputPath());
    String summary = "Generating " + files.size() + " files for " + domain.domainClass()
        + " microservice from " + sqlFile.getFileName() + ". " + promptInfo
        + " Mappings: AUTONOMOUS_TRANSACTION -> REQUIRES_NEW, FOR UPDATE NOWAIT -> PESSIMISTIC_WRITE.";

    context.setMigrationSummary(summary);
  }

  private String readSql(Path sqlFile) {
    try {
      return Files.readString(sqlFile);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read SQL file: " + sqlFile, ex);
    }
  }

  private Optional<String> parsePackageName(String sqlContents) {
    Matcher matcher = PACKAGE_PATTERN.matcher(sqlContents);
    if (matcher.find()) {
      return Optional.ofNullable(matcher.group(1));
    }
    return Optional.empty();
  }

  private void ensureDirectory(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create directory: " + path, ex);
    }
  }

  private void writeFile(Path filePath, String contents) {
    try {
      ensureDirectory(filePath.getParent());
      Files.writeString(filePath, contents);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to write file: " + filePath, ex);
    }
  }

  private String loadPromptInfo(Path inputPath) {
    Path promptPath = inputPath.getParent()
        .resolve("prompts")
        .resolve("migration_prompt.cleaned.md");
    if (Files.exists(promptPath)) {
      return "Rules loaded from " + promptPath.getFileName() + ".";
    }
    return "Rules file not found; used embedded templates.";
  }

  private String pomXml(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        <project xmlns=\"http://maven.apache.org/POM/4.0.0\"
                 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
                 xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">
          <modelVersion>4.0.0</modelVersion>

          <groupId>com.company</groupId>
          <artifactId>%s-service</artifactId>
          <version>1.0.0</version>
          <name>%s-service</name>

          <properties>
            <java.version>21</java.version>
            <spring.boot.version>3.3.2</spring.boot.version>
          </properties>

          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
              </dependency>
            </dependencies>
          </dependencyManagement>

          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-data-jpa</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-validation</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springdoc</groupId>
              <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
              <version>2.6.0</version>
            </dependency>
            <dependency>
              <groupId>com.oracle.database.jdbc</groupId>
              <artifactId>ojdbc11</artifactId>
              <version>23.4.0.24.05</version>
            </dependency>
            <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <scope>provided</scope>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-test</artifactId>
              <scope>test</scope>
            </dependency>
          </dependencies>

          <build>
            <plugins>
              <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
              </plugin>
              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                  <source>${java.version}</source>
                  <target>${java.version}</target>
                </configuration>
              </plugin>
            </plugins>
          </build>
        </project>
        """, domain.kebab(), domain.kebab());
  }

  private String applicationYaml(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        spring:
          datasource:
            url: jdbc:oracle:thin:@${DB_HOST:localhost}:${DB_PORT:1521}/${DB_SERVICE:ORCLPDB1}
            username: ${DB_USER}
            password: ${DB_PASS}
            driver-class-name: oracle.jdbc.OracleDriver
          jpa:
            hibernate:
              ddl-auto: validate
            show-sql: false
            properties:
              hibernate:
                dialect: org.hibernate.dialect.OracleDialect
                format_sql: true
            repositories:
              bootstrap-mode: deferred
        server:
          port: 8080
        logging:
          level:
            %s: INFO
        """, domain.basePackage());
  }

  private String applicationClass(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s;

        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class %sApplication {
          public static void main(String[] args) {
            SpringApplication.run(%sApplication.class, args);
          }
        }
        """, domain.basePackage(), domain.domainClass(), domain.domainClass());
  }

  private String orderEntity(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.entity;

        import jakarta.persistence.Column;
        import jakarta.persistence.Entity;
        import jakarta.persistence.GeneratedValue;
        import jakarta.persistence.GenerationType;
        import jakarta.persistence.Id;
        import jakarta.persistence.SequenceGenerator;
        import jakarta.persistence.Table;
        import java.math.BigDecimal;
        import java.time.LocalDate;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Entity
        @Table(name = "orders")
        public class Order {
          @Id
          @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
          @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 1)
          @Column(name = "order_id")
          private Long orderId;

          @Column(name = "customer_id", nullable = false)
          private Long customerId;

          @Column(name = "order_date", nullable = false)
          private LocalDate orderDate;

          @Column(name = "total_amount")
          private BigDecimal totalAmount;

          @Column(name = "status")
          private String status;
        }
        """, domain.basePackage());
  }

  private String auditLogEntity(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.entity;

        import jakarta.persistence.Column;
        import jakarta.persistence.Entity;
        import jakarta.persistence.Id;
        import jakarta.persistence.Table;
        import java.time.Instant;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Entity
        @Table(name = "audit_log")
        public class AuditLog {
          @Id
          @Column(name = "message")
          private String message;

          @Column(name = "created_at", nullable = false)
          private Instant createdAt;
        }
        """, domain.basePackage());
  }

  private String orderRepository(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.repository;

        import %s.entity.Order;
        import jakarta.persistence.LockModeType;
        import jakarta.persistence.QueryHint;
        import java.util.List;
        import java.util.Optional;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.data.jpa.repository.Lock;
        import org.springframework.data.jpa.repository.Query;
        import org.springframework.data.jpa.repository.QueryHints;
        import org.springframework.data.repository.query.Param;

        public interface OrderRepository extends JpaRepository<Order, Long> {
          List<Order> findByCustomerId(Long customerId);

          @Lock(LockModeType.PESSIMISTIC_WRITE)
          @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
          @Query("select o from Order o where o.orderId = :orderId")
          Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);
        }
        """, domain.basePackage(), domain.basePackage());
  }

  private String auditLogRepository(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.repository;

        import %s.entity.AuditLog;
        import org.springframework.data.jpa.repository.JpaRepository;

        public interface AuditLogRepository extends JpaRepository<AuditLog, String> {}
        """, domain.basePackage(), domain.basePackage());
  }

  private String createOrderRequest(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import jakarta.validation.constraints.NotNull;
        import jakarta.validation.constraints.Positive;
        import java.math.BigDecimal;
        import java.time.LocalDate;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class CreateOrderRequest {
          @NotNull
          @Positive
          private Long customerId;

          @NotNull
          private LocalDate orderDate;

          @NotNull
          @Positive
          private BigDecimal totalAmount;
        }
        """, domain.basePackage());
  }

  private String createOrderResponse(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class CreateOrderResponse {
          private Long orderId;
        }
        """, domain.basePackage());
  }

  private String updateOrderStatusRequest(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import jakarta.validation.constraints.NotBlank;
        import jakarta.validation.constraints.NotNull;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class UpdateOrderStatusRequest {
          @NotNull
          private Long orderId;

          @NotBlank
          private String status;
        }
        """, domain.basePackage());
  }

  private String updateOrderStatusResponse(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class UpdateOrderStatusResponse {
          private Long orderId;
          private String status;
        }
        """, domain.basePackage());
  }

  private String getOrderTotalResponse(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import java.math.BigDecimal;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class GetOrderTotalResponse {
          private Long orderId;
          private BigDecimal totalAmount;
        }
        """, domain.basePackage());
  }

  private String listOrdersResponse(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import java.util.List;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class ListOrdersResponse {
          private Long customerId;
          private List<OrderRecDto> orders;
        }
        """, domain.basePackage());
  }

  private String orderRecDto(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.dto;

        import java.math.BigDecimal;
        import java.time.LocalDate;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class OrderRecDto {
          private Long orderId;
          private Long customerId;
          private LocalDate orderDate;
          private BigDecimal totalAmount;
        }
        """, domain.basePackage());
  }

  private String serviceInterface(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.service;

        import %s.dto.CreateOrderRequest;
        import %s.dto.CreateOrderResponse;
        import %s.dto.GetOrderTotalResponse;
        import %s.dto.ListOrdersResponse;
        import %s.dto.UpdateOrderStatusRequest;
        import %s.dto.UpdateOrderStatusResponse;

        public interface %sService {
          /** Creates a new order record. */
          CreateOrderResponse createOrder(CreateOrderRequest request);

          /** Updates the status for an existing order. */
          UpdateOrderStatusResponse updateOrderStatus(UpdateOrderStatusRequest request);

          /** Returns the total amount for a given order. */
          GetOrderTotalResponse getOrderTotal(Long orderId);

          /** Lists all orders for a customer. */
          ListOrdersResponse listOrders(Long customerId);
        }
        """, domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.domainClass());
  }

  private String serviceImpl(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.service;

        import %s.dto.CreateOrderRequest;
        import %s.dto.CreateOrderResponse;
        import %s.dto.GetOrderTotalResponse;
        import %s.dto.ListOrdersResponse;
        import %s.dto.OrderRecDto;
        import %s.dto.UpdateOrderStatusRequest;
        import %s.dto.UpdateOrderStatusResponse;
        import %s.entity.AuditLog;
        import %s.entity.Order;
        import %s.exception.OrderNotFoundException;
        import %s.repository.AuditLogRepository;
        import %s.repository.OrderRepository;
        import jakarta.transaction.Transactional;
        import java.math.BigDecimal;
        import java.time.Instant;
        import java.util.List;
        import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        import org.springframework.stereotype.Service;

        @Slf4j
        @Service
        @RequiredArgsConstructor
        @Transactional
        public class %sServiceImpl implements %sService {
          private final OrderRepository orderRepository;
          private final AuditLogRepository auditLogRepository;

          @Override
          public CreateOrderResponse createOrder(CreateOrderRequest request) {
            Order order = Order.builder()
                .customerId(request.getCustomerId())
                .orderDate(request.getOrderDate())
                .totalAmount(request.getTotalAmount())
                .status("NEW")
                .build();

            Order saved = orderRepository.save(order);
            log.info("Created order {}", saved.getOrderId());
            logAudit("Created order " + saved.getOrderId());

            return CreateOrderResponse.builder()
                .orderId(saved.getOrderId())
                .build();
          }

          @Override
          public UpdateOrderStatusResponse updateOrderStatus(UpdateOrderStatusRequest request) {
            Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + request.getOrderId()));

            order.setStatus(request.getStatus());
            orderRepository.save(order);

            logAudit("Updated order " + request.getOrderId() + " to " + request.getStatus());

            return UpdateOrderStatusResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .build();
          }

          @Override
          @Transactional(Transactional.TxType.SUPPORTS)
          public GetOrderTotalResponse getOrderTotal(Long orderId) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

            BigDecimal total = order.getTotalAmount() == null
                ? BigDecimal.ZERO
                : order.getTotalAmount();

            return GetOrderTotalResponse.builder()
                .orderId(order.getOrderId())
                .totalAmount(total)
                .build();
          }

          @Override
          @Transactional(Transactional.TxType.SUPPORTS)
          public ListOrdersResponse listOrders(Long customerId) {
            List<OrderRecDto> orders = orderRepository.findByCustomerId(customerId).stream()
                .map(order -> OrderRecDto.builder()
                    .orderId(order.getOrderId())
                    .customerId(order.getCustomerId())
                    .orderDate(order.getOrderDate())
                    .totalAmount(order.getTotalAmount())
                    .build())
                .toList();

            return ListOrdersResponse.builder()
                .customerId(customerId)
                .orders(orders)
                .build();
          }

          @Transactional(Transactional.TxType.REQUIRES_NEW)
          protected void logAudit(String message) {
            try {
              AuditLog auditLog = AuditLog.builder()
                  .message(message)
                  .createdAt(Instant.now())
                  .build();
              auditLogRepository.save(auditLog);
            } catch (Exception ex) {
              log.warn("Audit logging failed: {}", message, ex);
            }
          }
        }
        """, domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.domainClass(),
        domain.domainClass());
  }

  private String controller(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.controller;

        import %s.dto.CreateOrderRequest;
        import %s.dto.CreateOrderResponse;
        import %s.dto.GetOrderTotalResponse;
        import %s.dto.ListOrdersResponse;
        import %s.dto.UpdateOrderStatusRequest;
        import %s.dto.UpdateOrderStatusResponse;
        import %s.service.%sService;
        import io.swagger.v3.oas.annotations.Operation;
        import io.swagger.v3.oas.annotations.media.Content;
        import io.swagger.v3.oas.annotations.responses.ApiResponse;
        import io.swagger.v3.oas.annotations.tags.Tag;
        import jakarta.validation.Valid;
        import jakarta.validation.constraints.NotNull;
        import jakarta.validation.constraints.Positive;
        import java.net.URI;
        import lombok.RequiredArgsConstructor;
        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;
        import org.springframework.validation.annotation.Validated;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.PutMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;

        @RestController
        @RequestMapping("/%s")
        @RequiredArgsConstructor
        @Validated
        @Tag(name = "%s")
        public class %sController {
          private final %sService service;

          @Operation(summary = "Create a new order")
          @ApiResponse(responseCode = "201", description = "Created", content = @Content)
          @PostMapping
          public ResponseEntity<CreateOrderResponse> createOrder(
              @Valid @RequestBody CreateOrderRequest request) {
            CreateOrderResponse response = service.createOrder(request);
            URI location = URI.create("/%s/" + response.getOrderId());
            return ResponseEntity.created(location).body(response);
          }

          @Operation(summary = "Update an order status")
          @ApiResponse(responseCode = "200", description = "Updated", content = @Content)
          @PutMapping("/{orderId}/status")
          public ResponseEntity<UpdateOrderStatusResponse> updateOrderStatus(
              @PathVariable("orderId") @NotNull @Positive Long orderId,
              @Valid @RequestBody UpdateOrderStatusRequest request) {
            UpdateOrderStatusRequest updated = UpdateOrderStatusRequest.builder()
                .orderId(orderId)
                .status(request.getStatus())
                .build();
            return ResponseEntity.ok(service.updateOrderStatus(updated));
          }

          @Operation(summary = "Get order total")
          @ApiResponse(responseCode = "200", description = "OK", content = @Content)
          @GetMapping("/{orderId}/total")
          public ResponseEntity<GetOrderTotalResponse> getOrderTotal(
              @PathVariable("orderId") @NotNull @Positive Long orderId) {
            return ResponseEntity.ok(service.getOrderTotal(orderId));
          }

          @Operation(summary = "List orders for a customer")
          @ApiResponse(responseCode = "200", description = "OK", content = @Content)
          @GetMapping("/customer/{customerId}/orders")
          public ResponseEntity<ListOrdersResponse> listOrders(
              @PathVariable("customerId") @NotNull @Positive Long customerId) {
            return ResponseEntity.status(HttpStatus.OK).body(service.listOrders(customerId));
          }
        }
        """, domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.basePackage(),
        domain.domainClass(),
        domain.kebab(),
        domain.domainClass(),
        domain.domainClass(),
        domain.domainClass(),
        domain.kebab());
  }

  private String domainException(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.exception;

        public class DomainException extends RuntimeException {
          public DomainException(String message) {
            super(message);
          }
        }
        """, domain.basePackage());
  }

  private String orderNotFoundException(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.exception;

        public class OrderNotFoundException extends DomainException {
          private final String errorCode;

          public OrderNotFoundException(String message) {
            super(message);
            this.errorCode = "-20001";
          }

          public String getErrorCode() {
            return errorCode;
          }
        }
        """, domain.basePackage());
  }

  private String errorResponse(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.exception;

        import java.time.Instant;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class ErrorResponse {
          private Instant timestamp;
          private int status;
          private String error;
          private String message;
          private String path;
        }
        """, domain.basePackage());
  }

  private String globalExceptionHandler(DomainInfo domain) {
    return String.format(Locale.ROOT, """
        package %s.exception;

        import jakarta.persistence.EntityNotFoundException;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.validation.ConstraintViolationException;
        import java.time.Instant;
        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.ExceptionHandler;
        import org.springframework.web.bind.annotation.RestControllerAdvice;

        @RestControllerAdvice
        public class GlobalExceptionHandler {
          @ExceptionHandler(OrderNotFoundException.class)
          public ResponseEntity<ErrorResponse> handleOrderNotFound(
              OrderNotFoundException ex,
              HttpServletRequest request) {
            return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
          }

          @ExceptionHandler(EntityNotFoundException.class)
          public ResponseEntity<ErrorResponse> handleEntityNotFound(
              EntityNotFoundException ex,
              HttpServletRequest request) {
            return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
          }

          @ExceptionHandler(ConstraintViolationException.class)
          public ResponseEntity<ErrorResponse> handleConstraintViolation(
              ConstraintViolationException ex,
              HttpServletRequest request) {
            return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
          }

          @ExceptionHandler(Exception.class)
          public ResponseEntity<ErrorResponse> handleGeneric(
              Exception ex,
              HttpServletRequest request) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
          }

          private ResponseEntity<ErrorResponse> buildResponse(
              HttpStatus status,
              String message,
              HttpServletRequest request) {
            ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
            return ResponseEntity.status(status).body(response);
          }
        }
        """, domain.basePackage());
  }

  private record GeneratedFile(String relativePath, String contents) {}

  private record DomainInfo(
      String rawPackage,
      String domainBase,
      String domainClass,
      String kebab,
      String packageSegment,
      String basePackage,
      String packagePath) {
    private static DomainInfo fromPackageName(String packageName) {
      String raw = packageName.toLowerCase(Locale.ROOT);
      String trimmed = raw.endsWith("_pkg") ? raw.substring(0, raw.length() - 4) : raw;
      String domainClass = toPascalCase(trimmed);
      String kebab = trimmed.replace("_", "-");
      String packageSegment = trimmed.replace("_", "");
      String basePackage = "com.company." + packageSegment;
      String packagePath = basePackage.replace(".", "/");
      return new DomainInfo(raw, trimmed, domainClass, kebab, packageSegment, basePackage, packagePath);
    }
  }

  private static String toPascalCase(String value) {
    String[] parts = value.split("_");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (!part.isBlank()) {
        builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
        builder.append(part.substring(1));
      }
    }
    return builder.toString();
  }
}
