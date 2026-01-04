// Elasticsearch service for indexing
package com.yowyob.erp.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {

    private final ElasticsearchClient client;

    /**
     * Creates an index if it does not exist
     */
    public void createIndexIfNotExists(String index_name) {
        try {
            ExistsRequest exists_request = ExistsRequest.of(e -> e.index(index_name));
            boolean exists = client.indices().exists(exists_request).value();

            if (!exists) {
                CreateIndexRequest create_request = CreateIndexRequest.of(c -> c
                        .index(index_name)
                        .mappings(m -> m
                                .properties("tenantId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                                .properties("id", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                                .properties("createdAt", Property.of(p -> p.date(DateProperty.of(d -> d))))
                                .properties("updatedAt", Property.of(p -> p.date(DateProperty.of(d -> d))))
                                .properties("searchText", Property.of(p -> p.text(TextProperty.of(t -> t
                                        .analyzer("standard")
                                        .searchAnalyzer("standard")))))));

                client.indices().create(create_request);
                log.info("Index created: {}", index_name);
            }
        } catch (IOException e) {
            log.error("Error creating index: {}", index_name, e);
        }
    }

    /**
     * Indexes a document
     */
    public void indexDocument(String index_name, String document_id, Object document) {
        try {
            createIndexIfNotExists(index_name);

            IndexRequest<Object> request = IndexRequest.of(i -> i
                    .index(index_name)
                    .id(document_id)
                    .document(document));

            client.index(request);
            log.debug("Document indexed: {} in index: {}", document_id, index_name);
        } catch (IOException e) {
            log.error("Error indexing document: {} in index: {}", document_id, index_name, e);
        }
    }

    /**
     * Search documents
     */
    public <T> List<T> searchDocuments(String index_name, String query, Class<T> clazz, String tenant_id) {
        try {
            SearchRequest search_request = SearchRequest.of(s -> s
                    .index(index_name)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .multiMatch(mm -> mm
                                                    .query(query)
                                                    .fields("searchText", "libelle", "description")))
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("tenantId")
                                                    .value(tenant_id)))))
                    .size(100));

            SearchResponse<T> response = client.search(search_request, clazz);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Error searching in index: {}", index_name, e);
            return List.of();
        }
    }

    /**
     * Updates a document
     */
    public void updateDocument(String index_name, String document_id, Object document) {
        try {
            UpdateRequest<Object, Object> request = UpdateRequest.of(u -> u
                    .index(index_name)
                    .id(document_id)
                    .doc(document));

            client.update(request, Object.class);
            log.debug("Document updated: {} in index: {}", document_id, index_name);
        } catch (IOException e) {
            log.error("Error updating document: {} in index: {}", document_id, index_name, e);
        }
    }

    /**
     * Deletes a document
     */
    public void deleteDocument(String index_name, String document_id) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                    .index(index_name)
                    .id(document_id));

            client.delete(request);
            log.debug("Document deleted: {} from index: {}", document_id, index_name);
        } catch (IOException e) {
            log.error("Error deleting document: {} from index: {}", document_id, index_name, e);
        }
    }

    /**
     * Indexes accounting entries for search
     */
    public void indexAccountingEntry(Object entry, String tenant_id) {
        String index_name = "accounting-entries-" + tenant_id.toLowerCase();
        String document_id = extractId(entry);
        indexDocument(index_name, document_id, entry);
    }

    /**
     * Search accounting entries
     */
    public <T> List<T> searchAccountingEntries(String query, Class<T> clazz, String tenant_id) {
        String index_name = "accounting-entries-" + tenant_id.toLowerCase();
        return searchDocuments(index_name, query, clazz, tenant_id);
    }

    /**
     * Indexes clients/suppliers
     */
    public void indexThirdParty(Object third_party, String tenant_id, String type) {
        String index_name = String.format("%s-%s", type.toLowerCase(), tenant_id.toLowerCase());
        String document_id = extractId(third_party);
        indexDocument(index_name, document_id, third_party);
    }

    /**
     * Search third parties (clients/suppliers)
     */
    public <T> List<T> searchThirdParties(String query, String type, Class<T> clazz, String tenant_id) {
        String index_name = String.format("%s-%s", type.toLowerCase(), tenant_id.toLowerCase());
        return searchDocuments(index_name, query, clazz, tenant_id);
    }

    private String extractId(Object obj) {
        if (obj == null)
            return java.util.UUID.randomUUID().toString();

        if (obj instanceof java.util.Map<?, ?> map) {
            Object id = map.get("id");
            if (id != null)
                return id.toString();
            id = map.get("uuid");
            if (id != null)
                return id.toString();
        }

        try {
            // Uses reflection to extract ID
            java.lang.reflect.Field id_field = obj.getClass().getDeclaredField("id");
            id_field.setAccessible(true);
            Object id = id_field.get(obj);
            return id != null ? id.toString() : java.util.UUID.randomUUID().toString();
        } catch (Exception e) {
            log.warn("Unable to extract object ID for {}, generating UUID", obj.getClass().getName());
            return java.util.UUID.randomUUID().toString();
        }
    }
}
