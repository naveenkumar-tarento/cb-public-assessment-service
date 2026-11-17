package com.assessment.cassandra.utils;

import com.assessment.model.SBApiResponse;
import com.assessment.util.Constants;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImplTest {

    @Mock
    private CassandraConnectionManager connectionManager;

    @Mock
    private Session session;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ColumnDefinitions columnDefinitions;

    @Mock
    private ColumnDefinitions.Definition definition;

    @Mock
    private Row row;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ExecutionInfo executionInfo;

    private final CassandraOperationImpl cassandraOperation = new CassandraOperationImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cassandraOperation, "connectionManager", connectionManager);
        lenient().when(connectionManager.getSession(anyString())).thenReturn(session);
    }

    private void stubSingleRowResult() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Collections.singletonList(definition));
        when(definition.getName()).thenReturn("id");
        when(row.getObject("id")).thenReturn("row-1");
        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());
    }

    @Test
    void insertRecordShouldReturnFailedResponseWhenPreparedStatementBindingFails() {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        Map<String, Object> request = new HashMap<>();
        request.put("col1", "val1");

        SBApiResponse response = cassandraOperation.insertRecord("ks", "tbl", request);

        assertEquals(Constants.FAILED, response.get(Constants.RESPONSE));
    }

    @Test
    void insertBulkRecordShouldNotThrowOnFailure() {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        List<Map<String, Object>> requests = new ArrayList<>();
        Map<String, Object> req = new HashMap<>();
        req.put("col1", "val1");
        requests.add(req);

        SBApiResponse response = cassandraOperation.insertBulkRecord("ks", "tbl", requests);
        assertNotNull(response);
    }

    @Test
    void getRecordsByPropertiesShouldReturnMappedRows() {
        stubSingleRowResult();
        Map<String, Object> propertyMap = Collections.singletonMap("userId", "u1");

        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("ks", "tbl", propertyMap, null);

        assertEquals(1, result.size());
        assertEquals("row-1", result.get(0).get("id"));
    }

    @Test
    void getRecordsByPropertiesWithFieldsAndListValueShouldUseInClause() {
        stubSingleRowResult();
        Map<String, Object> propertyMap = Collections.singletonMap("userId", Arrays.asList("u1", "u2"));

        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("ks", "tbl", propertyMap,
                Collections.singletonList("id"));

        assertEquals(1, result.size());
    }

    @Test
    void getRecordsByPropertiesShouldReturnEmptyListOnException() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("db down"));

        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRecordsByPropertiesWithKeyShouldReturnMapIndexedByKey() {
        stubSingleRowResult();
        Map<String, Object> result = cassandraOperation.getRecordsByProperties("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null, "id");

        assertTrue(result.containsKey("row-1"));
    }

    @Test
    void searchByWhereClauseShouldReturnMappedRows() {
        stubSingleRowResult();
        List<Map<String, Object>> result = cassandraOperation.searchByWhereClause("ks", "tbl",
                Collections.singletonList("id"), new Date());

        assertEquals(1, result.size());
    }

    @Test
    void getRecordsByPropertiesWithPaginationShouldApplyLimitAndUpdatedOn() {
        stubSingleRowResult();
        Map<String, Object> result = cassandraOperation.getRecordsByPropertiesWithPagination("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null, 10,
                java.util.UUID.randomUUID().toString(), "id");

        assertTrue(result.containsKey("row-1"));
    }

    @Test
    void getRecordsByPropertiesWithPaginationShouldReturnEmptyMapOnException() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("db down"));

        Map<String, Object> result = cassandraOperation.getRecordsByPropertiesWithPagination("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null, 10, null, "id");

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteRecordShouldExecuteDeleteStatement() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        Map<String, Object> compositeKey = Collections.singletonMap("id", "u1");

        cassandraOperation.deleteRecord("ks", "tbl", compositeKey);
    }

    @Test
    void deleteRecordShouldRethrowOnException() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("db down"));
        Map<String, Object> compositeKey = Collections.singletonMap("id", "u1");

        assertThrows(RuntimeException.class,
                () -> cassandraOperation.deleteRecord("ks", "tbl", compositeKey));
    }

    @Test
    void updateRecordShouldReturnSuccessResponse() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        Map<String, Object> updateAttrs = Collections.singletonMap("status", "DONE");
        Map<String, Object> compositeKey = Collections.singletonMap("id", "u1");

        Map<String, Object> response = cassandraOperation.updateRecord("ks", "tbl", updateAttrs, compositeKey);

        assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    }

    @Test
    void updateRecordShouldThrowAndSetFailedResponseOnException() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("db down"));
        Map<String, Object> updateAttrs = Collections.singletonMap("status", "DONE");
        Map<String, Object> compositeKey = Collections.singletonMap("id", "u1");

        assertThrows(RuntimeException.class,
                () -> cassandraOperation.updateRecord("ks", "tbl", updateAttrs, compositeKey));
    }

    @Test
    void getRecordCountShouldReturnCountFromRow() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getLong(0)).thenReturn(5L);

        Long count = cassandraOperation.getRecordCount("ks", "tbl");

        assertEquals(5L, count);
    }

    @Test
    void getRecordCountShouldRethrowOnException() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("db down"));
        assertThrows(RuntimeException.class, () -> cassandraOperation.getRecordCount("ks", "tbl"));
    }

    @Test
    void getAllRecordsShouldPopulateObjectInfoMap() {
        stubSingleRowResult();
        Map<String, Map<String, String>> objectInfo = new HashMap<>();

        cassandraOperation.getAllRecords("ks", "tbl", Collections.singletonList("id"), "id", objectInfo);

        assertTrue(objectInfo.containsKey("row-1"));
    }

    @Test
    void getRecordsWithInClauseShouldReturnMappedRows() {
        stubSingleRowResult();
        List<Map<String, Object>> propertyMaps = Collections.singletonList(
                Collections.singletonMap("userId", Arrays.asList("u1", "u2")));

        List<Map<String, Object>> result = cassandraOperation.getRecordsWithInClause("ks", "tbl", propertyMaps,
                Collections.singletonList("id"));

        assertEquals(1, result.size());
    }

    @Test
    void getRecordsByPropertiesWithoutFilteringShouldReturnMappedRows() {
        stubSingleRowResult();
        List<Map<String, Object>> result = cassandraOperation.getRecordsByPropertiesWithoutFiltering("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null, 5);

        assertEquals(1, result.size());
    }

    @Test
    void getRecordsByPropertiesWithoutFilteringOverloadShouldDelegate() {
        stubSingleRowResult();
        List<Map<String, Object>> result = cassandraOperation.getRecordsByPropertiesWithoutFiltering("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null);

        assertEquals(1, result.size());
    }

    @Test
    void getRecordsByPropertiesByKeyShouldReturnMapIndexedByKey() {
        stubSingleRowResult();
        Map<String, Object> result = cassandraOperation.getRecordsByPropertiesByKey("ks", "tbl",
                Collections.singletonMap("userId", "u1"), null, "id");

        assertTrue(result.containsKey("row-1"));
    }

    @Test
    void getKarmaPointsRecordsByPropertiesWithPaginationListShouldReturnMappedRows() {
        stubSingleRowResult();
        List<Map<String, Object>> result = cassandraOperation.getKarmaPointsRecordsByPropertiesWithPaginationList(
                "ks", "tbl", Collections.singletonMap("userId", "u1"), null, 10, new Date(), "id", new Date());

        assertEquals(1, result.size());
    }

    @Test
    void getRecordCountWithUserIdShouldReturnCount() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getLong(0)).thenReturn(3L);

        Long count = cassandraOperation.getRecordCountWithUserId("ks", "tbl", "u1", new Date());

        assertEquals(3L, count);
    }

    @Test
    void getRecordCountWithUserIdShouldRethrowOnException() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("db down"));
        assertThrows(RuntimeException.class,
                () -> cassandraOperation.getRecordCountWithUserId("ks", "tbl", "u1", new Date()));
    }

    @Test
    void getRecordByIdentifierWithPageShouldReturnResultsAndPagingState() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Collections.singletonList(definition));
        when(definition.getName()).thenReturn("id");
        when(row.getObject("id")).thenReturn("row-1");
        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());
        when(resultSet.getAvailableWithoutFetching()).thenReturn(1);
        when(resultSet.getExecutionInfo()).thenReturn(executionInfo);
        when(executionInfo.getPagingState()).thenReturn(null);

        Map<String, Object> response = cassandraOperation.getRecordByIdentifierWithPage("ks", "tbl",
                Collections.singletonMap("userId", "u1"), Collections.singletonList("id"), null, 10);

        assertNotNull(response.get(Constants.RESPONSE));
    }

    @Test
    void getRecordByIdentifierWithPageShouldHandlePagingStateString() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Collections.emptyList());
        when(resultSet.iterator()).thenReturn(Collections.<Row>emptyList().iterator());
        when(resultSet.getAvailableWithoutFetching()).thenReturn(0);
        when(resultSet.getExecutionInfo()).thenReturn(executionInfo);
        lenient().when(executionInfo.getPagingState()).thenReturn(null);

        Map<String, Object> response = cassandraOperation.getRecordByIdentifierWithPage("ks", "tbl",
                Collections.emptyMap(), null, null, 10);

        assertNotNull(response.get(Constants.RESPONSE));
    }

    @Test
    void getCountOfRecordByIdentifierShouldThrowForEmptyKeyAndReturnZero() {
        Long count = cassandraOperation.getCountOfRecordByIdentifier("ks", "tbl", Collections.emptyMap(), "id");
        assertEquals(0L, count);
    }

    @Test
    void getCountOfRecordByIdentifierShouldComputeCountFromResponse() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Collections.singletonList(definition));
        when(definition.getName()).thenReturn("system.count(id)");
        when(row.getObject("system.count(id)")).thenReturn(7L);
        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());

        Long count = cassandraOperation.getCountOfRecordByIdentifier("ks", "tbl",
                Collections.singletonMap("userId", "u1"), "id");

        assertEquals(7L, count);
    }
}
