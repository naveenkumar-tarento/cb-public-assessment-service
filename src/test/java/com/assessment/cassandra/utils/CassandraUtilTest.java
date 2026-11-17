package com.assessment.cassandra.utils;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CassandraUtilTest {

    @Mock
    private ResultSet resultSet;

    @Mock
    private ColumnDefinitions columnDefinitions;

    @Mock
    private ColumnDefinitions.Definition definition;

    @Mock
    private Row row;

    @Test
    void getPreparedStatementShouldBuildValidInsertQuery() {
        Map<String, Object> values = new HashMap<>();
        values.put("col1", "val1");

        String statement = CassandraUtil.getPreparedStatement("myKeyspace", "myTable", values);

        assertTrue(statement.startsWith("INSERT INTO myKeyspace.myTable(col1) VALUES ("));
        assertTrue(statement.endsWith(");"));
    }

    @Test
    void getPreparedStatementShouldSeparateMultipleColumnsWithCommas() {
        Map<String, Object> values = new HashMap<>();
        values.put("col1", "val1");
        values.put("col2", "val2");

        String statement = CassandraUtil.getPreparedStatement("ks", "tbl", values);

        assertTrue(statement.contains("col1,col2") || statement.contains("col2,col1"));
        long questionMarks = statement.chars().filter(ch -> ch == '?').count();
        assertEquals(2, questionMarks);
    }

    @Test
    void createResponseShouldMapRowsUsingColumnNames() {
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Collections.singletonList(definition));
        when(definition.getName()).thenReturn("id");

        when(row.getObject("id")).thenReturn("row-id-1");
        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());

        List<Map<String, Object>> response = CassandraUtil.createResponse(resultSet);

        assertEquals(1, response.size());
        assertEquals("row-id-1", response.get(0).get("id"));
    }

    @Test
    void createResponseWithKeyShouldIndexByGivenColumn() {
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Collections.singletonList(definition));
        when(definition.getName()).thenReturn("id");

        when(row.getObject("id")).thenReturn("row-id-1");
        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());

        Map<String, Object> response = CassandraUtil.createResponse(resultSet, "id");

        assertTrue(response.containsKey("row-id-1"));
    }

    @Test
    void fetchColumnsMappingShouldMapPropertyNameToColumnName() {
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.asList()).thenReturn(Arrays.asList(definition));
        when(definition.getName()).thenReturn("colName");

        Map<String, String> mapping = CassandraUtil.fetchColumnsMapping(resultSet);

        assertEquals("colName", mapping.get("colName"));
    }
}
