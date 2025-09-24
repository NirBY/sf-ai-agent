# Unit Tests

This directory contains unit tests for the Salesforce AI Agent project.

## Test Structure

```
src/test/java/com/nby/agent/
├── AgentApplicationTest.java          # Integration test for Spring Boot context
├── config/
│   ├── AppConfigTest.java            # Tests for application configuration
│   └── PromptTemplatesTest.java      # Tests for prompt templates
├── llm/
│   ├── LlmFactoryTest.java           # Tests for LLM provider factory
│   └── RagServiceTest.java           # Tests for RAG service with metrics
├── metrics/
│   ├── MetricsServiceTest.java       # Unit tests for metrics service
│   └── MetricsIntegrationTest.java   # Integration tests for metrics
├── scheduler/
│   └── CaseWatcherTest.java          # Tests for case watcher with metrics
└── storage/
    ├── CaseMemoryEntityTest.java     # Tests for case memory entity
    └── CaseMemoryRepositoryTest.java # Tests for case memory repository
```

## Running Tests

### Run all tests:
```bash
mvn test
```

### Run specific test class:
```bash
mvn test -Dtest=PromptTemplatesTest
```

### Run tests with coverage:
```bash
mvn test jacoco:report
```

## Test Categories

### Unit Tests
- **PromptTemplatesTest**: Tests prompt generation with various inputs
- **AppConfigTest**: Tests timezone configuration
- **LlmFactoryTest**: Tests LLM provider selection logic
- **RagServiceTest**: Tests RAG service with metrics integration
- **MetricsServiceTest**: Tests metrics service functionality
- **CaseWatcherTest**: Tests case watcher with metrics integration
- **CaseMemoryEntityTest**: Tests data entity behavior
- **CaseMemoryRepositoryTest**: Tests database operations with metrics

### Integration Tests
- **AgentApplicationTest**: Tests Spring Boot context loading
- **MetricsIntegrationTest**: Tests metrics integration with Micrometer

## Test Configuration

Tests use `application-test.yml` for configuration:
- Random port assignment
- Reduced logging levels
- Test-specific database settings

## Test Dependencies

- **JUnit 5**: Testing framework
- **Spring Boot Test**: Integration testing
- **Mockito**: Mocking framework (via Spring Boot Test)
- **TestContainers**: Container-based testing (for future use)

## Best Practices

1. **Test Isolation**: Each test is independent
2. **Clear Naming**: Test methods describe what they test
3. **Given-When-Then**: Tests follow the BDD pattern
4. **Edge Cases**: Tests cover null, empty, and special character inputs
5. **Mocking**: External dependencies are mocked where appropriate
